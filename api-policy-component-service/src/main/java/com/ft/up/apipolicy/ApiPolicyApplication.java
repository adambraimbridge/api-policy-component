package com.ft.up.apipolicy;

import java.util.EnumSet;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.DispatcherType;

import com.ft.api.jaxrs.errors.RuntimeExceptionMapper;
import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.filters.AddBrandFilterParameters;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.filters.RemoveJsonPropertyUnlessPolicyPresentFilter;
import com.ft.up.apipolicy.filters.SuppressJsonPropertyFilter;
import com.ft.up.apipolicy.filters.SuppressRichContentMarkupFilter;
import com.ft.up.apipolicy.filters.WebUrlCalculator;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.KnownEndpoint;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformerFactory;
import com.sun.jersey.api.client.Client;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ApiPolicyApplication extends Application<ApiPolicyConfiguration> {

    private static final String MAIN_IMAGE_JSON_PROPERTY = "mainImage";
    private static final String IDENTIFIERS_JSON_PROPERTY = "identifiers";
    private static final String COMMENTS_JSON_PROPERTY = "comments";

    public static void main(final String[] args) throws Exception {
        new ApiPolicyApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<ApiPolicyConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
    }

    @Override
    public void run(final ApiPolicyConfiguration configuration, final Environment environment) throws Exception {
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new RuntimeExceptionMapper());

        Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).build();

		RequestForwarder requestForwarder = new JerseyRequestForwarder(client,configuration.getVarnish());

        JsonConverter jsonTweaker = new JsonConverter(environment.getObjectMapper());


        final ApiFilter webUrlAdder = new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(),
                jsonTweaker);
        final ApiFilter mainImageFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, MAIN_IMAGE_JSON_PROPERTY, Policy.INCLUDE_RICH_CONTENT);
        final ApiFilter identifiersFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, IDENTIFIERS_JSON_PROPERTY, Policy.INCLUDE_IDENTIFIERS);
        final ApiFilter commentsFilterForEnrichedContentEndpoint = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, COMMENTS_JSON_PROPERTY, Policy.INCLUDE_COMMENTS);
        final ApiFilter commentsFilterForContentEndpoint = new SuppressJsonPropertyFilter(jsonTweaker, COMMENTS_JSON_PROPERTY);

        ApiFilter suppressMarkup = new SuppressRichContentMarkupFilter(jsonTweaker, getBodyProcessingFieldTransformer());

        SortedSet<KnownEndpoint> knownEndpoints = new TreeSet<>();
        //identifiersFilter needs to be added before webUrlAdder in the pipeline since webUrlAdder's logic is based on the json property that identifiersFilter might remove
		knownEndpoints.add(new KnownEndpoint("^/content/.*",
				new HttpPipeline(requestForwarder, identifiersFilter, webUrlAdder, suppressMarkup, mainImageFilter, commentsFilterForContentEndpoint)));

        PolicyBrandsResolver resolver = configuration.getPolicyBrandsResolver();

        knownEndpoints.add(new KnownEndpoint("^/content/notifications.*",
                new HttpPipeline(requestForwarder, new AddBrandFilterParameters(jsonTweaker, resolver))));

        knownEndpoints.add(new KnownEndpoint("^/enrichedcontent/.*",
                new HttpPipeline(requestForwarder, identifiersFilter, webUrlAdder, suppressMarkup, mainImageFilter, commentsFilterForEnrichedContentEndpoint)));

        // DEFAULT CASE: Just forward it
        knownEndpoints.add(new KnownEndpoint("^/.*", new HttpPipeline(requestForwarder)));


        environment.jersey().register(new WildcardEndpointResource(new MutableHttpTranslator(), knownEndpoints));

        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity", configuration.getVarnish(), client));

        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    }
    private BodyProcessingFieldTransformer getBodyProcessingFieldTransformer() {
        return (BodyProcessingFieldTransformer) (new BodyProcessingFieldTransformerFactory()).newInstance();
    }

}
