package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class VarnishConfiguration {
    @NotNull
    @JsonProperty
    @Valid
    private EndpointConfiguration endpointConfiguration;

    @NotNull
    @JsonProperty("connectionConfiguration")
    @Valid
    private ConnectionConfiguration connectionConfiguration;

    public EndpointConfiguration getEndpointConfiguration() {
        return endpointConfiguration;
    }

    public ConnectionConfiguration getConnectionConfiguration() {
        return connectionConfiguration;
    }
}
