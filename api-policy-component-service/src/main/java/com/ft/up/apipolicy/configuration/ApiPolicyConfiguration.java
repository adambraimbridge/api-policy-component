package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ApiPolicyConfiguration extends Configuration {

    @NotNull
    @JsonProperty("pipeline")
    @Valid
    private PipelineConfiguration pipelineConfiguration;

    @NotNull
    @JsonProperty("policyBrandsMapper")
    @Valid
    private PolicyBrandsResolver policyBrandsResolver;

    @NotNull
    @JsonProperty("varnish")
    @Valid
    private VarnishConfiguration varnishConfiguration;

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public PolicyBrandsResolver getPolicyBrandsResolver() {
        return policyBrandsResolver;
    }

    public VarnishConfiguration getVarnishConfiguration() {
        return varnishConfiguration;
    }
}
