package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang.mutable.Mutable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebUrlCalculatorTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class WebUrlCalculatorTest {

    private final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
    private final static byte[] EXAMPLE_PARTIAL_MATCH_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-LABS-WP-1-91\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}],\n" +
            "\"type\": \"http://www.ft.com/ontology/content/Article\" }").getBytes();
    private final static byte[] MINIMAL_EXAMPLE_NON_ARTICLE_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
    public final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";

    public final static String MINIMAL_EXAMPLE_IDENTIFIER_RESPONSE = "{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}] }").getBytes();
    private final static byte[] MINIMAL_EXAMPLE_ARTICLE_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}],\n" +
            "\"type\": \"http://www.ft.com/ontology/content/Article\" }").getBytes();
    private final static String NO_IDENTIFIERS_RESPONSE = "{ \"identifiers\": [] }";
    private final static String NO_CONTENT_ORIGIN_RESPONSE = "{ \"contentOrigin\": {} }";
    private final static Map<String, String> WEB_URL_TEMPLATES = new HashMap<String, String>();
            "}] }";

    public final static String MINIMAL_EXAMPLE_CONTENTORIGIN_RESPONSE = "{ \"contentOrigin\": {\n" +
            "\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"originatingIdentifier\": \"581814c4-748e-11e4-b30b-00144feabdc0\"\n" +
            "}}";

    public final static String NO_IDENTIFIERS_RESPONSE =  "{ \"identifiers\": [] }";
    public final static String NO_CONTENT_ORIGIN_RESPONSE =  "{ \"contentOrigin\": {} }";

    public Map<String,String> FASTFT_TEMPLATE = Collections.singletonMap("http://www.ft.com/ontology/origin/FT-CLAMO","TEST{{originatingIdentifier}}");

    @Mock
    private HttpPipelineChain mockChain;


    private MutableRequest exampleRequest = new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

    private MutableResponse exampleErrorResponse;
    private MutableResponse minimalExampleResponse;
    private MutableResponse originatingSystemIsNullResponse;
    private MutableResponse minimalContentOriginResponse;
    private MutableResponse contentOriginIsNullResponse;

    @Before
    public void setUpExamples() {
        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(),ERROR_RESPONSE.getBytes());
        exampleErrorResponse.setStatus(500);

        minimalExampleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_IDENTIFIER_RESPONSE.getBytes());
        minimalExampleResponse.setStatus(200);

        originatingSystemIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_IDENTIFIERS_RESPONSE.getBytes());
        originatingSystemIsNullResponse.setStatus(200);

        minimalContentOriginResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_CONTENTORIGIN_RESPONSE.getBytes());
        minimalContentOriginResponse.setStatus(200);

        contentOriginIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_CONTENT_ORIGIN_RESPONSE.getBytes());
        contentOriginIsNullResponse.setStatus(200);

        minimalExampleResponse.getHeaders().putSingle("Content-Type","application/json");
        originatingSystemIsNullResponse.getHeaders().putSingle("Content-Type","application/json");
        minimalContentOriginResponse.getHeaders().putSingle("Content-Type","application/json");

        contentOriginIsNullResponse.getHeaders().putSingle("Content-Type","application/json");
    }

    @Test
    public void shouldNotProcessErrorResponse() {

        when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntity(), is(ERROR_RESPONSE.getBytes()));

    }

    @Test
    public void shouldNotProcessJSONLD() {

        minimalExampleResponse.getHeaders().putSingle("Content-Type", "application/ld-json");
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntity(),is(MINIMAL_EXAMPLE_IDENTIFIER_RESPONSE.getBytes()));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponse() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),containsString("\"webUrl\":\"TEST219512\""));
    }

    @Test
    public void shouldReturnSuccessResponseWithoutWebUrlWhenOriginatingSystemIsNull() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(originatingSystemIsNullResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),not(containsString("\"webUrl\":")));

    }

    @Test
    public void shouldAddWebUrlToSuccessfulResponseWithContentOrigin(){
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalContentOriginResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),containsString("\"webUrl\":\"TEST581814c4-748e-11e4-b30b-00144feabdc0\""));
    }

    @Test
    public void shouldReturnSuccessResponseWithoutWebUrlWhenContentOriginIsNull(){
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(contentOriginIsNullResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),not(containsString("\"webUrl\":")));
    }
}
