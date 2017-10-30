package com.ft.up.apipolicy.resources;

import com.ft.api.jaxrs.errors.ServerError;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler {

    public static final Joiner COMMA_DELIMITED = Joiner.on(", ");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
    private MutableHttpTranslator translator;
    private Set<KnownEndpoint> knownEndpoints;

    public RequestHandler(MutableHttpTranslator translator, Set<KnownEndpoint> knownEndpoints) {
        this.translator = translator;
        this.knownEndpoints = knownEndpoints;
    }

    public Response handleRequest(HttpServletRequest request, UriInfo uriInfo) {
        MutableRequest mutableRequest = translator.translateFrom(request);

        String pathPart = uriInfo.getBaseUri().getPath() + uriInfo.getPath();
        MutableResponse response;
        try {
            response = handleRequest(mutableRequest, pathPart);
        } catch (ClientErrorException che) {
            if (che.getCause() instanceof SocketTimeoutException) {
                throw ServerError.status(504).error(che.getMessage()).exception(che);
            } else {
                throw ServerError.status(503).error(che.getMessage()).exception(che);
            }
        } catch (UnsupportedRequestException ure) {
            return Response.status(HttpServletResponse.SC_METHOD_NOT_ALLOWED).type(MediaType.APPLICATION_JSON)
                .entity(Collections.singletonMap("message", ure.getMessage()))
                .build();
        }
        if (response == null) {
            return Response.serverError().build();
        }

        Set<String> varyByHeadersSet = response.getHeadersInVaryList();

        varyByHeadersSet.add(HttpPipeline.POLICY_HEADER_NAME);

        Response.ResponseBuilder result = translator.translateTo(response);
        result.header(MutableResponse.VARY_HEADER, null);
        result.header(MutableResponse.VARY_HEADER, COMMA_DELIMITED.join(varyByHeadersSet));

        return result.build();
    }

    private MutableResponse handleRequest(MutableRequest request, String path) {
        for (KnownEndpoint candidate : knownEndpoints) {
            Pattern compiledUriRegex = candidate.getUriPattern();

            Matcher matcher = compiledUriRegex.matcher(path);

            if (matcher.find()) {

                LOGGER.debug("Matched request to pipeline=" + candidate);

                HttpPipelineChain chain = new HttpPipelineChain(candidate.getPipeline());
                return chain.callNextFilter(request);
            }
        }

        throw new UnsupportedRequestException(path, request.getHttpMethod());
    }
}