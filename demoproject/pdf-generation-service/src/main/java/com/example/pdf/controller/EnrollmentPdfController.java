package com.example.pdf.controller;

import com.example.service.ConfigSelectionService;
import com.example.service.FlexiblePdfMergeService;
import com.example.service.EnrollmentPdfService;
import com.example.service.PdfMergeConfigService;
import com.example.service.PdfMergeConfig;
import com.example.service.EnrollmentSubmission;
import com.example.preprocessing.service.ConfigurablePayloadPreProcessor;
import com.example.util.PayloadPathExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enrollment")
public class EnrollmentPdfController {

    @Autowired
    private FlexiblePdfMergeService pdfMergeService;
    
    @Autowired
    private EnrollmentPdfService enrollmentPdfService;
    
    @Autowired
    private PdfMergeConfigService configService;
    
    @Autowired
    private ConfigSelectionService configSelectionService;
    
    @Autowired
    private ConfigurablePayloadPreProcessor preprocessor;
    
    @Value("${preprocessing.rules.default:preprocessing/standard-enrollment-rules.yml}")
    private String defaultPreprocessingRules;

    /**
     * Generate enrollment PDF with automatic config selection
     * POST /api/enrollment/generate
     * 
     * Now supports complex applicant structures with automatic pre-processing:
     * - Separates PRIMARY, SPOUSE, DEPENDENTs
     * - Handles BILLING vs MAILING addresses
     * - Separates MEDICAL, DENTAL, VISION products
     * - Manages overflow for 4+ dependents
     * - Automatically collects products from all applicants/members if not provided
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateEnrollmentPdf(@RequestBody EnrollmentPdfRequest request) {
        try {
            // Preliminary config selection to get product collection paths
            String preliminaryConfigName = configSelectionService.selectConfigByConvention(
                request.getEnrollment() != null ? request.getEnrollment() : new EnrollmentSubmission()
            );
            
            // Load config to get product collection paths
            PdfMergeConfig config = configService.loadConfig(preliminaryConfigName);
            
            // Auto-collect products from payload if not explicitly provided
            EnrollmentSubmission enrollment = enrichEnrollmentWithProducts(
                request.getEnrollment(), 
                request.getPayload(),
                config
            );
            
            // Re-select config with enriched enrollment data
            String configName = configSelectionService.selectConfigByConvention(enrollment);
            
            // Reload config if it changed
            if (!configName.equals(preliminaryConfigName)) {
                config = configService.loadConfig(configName);
            }
            
            System.out.println("Selected config: " + configName);
            System.out.println("Products: " + enrollment.getProducts());
            System.out.println("Market: " + enrollment.getMarketCategory());
            System.out.println("State: " + enrollment.getState());
            
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
            // Preliminary config selection
            String preliminaryConfigName = configSelectionService.selectConfigByRules(
                request.getEnrollment() != null ? request.getEnrollment() : new EnrollmentSubmission()
            );
            
            // Load config to get product collection paths
            PdfMergeConfig config = configService.loadConfig(preliminaryConfigName);
            
            // Auto-collect products from payload if not explicitly provided
            EnrollmentSubmission enrollment = enrichEnrollmentWithProducts(
                request.getEnrollment(), 
                request.getPayload(),
                config
            );
            
            // Strategy 4: Use business rules
            String configName = configSelectionService.selectConfigByRules(enrollment);
            
            // Reload config if it changed
            if (!configName.equals(preliminaryConfigName)) {
                config = configService.loadConfig(configName);
            }
            
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
            enrollment.getMarketCategory(),
            enrollment.getState());
    }
    
    /**
     * Enriches enrollment submission by auto-collecting products from payload if not explicitly provided.
     * 
     * This method examines the payload structure to extract all unique products selected by applicants/members.
     * It supports multiple payload structures:
     * - members[] with products[] arrays
     * - application.applicants[] with products
     * - enrollment.members with product information
     * 
     * @param enrollment The enrollment submission (may have empty or partial products list)
     * @param payload The payload containing member/applicant data
     * @return Enriched enrollment submission with complete products list
     */
    private EnrollmentSubmission enrichEnrollmentWithProducts(
            EnrollmentSubmission enrollment, 
            Map<String, Object> payload,
            PdfMergeConfig config) {
        
        // If products already provided and not empty, use as-is
        if (enrollment.getProducts() != null && !enrollment.getProducts().isEmpty()) {
            System.out.println("Using explicitly provided products: " + enrollment.getProducts());
            return enrollment;
        }
        
        // Auto-collect products from payload using config-driven paths
        List<String> collectedProducts = collectProductsFromPayload(payload, config);
        
        if (!collectedProducts.isEmpty()) {
            System.out.println("Auto-collected products from payload using config paths: " + collectedProducts);
            enrollment.setProducts(collectedProducts);
        } else if (config.getDefaultProducts() != null && !config.getDefaultProducts().isEmpty()) {
            System.out.println("Using default products from config: " + config.getDefaultProducts());
            enrollment.setProducts(config.getDefaultProducts());
        } else {
            System.out.println("Warning: No products found in payload and no defaults configured. Using empty list.");
            enrollment.setProducts(new ArrayList<>());
        }
        
        return enrollment;
    }
    
