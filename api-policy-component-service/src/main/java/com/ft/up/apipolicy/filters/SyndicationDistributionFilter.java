package com.ft.up.apipolicy.filters;

import com.ft.api.jaxrs.errors.ClientError;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;

public class SyndicationDistributionFilter implements ApiFilter {

    private static final String CAN_BE_DISTRIBUTED_KEY = "canBeDistributed";
    private static final String CAN_BE_DISTRIBUTED_VALUE_YES = "yes";

    private JsonConverter jsonConverter;
    private Policy policy;

    public SyndicationDistributionFilter(JsonConverter jsonConverter, Policy policy) {
        this.jsonConverter = jsonConverter;
        this.policy = policy;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
        final MutableResponse originalResponse = chain.callNextFilter(request);

        if (OK.getStatusCode() != originalResponse.getStatus() || !jsonConverter.isJson(originalResponse)) {
            return originalResponse;
        }

        if (request.policyIs(policy)) {
            return originalResponse;
        }

        final Map<String, Object> content = jsonConverter.readEntity(originalResponse);

        if (content.containsKey(CAN_BE_DISTRIBUTED_KEY)) {

            String canBeDistributed = (String) content.get(CAN_BE_DISTRIBUTED_KEY);
            if (CAN_BE_DISTRIBUTED_VALUE_YES.equals(canBeDistributed)) {
                content.remove(CAN_BE_DISTRIBUTED_KEY);
                jsonConverter.replaceEntity(originalResponse, content);

            } else {
                throw ClientError.status(403).error("Access denied.").exception();

            }
        }

        return originalResponse;
    }
}