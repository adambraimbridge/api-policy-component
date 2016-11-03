package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.Response;
import java.util.Map;

public class AddSyndication implements ApiFilter {

    private static final String CAN_BE_SYNDICATED_KEY = "canBeSyndicated";
    private static final String CAN_BE_SYNDICATED_VERIFY = "verify";

    private JsonConverter jsonConverter;
    private Policy policy;

    public AddSyndication(final JsonConverter jsonConverter, final Policy policy) {
        this.jsonConverter = jsonConverter;
        this.policy = policy;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse originalResponse = chain.callNextFilter(request);
        if (request.policyIs(policy)) {
            if (!isEligibleForSyndicationField(originalResponse)) {
                return originalResponse;
            }
            final Map<String, Object> content = jsonConverter.readEntity(originalResponse);
            if (!content.containsKey(CAN_BE_SYNDICATED_KEY)) {
                content.put(CAN_BE_SYNDICATED_KEY, CAN_BE_SYNDICATED_VERIFY);
                jsonConverter.replaceEntity(originalResponse, content);
            }
            return originalResponse;
        } else {
            final Map<String, Object> content = jsonConverter.readEntity(originalResponse);
            if (content.containsKey(CAN_BE_SYNDICATED_KEY)) {
                content.remove(CAN_BE_SYNDICATED_KEY);
                jsonConverter.replaceEntity(originalResponse, content);
            }
            return originalResponse;
        }
    }

    private boolean isEligibleForSyndicationField(final MutableResponse response) {
        return !(isNotOKResponse(response) || isNotJson(response));
    }

    private boolean isNotOKResponse(final MutableResponse response) {
        return Response.Status.OK.getStatusCode() != response.getStatus();
    }

    private boolean isNotJson(final MutableResponse response) {
        return !jsonConverter.isJson(response);
    }
}
