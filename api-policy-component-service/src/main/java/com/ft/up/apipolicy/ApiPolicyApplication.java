package com.ft.up.apipolicy;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.platform.dropwizard.GoodToGoBundle;
import com.ft.platform.dropwizard.GoodToGoResult;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.filters.AddBrandFilterParameters;
import com.ft.up.apipolicy.filters.AddSyndication;
import com.ft.up.apipolicy.filters.CanBeDistributedAccessFilter;
import com.ft.up.apipolicy.filters.CanBeSyndicatedAccessFilter;
import com.ft.up.apipolicy.filters.ExpandedImagesFilter;
import com.ft.up.apipolicy.filters.LinkedContentValidationFilter;
import com.ft.up.apipolicy.filters.NotificationsTypeFilter;
import com.ft.up.apipolicy.filters.PolicyBasedJsonFilter;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.filters.RemoveHeaderUnlessPolicyPresentFilter;
import com.ft.up.apipolicy.filters.RemoveJsonPropertiesUnlessPolicyPresentFilter;
import com.ft.up.apipolicy.filters.SuppressJsonPropertiesFilter;
import com.ft.up.apipolicy.filters.SuppressRichContentMarkupFilter;
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
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.server.ServerProperties;

import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.ft.up.apipolicy.configuration.Policy.EXPAND_RICH_CONTENT;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_COMMENTS;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_IDENTIFIERS;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_LAST_MODIFIED_DATE;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_PROVENANCE;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;
import static com.ft.up.apipolicy.configuration.Policy.INTERNAL_UNSTABLE;
import static com.ft.up.apipolicy.configuration.Policy.RESTRICT_NON_SYNDICATABLE_CONTENT;

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
    private static final String MASTER_SOURCE_JSON_PROPERTY = "masterSource";

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
    private ApiFilter stripOpeningXml;
    private ApiFilter linkValidationFilter;
    private ApiFilter mediaResourceNotificationsFilter;
    private ApiFilter accessLevelPropertyFilter;
    private ApiFilter removeAccessFieldRegardlessOfPolicy;
    private ApiFilter accessLevelHeaderFilter;
    private ApiFilter canBeDistributedAccessFilter;
    private ApiFilter canBeSyndicatedAccessFilter;
    private ApiFilter contentPackageFilter;
    private ApiFilter expandedImagesFilter;

    public static void main(final String[] args) throws Exception {
        new ApiPolicyApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<ApiPolicyConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
        bootstrap.addBundle(new GoodToGoBundle(environment -> new GoodToGoResult(true, "")));
    }

    @Override
    public void run(final ApiPolicyConfiguration configuration, final Environment environment) throws Exception {
        environment.jersey().property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED,true);
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new ApiPolicyExceptionMapper());
        setFilters(configuration, environment);

        Set<KnownEndpoint> knownWildcardEndpoints = new LinkedHashSet<>();
        Set<KnownEndpoint> knownWhitelistedPostEndpoints = new LinkedHashSet<>();

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/things.*", "things", new ApiFilter[]{}));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists/notifications.*", "lists-notifications", notificationsFilter()));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists.*", "lists",
                stripProvenance,
                stripLastModifiedDate));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/internalcontent/.*", "internalcontent",
                canBeDistributedAccessFilter,
                addSyndication,
                canBeSyndicatedAccessFilter,
                identifiersFilter,
                webUrlAdder,
                linkValidationFilter,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                stripOpeningXml,
                accessLevelPropertyFilter,
                accessLevelHeaderFilter,
                contentPackageFilter,
                expandedImagesFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/internalcontent-preview/.*", "internalcontent-preview",
                addSyndication,
                canBeSyndicatedAccessFilter,
                identifiersFilter,
                webUrlAdder,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                stripOpeningXml,
                removeAccessFieldRegardlessOfPolicy,
                expandedImagesFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/enrichedcontent/.*", "enrichedcontent",
                canBeDistributedAccessFilter,
                addSyndication,
                canBeSyndicatedAccessFilter,
                identifiersFilter,
                webUrlAdder,
                linkValidationFilter,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                stripOpeningXml,
                accessLevelPropertyFilter,
                accessLevelHeaderFilter,
                contentPackageFilter,
                expandedImagesFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/notifications.*", "notifications",
                mediaResourceNotificationsFilter,
                brandFilter,
                notificationsFilter()));

        // no filters needed for public-annotations-api
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/annotations", "public-annotations-api", new ApiFilter[]{}));

        //identifiersFilter needs to be added before webUrlAdder in the pipeline since webUrlAdder's logic is based on the json property that identifiersFilter might remove
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/.*", "content",
                canBeDistributedAccessFilter,
                addSyndication,
                canBeSyndicatedAccessFilter,
                identifiersFilter,
                webUrlAdder,
                linkValidationFilter,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                removeCommentsFieldRegardlessOfPolicy,
                stripProvenance,
                stripLastModifiedDate,
                stripOpeningXml,
                removeAccessFieldRegardlessOfPolicy));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content-preview/.*", "content-preview",
                addSyndication,
                canBeSyndicatedAccessFilter,
                identifiersFilter,
                webUrlAdder,
                suppressMarkup,
                mainImageFilter,
                alternativeTitlesFilter,
                alternativeImagesFilter,
                alternativeStandfirstsFilter,
                stripCommentsFields,
                stripProvenance,
                stripLastModifiedDate,
                stripOpeningXml,
                removeAccessFieldRegardlessOfPolicy,
                expandedImagesFilter));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/concordances.*", "concordances", new ApiFilter[]{}));

        // DEFAULT CASE: Just forward it
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/.*", "other", new ApiFilter[]{}));

        // Must specifically list any POST, PUT etc endpoint you want access to
        knownWhitelistedPostEndpoints.add(createEndpoint(environment, configuration, "^/suggest", "suggest", new ApiFilter[]{}));

        environment.jersey().register(new WildcardEndpointResource(new RequestHandler(new MutableHttpTranslator(), knownWildcardEndpoints),
                new RequestHandler(new MutableHttpTranslator(), knownWhitelistedPostEndpoints)));

        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        Client healthcheckClient = JerseyClientBuilder.newClient();
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
        stripProvenance = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_PROVENANCE, PROVENANCE_JSON_PROPERTY, MASTER_SOURCE_JSON_PROPERTY);
        stripLastModifiedDate =  new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INCLUDE_LAST_MODIFIED_DATE, LAST_MODIFIED_JSON_PROPERTY);
        stripOpeningXml = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, OPENING_XML_JSON_PROPERTY);
        suppressMarkup = new SuppressRichContentMarkupFilter(jsonTweaker, getBodyProcessingFieldTransformer());
        webUrlAdder = new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(), jsonTweaker);
        addSyndication = new AddSyndication(jsonTweaker);
        brandFilter = new AddBrandFilterParameters(jsonTweaker, resolver);
        linkValidationFilter = new LinkedContentValidationFilter();
        mediaResourceNotificationsFilter = new NotificationsTypeFilter(jsonTweaker, INTERNAL_UNSTABLE);
        accessLevelPropertyFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, ACCESS_LEVEL_JSON_PROPERTY);
        removeAccessFieldRegardlessOfPolicy = new SuppressJsonPropertiesFilter(jsonTweaker, ACCESS_LEVEL_JSON_PROPERTY);
        accessLevelHeaderFilter = new RemoveHeaderUnlessPolicyPresentFilter(ACCESS_LEVEL_HEADER, INTERNAL_UNSTABLE);
        canBeDistributedAccessFilter = new CanBeDistributedAccessFilter(jsonTweaker, INTERNAL_UNSTABLE);
        canBeSyndicatedAccessFilter = new CanBeSyndicatedAccessFilter(jsonTweaker, RESTRICT_NON_SYNDICATABLE_CONTENT);
        contentPackageFilter = new RemoveJsonPropertiesUnlessPolicyPresentFilter(jsonTweaker, INTERNAL_UNSTABLE, CONTENT_PACKAGE_CONTAINS_JSON_PROPERTY, CONTENT_PACKAGE_CONTAINED_IN_JSON_PROPERTY);
        expandedImagesFilter = new ExpandedImagesFilter(INCLUDE_RICH_CONTENT, EXPAND_RICH_CONTENT);
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
      
        final Client client = JerseyClientBuilder.newBuilder()
            .property(ClientProperties.FOLLOW_REDIRECTS, false)
            .property(ClientProperties.CONNECT_TIMEOUT, configuration.getVarnish().getConnectTimeoutMillis())
            .property(ClientProperties.READ_TIMEOUT, configuration.getVarnish().getReadTimeoutMillis())
            .build();

        final RequestForwarder requestForwarder = new JerseyRequestForwarder(client, configuration.getVarnish());
        return new KnownEndpoint(urlPattern, new HttpPipeline(requestForwarder, filterChain));
    }
}
