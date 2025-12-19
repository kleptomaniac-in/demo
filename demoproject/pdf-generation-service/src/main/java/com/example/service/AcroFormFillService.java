package com.example.service;

import com.example.function.FunctionExpressionResolver;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for filling AcroForm PDF templates with data mapping.
 * Supports function expressions for field transformations.
 * 
 * Performance optimizations:
 * - Path resolution caching per payload
 * - Early termination for single-match filters
 * - Optimized bracket-aware path splitting
 */
@Service
public class AcroFormFillService {
    
    private final FunctionExpressionResolver functionResolver;
    
    // Thread-local cache for path resolutions within a single form fill operation
    private final ThreadLocal<Map<String, Object>> pathCache = ThreadLocal.withInitial(HashMap::new);
    
    // Thread-local cache for filtered list results (e.g., "applicants[relationship=PRIMARY]")
    private final ThreadLocal<Map<String, Object>> filterCache = ThreadLocal.withInitial(HashMap::new);
    
    public AcroFormFillService(FunctionExpressionResolver functionResolver) {
        this.functionResolver = functionResolver;
    }
    
    /**
     * Clear thread-local caches. Useful for testing.
     */
    void clearCaches() {
        pathCache.get().clear();
        filterCache.get().clear();
    }
    
    /**
     * Fill AcroForm PDF using field mappings from configuration
     * 
     * @param templatePath Path to AcroForm PDF template
     * @param fieldMappings Map of PDF field name → payload path
     * @param payload Data to fill into form
     * @return Filled PDF as byte array
     */
    public byte[] fillAcroForm(String templatePath, Map<String, String> fieldMappings, Map<String, Object> payload) throws IOException {
        // Clear thread-local caches at start of each form fill
        pathCache.get().clear();
        filterCache.get().clear();
        
        // Load the AcroForm template
        try (PDDocument document = loadTemplate(templatePath)) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            if (acroForm == null) {
                throw new IllegalArgumentException("PDF does not contain an AcroForm: " + templatePath);
            }
            
            // Resolve all field values first (leveraging our caching optimizations)
            Map<String, Object> fieldValues = new LinkedHashMap<>();
            
            for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
                String pdfFieldName = mapping.getKey();
                String payloadPath = mapping.getValue();
                
                // Check if it's a function expression
                Object value;
                if (functionResolver.isFunction(payloadPath)) {
                    // Resolve function expression
                    String resolvedValue = functionResolver.resolve(payloadPath, payload);
                    value = resolvedValue;
                } else {
                    // Resolve value from payload using path notation (with caching)
                    value = resolveValue(payload, payloadPath);
                }
                
                if (value != null) {
                    fieldValues.put(pdfFieldName, value);
                }
            }
            
            // Batch fill all fields at once for better performance
            fillFieldsBatch(acroForm, fieldValues);
            
