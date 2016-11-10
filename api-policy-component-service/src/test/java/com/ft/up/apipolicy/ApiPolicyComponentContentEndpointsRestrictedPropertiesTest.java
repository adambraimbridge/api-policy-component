package com.ft.up.apipolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.ft.up.apipolicy.JsonConverter.JSON_MAP_TYPE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Parameterized JUnit test for properties that are filtered for the content,
 * preview, and enriched content endpoints.
 */
@RunWith(Parameterized.class)
public class ApiPolicyComponentContentEndpointsRestrictedPropertiesTest extends AbstractApiComponentTest {
    private static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";
    private static final String ENRICHED_CONTENT_PATH = "/enrichedcontent/bcafca32-5bc7-343f-851f-fd6d3514e694";


    @ClassRule
    public static final DropwizardAppRule<ApiPolicyConfiguration> policyComponent = new DropwizardAppRule<>(
            ApiPolicyApplication.class,
            resourceFilePath("config-junit.yml"),
            config("varnish.primaryNodes", primaryNodes)
    );
    @Parameters
    public static Collection<Object[]> data() {
      /* Elements in each array are as follows:
       * - Property name
       * - Required policy
       * - Expect value to be present in /content endpoint when allowed by policy
       * - Expect value to be present in /content-preview endpoint when allowed by policy
       * - Expect value to be present in /enrichedcontent endpoint when allowed by policy
       */
        return Arrays.asList(new Object[][]{
                {"comments", Policy.INCLUDE_COMMENTS, false, true, true},
                {"identifiers", Policy.INCLUDE_IDENTIFIERS, true, true, true},
                {"lastModified", Policy.INCLUDE_LAST_MODIFIED_DATE, true, true, true},
                {"publishReference", Policy.INCLUDE_PROVENANCE, true, true, true},
                {"mainImage", Policy.INCLUDE_RICH_CONTENT, true, true, true},
                {"openingXML", Policy.INTERNAL_UNSTABLE, true, true, true},
                {"alternativeTitles", Policy.INTERNAL_UNSTABLE, true, true, true}
        });
    }

    private static final String CONTENT_PREVIEW_PATH = "/content-preview/bcafca32-5bc7-343f-851f-fd6d3514e694";

    private static final String ARTICLE_JSON =
            "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                    "\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\",\n" +
                    "\"openingXML\" : \"<body>a video</body>\",\n" +
                    "\"alternativeTitles\" : {},\n" +
                    "\"lastModified\": \"2015-12-13T17:04:54.636Z\",\n" +
                    "\"publishReference\": \"tid_junit_publishref\",\n" +
                    "\"identifiers\": [{\n" +
                    "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                    "\"identifierValue\": \"220322\"\n" +
                    "}],\n" +
                    "\"mainImage\": {" +
                    "\"id\": \"http://www.ft.com/content/273563f3-95a0-4f00-8966-6973c0111923\"" +
                    "},\n" +
                    "\"comments\": {\n" +
                    "\"enabled\": true\n" +
                    "}\n";

    private static final String CONTENT_JSON = "{" + ARTICLE_JSON + "}";

    private static final String ENRICHED_CONTENT_JSON =
            "{" +
                    ARTICLE_JSON + ",\n" +
                    "\"brands\": [ ],\n" +
                    "\"annotations\": [ ]" +
                    "}";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Rule
    public final WireMockClassRule wireMockForVarnish = WIRE_MOCK_1;

    private final String propertyName;
    private final Policy requiredPolicy;
    private final boolean allowForContentEndpoint;
    private final boolean allowForContentPreviewEndpoint;
    private final boolean allowForEnrichedContentEndpoint;

    private final Client client = Client.create();
    private ClientResponse response;

