package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Map;

import static com.ft.up.apipolicy.configuration.Policy.INCLUDE_PROVENANCE;

/**
 * StripProvenanceFilter
 *
 * @author Simon.Gibbs
 */
public class StripProvenanceFilter implements ApiFilter {
    private JsonConverter jsonConverter;

    public StripProvenanceFilter(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

        MutableResponse response = chain.callNextFilter(request);

        if(request.policyIs(INCLUDE_PROVENANCE)) {
            return response;
        }
        if(response.getStatus()!=200) {
            return response;
        }
        if(!jsonConverter.isJson(response)) {
            return response;
        }



        Map<String,Object> content = jsonConverter.readEntity(response);

        content.remove("publishReference");

        jsonConverter.replaceEntity(response,content);

        return response;
    }
}
