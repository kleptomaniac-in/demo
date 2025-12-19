package com.example.service;

import com.example.pdf.controller.GenerateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compose mapping fragments by fetching candidate fragments (via mapping sources)
 * and deep-merging them in order.
 */
public class MappingComposer {

    private static final Logger log = LoggerFactory.getLogger(MappingComposer.class);

    private final ConfigServerClient client;

    public MappingComposer(ConfigServerClient client) {
        this.client = client;
    }

    /**
     * Compose using textual candidate names. If a candidate contains '/', it is
     * treated as a repo file path (ConfigFileMappingSource will be used); otherwise
     * it is treated as an application name (ApplicationMappingSource).
     */
    public Map<String, Object> compose(GenerateRequest req, String label, List<String> candidates) {
        Map<String, Object> merged = new LinkedHashMap<>();
        // per-compose cache to avoid duplicate HTTP calls for the same candidate
        Map<String, Optional<Map<String, Object>>> cache = new HashMap<>();

        for (String candidate : candidates) {
            String raw = candidate == null ? "" : candidate.trim();
            if (raw.isEmpty()) continue;

            try {
                // normalize key used for caching & dispatch
                String key;
                MappingSource src;
                if (raw.startsWith("file:")) {
                    String p = raw.substring("file:".length());
                    if (!p.endsWith(".yml") && !p.endsWith(".yaml") && !p.endsWith(".json")) {
                        p = p + ".yml";
                    }
                    key = "file:" + p; // canonical file key (with extension)
                    src = new ConfigFileMappingSource(client, p);
                } else if (raw.startsWith("app:")) {
                    String appName = raw.substring("app:".length());
                    key = "app:" + appName;
                    src = new ApplicationMappingSource(client, appName);
                } else if (raw.startsWith("mappings/") || raw.endsWith(".yml") || raw.endsWith(".yaml") || raw.endsWith(".json") || raw.contains("/")) {
                    // treat as file path by default when it looks like one
                    String p = raw;
                    if (!p.endsWith(".yml") && !p.endsWith(".yaml") && !p.endsWith(".json")) {
                        p = p + ".yml";
                    }
                    key = "file:" + p;
                    src = new ConfigFileMappingSource(client, p);
                } else {
                    String appName = raw;
                    key = "app:" + appName;
                    src = new ApplicationMappingSource(client, appName);
                }

                Optional<Map<String, Object>> fragment = cache.get(key);
                if (fragment == null) {
                    try {
                        fragment = src.fetch(req, label);
                    } catch (Exception e) {
                        log.warn("Error fetching candidate {}: {}", candidate, e.toString());
                        log.debug("Candidate fetch error", e);
                        fragment = Optional.empty();
                    }
                    cache.put(key, fragment);
                }

                if (fragment.isPresent()) {
                    Map<String, Object> nested = unflatten(fragment.get());
                    if (nested.containsKey("pdf") && !nested.containsKey("mapping")) {
                        Object pdfNode = nested.remove("pdf");
                        Map<String, Object> mappingNode = new LinkedHashMap<>();
                        mappingNode.put("pdf", pdfNode);
                        nested.put("mapping", mappingNode);
                    }
                    deepMerge(merged, nested);
                }
            } catch (Exception ex) {
                log.warn("Ignoring candidate {} due to error: {}", candidate, ex.toString());
                log.debug("Candidate processing error", ex);
            }
        }
        return merged;
    }

    // Unflatten a map with dotted keys into a nested map
    @SuppressWarnings("unchecked")
    private Map<String, Object> unflatten(Map<?,?> flat) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (Object ko : flat.keySet()) {
            String k = String.valueOf(ko);
            Object v = flat.get(ko);
            String[] parts = k.split("\\.");
            Map<String, Object> cur = root;
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
                if (i == parts.length - 1) {
                    cur.put(p, v);
                } else {
                    Object next = cur.get(p);
                    if (!(next instanceof Map)) {
                        Map<String, Object> nm = new LinkedHashMap<>();
                        cur.put(p, nm);
                        cur = nm;
                    } else {
                        cur = (Map<String, Object>) next;
                    }
                }
            }
        }
        return root;
    }

    // Deep-merge override into base. For Map values, merge recursively; lists are replaced.
    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> base, Map<String, Object> override) {
        for (Map.Entry<String, Object> e : override.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (v instanceof Map && base.get(k) instanceof Map) {
                deepMerge((Map<String, Object>) base.get(k), (Map<String, Object>) v);
            } else {
                base.put(k, v);
            }
        }
    }
}
