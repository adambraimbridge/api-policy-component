package com.ft.up.apipolicy.filters;


import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;

import java.util.HashMap;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_RICH_CONTENT;

public class SuppressRichContentMarkupFilter implements ApiFilter {

    public static final String BODY_XML_KEY = "bodyXML";
	private final JsonConverter jsonConverter;
    private BodyProcessingFieldTransformer transformer;

    public SuppressRichContentMarkupFilter(JsonConverter jsonConverter, BodyProcessingFieldTransformer transformer) {
        this.jsonConverter = jsonConverter;
        this.transformer = transformer;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

        MutableResponse response = chain.callNextFilter(request);

        if(request.policyIs(INCLUDE_RICH_CONTENT)) {
            return response;
        }

        if(!jsonConverter.isJson(response)) {
            return response;
        }

        HashMap<String, Object> content = jsonConverter.readEntity(response);

        String body = ((String)content.get(BODY_XML_KEY));

        if(body == null) {
            return response;
        }

        body = transformer.transform(body, request.getTransactionId());

        content.put(BODY_XML_KEY, body);

        jsonConverter.replaceEntity(response, content);

        return response;
    }
}
