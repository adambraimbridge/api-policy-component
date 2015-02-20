package com.ft.up.apipolicy.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

/**
 * WebUrlCalculator
 *
 * @author Simon.Gibbs
 */
public class WebUrlCalculator implements ApiFilter {

    private static final String TYPE_KEY = "type";
    private static final String TYPE_VALUE_ARTICLE = "http://www.ft.com/ontology/content/Article";

    private final Map<String, String> urlTemplates;
    private JsonConverter jsonConverter;

    public WebUrlCalculator(final Map<String, String> urlTemplates, JsonConverter converter) {
        this.urlTemplates = urlTemplates;
        this.jsonConverter = converter;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {


        MutableResponse response = chain.callNextFilter(request);

        if(response.getStatus()!=200) {
            return response;
        }

        if(!jsonConverter.isJson(response)) {
            return response;
		}

		HashMap<String, Object> content = jsonConverter.readEntity(response);
        if (isNotArticleType(content)) {
            return response;
        }

		String webUrl = generateWebUrlFromIdentifiers(content);
		if(webUrl != null ){
			content.put("webUrl", webUrl);
			jsonConverter.replaceEntity(response, content);
			return response;
		}

        return response;
    }

    private boolean isNotArticleType(final Map<String, Object> content) {
        return !content.containsKey(TYPE_KEY) || TYPE_VALUE_ARTICLE != content.get(TYPE_KEY);
    }

	private String generateWebUrlFromIdentifiers(Map<String, Object> content){
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> identifiers = (List<Map<String, Object>>) content.get("identifiers");
		if(identifiers != null) {

			for (Map<String, Object> map : identifiers) {
				String authority = (String) map.get("authority");
				String value = (String) map.get("identifierValue");

				String template = urlTemplates.get(authority);
				if (template != null) {
					return template.replace("{{originatingIdentifier}}", value);
				}
			}
		}
        return null;
	}

}
