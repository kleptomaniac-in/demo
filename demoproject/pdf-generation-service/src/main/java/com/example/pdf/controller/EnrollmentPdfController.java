package com.example.pdf.controller;

import com.example.service.ConfigSelectionService;
import com.example.service.FlexiblePdfMergeService;
import com.example.service.EnrollmentSubmission;
import com.pdfgen.service.EnrollmentApplicationPreProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollment")
public class EnrollmentPdfController {

    @Autowired
    private FlexiblePdfMergeService pdfMergeService;
    
    @Autowired
    private ConfigSelectionService configSelectionService;
    
    @Autowired
    private EnrollmentApplicationPreProcessor preprocessor;

    /**
     * Generate enrollment PDF with automatic config selection
     * POST /api/enrollment/generate
     * 
     * Now supports complex applicant structures with automatic pre-processing:
     * - Separates PRIMARY, SPOUSE, DEPENDENTs
     * - Handles BILLING vs MAILING addresses
     * - Separates MEDICAL, DENTAL, VISION products
     * - Manages overflow for 4+ dependents
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateEnrollmentPdf(@RequestBody EnrollmentPdfRequest request) {
        try {
            // Strategy 1: Use convention-based selection
            String configName = configSelectionService.selectConfigByConvention(request.getEnrollment());
            
            System.out.println("Selected config: " + configName);
            System.out.println("Products: " + request.getEnrollment().getProducts());
            System.out.println("Market: " + request.getEnrollment().getMarketCategory());
            System.out.println("State: " + request.getEnrollment().getState());
            
            // Prepare payload with optional pre-processing for complex structures
            Map<String, Object> processedPayload = preparePayload(request.getPayload());
            
            // Generate PDF
            byte[] pdfBytes = pdfMergeService.generateMergedPdf(configName, processedPayload);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                request.getOutputFileName() != null ? request.getOutputFileName() : "enrollment.pdf");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            System.err.println("Error generating enrollment PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generate enrollment PDF with rule-based config selection
     * POST /api/enrollment/generate-with-rules
     */
    @PostMapping("/generate-with-rules")
    public ResponseEntity<byte[]> generateWithRules(@RequestBody EnrollmentPdfRequest request) {
        try {
            // Strategy 4: Use business rules
            String configName = configSelectionService.selectConfigByRules(request.getEnrollment());
            
            System.out.println("Rule-based config selection: " + configName);
            
            // Prepare payload with optional pre-processing
            Map<String, Object> processedPayload = preparePayload(request.getPayload());
            
            byte[] pdfBytes = pdfMergeService.generateMergedPdf(configName, processedPayload);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                request.getOutputFileName() != null ? request.getOutputFileName() : "enrollment.pdf");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            System.err.println("Error generating enrollment PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Preview which config would be selected (without generating PDF)
     * POST /api/enrollment/preview-config
     */
    @PostMapping("/preview-config")
    public ResponseEntity<ConfigPreviewResponse> previewConfig(@RequestBody EnrollmentSubmission enrollment) {
        String conventionBased = configSelectionService.selectConfigByConvention(enrollment);
        String ruleBased = configSelectionService.selectConfigByRules(enrollment);
        Map<String, Object> dynamicComposition = configSelectionService.buildDynamicComposition(enrollment);
        
        ConfigPreviewResponse response = new ConfigPreviewResponse();
        response.setConventionBasedConfig(conventionBased);
        response.setRuleBasedConfig(ruleBased);
        response.setDynamicComposition(dynamicComposition);
        response.setEnrollmentSummary(buildSummary(enrollment));
        
        return ResponseEntity.ok(response);
    }
    
    private String buildSummary(EnrollmentSubmission enrollment) {
        return String.format("Products: %s, Market: %s, State: %s", 
            String.join(", ", enrollment.getProducts()),
    
    /**
     * Prepares payload with automatic pre-processing for complex structures.
     * Detects if payload contains complex applicant arrays and flattens them.
     * 
     * @param originalPayload The original payload from the request
     * @return Processed payload with flattened structure (if needed) + original for FreeMarker
     */
    private Map<String, Object> preparePayload(Map<String, Object> originalPayload) {
        // Check if payload has complex application structure
        boolean hasComplexStructure = originalPayload.containsKey("application") &&
            originalPayload.get("application") instanceof Map;
        
        if (!hasComplexStructure) {
            // Simple payload, return as-is
            return originalPayload;
        }
        
        Map<String, Object> applicationData = (Map<String, Object>) originalPayload.get("application");
        
        // Check if it has applicants array (complex structure indicator)
        boolean hasApplicantsArray = applicationData.containsKey("applicants") &&
            applicationData.get("applicants") instanceof java.util.List;
        
        if (!hasApplicantsArray) {
            // No applicants array, return as-is
            return originalPayload;
        }
        
        System.out.println("Detected complex applicant structure - applying pre-processing");
        
        // Pre-process: Flatten nested arrays for simplified field mapping
        Map<String, Object> flattenedPayload = preprocessor.prepareForPdfMapping(originalPayload);
        
        // Create combined payload:
        // - Flattened structure for AcroForm field mapping (primary, spouse, dependent1-3, etc.)
        // - Original structure for FreeMarker templates (addendum, dynamic sections)
        Map<String, Object> fullPayload = new HashMap<>();
        fullPayload.putAll(flattenedPayload);
        fullPayload.put("application", applicationData);
        
        System.out.println("Pre-processing complete: " +
            "hasPrimary=" + fullPayload.containsKey("primary") +
            ", hasSpouse=" + fullPayload.getOrDefault("hasSpouse", false) +
            ", dependents=" + fullPayload.getOrDefault("dependentCount", 0));
        
        return fullPayload;
    }
            enrollment.getMarketCategory(),
            enrollment.getState());
    }
}

class EnrollmentPdfRequest {
    private EnrollmentSubmission enrollment;
    private Map<String, Object> payload;
    private String outputFileName;
    
    public EnrollmentSubmission getEnrollment() { return enrollment; }
    public void setEnrollment(EnrollmentSubmission enrollment) { this.enrollment = enrollment; }
    
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}

class ConfigPreviewResponse {
    private String conventionBasedConfig;
    private String ruleBasedConfig;
    private Map<String, Object> dynamicComposition;
    private String enrollmentSummary;
    
    public String getConventionBasedConfig() { return conventionBasedConfig; }
    public void setConventionBasedConfig(String config) { this.conventionBasedConfig = config; }
    
    public String getRuleBasedConfig() { return ruleBasedConfig; }
    public void setRuleBasedConfig(String config) { this.ruleBasedConfig = config; }
    
    public Map<String, Object> getDynamicComposition() { return dynamicComposition; }
    public void setDynamicComposition(Map<String, Object> comp) { this.dynamicComposition = comp; }
    
    public String getEnrollmentSummary() { return enrollmentSummary; }
    public void setEnrollmentSummary(String summary) { this.enrollmentSummary = summary; }
}
