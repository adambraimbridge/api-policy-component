package com.ft.up.apipolicy.filters;

import java.util.List;
import java.util.Map;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.Response.Status;

public class WebUrlCalculator implements ApiFilter {

    private static final String BODY_KEY = "bodyXML";
    private static final String WEB_URL_KEY = "webUrl";

    private final Map<String, String> urlTemplates;
    private JsonConverter jsonConverter;

    public WebUrlCalculator(final Map<String, String> urlTemplates, final JsonConverter converter) {
        this.urlTemplates = urlTemplates;
        this.jsonConverter = converter;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse originalResponse = chain.callNextFilter(request);
        if (isEligibleForWebUrl(originalResponse)) {
            final Map<String, Object> content = extractContent(originalResponse);
            return createResponseWithWebUrlCompleted(originalResponse, content);
        }
        return originalResponse;
    }

    private boolean isEligibleForWebUrl(final MutableResponse response) {
        if (isNotOKResponse(response) || isNotJson(response)) {
            return false;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        return content.containsKey(BODY_KEY);
    }

    private boolean isNotOKResponse(final MutableResponse response) {
        return Status.OK.getStatusCode() != response.getStatus();
    }

    private boolean isNotJson(final MutableResponse response) {
        return !jsonConverter.isJson(response);
    }

    private Map<String, Object> extractContent(final MutableResponse response) {
        return jsonConverter.readEntity(response);
    }

    private MutableResponse createResponseWithWebUrlCompleted(final MutableResponse response,
            final Map<String, Object> content) {
        final String webUrl = generateWebUrlFromIdentifiers(content);
        if (webUrl != null) {
            content.put(WEB_URL_KEY, webUrl);
            jsonConverter.replaceEntity(response, content);
            return response;
        }
        return response;
    }

    private String generateWebUrlFromContentOrigin(Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        Map<String, String> contentOrigin = (Map<String, String>) content.get("contentOrigin");
        if (contentOrigin != null) {
            String authority = contentOrigin.get("originatingSystem");
            String value = contentOrigin.get("originatingIdentifier");
            String template = urlTemplates.get(authority);
            if (template != null) {
                return template.replace("{{originatingIdentifier}}", value);
            }
        }
        return null;
    }

    private String generateWebUrlFromIdentifiers(Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> identifiers = (List<Map<String, String>>) content.get("identifiers");
        if (identifiers != null) {
            for (Map<String, String> map : identifiers) {
                String authority = map.get("authority");
                String value = map.get("identifierValue");
                String template = urlTemplates.get(authority);
                if (template != null) {
                    return template.replace("{{originatingIdentifier}}", value);
                }
            }
        }
        return generateWebUrlFromContentOrigin(content);
    }
}
