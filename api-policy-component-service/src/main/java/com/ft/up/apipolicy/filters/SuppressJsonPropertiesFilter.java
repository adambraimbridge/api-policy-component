package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SuppressJsonPropertiesFilter extends AbstractApiFilter {

    private final JsonConverter jsonConverter;
    private final List<String> jsonProperties;

    public SuppressJsonPropertiesFilter(final JsonConverter jsonConverter, final String... jsonProperties) {
        this.jsonConverter = jsonConverter;
        this.jsonProperties = Arrays.asList(jsonProperties);
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        jsonProperties.forEach(jsonProperty -> {
            FieldModifier modifier = (jsonProp, contentModel) -> {
                if (shouldPropertyFilteredOut(jsonProp, request, contentModel)) {
                    contentModel.remove(jsonProp);
                    jsonConverter.replaceEntity(response, content);
                }
            };
            applyFilter(jsonProperty, modifier, content);
        });
        return response;
    }

    protected boolean shouldPropertyFilteredOut(final String jsonProperty, MutableRequest request, final Map content) {
        return content.containsKey(jsonProperty);
    }
}
