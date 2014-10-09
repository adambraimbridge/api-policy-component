package com.ft.up.apipolicy;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.up.apipolicy.configuration.ApplicationConfiguration;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.resources.ReaderEndpointsResource;
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
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new ReaderEndpointsResource());

        environment.servlets().addFilter(
                "Slow Servlet Filter",
                new SlowRequestFilter(Duration.milliseconds(configuration.getSlowRequestTimeout()))).addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                false,
                configuration.getSlowRequestPattern());

        environment.healthChecks().register("My Health", new ReaderNodesHealthCheck("replace me"));



    }

}
