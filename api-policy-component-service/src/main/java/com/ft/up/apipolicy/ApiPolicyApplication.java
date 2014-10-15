package com.ft.up.apipolicy;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.up.apipolicy.configuration.ApplicationConfiguration;
import com.ft.up.apipolicy.filters.WebUrlCalculator;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.KnownEndpoint;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.sun.jersey.api.client.Client;
import io.dropwizard.Application;

import io.dropwizard.servlets.SlowRequestFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.SortedSet;
import java.util.TreeSet;

public class ApiPolicyApplication extends Application<ApplicationConfiguration> {

    public static void main(final String[] args) throws Exception {
        new ApiPolicyApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
    }

    @Override
    public void run(final ApplicationConfiguration configuration, final Environment environment) throws Exception {
        environment.jersey().register(new BuildInfoResource());

        Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).build();



		RequestForwarder requestForwarder = new JerseyRequestForwarder(client,configuration.getVarnish());

        SortedSet<KnownEndpoint> knownEndpoints = new TreeSet<>();
		knownEndpoints.add(new KnownEndpoint("^/content/.*",
				new HttpPipeline(requestForwarder, new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(),environment.getObjectMapper()))));

        knownEndpoints.add(new KnownEndpoint("^/content/notifications.*",
                new HttpPipeline(requestForwarder)));

        // DEFAULT CASE: Just forward it
        knownEndpoints.add(new KnownEndpoint("^/.*", new HttpPipeline(requestForwarder)));


        environment.jersey().register(new WildcardEndpointResource(new MutableHttpTranslator(), knownEndpoints));

        environment.servlets().addFilter(
                "Slow Servlet Filter",
                new SlowRequestFilter(Duration.milliseconds(configuration.getSlowRequestTimeout()))).addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                false,
                configuration.getSlowRequestPattern());




        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity", configuration.getVarnish(), client));



    }

}
