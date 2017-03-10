package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class RemoveJsonPropertiesUnlessPolicyPresentFilter extends SuppressJsonPropertiesFilter {

    private final Policy policy;

    public RemoveJsonPropertiesUnlessPolicyPresentFilter(final JsonConverter jsonConverter, final Policy policy, final String... jsonProperties) {
        super(jsonConverter, jsonProperties);
        this.policy = policy;
    }

    @Override
    protected boolean shouldPropertyFilteredOut(final String jsonProperty, final MutableRequest request, final MutableResponse response) {
        return !request.policyIs(policy) && super.shouldPropertyFilteredOut(jsonProperty, request, response);
    }
}
