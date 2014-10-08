package com.ft.up.apipolicy.health;

import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;

public class ReaderNodesHealthCheck extends AdvancedHealthCheck {

    public ReaderNodesHealthCheck(final String name) {

        super(name);
    }

    @Override
    protected AdvancedResult checkAdvanced() throws Exception {

        if (true) {
            return AdvancedResult.healthy("All is ok");
        }

        return AdvancedResult.error(this, "Not ok");

    }

    @Override
    protected int severity() {
        return 0;
    }

    @Override
    protected String businessImpact() {
        return "business impact";
    }

    @Override
    protected String technicalSummary() {
        return "technical summary";
    }

    @Override
    protected String panicGuideUrl() {
        return "http://mypanicguide.com";
    }

}