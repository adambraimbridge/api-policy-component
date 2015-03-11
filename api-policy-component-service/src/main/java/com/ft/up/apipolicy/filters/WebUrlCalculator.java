package com.ft.up.apipolicy.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

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

		String webUrl = generateWebUrlFromIdentifiers(content);
		if(webUrl != null ){
			content.put("webUrl", webUrl);
			jsonConverter.replaceEntity(response, content);
			return response;
		}

        return response;
    }

	private String generateWebUrlFromContentOrigin(Map<String, Object> content){
		@SuppressWarnings("unchecked")
		Map<String, String> contentOrigin = (Map<String, String>) content.get("contentOrigin");
		if(contentOrigin != null) {
            String authority = contentOrigin.get("originatingSystem");
            String value = contentOrigin.get("originatingIdentifier");
            for(String key : urlTemplates.keySet()){
                if(key == null || authority == null){
                    return null;
                }
                if (Pattern.matches(key, authority)){
                    String template = urlTemplates.get(key);
                    if (template != null) {
                        return template.replace("{{originatingIdentifier}}", value);
                    }
                    break;
                }
            }
		}
		return null;
	}

    
    private String generateWebUrlFromIdentifiers(Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> identifiers = (List<Map<String, Object>>) content.get("identifiers");
        if (identifiers != null) {
            for (Map<String, Object> map : identifiers) {
                String authority = (String) map.get("authority");
                String value = (String) map.get("identifierValue");
                for(String key : urlTemplates.keySet()){
                    if (Pattern.matches(key, authority)){

                        String template = urlTemplates.get(key);
                        if (template != null) {
                            return template.replace("{{originatingIdentifier}}", value);
                        }
                        break;
                    }
                }
            }
        }
        return generateWebUrlFromContentOrigin(content);
    }
}
