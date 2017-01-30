package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

public class SuppressNullJsonPropertyFilter extends SuppressJsonPropertyFilter {

    public SuppressNullJsonPropertyFilter(JsonConverter jsonConverter, String jsonProperty) {
        super(jsonConverter, jsonProperty);
    }

    @Override
    protected boolean shouldPropertyFilteredOut(final MutableRequest request, final MutableResponse response) {
        final Map<String, Object> content = getJsonConverter().readEntity(response);
        return content.containsKey(getJsonProperty()) && content.get(getJsonProperty()) == null;
    }
}
