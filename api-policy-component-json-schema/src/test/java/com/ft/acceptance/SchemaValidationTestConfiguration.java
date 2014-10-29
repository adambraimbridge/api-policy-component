package com.ft.acceptance;


import com.fasterxml.jackson.annotation.JsonProperty;

public class SchemaValidationTestConfiguration {

    private final String notificationPath;
    private final String contentPath;
    private final ApiConfig readApi;
    private final ApiConfig apiPolicyComponent;



    public SchemaValidationTestConfiguration(@JsonProperty("readApi") ApiConfig readApi,
                                             @JsonProperty("apiPolicyComponent") ApiConfig apiPolicyComponent,
                                             @JsonProperty("notificationPath") String  notificationPath,
                                             @JsonProperty("contentPath") String contentPath
                                             ) {
        this.readApi = readApi;
        this.apiPolicyComponent = apiPolicyComponent;
        this.notificationPath = notificationPath;
        this.contentPath = contentPath;


    }


    public String getApiPolicyComponentHost() {
        return apiPolicyComponent.getHost();
    }

    public int getApiPolicyComponentPort() {
        return apiPolicyComponent.getPort();
    }

    public int getApiPolicyComponentAdminPort() {
        return apiPolicyComponent.getAdminPort();
    }

    public String getReadApiHost() {
        return readApi.getHost();
    }

    public int getReadApiPort() {
        return readApi.getPort();
    }

    public int getReadApiAdminPort() {
        return readApi.getAdminPort();
    }

    public String getNotificationPath() {
        return notificationPath;
    }

    public String getContentPath() {
        return contentPath;
    }

}
