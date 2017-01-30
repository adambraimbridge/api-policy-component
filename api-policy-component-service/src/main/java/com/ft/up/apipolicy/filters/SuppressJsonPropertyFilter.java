package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

public class SuppressJsonPropertyFilter implements ApiFilter {

    private final JsonConverter jsonConverter;
    private final String jsonProperty;

    public SuppressJsonPropertyFilter(final JsonConverter jsonConverter, final String jsonProperty) {
        this.jsonConverter = jsonConverter;
        this.jsonProperty = jsonProperty;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        if (shouldPropertyFilteredOut(request, response)) {
            content.remove(jsonProperty);
            jsonConverter.replaceEntity(response, content);
        }
        return response;
    }

    protected boolean shouldPropertyFilteredOut(final MutableRequest request, final MutableResponse response) {
        final Map<String, Object> content = jsonConverter.readEntity(response);
        return content.containsKey(jsonProperty);
    }

    protected JsonConverter getJsonConverter() {
        return jsonConverter;
    }

    protected String getJsonProperty() {
        return jsonProperty;
    }
}
