package com.ft.up.apipolicy.health;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class ReaderNodesHealthCheck extends AdvancedHealthCheck {

    private EndpointConfiguration endpoint;
    private Client client;

    public ReaderNodesHealthCheck(final String name, EndpointConfiguration endpoint, Client client) {
        super(name);
        this.endpoint = endpoint;
        this.client = client;
    }

    @Override
    protected AdvancedResult checkAdvanced() throws Exception {

        URI healthcheckUri = UriBuilder
                .fromPath("/build-info")
                .host(endpoint.getHost())
                .port(endpoint.getPort())
                .scheme("http")
                .build();

        ClientResponse response = null;
        try {
            // resilient client provides "works at least once" semantics.
            response = client.resource(healthcheckUri).header("Cache-Control","max-age=0").get(ClientResponse.class);

            if (response.getStatus()==200) {
                return AdvancedResult.healthy("All is ok");
            }

            return AdvancedResult.error(this, "Unexpected response status: " + response.getStatus() );
        } catch (Exception e) {
            return AdvancedResult.error(this, "Could not connect",e);
        } finally {
            if(response!=null) {
                response.close();
            }
        }

    }

    @Override
    protected int severity() {
        return 1;
    }

    @Override
    protected String businessImpact() {
        return "Business partners will not be able to access content while the problem continues reducing editorial reach and influence.";
    }

    @Override
    protected String technicalSummary() {
        return "This service facade cannot connect to the services behind it. Requests from partners will fail to be processed.";
    }

    @Override
    protected String panicGuideUrl() {
        return "http://mypanicguide.com";
    }

}