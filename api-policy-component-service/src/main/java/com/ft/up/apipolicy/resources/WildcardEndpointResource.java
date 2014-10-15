package com.ft.up.apipolicy.resources;


import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.SortedSet;
import java.util.regex.Matcher;
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
    public Response service(@Context final HttpServletRequest request, @Context final UriInfo uriInfo) {

        MutableRequest mutableRequest = translator.translateFrom(request);

        for(KnownEndpoint candidate : knownEndpoints) {

            Pattern compiledUriRegex = candidate.getUriPattern();

            String pathPart = uriInfo.getBaseUri().getPath() + uriInfo.getPath();
            Matcher matcher = compiledUriRegex.matcher(pathPart);

            if(matcher.find()) {

                LOGGER.debug("Matched request to pipeline=" + candidate);

                HttpPipelineChain chain = new HttpPipelineChain(candidate.getPipeline());
                MutableResponse clientResponse = chain.callNextFilter(mutableRequest);

                return translator.translateTo(clientResponse);
            }
        }

        return Response.serverError().build();
    }



}
