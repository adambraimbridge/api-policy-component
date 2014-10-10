package com.ft.up.apipolicy.health;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ReaderNodesHealthCheckTest
 *
 * @author Simon.Gibbs
 */
public class ReaderNodesHealthCheckTest {

    @Test
    public void shouldReturnErrorStateIfClientThrowsException() throws Exception {
        ClientHandler mockHandler = mock(ClientHandler.class);
        Client client = new Client(mockHandler);
        EndpointConfiguration config = mock(EndpointConfiguration.class);

        when(mockHandler.handle(any(ClientRequest.class))).thenThrow(new RuntimeException("Synthetic client exception"));

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test",config,client);

        assertThat(check.checkAdvanced().status(),is(AdvancedResult.Status.ERROR));
    }

    @Test
    public void shouldReturnErrorStateIfServerGives500() throws Exception {

        EndpointConfiguration config = mock(EndpointConfiguration.class);

        Client client = primeClientToGive(500);

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test",config,client);

        assertThat(check.checkAdvanced().status(),is(AdvancedResult.Status.ERROR));
    }


    @Test
    public void shouldReturnOKStateIfServerGives200() throws Exception {

        EndpointConfiguration config = mock(EndpointConfiguration.class);

        Client client = primeClientToGive(200);

        ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test",config,client);

        assertThat(check.checkAdvanced().status(),is(AdvancedResult.Status.OK));
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
