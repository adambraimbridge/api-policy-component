package com.ft.up.apipolicy;

import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.util.FluentLoggingWrapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.*;

import static com.ft.up.apipolicy.util.FluentLoggingWrapper.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * JerseyRequestForwarder
 *
 * @author Simon.Gibbs
 */
public class JerseyRequestForwarder implements RequestForwarder {

    private final Client client;
    private final EndpointConfiguration varnish;

    private FluentLoggingWrapper log;

    public JerseyRequestForwarder(Client client, EndpointConfiguration varnish) {
        this.client = client;
        this.varnish = varnish;
        log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString());
    }

    @Override
    public MutableResponse forwardRequest(MutableRequest request) {
        log.withMethodName("forwardRequest")
                .withTransactionId(request.getTransactionId());

        UriBuilder builder = UriBuilder.fromPath(request.getAbsolutePath())
                .scheme("http")
                .host(varnish.getHost())
                .port(varnish.getPort());

        extractQueryParameters(request, log, builder);

        Builder resource = client.target(builder.build()).request();

        resource = extractHeaders(request, log, resource);

        log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString());
        log.withMethodName("forwardRequest")
                .withTransactionId(request.getTransactionId())
                .withRequest(request)
                .withField(URI, request.getAbsolutePath())
                .withField(MESSAGE, "Forwarding request")
                .build().logInfo();

        return constructMutableResponse(request, log, resource);
    }

    private MutableResponse constructMutableResponse(MutableRequest request, FluentLoggingWrapper log, Builder resource) {
        Response clientResponse;

        String requestEntity = request.getRequestEntityAsString();

        if (StringUtils.isNotEmpty(requestEntity)) {
            clientResponse = resource.build(request.getHttpMethod(), Entity.json(requestEntity)).invoke();
        } else {
            clientResponse = resource.build(request.getHttpMethod()).invoke();
        }
        MutableResponse result = new MutableResponse();

        try {
            byte[] responseEntity = null;
            try {
                if (clientResponse.hasEntity()) {
                    responseEntity = clientResponse.readEntity(byte[].class);
                }
            } catch (IllegalStateException e) {
                // thrown if there is an IOException in hasEntity()
                log.withMethodName("constructMutableResponse")
                        .withField(MESSAGE, "unable to obtain a response entity")
                        .withException(e)
                        .build().logError();
            }

            int responseStatus = handleResponseStatus(log, clientResponse, result, responseEntity);

            result.setStatus(responseStatus);
            result.setHeaders(clientResponse.getHeaders());
        } finally {
            clientResponse.close();
//            MDC.remove("transaction_id");
        }

        return result;
    }

    private int handleResponseStatus(FluentLoggingWrapper log, Response clientResponse, MutableResponse result, byte[] responseEntity) {
        int responseStatus = clientResponse.getStatus();
        if ((responseStatus >= 500)
                && ((responseEntity == null) || (responseEntity.length == 0))) {

            log.withMethodName("handleResponseStatus")
                    .withField(MESSAGE, "server error response has no entity")
                    .build().logDebug();

            responseEntity = "{\"message\":\"server error\"}" .getBytes(UTF_8);
        }

        if (responseEntity != null) {
            result.setEntity(responseEntity);
        }
        return responseStatus;
    }

    private Builder extractHeaders(MutableRequest request, FluentLoggingWrapper log, Builder resource) {
        Map<String, List<String>> headerParameters = new HashMap<>();

        MultivaluedMap<String, String> headers = request.getHeaders();
        for (String headerName : headers.keySet()) {
            List<String> headerParameterValues = new ArrayList<>();
            for (String value : headers.get(headerName)) {
                resource = resource.header(headerName, value);
                headerParameterValues.add(value);
            }
            headerParameters.put(headerName, headerParameterValues);
        }
        log.withMethodName("extractHeaders");
        logForwarderCustomMessage(log, "Sending Header: ", headerParameters);

        return resource;
    }

    private void extractQueryParameters(MutableRequest request, FluentLoggingWrapper log, UriBuilder builder) {
        Map<String, List<String>> queryParameters = new HashMap<>();

        for (String parameterName : request.getQueryParameters().keySet()) {
            List<String> queryParameterValues = new ArrayList<>();
            for (String value : request.getQueryParameters().get(parameterName)) {
                builder.queryParam(parameterName, value);
                queryParameterValues.add(value);
            }
            queryParameters.put(parameterName, queryParameterValues);
        }
        log.withMethodName("extractQueryParameters");
        logForwarderCustomMessage(log, "Sending query parameters: ", queryParameters);
    }

    private void logForwarderCustomMessage(FluentLoggingWrapper log, String customMessage,
                                           Map<String, List<String>> logArguments) {
        log.withField(MESSAGE, customMessage + logArguments.keySet().toString() +
                        " : " + logArguments.values().toString())
                .build().logDebug();
    }
}
