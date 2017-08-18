package com.ft.up.apipolicy.health;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReaderNodesHealthCheckTest
 *
 * @author Simon.Gibbs
 */
public class ReaderNodesHealthCheckTest {

    // used in stacks with vulcan-based routing
    private static final String VULCAN_HC_PATH = "/v2/status";

    //used in stacks with varnish-based routing
    private static final String VARNISH_HC_PATH = "/status";

    @Test
    public void shouldReturnErrorStateIfClientThrowsException() throws Exception {
        ClientHandler mockHandler = mock(ClientHandler.class);
        Client client = new Client(mockHandler);
        EndpointConfiguration config = mock(EndpointConfiguration.class);

        when(mockHandler.handle(any(ClientRequest.class))).thenThrow(new RuntimeException("Synthetic client exception"));

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test",config,client,false);

        assertThat(check.checkAdvanced().status(),is(AdvancedResult.Status.ERROR));
    }

    @Test
    public void shouldReturnErrorStateIfServerGives500() throws Exception {

        EndpointConfiguration config = mock(EndpointConfiguration.class);

        Client client = primeClientToGive(500);

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test",config,client,false);

        assertThat(check.checkAdvanced().status(),is(AdvancedResult.Status.ERROR));
    }


    @Test
    public void shouldReturnOKStateIfServerGives200() throws Exception {

        EndpointConfiguration config = mock(EndpointConfiguration.class);

        Client client = primeClientToGive(200);

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test",config,client,false);

        assertThat(check.checkAdvanced().status(),is(AdvancedResult.Status.OK));
    }

    @Test
    public void shouldCheckVarnishIfCheckVulcanHealthFalse() throws Exception {
        testHealthcheckURI(false, VARNISH_HC_PATH);
    }

    @Test
    public void shouldCheckVulcanIfCheckVulcanHealthTrue() throws Exception {
        testHealthcheckURI(true, VULCAN_HC_PATH);
    }

    private void testHealthcheckURI(boolean checkVulcanHealth, String expectedPath) throws Exception {
        EndpointConfiguration config = mock(EndpointConfiguration.class);
        Client client = mock(Client.class);

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test", config, client, checkVulcanHealth);
        check.checkAdvanced();

        ArgumentCaptor<URI> argument = ArgumentCaptor.forClass(URI.class);
        verify(client).resource(argument.capture());

        assertNotNull(argument.getValue());
        String path = argument.getValue().getPath();
        assertNotNull(path);
        assertThat(path, is(expectedPath));
    }

    private Client primeClientToGive(int status) {
        ClientHandler mockHandler = mock(ClientHandler.class);
        Client client = new Client(mockHandler);
        ClientResponse mockResponse = mock(ClientResponse.class);
        when(mockResponse.getStatus()).thenReturn(status);

        when(mockHandler.handle(any(ClientRequest.class))).thenReturn(mockResponse);
        return client;
    }
}
