package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.BodyProcessingContext;

public interface TransactionIdBodyProcessingContext extends BodyProcessingContext {

    String getTransactionId();

}
