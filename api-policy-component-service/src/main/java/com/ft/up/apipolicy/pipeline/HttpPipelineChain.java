package com.ft.up.apipolicy.pipeline;


import com.ft.up.apipolicy.util.FluentLoggingWrapper;
import org.slf4j.MDC;

import java.util.Arrays;

import static com.ft.up.apipolicy.util.FluentLoggingWrapper.*;

/**
 * HttpPipelineChain
 *
 * @author Simon.Gibbs
 */
public class HttpPipelineChain {

    private final HttpPipeline pipeline;
    private int pointer = 0;
//    private FluentLoggingWrapper log;

    public HttpPipelineChain(final HttpPipeline pipeline) {
        this.pipeline = pipeline;
//        log = new FluentLoggingWrapper();
//        log.withClassName(this.getClass().toString());
    }

    public MutableResponse callNextFilter(final MutableRequest request) {

        ApiFilter nextFilter = pipeline.getFilter(pointer++);

//        if(nextFilter != null) {
//            log.withMethodName("callNextFilter")
//                    .withTransactionId(MDC.get("transaction_id"))
//                    .withRequest(request)
//                    .withField(URI, request.getAbsolutePath())
//                    .withField(MESSAGE, "Calling filter [" + nextFilter.getClass() + "] #: [" + pointer + "] and transaction_id is: " + MDC.get("transaction_id"))
//                    .build().logInfo();
//        }

        if (nextFilter == null) {
            return pipeline.forwardRequest(request);
        } else {
            return nextFilter.processRequest(request, this);
        }
    }
}
