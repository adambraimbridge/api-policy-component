package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MainImageFilterTest extends TestCase {

    private MainImageFilter mainImageFilter = new MainImageFilter(new JsonConverter(new ObjectMapper()));

    @Test
    public void testFiltersMainImage() throws Exception {
        final MutableRequest mockedRequest = Mockito.mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = Mockito.mock(HttpPipelineChain.class);
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        final MutableResponse initialResponse = new MutableResponse(headers, readFileBytes("sample-article-with-image.json"));
        initialResponse.setStatus(200);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = mainImageFilter.processRequest(mockedRequest, mockedChain);

        assertThat(processedResponse.getEntityAsString(), not(containsString("mainImage")));
    }

    @Test
    public void testLeavesMainImageWhenPolicyHeaderSet() throws Exception {
        final MutableRequest mockedRequest = Mockito.mock(MutableRequest.class);
        when(mockedRequest.policyIs(INCLUDE_RICH_CONTENT)).thenReturn(true);
        final HttpPipelineChain mockedChain = Mockito.mock(HttpPipelineChain.class);
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        byte[] body = readFileBytes("sample-article-with-image.json");
        final MutableResponse initialResponse = new MutableResponse(headers, body);
        initialResponse.setStatus(200);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = mainImageFilter.processRequest(mockedRequest, mockedChain);

        assertThat(processedResponse.getEntityAsString(), containsString("mainImage"));
        final String bodyString = new String(body, Charset.forName("UTF-8"));
        assertThat(processedResponse.getEntityAsString(), equalToIgnoringWhiteSpace(bodyString));
    }

    @Test
    public void testDoesntTouchWhenNoMainImageIsPresent() throws Exception {
        final MutableRequest mockedRequest = Mockito.mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = Mockito.mock(HttpPipelineChain.class);
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        byte[] body = readFileBytes("sample-article-no-image.json");
        final MutableResponse initialResponse = new MutableResponse(headers, body);
        initialResponse.setStatus(200);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = mainImageFilter.processRequest(mockedRequest, mockedChain);

        final String bodyString = new String(body, Charset.forName("UTF-8"));
        assertThat(processedResponse.getEntityAsString(), equalToIgnoringWhiteSpace(bodyString));
    }

    @Test
    public void testLeavesEverythingWhenNot200() throws Exception {
        final MutableRequest mockedRequest = Mockito.mock(MutableRequest.class);
        final HttpPipelineChain mockedChain = Mockito.mock(HttpPipelineChain.class);
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        final String body = "{\"message\": \"Bad request\"}";
        final MutableResponse initialResponse = new MutableResponse(headers, body.getBytes("UTF-8"));
        initialResponse.setStatus(400);
        when(mockedChain.callNextFilter(mockedRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = mainImageFilter.processRequest(mockedRequest, mockedChain);

        assertThat(processedResponse.getEntityAsString(), equalTo(body));
    }

    private static byte[] readFileBytes(final String path) {
        try {
            return Files.readAllBytes(Paths.get(MainImageFilterTest.class.getClassLoader().getResource(path).toURI()));
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
