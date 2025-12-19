package com.example.service;

import com.example.pdf.controller.GenerateRequest;

import java.util.Map;
import java.util.Optional;

/**
 * Fetches configuration published under an application name from the config server.
 * Example application name: "order-service-invoice-v2".
 */
public class ApplicationMappingSource implements MappingSource {

    private final ConfigServerClient client;
    private final String applicationName;

    public ApplicationMappingSource(ConfigServerClient client, String applicationName) {
        this.client = client;
        this.applicationName = applicationName;
    }

    @Override
    public Optional<Map<String, Object>> fetch(GenerateRequest req, String label) throws Exception {
        return client.getApplicationSource(applicationName, "default", label);
    }
}