    /**
     * Collects all unique product types from the payload using configurable paths.
     * Falls back to default hardcoded paths if config doesn't specify custom paths.
     * 
     * @param payload The request payload
     * @param config The PDF merge config (may contain productCollectionPaths)
     * @return Sorted list of unique product types (e.g., ["dental", "medical", "vision"])
     */
    private List<String> collectProductsFromPayload(Map<String, Object> payload, PdfMergeConfig config) {
        List<String> paths;
        
        // Use paths from config if available, otherwise use defaults
        if (config.getProductCollectionPaths() != null && !config.getProductCollectionPaths().isEmpty()) {
            paths = config.getProductCollectionPaths();
            System.out.println("Using custom product collection paths from config: " + paths);
        } else {
            paths = getDefaultProductCollectionPaths();
            System.out.println("Using default product collection paths");
        }
        
        // Use PayloadPathExtractor to extract products from configured paths
        return PayloadPathExtractor.extractProductTypes(payload, paths);
    }
    
    /**
     * Returns default product collection paths for backward compatibility.
     * These paths cover common payload structures.
     */
    private List<String> getDefaultProductCollectionPaths() {
        return Arrays.asList(
            "members[].products[].type",
            "application.applicants[].products[].productType",
            "application.proposedProducts[].type",
            "enrollment.members[].products[].type",
            "applicants[].coverages[].productType",
            "members[].products[].productType",
            "members[].products[].name"
        );
    }
    
    /**
     * Prepares payload with configuration-driven pre-processing.
     * Uses YAML rules to flatten complex structures without code changes.
     * 
     * @param originalPayload The original payload from the request
     * @return Processed payload with flattened structure (if needed) + original for FreeMarker
     */
    private Map<String, Object> preparePayload(Map<String, Object> originalPayload) {
        // Check if payload needs preprocessing (has nested arrays)
        boolean hasComplexStructure = hasNestedArrays(originalPayload);
        
        if (!hasComplexStructure) {
            // Simple payload, return as-is
            return originalPayload;
        }
        
        System.out.println("Detected complex structure - applying configuration-driven pre-processing");
        
        // Get preprocessing rules (can be overridden per client via config)
        String rulesConfig = determinePreprocessingRules(originalPayload);
        
        // Pre-process using configuration rules
        Map<String, Object> flattenedPayload = preprocessor.preProcess(originalPayload, rulesConfig);
        
        // Create combined payload:
        // - Flattened structure for AcroForm field mapping (primary, spouse, dependent1-3, etc.)
        // - Original structure for FreeMarker templates (addendum, dynamic sections)
        Map<String, Object> fullPayload = new HashMap<>();
        fullPayload.putAll(flattenedPayload);
        fullPayload.putAll(originalPayload); // Keep original nested structure
        
        System.out.println("Pre-processing complete using rules: " + rulesConfig +
            " | hasPrimary=" + fullPayload.containsKey("primary") +
            ", hasSpouse=" + fullPayload.getOrDefault("hasSpouse", false) +
            ", dependents=" + fullPayload.getOrDefault("dependentCount", 0));
        
        return fullPayload;
    }
    
    /**
     * Determines which preprocessing rules to use based on payload structure.
     * Can be extended to use client ID, tenant context, etc.
     */
    private String determinePreprocessingRules(Map<String, Object> payload) {
        // Strategy 1: Check for client identifier in payload
        if (payload.containsKey("clientId")) {
            String clientId = payload.get("clientId").toString();
            return "preprocessing/" + clientId + "-rules.yml";
        }
        
        // Strategy 2: Detect structure pattern
        if (payload.containsKey("enrollment") && 
            payload.get("enrollment") instanceof Map) {
            Map<String, Object> enrollment = (Map<String, Object>) payload.get("enrollment");
            if (enrollment.containsKey("members")) {
                return "preprocessing/client-b-rules.yml"; // Different structure
            }
        }
        
        // Strategy 3: Use default
        return defaultPreprocessingRules;
    }
    
    /**
     * Checks if payload has nested arrays that need preprocessing.
     */
    private boolean hasNestedArrays(Map<String, Object> payload) {
        // Check common patterns
        if (payload.containsKey("application")) {
            Map<String, Object> app = (Map<String, Object>) payload.get("application");
            if (app.containsKey("applicants") && app.get("applicants") instanceof java.util.List) {
                return true;
            }
        }
        
        if (payload.containsKey("enrollment")) {
            Map<String, Object> enrollment = (Map<String, Object>) payload.get("enrollment");
            if (enrollment.containsKey("members") && enrollment.get("members") instanceof java.util.List) {
                return true;
            }
        }
        
        return false;
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
