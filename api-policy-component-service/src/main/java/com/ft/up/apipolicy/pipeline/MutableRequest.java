package com.ft.up.apipolicy.pipeline;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import com.ft.up.apipolicy.configuration.Policy;

/**
 * MutableRequest
 *
 * @author Simon.Gibbs
 */
public class MutableRequest {

    private MultivaluedMap<String, String> headers;
    private MultivaluedMap<String, String> queryParameters;
    private String absolutePath;
    private String httpMethod;
    private Object requestEntity;

    private final Set<String> policies;
    private String transactionId;

    public MutableRequest(final Set<String> policies, final String transactionId) {
        this.policies = Collections.unmodifiableSet(policies);
        this.transactionId = transactionId;
    }

    public Set<String> getPolicies() {
        return policies;
    }

    public boolean policyIs(final Policy policy) {
        return policies.contains(policy.getHeaderValue());
    }

    public MultivaluedMap<String,String> getHeaders() {
        return headers;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Object getRequestEntity() {
        return requestEntity;
    }

    public void setRequestEntity(Object requestEntity) {
        this.requestEntity = requestEntity;
    }

    public void setQueryParameters(MultivaluedMap<String, String> queryParameters) {
        this.queryParameters = queryParameters;
    }
    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
