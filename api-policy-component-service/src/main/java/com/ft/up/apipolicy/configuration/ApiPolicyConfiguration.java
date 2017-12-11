package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.platform.dropwizard.ConfigWithAppInfo;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import com.ft.platform.dropwizard.AppInfo;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ApiPolicyConfiguration extends Configuration implements ConfigWithAppInfo {
    @JsonProperty
    private AppInfo appInfo = new AppInfo();
    @NotNull
    @JsonProperty @Valid
    private EndpointConfiguration varnish;

    @JsonProperty("checkingVulcanHealth")
    private boolean checkingVulcanHealth;

    @NotNull
    @JsonProperty("pipeline")
    @Valid
    private PipelineConfiguration pipelineConfiguration;

    @NotNull
    @JsonProperty("policyBrandsMapper")
    @Valid
    private PolicyBrandsResolver policyBrandsResolver;

    public EndpointConfiguration getVarnish() {
        return varnish;
    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public PolicyBrandsResolver getPolicyBrandsResolver() {
        return policyBrandsResolver;
    }

    public boolean isCheckingVulcanHealth() {
        return checkingVulcanHealth;
    }

    @Override
    public AppInfo getAppInfo() {
        return appInfo;
    }
}
