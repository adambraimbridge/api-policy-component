package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import org.junit.Test;
import org.mockito.InOrder;

import javax.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaResourceNotificationsFilterTest {

    private MediaResourceNotificationsFilter filter = new MediaResourceNotificationsFilter();
    private MutableRequest request = mock(MutableRequest.class);
    private HttpPipelineChain chain = mock(HttpPipelineChain.class);

    @Test
    public void testThatArticleTypeQueryParamIsAddedWhenNoIncludeMediaResourcePolicyIsPresent() throws Exception {
        when(request.policyIs(Policy.INCLUDE_MEDIARESOURCE)).thenReturn(false);
        @SuppressWarnings("unchecked")
        MultivaluedMap<String,String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        filter.processRequest(request, chain);

        InOrder inOrder = inOrder(chain, params);
        inOrder.verify(params).put("type", Collections.singletonList("article"));
        inOrder.verify(chain).callNextFilter(request);
    }

    @Test
    public void testThatMediaResourceTypeQueryParamIsAddedWhenIncludeMediaResourcePolicyIsPresent() throws Exception {
        when(request.policyIs(Policy.INCLUDE_MEDIARESOURCE)).thenReturn(true);
        @SuppressWarnings("unchecked")
        MultivaluedMap<String,String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        filter.processRequest(request, chain);

        InOrder inOrder = inOrder(chain, params);
        inOrder.verify(params).put("type", Arrays.asList("article","mediaResource"));
        inOrder.verify(chain).callNextFilter(request);
    }
}