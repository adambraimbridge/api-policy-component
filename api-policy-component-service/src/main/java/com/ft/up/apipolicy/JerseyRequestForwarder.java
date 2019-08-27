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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * JerseyRequestForwarder
 *
 * @author Simon.Gibbs
 */
public class JerseyRequestForwarder implements RequestForwarder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyRequestForwarder.class);
    public static final String CONTENT_TYPE = "content-type";
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

        for (String parameterName : request.getQueryParameters().keySet()) {
            for (String value : request.getQueryParameters().get(parameterName)) {
                builder.queryParam(parameterName, value);
                LOGGER.debug("Sending query parameter: {}={}", parameterName, value);
            }
        }
        
        Invocation.Builder resource = client.target(builder.build()).request();

        String contentType = null;
        MultivaluedMap<String, String> headers = request.getHeaders();
        for (String headerName : headers.keySet()) {
            for (String value : headers.get(headerName)) {
                if (contentType == null && headerName.toLowerCase().equals(CONTENT_TYPE)) {
                    contentType = value;
                }
                resource = resource.header(headerName, value);
                LOGGER.debug("Sending Header: {}={}", headerName, value);
            }
        }
        
        Response clientResponse;

        String requestEntity = request.getRequestEntityAsString();

        if (StringUtils.isNotEmpty(requestEntity)) {
            Entity entity = contentType != null ? Entity.entity(requestEntity, contentType) :
                    Entity.json(requestEntity);

            String method = request.getHttpMethod();
            Invocation invocation = resource.build(method, entity);
            clientResponse = invocation.invoke();
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