    public ApiPolicyComponentContentEndpointsRestrictedPropertiesTest(String propertyName, Policy requiredPolicy,
                                                                      boolean allowForContent, boolean allowForContentPreview, boolean allowForEnrichedContent) {

        this.propertyName = propertyName;
        this.requiredPolicy = requiredPolicy;
        this.allowForContentEndpoint = allowForContent;
        this.allowForContentPreviewEndpoint = allowForContentPreview;
        this.allowForEnrichedContentEndpoint = allowForEnrichedContent;
    }

    @Before
    public void setUp() {
        stubFor(WireMock.get(urlPathEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(SC_OK)));
        stubFor(WireMock.get(urlPathEqualTo(CONTENT_PREVIEW_PATH)).willReturn(aResponse().withBody(CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        stubFor(WireMock.get(urlPathEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        stubFor(WireMock.get(urlPathEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
    }

    @After
    public void tearDown() {
        if (response != null) response.close();
    }

    private UriBuilder fromFacade(String path) {
        return UriBuilder.fromPath(path).host("localhost").port(policyComponent.getLocalPort()).scheme("http");
    }

    private void checkResponse(String path, boolean expectProperty) {
        verify(getRequestedFor(urlPathEqualTo(path)));
        assertThat(response.getStatus(), is(SC_OK));
        if (expectProperty) {
            assertThat(response.getEntity(String.class), containsJsonProperty(propertyName));
        } else {
            assertThat(response.getEntity(String.class), not(containsJsonProperty(propertyName)));
        }
    }

    private Matcher<? super String> containsJsonProperty(final String jsonProperty) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("json property should be present: ").appendValue(jsonProperty);
            }

            @Override
            protected boolean matchesSafely(String jsonPayload) {
                Map<String, Object> jsonMap;
                try {
                    jsonMap = OBJECT_MAPPER.readValue(jsonPayload, JSON_MAP_TYPE);
                } catch (IOException e) {
                    return false;
                }
                return jsonMap.containsKey(jsonProperty);
            }
        };
    }

    @Test
    public void shouldAllowPropertyForContentWhenPolicyIsPresent() throws Exception {
        final URI uri = fromFacade(CONTENT_PATH).build();
        response = client.resource(uri)
                .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
                .get(ClientResponse.class);

        checkResponse(CONTENT_PATH, allowForContentEndpoint);
    }

    @Test
    public void shouldAllowPropertyForContentPreviewWhenPolicyIsPresent() throws Exception {
        final URI uri = fromFacade(CONTENT_PREVIEW_PATH).build();
        response = client.resource(uri)
                .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
                .get(ClientResponse.class);

        checkResponse(CONTENT_PREVIEW_PATH, allowForContentPreviewEndpoint);
    }

    @Test
    public void shouldAllowPropertyForEnrichedContentWhenPolicyIsPresent() throws Exception {
        final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
        response = client.resource(uri)
                .header(HttpPipeline.POLICY_HEADER_NAME, requiredPolicy.getHeaderValue())
                .get(ClientResponse.class);

        checkResponse(ENRICHED_CONTENT_PATH, allowForEnrichedContentEndpoint);
    }

    @Test
    public void shouldRemovePropertyForContentWhenPolicyIsNotPresent() throws Exception {
        final URI uri = fromFacade(CONTENT_PATH).build();
        response = client.resource(uri)
                .get(ClientResponse.class);

        checkResponse(CONTENT_PATH, false);
    }

    @Test
    public void shouldRemovePropertyForContentPreviewWhenPolicyIsNotPresent() throws Exception {
        final URI uri = fromFacade(CONTENT_PREVIEW_PATH).build();
        response = client.resource(uri)
                .get(ClientResponse.class);

        checkResponse(CONTENT_PREVIEW_PATH, false);
    }

    @Test
    public void shouldRemovePropertyForEnrichedContentWhenPolicyIsNotPresent() throws Exception {
        final URI uri = fromFacade(ENRICHED_CONTENT_PATH).build();
        response = client.resource(uri)
                .get(ClientResponse.class);

        checkResponse(ENRICHED_CONTENT_PATH, false);
    }
}
