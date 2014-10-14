package com.ft.up.apipolicy.resources;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpToServletsHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.PipelineConfiguration;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


@Path("/{path:.*}")
public class WildcardEndpointResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WildcardEndpointResource.class);

    Map<KnownEndpoints, HttpPipeline> pipelines;
    private MutableHttpToServletsHttpTranslator translator;

    public WildcardEndpointResource(PipelineConfiguration config, RequestForwarder forwarder, MutableHttpToServletsHttpTranslator translator, ObjectMapper objectMapper) {
        this.translator = translator;
        pipelines = new HashMap<>();

        for(KnownEndpoints endpoint : KnownEndpoints.values()) {
            pipelines.put(endpoint,endpoint.pipeline(config, forwarder, objectMapper));
        }
    }

    @GET @Consumes(MediaType.WILDCARD) @Produces(MediaType.WILDCARD)
    public void service(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @Context final UriInfo uriInfo) {

        MutableRequest mutableRequest = translator.translateFrom(request);

        for(KnownEndpoints candidate : pipelines.keySet()) {
            if(Pattern.matches(candidate.getUriRegex(),uriInfo.getAbsolutePath().toString())) {

                LOGGER.debug("Matched request to pipeline=" + candidate.name());

                HttpPipelineChain chain = new HttpPipelineChain(pipelines.get(candidate));
                MutableResponse clientResponse = chain.callNextFilter(mutableRequest);

                translator.writeMutableResponseIntoActualResponse(clientResponse, response);


                break; //  STOP! Cannot use more than one pipeline.
            }
        }

    }



}
