package com.ft.up.apipolicy.configuration;

public enum Policy {

    FASTFT_CONTENT_ONLY("FASTFT_CONTENT_ONLY"),
    EXCLUDE_FASTFT_CONTENT("EXCLUDE_FASTFT_CONTENT"),
    ALPHAVILLE_CONTENT_ONLY("ALPHAVILLE_CONTENT_ONLY"),
    EXCLUDE_ALPHAVILLE_CONTENT("EXCLUDE_ALPHAVILLE_CONTENT"),
    INCLUDE_RICH_CONTENT("INCLUDE_RICH_CONTENT"),
    INCLUDE_IDENTIFIERS("INCLUDE_IDENTIFIERS"),
    INCLUDE_COMMENTS("INCLUDE_COMMENTS"),
    INCLUDE_PROVENANCE("INCLUDE_PROVENANCE"),
    INCLUDE_LAST_MODIFIED_DATE("INCLUDE_LAST_MODIFIED_DATE"),
    INTERNAL_UNSTABLE("INTERNAL_UNSTABLE");

    private final String headerValue;

    private Policy(final String headerValue) {
        this.headerValue = headerValue;
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
