package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * WebUrlCalculator
 *
 * @author Simon.Gibbs
 */
public class WebUrlCalculator implements ApiFilter {

    private Map<String, String> urlTemplates;

    public WebUrlCalculator(Map<String, String> urlTemplates) {
        this.urlTemplates = urlTemplates;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response, HttpPipelineChain chain) {

        chain.callNextFilter(request, response);

    }
}
