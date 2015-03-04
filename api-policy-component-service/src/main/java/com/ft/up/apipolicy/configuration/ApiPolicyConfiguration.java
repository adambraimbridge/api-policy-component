package com.ft.up.apipolicy.configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import io.dropwizard.Configuration;

import java.util.List;

public class ApiPolicyConfiguration extends Configuration {

    @NotNull
    @JsonProperty @Valid
    private EndpointConfiguration varnish;

    @NotNull
    @JsonProperty("pipeline") @Valid
    private PipelineConfiguration pipelineConfiguration;

    @NotNull
    @JsonProperty("policyBrandsMapper") @Valid
    private PolicyBrandsResolver policyBrandsResolver;

    @NotNull
    @JsonProperty @Valid
    private List<String> webUrlEligibleUrls;

    public EndpointConfiguration getVarnish() {
        return varnish;
    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public PolicyBrandsResolver getPolicyBrandsResolver() {
        return policyBrandsResolver;
    }

    public List<String> getWebUrlEligibleUrls() {
        return webUrlEligibleUrls;
    }
}
