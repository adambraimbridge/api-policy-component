package com.ft.up.apipolicy.filters;

import com.ft.api.jaxrs.errors.ClientError;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;

public class SyndicationDistributionFilter extends AbstractImageFilter {

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

        FieldModifier modifier = (jsonProperty, contentModel) -> {
            if (contentModel.containsKey(jsonProperty)) {
                String canBeDistributed = (String) contentModel.get(jsonProperty);
                if (CAN_BE_DISTRIBUTED_VALUE_YES.equals(canBeDistributed)) {
                    contentModel.remove(jsonProperty);
                    jsonConverter.replaceEntity(originalResponse, content);
                } else {
                    throw ClientError.status(403).error("Access denied.").exception();
                }
            }
        };
        applyFilter(CAN_BE_DISTRIBUTED_KEY, modifier, content);

        return originalResponse;
    }
}
