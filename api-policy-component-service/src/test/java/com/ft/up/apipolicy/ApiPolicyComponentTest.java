package com.ft.up.apipolicy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
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

/**
 * ApiPolicyComponentTest
 *
 * @author Simon.Gibbs
 */
public class ApiPolicyComponentTest {

    public static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String ALPHAVILLE_BRAND = "http://api.ft.com/things/89d15f70-640d-11e4-9803-0800200c9a66";
    public static final String BEYONDBRICS_BRAND = "http://api.ft.com/things/3a37a89e-14ce-4ac8-af12-961a9630dce3";


    public static final String EXAMPLE_PATH = "/example";
    private static final int SOME_PORT = (int)(Math.random() * 10000) + 40000;

    public static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";
    public static final String CONTENT_PATH_2 = "/content/f3b60ad0-acda-11e2-a7c4-002128161462";

    public static final String ENRICHED_CONTENT_PATH = "/enrichedcontent/bcafca32-5bc7-343f-851f-fd6d3514e694";

    public static final String BASE_NOTIFICATION_PATH = "/content/notifications?since=2014-10-15";
    public static final String FOR_BRAND = "&forBrand=";
    public static final String NOT_FOR_BRAND = "&notForBrand=";

    public static final String PLAIN_NOTIFICATIONS_FEED_URI = "http://contentapi2.ft.com/content/notifications?since=2014-10-15";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiPolicyComponentTest.class);

    private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";

    private static final String CONTENT_JSON =
            "{" +
                "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                "\"identifiers\": [{\n" +
                "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                "\"identifierValue\": \"220322\"\n" +
                "}]" +
            "}";

    private static final String ENRICHED_CONTENT_JSON =
            "{" +
                "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                "\"identifiers\": [{\n" +
                "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                "\"identifierValue\": \"220322\"\n" +
                "}]," +
                "\"brands\": [ ],\n" +
                "\"annotations\": [ ]" +
            "}";

	private static final String RICH_CONTENT_JSON = "{" +
				"\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
				"\"bodyXML\" : \"<body>a video: <a href=\\\"https://www.youtube.com/watch?v=dfvLde-FOXw\\\"></a>.</body>\", " +
				"\"contentOrigin\": {\n" +
				"\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
				"\"originatingIdentifier\": \"220322\"\n" +
				"}" +
			"}";

    private static final String CONTENT_WITH_IMAGE_JSON =
            "{" +
                "\"uuid\": \"bcafca32-5bc7-343f-851f-fd6d3514e694\", " +
                "\"identifiers\": [{\n" +
                    "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
                    "\"identifierValue\": \"220322\"\n" +
                "}]," +
                "\"mainImage\": {" +
                    "\"id\": \"http://www.ft.com/content/273563f3-95a0-4f00-8966-6973c0111923\"" +
                "}" +
            "}";

	public static final String RICH_CONTENT_KEY = "INCLUDE_RICH_CONTENT";
    public static final String EXAMPLE_TRANSACTION_ID = "010101";

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




    private static final String NOTIFICATIONS_RESPONSE_TEMPLATE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15%s\" " +
            "}";
    
    private static final String ALL_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, "");
    private static final String FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, FOR_BRAND + FASTFT_BRAND);
    private static final String NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, NOT_FOR_BRAND + FASTFT_BRAND);
    private static final String ALPHAVILLE_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, FOR_BRAND + ALPHAVILLE_BRAND);
    private static final String NOT_ALPHAVILLE_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, NOT_FOR_BRAND + ALPHAVILLE_BRAND);
    private static final String BLOG_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, FOR_BRAND + ALPHAVILLE_BRAND + NOT_FOR_BRAND + BEYONDBRICS_BRAND);
    private static final String NOT_BLOG_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, NOT_FOR_BRAND + ALPHAVILLE_BRAND + NOT_FOR_BRAND + BEYONDBRICS_BRAND);
    private static final String FASTFT_AND_NOT_FASTFT_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, 
            FOR_BRAND + FASTFT_BRAND + NOT_FOR_BRAND + FASTFT_BRAND);
    private static final String FASTFT_AND_ALPHAVILLE_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, 
            FOR_BRAND + FASTFT_BRAND + FOR_BRAND + ALPHAVILLE_BRAND);
    private static final String FASTFT_AND_BLOG_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE,
            FOR_BRAND + FASTFT_BRAND + FOR_BRAND + ALPHAVILLE_BRAND + FOR_BRAND + BEYONDBRICS_BRAND);
    private static final String NOT_FASTFT_AND_NOT_ALPHAVILLE_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE, 
            NOT_FOR_BRAND + FASTFT_BRAND + NOT_FOR_BRAND + ALPHAVILLE_BRAND);
    private static final String NOT_FASTFT_AND_NOT_BLOG_NOTIFICATIONS_JSON = String.format(NOTIFICATIONS_RESPONSE_TEMPLATE,
            NOT_FOR_BRAND + FASTFT_BRAND + NOT_FOR_BRAND + ALPHAVILLE_BRAND + NOT_FOR_BRAND + ALPHAVILLE_BRAND);

    private Client client;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));
        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

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

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardUnknownHeaders() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).header("Arbitrary", "Example").get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader("Arbitrary",equalTo("Example")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldForwardTransactionId() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).header(TransactionIdUtils.TRANSACTION_ID_HEADER, EXAMPLE_TRANSACTION_ID).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader(TransactionIdUtils.TRANSACTION_ID_HEADER,equalTo("010101")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGenerateAndForwardTransactionIdIfMissing() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)).withHeader(TransactionIdUtils.TRANSACTION_ID_HEADER,containing("tid_")));
        } finally {
            response.close();
        }
    }


    @Test
    public void shouldGetTheContentWithExtraWebUrlField() throws IOException {
        URI uri  = fromFacade(CONTENT_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlMatching(CONTENT_PATH)));

            HashMap<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "http://www.ft.com/fastft/220322");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetEnrichedContentWithExtraWebUrlField() throws IOException {

        stubFor(WireMock.get(urlEqualTo(ENRICHED_CONTENT_PATH)).willReturn(aResponse().withBody(ENRICHED_CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        URI uri  = fromFacade(ENRICHED_CONTENT_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlMatching(ENRICHED_CONTENT_PATH)));

            HashMap<String, Object> result = expectOKResponseWithJSON(response);

            assertWebUrl(result, "http://www.ft.com/fastft/220322");

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldTreatMultiplePolicyHeadersTheSame() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + FASTFT_BRAND
                + NOT_FOR_BRAND + FASTFT_BRAND;

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
            IOUtils.closeQuietly(socket);
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
        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenPolicyFASTFT_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + FASTFT_BRAND;

        stubForNotifications(url, FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }
    }

    @Test
    public void givenPolicyEXCLUDE_FASTFT_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND + FASTFT_BRAND;

        stubForNotifications(url, NOT_FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenPolicyBLOG_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + ALPHAVILLE_BRAND + FOR_BRAND + BEYONDBRICS_BRAND;

        stubForNotifications(url, BLOG_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "BLOG_CONTENT_ONLY")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }


    // TODO Will be redundant
    @Test
    public void givenPolicyALPHAVILLE_CONTENT_ONLYShouldGetNotificationsWithForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + ALPHAVILLE_BRAND;

        stubForNotifications(url, ALPHAVILLE_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "ALPHAVILLE_CONTENT_ONLY")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenPolicyEXCLUDE_BLOG_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND + ALPHAVILLE_BRAND + NOT_FOR_BRAND + BEYONDBRICS_BRAND;

        stubForNotifications(url, NOT_BLOG_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_BLOG_CONTENT")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    // TODO Will be redundant
    @Test
    public void givenPolicyEXCLUDE_ALPHAVILLE_CONTENTShouldGetNotificationsWithNotForBrandParameterAndStripItFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND + ALPHAVILLE_BRAND;

        stubForNotifications(url, NOT_ALPHAVILLE_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_ALPHAVILLE_CONTENT")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenListedPoliciesFASTFT_CONTENT_ONLY_And_BLOG_CONTENT_ONLYShouldGetNotificationsWithForBrandParametersAndStripThemFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + FASTFT_BRAND + FOR_BRAND + ALPHAVILLE_BRAND + FOR_BRAND + BEYONDBRICS_BRAND;

        stubForNotifications(url, FASTFT_AND_BLOG_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .header(HttpPipeline.POLICY_HEADER_NAME, "BLOG_CONTENT_ONLY")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    // TODO Will be redundant
    @Test
    public void givenListedPoliciesFASTFT_CONTENT_ONLY_And_ALPHAVILLE_CONTENT_ONLYShouldGetNotificationsWithForBrandParametersAndStripThemFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + FASTFT_BRAND + FOR_BRAND + ALPHAVILLE_BRAND;

        stubForNotifications(url, FASTFT_AND_ALPHAVILLE_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY")
                .header(HttpPipeline.POLICY_HEADER_NAME, "ALPHAVILLE_CONTENT_ONLY")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenListedPoliciesEXCLUDE_FASTFT_CONTENT_And_EXCLUDE_BLOG_CONTENTShouldGetNotificationsWithForBrandParametersAndStripThemFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();

        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND + FASTFT_BRAND + NOT_FOR_BRAND + ALPHAVILLE_BRAND + NOT_FOR_BRAND + BEYONDBRICS_BRAND;

        stubForNotifications(url, NOT_FASTFT_AND_NOT_BLOG_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_BLOG_CONTENT")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    // TODO Will be redundant
    @Test
    public void givenListedPoliciesEXCLUDE_FASTFT_CONTENT_And_EXCLUDE_ALPHAVILLE_CONTENTShouldGetNotificationsWithForBrandParametersAndStripThemFromResponseRequestUrl() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + NOT_FOR_BRAND + FASTFT_BRAND + NOT_FOR_BRAND + ALPHAVILLE_BRAND;

        stubForNotifications(url, NOT_FASTFT_AND_NOT_ALPHAVILLE_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_FASTFT_CONTENT")
                .header(HttpPipeline.POLICY_HEADER_NAME, "EXCLUDE_ALPHAVILLE_CONTENT")
                .get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }  
    
    @Test
    public void givenListedPoliciesFASTFT_CONTENT_ONLYCommaEXCLUDE_FASTFT_CONTENTShouldProcessBothAsNormal() throws IOException {
        // build a URL on localhost corresponding to PLAIN_NOTIFICATIONS_FEED_URI
        URI facadeUri  = sinceSomeDateFromFacade();
        
        String url = BASE_NOTIFICATION_PATH + FOR_BRAND + FASTFT_BRAND
                + NOT_FOR_BRAND + FASTFT_BRAND;

        stubForNotifications(url, FASTFT_NOTIFICATIONS_JSON);

        ClientResponse response = client.resource(facadeUri)
                .header(HttpPipeline.POLICY_HEADER_NAME, "FASTFT_CONTENT_ONLY, EXCLUDE_FASTFT_CONTENT")
                .get(ClientResponse.class);


        try {
            verify(getRequestedFor(urlEqualTo(url)));

            String requestUrl = expectRequestUrl(response);

            assertThat(requestUrl,is(PLAIN_NOTIFICATIONS_FEED_URI));
        } finally {
            response.close();
        }

    }

    @Test
    public void givenVaryHeaderWithAcceptShouldAddXPolicy() {
        URI uri  = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withHeader("Vary","Accept")));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));
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

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(aResponse().withBody(CONTENT_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200)));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            List<String> varyHeaderValue = response.getHeaders().get("Vary");

            assertThat(varyHeaderValue.size(), is(1));
            assertThat(varyHeaderValue, hasItems(HttpPipeline.POLICY_HEADER_NAME));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldPassDownArbitraryResponseHeadersUnlessBlackListed() {

        URI uri = fromFacade(CONTENT_PATH_2).build();

        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(
                aResponse()
                    .withBody(CONTENT_JSON)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(200)
                    .withHeader("X-Example", "100")
                    .withHeader("Accept-Encoding", "test") // out of place for a response, but this is a test
                ));

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

            assertThat(response.getStatus(), is(200));

            assertThat(response.getHeaders().getFirst("X-Example"), is("100"));
            assertThat(response.getHeaders().getFirst("Accept-Encoding"),nullValue());
        } finally {
            response.close();
        }
    }


	@Test
	public void givenRICH_CONTENTIsOnIShouldReceiveRichContent() {
		URI uri = fromFacade(CONTENT_PATH_2).build();

		stubForRichContentWithYouTubeVideo();

		ClientResponse response = client.resource(uri)
			.header(HttpPipeline.POLICY_HEADER_NAME, RICH_CONTENT_KEY)
			.get(ClientResponse.class);

		try {
			verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

			assertThat(response.getStatus(), is(200));

			String json = response.getEntity(String.class);

			assertThat(json,containsString("youtube.com"));


		} finally {
			response.close();
		}
	}


	@Test
	public void givenRICH_CONTENTIsOffIShouldNotReceiveRichContent() {
		URI uri = fromFacade(CONTENT_PATH_2).build();

		stubForRichContentWithYouTubeVideo();

		ClientResponse response = client.resource(uri).get(ClientResponse.class);

		try {
			verify(getRequestedFor(urlMatching(CONTENT_PATH_2)));

			assertThat(response.getStatus(), is(200));

			String json = response.getEntity(String.class);

			assertThat(json,not(containsString("youtube.com")));


		} finally {
			response.close();
		}
	}

    @Test
    public void shouldRemoveMainImageFromJson() {
        final URI uri = fromFacade(CONTENT_PATH).build();
        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_WITH_IMAGE_JSON)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlMatching(CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), not(containsString("mainImage")));
        } finally {
            response.close();
        }
    }

    @Test
    public void shouldLeaveMainImageInJsonWhenPolicyIncludeRichContent() {
        final URI uri = fromFacade(CONTENT_PATH).build();
        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_WITH_IMAGE_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withStatus(200)));
        final ClientResponse response = client.resource(uri)
                .header(HttpPipeline.POLICY_HEADER_NAME, RICH_CONTENT_KEY)
                .get(ClientResponse.class);
        try {
            verify(getRequestedFor(urlMatching(CONTENT_PATH)));
            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), containsString("mainImage"));
        } finally {
            response.close();
        }
    }

	private void stubForRichContentWithYouTubeVideo() {
		stubFor(WireMock.get(urlEqualTo(CONTENT_PATH_2)).willReturn(
				aResponse()
						.withBody(RICH_CONTENT_JSON)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON)
						.withStatus(200)
		));
	}

    private String expectRequestUrl(ClientResponse response) throws IOException {
        HashMap<String, Object> result = expectOKResponseWithJSON(response);

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

    private void assertWebUrl(HashMap<String, Object> result, String webUrl) {
        assertThat((String)result.get("webUrl"),is(webUrl));
    }

    private HashMap<String, Object> expectOKResponseWithJSON(ClientResponse response) throws IOException {
        assertThat(response.getStatus(), is(200));
        String bodyString = response.getEntity(String.class);

        return objectMapper.readValue(bodyString, jsonMapType());
    }

}
