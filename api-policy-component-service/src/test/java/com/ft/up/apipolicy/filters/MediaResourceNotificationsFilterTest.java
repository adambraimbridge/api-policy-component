package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaResourceNotificationsFilterTest {

    public final static String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";
    public final static String SUCCESS_RESPONSE = "{ \"requestUrl\": \"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource\", \"links\": [ {\"href\": \"http://example.org/content/100?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource\", \"rel\" : \"next\"}] }";
    public final static String STRIPPED_SUCCESS_RESPONSE = "{\"requestUrl\":\"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z\",\"links\":[{\"href\":\"http://example.org/content/100?since=2016-07-23T00:00:00.000Z\",\"rel\":\"next\"}]}";

    private final JsonConverter jsonConverter = JsonConverter.testConverter();

    private MediaResourceNotificationsFilter filter = new MediaResourceNotificationsFilter(jsonConverter);
    private MutableRequest request = mock(MutableRequest.class);
    private HttpPipelineChain chain = mock(HttpPipelineChain.class);
    private MultivaluedMapImpl headers = new MultivaluedMapImpl();

    private MutableResponse errorResponse;
    private MutableResponse successResponse;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        errorResponse = new MutableResponse(headers, ERROR_RESPONSE.getBytes());
        errorResponse.setStatus(500);

        successResponse = new MutableResponse(headers, SUCCESS_RESPONSE.getBytes());
        successResponse.setStatus(200);
    }

    @Test
    public void testThatArticleTypeQueryParamIsAddedWhenNoIncludeMediaResourcePolicyIsPresent() throws Exception {
        when(request.policyIs(Policy.INCLUDE_MEDIARESOURCE)).thenReturn(false);
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        when(chain.callNextFilter(request)).thenReturn(successResponse);

        filter.processRequest(request, chain);

        InOrder inOrder = inOrder(chain, params);
        inOrder.verify(params).put("type", Collections.singletonList("article"));
        inOrder.verify(chain).callNextFilter(request);
    }

    @Test
    public void testThatMediaResourceTypeQueryParamIsAddedWhenIncludeMediaResourcePolicyIsPresent() throws Exception {
        when(request.policyIs(Policy.INCLUDE_MEDIARESOURCE)).thenReturn(true);
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        when(chain.callNextFilter(request)).thenReturn(successResponse);

        filter.processRequest(request, chain);

        InOrder inOrder = inOrder(chain, params);
        inOrder.verify(params).put("type", Arrays.asList("article", "mediaResource"));
        inOrder.verify(chain).callNextFilter(request);
    }

    @Test
    public void testThatForNon200ResponseNoOtherInteractionHappens() {
        when(request.policyIs(Policy.INCLUDE_MEDIARESOURCE)).thenReturn(true);
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        when(chain.callNextFilter(request)).thenReturn(errorResponse);

        filter.processRequest(request, chain);

        verify(request).policyIs(Policy.INCLUDE_MEDIARESOURCE);
        verify(request).getQueryParameters();
        verifyNoMoreInteractions(request);
    }

    @Test
    public void testThatFilterExceptionIsThrownWhenUnexpectedJSONFieldTypes() {
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);

        MutableResponse badResponse = new MutableResponse(headers, "{ \"requestUrl\": [] }".getBytes());
        badResponse.setStatus(200);
        when(chain.callNextFilter(request)).thenReturn(badResponse);

        expectedException.expect(FilterException.class);

        filter.processRequest(request, chain);
    }

    @Test
    public void testThatForEmptyLinksArrayReturnedBodySameAsStrippedResponseBody() {
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);

        String responseBody = "{ \"requestUrl\": \"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z&type=article&type=mediaResource\", \"links\": [] }";
        String strippedBody = "{\"requestUrl\":\"http://example.org/content/notifications?since=2016-07-23T00:00:00.000Z\",\"links\":[]}";
        MutableResponse responseWithEmptyLinksArray = new MutableResponse(headers, responseBody.getBytes());
        responseWithEmptyLinksArray.setStatus(200);
        when(chain.callNextFilter(request)).thenReturn(responseWithEmptyLinksArray);

        MutableResponse returned = filter.processRequest(request, chain);

        assertThat("", returned.getEntityAsString(), is(strippedBody));
    }

    @Test
    public void testThatForHappyResponseReturnedBodySameAsStrippedResponseBody() {
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);

        MutableResponse happyResponse = new MutableResponse(headers, SUCCESS_RESPONSE.getBytes());
        happyResponse.setStatus(200);
        when(chain.callNextFilter(request)).thenReturn(happyResponse);

        MutableResponse returned = filter.processRequest(request, chain);

        assertThat("", returned.getEntityAsString(), is(STRIPPED_SUCCESS_RESPONSE));
    }
}