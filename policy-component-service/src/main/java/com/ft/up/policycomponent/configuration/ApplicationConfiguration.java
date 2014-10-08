package com.ft.up.policycomponent.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class ApplicationConfiguration extends Configuration {

    @NotNull
    @JsonProperty
    private long slowRequestTimeout;
    @NotNull
    @JsonProperty
    private String slowRequestPattern;

    public long getSlowRequestTimeout() {
        return slowRequestTimeout;
    }

    public String getSlowRequestPattern() {
        return slowRequestPattern;
    }
}