            // Flatten the form (optional - makes fields non-editable)
            // acroForm.flatten();
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } finally {
            // Clear caches after form fill completes
            pathCache.remove();
            filterCache.remove();
        }
    }
    
    /**
     * Load AcroForm template from file system with caching.
     * Returns byte array instead of PDDocument to make it cacheable.
     */
    @Cacheable(value = "acroformTemplates", key = "#templatePath")
    public byte[] loadTemplateBytes(String templatePath) throws IOException {
        System.out.println("Loading AcroForm template from disk (cache miss): " + templatePath);
        
        String fullPath = "../config-repo/acroforms/" + templatePath;
        
        if (!Files.exists(Paths.get(fullPath))) {
            fullPath = "acroforms/" + templatePath;
        }
        
        return Files.readAllBytes(Paths.get(fullPath));
    }
    
    /**
     * Load PDDocument from cached bytes
     */
    private PDDocument loadTemplate(String templatePath) throws IOException {
        byte[] templateBytes = loadTemplateBytes(templatePath);
        return PDDocument.load(new ByteArrayInputStream(templateBytes));
    }
    
    /**
     * Fill a single form field with a value
     */
    private void fillField(PDAcroForm acroForm, String fieldName, Object value) throws IOException {
        PDField field = acroForm.getField(fieldName);
        
        if (field == null) {
            System.err.println("Warning: Field not found in PDF: " + fieldName);
            return;
        }
        
        try {
            // Convert value to string for form field
            String stringValue = convertToString(value);
            field.setValue(stringValue);
            
            System.out.println("Filled field: " + fieldName + " = " + stringValue);
        } catch (Exception e) {
            System.err.println("Error filling field " + fieldName + ": " + e.getMessage());
        }
    }
    
    /**
     * Batch fill multiple form fields. This is more efficient than calling fillField() 
     * repeatedly as it can defer certain expensive operations until all fields are set.
     * 
     * @param acroForm The AcroForm to fill
     * @param fieldValues Map of field name to value
     */
    private void fillFieldsBatch(PDAcroForm acroForm, Map<String, Object> fieldValues) throws IOException {
        // Set needAppearances to true - this defers appearance stream generation
        // The PDF reader will generate appearances when the form is opened
        acroForm.setNeedAppearances(true);
        
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            PDField field = acroForm.getField(fieldName);
            
            if (field == null) {
                System.err.println("Warning: Field not found in PDF: " + fieldName);
                continue;
            }
            
            try {
                // Convert value to string for form field
                String stringValue = convertToString(value);
                
                // Set value without triggering appearance generation
                // The appearance will be generated by the PDF reader
                field.setValue(stringValue);
                
                System.out.println("Filled field: " + fieldName + " = " + stringValue);
            } catch (Exception e) {
                System.err.println("Error filling field " + fieldName + ": " + e.getMessage());
            }
        }
        
        // Appearance streams will be generated by PDF reader software when needed
        // This is significantly faster than generating them for each field individually
    }
    
    /**
     * Expand pattern-based field mappings into individual field mappings
     * 
     * Example pattern:
     *   fieldPattern: "Dependent{n}_*"
     *   source: "applicants[relationship=DEPENDENT][{n}]"
     *   maxIndex: 2
     *   fields: { "FirstName": "demographic.firstName", "LastName": "demographic.lastName" }
     * 
     * Expands to:
     *   "Dependent1_FirstName" → "applicants[relationship=DEPENDENT][0].demographic.firstName"
     *   "Dependent1_LastName" → "applicants[relationship=DEPENDENT][0].demographic.lastName"
     *   "Dependent2_FirstName" → "applicants[relationship=DEPENDENT][1].demographic.firstName"
     *   ...
     * 
     * @param patterns List of field patterns from configuration
     * @return Expanded field mappings
     */
    public Map<String, String> expandPatterns(List<FieldPattern> patterns) {
        Map<String, String> expanded = new HashMap<>();
        
        if (patterns == null || patterns.isEmpty()) {
            return expanded;
        }
        
        for (FieldPattern pattern : patterns) {
            String fieldPattern = pattern.getFieldPattern();
            String source = pattern.getSource();
            int maxIndex = pattern.getMaxIndex();
            Map<String, String> fields = pattern.getFields();
            
            if (fieldPattern == null || source == null || fields == null) {
                System.err.println("Warning: Invalid pattern configuration, skipping");
                continue;
            }
            
            // Expand pattern for each index (0 to maxIndex)
            for (int i = 0; i <= maxIndex; i++) {
                // Replace {n} with actual index in both pattern and source
                // Note: Display index starts at 1 for field names (Dependent1, Dependent2)
                String displayIndex = String.valueOf(i + 1);
                String arrayIndex = String.valueOf(i);
                
                String expandedFieldPrefix = fieldPattern
                    .replace("{n}", displayIndex)
                    .replace("*", "");  // Remove wildcard
                
                String expandedSourcePrefix = source.replace("{n}", arrayIndex);
                
                // Expand each field
                for (Map.Entry<String, String> field : fields.entrySet()) {
                    String fieldSuffix = field.getKey();
                    String fieldPath = field.getValue();
                    
                    // Build final field name and path
                    String finalFieldName = expandedFieldPrefix + fieldSuffix;
                    
                    // If field path starts with "static:", don't prepend source path
                    String finalPath;
                    if (fieldPath != null && fieldPath.startsWith("static:")) {
                        finalPath = fieldPath;  // Keep static value as-is
                    } else {
                        finalPath = expandedSourcePrefix + "." + fieldPath;
                    }
                    
                    expanded.put(finalFieldName, finalPath);
                }
            }
        }
        
        System.out.println("Expanded " + patterns.size() + " patterns into " + expanded.size() + " field mappings");
        return expanded;
    }
    
    /**
     * Resolve value from payload using path notation with enhanced filter support
     * 
     * Examples:
     *   "memberName" → payload.get("memberName")
     *   "member.name" → payload.get("member").get("name")
     *   "members[0].name" → payload.get("members").get(0).get("name")
     *   "applicants[relationship=PRIMARY].firstName" → filter array by field value
     *   "applicants[relationship=DEPENDENT][0].name" → filter then index
     *   "coverages[applicantId=A001][productType=MEDICAL].carrier" → multiple filters
     *   "static:Enrollment Form" → returns literal string "Enrollment Form"
     *   "static:v2.0" → returns literal string "v2.0"
     *   
     * Performance: Uses thread-local caching to avoid repeated path resolutions
     */
    Object resolveValue(Map<String, Object> payload, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Handle static/literal values with "static:" prefix
        if (path.startsWith("static:")) {
            return path.substring(7); // Return everything after "static:"
        }
        
        // Check cache first
        Map<String, Object> cache = pathCache.get();
        String cacheKey = path; // In production, consider including payload hashCode for safety
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        // Resolve the path
        Object result = resolveValueInternal(payload, path);
        
        // Cache the result
        cache.put(cacheKey, result);
        
        return result;
    }
    
    /**
     * Internal path resolution without caching
     */
    private Object resolveValueInternal(Map<String, Object> payload, String path) {
        Object current = payload;
        // Split path by dots, but preserve dots inside brackets
        String[] parts = splitPathRespectingBrackets(path);
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            // Handle array notation with filters or index
            if (part.contains("[")) {
                String arrayName = part.substring(0, part.indexOf('['));
                
                // Get the array/list from current object
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(arrayName);
                }
                
                if (current == null) {
                    return null;
                }
                
                if (!(current instanceof java.util.List)) {
                    System.err.println("Warning: Expected list for path part '" + part + "' but got: " + current.getClass());
                    return null;
                }
                
                java.util.List<?> list = (java.util.List<?>) current;
                
                // Extract all filters from the path part: [filter1][filter2]...
                java.util.List<String> filters = extractFilters(part);
                
                // Determine if we need all matches or just first one
                // If there's only one filter and no subsequent index, we only need first match
                boolean needsAllMatches = filters.size() > 1 || hasNumericIndexInFilters(filters);
                
                // Apply each filter in sequence
                for (int i = 0; i < filters.size(); i++) {
                    String filter = filters.get(i);
                    
                    if (isNumericIndex(filter)) {
                        // Numeric index: [0], [1], etc.
                        int index = Integer.parseInt(filter);
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                            list = null; // No longer a list after indexing
                            break;
                        } else {
                            System.err.println("Warning: Index " + index + " out of bounds for list of size " + list.size());
                            return null;
                        }
                    } else {
                        // Filter expression: [field=value]
                        String[] filterParts = filter.split("=", 2);
                        if (filterParts.length != 2) {
                            System.err.println("Warning: Invalid filter syntax: " + filter);
                            return null;
                        }
                        
                        String fieldName = filterParts[0].trim();
                        String fieldValue = filterParts[1].trim();
                        
                        // Build cache key for this filter operation
                        // Format: "listHashCode:arrayName[fieldName=fieldValue]"
                        String filterCacheKey = System.identityHashCode(list) + ":" + arrayName + "[" + fieldName + "=" + fieldValue + "]";
                        
                        // Check filter cache first
                        Map<String, Object> fCache = filterCache.get();
                        if (fCache.containsKey(filterCacheKey)) {
                            list = (java.util.List<?>) fCache.get(filterCacheKey);
                        } else {
                            // Check if next filter is numeric index - if so, we need all matches
                            boolean stopAfterFirst = !needsAllMatches && (i == filters.size() - 1);
                            
                            // Filter the list (with early termination if possible)
                            list = filterList(list, fieldName, fieldValue, stopAfterFirst);
                            
                            // Cache the filtered result
                            fCache.put(filterCacheKey, list);
                        }
                        
                        if (list.isEmpty()) {
                            System.err.println("Warning: No items match filter [" + filter + "]");
                            return null;
                        }
                        
                        current = list;
                    }
                }
                
                // If we still have a list after all filters (no index was used), take first element
                if (current instanceof java.util.List) {
                    java.util.List<?> resultList = (java.util.List<?>) current;
                    if (!resultList.isEmpty()) {
                        current = resultList.get(0);
                    } else {
                        return null;
                    }
                }
                
            } else {
                // Simple property access
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }
        }
        
        return current;
    }
    
    /**
     * Extract all filters from a path part like "members[relationship=PRIMARY][0]"
     * Returns: ["relationship=PRIMARY", "0"]
     */
    /**
     * Split path by dots while preserving dots inside brackets.
     * Example: "applicants[demographic.relationshipType=APPLICANT].firstName"
     * Returns: ["applicants[demographic.relationshipType=APPLICANT]", "firstName"]
     */
    private String[] splitPathRespectingBrackets(String path) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            
            if (ch == '[') {
                bracketDepth++;
                current.append(ch);
            } else if (ch == ']') {
                bracketDepth--;
                current.append(ch);
            } else if (ch == '.' && bracketDepth == 0) {
                // Dot outside brackets - split here
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(ch);
            }
        }
        
        // Add the last part
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Extract all filter expressions from a path part.
     * Example: "applicants[filter1][filter2]" returns ["filter1", "filter2"]
     */
    private java.util.List<String> extractFilters(String part) {
        java.util.List<String> filters = new java.util.ArrayList<>();
        int startIndex = part.indexOf('[');
        
        while (startIndex != -1) {
            int endIndex = part.indexOf(']', startIndex);
            if (endIndex == -1) break;
            
            String filter = part.substring(startIndex + 1, endIndex);
            filters.add(filter);
            
            startIndex = part.indexOf('[', endIndex);
        }
        
        return filters;
    }
    
    /**
     * Check if a filter is a numeric index
     */
    private boolean isNumericIndex(String filter) {
        try {
            Integer.parseInt(filter);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if any filter in the list is a numeric index
     * Used to determine if we need all matches or can stop early
     */
    private boolean hasNumericIndexInFilters(java.util.List<String> filters) {
        for (String filter : filters) {
            if (isNumericIndex(filter)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Filter a list by matching a field value with early termination optimization
     * 
     * @param list List of objects (typically Map objects)
     * @param fieldName Field name to match (supports nested: "demographic.relationshipType")
     * @param fieldValue Expected value (string comparison)
     * @param stopAfterFirst If true, stops after finding first match (optimization for single-result scenarios)
     * @return Filtered list containing only matching items
     */
    private java.util.List<?> filterList(java.util.List<?> list, String fieldName, String fieldValue, boolean stopAfterFirst) {
        java.util.List<Object> filtered = new java.util.ArrayList<>();
        
        for (Object item : list) {
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                
                // Support nested field access in filters (e.g., demographic.relationshipType)
                Object actualValue = getNestedFieldValue(map, fieldName);
                
                if (actualValue != null && actualValue.toString().equals(fieldValue)) {
                    filtered.add(item);
                    
                    // Early termination optimization
                    if (stopAfterFirst) {
                        break;
                    }
                }
            }
        }
        
        return filtered;
    }
    
    /**
     * Extract nested field value from a map using dot notation
     * Optimized helper to avoid code duplication
     * 
     * @param map Source map
     * @param fieldPath Field path (e.g., "demographic.relationshipType")
     * @return Field value or null if not found
     */
    private Object getNestedFieldValue(Map<?, ?> map, String fieldPath) {
        if (!fieldPath.contains(".")) {
            // Simple field access - fast path
            return map.get(fieldPath);
        }
        
        // Navigate nested path
        String[] parts = fieldPath.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    break;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Convert value to string for PDF form field
     */
    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }
        
        // Handle common types
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "Yes" : "No";
        } else if (value instanceof java.util.Date) {
            return new java.text.SimpleDateFormat("MM/dd/yyyy").format((java.util.Date) value);
        } else if (value instanceof java.time.LocalDate) {
            return ((java.time.LocalDate) value).format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } else {
            return value.toString();
        }
    }
    
    /**
     * Get list of all field names in an AcroForm PDF (for debugging/discovery)
     */
    public java.util.List<String> getFieldNames(String templatePath) throws IOException {
        java.util.List<String> fieldNames = new java.util.ArrayList<>();
        
        try (PDDocument document = loadTemplate(templatePath)) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            if (acroForm != null) {
                for (PDField field : acroForm.getFields()) {
                    fieldNames.add(field.getFullyQualifiedName());
                }
            }
        }
        
        return fieldNames;
    }
    
    /**
     * Evict specific AcroForm template from cache (useful for hot-reload)
     */
    @CacheEvict(value = "acroformTemplates", key = "#templatePath")
    public void evictTemplate(String templatePath) {
        System.out.println("Evicted AcroForm template from cache: " + templatePath);
    }
    
    /**
     * Clear entire AcroForm template cache
     */
    @CacheEvict(value = "acroformTemplates", allEntries = true)
    public void clearTemplateCache() {
        System.out.println("Cleared all AcroForm templates from cache");
    }
}
