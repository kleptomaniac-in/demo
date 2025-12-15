package com.pdfgen.service;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Flexible payload pre-processor driven by YAML configuration rules.
 * Supports multiple client payload structures without code changes.
 * 
 * Configuration defines:
 * - Source paths (where to find data in payload)
 * - Filter rules (how to extract specific items from arrays)
 * - Target keys (where to put extracted data)
 */
@Service
public class ConfigurablePayloadPreProcessor {
    
    private Map<String, PreProcessingRules> rulesCache = new HashMap<>();
    
    /**
     * Pre-process payload using configuration rules.
     * 
     * @param payload Original nested payload
     * @param rulesConfigPath Path to YAML rules file (e.g., "preprocessing/client-a-rules.yml")
     * @return Flattened payload based on rules
     */
    public Map<String, Object> preProcess(Map<String, Object> payload, String rulesConfigPath) {
        PreProcessingRules rules = loadRules(rulesConfigPath);
        return applyRules(payload, rules);
    }
    
    /**
     * Loads pre-processing rules from YAML configuration.
     */
    private PreProcessingRules loadRules(String configPath) {
        if (rulesCache.containsKey(configPath)) {
            return rulesCache.get(configPath);
        }
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (input == null) {
                throw new RuntimeException("Preprocessing rules not found: " + configPath);
            }
            
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            
            PreProcessingRules rules = new PreProcessingRules();
            rules.arrayFilters = (List<Map<String, Object>>) config.get("arrayFilters");
            rules.simpleExtractors = (List<Map<String, Object>>) config.get("simpleExtractors");
            rules.calculatedFields = (List<Map<String, Object>>) config.get("calculatedFields");
            
            rulesCache.put(configPath, rules);
            return rules;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load preprocessing rules: " + configPath, e);
        }
    }
    
    /**
     * Applies configured rules to transform payload.
     */
    private Map<String, Object> applyRules(Map<String, Object> payload, PreProcessingRules rules) {
        Map<String, Object> result = new HashMap<>();
        
        // Apply array filters (e.g., extract PRIMARY, SPOUSE, dependents)
        if (rules.arrayFilters != null) {
            for (Map<String, Object> filter : rules.arrayFilters) {
                applyArrayFilter(payload, filter, result);
            }
        }
        
        // Apply simple extractors (e.g., copy top-level fields)
        if (rules.simpleExtractors != null) {
            for (Map<String, Object> extractor : rules.simpleExtractors) {
                applySimpleExtractor(payload, extractor, result);
            }
        }
        
        // Apply calculated fields (e.g., counts, flags)
        if (rules.calculatedFields != null) {
            for (Map<String, Object> calc : rules.calculatedFields) {
                applyCalculatedField(result, calc);
            }
        }
        
        return result;
    }
    
    /**
     * Extracts items from arrays based on filter criteria.
     * 
     * Supports:
     * 1. Single condition: filterField + filterValue
     * 2. Multiple conditions: conditions array with AND/OR logic
     * 
     * Example single condition:
     *   filterField: "relationship"
     *   filterValue: "PRIMARY"
     * 
     * Example multiple conditions:
     *   conditions:
     *     - field: "relationship"
     *       operator: "equals"
     *       value: "DEPENDENT"
     *     - field: "age"
     *       operator: "greaterThan"
     *       value: 18
     *   conditionLogic: "AND"  # or "OR"
     */
    private void applyArrayFilter(Map<String, Object> payload, 
                                   Map<String, Object> filterConfig, 
                                   Map<String, Object> result) {
        String sourcePath = (String) filterConfig.get("sourcePath");
        String targetKey = (String) filterConfig.get("targetKey");
        String mode = (String) filterConfig.getOrDefault("mode", "first"); // first, all, indexed
        Integer maxItems = (Integer) filterConfig.get("maxItems");
        
        // Navigate to source array
        Object sourceData = resolvePath(payload, sourcePath);
        if (!(sourceData instanceof List)) {
            return; // Source not found or not an array
        }
        
        List<Map<String, Object>> sourceList = (List<Map<String, Object>>) sourceData;
        
        // Filter array using single or multiple conditions
        List<Map<String, Object>> filtered;
        
        if (filterConfig.containsKey("conditions")) {
            // Multiple conditions with AND/OR logic
            filtered = filterWithMultipleConditions(sourceList, filterConfig);
        } else {
            // Single condition (backward compatible)
            String filterField = (String) filterConfig.get("filterField");
            Object filterValue = filterConfig.get("filterValue");
            filtered = sourceList.stream()
                .filter(item -> filterValue.equals(item.get(filterField)))
                .collect(Collectors.toList());
        }
        
        // Apply mode
        switch (mode) {
            case "first":
                if (!filtered.isEmpty()) {
                    result.put(targetKey, filtered.get(0));
                }
                break;
                
            case "all":
                result.put(targetKey, filtered);
                break;
                
            case "indexed":
                // Create dependent1, dependent2, dependent3, etc.
                int limit = maxItems != null ? Math.min(maxItems, filtered.size()) : filtered.size();
                for (int i = 0; i < limit; i++) {
                    result.put(targetKey + (i + 1), filtered.get(i));
                }
                // Store overflow
                if (maxItems != null && filtered.size() > maxItems) {
                    result.put(targetKey + "Overflow", 
                        filtered.subList(maxItems, filtered.size()));
                }
                break;
        }
    }
    
    /**
     * Filters array with multiple conditions using AND/OR logic.
     */
    private List<Map<String, Object>> filterWithMultipleConditions(
            List<Map<String, Object>> sourceList,
            Map<String, Object> filterConfig) {
        
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) filterConfig.get("conditions");
        String logic = (String) filterConfig.getOrDefault("conditionLogic", "AND");
        
        return sourceList.stream()
            .filter(item -> {
                if ("OR".equalsIgnoreCase(logic)) {
                    // OR: At least one condition must match
                    return conditions.stream().anyMatch(cond -> matchesCondition(item, cond));
                } else {
                    // AND: All conditions must match
                    return conditions.stream().allMatch(cond -> matchesCondition(item, cond));
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if an item matches a single condition.
     */
    private boolean matchesCondition(Map<String, Object> item, Map<String, Object> condition) {
        String field = (String) condition.get("field");
        String operator = (String) condition.getOrDefault("operator", "equals");
        Object expectedValue = condition.get("value");
        Object actualValue = item.get(field);
        
        if (actualValue == null) {
            return false;
        }
        
        switch (operator.toLowerCase()) {
            case "equals":
                return expectedValue.equals(actualValue);
                
            case "notequals":
                return !expectedValue.equals(actualValue);
                
            case "contains":
                return actualValue.toString().contains(expectedValue.toString());
                
            case "startswith":
                return actualValue.toString().startsWith(expectedValue.toString());
                
            case "endswith":
                return actualValue.toString().endsWith(expectedValue.toString());
                
            case "greaterthan":
                return compareNumbers(actualValue, expectedValue) > 0;
                
            case "lessthan":
                return compareNumbers(actualValue, expectedValue) < 0;
                
            case "greaterthanorequal":
                return compareNumbers(actualValue, expectedValue) >= 0;
                
            case "lessthanorequal":
                return compareNumbers(actualValue, expectedValue) <= 0;
                
            case "in":
                // expectedValue should be a list
                return ((List<?>) expectedValue).contains(actualValue);
                
            case "notin":
                return !((List<?>) expectedValue).contains(actualValue);
                
            default:
                return false;
        }
    }
    
    /**
     * Compares two numbers for numeric operators.
     */
    private int compareNumbers(Object actual, Object expected) {
        double actualNum = Double.parseDouble(actual.toString());
        double expectedNum = Double.parseDouble(expected.toString());
        return Double.compare(actualNum, expectedNum);
    }
    
    /**
     * Copies fields from source to target with optional transformation.
     * 
     * Example config:
     *   sourcePath: "application.applicationId"
     *   targetKey: "applicationId"
     */
    private void applySimpleExtractor(Map<String, Object> payload,
                                      Map<String, Object> extractorConfig,
                                      Map<String, Object> result) {
        String sourcePath = (String) extractorConfig.get("sourcePath");
        String targetKey = (String) extractorConfig.get("targetKey");
        
        Object value = resolvePath(payload, sourcePath);
        if (value != null) {
            result.put(targetKey, value);
        }
    }
    
    /**
     * Calculates derived fields based on existing data.
     * 
     * Example config:
     *   type: "exists"
     *   checkKey: "spouse"
     *   targetKey: "hasSpouse"
     */
    private void applyCalculatedField(Map<String, Object> data,
                                     Map<String, Object> calcConfig) {
        String type = (String) calcConfig.get("type");
        String targetKey = (String) calcConfig.get("targetKey");
        
        switch (type) {
            case "exists":
                String checkKey = (String) calcConfig.get("checkKey");
                data.put(targetKey, data.containsKey(checkKey) && data.get(checkKey) != null);
                break;
                
            case "count":
                String countKey = (String) calcConfig.get("sourceKey");
                Object source = data.get(countKey);
                int count = (source instanceof List) ? ((List) source).size() : 0;
                data.put(targetKey, count);
                break;
                
            case "subtract":
                String minuendKey = (String) calcConfig.get("minuend");
                String subtrahendKey = (String) calcConfig.get("subtrahend");
                int minuend = (Integer) data.getOrDefault(minuendKey, 0);
                int subtrahend = (Integer) data.getOrDefault(subtrahendKey, 0);
                data.put(targetKey, Math.max(0, minuend - subtrahend));
                break;
        }
    }
    
    /**
     * Resolves dot-notation path in nested map structure.
     * Supports: "application.applicants", "member.address.city"
     */
    private Object resolvePath(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Internal class to hold preprocessing rules.
     */
    private static class PreProcessingRules {
        List<Map<String, Object>> arrayFilters;
        List<Map<String, Object>> simpleExtractors;
        List<Map<String, Object>> calculatedFields;
    }
}
