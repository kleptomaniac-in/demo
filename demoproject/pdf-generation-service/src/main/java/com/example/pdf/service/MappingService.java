package com.example.pdf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * Service responsible for resolving and composing mapping documents used to
 * populate PDFs.
 *
 * <p>Responsibilities:
 * - Resolve a single mapping document from a per-request inline override (YAML)
 *   or by fetching an application-style mapping from the Config Server.
 * - Compose mapping documents from multiple ordered candidate sources (files
 *   and application property sources) and deep-merge them.
 * - Support pattern expansion in configured candidate lists. Patterns may
 *   include placeholders: {@code {template}}, {@code {product}}, {@code {market}},
 *   and {@code {state}}.
 *
 * <p>Placeholder expansion rules:
 * - For each placeholder, the service first looks for a payload list under
 *   common keys in the request payload: the plural name (e.g. {@code products}),
 *   the {@code <name>List} form (e.g. {@code productList}), and the bare key
 *   (e.g. {@code product}). If a payload list exists, each element will be
 *   used to produce one candidate.
 * - If no payload list exists for a placeholder, the service falls back to the
 *   single-valued getters on {@link com.example.pdf.controller.GenerateRequest}
 *   (for example, {@code req.getProductType()} for {@code {product}}).
 * - When multiple placeholders map to lists (e.g. {@code {product}} and
 *   {@code {market}}), the implementation emits the Cartesian product of
 *   values (ordered by the configured pattern order and the list order).
 * - Recognized placeholders with no value (null, empty, or the literal
 *   string "null") cause the pattern to be skipped rather than producing a
 *   malformed candidate. Unknown placeholders are preserved as literals.
 *
 * <p>Examples:
 * - Config candidate {@code mappings/products/{product}} with payload
 *   {@code { products: [A,B] }} expands to {@code [mappings/products/A,
 *   mappings/products/B]}.
 * - Config candidate {@code mappings/templates/{product}/{template}} with
 *   {@code products: [X,Y]} and {@code templateName: inv} expands to
 *   {@code [mappings/templates/X/inv, mappings/templates/Y/inv]}.
 */
public class MappingService {

    private static final Logger log = LoggerFactory.getLogger(MappingService.class);

    private final ConfigServerClient configClient;
    private MappingProperties mappingProperties;
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper json = new ObjectMapper();

    public MappingService() {
        this.configClient = new ConfigServerClient();
    }

    // Constructor to inject custom client
    public MappingService(ConfigServerClient client) {
        this.configClient = client == null ? new ConfigServerClient() : client;
    }

    // Resolve mapping either from override YAML or from Config Server
    public Map<String, String> resolveMapping(com.example.pdf.controller.GenerateRequest req) throws Exception {
        log.debug("Resolving mapping for clientService='{}', templateName='{}', label='{}'", req.getClientService(), req.getTemplateName(), req.getLabel());
        if (StringUtils.hasText(req.getMappingOverride())) {
            log.debug("Using mapping override YAML:\n{}", req.getMappingOverride());
            Map<?,?> parsed = yaml.readValue(req.getMappingOverride(), Map.class);
            return flattenToStringMap(parsed);
        }

        String label = StringUtils.hasText(req.getLabel()) ? req.getLabel() : "main";
        log.debug("Fetching mapping from Config Server with label='{}'", label);
        String mappingName = req.getClientService() + "-" + req.getTemplateName();
        log.debug("Mapping name: {}", mappingName);
        ConfigServerClient.ConfigServerResponse resp = configClient.getApplicationConfig(mappingName, "default", label);
        if (resp == null) {
            log.debug("Config server returned null for application {}", mappingName);
            return Map.of();
        }
        List<ConfigServerClient.PropertySource> propertySources = resp.propertySources;
        if (propertySources == null || propertySources.isEmpty()) return Map.of();
        Map<String, Object> source = propertySources.get(0).source;
        if (source == null) return Map.of();

        Map<String, String> result = new LinkedHashMap<>();
        for (Object k : source.keySet()) {
            Object v = source.get(k);
            result.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
        }
        return result;
    }
    
