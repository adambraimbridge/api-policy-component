package com.ft.up.apipolicy.pipeline;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * PipelineConfiguration
 *
 * @author Simon.Gibbs
 */
public class PipelineConfiguration {

    private Map<String, String> webUrlTemplates;

    @NotNull
    public Map<String, String> getWebUrlTemplates() {
        return webUrlTemplates;
    }
}
