package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.List;
import java.util.Map;

public class NotificationsProvenanceFilter implements ApiFilter {

    public static final String NOTIFICATIONS = "notifications";
    public static final String PROVENANCE = "publishReference";
    private JsonConverter jsonConverter;
    private Policy includeProvenance;

    public NotificationsProvenanceFilter(JsonConverter jsonConverter, Policy includeProvenance) {
        this.jsonConverter = jsonConverter;
        this.includeProvenance = includeProvenance;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (shouldSkipFilteringProvenanceProperty(request, response)) {
            return response;
        }

        final Map<String, Object> content = jsonConverter.readEntity(response);
        if (typeCheckFails(content)) {
            throw new FilterException(new IllegalStateException("Notifications json response is not in expected format."));
        }

        List<Map<String, Object>> notifications = (List) content.get(NOTIFICATIONS);
        for (Map<String, Object> notification : notifications) {
            notification.remove(PROVENANCE);
        }
        jsonConverter.replaceEntity(response, content);
        return response;
    }

    private boolean shouldSkipFilteringProvenanceProperty(MutableRequest request, MutableResponse response) {
        return response.getStatus() != 200 || !jsonConverter.isJson(response) || request.policyIs(includeProvenance);
    }

    private boolean typeCheckFails(Map<String, Object> content) {
        return !(content.get(NOTIFICATIONS) instanceof List);
    }
}
