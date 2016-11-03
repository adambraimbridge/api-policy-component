package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddSyndicationTest {

    private AddSyndication filter = new AddSyndication(JsonConverter.testConverter(), Policy.INTERNAL_UNSTABLE);
    private final HttpPipelineChain mockChain = mock(HttpPipelineChain.class);

    @Test
    public void shouldNotProcessErrorResponse() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{ \"message\" : \"TestError\" }";
        MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes());
        response.setStatus(500);
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes())));
    }

    @Test
    public void shouldNotProcessNotJSON() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{ \"bodyXML\": \"<body>Testing.</body>\" }";
        final MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/ld-json");
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldHaveOriginalFieldIfPresentAndPolicy() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{ \"bodyXML\": \"<body>Testing.</body>\", \"canBeSyndicated\": \"yes\" }";
        final MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldHideOriginalFieldIfPresentButNotPolicy() {
        final MutableRequest request = new MutableRequest(new HashSet<String>(), getClass().getSimpleName());
        final String responseBody = "{ \"bodyXML\": \"<body>Testing.</body>\", \"canBeSyndicated\": \"yes\" }";
        final String filteredResponseBody = "{\"bodyXML\":\"<body>Testing.</body>\"}";
        final MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(filteredResponseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldHaveFieldAddedIfMissingAndPolicy() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{ \"bodyXML\": \"<body>Testing.</body>\" }";
        final String filteredResponseBody = "{\"bodyXML\":\"<body>Testing.</body>\",\"canBeSyndicated\":\"verify\"}";
        final MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(filteredResponseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldNotIncludeIfMissingAndNotPolicy() {
        final MutableRequest request = new MutableRequest(new HashSet<String>(), getClass().getSimpleName());
        final String responseBody = "{\"bodyXML\":\"<body>Testing.</body>\"}";
        final MutableResponse response = new MutableResponse(new MultivaluedMapImpl(), responseBody.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }
}
