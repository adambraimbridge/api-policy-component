package com.ft.up.apipolicy.pipeline;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

/**
 * MutableHttpTranslator
 *
 * @author Simon.Gibbs
 */
public class MutableHttpTranslator {


    private static final Logger LOGGER = LoggerFactory.getLogger(MutableHttpTranslator.class);

    public Set<String> HEADER_BLACKLIST = new TreeSet<>(Arrays.asList("Host","Connection","Accept-Encoding","Content-Length","Transfer-Encoding"));


    public MutableRequest translateFrom(HttpServletRequest realRequest) {


        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

        Enumeration<String> headerNames = realRequest.getHeaderNames();
        if(headerNames!=null) {
            while(headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();

                if(HEADER_BLACKLIST.contains(headerName)) {
                    continue;
                }

                Enumeration<String> values = realRequest.getHeaders(headerName);
                while(values.hasMoreElements()) {
                    String value = values.nextElement();
                    headers.add(headerName, value);
                }
            }
        } else {
            LOGGER.debug("No headers");
        }

        MultivaluedMap<String, String> queryParameters = new MultivaluedMapImpl();
        for(String queryParam : realRequest.getParameterMap().keySet()) {
            queryParameters.put(queryParam, Arrays.asList(realRequest.getParameterMap().get(queryParam)));
        }

        String absolutePath = URI.create(realRequest.getRequestURL().toString()).getPath();

        MutableRequest request = new MutableRequest();
        request.setAbsolutePath(absolutePath);
        request.setQueryParameters(queryParameters);
        request.setHeaders(headers);

        return request;
    }

    public Response translateTo(MutableResponse mutableResponse) {

        Response.ResponseBuilder responseBuilder = Response.status(mutableResponse.getStatus());

        for(String headerName : mutableResponse.getHeaders().keySet()) {
            if(HEADER_BLACKLIST.contains(headerName)) {
                continue;
            }
            for(String value : mutableResponse.getHeaders().get(headerName)) {
                responseBuilder.header(headerName, value);
            }
        }

        responseBuilder.entity(mutableResponse.getEntity());

        return responseBuilder.build();
    }

}
