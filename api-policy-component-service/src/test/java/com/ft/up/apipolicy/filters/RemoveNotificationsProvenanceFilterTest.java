package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemoveNotificationsProvenanceFilterTest {

    @Mock
    private MutableRequest mockRequest;
    @Mock
    private MutableResponse mockResponse;
    @Mock
    private HttpPipelineChain mockChain;
    @Mock
    private MultivaluedMap<String, String> mockHeaders;
    private RemoveNotificationsProvenanceFilter filter;

    private static final String NOTIFICATIONS_RESPONSE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15T00:00:00.000T\", " +
            "\"notifications\": [ " +
            "{ \"type\": \"http://www.ft.com/thing/ThingChangeType/UPDATE\", " +
            "\"id\": \"http://www.ft.com/thing/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"apiUrl\": \"http://int.api.ft.com/content/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"publishReference\": \"tid_AbCd1203\" } " +
            " ] " +
            "}";

    private static final String NOTIFICATIONS_EMPTY_RESPONSE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15T00:00:00.000T\", " +
            "\"notifications\": [ ] " +
            "}";

    private static final String NOTIFICATIONS_FILTERED_RESPONSE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15T00:00:00.000T\", " +
            "\"notifications\": [ " +
            "{ \"type\": \"http://www.ft.com/thing/ThingChangeType/UPDATE\", " +
            "\"id\": \"http://www.ft.com/thing/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"apiUrl\": \"http://int.api.ft.com/content/a1d6ca52-f9aa-407e-b682-03052dea7e25\" } " +
            " ] " +
            "}";

    private static final String NOTIFICATIONS_UNEXPECTED_JSON_FORMAT_RESPONSE = "{" +
            "\"requestUrl\": \"http://contentapi2.ft.com/content/notifications?since=2014-10-15T00:00:00.000T\", " +
            "\"notifications\": { \"notification\": [ " +
            "{ \"type\": \"http://www.ft.com/thing/ThingChangeType/UPDATE\", " +
            "\"id\": \"http://www.ft.com/thing/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"apiUrl\": \"http://int.api.ft.com/content/a1d6ca52-f9aa-407e-b682-03052dea7e25\", " +
            "\"publishReference\": \"tid_AbCd1203\" } " +
            " ] } " +
            "}";

    private JsonConverter jsonConverter;


    @Before
    public void setUp() throws Exception {
        jsonConverter = new JsonConverter(new ObjectMapper());
        filter = new RemoveNotificationsProvenanceFilter(jsonConverter, "publishReference", Policy.INCLUDE_PROVENANCE);

        when(mockHeaders.getFirst("Content-Type")).thenReturn(MediaType.APPLICATION_JSON);
    }

    @Test
    public void shouldRemoveProvenancePropertyWhenResponseIsSuccessfulAndIncludedPolicyIsFalse() throws Exception {
        when(mockRequest.policyIs(Policy.INCLUDE_PROVENANCE)).thenReturn(false);
        when(mockChain.callNextFilter(mockRequest)).thenReturn(mutableResponse(200, NOTIFICATIONS_RESPONSE));
        Map<String, Object> expected = jsonConverter.readEntity(mutableResponse(NOTIFICATIONS_FILTERED_RESPONSE));

        Map<String, Object> actual = jsonConverter.readEntity(filter.processRequest(mockRequest, mockChain));

        assertEquals(expected, actual);
    }

    @Test
    public void shouldNotFilterAnythingWhenNotificationsListIsEmpty() {
        MutableResponse expectedResponse = mutableResponse(200, NOTIFICATIONS_EMPTY_RESPONSE);
        when(mockChain.callNextFilter(mockRequest)).thenReturn(expectedResponse);

        Map<String, Object> expected = jsonConverter.readEntity(expectedResponse);
        Map<String, Object> actual = jsonConverter.readEntity(filter.processRequest(mockRequest, mockChain));

        assertEquals(expected, actual);
    }

    @Test
    public void shouldNotRemoveProvenancePropertyWhenReponseIsNotSuccessful() {
        MutableResponse expectedResponse = mutableResponse(500, NOTIFICATIONS_RESPONSE);
        when(mockChain.callNextFilter(mockRequest)).thenReturn(expectedResponse);
        Map<String, Object> expected = jsonConverter.readEntity(expectedResponse);

        Map<String, Object> actual = jsonConverter.readEntity(filter.processRequest(mockRequest, mockChain));

        assertEquals(expected, actual);
    }

    @Test
    public void shouldNotRemoveProvenancePropertyWhenResponseIsSuccessfulAndIncludedPolicyIsTrue() {
        when(mockRequest.policyIs(Policy.INCLUDE_PROVENANCE)).thenReturn(true);
        MutableResponse expectedResponse = mutableResponse(200, NOTIFICATIONS_RESPONSE);
        when(mockChain.callNextFilter(mockRequest)).thenReturn(expectedResponse);
        Map<String, Object> expected = jsonConverter.readEntity(expectedResponse);

        Map<String, Object> actual = jsonConverter.readEntity(filter.processRequest(mockRequest, mockChain));

        assertEquals(expected, actual);
    }

    @Test(expected = FilterException.class)
    public void shouldThrowExceptionIfResponseJsonIsNotInExpectedFormat(){
        when(mockChain.callNextFilter(mockRequest)).thenReturn(mutableResponse(200, NOTIFICATIONS_UNEXPECTED_JSON_FORMAT_RESPONSE));

        filter.processRequest(mockRequest, mockChain);
    }

    private MutableResponse mutableResponse(String body) {
        return mutableResponse(200, body);
    }

    private MutableResponse mutableResponse(int status, String body) {
        MutableResponse response = new MutableResponse();
        response.setEntity(body.getBytes());
        response.setStatus(status);
        response.setHeaders(mockHeaders);
        return response;
    }
}