package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class ExpandedImagesFilter implements ApiFilter {

    private static final String EXPAND_IMAGES = "expandImages";

    private final Policy[] policies;

    public ExpandedImagesFilter(Policy... policies) {
        this.policies = policies;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
        if (policies.length > 0 && shouldAddParameter(request)) {
            request.getQueryParameters().putSingle(EXPAND_IMAGES, Boolean.TRUE.toString());
        }
        return chain.callNextFilter(request);
    }

    private boolean shouldAddParameter(MutableRequest request) {
        for (Policy policy : policies) {
            if (!request.policyIs(policy)) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }
}
