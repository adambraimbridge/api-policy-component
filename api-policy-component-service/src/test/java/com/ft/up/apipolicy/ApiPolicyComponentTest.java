package com.ft.up.apipolicy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.filters.AddBrandFilterParameters;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.IOUtils;
import org.fest.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.hasItems;
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
    public static final String CONTENT_PATH_2 = "/content/f3b60ad0-acda-11e2-a7c4-002128161462";
    
    public static final String BASE_NOTIFICATION_PATH = "/content/notifications?since=2014-10-15";
    public static final String FOR_BRAND_FILTER = "&forBrand=";
    public static final String NOT_FOR_BRAND_FILTER = "&notForBrand=";

    public static final String PLAIN_NOTIFICATIONS_FEED_URI = "http://contentapi2.ft.com/content/notifications?since=2014-10-15";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiPolicyComponentTest.class);

    @Rule
    public WireMockRule wireMockForVarnish = new WireMockRule(SOME_PORT);

    @Rule
    public DropwizardAppRule<ApiPolicyConfiguration> policyComponent = new DropwizardAppRule<>(
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

    private static final String NOTIFICATIONS_RESPONSE_TEMPLATE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15%s\" " +
            "}";
    
    private static final String ALL_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "");
    private static final String FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND);
    private static final String NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND);  
    private static final String ALPHAVILLE_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, FOR_BRAND_FILTER + AddBrandFilterParameters.ALPHAVILLE_BRAND);
    private static final String NOT_ALPHAVILLE_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.ALPHAVILLE_BRAND);   
    private static final String FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, 
            FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND + NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND);

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
    public void shouldAllowUnknownRequestsThrough() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

        try {
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), is(EXAMPLE_JSON));

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
    public void shouldTreatMultiplePolicyHeadersTheSame() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND
                + NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND;

        stubForNotifications(url, FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON);

        /*

        Drop to the TCP layer to simulate a strangely formatted HTTP request
        Making sure this works is important for simplifying ApiGee and since socket programming
        is really easy what's the harm?

        ;-)

        */

        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;
        
        try {
            socket = new Socket(facadeUri.getHost(), facadeUri.getPort());

            writer = new PrintWriter( socket.getOutputStream() );
            reader = new BufferedReader( new InputStreamReader( socket.getInputStream() )); // the buffer enables readLine()

        
            writer.println("GET /content/notifications?since=2014-10-15 HTTP/1.1");
            writer.println("Host: " + facadeUri.getAuthority()); // I think we want the port number so "authority" not "host"
            writer.println("X-Policy: FASTFT_CONTENT_ONLY");
            writer.println("X-Policy: EXCLUDE_FASTFT_CONTENT");
            writer.println();

            // SEND the request
            writer.flush();

            String line = reader.readLine();

            // stop at the blank line, so we don't wait on the buffer refilling.
            while(!Strings.isNullOrEmpty(line)) {
                LOGGER.info(line);
                line = reader.readLine();
            }

        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(reader);
            socket.close();
        }

        // after all that, we're only really interested in whether the app called the varnish layer with the same parameters.
        verify(getRequestedFor(urlEqualTo(url)));

    }
    
    @Test
    public void givenNoFastFtRelatedPolicyShouldGetNotificationsWithNoBrandParameter() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH;

        stubForNotifications(url, ALL_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(url)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));

    }

    @Test
    public void givenPolicyFASTFT_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND;

        stubForNotifications(url, FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(url)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));

    }

    @Test
    public void givenPolicyEXCLUDE_FASTFT_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND;

        stubForNotifications(url, NOT_FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(url)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));

    }

    @Test
    public void givenPolicyALPHAVILLE_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND_FILTER + AddBrandFilterParameters.ALPHAVILLE_BRAND;

        stubForNotifications(url, ALPHAVILLE_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "ALPHAVILLE_CONTENT_ONLY")
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(url)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));

    }

    @Test
    public void givenPolicyEXCLUDE_ALPHAVILLE_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.ALPHAVILLE_BRAND;

        stubForNotifications(url, NOT_ALPHAVILLE_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_ALPHAVILLE_CONTENT")
                .get(ClientResponse.class);

        verify(getRequestedFor(urlEqualTo(url)));

        String requestUrl = expectRequestUrl(response);

        assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));

    }    
    
    @Test
    public void givenListedPoliciesFASTFT_CONTENT_ONLYCommaEXCLUDE_FASTFT_CONTENTShouldProcessBothAsNormal() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND
                + NOT_FOR_BRAND_FILTER + AddBrandFilterParameters.FASTFT_BRAND;

        stubForNotifications(url, FASTFT_NOTIFICATIONS_JSON);

        client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY, EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);


        verify(getRequestedFor(urlEqualTo(url)));

    }

    @Test
    public void givenVaryHeaderWithAcceptShouldAddXPolicy() {
        URI uri  = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withStatus(200).withHeader("Vary","Accept")));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

        try {
            assumeThat(response.getStatus(), is(200));
            assertThat(response.getHeaders().get("Vary").size(), is(1));

            List<String> varyHeaderValue = atomise(response.getHeaders().get("Vary"));
            assertThat(varyHeaderValue, hasItems("Accept",HttpPipeline.POLICY_HEADER_NAME));


        } finally {
            response.close();
        }
    }

    private URI sinceSomeDateFromFacade() {
        return fromFacade("/content/notifications")
                .queryParam("since","2014-10-15")
                .build();
    }
    
    private void stubForNotifications(String url, String responseBody) {
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse().withBody(responseBody)));
    }

    private List<String> atomise(List<String> varyHeaderValues) {
        List<String> result = Lists.newArrayList();
        for(String varyHeaderValue : varyHeaderValues) {
            result.addAll(Arrays.asList(varyHeaderValue.split("[ ,]")));
        }

        return result;
    }

    @Test
    public void shouldAddVaryHeaderWithXPolicy() {
        URI uri  = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withStatus(200)));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

        try {
            assumeThat(response.getStatus(), is(200));

            List<String> varyHeaderValue = response.getHeaders().get("Vary");

            assertThat(varyHeaderValue.size(), is(1));
            assertThat(varyHeaderValue, hasItems(HttpPipeline.POLICY_HEADER_NAME));

        } finally {
            response.close();
        }
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
