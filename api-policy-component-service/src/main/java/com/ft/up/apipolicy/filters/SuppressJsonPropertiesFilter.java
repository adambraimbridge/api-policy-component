package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.List;
import java.util.Map;

public class SuppressJsonPropertiesFilter implements ApiFilter {

    private final JsonConverter jsonConverter;
    private final List<String> jsonProperties;

    public SuppressJsonPropertiesFilter(final JsonConverter jsonConverter, final List<String> jsonProperties) {
        this.jsonConverter = jsonConverter;
        this.jsonProperties = jsonProperties;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        jsonProperties.forEach(jsonProperty -> {
            if (shouldPropertyFilteredOut(jsonProperty, request, response)) {
                content.remove(jsonProperty);
                jsonConverter.replaceEntity(response, content);
            }
        });
        return response;
    }

    protected boolean shouldPropertyFilteredOut(final String jsonProperty, final MutableRequest request, final MutableResponse response) {
        final Map<String, Object> content = jsonConverter.readEntity(response);
        return content.containsKey(jsonProperty);
    }
}
