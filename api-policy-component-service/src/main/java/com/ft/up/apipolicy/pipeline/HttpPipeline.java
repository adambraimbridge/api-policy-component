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

    public HttpPipeline(RequestForwarder forwarder, ApiFilter... filters) {
        this.forwarder = forwarder;
        this.filters = filters;
    }

    public ApiFilter getFilter(int pointer) {
        if(pointer>=filters.length) {
            return null;
        }
        return filters[pointer];
    }

    public void forwardRequest(HttpServletRequest request, HttpServletResponse response) {
        forwarder.forwardRequest(request,response);
    }
}
