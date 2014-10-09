package com.ft.up.apipolicy.pipeline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * DummyFilter
 *
 * @author Simon.Gibbs
 */
public class DummyFilter implements ApiFilter {
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response, HttpPipelineChain chain) {
        chain.callNextFilter(request, response);
    }
}
