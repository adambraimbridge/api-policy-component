package com.ft.up.apipolicy;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * JerseyRequestForwarder
 *
 * @author Simon.Gibbs
 */
public class JerseyRequestForwarder implements RequestForwarder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyRequestForwarder.class);
    private Client client;
    private EndpointConfiguration varnish;

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

        for (String parameterName : request.getQueryParameters().keySet()) {
            for (String value : request.getQueryParameters().get(parameterName)) {
                try {
                    builder.queryParam(parameterName, URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new ForwarderException(e);
                }
                LOGGER.debug("Sending Parameter: {}={}", parameterName, value);
            }
        }

        Invocation.Builder resource = client.target(builder.build()).request();

        MultivaluedMap<String, String> headers = request.getHeaders();
        for (String headerName : headers.keySet()) {
            for (String value : headers.get(headerName)) {
                resource = resource.header(headerName, value);
                LOGGER.debug("Sending Header: {}={}", headerName, value);
            }
        }

        Response clientResponse = null;

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
                LOGGER.error("unable to obtain a response entity", e);
            }

            int responseStatus = clientResponse.getStatus();
            if ((responseStatus >= 500)
                    && ((responseEntity == null) || (responseEntity.length == 0))) {

                LOGGER.debug("server error response has no entity");
                responseEntity = "{\"message\":\"server error\"}".getBytes(UTF_8);
            }

            if (responseEntity != null) {
                result.setEntity(responseEntity);
            }

            result.setStatus(responseStatus);
            result.setHeaders(clientResponse.getHeaders());
        } finally {
            clientResponse.close();
        }

        return result;
    }
}
