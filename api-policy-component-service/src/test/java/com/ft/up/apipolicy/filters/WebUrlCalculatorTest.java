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

@RunWith(MockitoJUnitRunner.class)
public class WebUrlCalculatorTest {

    private final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
    private final static byte[] EXAMPLE_PARTIAL_MATCH_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
            "\"authority\": \"http://www.ft.com/ontology/origin/FT-LABS-WP-1-91\",\n" +
            "\"identifierValue\": \"219512\"\n" +
            "}],\n" +
            "\"type\": \"http://www.ft.com/ontology/content/Article\" }").getBytes();
    private final static byte[] MINIMAL_EXAMPLE_NON_ARTICLE_IDENTIFIER_RESPONSE = ("{ \"identifiers\": [{\n" +
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
    private final static Map<String, String> WEB_URL_TEMPLATE = new HashMap<String, String>();

    @Mock
    private HttpPipelineChain mockChain;

    private MutableRequest exampleRequest = new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

    private MutableResponse exampleErrorResponse;
    private MutableResponse minimalExampleNonArticleResponse;
    private MutableResponse minimalExampleArticleResponse;
    private MutableResponse partialExampleNonArticleResponse;
    private MutableResponse originatingSystemIsNullResponse;
    private MutableResponse contentOriginIsNullResponse;

    @Before
    public void setUpExamples() {
        WEB_URL_TEMPLATE.put("http://www.ft.com/ontology/origin/FT-CLAMO", "TEST{{originatingIdentifier}}");
        WEB_URL_TEMPLATE.put("http://www.ft.com/ontology/origin/FT-LABS-WP-1-[0-9]+", "WP{{originatingIdentifier}}");

        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(), ERROR_RESPONSE.getBytes());
        exampleErrorResponse.setStatus(500);

        minimalExampleNonArticleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_NON_ARTICLE_IDENTIFIER_RESPONSE);
        minimalExampleNonArticleResponse.setStatus(200);
        minimalExampleNonArticleResponse.getHeaders().putSingle("Content-Type", "application/json");

        minimalExampleArticleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_ARTICLE_IDENTIFIER_RESPONSE);
        minimalExampleArticleResponse.setStatus(200);
        minimalExampleArticleResponse.getHeaders().putSingle("Content-Type", "application/json");

        originatingSystemIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_IDENTIFIERS_RESPONSE.getBytes());
        originatingSystemIsNullResponse.setStatus(200);
        originatingSystemIsNullResponse.getHeaders().putSingle("Content-Type", "application/json");

        contentOriginIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), NO_CONTENT_ORIGIN_RESPONSE.getBytes());
        contentOriginIsNullResponse.setStatus(200);
        contentOriginIsNullResponse.getHeaders().putSingle("Content-Type", "application/json");

        partialExampleNonArticleResponse = new MutableResponse(new MultivaluedMapImpl(), EXAMPLE_PARTIAL_MATCH_IDENTIFIER_RESPONSE);
        partialExampleNonArticleResponse.setStatus(200);
        partialExampleNonArticleResponse.getHeaders().putSingle("Content-Type", "application/json");
    }

    @Test
    public void shouldNotProcessErrorResponse() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(ERROR_RESPONSE.getBytes()));
    }

    @Test
    public void shouldNotProcessJSONLD() {
        minimalExampleNonArticleResponse.getHeaders().putSingle("Content-Type", "application/ld-json");
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleNonArticleResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(MINIMAL_EXAMPLE_NON_ARTICLE_IDENTIFIER_RESPONSE));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponseForNonArticles() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleNonArticleResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(MINIMAL_EXAMPLE_NON_ARTICLE_IDENTIFIER_RESPONSE));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponseForArticles() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleArticleResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), containsString("\"webUrl\":\"TEST219512\""));
    }

    @Test
    public void shouldAddWebUrlToSuccessResponseForRegexMatches() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(partialExampleNonArticleResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), containsString("\"webUrl\":\"WP219512\""));
    }



    @Test
    public void shouldReturnSuccessResponseWithoutWebUrlWhenOriginatingSystemIsNull() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(originatingSystemIsNullResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), not(containsString("\"webUrl\":")));

    }

    @Test
    public void shouldReturnSuccessResponseWithoutWebUrlWhenContentOriginIsNull() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(contentOriginIsNullResponse);
        WebUrlCalculator calculator = new WebUrlCalculator(WEB_URL_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntityAsString(), not(containsString("\"webUrl\":")));
    }
}
