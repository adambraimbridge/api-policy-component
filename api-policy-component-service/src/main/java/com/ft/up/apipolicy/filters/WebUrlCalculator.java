package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

/**
 * WebUrlCalculator
 *
 * @author Simon.Gibbs
 */
public class WebUrlCalculator implements ApiFilter {

    private final Map<String, String> urlTemplates;

    public WebUrlCalculator(final Map<String, String> urlTemplates) {
        this.urlTemplates = urlTemplates;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {



        MutableResponse response = chain.callNextFilter(request);



        return response;

    }
}
