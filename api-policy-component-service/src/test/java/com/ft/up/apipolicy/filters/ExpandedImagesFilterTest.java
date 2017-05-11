package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExpandedImagesFilterTest {

    private ExpandedImagesFilter filter = new ExpandedImagesFilter();

    @Mock
    private MutableRequest request;

    @Mock
    private HttpPipelineChain chain;

    @Test
    public void thatRequestParameterIsAppliedWhenRichContentPolicy() {
        when(request.policyIs(Policy.EXPAND_RICH_CONTENT)).thenReturn(true);
        when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(true);
        when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(true);
        @SuppressWarnings("unchecked")
        MultivaluedMap<String,String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        filter.processRequest(request, chain);

        InOrder inOrder = inOrder(chain, params);
        inOrder.verify(params).putSingle("expandImages", "true");
        inOrder.verify(chain).callNextFilter(request);
    }

    @Test
    public void thatNoChangeIsAppliedWhenNoValidPolicy() {
        filter.processRequest(request, chain);

        verify(request).policyIs(Policy.INCLUDE_RICH_CONTENT);
        verifyNoMoreInteractions(request);
        verify(chain).callNextFilter(request);
    }

    @Test
    public void thatNoChangeIsAppliedWhenInternalUnstableAndExpandRichContentPoliciesArePresent() {
        when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(Boolean.TRUE);
        filter.processRequest(request, chain);

        verify(request).policyIs(Policy.INCLUDE_RICH_CONTENT);
        verify(request).policyIs(Policy.INTERNAL_UNSTABLE);
        verifyNoMoreInteractions(request);
        verify(chain).callNextFilter(request);
    }

    @Test
    public void thatNoChangeIsAppliedWhenExpandRichContentPolicyIsPresent() {
        when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(Boolean.TRUE);
        when(request.policyIs(Policy.INTERNAL_UNSTABLE)).thenReturn(Boolean.TRUE);
        filter.processRequest(request, chain);

        verify(request).policyIs(Policy.INCLUDE_RICH_CONTENT);
        verify(request).policyIs(Policy.INTERNAL_UNSTABLE);
        verify(request).policyIs(Policy.EXPAND_RICH_CONTENT);
        verifyNoMoreInteractions(request);
        verify(chain).callNextFilter(request);
    }
}
