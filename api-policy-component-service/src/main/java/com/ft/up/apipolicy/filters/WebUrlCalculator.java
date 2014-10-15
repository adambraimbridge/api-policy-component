package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * WebUrlCalculator
 *
 * @author Simon.Gibbs
 */
public class WebUrlCalculator implements ApiFilter {

    public static final TypeReference<HashMap<String,Object>> JSON_MAP_TYPE = new TypeReference<HashMap<String, Object>>() {
    };


    private final Map<String, String> urlTemplates;
    private ObjectMapper objectMapper;

    public WebUrlCalculator(final Map<String, String> urlTemplates, ObjectMapper objectMapper) {
        this.urlTemplates = urlTemplates;
        this.objectMapper = objectMapper;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {


        MutableResponse response = chain.callNextFilter(request);

        HashMap<String, Object> content = null;
        try {
            content = objectMapper.readValue(response.getEntity(), jsonMapType());
        } catch (IOException e) {
            throw new FilterException(e);
        }

        Map<String,String> contentOrigin = (Map<String, String>) content.get("contentOrigin");

        String originSystem = contentOrigin.get("originatingSystem");
        String originatingIdentifier = contentOrigin.get("originatingIdentifier");

        String template = urlTemplates.get(originSystem);

        String webUrl = template.replace("{{originatingIdentifier}}",originatingIdentifier);

        content.put("webUrl",webUrl);

        try {
            response.setEntity(objectMapper.writeValueAsBytes(content));
        } catch (JsonProcessingException e) {
            throw new FilterException(e);
        }

        return response;

    }

    private TypeReference<HashMap<String, Object>> jsonMapType() {
        return JSON_MAP_TYPE;
    }

}
