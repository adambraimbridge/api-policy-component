package com.ft.up.apipolicy.pipeline;

import com.ft.api.jaxrs.errors.ServerError;
import com.ft.up.apipolicy.util.FluentLoggingWrapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.util.*;

import static com.ft.api.util.transactionid.TransactionIdUtils.TRANSACTION_ID_HEADER;
import static com.ft.up.apipolicy.pipeline.HttpPipeline.POLICY_HEADER_NAME;
import static com.ft.up.apipolicy.util.FluentLoggingWrapper.MESSAGE;
import static com.ft.up.apipolicy.util.FluentLoggingWrapper.flattenHeaderToString;
import static java.util.Collections.emptySet;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.slf4j.MDC.*;


/**
 * MutableHttpTranslator
 *
 * @author Simon.Gibbs
 */
public class MutableHttpTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MutableHttpTranslator.class);

    private FluentLoggingWrapper log;

    // So we don't blindly pass on headers that should be set by THIS application on the request/response
    public Set<String> HEADER_BLACKLIST = new TreeSet<>(Arrays.asList(
            "Connection",
            "Accept-Encoding",
            "Content-Length",
            "Transfer-Encoding",
            "Content-Encoding",
            "Date",
            POLICY_HEADER_NAME
    ));

    public MutableHttpTranslator() {
        log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString());
    }


    public MutableRequest translateFrom(HttpServletRequest realRequest) {
        log.withMethodName("translateFrom")
                .withRequest(realRequest)
                .withField(FluentLoggingWrapper.URI, realRequest.getRequestURI())
                .withField(FluentLoggingWrapper.PATH, realRequest.getContextPath());

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        Set<String> policies = emptySet();
        String transactionId = null;
        Enumeration<String> headerNames = realRequest.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                Enumeration<String> values = realRequest.getHeaders(headerName);

                if (TRANSACTION_ID_HEADER.equals(headerName)) {
                    transactionId = values.nextElement();
                    if (isBlank(transactionId)) {
                        transactionId = get("transaction_id");
                    }
                } else if (POLICY_HEADER_NAME.equalsIgnoreCase(headerName)) {
                    policies = getPolicies(values);
                } else if (HEADER_BLACKLIST.contains(headerName)) {
                    logBlacklistedHeader(headerName, list(values), log, transactionId);
                } else if (("Host").equals(headerName)) { // for Containerisation
                    headers.add(headerName, "public-services");
                } else {
                    logPassed(headerName, list(values), headers, null, "Passed Up: ", log, transactionId);
                }
            }

            // Always add the transaction ID including the default random one if it was missing
            headers.add(TRANSACTION_ID_HEADER, transactionId);
            log.withTransactionId(transactionId);

            if (!policies.isEmpty() && !isBlank(transactionId)) {
                log.withField(MESSAGE, "Processed " + POLICY_HEADER_NAME + " : " + policies.toString())
                        .build().logInfo();
            } else {
                log.withField(MESSAGE, "No X-Policy Headers")
                        .build().logDebug();
            }
        } else {
            log.withField(MESSAGE, "No headers")
                    .build().logDebug();
        }

        return getMutableRequest(realRequest, headers, policies, transactionId);
    }

    private Set<String> getPolicies(Enumeration<String> values) {
        Set<String> policies = new LinkedHashSet<>();
        if (values != null) {
            while (values.hasMoreElements()) {
                String value = values.nextElement();
                policies.addAll(Arrays.asList(value.split("[ ,]")));
            }
        }
        return policies;
    }

    private MutableRequest getMutableRequest(HttpServletRequest realRequest, MultivaluedMap<String, String> headers,
                                             Set<String> policies, String transactionId) {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        Enumeration<String> parameterNames = realRequest.getParameterNames();
        if (parameterNames != null) {
            while (parameterNames.hasMoreElements()) {
                String queryParam = parameterNames.nextElement();
                queryParameters.put(queryParam, Arrays.asList(realRequest.getParameterMap().get(queryParam)));
            }
        }

        String absolutePath = java.net.URI.create(realRequest.getRequestURL().toString()).getPath();

        MutableRequest request = new MutableRequest(policies, transactionId);
        request.setAbsolutePath(absolutePath);
        request.setQueryParameters(queryParameters);
        request.setHeaders(headers);
        request.setRequestEntity(getEntityIfSupplied(realRequest));
        request.setHttpMethod(realRequest.getMethod());

        return request;
    }

    private void logBlacklistedHeader(String headerName, List<String> values, FluentLoggingWrapper log, String transactionId) {
        if (LOGGER.isDebugEnabled() && !values.isEmpty()) {
            log.withField(MESSAGE, "Not Processed: " + headerName + "=" + values.toString())
                    .withTransactionId(transactionId)
                    .build().logDebug();
        }
    }

    private void logPassed(String headerName, List<String> values, MultivaluedMap<String, String> headers,
                           ResponseBuilder responseBuilder, String msgParam, FluentLoggingWrapper log, String transactionId) {
        List<String> headerValues = new ArrayList<>();
        for (String value : values) {
            if (headers != null) {
                headers.add(headerName, value);
            }
            if (responseBuilder != null) {
                responseBuilder.header(headerName, value);
            }
            headerValues.add(value);
        }
        log.withField(MESSAGE, msgParam + headerName + "=" + headerValues.toString())
                .withTransactionId(transactionId)
                .build().logDebug();
    }

    private byte[] getEntityIfSupplied(HttpServletRequest realRequest) {
        try {
            ServletInputStream inputStream = realRequest.getInputStream();
            if (inputStream != null) {
                byte[] entity = IOUtils.toByteArray(inputStream);
                return entity;
            }
            return null;
        } catch (IOException e) {
            throw ServerError.status(500).error(e.getMessage()).exception(e);
        }
    }

    public ResponseBuilder translateTo(MutableResponse mutableResponse) {
        String tid = flattenHeaderToString(mutableResponse, TRANSACTION_ID_HEADER);

        if (!isBlank(tid)) {
            log.withTransactionId(tid);
        }
        log.withMethodName("translateTo")
                .withResponse(mutableResponse);

        ResponseBuilder responseBuilder = status(mutableResponse.getStatus());

        for (String headerName : mutableResponse.getHeaders().keySet()) {

            List<Object> values = mutableResponse.getHeaders().get(headerName);

            List<String> valuesAsStrings = values.stream()
                    .map(object -> Objects.toString(object, null))
                    .collect(toList());

            if (HEADER_BLACKLIST.contains(headerName)) {
                logBlacklistedHeader(headerName, valuesAsStrings, log, tid);
            } else {
                logPassed(headerName, valuesAsStrings, null, responseBuilder, "Passed Down: ", log, tid);
            }
        }

        responseBuilder.entity(mutableResponse.getEntity());

        return responseBuilder;
    }

}