    public com.example.pdf.model.MappingDocument resolveMappingDocument(com.example.pdf.controller.GenerateRequest req) throws Exception {
        if (StringUtils.hasText(req.getMappingOverride())) {
            Map<?,?> parsed = yaml.readValue(req.getMappingOverride(), Map.class);
            Map<String, Object> nested = unflatten(parsed);
            log.debug("Unflattened inline mapping override:\n{}", yaml.writeValueAsString(nested));
            if (nested.containsKey("pdf") && !nested.containsKey("mapping")) {
                Object pdfNode = nested.remove("pdf");
                Map<String, Object> mappingNode = new LinkedHashMap<>();
                mappingNode.put("pdf", pdfNode);
                nested.put("mapping", mappingNode);
                log.debug("Wrapped root 'pdf' under 'mapping' for inline override\n{}", yaml.writeValueAsString(nested));
            }
            return json.convertValue(nested, com.example.pdf.model.MappingDocument.class);
        }
        
        String label = StringUtils.hasText(req.getLabel()) ? req.getLabel() : "main";
        log.debug("Fetching mapping document from Config Server with label='{}'", label);
        String mappingName = req.getClientService() + "-" + req.getTemplateName();
        log.debug("Mapping name: {}", mappingName);
        ConfigServerClient.ConfigServerResponse resp = configClient.getApplicationConfig(mappingName, "default", label);
        if (resp == null) {
            log.debug("Empty response body from Config Server");
            return new com.example.pdf.model.MappingDocument();
        }

        List<ConfigServerClient.PropertySource> propertySources = resp.propertySources;
        if (propertySources == null || propertySources.isEmpty()) {
            log.debug("No propertySources found in Config Server response");
            return new com.example.pdf.model.MappingDocument();
        }

        Map<String, Object> source = propertySources.get(0).source;
        if (source == null) {
            log.debug("No source found in first propertySource");
            return new com.example.pdf.model.MappingDocument();
        }

        Map<String, Object> nested = unflatten(source);
        log.debug("Unflattened mapping document:\n{}", yaml.writeValueAsString(nested));

        if (nested.containsKey("pdf") && !nested.containsKey("mapping")) {
            Object pdfNode = nested.remove("pdf");
            Map<String, Object> mappingNode = new LinkedHashMap<>();
            mappingNode.put("pdf", pdfNode);
            nested.put("mapping", mappingNode);
            log.debug("Wrapped root 'pdf' under 'mapping' for compatibility\n{}", yaml.writeValueAsString(nested));
        }

        return json.convertValue(nested, com.example.pdf.model.MappingDocument.class);
    }

    /**
     * Compose mapping documents from multiple candidate sources based on the supplied attributes
     * (productType, marketCategory, state, templateName). The order is from least-specific
     * to most-specific; later maps override earlier ones.
     */
    public com.example.pdf.model.MappingDocument composeMappingDocument(com.example.pdf.controller.GenerateRequest req) throws Exception {
        String label = StringUtils.hasText(req.getLabel()) ? req.getLabel() : "main";

        List<String> candidates = buildCandidates(req);

        MappingComposer composer = new MappingComposer(configClient);
        Map<String, Object> merged = composer.compose(req, label, candidates);

        // If a per-request mappingOverride is provided, merge it into the composed mapping
        if (StringUtils.hasText(req.getMappingOverride())) {
            Map<?,?> parsed = yaml.readValue(req.getMappingOverride(), Map.class);
            Map<String, Object> overrideNested;
            // If override is supplied as dotted keys (e.g. 'mapping.pdf.field.x'), unflatten it
            boolean looksFlat = parsed.keySet().stream().map(Object::toString).anyMatch(k -> k.contains("."));
            if (looksFlat) {
                overrideNested = unflatten(parsed);
            } else {
                // Otherwise assume YAML is already nested
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) parsed;
                overrideNested = casted;
            }

            // If the override contains a top-level 'pdf' but not 'mapping', wrap under 'mapping'
            if (overrideNested.containsKey("pdf") && !overrideNested.containsKey("mapping")) {
                Object pdfNode = overrideNested.remove("pdf");
                Map<String, Object> mappingNode = new LinkedHashMap<>();
                mappingNode.put("pdf", pdfNode);
                overrideNested.put("mapping", mappingNode);
            }

            // Deep-merge override into composed mapping (override wins)
            deepMerge(merged, overrideNested);
        }

