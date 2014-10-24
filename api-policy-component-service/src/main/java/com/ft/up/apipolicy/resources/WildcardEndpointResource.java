package com.ft.up.apipolicy.resources;


import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Path("/{path:.*}")
public class WildcardEndpointResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WildcardEndpointResource.class);
    public static final Joiner COMMA_DELIMITED = Joiner.on(", ");

    private SortedSet<KnownEndpoint> knownEndpoints;
    private MutableHttpTranslator translator;

    public WildcardEndpointResource(MutableHttpTranslator translator, SortedSet<KnownEndpoint> knownEndpoints) {
        this.translator = translator;
		this.knownEndpoints = knownEndpoints;
    }

    @GET
    @Consumes(MediaType.WILDCARD) @Produces(MediaType.WILDCARD)
    public Response service(@Context final HttpServletRequest request, @Context final UriInfo uriInfo) {

        MutableRequest mutableRequest = translator.translateFrom(request);

        String pathPart = uriInfo.getBaseUri().getPath() + uriInfo.getPath();

        MutableResponse response = handleRequest(mutableRequest,pathPart);

        if(response==null) {
            return Response.serverError().build();
        }

        Set<String> varyByHeadersSet = response.getHeadersInVaryList();

        varyByHeadersSet.add(HttpPipeline.POLICY_HEADER_NAME);

        Response.ResponseBuilder result = translator.translateTo(response);
        result.header(MutableResponse.VARY_HEADER,null);
        result.header(MutableResponse.VARY_HEADER,COMMA_DELIMITED.join(varyByHeadersSet));

        return result.build();
    }

    private MutableResponse handleRequest(MutableRequest request, String path) {
        for(KnownEndpoint candidate : knownEndpoints) {

            Pattern compiledUriRegex = candidate.getUriPattern();

            Matcher matcher = compiledUriRegex.matcher(path);

            if(matcher.find()) {

                LOGGER.debug("Matched request to pipeline=" + candidate);

                HttpPipelineChain chain = new HttpPipelineChain(candidate.getPipeline());
                return chain.callNextFilter(request);
            }
        }

        return null;
    }

}
