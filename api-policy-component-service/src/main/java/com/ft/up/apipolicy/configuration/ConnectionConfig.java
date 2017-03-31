package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectionConfig {

    private final int numberOfConnectionAttempts;
    private final int timeoutMultiplier;

    public ConnectionConfig(@JsonProperty("numberOfConnectionAttempts") int numberOfConnectionAttempts,
                            @JsonProperty("timeoutMultiplier") int timeoutMultiplier) {
        this.numberOfConnectionAttempts = numberOfConnectionAttempts;
        this.timeoutMultiplier = timeoutMultiplier;
    }

    public int getNumberOfConnectionAttempts() { return numberOfConnectionAttempts; }

    public int getTimeoutMultiplier() { return timeoutMultiplier; }
}
