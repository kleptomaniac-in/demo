package com.example.service;

import com.example.pdf.controller.GenerateRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Helper to normalize incoming payloads into a predictable shape suitable for
 * PDF mappings. The preprocessor is configurable through {@link PreprocessorConfig}
 * so it can operate against different domain shapes without hard-coded keys.
 */
public class MappingPreprocessor {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalize(GenerateRequest req) {
        return normalize(req, new PreprocessorConfig());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalize(GenerateRequest req, PreprocessorConfig cfg) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (req == null) return out;
        Object payloadObj = req.getPayload();
        if (!(payloadObj instanceof Map)) return out;

        Map<String, Object> payload = (Map<String, Object>) payloadObj;

        // Support payloads where the application object is under configured root or at root
        Map<String, Object> app = null;
        if (cfg.rootAppKey != null && payload.get(cfg.rootAppKey) instanceof Map) app = (Map<String,Object>) payload.get(cfg.rootAppKey);
        else app = payload;

        // If JSONPath selectors are provided in config, attempt to resolve
        // primary/spouse/children using them. This allows fully declarative
        // selection without embedding domain strings in the code.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.valueToTree(payload);

        // applicants list (legacy behavior uses this)
        List<Map<String, Object>> applicants = null;
        Object aobj = app.get(cfg.applicantsKey);
        if (aobj instanceof List) applicants = (List<Map<String,Object>>) aobj;

        // If arbitrary selectors are configured, treat them as a map of
        // outputKey -> JSONPath. This is fully generic: the library no
        // longer has any special-cased knowledge of domain roles when
        // selectors are supplied.
        boolean hasSelectors = cfg.selectors != null && !cfg.selectors.isEmpty();
        if (hasSelectors) {
            try {
                Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
                for (Map.Entry<String, String> se : cfg.selectors.entrySet()) {
                    String outKey = se.getKey();
                    String expr = se.getValue();
                    try {
                        Object found = JsonPath.using(conf).parse(payload).read(expr);
                        if (found instanceof Map) {
                            Map<String,Object> m = new LinkedHashMap<>((Map<String,Object>) found);
                            // normalize applicant-like maps where applicable
                            pickAddressesAndPriorCoverages(m, cfg);
                            normalizeProducts(m, cfg);
                            out.put(outKey, m);
                        } else if (found instanceof List) {
                            List<?> raw = (List<?>) found;
                            List<Object> normalized = new ArrayList<>();
                            for (Object o : raw) {
                                if (o instanceof Map) {
                                    Map<String,Object> m = new LinkedHashMap<>((Map<String,Object>) o);
                                    pickAddressesAndPriorCoverages(m, cfg);
                                    normalizeProducts(m, cfg);
                                    normalized.add(m);
                                } else {
                                    normalized.add(o);
                                }
                            }
                            out.put(outKey, normalized);
                        } else {
                            // scalar or null
                            out.put(outKey, found);
                        }
                    } catch (Exception ex) {
                        // don't fail the whole preprocess step for a single selector
                        out.put(outKey, null);
                    }
                }
            } catch (Exception ex) {
                // fall back to legacy behavior below if JsonPath evaluation fails
            }
            // Backwards-compat: if selectors used logical names like "primary" or
            // "children" but callers set output keys (e.g. primaryOutputKey,
            // childSlotPrefix), map selector results to those configured output
            // names so existing mappings continue to work without changing
            // selector names in tests/config.
            if (cfg.selectors.containsKey("primary") && cfg.primaryOutputKey != null && out.containsKey("primary")) {
                Object v = out.get("primary");
                if (v instanceof Map) out.put(cfg.primaryOutputKey, copyMapShallow((Map<String,Object>) v));
                else if (v instanceof List) {
                    List<?> l = (List<?>) v;
                    if (!l.isEmpty() && l.get(0) instanceof Map) out.put(cfg.primaryOutputKey, copyMapShallow((Map<String,Object>) l.get(0)));
                    else out.put(cfg.primaryOutputKey, v);
                } else out.put(cfg.primaryOutputKey, v);
            }
            if (cfg.selectors.containsKey("children") && out.containsKey("children")) {
                Object v = out.get("children");
                if (v instanceof List) {
                    List<?> l = (List<?>) v;
                    for (int i = 0; i < cfg.childSlotCount; i++) {
                        if (i < l.size() && l.get(i) instanceof Map) out.put(cfg.childSlotPrefix + (i+1), copyMapShallow((Map<String,Object>) l.get(i)));
                        else out.put(cfg.childSlotPrefix + (i+1), null);
                    }
                }
            }
        }

