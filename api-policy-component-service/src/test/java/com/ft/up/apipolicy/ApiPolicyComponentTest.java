package com.ft.up.apipolicy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.configuration.ApplicationConfiguration;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * ApiPolicyComponentTest
 *
 * @author Simon.Gibbs
 */
public class ApiPolicyComponentTest {

    public static final String EXAMPLE_PATH = "/example";
    private static final int SOME_PORT = (int)(Math.random() * 10000) + 40000;

    public static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";
    public static final String FILTERED_FASTFT_ONLY_NOTIFICATION_FEED = "/content/notifications?since=2014-10-15&forBrand=http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String FILTERED_NO_FASTFT_NOTIFICATION_FEED = "/content/notifications?since=2014-10-15&notForBrand=http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String PLAIN_NOTIFICATIONS_FEED_URI = "http://contentapi2.ft.com/content/notifications?since=2014-10-15";

    @Rule
    public WireMockRule wireMockForVarnish = new WireMockRule(SOME_PORT);

    @Rule
    public DropwizardAppRule<ApplicationConfiguration> policyComponent = new DropwizardAppRule<>(
            ApiPolicyApplication.class,
            resourceFilePath("config-junit.yml"),
            config("varnish.primaryNodes",
                    String.format("localhost:%d:%d", SOME_PORT, SOME_PORT +1 )
            )
    );


    private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";

    private static final String CONTENT_JSON =
            "{" +
                "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                "\"contentOrigin\": {\n" +
                    "\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                    "\"originatingIdentifier\": \"220322\"\n" +
                "}" +
            "}";

    private static final String FASTFT_NOTIFICATIONS_JSON =
            "{" +
                "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15&forBrand=http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54\" " +
            "}";

    private static final String NOT_FASTFT_NOTIFICATIONS_JSON =
            "{" +
                    "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15&notForBrand=http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54\" " +
                    "}";

    private Client client;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON).withStatus(200)));
        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON).withStatus(200)));

        this.client = Client.create();

        objectMapper = new ObjectMapper();

    }

    @After
    public void tearDown() {
        WireMock.reset();
    }

    @Test
    public void assumingWeCanGetAnArbitraryExample() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

        try {
            assumeThat(response.getStatus(), is(200));
            assumeThat(response.getEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetTheContentWithExtraWebUrlField() throws IOException {
        URI uri  = fromFacade(CONTENT_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        verify(getRequestedFor(urlMatching(CONTENT_PATH)));

        try {
            assertThat(response.getStatus(), is(200));
            String bodyString = response.getEntity(String.class);

            HashMap<String,Object> result = objectMapper.readValue(bodyString, jsonMapType());

            assertThat((String)result.get("webUrl"),is("http://www.ft.com/fastft/220322"));

        } finally {
            response.close();
        }
    }

    @Test
    public void givenPolicyFASTFT_CONTENT_ONLYShouldGetNotificationsWithExtraParameterAndStripItFromBody() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI apiGeeUri  = fromFacade("/content/notifications")
                .queryParam("since","2014-10-15")
                .build();

        stubFor(get(urlEqualTo(FILTERED_FASTFT_ONLY_NOTIFICATION_FEED))
                .willReturn(aResponse().withBody(FASTFT_NOTIFICATIONS_JSON)));

        ClientResponse response = client.resource(apiGeeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(FILTERED_FASTFT_ONLY_NOTIFICATION_FEED)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));


    }

    @Test
    public void givenPolicyEXCLUDE_FASTFT_CONTENTShouldGetNotificationsWithExtraParameterAndStripItFromBody() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI apiGeeUri  = fromFacade("/content/notifications")
                .queryParam("since","2014-10-15")
                .build();

        stubFor(get(urlEqualTo(FILTERED_NO_FASTFT_NOTIFICATION_FEED))
                .willReturn(aResponse().withBody(NOT_FASTFT_NOTIFICATIONS_JSON)));

        ClientResponse response = client.resource(apiGeeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(FILTERED_NO_FASTFT_NOTIFICATION_FEED)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));


    }

    private String expectRequestUrl(ClientResponse response) throws IOException {
        assertThat(response.getStatus(), is(200));
        String bodyString = response.getEntity(String.class);

        HashMap<String,Object> result = objectMapper.readValue(bodyString, jsonMapType());

        return (String) result.get("requestUrl");
    }


    private TypeReference<HashMap<String, Object>> jsonMapType() {
        return new TypeReference<HashMap<String,Object>>() {};
    }

    private UriBuilder fromFacade(String path) {
        return UriBuilder.fromPath(path).host("localhost").port(policyComponent.getLocalPort()).scheme("http");
    }


    public static String resourceFilePath(String resourceClassPathLocation) {

        File file = null;

        try {

            file = new File(Resources.getResource(resourceClassPathLocation).toURI());
            return file.getAbsolutePath();

        } catch (Exception e) {
            if(file!=null) {
                throw new RuntimeException(file.toString(), e);
            }
            throw new RuntimeException(e);
        }
    }


}
