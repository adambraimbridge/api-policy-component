package com.ft.up.apipolicy.configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import io.dropwizard.Configuration;

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
    @JsonProperty("connectionConfig") @Valid
    private ConnectionConfig connectionConfig;
    public EndpointConfiguration getVarnish() {
        return varnish;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public PolicyBrandsResolver getPolicyBrandsResolver() {
        return policyBrandsResolver;
    }

}
