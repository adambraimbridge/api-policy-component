package com.ft.up.apipolicy;

import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;

import java.io.File;

import static io.dropwizard.testing.junit.ConfigOverride.config;

public abstract class AbstractApiComponentTest {
    static final int SOME_PORT = (int) (Math.random() * 10000) + 40000;
    static final String primaryNodes = String.format("localhost:%d:%d, localhost:%d:%d",
            SOME_PORT, SOME_PORT + 1,
            SOME_PORT + 2, SOME_PORT + 3);

    @ClassRule
    public static WireMockClassRule WIRE_MOCK_1 = new WireMockClassRule(SOME_PORT);

    @ClassRule
    public static WireMockClassRule WIRE_MOCK_2 = new WireMockClassRule(SOME_PORT + 2);

    static String resourceFilePath(String resourceClassPathLocation) {

        File file = null;

        try {

            file = new File(Resources.getResource(resourceClassPathLocation).toURI());
            return file.getAbsolutePath();

        } catch (Exception e) {
            if (file != null) {
                throw new RuntimeException(file.toString(), e);
            }
            throw new RuntimeException(e);
        }
    }
}
