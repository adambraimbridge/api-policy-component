package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

public class RemoveJsonPropertyUnlessPolicyPresentFilter implements ApiFilter {

    private final JsonConverter jsonConverter;
    private final String jsonProperty;
    private final Policy policy;

    public RemoveJsonPropertyUnlessPolicyPresentFilter(final JsonConverter jsonConverter, final String jsonProperty, final Policy policy) {
        this.jsonConverter = jsonConverter;
        this.jsonProperty = jsonProperty;
        this.policy = policy;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        if (!request.policyIs(policy) && content.containsKey(jsonProperty)) {
            content.remove(jsonProperty);
            jsonConverter.replaceEntity(response, content);
        }
        return response;
    }
}
