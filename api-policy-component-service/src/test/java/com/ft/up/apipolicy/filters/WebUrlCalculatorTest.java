package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * WebUrlCalculatorTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class WebUrlCalculatorTest {

    public final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";

    public final static String MINIMAL_EXAMPLE_IDENTIFIER_RESPONSE = "{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}] }";

    public final static String MINIMAL_PARTIAL_EXAMPLE_IDENTIFIER_RESPONSE = "{ \"identifiers\": [{\n" +
            "\"authority\": \"http://api.ft.com/system/FT-LABS-WP-1-91\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}] }";

    public final static String MINIMAL_EXAMPLE_CONTENTORIGIN_RESPONSE = "{ \"contentOrigin\": {\n" +
            "\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"originatingIdentifier\": \"581814c4-748e-11e4-b30b-00144feabdc0\"\n" +
            "}}";

    public final static String NO_IDENTIFIERS_RESPONSE =  "{ \"identifiers\": [] }";
    public final static String NO_CONTENT_ORIGIN_RESPONSE =  "{ \"contentOrigin\": {} }";

    public Map<String,String> FASTFT_TEMPLATE = Collections.singletonMap("http://www.ft.com/ontology/origin/FT-CLAMO","TEST{{originatingIdentifier}}");

    private final static Map<String, String> WEB_URL_TEMPLATES = new HashMap<String, String>();

    @Mock
    private HttpPipelineChain mockChain;


    private MutableRequest exampleRequest = new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

    private MutableResponse exampleErrorResponse;
    private MutableResponse minimalExampleResponse;
    private MutableResponse minimalPartialExampleResponse;
    private MutableResponse originatingSystemIsNullResponse;
    private MutableResponse minimalContentOriginResponse;
    private MutableResponse contentOriginIsNullResponse;

    @Before
    public void setUpExamples() {
        WEB_URL_TEMPLATES.put("http://www.ft.com/ontology/origin/FT-CLAMO", "TEST{{originatingIdentifier}}");
        WEB_URL_TEMPLATES.put("http://www.ft.com/ontology/origin/FT-LABS-WP-1-[0-9]+", "WP{{originatingIdentifier}}");

        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(),ERROR_RESPONSE.getBytes());
        exampleErrorResponse.setStatus(500);

        minimalExampleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_IDENTIFIER_RESPONSE.getBytes());
        minimalExampleResponse.setStatus(200);

        minimalPartialExampleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_PARTIAL_EXAMPLE_IDENTIFIER_RESPONSE.getBytes());
        minimalPartialExampleResponse.setStatus(200);

        originatingSystemIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_IDENTIFIERS_RESPONSE.getBytes());
        originatingSystemIsNullResponse.setStatus(200);

        minimalContentOriginResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_CONTENTORIGIN_RESPONSE.getBytes());
        minimalContentOriginResponse.setStatus(200);

        contentOriginIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_CONTENT_ORIGIN_RESPONSE.getBytes());
        contentOriginIsNullResponse.setStatus(200);

        minimalExampleResponse.getHeaders().putSingle("Content-Type","application/json");
        minimalPartialExampleResponse.getHeaders().putSingle("Content-Type","application/json");
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

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter()) ;

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

    @Test
    public void shouldAddWebUrlToSuccessResponseForRegexMatches(){
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalPartialExampleResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATES, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),not(containsString("\"webUrl\":\"WP219512\"")));
    }
}
