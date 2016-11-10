package com.ft.up.apipolicy;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.params.ClientPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        configureJersey();
    }

    private void configureJersey() {
        client.setFollowRedirects(false);
        // Hack to force http client to stop handling redirects. This needs to be changed to the new 'DW way' when we upgrade from v0.7.1
        ClientHandler handler = client.getHeadHandler();
        while (handler instanceof ClientFilter) {
            handler = ((ClientFilter) handler).getNext();
            if (handler instanceof ApacheHttpClient4Handler) {
                LOGGER.info("Reconfiguring underlying http client to stop handling redirects.");
                ApacheHttpClient4Handler apacheHandler = (ApacheHttpClient4Handler) handler;
                apacheHandler.getHttpClient().getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
            }
        }
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

        WebResource.Builder resource = client.resource(builder.build()).getRequestBuilder();

        MultivaluedMap<String, String> headers = request.getHeaders();
        for (String headerName : headers.keySet()) {
            for (String value : headers.get(headerName)) {
                resource = resource.header(headerName, value);
                LOGGER.debug("Sending Header: {}={}", headerName, value);
            }
        }

        ClientResponse clientResponse = null;

        String requestEntity = request.getRequestEntityAsString();

        if (StringUtils.isNotEmpty(requestEntity)) {
            clientResponse = resource.method(request.getHttpMethod(), ClientResponse.class, requestEntity);
        } else {
            clientResponse = resource.method(request.getHttpMethod(), ClientResponse.class);
        }
        MutableResponse result = new MutableResponse();

        try {
            byte[] responseEntity = null;
            try {
                if (clientResponse.hasEntity()) {
                    responseEntity = IOUtils.toByteArray(clientResponse.getEntityInputStream());
                }
            } catch (ClientHandlerException e) {
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
        } catch (IOException e) {
            throw new ForwarderException(e);
        } finally {
            clientResponse.close();
        }

        return result;

    }
}
