package com.ft.up.apipolicy;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.up.apipolicy.configuration.ApplicationConfiguration;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.MutableHttpToServletsHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.sun.jersey.api.client.Client;
import io.dropwizard.Application;

import io.dropwizard.servlets.SlowRequestFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

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

        Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).build();

        RequestForwarder requestForwarder = new JerseyRequestForwarder(client, configuration.getVarnish());

        environment.jersey().register(
                new WildcardEndpointResource(configuration.getPipelineConfiguration(),
                    requestForwarder,
                    new MutableHttpToServletsHttpTranslator(),
                    environment.getObjectMapper())
                );

        environment.servlets().addFilter(
                "Slow Servlet Filter",
                new SlowRequestFilter(Duration.milliseconds(configuration.getSlowRequestTimeout()))).addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                false,
                configuration.getSlowRequestPattern());



        environment.jersey().register(new BuildInfoResource());
        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity", configuration.getVarnish(), client));



    }

}
