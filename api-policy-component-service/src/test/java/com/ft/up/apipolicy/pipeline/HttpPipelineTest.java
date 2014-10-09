package com.ft.up.apipolicy.pipeline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.Mockito.*;

/**
 * HttpPipelineTest
 *
 * @author Simon.Gibbs
 */

@RunWith(MockitoJUnitRunner.class)
public class HttpPipelineTest {

    public static final String SYNTHETIC_EXCEPTION_MESSAGE = "Synthetic exception";
    @Mock
    RequestForwarder forwarder;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Rule
    public org.junit.rules.ExpectedException expectedException = ExpectedException.none();

    @Test
    public void successfulRequestShouldPropagateToForwarder() {

        ApiFilter filter1 = new DummyFilter();
        ApiFilter filter2 = new DummyFilter();


        HttpPipelineChain chain = makeChain(filter1, filter2);

        chain.callNextFilter(request, response);

        verify(forwarder,times(1)).forwardRequest(request,response);

    }


    @Test
    public void successfulRequestShouldTouchAllTheFilters() {

        ApiFilter filter1 = spy(new DummyFilter());
        ApiFilter filter2 = spy(new DummyFilter());

        HttpPipelineChain chain = makeChain(filter1, filter2);

        chain.callNextFilter(request, response);

        verify(filter1,times(1)).processRequest(request,response,chain);
        verify(filter2,times(1)).processRequest(request,response,chain);

    }

    @Test
    public void requestFailingBeforeForwarderCallShouldNotPropagateToForwarder() {
        ApiFilter filter1 = spy(new DummyFilter());
        ApiFilter filter2 = spy(new DummyFilter());

        HttpPipelineChain chain = makeChain(filter1, filter2);

        doThrow(new RuntimeException("Synthetic exception")).when(filter2).processRequest(request, response, chain);

        try {
            chain.callNextFilter(request, response);
        } catch (RuntimeException e){
            //ok
        }

        verify(filter1,times(1)).processRequest(request,response,chain);
        verify(filter2,times(1)).processRequest(request,response,chain);
        verify(forwarder,never()).forwardRequest(request, response);
    }

    @Test
    public void requestFailingInFirstFilterDoesNotCallSecondFilter() {
        ApiFilter filter1 = spy(new DummyFilter());
        ApiFilter filter2 = spy(new DummyFilter());

        HttpPipelineChain chain = makeChain(filter1, filter2);

        doThrow(new RuntimeException(SYNTHETIC_EXCEPTION_MESSAGE)).when(filter1).processRequest(request, response, chain);

        try {
            chain.callNextFilter(request, response);
        } catch (RuntimeException e){
            // ok
        }

        verify(filter1,times(1)).processRequest(request,response,chain);
        verify(filter2,never()).processRequest(request,response,chain);
        verify(forwarder,never()).forwardRequest(request,response);
    }


    @Test
    public void filterExceptionsShouldBubbledToInvokingMethod() {
        ApiFilter filter1 = new DummyFilter();
        ApiFilter filter2 = spy(new DummyFilter());

        HttpPipelineChain chain = makeChain(filter1, filter2);

        doThrow(new RuntimeException(SYNTHETIC_EXCEPTION_MESSAGE)).when(filter2).processRequest(request, response, chain);

        expectedException.expect(RuntimeException.class);
        expectedException.expect(hasProperty("message", equalTo(SYNTHETIC_EXCEPTION_MESSAGE)));

        chain.callNextFilter(request, response);
    }


    private HttpPipelineChain makeChain(ApiFilter filter1, ApiFilter filter2) {
        HttpPipeline pipeline = new HttpPipeline(forwarder, filter1, filter2);

        return new HttpPipelineChain(pipeline);
    }
}
