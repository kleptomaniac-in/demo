package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Service for filling AcroForm PDF templates with data mapping
 */
@Service
public class AcroFormFillService {
    
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
                
                // Resolve value from payload using path notation
                Object value = resolveValue(payload, payloadPath);
                
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
     * Load AcroForm template from file system
     */
    private PDDocument loadTemplate(String templatePath) throws IOException {
        String fullPath = "../config-repo/acroforms/" + templatePath;
        
        if (!Files.exists(Paths.get(fullPath))) {
            fullPath = "acroforms/" + templatePath;
        }
        
        return PDDocument.load(Files.newInputStream(Paths.get(fullPath)));
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
     * Resolve value from payload using path notation
     * Examples:
     *   "memberName" → payload.get("memberName")
     *   "member.name" → payload.get("member").get("name")
     *   "members[0].name" → payload.get("members").get(0).get("name")
     */
    private Object resolveValue(Map<String, Object> payload, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        Object current = payload;
        String[] parts = path.split("\\.");
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            // Handle array notation: members[0]
            if (part.contains("[")) {
                String arrayName = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(arrayName);
                }
                
                if (current instanceof java.util.List) {
                    current = ((java.util.List<?>) current).get(index);
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
}
