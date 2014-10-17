package com.ft.up.apipolicy;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

/**
 * JerseyRequestForwarder
 *
 * @author Simon.Gibbs
 */
public class JerseyRequestForwarder implements RequestForwarder {

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
            }
        }

        WebResource resource = client.resource(builder.build());
        for(String headerName : request.getHeaders().keySet()) {
            resource.header(headerName,request.getHeaders().get(headerName));
        }

        ClientResponse clientResponse = resource.method("GET", ClientResponse.class);
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
