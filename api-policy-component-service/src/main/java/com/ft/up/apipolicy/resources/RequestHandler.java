package com.ft.up.apipolicy.resources;

import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.google.common.base.Joiner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ft.api.jaxrs.errors.ServerError.status;
import static com.ft.up.apipolicy.pipeline.HttpPipeline.POLICY_HEADER_NAME;
import static com.ft.up.apipolicy.pipeline.MutableResponse.VARY_HEADER;
import static java.util.Collections.singletonMap;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ResponseBuilder;
import static javax.ws.rs.core.Response.serverError;

public class RequestHandler {

    public static final Joiner COMMA_DELIMITED = Joiner.on(", ");
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
                throw status(504).error(che.getMessage()).exception(che);
            } else {
                throw status(503).error(che.getMessage()).exception(che);
            }
        } catch (UnsupportedRequestException ure) {
            return Response.status(SC_METHOD_NOT_ALLOWED).type(APPLICATION_JSON)
                    .entity(singletonMap("message", ure.getMessage()))
                    .build();
        }
        if (response == null) {
            return serverError().build();
        }

        Set<String> varyByHeadersSet = response.getHeadersInVaryList();

        varyByHeadersSet.add(POLICY_HEADER_NAME);

        ResponseBuilder result = translator.translateTo(response);
        result.header(VARY_HEADER, null);
        result.header(VARY_HEADER, COMMA_DELIMITED.join(varyByHeadersSet));

        return result.build();
    }

    private MutableResponse handleRequest(MutableRequest request, String path) {

        List<KnownEndpoint> matchedCandidates = new ArrayList<>();

        for (KnownEndpoint candidate : knownEndpoints) {
            Pattern compiledUriRegex = candidate.getUriPattern();

            Matcher matcher = compiledUriRegex.matcher(path);

            if (matcher.find()) {
                matchedCandidates.add(candidate);
                HttpPipelineChain chain = new HttpPipelineChain(candidate.getPipeline());
                return chain.callNextFilter(request);
            }
        }
        throw new UnsupportedRequestException(path, request.getHttpMethod());
    }
}
