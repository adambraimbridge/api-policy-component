package com.ft.up.apipolicy;

import com.ft.api.jaxrs.errors.RuntimeExceptionMapper;
import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.platform.dropwizard.DefaultGoodToGoChecker;
import com.ft.platform.dropwizard.GoodToGoBundle;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.filters.AddBrandFilterParameters;
import com.ft.up.apipolicy.filters.AddSyndication;
import com.ft.up.apipolicy.filters.LinkedContentValidationFilter;
import com.ft.up.apipolicy.filters.NotificationsTypeFilter;
import com.ft.up.apipolicy.filters.PolicyBasedJsonFilter;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.filters.RemoveHeaderUnlessPolicyPresentFilter;
import com.ft.up.apipolicy.filters.RemoveJsonPropertiesUnlessPolicyPresentFilter;
import com.ft.up.apipolicy.filters.SuppressJsonPropertiesFilter;
import com.ft.up.apipolicy.filters.SuppressRichContentMarkupFilter;
import com.ft.up.apipolicy.filters.SyndicationDistributionFilter;
import com.ft.up.apipolicy.filters.WebUrlCalculator;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.KnownEndpoint;
import com.ft.up.apipolicy.resources.RequestHandler;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformerFactory;
import com.sun.jersey.api.client.Client;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_COMMENTS;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_IDENTIFIERS;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_LAST_MODIFIED_DATE;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_PROVENANCE;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;
import static com.ft.up.apipolicy.configuration.Policy.INTERNAL_UNSTABLE;

public class ApiPolicyApplication extends Application<ApiPolicyConfiguration> {

    private static final String MAIN_IMAGE_JSON_PROPERTY = "mainImage";
    private static final String IDENTIFIERS_JSON_PROPERTY = "identifiers";
    private static final String ALT_TITLES_JSON_PROPERTY = "alternativeTitles";
    private static final String ALT_IMAGES_JSON_PROPERTY = "alternativeImages";
    private static final String ALT_STANDFIRST_JSON_PROPERTY = "alternativeStandfirsts";
    private static final String COMMENTS_JSON_PROPERTY = "comments";
    private static final String PROVENANCE_JSON_PROPERTY = "publishReference";
    private static final String LAST_MODIFIED_JSON_PROPERTY = "lastModified";
    private static final String OPENING_XML_JSON_PROPERTY = "openingXML";
    private static final String ACCESS_LEVEL_JSON_PROPERTY = "accessLevel";
    private static final String CONTENT_PACKAGE_CONTAINS_JSON_PROPERTY = "contains";
    private static final String CONTENT_PACKAGE_CONTAINED_IN_JSON_PROPERTY = "containedIn";
    private static final String ACCESS_LEVEL_HEADER = "X-FT-Access-Level";

    private ApiFilter mainImageFilter;
    private ApiFilter identifiersFilter;
    private ApiFilter alternativeTitlesFilter;
    private ApiFilter alternativeImagesFilter;
    private ApiFilter alternativeStandfirstsFilter;
    private ApiFilter stripCommentsFields;
    private ApiFilter removeCommentsFieldRegardlessOfPolicy;
    private ApiFilter stripProvenance;
    private ApiFilter suppressMarkup;
    private ApiFilter webUrlAdder;
    private ApiFilter addSyndication;
    private ApiFilter brandFilter;
    private ApiFilter stripLastModifiedDate;
    private ApiFilter _unstable_stripOpeningXml;
    private ApiFilter linkValidationFilter;
    private ApiFilter mediaResourceNotificationsFilter;
    private ApiFilter accessLevelPropertyFilter;
    private ApiFilter removeAccessFieldRegardlessOfPolicy;
    private ApiFilter accessLevelHeaderFilter;
    private ApiFilter syndicationDistributionFilter;
    private ApiFilter contentPackageFilter;

    public static void main(final String[] args) throws Exception {
        new ApiPolicyApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<ApiPolicyConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
        bootstrap.addBundle(new GoodToGoBundle(new DefaultGoodToGoChecker()));
    }

    @Override
    public void run(final ApiPolicyConfiguration configuration, final Environment environment) throws Exception {
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new RuntimeExceptionMapper());
        setFilters(configuration, environment);

        SortedSet<KnownEndpoint> knownWildcardEndpoints = new TreeSet<>();
        SortedSet<KnownEndpoint> knownWhitelistedPostEndpoints = new TreeSet<>();

