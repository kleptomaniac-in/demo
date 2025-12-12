package com.pdfgen.controller;

import com.pdfgen.service.EnrollmentApplicationPreProcessor;
import com.pdfgen.service.FlexiblePdfMergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API endpoint for generating enrollment PDFs with complex applicant structures.
 * Handles PRIMARY, SPOUSE, and multiple DEPENDENTs with overflow to addendum.
 */
@RestController
@RequestMapping("/api/enrollment-complex")
public class ComplexEnrollmentPdfController {
    
    @Autowired
    private FlexiblePdfMergeService pdfMergeService;
    
    @Autowired
    private EnrollmentApplicationPreProcessor preprocessor;
    
    /**
     * Generate enrollment PDF with automatic pre-processing.
     * Handles complex nested structure and role-based filtering.
     * 
     * POST /api/enrollment-complex/generate
     * 
     * Request body: Full application structure with applicants array
     * Response: PDF with main form + addendum (if 4+ dependents)
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateEnrollmentPdf(@RequestBody Map<String, Object> request) {
        
        // Extract application data
        Map<String, Object> applicationData = (Map<String, Object>) request.get("application");
        
        // Pre-process: Flatten nested arrays for simplified field mapping
        Map<String, Object> flattenedPayload = preprocessor.prepareForPdfMapping(
            Map.of("application", applicationData)
        );
        
        // Add back original structure for FreeMarker addendum template
        Map<String, Object> fullPayload = new HashMap<>();
        fullPayload.putAll(flattenedPayload);
        fullPayload.put("application", applicationData);
        
        // Determine config file (use preprocessed version)
        String configName = request.getOrDefault("configName", 
            "examples/preprocessed-enrollment-mapping.yml").toString();
        
        // Generate PDF
        byte[] pdfBytes = pdfMergeService.generatePdf(configName, fullPayload);
        
        // Return PDF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "enrollment-application.pdf");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }
    
    /**
     * Preview the flattened payload structure after pre-processing.
     * Useful for debugging field mappings.
     * 
     * POST /api/enrollment-complex/preview-flattened
     */
    @PostMapping("/preview-flattened")
    public ResponseEntity<Map<String, Object>> previewFlattenedPayload(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> applicationData = (Map<String, Object>) request.get("application");
        
        Map<String, Object> flattenedPayload = preprocessor.prepareForPdfMapping(
            Map.of("application", applicationData)
        );
        
        return ResponseEntity.ok(flattenedPayload);
    }
    
    /**
     * Get applicant summary for UI display.
     * Shows primary, spouse, and dependent count.
     */
    @PostMapping("/applicant-summary")
    public ResponseEntity<Map<String, Object>> getApplicantSummary(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> applicationData = (Map<String, Object>) request.get("application");
        Map<String, Object> flattened = preprocessor.prepareForPdfMapping(
            Map.of("application", applicationData)
        );
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("hasPrimary", flattened.containsKey("primary"));
        summary.put("hasSpouse", flattened.get("hasSpouse"));
        summary.put("dependentCount", flattened.get("dependentCount"));
        summary.put("primaryDependentCount", flattened.get("primaryDependentCount"));
        summary.put("additionalDependentCount", flattened.get("additionalDependentCount"));
        summary.put("needsAddendum", (Integer) flattened.getOrDefault("additionalDependentCount", 0) > 0);
        
        if (flattened.containsKey("primary")) {
            Map<String, Object> primary = (Map<String, Object>) flattened.get("primary");
            Map<String, Object> demographic = (Map<String, Object>) primary.get("demographic");
            summary.put("primaryName", demographic.get("firstName") + " " + demographic.get("lastName"));
        }
        
        return ResponseEntity.ok(summary);
    }
}
