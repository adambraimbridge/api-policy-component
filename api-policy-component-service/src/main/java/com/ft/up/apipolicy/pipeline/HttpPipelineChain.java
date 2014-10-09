package com.ft.up.apipolicy.pipeline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HttpPipelineChain
 *
 * @author Simon.Gibbs
 */
public class HttpPipelineChain {

    private HttpPipeline pipeline;
    int pointer =0;

    HttpPipelineChain(HttpPipeline pipeline) {

        this.pipeline = pipeline;
    }

    public void callNextFilter(HttpServletRequest request, HttpServletResponse response) {
        ApiFilter nextFilter = pipeline.getFilter(pointer++);
        if(nextFilter==null) {
            pipeline.forwardRequest(request, response);
        } else {
           nextFilter.processRequest(request, response, this);
        }
    }
}
