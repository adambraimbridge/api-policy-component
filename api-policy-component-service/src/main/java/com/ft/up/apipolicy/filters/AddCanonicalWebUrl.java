package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class AddCanonicalWebUrl implements ApiFilter {

    private static final String BODY_KEY = "bodyXML";
    private static final String TYPE_KEY = "type";
    private static final String TYPES_KEY = "types";
    private static final String ARTICLE_TYPE = "http://www.ft.com/ontology/content/Article";
    private static final String IDENTIFIERS_KEY = "identifiers";
    private static final String AUTHORITY_KEY = "authority";
    private static final String NEXT_VIDEO_EDITOR_AUTHORITY = "http://api.ft.com/system/NEXT-VIDEO-EDITOR";
    private static final String UUID_KEY = "uuid";
    private static final String CANONICAL_WEB_URL_KEY = "canonicalWebUrl";

    private final String canonicalWebUrlTemplate;
    private JsonConverter jsonConverter;

    public AddCanonicalWebUrl(String canonicalWebUrlTemplate, JsonConverter jsonConverter) {
        this.canonicalWebUrlTemplate = canonicalWebUrlTemplate;
        this.jsonConverter = jsonConverter;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (isEligibleForCanonicalWebUrl(response)) {
            final Map<String, Object> content = extractContent(response);
            if (!content.containsKey(CANONICAL_WEB_URL_KEY)) {
                return createResponseWithCanonicalWebUrlCompleted(response, content);
            }
        }
        return response;
    }

    private boolean isEligibleForCanonicalWebUrl(final MutableResponse response) {
        if (isNotOKResponse(response) || isNotJson(response)) {
            return false;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);

        boolean bodyPresent = content.containsKey(BODY_KEY);

        return bodyPresent || isArticle(content) || isVideo(content);
    }

    private boolean isNotOKResponse(final MutableResponse response) {
        return Response.Status.OK.getStatusCode() != response.getStatus();
    }

    private boolean isNotJson(final MutableResponse response) {
        return !jsonConverter.isJson(response);
    }

    private boolean isArticle(Map<String, Object> content) {
        Object type = content.get(TYPE_KEY);
        Object types = content.get(TYPES_KEY);
        return ARTICLE_TYPE.equals(type)
                || ((types instanceof Collection) && ((Collection) types).contains(ARTICLE_TYPE));
    }

    private boolean isVideo(Map<String, Object> content) {
        if (content.containsKey(IDENTIFIERS_KEY)) {
            final Object identifiersRaw = content.get(IDENTIFIERS_KEY);
            if (identifiersRaw instanceof Collection) {
                Iterator identifiersIterator = ((Collection) identifiersRaw).iterator();
                if (identifiersIterator.hasNext()) {
                    Object identifierRaw = identifiersIterator.next();
                    if (identifierRaw instanceof Map) {
                        Map identifier = ((Map) identifierRaw);
                        if (identifier.containsKey(AUTHORITY_KEY)) {
                            final Object authority = identifier.get(AUTHORITY_KEY);
                            if (NEXT_VIDEO_EDITOR_AUTHORITY.equals(authority)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Map<String, Object> extractContent(final MutableResponse response) {
        return jsonConverter.readEntity(response);
    }

    private MutableResponse createResponseWithCanonicalWebUrlCompleted(
            final MutableResponse response,
            final Map<String, Object> content) {
        Object uuid = content.get(UUID_KEY);
        String canonicalWebUrl = String.format(canonicalWebUrlTemplate, uuid);
        if (canonicalWebUrl != null) {
            content.put(CANONICAL_WEB_URL_KEY, canonicalWebUrl);
            jsonConverter.replaceEntity(response, content);
        }
        return response;
    }
}
