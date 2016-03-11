package com.ft.up.apipolicy.resources;

import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.jaxrs.errors.ServerError;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import com.google.common.base.Joiner;

import com.sun.jersey.api.client.ClientHandlerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public class RequestHandler {

    public static final Joiner COMMA_DELIMITED = Joiner.on(", ");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
    private MutableHttpTranslator translator;
    private SortedSet<KnownEndpoint> knownEndpoints;

    public RequestHandler(MutableHttpTranslator translator, SortedSet<KnownEndpoint> knownEndpoints) {
        this.translator = translator;
        this.knownEndpoints = knownEndpoints;
    }

    public Response handleRequest(HttpServletRequest request, UriInfo uriInfo) {


        MutableRequest mutableRequest = translator.translateFrom(request);

        String pathPart = uriInfo.getBaseUri().getPath() + uriInfo.getPath();
        MutableResponse response = null;
        try {
            response = handleRequest(mutableRequest, pathPart);
        } catch (ClientHandlerException che) {
            if (che instanceof ClientHandlerException) {
                if (che.getCause() instanceof SocketTimeoutException) {
                    throw ServerError.status(504).error(che.getMessage()).exception(che);
                } else {
                    throw ServerError.status(503).error(che.getMessage()).exception(che);
                }
            }
        } catch (UnsupportedRequestException ure) {
            throw ClientError.status(405).error(ure.getMessage()).exception(ure);
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