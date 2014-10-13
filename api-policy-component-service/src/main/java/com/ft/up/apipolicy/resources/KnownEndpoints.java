package com.ft.up.apipolicy.resources;

import com.ft.up.apipolicy.filters.WebUrlCalculator;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import com.ft.up.apipolicy.pipeline.RequestForwarder;

/**
 * KnownEndpoints
 *
 * @author Simon.Gibbs
 */
public enum KnownEndpoints {

    CONTENT("^/content/") {
        @Override
        public HttpPipeline pipeline(final PipelineConfiguration config, final RequestForwarder forwarder) {
            return new HttpPipeline(forwarder, new WebUrlCalculator(config.getWebUrlTemplates()));
        }
    },
    NOTIFICATIONS("^/content/notifications") {
        @Override
        public HttpPipeline pipeline(final PipelineConfiguration config, final RequestForwarder forwarder) {
            return new HttpPipeline(forwarder);
        }
    };

    private final String uriRegex;

    KnownEndpoints(final String uriRegex) {
        this.uriRegex = uriRegex;
    }

    public String getUriRegex() {
        return uriRegex;
    }

    public abstract HttpPipeline pipeline(final PipelineConfiguration config, final RequestForwarder forwarder);

}
