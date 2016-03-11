package com.ft.up.apipolicy;

import com.fasterxml.jackson.databind.JsonNode;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.junit.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * ApiPolicyComponentUnhappyPathsTest
 *
 * @author sarah.wells
 */
public class ApiPolicyComponentUnhappyPathsTest {

    public static final String EXAMPLE_PATH = "/example";
    private static final int SOME_PORT = (int)(Math.random() * 10000) + 40000;

    private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";
    private static final String ERROR_JSON = "{\"message\":\"Something went wrong\"}";
    private static final String SERVER_ERROR_JSON = "{\"message\":\"server error\"}";
    private static final String SOCKET_TIMEOUT_JSON = "{\"message\":\"java.net.SocketTimeoutException: Read timed out\"}";
    private static final String UNSUPPORTED_REQUEST_EXCEPTION_JSON = "{\"message\":\"Unsupported request: path [/example] with method [POST].\"}";

    @Rule
    public WireMockRule wireMockForVarnish1 = new WireMockRule(SOME_PORT);
    @Rule
    public WireMockRule wireMockForVarnish2 = new WireMockRule(SOME_PORT + 2);

    @Rule
    public DropwizardAppRule<ApiPolicyConfiguration> policyComponent = new DropwizardAppRule<>(
            ApiPolicyApplication.class,
            resourceFilePath("config-junit.yml"),
            config("varnish.primaryNodes",
                    String.format("localhost:%d:%d, localhost:%d:%d", 
                            SOME_PORT, SOME_PORT +1, 
                            SOME_PORT + 2, SOME_PORT + 3 )
            )
    );

    private Client client;
    
    private int leasedConnectionsBeforeForContent = 0;
    private int leasedConnectionsBeforeForNotifications = 0;
    private int leasedConnectionsBeforeForEnrichedContent = 0;
    private int leasedConnectionsBeforeForOther = 0;

    @Before
    public void setup() {
        this.client = Client.create();
        leasedConnectionsBeforeForContent = getLeasedConnections("content");
        leasedConnectionsBeforeForNotifications = getLeasedConnections("notifications");
        leasedConnectionsBeforeForEnrichedContent = getLeasedConnections("enrichedcontent");
        leasedConnectionsBeforeForOther = getLeasedConnections("other");
    }

    @After
    public void checkThatNumberOfLeasedConnectionsHaveNotChanged() {
        try {
            assertThat(leasedConnectionsBeforeForContent, equalTo(getLeasedConnections("content")));
            assertThat(leasedConnectionsBeforeForNotifications, equalTo(getLeasedConnections("notifications")));
            assertThat(leasedConnectionsBeforeForEnrichedContent, equalTo(getLeasedConnections("enrichedcontent")));
            assertThat(leasedConnectionsBeforeForOther, equalTo(getLeasedConnections("other")));
        }
        finally {
            WireMock.reset();
        }
    }
    
   
    @Test
    public void shouldNotAllowNonWhitelistedPostRequestsThrough() {
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, "{}");

        try {
            verify(0, postRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(405));
            assertThat(response.getEntity(String.class), is(UNSUPPORTED_REQUEST_EXCEPTION_JSON));

        } finally {
            response.close();
        }
    }

    // Fails completely    
    @Test
    public void shouldFailWhereBothNodesAreReturning500() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(500)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(SERVER_ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(500)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(500));//TODO should map downstream errors to a 503
            assertThat(response.getEntity(String.class), is(SERVER_ERROR_JSON));

        } finally {
            response.close();
        }
    }
    
    @Test
    public void shouldFailWhereBothNodesAreReturning503() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(503)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(SERVER_ERROR_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(503)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(503));
            assertThat(response.getEntity(String.class), is(SERVER_ERROR_JSON));

        } finally {
            response.close();
        }
    }
    
    @Test
    public void shouldFailWhereBothNodesAreReturning503WithoutABody() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse()
                .withStatus(503)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse()
                .withStatus(503)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(503));
            assertThat(response.getEntity(String.class), is(SERVER_ERROR_JSON));

        } finally {
            response.close();
        }
    }
    

    
    @Test
    public void shouldReturnErrorIfTimeoutOccurs() {
        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withFixedDelay(2000)));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON).withStatus(200).withFixedDelay(2000)));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(504));
            assertThat(response.getEntity(String.class), is(SOCKET_TIMEOUT_JSON));

        } finally {
            response.close();
        }
    }
    
    // First request fails, second request is OK
    @Test
    public void shouldReturnSuccessFromSecondNodeWhereRecoverableErrorOccursForFirstNode() {

        wireMockForVarnish1.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH))
                .willReturn(
                        aResponse()
                        .withBody(ERROR_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(500)
                ));
        
        wireMockForVarnish2.stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH))
                .willReturn(
                        aResponse()
                        .withBody(EXAMPLE_JSON).withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withStatus(200)
                ));
        
        URI uri  = fromFacade(EXAMPLE_PATH).build();

        ClientResponse response = client.resource(uri).get(ClientResponse.class);

        try {
            wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));
            wireMockForVarnish2.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
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


    private int getLeasedConnections(String name){
        return client.resource("http://localhost:" + 21082).path("/metrics") //hardcoded because we have no access to getAdminPort() on the app rule
                .get(JsonNode.class)
                .get("gauges")
                .get("org.apache.http.conn.ClientConnectionManager." + name + ".leased-connections")
                        .get("value").asInt();

    }
}
