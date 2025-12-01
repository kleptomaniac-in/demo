package com.example.pdf.service;

import com.example.pdf.controller.GenerateRequest;

import java.util.Map;
import java.util.Optional;

/**
 * Fetches a mapping file from the config server repo via the file endpoint.
 * The `path` should be the repo-relative path (e.g. "mappings/base-application.yml").
 */
public class ConfigFileMappingSource implements MappingSource {

    private final ConfigServerClient client;
    private final String path;

    public ConfigFileMappingSource(ConfigServerClient client, String path) {
        this.client = client;
        this.path = path;
    }

    @Override
    public Optional<Map<String, Object>> fetch(GenerateRequest req, String label) throws Exception {
        return client.getFileSource("default", label, path);
    }
}
