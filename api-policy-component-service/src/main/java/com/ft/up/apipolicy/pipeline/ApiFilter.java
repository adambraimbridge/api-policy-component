package com.ft.up.apipolicy.pipeline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ApiFilter
 *
 * @author Simon.Gibbs
 */
public interface ApiFilter {

    public void processRequest(HttpServletRequest request, HttpServletResponse response, HttpPipelineChain chain);

}
