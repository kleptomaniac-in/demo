package com.example.service;

import com.example.pdfgeneration.function.FunctionExpressionResolver;
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
import java.util.Map;
import java.util.List;

/**
 * Service for filling AcroForm PDF templates with data mapping.
 * Supports function expressions for field transformations.
 */
@Service
public class AcroFormFillService {
    
    private final FunctionExpressionResolver functionResolver;
    
    public AcroFormFillService(FunctionExpressionResolver functionResolver) {
        this.functionResolver = functionResolver;
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
        // Load the AcroForm template
        try (PDDocument document = loadTemplate(templatePath)) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            if (acroForm == null) {
                throw new IllegalArgumentException("PDF does not contain an AcroForm: " + templatePath);
            }
            
            // Fill each field according to mappings
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
                    // Resolve value from payload using path notation
                    value = resolveValue(payload, payloadPath);
                }
                
                if (value != null) {
                    fillField(acroForm, pdfFieldName, value);
                }
            }
            
            // Flatten the form (optional - makes fields non-editable)
            // acroForm.flatten();
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
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
     */
    private Object resolveValue(Map<String, Object> payload, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Handle static/literal values with "static:" prefix
        if (path.startsWith("static:")) {
            return path.substring(7); // Return everything after "static:"
        }
        
        Object current = payload;
        String[] parts = path.split("\\.");
        
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
                
                // Apply each filter in sequence
                for (String filter : filters) {
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
                        
                        // Filter the list
                        list = filterList(list, fieldName, fieldValue);
                        
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
     * Filter a list by matching a field value
     * 
     * @param list List of objects (typically Map objects)
     * @param fieldName Field name to match
     * @param fieldValue Expected value (string comparison)
     * @return Filtered list containing only matching items
     */
    private java.util.List<?> filterList(java.util.List<?> list, String fieldName, String fieldValue) {
        java.util.List<Object> filtered = new java.util.ArrayList<>();
        
        for (Object item : list) {
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                Object actualValue = map.get(fieldName);
                
                if (actualValue != null && actualValue.toString().equals(fieldValue)) {
                    filtered.add(item);
                }
            }
        }
        
        return filtered;
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
