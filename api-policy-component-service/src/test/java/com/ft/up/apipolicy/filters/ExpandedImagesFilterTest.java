package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedMap;

import static com.ft.up.apipolicy.configuration.Policy.EXPAND_RICH_CONTENT;
import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;
import static com.ft.up.apipolicy.configuration.Policy.INTERNAL_UNSTABLE;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExpandedImagesFilterTest {

    private ExpandedImagesFilter filter;

    @Mock
    private MutableRequest request;

    @Mock
    private HttpPipelineChain chain;

    @Before
    public void setUp() {
        filter = new ExpandedImagesFilter(INCLUDE_RICH_CONTENT, INTERNAL_UNSTABLE, EXPAND_RICH_CONTENT);
    }

    @Test
    public void thatRequestParameterIsAppliedWhenRichContentPolicy() {
        when(request.policyIs(EXPAND_RICH_CONTENT)).thenReturn(true);
        when(request.policyIs(INCLUDE_RICH_CONTENT)).thenReturn(true);
        when(request.policyIs(INTERNAL_UNSTABLE)).thenReturn(true);

        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> params = mock(MultivaluedMap.class);
        when(request.getQueryParameters()).thenReturn(params);
        filter.processRequest(request, chain);

        InOrder inOrder = inOrder(chain, params);
        inOrder.verify(params).putSingle("expandImages", "true");
        inOrder.verify(chain).callNextFilter(request);
    }

    @Test
    public void thatNoChangeIsAppliedWhenNoValidPolicy() {
        filter.processRequest(request, chain);

        verify(request).policyIs(INCLUDE_RICH_CONTENT);
        verifyNoMoreInteractions(request);
        verify(chain).callNextFilter(request);
    }

    @Test
    public void thatNoChangeIsAppliedWhenInternalUnstableAndExpandRichContentPoliciesArePresent() {
        when(request.policyIs(INCLUDE_RICH_CONTENT)).thenReturn(Boolean.TRUE);
        filter.processRequest(request, chain);

        verify(request).policyIs(INCLUDE_RICH_CONTENT);
        verify(request).policyIs(INTERNAL_UNSTABLE);
        verifyNoMoreInteractions(request);
        verify(chain).callNextFilter(request);
    }

    @Test
    public void thatNoChangeIsAppliedWhenExpandRichContentPolicyIsPresent() {
        when(request.policyIs(INCLUDE_RICH_CONTENT)).thenReturn(Boolean.TRUE);
        when(request.policyIs(INTERNAL_UNSTABLE)).thenReturn(Boolean.TRUE);
        filter.processRequest(request, chain);

        verify(request).policyIs(INCLUDE_RICH_CONTENT);
        verify(request).policyIs(INTERNAL_UNSTABLE);
        verify(request).policyIs(EXPAND_RICH_CONTENT);
        verifyNoMoreInteractions(request);
        verify(chain).callNextFilter(request);
    }
}
