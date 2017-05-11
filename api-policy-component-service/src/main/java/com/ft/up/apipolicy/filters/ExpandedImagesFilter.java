package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class ExpandedImagesFilter implements ApiFilter {

    private static final String EXPAND_IMAGES = "expandImages";

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
        if (request.policyIs(Policy.INCLUDE_RICH_CONTENT) && request.policyIs(Policy.INTERNAL_UNSTABLE) && request.policyIs(Policy.EXPAND_RICH_CONTENT)) {
            request.getQueryParameters().putSingle(EXPAND_IMAGES, Boolean.TRUE.toString());
        }
        return chain.callNextFilter(request);
    }
}
