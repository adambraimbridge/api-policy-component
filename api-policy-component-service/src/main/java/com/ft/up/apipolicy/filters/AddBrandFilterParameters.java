package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;

/**
 * AddBrandFilterParameters
 *
 * @author Simon.Gibbs
 */
public class AddBrandFilterParameters implements ApiFilter {

    public static final String REQUEST_URL_KEY = "requestUrl";
    private JsonConverter converter;

    public AddBrandFilterParameters(JsonConverter jsonConverter) {
        this.converter = jsonConverter;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

        /*
        <Step>
        <FaultRules/>
        <Condition>verifyapikey.ValidateAPIKey.app.FASTFT_CONTENT_ONLY=true</Condition>
        <Name>AddParamForBrandFastFT</Name>
        </Step>
        */

        if(request.policyIs("FASTFT_CONTENT_ONLY")) {
            request.getQueryParameters().add("forBrand","http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54");
        }

        /*
        <Step>
        <FaultRules/>
        <Condition>(verifyapikey.ValidateAPIKey.apiproduct.EXCLUDE_FASTFT_CONTENT=true) AND (verifyapikey.ValidateAPIKey.app.FASTFT_CONTENT_ONLY!=true)
        AND (verifyapikey.ValidateAPIKey.app.INCLUDE_FASTFT_CONTENT!=true)</Condition>
        <Name>AddParamNotForBrandFastFT</Name>
        </Step>
        */

        if(request.policyIs("EXCLUDE_FASTFT_CONTENT") && !request.policyIs("INCLUDE_FASTFT_CONTENT") && !request.policyIs("FASTFT_CONTENT_ONLY")) {
            request.getQueryParameters().add("notForBrand","http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54");
        }


        MutableResponse response = chain.callNextFilter(request);

        HashMap<String, Object> content = converter.readEntity(response);

        UriBuilder requestUriBuilder = UriBuilder.fromUri((String)content.get(REQUEST_URL_KEY));
        requestUriBuilder.replaceQueryParam("notForBrand", null);
        requestUriBuilder.replaceQueryParam("forBrand",null);

        content.put(REQUEST_URL_KEY, requestUriBuilder.build());


        converter.replaceEntity(response, content);

        return response;

    }

}