        //identifiersFilter needs to be added before webUrlAdder in the pipeline since webUrlAdder's logic is based on the json property that identifiersFilter might remove
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/.*", "content",
                identifiersFilter,
                webUrlAdder,
                addSyndication,
                linkValidationFilter,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                removeCommentsFieldRegardlessOfPolicy,
                stripProvenance,
                stripLastModifiedDate,
                _unstable_stripOpeningXml,
                removeAccessFieldRegardlessOfPolicy,
                syndicationDistributionFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content-preview/.*", "content-preview",
                identifiersFilter,
                webUrlAdder,
                addSyndication,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                _unstable_stripOpeningXml,
                removeAccessFieldRegardlessOfPolicy));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/internalcontent-preview/.*", "internalcontent-preview",
                identifiersFilter,
                webUrlAdder,
                addSyndication,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                _unstable_stripOpeningXml,
                removeAccessFieldRegardlessOfPolicy));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/notifications.*", "notifications",
                mediaResourceNotificationsFilter,
                brandFilter,
                notificationsFilter()));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/enrichedcontent/.*", "enrichedcontent",
                identifiersFilter,
                webUrlAdder,
                addSyndication,
                linkValidationFilter,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                _unstable_stripOpeningXml,
                accessLevelPropertyFilter,
                accessLevelHeaderFilter,
                syndicationDistributionFilter,
                contentPackageFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/internalcontent/.*", "internalcontent",
                identifiersFilter,
                webUrlAdder,
                addSyndication,
                linkValidationFilter,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                _unstable_stripOpeningXml,
                accessLevelPropertyFilter,
                accessLevelHeaderFilter,
                syndicationDistributionFilter,
                contentPackageFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists.*", "lists",
                stripProvenance,
                stripLastModifiedDate));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists/notifications.*", "lists-notifications", notificationsFilter()));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/concordances.*", "concordances", new ApiFilter[]{}));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/things.*", "things", new ApiFilter[]{}));

        // DEFAULT CASE: Just forward it
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/.*", "other", new ApiFilter[]{}));

        // Must specifically list any POST, PUT etc endpoint you want access to
        knownWhitelistedPostEndpoints.add(createEndpoint(environment, configuration, "^/suggest", "suggest", new ApiFilter[]{}));

        environment.jersey().register(new WildcardEndpointResource(new RequestHandler(new MutableHttpTranslator(), knownWildcardEndpoints),
                new RequestHandler(new MutableHttpTranslator(), knownWhitelistedPostEndpoints)));

        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        Client healthcheckClient;
        if (configuration.isCheckingVulcanHealth()) {
            healthcheckClient = ResilientClientBuilder.in(environment).usingDNS().named("healthcheck-client").build();
        } else {
            healthcheckClient = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).named("healthcheck-client").build();
        }
        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity ", configuration.getVarnish(), healthcheckClient, configuration.isCheckingVulcanHealth()));
    }

    private BodyProcessingFieldTransformer getBodyProcessingFieldTransformer() {
        return (BodyProcessingFieldTransformer) (new BodyProcessingFieldTransformerFactory()).newInstance();
    }

    private void setFilters(ApiPolicyConfiguration configuration, Environment environment) {
        JsonConverter jsonTweaker = new JsonConverter(environment.getObjectMapper());
        PolicyBrandsResolver resolver = configuration.getPolicyBrandsResolver();

        mainImageFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_RICH_CONTENT, MAIN_IMAGE_JSON_PROPERTY);
        identifiersFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_IDENTIFIERS, IDENTIFIERS_JSON_PROPERTY);
        alternativeTitlesFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, ALT_TITLES_JSON_PROPERTY);
        alternativeImagesFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, ALT_IMAGES_JSON_PROPERTY);
        alternativeStandfirstsFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, ALT_STANDFIRST_JSON_PROPERTY);
        stripCommentsFields = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_COMMENTS, COMMENTS_JSON_PROPERTY);
        removeCommentsFieldRegardlessOfPolicy = new SuppressJsonPropertiesFilter(jsonTweaker, COMMENTS_JSON_PROPERTY);
        stripProvenance = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_PROVENANCE, PROVENANCE_JSON_PROPERTY);
        stripLastModifiedDate =  new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_LAST_MODIFIED_DATE, LAST_MODIFIED_JSON_PROPERTY);
        _unstable_stripOpeningXml = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, OPENING_XML_JSON_PROPERTY);
        suppressMarkup = new SuppressRichContentMarkupFilter(jsonTweaker, getBodyProcessingFieldTransformer());
        webUrlAdder = new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(), jsonTweaker);
        addSyndication = new AddSyndication(jsonTweaker, INTERNAL_UNSTABLE);
        brandFilter = new AddBrandFilterParameters(jsonTweaker, resolver);
        linkValidationFilter = new LinkedContentValidationFilter();
        mediaResourceNotificationsFilter = new NotificationsTypeFilter(jsonTweaker, INTERNAL_UNSTABLE);
        accessLevelPropertyFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, ACCESS_LEVEL_JSON_PROPERTY);
        removeAccessFieldRegardlessOfPolicy = new SuppressJsonPropertiesFilter(jsonTweaker, ACCESS_LEVEL_JSON_PROPERTY);
        accessLevelHeaderFilter = new RemoveHeaderUnlessPolicyPresentFilter(ACCESS_LEVEL_HEADER, INTERNAL_UNSTABLE);
        syndicationDistributionFilter = new SyndicationDistributionFilter(jsonTweaker, INTERNAL_UNSTABLE);
        contentPackageFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, CONTENT_PACKAGE_CONTAINS_JSON_PROPERTY, CONTENT_PACKAGE_CONTAINED_IN_JSON_PROPERTY);
    }
    
    private ApiFilter notificationsFilter() {
      Map<String,Policy> notificationsJsonFilters = new HashMap<>();
      // whitelisted (no policy required)
      notificationsJsonFilters.put("$.requestUrl", null);
      notificationsJsonFilters.put("$.links[*].*", null);
      notificationsJsonFilters.put("$.notifications[*].id", null);
      notificationsJsonFilters.put("$.notifications[*].type", null);
      notificationsJsonFilters.put("$.notifications[*].apiUrl", null);
      // restricted (policy required)
      notificationsJsonFilters.put("$.notifications[*].lastModified", INCLUDE_LAST_MODIFIED_DATE);
      notificationsJsonFilters.put("$.notifications[*].notificationDate", INCLUDE_LAST_MODIFIED_DATE);
      notificationsJsonFilters.put("$.notifications[*].publishReference", INCLUDE_PROVENANCE);

      return new PolicyBasedJsonFilter(notificationsJsonFilters);
    }
    
    private KnownEndpoint createEndpoint(Environment environment, ApiPolicyConfiguration configuration,
                                         String urlPattern, String instanceName, ApiFilter... filterChain) {
        final Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).named(instanceName).build();

        final RequestForwarder requestForwarder = new JerseyRequestForwarder(client, configuration.getVarnish());
        return new KnownEndpoint(urlPattern, new HttpPipeline(requestForwarder, filterChain));
    }
}
