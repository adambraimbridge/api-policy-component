package com.ft.up.apipolicy.transformer;

import com.ft.up.apipolicy.ApiPolicyExceptionMapper;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.SocketTimeoutException;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;


public class ApiPolicyExceptionMapperTest {

    private final ApiPolicyExceptionMapper exceptionMapper = new ApiPolicyExceptionMapper();

    @Test
    public void testExceptionMapping() {
        assertExceptionMapping(new NotFoundException(), SC_NOT_FOUND, "404 Not Found");
        assertExceptionMapping(new WebApplicationException(SC_INTERNAL_SERVER_ERROR), SC_INTERNAL_SERVER_ERROR,
                "HTTP 500 Internal Server Error");
        assertExceptionMapping(new ProcessingException(new SocketTimeoutException()),
                SC_GATEWAY_TIMEOUT, "java.net.SocketTimeoutException");
        assertExceptionMapping(new ProcessingException("other processing exception"),
                SC_INTERNAL_SERVER_ERROR, "other processing exception");
        assertExceptionMapping(new Exception(), SC_INTERNAL_SERVER_ERROR, "server error");
    }

    private void assertExceptionMapping(Exception e, int status, String message) {
        Response response = exceptionMapper.toResponse(e);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getEntity().toString()).isEqualTo(format("{\"message\":\"%s\"}", message));
    }

}
