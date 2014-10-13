package com.ft.up.apipolicy.pipeline;

/**
 * ApiFilter
 *
 * @author Simon.Gibbs
 */
public interface ApiFilter {

    MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain);

}
