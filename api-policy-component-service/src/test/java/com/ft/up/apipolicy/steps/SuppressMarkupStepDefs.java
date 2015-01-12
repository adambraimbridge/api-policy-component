package com.ft.up.apipolicy.steps;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.filters.SuppressMarkupFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuppressMarkupStepDefs {

    private HttpPipelineChain mockChain;

    private MutableRequest mockRequest;

    private String unprocessedMarkup;

    private String processedMarkup;
    private JsonConverter jsonConverter;

    @Before
    public void setup() {
        mockRequest = mock(MutableRequest.class);
        mockChain = mock(HttpPipelineChain.class);

        jsonConverter = JsonConverter.testConverter();
    }

    @Given("^the unprocessed markup (.+)$")
    public void the_unprocessed_markup_(String unprocessedMarkup) throws Throwable {
        this.unprocessedMarkup = unprocessedMarkup;

    }

    @When("^it is transformed$")
    public void it_is_transformed() throws Throwable {

        HashMap<String,Object> jsonEntity = new HashMap<>(1);
        jsonEntity.put("bodyXML",(Object) unprocessedMarkup);

        MutableResponse expectedResponse = new MutableResponse();
        jsonConverter.replaceEntity(expectedResponse,jsonEntity);

        when(mockChain.callNextFilter(any(MutableRequest.class))).thenReturn(expectedResponse);

        SuppressMarkupFilter filter = new SuppressMarkupFilter(jsonConverter);

        MutableResponse rawResponse = filter.processRequest(mockRequest, mockChain);

        processedMarkup = (String) jsonConverter.readEntity(rawResponse).get("bodyXML");
    }

    @Then("^the mark up becomes (.*)$")
    public void the_mark_up_becomes(String expectedMarkup) throws Throwable {
        assertEquals(expectedMarkup, processedMarkup);
    }
}
