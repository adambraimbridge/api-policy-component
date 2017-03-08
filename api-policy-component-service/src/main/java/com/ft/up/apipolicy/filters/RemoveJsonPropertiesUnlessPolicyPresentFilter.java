package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.List;

public class RemoveJsonPropertiesUnlessPolicyPresentFilter extends SuppressJsonPropertiesFilter {

    private final Policy policy;

    public RemoveJsonPropertiesUnlessPolicyPresentFilter(final JsonConverter jsonConverter, final List<String> jsonProperties, final Policy policy) {
        super(jsonConverter, jsonProperties);
        this.policy = policy;
    }

    @Override
    protected boolean shouldPropertyFilteredOut(final String jsonProperty, final MutableRequest request, final MutableResponse response) {
        return !request.policyIs(policy) && super.shouldPropertyFilteredOut(jsonProperty, request, response);
    }
}
