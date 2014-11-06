package com.ft.up.apipolicy.pipeline;

import com.ft.up.apipolicy.LinkedMultivalueMap;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * MutableHttpTranslator
 *
 * @author Simon.Gibbs
 */
public class MutableHttpTranslator {


    private static final Logger LOGGER = LoggerFactory.getLogger(MutableHttpTranslator.class);

    // So we don't blindly pass on headers that should be set by THIS application on the request/response
    public Set<String> HEADER_BLACKLIST = new TreeSet<>(Arrays.asList(
            "Host",
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

        Enumeration<String> headerNames = realRequest.getHeaderNames();
        if(headerNames!=null) {
            while(headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();

                Enumeration<String> values = realRequest.getHeaders(headerName);

                if(HttpPipeline.POLICY_HEADER_NAME.equalsIgnoreCase(headerName)) {
                    if(values!=null) {
                        policies  = new LinkedHashSet<>();
                        while(values.hasMoreElements()) {
                            String value = values.nextElement();
                            LOGGER.debug("Processed Policies: {}", value);
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
                } else {
                    while(values.hasMoreElements()) {
                        String value = values.nextElement();
                        headers.add(headerName, value);
                        LOGGER.debug("Passed Up: {}={}", headerName, value);
                    }
                }
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

        MutableRequest request = new MutableRequest(policies);
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
