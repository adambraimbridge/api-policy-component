package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
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

    public long getSlowRequestTimeout() {
        return slowRequestTimeout;
    }

    public String getSlowRequestPattern() {
        return slowRequestPattern;
    }

    public EndpointConfiguration getVarnish() {
        return varnish;
    }
}
