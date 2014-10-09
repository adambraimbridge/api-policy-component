package com.ft.up.apipolicy.pipeline;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * RequestForwarder
 *
 * @author Simon.Gibbs
 */
public interface RequestForwarder {
    void forwardRequest(HttpServletRequest request, HttpServletResponse response);
}
