package com.example.pdf.controller;

import com.example.service.ConfigSelectionService;
import com.example.service.FlexiblePdfMergeService;
import com.example.service.EnrollmentSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/enrollment")
public class EnrollmentPdfController {

    @Autowired
    private FlexiblePdfMergeService pdfMergeService;
    
    @Autowired
    private ConfigSelectionService configSelectionService;

    /**
     * Generate enrollment PDF with automatic config selection
     * POST /api/enrollment/generate
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
            
            // Generate PDF
            byte[] pdfBytes = pdfMergeService.generateMergedPdf(configName, request.getPayload());
            
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
            
            byte[] pdfBytes = pdfMergeService.generateMergedPdf(configName, request.getPayload());
            
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
