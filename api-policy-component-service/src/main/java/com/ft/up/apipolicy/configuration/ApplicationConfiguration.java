package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ApplicationConfiguration extends Configuration {

    @NotNull
    @JsonProperty
    private long slowRequestTimeout;

    @NotNull
    @JsonProperty
    private String slowRequestPattern;

    @NotNull
    @JsonProperty @Valid
    private EndpointConfiguration varnish;

    @NotNull
    @JsonProperty("pipeline") @Valid
    private PipelineConfiguration pipelineConfiguration;

    public long getSlowRequestTimeout() {
        return slowRequestTimeout;
    }

    public String getSlowRequestPattern() {
        return slowRequestPattern;
    }

    public EndpointConfiguration getVarnish() {
        return varnish;
    }

    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }
}
