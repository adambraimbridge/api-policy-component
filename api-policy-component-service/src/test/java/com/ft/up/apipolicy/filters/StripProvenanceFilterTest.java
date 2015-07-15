package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StripProvenanceFilterTest {

    private final static Charset UTF8 = Charset.forName("UTF-8");
    private final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
    private final static byte[] ENTITY_WITH_PUBLISH_REFERENCE = ("{ \"publishReference\": \"reference\" }").getBytes(UTF8);

    private final static byte[] STRIPPED_ENTITY = "{}".getBytes(UTF8);


    @Mock
    private HttpPipelineChain mockChain;

    private ApiFilter filter = new RemoveJsonPropertyUnlessPolicyPresentFilter(JsonConverter.testConverter(),"publishReference",Policy.INCLUDE_PROVENANCE);
    private MutableRequest exampleRequest = new MutableRequest(Collections.<String>emptySet(), getClass().getSimpleName());
    private MutableRequest monitoringRequest = new MutableRequest(
            Collections.singleton(Policy.INCLUDE_PROVENANCE.getHeaderValue()),
            getClass().getSimpleName()
    );
    private MutableResponse exampleErrorResponse;
    private MutableResponse exampleResponse;

    @Before
    public void setUpExamples() {
        exampleErrorResponse = new MutableResponse(new MultivaluedMapImpl(), ERROR_RESPONSE.getBytes());
        exampleErrorResponse.getHeaders().putSingle("Content-Type", "application/json");
        exampleErrorResponse.setStatus(500);


        exampleResponse = new MutableResponse(new MultivaluedMapImpl(), ENTITY_WITH_PUBLISH_REFERENCE);
        exampleResponse.setStatus(200);
        exampleResponse.getHeaders().putSingle("Content-Type", "application/json");


    }

    @Test
    public void shouldNotProcessErrorResponse() {

        MutableResponse response = spy(exampleErrorResponse);

        when(mockChain.callNextFilter(exampleRequest)).thenReturn(response);

        filter.processRequest(exampleRequest, mockChain);

        filter.processRequest(exampleRequest, mockChain);

        verify(response,never()).getEntity();
    }

    @Test
    public void shouldNotProcessJSONLD() {

        MutableResponse response = spy(exampleResponse);

        exampleResponse.getHeaders().putSingle("Content-Type", "application/ld-json");
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(response);

        filter.processRequest(exampleRequest, mockChain);

        verify(response,never()).getEntity();
    }

    @Test
    public void shouldUsuallyRemovePublishReference() {
        when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleResponse);

        MutableResponse response = filter.processRequest(exampleRequest, mockChain);

        assertThat(response.getEntity(), is(STRIPPED_ENTITY));
    }

    @Test
    public void shouldNotRemovePublishReferenceWhenPolicyIsSet() {

        when(mockChain.callNextFilter(monitoringRequest)).thenReturn(exampleResponse);

        MutableResponse response = filter.processRequest(monitoringRequest, mockChain);

        assertThat(response.getEntity(), is(ENTITY_WITH_PUBLISH_REFERENCE));
    }


}
