package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.HashMap;

public class SuppressMarkupFilter implements ApiFilter{

    public static final String BODY_XML_KEY = "bodyXML";
    private final JsonConverter jsonConverter;

    public SuppressMarkupFilter(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

        MutableResponse response = chain.callNextFilter(request);

        HashMap<String, Object> content = jsonConverter.readEntity(response);

        String body = ((String)content.get(BODY_XML_KEY));

        content.put(BODY_XML_KEY, body + "wibble");

        jsonConverter.replaceEntity(response, content);

        return response;
    }
}
