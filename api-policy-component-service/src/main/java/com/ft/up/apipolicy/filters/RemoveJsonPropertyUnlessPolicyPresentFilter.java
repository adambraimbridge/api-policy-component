package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

public class RemoveJsonPropertyUnlessPolicyPresentFilter extends SuppressJsonPropertyFilter {

    private final Policy policy;

    public RemoveJsonPropertyUnlessPolicyPresentFilter(final JsonConverter jsonConverter, final String jsonProperty, final Policy policy) {
        super(jsonConverter, jsonProperty);
        this.policy = policy;
    }

    @Override
    protected boolean shouldPropertyFilteredOut(MutableRequest request, MutableResponse response) {
        return (!request.policyIs(policy) && super.shouldPropertyFilteredOut(request, response));
    }
}
