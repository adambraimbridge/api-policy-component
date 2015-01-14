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

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> identifiers = (List<Map<String, Object>>) content
				.get("identifiers");

		for (Map<String, Object> map : identifiers) {
			String authority = (String) map.get("authority");
			String value = (String) map.get("identifierValue");

			String template = urlTemplates.get(authority);
			if (template != null) {
				String webUrl = template.replace("{{originatingIdentifier}}",
						value);
				content.put("webUrl", webUrl);
				jsonConverter.replaceEntity(response, content);
				return response;
			}
		}

        return response;

    }
    
}
