package com.ft.up.apipolicy.pipeline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HttpPipeline
 *
 * @author Simon.Gibbs
 */
public class HttpPipeline {

    private RequestForwarder forwarder;
    private ApiFilter[] filters;

    public HttpPipeline(final RequestForwarder forwarder, final ApiFilter... filters) {
        this.forwarder = forwarder;
        this.filters = filters;
    }

    public ApiFilter getFilter(final int pointer) {
        if (pointer >= filters.length) {
            return null;
        }
        return filters[pointer];
    }

    public void forwardRequest(final HttpServletRequest request, final HttpServletResponse response) {
        forwarder.forwardRequest(request, response);
    }
}
