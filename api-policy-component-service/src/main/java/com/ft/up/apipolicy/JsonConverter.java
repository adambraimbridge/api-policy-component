package com.ft.up.apipolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.filters.FilterException;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JsonConverter
 *
 * @author Simon.Gibbs
 */
public class JsonConverter {

    public static JsonConverter testConverter() {
        return new JsonConverter(new ObjectMapper());
    }

    public static final TypeReference<LinkedHashMap<String,Object>> JSON_MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {};
    public static final TypeReference<LinkedHashMap<String,Object>[]> JSON_ARRAY_TYPE = new TypeReference<LinkedHashMap<String, Object>[]>() {};
    private final ObjectMapper objectMapper;

    public JsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isJson(MutableResponse response) {

        if(response.getContentType()==null) {
            throw new NullPointerException("Content-Type not set");
        }

        return response.getContentType().startsWith(MediaType.APPLICATION_JSON);

    }

    public Map<String, Object> readEntity(MutableResponse response) {
        try {
            return objectMapper.readValue(response.getEntity(), JSON_MAP_TYPE);
        } catch (IOException e) {
            throw new FilterException(e);
        }
    }


    public void replaceEntity(MutableResponse response, Map<String, Object> content) {
        try {
            response.setEntity(objectMapper.writeValueAsBytes(content));
        } catch (JsonProcessingException e) {
            throw new FilterException(e);
        }
    }
}
