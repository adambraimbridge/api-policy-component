package com.ft.up.apipolicy.pipeline;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.MultivaluedMap;

/**
 * MutableResponse
 *
 * @author Simon.Gibbs
 */
public class MutableResponse {
    private byte[] entity;
    private MultivaluedMap<String, String> headers;

    public MutableResponse() {
        headers = new MultivaluedMapImpl();
    }

    public MutableResponse(MultivaluedMap<String,String> headers, byte[] entity) {
        this.headers = new MultivaluedMapImpl(headers);
        this.entity = entity;
    }

    public byte[] getEntity() {
        return entity;
    }

    public MultivaluedMap<String,String> getHeaders() {
        return headers;
    }
}
