package com.example.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting values from nested payload structures using path expressions.
 * 
 * Supports paths like:
 * - "members[].products[].type"
 * - "application.applicants[].products[].productType"
 * - "enrollment.members[].coverages[].type"
 * 
 * Path syntax:
 * - "." = navigate to child object
 * - "[]" = iterate over array
 * - "fieldName" = extract value from field
 */
public class PayloadPathExtractor {
    
    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[\\]");
    
    /**
     * Extract all string values from the payload following the given path.
     * 
     * @param payload The payload map
     * @param path The path expression (e.g., "members[].products[].type")
     * @return List of extracted string values (empty if path not found)
     */
    public static List<String> extractValues(Map<String, Object> payload, String path) {
        if (payload == null || path == null || path.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Object> results = new ArrayList<>();
        results.add(payload);
        
        // Split path by "." to get segments
        String[] segments = path.split("\\.");
        
        for (String segment : segments) {
            results = processSegment(results, segment);
            if (results.isEmpty()) {
                break;
            }
        }
        
        // Convert results to strings
        return convertToStrings(results);
    }
    
    /**
     * Process a single path segment (e.g., "members[]" or "type")
     */
    private static List<Object> processSegment(List<Object> currentObjects, String segment) {
        List<Object> nextObjects = new ArrayList<>();
        
        // Check if segment has array notation
        boolean isArray = segment.endsWith("[]");
        String fieldName = isArray ? segment.substring(0, segment.length() - 2) : segment;
        
        for (Object obj : currentObjects) {
            if (!(obj instanceof Map)) {
                continue;
            }
            
            Map<String, Object> map = (Map<String, Object>) obj;
            
            if (!map.containsKey(fieldName)) {
                continue;
            }
            
            Object value = map.get(fieldName);
            
            if (isArray) {
                // Expect array, iterate over it
                if (value instanceof List) {
                    nextObjects.addAll((List) value);
                }
            } else {
                // Regular field
                nextObjects.add(value);
            }
        }
        
        return nextObjects;
    }
    
    /**
     * Convert final results to string list
     */
    private static List<String> convertToStrings(List<Object> objects) {
        List<String> strings = new ArrayList<>();
        
        for (Object obj : objects) {
            if (obj instanceof String) {
                strings.add((String) obj);
            } else if (obj != null) {
                strings.add(obj.toString());
            }
        }
        
        return strings;
    }
    
    /**
     * Extract unique values from multiple paths and normalize them.
     * 
     * @param payload The payload map
     * @param paths List of path expressions to try
     * @return Sorted, unique, lowercase values
     */
    public static List<String> extractUniqueValues(Map<String, Object> payload, List<String> paths) {
        Set<String> uniqueValues = new HashSet<>();
        
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }
        
        for (String path : paths) {
            List<String> values = extractValues(payload, path);
            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    uniqueValues.add(value.toLowerCase().trim());
                }
            }
        }
        
        // Sort for consistency
        List<String> result = new ArrayList<>(uniqueValues);
        Collections.sort(result);
        return result;
    }
    
    /**
     * Extract product types with additional fuzzy matching for product names.
     * Handles cases where products are specified as "Medical PPO" instead of "medical".
     * 
     * @param payload The payload map
     * @param paths List of path expressions
     * @return Normalized product types
     */
    public static List<String> extractProductTypes(Map<String, Object> payload, List<String> paths) {
        List<String> rawValues = extractUniqueValues(payload, paths);
        Set<String> normalizedProducts = new HashSet<>();
        
        for (String value : rawValues) {
            String normalized = normalizeProductType(value);
            if (normalized != null) {
                normalizedProducts.add(normalized);
            }
        }
        
        List<String> result = new ArrayList<>(normalizedProducts);
        Collections.sort(result);
        return result;
    }
    
    /**
     * Normalize product type strings.
     * Handles cases like "Medical PPO" -> "medical", "DENTAL" -> "dental"
     */
    private static String normalizeProductType(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        String lower = value.toLowerCase();
        
        // Direct matches
        if (lower.equals("medical") || lower.equals("dental") || 
            lower.equals("vision") || lower.equals("life")) {
            return lower;
        }
        
        // Fuzzy matching
        if (lower.contains("medical") || lower.contains("med")) {
            return "medical";
        }
        if (lower.contains("dental") || lower.contains("dent")) {
            return "dental";
        }
        if (lower.contains("vision") || lower.contains("vis")) {
            return "vision";
        }
        if (lower.contains("life")) {
            return "life";
        }
        
        // Return as-is if no match (lowercase)
        return lower;
    }
}
