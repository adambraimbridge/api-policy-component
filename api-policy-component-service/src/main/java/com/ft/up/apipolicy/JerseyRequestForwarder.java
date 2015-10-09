package com.ft.up.apipolicy;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        for(String parameterName : request.getQueryParameters().keySet()) {
            for(String value : request.getQueryParameters().get(parameterName)) {
                builder.queryParam(parameterName, value);
                LOGGER.debug("Sending Parameter: {}={}",parameterName,value);
            }
        }

        WebResource.Builder resource = client.resource(builder.build()).getRequestBuilder();

        MultivaluedMap<String,String> headers = request.getHeaders();
        for(String headerName : headers.keySet()) {
            for(String value : headers.get(headerName)) {
                resource = resource.header(headerName,value);
                LOGGER.debug("Sending Header: {}={}",headerName,value);
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
            result.setEntity(IOUtils.toByteArray(clientResponse.getEntityInputStream()));
            result.setStatus(clientResponse.getStatus());
            result.setHeaders(clientResponse.getHeaders());
        } catch (IOException e) {
            throw new ForwarderException(e);
        } finally {
            clientResponse.close();
        }

        return result;

    }
}
