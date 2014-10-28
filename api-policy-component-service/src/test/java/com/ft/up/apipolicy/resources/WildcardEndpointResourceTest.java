package com.ft.up.apipolicy.resources;

import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class WildcardEndpointResourceTest {

	private WildcardEndpointResource wildcardEndpointResource;
	private HttpServletRequest request;
	private UriInfo uriInfo;
	private HttpPipeline contentPipeline;
	private HttpPipeline notificationsPipeline;

    private static final URI LOCALHOST = URI.create("http://localhost");

	@Before
	public void setup() throws IOException {
		RequestForwarder requestForwarder = mock(RequestForwarder.class);
		MutableResponse mutableResponse = new MutableResponse(new MultivaluedMapImpl(), "response".getBytes());
		when(requestForwarder.forwardRequest(any(MutableRequest.class))).thenReturn(mutableResponse);

		SortedSet<KnownEndpoint> knownEndpoints = new TreeSet<>();

        ApiFilter mockFilter = new ApiFilter() {
            @Override
            public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
                return chain.callNextFilter(request);
            }
        };

        contentPipeline = spy(new HttpPipeline(requestForwarder , mockFilter ));
		knownEndpoints.add(new KnownEndpoint("^/content/.*", contentPipeline));

        notificationsPipeline = spy(new HttpPipeline(requestForwarder, mockFilter));
		knownEndpoints.add(new KnownEndpoint("^/content/notifications.*", notificationsPipeline));

		wildcardEndpointResource = new WildcardEndpointResource(new MutableHttpTranslator(), knownEndpoints);

		request = mock(HttpServletRequest.class);
		when(request.getHeaderNames()).thenReturn(Collections.<String>emptyEnumeration());


		uriInfo = mock(UriInfo.class);
	}

	@Test
	public void shouldUseNotificationsPipelineWhenNotificationsUrlPassed() throws URISyntaxException {
        mockEnvironmentForRequestTo("/content/notifications");

		wildcardEndpointResource.service(request, uriInfo);
		verify(contentPipeline, never()).forwardRequest(any(MutableRequest.class));
		verify(notificationsPipeline, times(1)).forwardRequest(any(MutableRequest.class));
	}

    private StringBuffer absoluteUriFor(URI notificationsPath) {
        return new StringBuffer(LOCALHOST.resolve(notificationsPath).toString());
    }

    @Test
	public void shouldUseContentPipelineWhenContentUrlPassed() throws URISyntaxException {
        mockEnvironmentForRequestTo("/content/54307a12-37fa-11e3-8f44-002128161462");

		wildcardEndpointResource.service(request, uriInfo);

        verify(contentPipeline, times(1)).forwardRequest(any(MutableRequest.class));
		verify(notificationsPipeline, never()).forwardRequest(any(MutableRequest.class));
	}

    private void mockEnvironmentForRequestTo(String path) throws URISyntaxException {
        URI contentPath = new URI(path);
        when(uriInfo.getAbsolutePath()).thenReturn(contentPath);
        when(request.getRequestURL()).thenReturn(absoluteUriFor(contentPath));
        when(uriInfo.getBaseUri()).thenReturn(LOCALHOST);
        when(uriInfo.getPath()).thenReturn(path);
    }

    @Test
	public void shouldNotUseAnyPipelineWhenUnknownUrlPassed() throws URISyntaxException {
        mockEnvironmentForRequestTo("/you_ready_folks?");

		wildcardEndpointResource.service(request, uriInfo);

        verify(contentPipeline, never()).forwardRequest(any(MutableRequest.class));
		verify(notificationsPipeline, never()).forwardRequest(any(MutableRequest.class));
	}

}
