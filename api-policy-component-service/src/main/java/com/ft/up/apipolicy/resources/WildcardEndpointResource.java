package com.ft.up.apipolicy.resources;


import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Pattern;


@Path("/{path:.*}")
public class WildcardEndpointResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WildcardEndpointResource.class);

    private SortedSet<KnownEndpoint> knownEndpoints;
    private MutableHttpTranslator translator;

    public WildcardEndpointResource(MutableHttpTranslator translator, SortedSet<KnownEndpoint> knownEndpoints) {
        this.translator = translator;
		this.knownEndpoints = knownEndpoints;
    }

    @GET @Consumes(MediaType.WILDCARD) @Produces(MediaType.WILDCARD)
    public void service(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @Context final UriInfo uriInfo) {

        MutableRequest mutableRequest = translator.translateFrom(request);

        for(KnownEndpoint candidate : knownEndpoints) {
            if(Pattern.matches(candidate.getUriRegex(), uriInfo.getAbsolutePath().toString())) {

                LOGGER.debug("Matched request to pipeline=" + candidate);

                HttpPipelineChain chain = new HttpPipelineChain(candidate.getPipeline());
                MutableResponse clientResponse = chain.callNextFilter(mutableRequest);

                translator.writeMutableResponseIntoActualResponse(clientResponse, response);

                break; //  STOP! Cannot use more than one pipeline.
            }
        }

    }



}
