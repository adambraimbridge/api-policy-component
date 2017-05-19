package com.ft.up.apipolicy.filters;

import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
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
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyndicationDistributionFilterTest {

    private SyndicationDistributionFilter filter;

    @Mock
    private HttpPipelineChain mockChain;

    @Before
    public void setUp() {
        filter = new SyndicationDistributionFilter(JsonConverter.testConverter(), Policy.INTERNAL_UNSTABLE);
    }

    @Test
    public void shouldNotProcessErrorResponse() {
        final MutableRequest request = new MutableRequest(Collections.<String>emptySet(), getClass().getSimpleName());
        final String responseBody = "{\"message\":\"TestError\"}";
        MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes());
        response.setStatus(500);
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes())));
    }

    @Test
    public void shouldNotModifyResponseWhenPolicy() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());

        final String responseBody = "{\"bodyXML\":\"<body>Testing.</body>\",\"canBeDistributed\":\"no\"}";
        final MutableResponse response = createSuccessfulResponse(responseBody);

        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldNotModifyResponseWhenNotPolicyAndMissingCanBeDistributedField() {
        MutableRequest request = new MutableRequest(Collections.<String>emptySet(), getClass().getSimpleName());

        String responseBody = "{\"bodyXML\":\"<body>Testing.</body>\"}";
        MutableResponse response = createSuccessfulResponse(responseBody);

        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldModifyResponseWhenNotPolicyAndCanBeDistributedFieldYes() {
        MutableRequest request = new MutableRequest(Collections.<String>emptySet(), getClass().getSimpleName());
        MutableResponse chainedResponse = createSuccessfulResponse("{\"bodyXML\":\"<body>Testing.</body>\",\"canBeDistributed\":\"yes\"}");

        when(mockChain.callNextFilter(request)).thenReturn(chainedResponse);

        final String expectedFilteredResponseBody = "{\"bodyXML\":\"<body>Testing.</body>\"}";

        MutableResponse actualFilteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(actualFilteredResponse.getEntity()), is(new String(expectedFilteredResponseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldReturnErrorWhenNotPolicyAndCanBeDistributedFieldNotYes() {
        MutableRequest request = new MutableRequest(Collections.<String>emptySet(), getClass().getSimpleName());
        MutableResponse chainedResponse = createSuccessfulResponse("{\"bodyXML\":\"<body>Testing.</body>\",\"canBeDistributed\":\"verify\"}");

        when(mockChain.callNextFilter(request)).thenReturn(chainedResponse);

        try {
            filter.processRequest(request, mockChain);
            fail("No exception was thrown, but expected one.");

        } catch (WebApplicationClientException e) {
            assertThat(e.getResponse().getStatus(), is(403));
        }
    }

    @Test
    public void shouldReturnErrorWhenNotPolicyAndCanBeDistributedFieldNotYesForNestedImageContent() {
        MutableRequest request = new MutableRequest(Collections.<String>emptySet(), getClass().getSimpleName());
        MutableResponse chainedResponse = createSuccessfulResponse("{\"bodyXML\":\"<body>Testing.</body>\",\"canBeDistributed\":\"yes\",\"mainImage\":{\"id\":\"sampleId\",\"canBeDistributed\":\"verify\"}}");

        when(mockChain.callNextFilter(request)).thenReturn(chainedResponse);

        try {
            filter.processRequest(request, mockChain);
            fail("No exception was thrown, but expected one.");

        } catch (WebApplicationClientException e) {
            assertThat(e.getResponse().getStatus(), is(403));
        }
    }

    private MutableResponse createSuccessfulResponse(String body) {
        MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), body.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/json");
        return response;
    }
}
