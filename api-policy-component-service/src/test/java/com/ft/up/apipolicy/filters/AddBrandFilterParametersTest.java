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

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * AddBrandFilterParametersTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class AddBrandFilterParametersTest {


    public final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";

    @Mock
    private HttpPipelineChain mockChain;


    private MutableRequest exampleRequest = new MutableRequest(Collections.singleton("TEST"));

    private MutableResponse exampleErrorResponse;

    @Before
    public void setUpExamples() {
        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(),ERROR_RESPONSE.getBytes());
        exampleErrorResponse.setStatus(500);
    }

    @Test
    public void shouldNotProcessErrorResponse() {


        when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);

        AddBrandFilterParameters filter  = new AddBrandFilterParameters(JsonConverter.testConverter());

        MutableResponse response = filter.processRequest(exampleRequest,mockChain);

        assertThat(response.getEntity(),is(ERROR_RESPONSE.getBytes()));

    }
}
