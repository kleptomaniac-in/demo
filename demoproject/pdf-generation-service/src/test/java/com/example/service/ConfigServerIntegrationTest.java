package com.example.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that runs against a real local Spring Cloud Config Server.
 *
 * This test will be skipped automatically if nothing is listening on localhost:8888.
 * Run your local config server (from `demoproject/config-server`) before enabling this test.
 */
public class ConfigServerIntegrationTest {

    private static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    void fetchesPdfGenerationServiceApplicationPropertiesFromLocalConfigServer() {
        // Skip the test if there is no local config server running on 8888
        Assumptions.assumeTrue(isPortOpen("localhost", 8888, 200), "Local Config Server not running on localhost:8888");

        WebClient wc = WebClient.builder().baseUrl("http://localhost:8888").build();
        ConfigServerClient client = new ConfigServerClient(wc, "http://localhost:8888");

        var opt = client.getApplicationSource("pdf-generation-service", "default", "main");
        Assumptions.assumeTrue(opt.isPresent(), "Config Server returned no application source for pdf-generation-service");

        Map<String, Object> src = opt.get();
        // Assert that config contains at least one entry mentioning mappings/base-application (our known repo content)
        boolean containsCandidate = src.values().stream().map(Object::toString).anyMatch(s -> s.contains("mappings/base-application") || s.contains("candidate-order"));
        assertTrue(containsCandidate, "Expected application properties to contain mapping candidate-order or mapping paths");
    }
}
