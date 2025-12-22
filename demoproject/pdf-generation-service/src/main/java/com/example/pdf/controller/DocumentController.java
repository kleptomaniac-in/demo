package com.example.pdf.controller;

import com.example.monitoring.PerformanceMetrics;
import com.example.monitoring.PerformanceMonitoringContext;
import com.example.service.FlexiblePdfMergeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private FlexiblePdfMergeService flexiblePdfMergeService;
    
    @Autowired
    private PerformanceMonitoringContext performanceMonitor;

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> mergePdf(@RequestBody MergePdfRequest request) {
        try {
            log.info("Received document generation request with config: {}", request.getConfigName());
            
            byte[] pdfBytes = flexiblePdfMergeService.generateMergedPdf(
                request.getConfigName(),
                request.getPayload()
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                request.getOutputFileName() != null ? request.getOutputFileName() : "merged-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            log.info("Successfully generated merged PDF with {} bytes", pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error generating merged PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Generate PDF and return performance metrics along with the PDF
     */
    @PostMapping(value = "/generate-with-metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> mergePdfWithMetrics(@RequestBody MergePdfRequest request) {
        try {
            log.info("Received document generation request with metrics for config: {}", request.getConfigName());
            
            byte[] pdfBytes = flexiblePdfMergeService.generateMergedPdf(
                request.getConfigName(),
                request.getPayload()
            );
            
            // Get performance metrics from the just-completed operation
            PerformanceMetrics metrics = performanceMonitor.getCurrentMetrics();
            
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("success", true);
            response.put("pdfSizeBytes", pdfBytes.length);
            response.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(pdfBytes));
            
            if (metrics != null) {
                response.put("performanceMetrics", metrics.toMap());
            }
            
            log.info("Successfully generated merged PDF with metrics");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating merged PDF with metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "FlexiblePdfMergeService"
        ));
    }
}

class MergePdfRequest {
    private String configName;
    private Map<String, Object> payload;
    private String outputFileName;

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
}
