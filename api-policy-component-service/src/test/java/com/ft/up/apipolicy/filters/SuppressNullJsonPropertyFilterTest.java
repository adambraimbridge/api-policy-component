package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SuppressNullJsonPropertyFilterTest {

    private static final String PROPERTY_NAME = "firstPublishedDate";

    @Mock
    private MutableRequest mockRequest;
    @Mock
    private MutableResponse mockResponse;

    @Mock
    private HttpPipelineChain mockChain;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MultivaluedMap<String, String> mockHeaders;

    private SuppressNullJsonPropertyFilter filter;

    @Before
    public void setUp() throws Exception {
        JsonConverter jsonConverter = new JsonConverter(new ObjectMapper());
        filter = new SuppressNullJsonPropertyFilter(jsonConverter, "firstPublishedDate");
    }

    @Test
    public void testFilterNullJsonProperty() throws Exception {
        final JsonNode actualTree = processRequest("sample-article-null-property.json");
        assertFalse(actualTree.has(PROPERTY_NAME));
    }

    @Test
    public void testKeepNotNullJsonProperty() throws Exception {
        final JsonNode actualTree = processRequest("sample-article-not-null-property.json");
        assertTrue(actualTree.has(PROPERTY_NAME));
    }

    private JsonNode processRequest(String fileName) throws IOException {
        final MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.add("Content-Type", MediaType.APPLICATION_JSON);
        final MutableResponse initialResponse = new MutableResponse(headers, readFileBytes(fileName));
        initialResponse.setStatus(200);
        when(mockChain.callNextFilter(mockRequest)).thenReturn(initialResponse);

        final MutableResponse processedResponse = filter.processRequest(mockRequest, mockChain);

        return objectMapper.readTree(processedResponse.getEntityAsString());
    }

    private static byte[] readFileBytes(final String path) {
        try {
            URL url = SuppressNullJsonPropertyFilter.class.getClassLoader().getResource(path);
            if (url == null) {
                return null;
            }
            return Files.readAllBytes(Paths.get(url.toURI()));
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
