package com.ft.up.apipolicy;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * RuntimeExceptionMapper
 *
 * Converts any RuntimeException including those that result in a response code of
 * - 415
 * - 500
 * - 503
 * to a sensible response with a human-readable error message
 *
 * Some other exceptional scenarios like requests that result in a response code of
 * - 400
 * are not handled because Jersey constructs error responses.
 *
 * @author Simon.Gibbs
 */
public class ApiPolicyExceptionMapper  implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(ApiPolicyExceptionMapper.class);

    public static final String GENERIC_MESSAGE = "server error";

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof NotFoundException) {
            String message = "404 Not Found";
            LOG.debug(message);
            return Response.status(HttpServletResponse.SC_NOT_FOUND).type(MediaType.APPLICATION_JSON)
                .entity(Collections.singletonMap("message", message))
                .build();
        }

        if (exception instanceof WebApplicationException) {
            Response response = ((WebApplicationException) exception).getResponse();

            // skip processing of responses that are already present.
            if(response.getEntity() != null) {
                return response;
            }

            // fill out null responses
            String message = MoreObjects.firstNonNull(exception.getMessage(), GENERIC_MESSAGE);

            if(!GENERIC_MESSAGE.equals(message)) {
                // Don't turn this off. You should be using ServerError and ClientError builders.
                LOG.warn("Surfaced exception message from unknown tier. Expected ErrorEntity from web tier.");
            }
            
            if (response.getStatus()<500) {
            	if (GENERIC_MESSAGE.equals(message)) { // if we didn't get a specific message from the exception
                	message = "client error";
            	}
            }
            else {
                // ensure server error exceptions are logged!
                LOG.error("Server error: ", exception);
            }

            return Response.status(response.getStatus()).type(MediaType.APPLICATION_JSON)
                .entity(Collections.singletonMap("message", message))
                .build();
        }
        
        // ensure server error exceptions are logged!
        LOG.error("Server error: ", exception);
        
        // unless we know otherwise
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String message = GENERIC_MESSAGE;
        
        if (exception instanceof ProcessingException) {
            message = exception.getMessage();
            if (exception.getCause() instanceof SocketTimeoutException) {
                status = HttpServletResponse.SC_GATEWAY_TIMEOUT;
            }
        }
        
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(Collections.singletonMap("message", message))
                .build();

    }
}
