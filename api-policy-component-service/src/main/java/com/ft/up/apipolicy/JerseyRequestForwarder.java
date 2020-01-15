package com.ft.up.apipolicy;

import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.util.FluentLoggingWrapper;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public JerseyRequestForwarder(Client client, EndpointConfiguration varnish) {
        this.client = client;
        this.varnish = varnish;
    }

    @Override
    public MutableResponse forwardRequest(MutableRequest request) {


        UriBuilder builder = UriBuilder.fromPath(request.getAbsolutePath())
                .scheme("http")
                .host(varnish.getHost())
                .port(varnish.getPort());

        extractQueryParameters(request, builder);

        Builder resource = client.target(builder.build()).request();

        resource = extractHeaders(request, resource);

        FluentLoggingWrapper log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString())
                .withMethodName("forwardRequest")
                .withTransactionId(request.getTransactionId())
                .withRequest(request)
                .withField(URI, request.getAbsolutePath())
                .withField(MESSAGE, "Request")
                .build().logInfo();

        return constructMutableResponse(request, resource);
    }

    private MutableResponse constructMutableResponse(MutableRequest request, Builder resource) {
        FluentLoggingWrapper log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString());

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

            int responseStatus = handleResponseStatus(clientResponse, result, responseEntity);

            result.setStatus(responseStatus);
            result.setHeaders(clientResponse.getHeaders());
        } finally {
            log.withClassName(this.getClass().toString())
                    .withMethodName("constructMutableResponse")
                    .withTransactionId(request.getTransactionId())
                    .withResponse(clientResponse)
                    .withField(PATH, request.getAbsolutePath())
                    .withField(URI, clientResponse.getLocation())
                    .withField(MESSAGE, "Response")
                    .withField(STATUS, clientResponse.getStatus())
                    .build().logInfo();
            clientResponse.close();
        }

        return result;
    }

    private int handleResponseStatus(Response clientResponse, MutableResponse result, byte[] responseEntity) {
        FluentLoggingWrapper log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString());

        int responseStatus = clientResponse.getStatus();
        if ((responseStatus >= 500)
                && ((responseEntity == null) || (responseEntity.length == 0))) {

            log.withMethodName("handleResponseStatus")
                    .withField(MESSAGE, "server error response has no entity")
                    .build().logDebug();

            responseEntity = "{\"message\":\"server error\"}".getBytes(UTF_8);
        }

        if (responseEntity != null) {
            result.setEntity(responseEntity);
        }
        return responseStatus;
    }

    private Builder extractHeaders(MutableRequest request, Builder resource) {
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

        logForwarderCustomMessage("extractHeaders", "Sending Header: ", headerParameters,
                request.getTransactionId());

        return resource;
    }

    private void extractQueryParameters(MutableRequest request, UriBuilder builder) {
        Map<String, List<String>> queryParameters = new HashMap<>();

        for (String parameterName : request.getQueryParameters().keySet()) {
            List<String> queryParameterValues = new ArrayList<>();
            for (String value : request.getQueryParameters().get(parameterName)) {
                builder.queryParam(parameterName, value);
                queryParameterValues.add(value);
            }
            queryParameters.put(parameterName, queryParameterValues);
        }
        logForwarderCustomMessage("extractQueryParameters", "Sending query parameters: ",
                queryParameters, request.getTransactionId());
    }

    private void logForwarderCustomMessage(String methodName, String customMessage,
                                           Map<String, List<String>> logArguments, String transactionId) {
        FluentLoggingWrapper log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString())
                .withMethodName(methodName)
                .withTransactionId(transactionId)
                .withField(MESSAGE, customMessage + logArguments.keySet().toString() +
                        " : " + logArguments.values().toString())
                .build().logDebug();
    }
}
