package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
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

@RunWith(MockitoJUnitRunner.class)
public class WebUrlCalculatorTest {

    private final static Charset UTF8 = Charset.forName("UTF-8");
    private final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
    private final static byte[] WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}] }").getBytes(UTF8);
    private final static byte[] WEB_URL_ELIGIBLE_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}],\n" +
            "\"bodyXML\": \"<body>something here</body>\" }").getBytes(UTF8);
    private final static byte[] WEB_URL_NON_ELIGIBLE_CONTENT_ORIGIN_RESPONSE = ("{ \"contentOrigin\": {\n" +
            "\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"originatingIdentifier\": \"581814c4-748e-11e4-b30b-00144feabdc0\"\n" +
            "}}").getBytes(UTF8);
    private final static byte[] WEB_URL_ELIGIBLE_CONTENT_ORIGIN_RESPONSE = ("{ \"contentOrigin\": {\n" +
            "\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"originatingIdentifier\": \"581814c4-748e-11e4-b30b-00144feabdc0\"\n" +
            "},\n" +
            "\"bodyXML\": \"<body>something here</body>\"}").getBytes(UTF8);
    private final static String MINIMAL_PARTIAL_EXAMPLE_IDENTIFIER_RESPONSE = "{ \"identifiers\": [{\n" +
            "\"authority\": \"http://api.ft.com/system/FT-LABS-WP-1-91\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}] }";
    private final static String NO_IDENTIFIERS_RESPONSE = "{ \"identifiers\": [] }";
    private final static String NO_CONTENT_ORIGIN_RESPONSE = "{ \"contentOrigin\": {} }";
    private final static Map<String, String> WEB_URL_TEMPLATES = new HashMap();


    @Mock
    private HttpPipelineChain mockChain;

    private WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATES, JsonConverter.testConverter());
    private MutableRequest exampleRequest = new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

    private MutableResponse exampleErrorResponse;
    private MutableResponse webUrlNonEligibleIdentifierResponse;
    private MutableResponse webUrlEligibleIdentifierResponse;
    private MutableResponse webUrlNonEligibleContentOriginResponse;
    private MutableResponse webUrlEligibleContentOriginResponse;
    private MutableResponse originatingSystemIsNullResponse;
    private MutableResponse minimalPartialExampleResponse;
    private MutableResponse contentOriginIsNullResponse;

    @Before
    public void setUpExamples() {
        WEB_URL_TEMPLATES.put("http://www.ft.com/ontology/origin/FT-CLAMO", "TEST{{originatingIdentifier}}");
        WEB_URL_TEMPLATES.put("http://www.ft.com/ontology/origin/FT-LABS-WP-1-[0-9]+", "WP{{originatingIdentifier}}");

        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(), ERROR_RESPONSE.getBytes());
        exampleErrorResponse.setStatus(500);

        webUrlNonEligibleIdentifierResponse = new MutableResponse(new MultivaluedMapImpl(), WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE);
        webUrlNonEligibleIdentifierResponse.setStatus(200);
        webUrlNonEligibleIdentifierResponse.getHeaders().putSingle("Content-Type", "application/json");

        webUrlNonEligibleContentOriginResponse = new MutableResponse(new MultivaluedMapImpl(), WEB_URL_NON_ELIGIBLE_CONTENT_ORIGIN_RESPONSE);
        webUrlNonEligibleContentOriginResponse.setStatus(200);
        webUrlNonEligibleContentOriginResponse.getHeaders().putSingle("Content-Type", "application/json");

        minimalPartialExampleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_PARTIAL_EXAMPLE_IDENTIFIER_RESPONSE.getBytes());
        minimalPartialExampleResponse.setStatus(200);
        minimalPartialExampleResponse.getHeaders().putSingle("Content-Type", "application/json");

        webUrlEligibleIdentifierResponse = new MutableResponse(new MultivaluedMapImpl(), WEB_URL_ELIGIBLE_IDENTIFIER_RESPONSE);
        webUrlEligibleIdentifierResponse.setStatus(200);
        webUrlEligibleIdentifierResponse.getHeaders().putSingle("Content-Type", "application/json");

        webUrlEligibleContentOriginResponse = new MutableResponse(new MultivaluedMapImpl(), WEB_URL_ELIGIBLE_CONTENT_ORIGIN_RESPONSE);
        webUrlEligibleContentOriginResponse.setStatus(200);
        webUrlEligibleContentOriginResponse.getHeaders().putSingle("Content-Type", "application/json");

        originatingSystemIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_IDENTIFIERS_RESPONSE.getBytes());
        originatingSystemIsNullResponse.setStatus(200);
        originatingSystemIsNullResponse.getHeaders().putSingle("Content-Type", "application/json");

        contentOriginIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_CONTENT_ORIGIN_RESPONSE.getBytes());
        contentOriginIsNullResponse.setStatus(200);

        minimalPartialExampleResponse.getHeaders().putSingle("Content-Type","application/json");
        contentOriginIsNullResponse.getHeaders().putSingle("Content-Type", "application/json");
    }
    @Test
    public void shouldNotProcessErrorResponse() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(ERROR_RESPONSE.getBytes()));
    }

    @Test
    public void shouldNotProcessJSONLD() {
        webUrlNonEligibleIdentifierResponse.getHeaders().putSingle("Content-Type", "application/ld-json");
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlNonEligibleIdentifierResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE));
    }

    @Test
    public void shouldNotAddWebUrlToSuccessResponseForNonEligibleWithIdentifiers() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlNonEligibleIdentifierResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(WEB_URL_NON_ELIGIBLE_IDENTIFIER_RESPONSE));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponseForEligibleWithIdentifiers() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlEligibleIdentifierResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), containsString("\"webUrl\":\"TEST219512\""));
    }

    @Test
    public void shouldNotAddWebUrlToSuccessResponseForNonEligibleWithContentOrigin() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlNonEligibleContentOriginResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(WEB_URL_NON_ELIGIBLE_CONTENT_ORIGIN_RESPONSE));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponseForEligibleWithContentOrigin() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(webUrlEligibleContentOriginResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), containsString("\"webUrl\":\"TEST581814c4-748e-11e4-b30b-00144feabdc0\""));
    }

    @Test
    public void shouldReturnSuccessResponseWithoutWebUrlWhenOriginatingSystemIsNull() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(originatingSystemIsNullResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), not(containsString("\"webUrl\":")));

    }

    @Test
    public void shouldReturnSuccessResponseWithoutWebUrlWhenContentOriginIsNull() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(contentOriginIsNullResponse);

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), not(containsString("\"webUrl\":")));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponseForRegexMatches(){
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalPartialExampleResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATES, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),not(containsString("\"webUrl\":\"WP219512\"")));
    }
}
