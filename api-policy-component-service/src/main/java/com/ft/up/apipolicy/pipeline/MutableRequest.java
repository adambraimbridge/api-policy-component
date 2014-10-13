package com.ft.up.apipolicy.pipeline;

import javax.ws.rs.core.MultivaluedMap;

/**
 * MutableRequest
 *
 * @author Simon.Gibbs
 */
public class MutableRequest {

    private MultivaluedMap<String, String> headers;

    public MutableRequest(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public MultivaluedMap<String,String> getHeaders() {
        return headers;
    }
}