        return json.convertValue(merged, com.example.pdf.model.MappingDocument.class);
    }

    /**
     * Return the configured candidate order (raw patterns) if present.
     * This shows the order as configured (placeholders still present).
     */
    public List<String> getConfiguredCandidateOrder() {
        return mappingProperties == null || mappingProperties.getCandidateOrder() == null
                ? java.util.List.of()
                : java.util.List.copyOf(mappingProperties.getCandidateOrder());
    }

    @Autowired(required = false)
    public void setMappingProperties(MappingProperties mappingProperties) {
        this.mappingProperties = mappingProperties;
    }

    // With Spring Cloud Config client enabled, `MappingProperties` will be bound from the
    // remote configuration automatically via the Spring Environment. No manual fetch required.

    // Expand a pattern into one or more concrete candidate strings using values from the request.
    // If the request payload contains a list under common keys ("products", "productList", "product"),
    // and the pattern contains a "{product}" placeholder, this will emit one candidate per product
    // element in the list, preserving configured ordering.
    /**
     * Expand a candidate pattern into one or more concrete candidate strings
     * using values available on the request.
     *
     * <p>Behavior:
     * - Discovers placeholders in {@code pattern} (e.g. {@code {product}}).
     * - For each placeholder, if a payload list exists under plural names
     *   (e.g. {@code products}), {@code <name>List}, or the bare key, every
     *   element of that list will be used for expansion (preserving list order).
     * - If no payload list exists for a placeholder, the method falls back to
     *   single-valued getters on the request (for known placeholders).
     * - If multiple placeholders expand to lists, the Cartesian product of
     *   values is returned (pattern order is preserved).
     * - If a recognized placeholder (template/product/market/state) has no
     *   value, the pattern is skipped (no candidate emitted).
     * - Unknown placeholders that cannot be resolved are preserved as literals.
     *
     * <p>Examples:
     * {@code pattern="mappings/products/{product}" + payload {products:[A,B]}
     *  -> ["mappings/products/A","mappings/products/B"]}
     */
    private java.util.List<String> expandPatternToList(String pattern, com.example.pdf.controller.GenerateRequest req) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (pattern == null) return out;

        Object payloadObj = req.getPayload();
        java.util.Map<?,?> payload = payloadObj instanceof java.util.Map ? (java.util.Map<?,?>) payloadObj : java.util.Map.of();

        // Collect placeholders in the pattern in order
        java.util.LinkedHashSet<String> placeholders = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([^}]+)\\}").matcher(pattern);
        while (m.find()) placeholders.add(m.group(1));

        // If no placeholders, do a single substitution using available single-valued getters
        if (placeholders.isEmpty()) {
            String single = pattern;
            single = single.replace("{template}", req.getTemplateName() == null ? "" : req.getTemplateName());
            single = single.replace("{product}", req.getProductType() == null ? "" : req.getProductType());
            single = single.replace("{market}", req.getMarketCategory() == null ? "" : req.getMarketCategory());
            single = single.replace("{state}", req.getState() == null ? "" : req.getState());
            single = single.trim();
            if (StringUtils.hasText(single) && !single.toLowerCase().contains("null")) out.add(single);
            return out;
        }

        // For each placeholder, build a list of candidate values (from payload lists or single getters)
        java.util.List<java.util.List<String>> valueLists = new java.util.ArrayList<>();
        for (String ph : placeholders) {
            java.util.List<String> vals = new java.util.ArrayList<>();

            // check payload for list under common keys: plural, 'List', or bare
            Object candidate = null;
            if (payload.containsKey(ph + "s")) candidate = payload.get(ph + "s");
            else if (payload.containsKey(ph + "List")) candidate = payload.get(ph + "List");
            else if (payload.containsKey(ph)) candidate = payload.get(ph);

            if (candidate instanceof java.util.List) {
                for (Object el : (java.util.List<?>) candidate) {
                    if (el == null) continue;
                    String s = String.valueOf(el).trim();
                    if (StringUtils.hasText(s) && !s.equalsIgnoreCase("null")) vals.add(s);
                }
            }

            // If no payload list found, fall back to known single-valued getters
            if (vals.isEmpty()) {
                String single = null;
                switch (ph) {
                    case "template": single = req.getTemplateName(); break;
                    case "product": single = req.getProductType(); break;
                    case "market": single = req.getMarketCategory(); break;
                    case "state": single = req.getState(); break;
                    default: single = null; break;
                }
                if (single != null && StringUtils.hasText(single) && !single.equalsIgnoreCase("null")) {
                    vals.add(single.trim());
                } else {
                    // For recognized placeholders (template/product/market/state) with no value,
                    // do NOT preserve them as literals — treat as missing so the pattern will be skipped.
                    if (!("template".equals(ph) || "product".equals(ph) || "market".equals(ph) || "state".equals(ph))) {
                        vals.add("{" + ph + "}");
                    }
                    // otherwise leave vals empty to indicate this pattern cannot be expanded
                }
            }

            valueLists.add(vals);
        }

        // If any placeholder produced no values (recognized placeholder missing), skip expansion
        for (java.util.List<String> l : valueLists) {
            if (l.isEmpty()) return out;
        }

        // Cartesian product across valueLists
        int n = valueLists.size();
        int[] idx = new int[n];
        outer: while (true) {
            java.util.Iterator<String> it = placeholders.iterator();
            java.util.Map<String,String> map = new java.util.HashMap<>();
            int i = 0;
            while (it.hasNext()) {
                String ph = it.next();
                java.util.List<String> list = valueLists.get(i);
                if (list.isEmpty()) continue outer;
                map.put(ph, list.get(idx[i]));
                i++;
            }

            String s = pattern;
            for (java.util.Map.Entry<String,String> e : map.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue());
            }
            s = s.trim();
            if (StringUtils.hasText(s) && !s.toLowerCase().contains("null")) out.add(s);

            // increment index vector
            int pos = n - 1;
            while (pos >= 0) {
                idx[pos]++;
                if (idx[pos] < valueLists.get(pos).size()) break;
                idx[pos] = 0;
                pos--;
            }
            if (pos < 0) break;
        }

        return out;
    }

    /*
     * Build the ordered candidate list based on configured MappingProperties or fallback ordering.
     * Package-private so unit tests can call it.
     */
    /**
     * Build the ordered candidate list for mapping composition.
     *
     * <p>If {@link MappingProperties#getCandidateOrder()} is configured, each
     * pattern is expanded using {@link #expandPatternToList(String, GenerateRequest)}
     * which supports payload list expansion and placeholder substitution.
     *
     * <p>If no configuration is present, a conservative fallback ordering is
     * used: {@code mappings/base-application}, optional template, product(s),
     * market, state, and a combined product/template path.
     */
    List<String> buildCandidates(com.example.pdf.controller.GenerateRequest req) {
        List<String> candidates = new java.util.ArrayList<>();

        List<String> configured = mappingProperties == null ? null : mappingProperties.getCandidateOrder();
        if (configured == null || configured.isEmpty()) {
            // fallback to previous default ordering
            candidates.add("mappings/base-application");
            if (StringUtils.hasText(req.getTemplateName())) candidates.add(String.format("mappings/templates/%s", req.getTemplateName()));

            // support product list in payload or single productType
            Object payloadObj = req.getPayload();
            java.util.List<?> productList = null;
            if (payloadObj instanceof java.util.Map) {
                java.util.Map<?,?> pm = (java.util.Map<?,?>) payloadObj;
                Object p = pm.get("products");
                if (p == null) p = pm.get("productList");
                if (p == null) p = pm.get("product");
                if (p instanceof java.util.List) productList = (java.util.List<?>) p;
            }

            if (productList != null && !productList.isEmpty()) {
                for (Object el : productList) {
                    String prod = el == null ? "" : String.valueOf(el);
                    if (StringUtils.hasText(prod)) candidates.add(String.format("mappings/products/%s", prod));
                }
            } else if (StringUtils.hasText(req.getProductType())) {
                candidates.add(String.format("mappings/products/%s", req.getProductType()));
            }

            if (StringUtils.hasText(req.getMarketCategory())) candidates.add(String.format("mappings/markets/%s", req.getMarketCategory()));
            if (StringUtils.hasText(req.getState())) candidates.add(String.format("mappings/states/%s", req.getState()));

            if (StringUtils.hasText(req.getProductType()) && StringUtils.hasText(req.getTemplateName())) candidates.add(String.format("mappings/templates/%s/%s", req.getProductType(), req.getTemplateName()));
        } else {
            for (String pattern : configured) {
                java.util.List<String> expandedList = expandPatternToList(pattern, req);
                for (String expanded : expandedList) {
                    if (StringUtils.hasText(expanded)) {
                        candidates.add(expanded);
                    }
                }
            }
        }
        return candidates;
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

    // Resolve a dotted path into the payload map
    public Object resolvePath(Map<String, Object> payload, String path) {
        log.debug("resolvePath:Resolving path '{}' in payload", path);
        if (path == null) return null;
        String[] parts = path.split("\\.");
        Object cur = payload;
        for (String p : parts) {
            if (cur == null) return null;
            // support bracket syntax like items[0] or items[0][1]
            String remaining = p;
            // if current object is a Map, we expect a key next
            if (cur instanceof Map) {
                Map m = (Map) cur;
                // handle leading bracket, e.g. [0]
                if (remaining.startsWith("[")) {
                    // fall through to list handling below
                } else {
                    // extract base key before any bracket
                    int bi = remaining.indexOf('[');
                    String key = bi == -1 ? remaining : remaining.substring(0, bi);
                    cur = m.get(key);
                    // if there are bracketed indices after the key, process them
                    if (bi != -1) {
                        remaining = remaining.substring(bi);
                    } else {
                        continue;
                    }
                }
            }

            // now handle list indices if the current object is a List or remaining begins with brackets
            // remaining may be like "[0][1]" or a plain numeric index like "0"
            while (remaining != null && remaining.length() > 0) {
                if (remaining.startsWith("[")) {
                    int idxEnd = remaining.indexOf(']');
                    if (idxEnd == -1) return null;
                    String idxStr = remaining.substring(1, idxEnd).trim();
                    int idx;
                    try {
                        idx = Integer.parseInt(idxStr);
                    } catch (NumberFormatException nfe) {
                        return null;
                    }
                    if (!(cur instanceof java.util.List)) return null;
                    java.util.List list = (java.util.List) cur;
                    if (idx < 0 || idx >= list.size()) return null;
                    cur = list.get(idx);
                    remaining = remaining.substring(idxEnd + 1);
                } else {
                    // if remaining is a plain number (dot-separated), treat as index
                    if (remaining.matches("^\\d+$")) {
                        int idx = Integer.parseInt(remaining);
                        if (!(cur instanceof java.util.List)) return null;
                        java.util.List list = (java.util.List) cur;
                        if (idx < 0 || idx >= list.size()) return null;
                        cur = list.get(idx);
                        remaining = "";
                    } else {
                        // nothing to process, break
                        break;
                    }
                }
            }
        }
        log.debug("resolvePath: Resolved value: {}", (cur == null ? "null" : cur.toString()));
        return cur;
    }

    // flatten nested YAML/Map into flat string->string map by joining keys with '.'
    private Map<String, String> flattenToStringMap(Map<?,?> input) {
        Map<String, String> out = new LinkedHashMap<>();
        flatten("", input, out);
        return out;
    }

    private void flatten(String prefix, Map<?,?> m, Map<String, String> out) {
        for (Object k : m.keySet()) {
            String key = prefix.isEmpty() ? String.valueOf(k) : prefix + "." + String.valueOf(k);
            Object v = m.get(k);
            if (v instanceof Map) {
                flatten(key, (Map<?,?>) v, out);
            } else {
                out.put(key, v == null ? "" : String.valueOf(v));
            }
        }
    }
    
    // Unflatten a map with dotted keys into a nested map
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
    
    // convenience: extract field mapping (pdf.field.*) as flat map of pdfField->payloadPath
    public Map<String, String> extractFieldMap(com.example.pdf.model.MappingDocument doc) {
        if (doc == null || doc.getMapping() == null || doc.getMapping().getPdf() == null) return Map.of();
        Map<String, String> fields = doc.getMapping().getPdf().getField();
        if (fields == null) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            out.put(e.getKey(), sanitizePath(e.getValue()));
        }
        return out;
    }

    // Strip common prefixes so mapping paths resolve relative to the payload map
    private String sanitizePath(String p) {
        if (p == null) return null;
        p = p.trim();
        if (p.startsWith("payload.")) return p.substring("payload.".length());
        if (p.startsWith("$.")) return p.substring(2);
        return p;
    }
}
