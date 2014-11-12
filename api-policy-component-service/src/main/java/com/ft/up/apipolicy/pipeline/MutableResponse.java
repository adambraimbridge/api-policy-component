package com.ft.up.apipolicy.pipeline;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MutableResponse
 *
 * @author Simon.Gibbs
 */
public class MutableResponse {

    public static final String VARY_HEADER = "Vary";

    @SuppressWarnings("EI_EXPOSE_REP2")
    private byte[] entity;
    private MultivaluedMap<String, String> headers;
    private int status;

    public MutableResponse() {
        headers = new MultivaluedMapImpl();
    }

    public MutableResponse(MultivaluedMap<String,String> headers,  byte[] entity) {
        this.headers = new MultivaluedMapImpl(headers);
        this.entity = entity;
    }

    public byte[] getEntity() {
        return entity;
    }

    public String getEntityAsString() {
        return new String(entity);
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

    public String getContentType() {
        return headers.getFirst("Content-Type");
    }

    public Set<String> getHeadersInVaryList() {
        Set<String> varyByHeadersSet = new LinkedHashSet<>();
        if(headers.containsKey(VARY_HEADER)) {
            List<String> headerListsFromVaryHeaders = headers.get(VARY_HEADER);
            for(String headerListFromAVaryHeader : headerListsFromVaryHeaders) {
                String [] varyByHeaders = headerListFromAVaryHeader.split("[, ]");
                varyByHeadersSet.addAll(Arrays.asList(varyByHeaders));
            }
        }
        return varyByHeadersSet;
    }

    
}
