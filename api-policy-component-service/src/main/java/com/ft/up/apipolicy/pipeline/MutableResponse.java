package com.ft.up.apipolicy.pipeline;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import javax.ws.rs.core.MultivaluedMap;

/**
 * MutableResponse
 *
 * @author Simon.Gibbs
 */
public class MutableResponse {
    private byte[] entity;
    private MultivaluedMap<String, String> headers;
    private int status;

    public MutableResponse() {
        headers = new MultivaluedMapImpl();
    }

    public MutableResponse(MultivaluedMap<String,String> headers, @SuppressWarnings("EI_EXPOSE_REP2") byte[] entity) {
        this.headers = new MultivaluedMapImpl(headers);
        this.entity = entity;
    }

    @SuppressWarnings("EI_EXPOSE_REP2")
    public byte[] getEntity() {
        return entity;
    }

    public MultivaluedMap<String,String> getHeaders() {
        return headers;
    }

    public void setEntity(byte[] bytes) {
        this.entity = bytes;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
