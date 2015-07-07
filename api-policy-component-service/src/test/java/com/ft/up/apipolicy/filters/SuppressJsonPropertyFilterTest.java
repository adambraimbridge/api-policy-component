package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SuppressJsonPropertyFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonConverter jsonConverter = new JsonConverter(objectMapper);
    private final SuppressJsonPropertyFilter removeJsonPropertyUnlessPolicyPresentFilter = new SuppressJsonPropertyFilter(jsonConverter, "comments");

    @Test
    public void testFiltersJsonProperty() throws Exception {
        final MutableRequest mockedRequest = mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        final MutableResponse initialResponse = new MutableResponse(headers, readFileBytes("sample-article-with-image.json"));
        initialResponse.setStatus(200);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

        final JsonNode actualTree = objectMapper.readTree(processedResponse.getEntityAsString());
        assertFalse(actualTree.has("comments"));
    }

    @Test
    public void testDoesNotTouchWhenJsonPropertyIsAbsent() throws Exception {
        final MutableRequest mockedRequest = mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        byte[] body = readFileBytes("sample-article-no-image.json");
        final MutableResponse initialResponse = new MutableResponse(headers, body);
        initialResponse.setStatus(200);
        final MutableResponse spiedResponse = spy(initialResponse);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(spiedResponse);

        removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

        verifyResponseNotMutated(spiedResponse);
    }

    @Test
    public void testLeavesEverythingWhenNot200() throws Exception {
        final MutableRequest mockedRequest = mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = mock(HttpPipelineChain.class);
        final MutableResponse mockedResponse = mock(MutableResponse.class);
        when(mockedResponse.getStatus()).thenReturn(400);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(mockedResponse);

        removeJsonPropertyUnlessPolicyPresentFilter.processRequest(mockedRequest, mockedChain);

        verifyResponseNotMutated(mockedResponse);
    }

    public void verifyResponseNotMutated(final MutableResponse mockedResponse) {
        verify(mockedResponse, never()).setEntity(Mockito.any(byte[].class));
        verify(mockedResponse, never()).setHeaders(Mockito.any(MultivaluedMap.class));
        verify(mockedResponse, never()).setStatus(anyInt());
    }

    private static byte[] readFileBytes(final String path) {
        try {
            return Files.readAllBytes(Paths.get(SuppressJsonPropertyFilterTest.class.getClassLoader().getResource(path).toURI()));
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
