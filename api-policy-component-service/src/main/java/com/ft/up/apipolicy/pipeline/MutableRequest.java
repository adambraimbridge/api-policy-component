package com.ft.up.apipolicy.pipeline;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.Set;

/**
 * MutableRequest
 *
 * @author Simon.Gibbs
 */
public class MutableRequest {

    private MultivaluedMap<String, String> headers;
    private MultivaluedMap<String, String> queryParameters;
    private String absolutePath;

    private final Set<String> policies;

    public MutableRequest(final Set<String> policies) {
        this.policies = Collections.unmodifiableSet(policies);
    }

    public Set<String> getPolicies() {
        return policies;
    }

    public boolean policyIs(String policySymbol) {
        return policies.contains(policySymbol);
    }

    public MultivaluedMap<String,String> getHeaders() {
        return headers;
    }


    public MultivaluedMap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(MultivaluedMap<String, String> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }
}
