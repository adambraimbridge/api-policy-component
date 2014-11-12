package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.HashMap;
import java.util.Map;

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

        Map<String,String> contentOrigin = expectOriginIn(content);

        String originSystem = contentOrigin.get("originatingSystem");

        if(originSystem == null) {
            return response;
        }

        String originatingIdentifier = contentOrigin.get("originatingIdentifier");

        String template = urlTemplates.get(originSystem);

        String webUrl = template.replace("{{originatingIdentifier}}", originatingIdentifier);

        content.put("webUrl",webUrl);

        jsonConverter.replaceEntity(response, content);

        return response;

    }

    @SuppressWarnings("unchecked")
    private Map<String, String> expectOriginIn(HashMap<String, Object> content) {
        return (Map<String, String>) content.get("contentOrigin");
    }

}