        // Legacy behavior: if no selectors supplied, fall back to type-field
        // based selection and child slot expansion. This block is only executed
        // when selectors are not provided so the library avoids embedding
        // domain-specific role logic when declarative selectors exist.
        if (!hasSelectors) {
            Map<String, Object> primary = null;
            Map<String, Object> spouse = null;
            List<Map<String, Object>> children = new ArrayList<>();

            if (applicants != null) {
                for (Map<String, Object> ap : applicants) {
                    Object t = ap.get(cfg.applicantTypeField);
                    String type = t == null ? null : String.valueOf(t);
                    if (type == null) continue;
                    if (cfg.primaryTypeValue != null && type.equalsIgnoreCase(cfg.primaryTypeValue) && primary == null) {
                        primary = new LinkedHashMap<>(ap);
                        continue;
                    }
                    if (cfg.spouseTypeValue != null && type.equalsIgnoreCase(cfg.spouseTypeValue) && spouse == null) {
                        spouse = new LinkedHashMap<>(ap);
                        continue;
                    }
                    if (cfg.childTypeValue != null && type.equalsIgnoreCase(cfg.childTypeValue)) {
                        children.add(new LinkedHashMap<>(ap));
                        continue;
                    }
                }
            }

            if (primary != null && cfg.primaryOutputKey != null) out.put(cfg.primaryOutputKey, copyMapShallow(primary));
            if (spouse != null && cfg.spouseOutputKey != null) out.put(cfg.spouseOutputKey, copyMapShallow(spouse));

            // up to configured number of children slots
            for (int i = 0; i < cfg.childSlotCount; i++) {
                if (i < children.size()) out.put(cfg.childSlotPrefix + (i+1), copyMapShallow(children.get(i)));
                else out.put(cfg.childSlotPrefix + (i+1), null);
            }

            // For each applicant, pick addresses and prior coverages and normalize products
            if (primary != null) {
                Map<String, Object> prim = primary;
                pickAddressesAndPriorCoverages(prim, cfg);
                normalizeProducts(prim, cfg);
                if (cfg.primaryOutputKey != null) out.put(cfg.primaryOutputKey, prim);
            }

            if (spouse != null) {
                pickAddressesAndPriorCoverages(spouse, cfg);
                normalizeProducts(spouse, cfg);
                if (cfg.spouseOutputKey != null) out.put(cfg.spouseOutputKey, spouse);
            }

            for (int i = 0; i < children.size() && i < cfg.childSlotCount; i++) {
                Map<String, Object> ch = children.get(i);
                pickAddressesAndPriorCoverages(ch, cfg);
                normalizeProducts(ch, cfg);
                out.put(cfg.childSlotPrefix + (i+1), ch);
            }
        }

        // preserve top-level payload for any other mappings
        out.put("originalPayload", payload);

        return out;
    }

    @SuppressWarnings("unchecked")
    private static void pickAddressesAndPriorCoverages(Map<String, Object> applicant, PreprocessorConfig cfg) {
        if (applicant == null) return;
        Object addObj = applicant.get(cfg.addressListKey);
        if (addObj instanceof List) {
            List<Map<String, Object>> addresses = (List<Map<String, Object>>) addObj;
            for (Map<String, Object> addr : addresses) {
                Object at = addr.get(cfg.addressTypeField);
                String atype = at == null ? null : String.valueOf(at);
                if (atype == null) continue;
                if (atype.equalsIgnoreCase(cfg.homeAddressValue) && cfg.homeAddressOutputKey != null) applicant.put(cfg.homeAddressOutputKey, addr);
                else if (atype.equalsIgnoreCase(cfg.billingAddressValue) && cfg.billingAddressOutputKey != null) applicant.put(cfg.billingAddressOutputKey, addr);
            }
        }

        Object priorObj = applicant.get(cfg.priorCoveragesKey);
        if (priorObj instanceof List) {
            List<Map<String, Object>> priors = (List<Map<String, Object>>) priorObj;
            for (Map<String, Object> p : priors) {
                Object ct = p.get(cfg.coverageTypeField);
                String ctype = ct == null ? null : String.valueOf(ct);
                if (ctype == null) continue;
                String outKey = cfg.coverageOutputMap.getOrDefault(ctype.toUpperCase(), null);
                if (outKey != null && !applicant.containsKey(outKey)) applicant.put(outKey, p);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void normalizeProducts(Map<String, Object> applicant, PreprocessorConfig cfg) {
        if (applicant == null) return;
        Object prodObj = applicant.get(cfg.productsKey);
        if (prodObj == null) return;
        // if already a map keyed by product type, leave as-is
        if (prodObj instanceof Map) return;

        if (prodObj instanceof List) {
            List<Map<String, Object>> prodList = (List<Map<String, Object>>) prodObj;
            Map<String, Object> prodMap = new LinkedHashMap<>();
            for (Map<String, Object> p : prodList) {
                Object t = p.get(cfg.productTypeField);
                if (t == null) t = p.get("productType");
                String type = t == null ? null : String.valueOf(t);
                if (type == null) continue;
                prodMap.put(type, p);
            }
            applicant.put(cfg.productsKey, prodMap);
        }
    }

    private static Map<String, Object> copyMapShallow(Map<String, Object> src) {
        if (src == null) return null;
        return new LinkedHashMap<>(src);
    }
}
