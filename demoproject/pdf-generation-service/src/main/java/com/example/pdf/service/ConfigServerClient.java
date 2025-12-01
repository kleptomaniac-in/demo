package com.example.pdf.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ConfigServerClient {

    public ConfigServerClient() {
        this((WebClient) null, null);
    }

    private static final Logger log = LoggerFactory.getLogger(ConfigServerClient.class);

    private final WebClient webClient;
    private final String baseUrl;
    private final ObjectMapper json = new ObjectMapper();
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public ConfigServerClient(WebClient webClient, String baseUrl) {
        this.baseUrl = baseUrl == null ? "http://localhost:8888" : baseUrl;
        this.webClient = webClient == null ? WebClient.builder().baseUrl(this.baseUrl).build() : webClient;
    }

    public ConfigServerResponse getApplicationConfig(String application, String profile, String label) {
        String uri = String.format("/%s/%s/%s", application, profile, label);
        log.debug("Fetching application config from {}{}", baseUrl, uri);
        try {
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(ConfigServerResponse.class)
                    .block();
        } catch (WebClientResponseException wre) {
            log.warn("HTTP error fetching application config {}: {}", uri, wre.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Error fetching application config {}: {}", uri, ex.toString());
            return null;
        }
    }

    /**
     * Convenience: return the first property source 'source' map for the application endpoint.
     */
    @Cacheable(cacheNames = "appSource", key = "#application + '|' + #profile + '|' + #label")
    public Optional<Map<String, Object>> getApplicationSource(String application, String profile, String label) {
        ConfigServerResponse resp = getApplicationConfig(application, profile, label);
        if (resp == null || resp.propertySources == null || resp.propertySources.isEmpty()) return Optional.empty();
        return Optional.ofNullable(resp.propertySources.get(0).source);
    }

    public ConfigServerResponse getFile(String profile, String label, String pathWithExtension) {
        // Normalize inputs and encode each path segment so special characters don't break the URL.
        // pathWithExtension should be like "mappings/base-application.yml" or "mappings/templates/invoice-v2.yml"
        if (pathWithExtension == null) return null;
        String normalizedPath = pathWithExtension.trim();
        // remove any leading slashes
        while (normalizedPath.startsWith("/")) normalizedPath = normalizedPath.substring(1);

        // Encode each segment separately so '/' separators remain
        String encodedPath = Arrays.stream(normalizedPath.split("/"))
            .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
            .collect(Collectors.joining("/"));

        String encodedProfile = URLEncoder.encode(profile == null ? "default" : profile, StandardCharsets.UTF_8);
        String encodedLabel = URLEncoder.encode(label == null ? "main" : label, StandardCharsets.UTF_8);

        String uri = String.format("/application/%s/%s/%s", encodedProfile, encodedLabel, encodedPath);
        log.debug("Fetching file config from {}{}", baseUrl, uri);
        try {
            String body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (body == null || body.isBlank()) return null;

            // Try to parse as a ConfigServerResponse JSON first (some endpoints return JSON)
            try {
                return json.readValue(body, ConfigServerResponse.class);
            } catch (Exception je) {
                log.debug("Response not JSON ConfigServerResponse, will try YAML/props parsing");
            }

            // Try parsing as YAML into a Map of properties
            try {
                Map<?,?> parsed = yaml.readValue(body, Map.class);
                // If parsed contains propertySources, attempt to map to typed response
                if (parsed.containsKey("propertySources")) {
                    return json.convertValue(parsed, ConfigServerResponse.class);
                }
                // Otherwise, treat the parsed map as the 'source' of a single propertySource
                ConfigServerResponse out = new ConfigServerResponse();
                PropertySource ps = new PropertySource();
                ps.name = pathWithExtension;
                ps.source = (Map<String, Object>) parsed;
                out.propertySources = List.of(ps);
                return out;
            } catch (Exception ye) {
                log.warn("Failed to parse file response as YAML: {}", ye.toString());
                return null;
            }
        } catch (WebClientResponseException wre) {
            log.warn("HTTP error fetching file {}: {}", uri, wre.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Error fetching file {}: {}", uri, ex.toString());
            return null;
        }
    }

    /**
     * Convenience: return the first property source 'source' map for a file endpoint.
     */
    @Cacheable(cacheNames = "configFile", key = "#profile + '|' + #label + '|' + #pathWithExtension")
    public Optional<Map<String, Object>> getFileSource(String profile, String label, String pathWithExtension) {
        ConfigServerResponse resp = getFile(profile, label, pathWithExtension);
        if (resp == null || resp.propertySources == null || resp.propertySources.isEmpty()) return Optional.empty();
        return Optional.ofNullable(resp.propertySources.get(0).source);
    }

    @CacheEvict(cacheNames = {"configFile", "appSource"}, allEntries = true)
    public void evictAllConfigCaches() {
        // Intended to be called when config changes are known (e.g., via actuator endpoint or CI hook).
        log.info("Evicted all config caches");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigServerResponse {
        @JsonProperty("name")
        public String name;
        @JsonProperty("profiles")
        public List<String> profiles;
        @JsonProperty("label")
        public String label;
        @JsonProperty("version")
        public String version;
        @JsonProperty("propertySources")
        public List<PropertySource> propertySources;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertySource {
        @JsonProperty("name")
        public String name;
        @JsonProperty("source")
        public Map<String, Object> source;
    }
}
