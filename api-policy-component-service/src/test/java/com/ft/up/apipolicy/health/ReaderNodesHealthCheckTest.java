package com.ft.up.apipolicy.health;

import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedResult;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.Response;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
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
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(builder.header(any(String.class), any(String.class))).thenReturn(builder);
        WebTarget target = mock(WebTarget.class);
        when(target.request()).thenReturn(builder);
        
        Client client = mock(Client.class);
        when(client.target(any(URI.class))).thenReturn(target);
        
        when(builder.get()).thenThrow(new RuntimeException("Synthetic client exception"));
        
        EndpointConfiguration config = mock(EndpointConfiguration.class);

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

    private Client primeClientToGive(int status) {
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(builder.header(any(String.class), any(String.class))).thenReturn(builder);
        WebTarget target = mock(WebTarget.class);
        when(target.request()).thenReturn(builder);
        
        Client client = mock(Client.class);
        when(client.target(any(URI.class))).thenReturn(target);
        
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(status);
        when(builder.get()).thenReturn(mockResponse);
        
        return client;
    }
}
