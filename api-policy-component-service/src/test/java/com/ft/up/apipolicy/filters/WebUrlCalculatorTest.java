package com.ft.up.apipolicy.filters;

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
import sun.invoke.util.VerifyType;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.when;

/**
 * WebUrlCalculatorTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class WebUrlCalculatorTest {

    public final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";

    public final static String MINIMAL_EXAMPLE_RESPONSE = "{ \"contentOrigin\": {\n" +
            "\"originatingSystem\": \"http://www.ft.com/ontology/origin/FT-CLAMO\",\n" +
            "\"originatingIdentifier\": \"219512\"\n" +
            "} }";

    public final static String ORIGINATINGSYSTEM_IS_NULL_RESPONSE = "{ \"contentOrigin\": {\n" +
            "\"originatingIdentifier\": \"219512\"\n" +
            "} }";

    public Map<String,String> FASTFT_TEMPLATE = Collections.singletonMap("http://www.ft.com/ontology/origin/FT-CLAMO","TEST{{originatingIdentifier}}");

    @Mock
    private HttpPipelineChain mockChain;


    private MutableRequest exampleRequest = new MutableRequest(Collections.singleton("TEST"));

    private MutableResponse exampleErrorResponse;
    private MutableResponse minimalExampleResponse;
    private MutableResponse originatingSystemIsNullResponse;

    @Before
    public void setUpExamples() {
        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(),ERROR_RESPONSE.getBytes());
        exampleErrorResponse.setStatus(500);

        minimalExampleResponse = new MutableResponse(new MultivaluedMapImpl(), MINIMAL_EXAMPLE_RESPONSE.getBytes());
        minimalExampleResponse.setStatus(200);

        originatingSystemIsNullResponse = new MutableResponse(new MultivaluedMapImpl(), ORIGINATINGSYSTEM_IS_NULL_RESPONSE.getBytes());
        originatingSystemIsNullResponse.setStatus(200);
    }

    @Test
    public void shouldNotProcessErrorResponse() {


        when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntity(),is(ERROR_RESPONSE.getBytes()));

    }

    @Test
    public void shouldAddWebUrlToSuccessResponse() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),containsString("\"TEST219512\""));
    }

    @Test
    public void shouldReturnSuccessResponseWhenOriginatingSystemIsNull() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(originatingSystemIsNullResponse);

        WebUrlCalculator calculator = new WebUrlCalculator(FASTFT_TEMPLATE, JsonConverter.testConverter());

        MutableResponse response = calculator.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntityAsString(),containsString("\"219512\""));
    }
}
