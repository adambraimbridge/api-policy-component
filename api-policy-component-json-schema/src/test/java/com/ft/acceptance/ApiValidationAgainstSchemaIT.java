package com.ft.acceptance;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiValidationAgainstSchemaIT {

    private static final String SINCE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiValidationAgainstSchemaIT.class);
    private static final String HTTP = "http://";
    private static final String KITCHEN_SINK_UUID = "54307a12-37fa-11e3-8f44-002128161462";
    private SchemaValidationTestConfiguration configuration;
    private Client client;

    private JsonSchema notificationSchema;
    private JsonSchema contentSchema;

    @Before
    public void setUp() throws Exception{
        final String configFileName = System.getProperty("test.schemaValidation.configFile");

        Preconditions.checkNotNull(configFileName, "System property test.schemaValidation.configFile is null");

        LOGGER.debug("test.schemaValidation.configFile = {}", configFileName);
        final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            final File file = new File(configFileName).getCanonicalFile();
            LOGGER.debug("using {} as config file", file);
            configuration = objectMapper.readValue(file, SchemaValidationTestConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        client = Client.create();

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonNode notificationNode = JsonLoader.fromResource("/notifications-schema.json");
        JsonNode contentNode = JsonLoader.fromResource("/content-schema.json");
        notificationSchema = factory.getJsonSchema(notificationNode);
        contentSchema = factory.getJsonSchema(contentNode);

        checkDependentSystemsAreUpAndRunning();
    }

    private void checkDependentSystemsAreUpAndRunning() {
        checkReadApiHealthcheck();
        checkApiPolicyComponentHealthcheck();
    }


    private void checkApiPolicyComponentHealthcheck() {
        String healthcheckUrl = getHealthcheckUrl(configuration.getApiPolicyComponentHost(), configuration.getApiPolicyComponentAdminPort());
        ClientResponse response= hitResource(healthcheckUrl);
        assertThat("PolicyComponent Healthcheck has failed", response.getStatus(), equalTo(200));
    }

    private void checkReadApiHealthcheck() {
        String healthcheckUrl = getHealthcheckUrl(configuration.getReadApiHost(), configuration.getReadApiAdminPort());
        ClientResponse response= hitResource(healthcheckUrl);
        assertThat("ReadApi Healthcheck has failed", response.getStatus(), equalTo(200));
    }

    private ClientResponse hitResource(String url){
        WebResource webResource = client.resource(url);
        return webResource.get(ClientResponse.class);
    }

    private String getHealthcheckUrl(String host, int port) {
        StringBuilder sb = new StringBuilder("http://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append("healthcheck");
        return sb.toString();
    }


    @Test
    public void shouldValidateAgainstContentJson() throws IOException, ProcessingException {
        String resource = getContentResource();
        validateSchema(resource, contentSchema);
    }

    @Test
    public void shouldValidateAgainstNotificationJson() throws IOException, ProcessingException {
        String resource = getNotificationResource();
        System.out.print(resource);
        validateSchema(resource, notificationSchema);
    }

    private void validateSchema(String resource, JsonSchema schema) throws IOException, ProcessingException{

        WebResource webResource = client.resource(resource);
        ClientResponse response = webResource.get(ClientResponse.class);
        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
        }

        String jsonOutput = response.getEntity(String.class);
        final JsonNode jsonNode = JsonLoader.fromString(jsonOutput);
        ProcessingReport report = schema.validate(jsonNode);
        assertThat("The validation against the schema has problems " + report.toString(), report.isSuccess(), equalTo(true));
    }

    private String getContentResource(){
        StringBuilder sb = new StringBuilder(HTTP)
                .append(configuration.getApiPolicyComponentHost())
                .append(":")
                .append(configuration.getApiPolicyComponentPort())
                .append(configuration.getContentPath())
                .append(KITCHEN_SINK_UUID);

        return sb.toString();
    }

    private String getNotificationResource(){
        DateTime since = new DateTime();
        String sinceStr = since.minusHours(48).toString(SINCE_DATE_FORMAT);
        StringBuilder sb = new StringBuilder(HTTP)
                .append(configuration.getApiPolicyComponentHost())
                .append(":")
                .append(configuration.getApiPolicyComponentPort())
                .append(configuration.getNotificationPath())
                .append(sinceStr);

        return sb.toString();
    }


}
