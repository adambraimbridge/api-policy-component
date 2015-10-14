package com.ft.up.apipolicy.filters;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;


public class WebUrlCalculator implements ApiFilter {
    private static final String BODY_KEY = "bodyXML";
    private static final String WEB_URL_KEY = "webUrl";
    private static final String TYPE_KEY = "type";
    
    private static final String ARTICLE_TYPE = "http://www.ft.com/ontology/content/Article";
    
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
        
        boolean bodyPresent = content.containsKey(BODY_KEY);
        
        String documentType = (String)content.get(TYPE_KEY);
        
        return bodyPresent
                || ARTICLE_TYPE.equals(documentType);
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

    private String generateWebUrlFromIdentifiers(Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> identifiers = (List<Map<String, String>>) content.get("identifiers");
        if (identifiers != null) {
            for (Map<String, String> map : identifiers) {
                String authority = map.get("authority");
                String value = map.get("identifierValue");
                for(String key : urlTemplates.keySet()){
                    if (Pattern.matches(key, authority)){

                        String template = urlTemplates.get(key);
                        if (template != null) {
                            return template.replace("{{identifierValue}}", value);
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }
}
