package com.ft.up.apipolicy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.configuration.ApplicationConfiguration;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
    private static final int SOME_PORT = 44333;
    public static final String CONTENT_PATH = "/content/bcafca32-5bc7-343f-851f-fd6d3514e694";

    @Rule
    public WireMockRule wireMockForVarnish = new WireMockRule(SOME_PORT);

    @Rule
    public DropwizardAppRule<ApplicationConfiguration> ingester = new DropwizardAppRule<>(
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

    private Client client;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        stubFor(WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withBody(EXAMPLE_JSON)));
        stubFor(WireMock.get(urlEqualTo(CONTENT_PATH)).willReturn(aResponse().withBody(CONTENT_JSON)));

        this.client = Client.create();

        objectMapper = new ObjectMapper();

    }

    @Test
    public void assumingWeCanGetAnArbitraryExample() {
        URI uri  = fromWiremock(EXAMPLE_PATH);

        ClientResponse response = client.resource(uri).get(ClientResponse.class);
        try {
            assumeThat(response.getStatus(), is(200));
            assumeThat(response.getEntity(String.class), is(EXAMPLE_JSON));

        } finally {
            response.close();
        }
    }

    @Test
    public void shouldGetTheContentWithExtraWebUrlField() throws IOException {
        URI uri  = fromWiremock(CONTENT_PATH);

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

    private TypeReference<HashMap<String, Object>> jsonMapType() {
        return new TypeReference<HashMap<String,Object>>() {};
    }

    private URI fromWiremock(String path) {
        return UriBuilder.fromPath(path).host("localhost").port(wireMockForVarnish.port()).scheme("http").build();
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
