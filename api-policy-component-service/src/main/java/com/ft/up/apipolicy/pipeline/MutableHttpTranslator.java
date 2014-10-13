package com.ft.up.apipolicy.pipeline;

import com.ft.up.apipolicy.resources.FailedToWriteToResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.OutputStream;
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


    public Set<String> HEADER_BLACKLIST = new TreeSet<>(Arrays.asList("Host","Connection","Accept-Encoding","Content-Length","Transfer-Encoding"));


    public MutableRequest translateFrom(HttpServletRequest realRequest) {


        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();

        Enumeration<String> headerNames = realRequest.getHeaderNames();
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

        return new MutableRequest(headers);
    }

    public void writeMutableResponseIntoActualResponse(MutableResponse mutableResponse, HttpServletResponse actualResponse) {

        for(String headerName : mutableResponse.getHeaders().keySet()) {
            if(HEADER_BLACKLIST.contains(headerName)) {
                continue;
            }
            for(String value : mutableResponse.getHeaders().get(headerName)) {
                actualResponse.addHeader(headerName, value);
            }
        }

        try {
            OutputStream out = actualResponse.getOutputStream();
            out.write(mutableResponse.getEntity());
        } catch (IOException e) {
            throw new FailedToWriteToResponse();
        }
    }

}
