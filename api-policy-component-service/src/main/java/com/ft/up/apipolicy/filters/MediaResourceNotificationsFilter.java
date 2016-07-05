package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.ArrayList;
import java.util.List;

public class MediaResourceNotificationsFilter implements ApiFilter {
    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
        List<String> typeParams = new ArrayList<>();
        typeParams.add("article");
        if (request.policyIs(Policy.INCLUDE_MEDIARESOURCE)) {
            typeParams.add("mediaResource");
        }
        request.getQueryParameters().put("type", typeParams);
        return chain.callNextFilter(request);
    }
}
