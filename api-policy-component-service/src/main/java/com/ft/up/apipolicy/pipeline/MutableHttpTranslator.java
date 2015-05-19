package com.ft.up.apipolicy.pipeline;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.up.apipolicy.LinkedMultivalueMap;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MutableHttpTranslator
 *
 * @author Simon.Gibbs
 */
public class MutableHttpTranslator {


    private static final Logger LOGGER = LoggerFactory.getLogger(MutableHttpTranslator.class);

    // So we don't blindly pass on headers that should be set by THIS application on the request/response
    public Set<String> HEADER_BLACKLIST = new TreeSet<>(Arrays.asList(
            "Connection",
            "Accept-Encoding",
            "Content-Length",
            "Transfer-Encoding",
            "Content-Encoding",
            "Date",
            HttpPipeline.POLICY_HEADER_NAME
    ));


    public MutableRequest translateFrom(HttpServletRequest realRequest) {


        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        Set<String> policies = Collections.emptySet();

        String transactionId = null;

        Enumeration<String> headerNames = realRequest.getHeaderNames();
        if(headerNames!=null) {
            boolean hasXPolicyHeaders = false;
            while(headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();

                Enumeration<String> values = realRequest.getHeaders(headerName);

                if(HttpPipeline.POLICY_HEADER_NAME.equalsIgnoreCase(headerName)) {
                    if(values!=null) {
                        hasXPolicyHeaders = true;
                        policies = new LinkedHashSet<>();
                        while(values.hasMoreElements()) {
                            String value = values.nextElement();
                            LOGGER.info("Processed Policies: {}", value);
                            policies.addAll(Arrays.asList(value.split("[ ,]")));
                        }

                    }
                } else if(HEADER_BLACKLIST.contains(headerName)) {
                    if(LOGGER.isDebugEnabled()) {
                        while(values.hasMoreElements()) {
                            String value = values.nextElement();
                            LOGGER.debug("Not Processed: {}={}", headerName, value);
                        }
                    }
                } else if(("Host").equals(headerName)) { // for Containerisation
                    headers.add(headerName, "read-services");
                } else if(TransactionIdUtils.TRANSACTION_ID_HEADER.equals(headerName)) {
                    transactionId = values.nextElement();
                }
                 else {
                    while(values.hasMoreElements()) {
                        String value = values.nextElement();
                        headers.add(headerName, value);
                        LOGGER.debug("Passed Up: {}={}", headerName, value);
                    }
                }
            }

            // Always add the transaction ID including the default random one if it was missing
            headers.add(TransactionIdUtils.TRANSACTION_ID_HEADER, transactionId);

            if (!hasXPolicyHeaders) {
                LOGGER.info("No X-Policy Headers");
            }
        } else {
            LOGGER.debug("No headers");
        }

        MultivaluedMap<String, String> queryParameters = new LinkedMultivalueMap();
        Enumeration<String> parameterNames = realRequest.getParameterNames();
        if(parameterNames!=null) {
            while(parameterNames.hasMoreElements()) {
                String queryParam = parameterNames.nextElement();
                queryParameters.put(queryParam, Arrays.asList(realRequest.getParameterMap().get(queryParam)));
            }
        }

        String absolutePath = URI.create(realRequest.getRequestURL().toString()).getPath();


        MutableRequest request = new MutableRequest(policies, transactionId);
        request.setAbsolutePath(absolutePath);
        request.setQueryParameters(queryParameters);
        request.setHeaders(headers);

        return request;
    }

    public Response.ResponseBuilder translateTo(MutableResponse mutableResponse) {

        Response.ResponseBuilder responseBuilder = Response.status(mutableResponse.getStatus());

        for(String headerName : mutableResponse.getHeaders().keySet()) {

            List<String> values = mutableResponse.getHeaders().get(headerName);

            if(HEADER_BLACKLIST.contains(headerName)) {
                if(LOGGER.isDebugEnabled()) {
                    for(String value : values) {
                        LOGGER.debug("Not Processed: {}={}", headerName, value);
                    }
                }
            } else {
                for(String value : values) {
                    responseBuilder.header(headerName, value);
                    LOGGER.debug("Passed Down: {}={}", headerName, value);
                }
            }
        }

        responseBuilder.entity(mutableResponse.getEntity());

        return responseBuilder;
    }

}
