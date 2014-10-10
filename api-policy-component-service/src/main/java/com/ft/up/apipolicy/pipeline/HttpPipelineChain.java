package com.ft.up.apipolicy.pipeline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HttpPipelineChain
 *
 * @author Simon.Gibbs
 */
public class HttpPipelineChain {

    private final HttpPipeline pipeline;
    private int pointer = 0;

    HttpPipelineChain(final HttpPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void callNextFilter(final HttpServletRequest request, final HttpServletResponse response) {
        ApiFilter nextFilter = pipeline.getFilter(pointer++);
        if (nextFilter == null) {
            pipeline.forwardRequest(request, response);
        } else {
           nextFilter.processRequest(request, response, this);
        }
    }
}
