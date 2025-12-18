package com.example.pdf.controller;

import com.pdfgen.service.ConfigurablePayloadPreProcessor;
import com.example.service.ExcelTemplateService;
import com.example.service.ExcelMergeConfigService;
import com.example.service.ExcelMergeConfig;
import com.example.service.ExcelToPdfConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Excel template generation
 */
@RestController
@RequestMapping("/api/excel")
public class ExcelGenerationController {
    
    @Autowired
    private ExcelTemplateService excelTemplateService;
    
    @Autowired
    private ExcelMergeConfigService excelConfigService;
    
    @Autowired
    private ConfigurablePayloadPreProcessor payloadPreProcessor;
    
    @Autowired
    private ExcelToPdfConverter excelToPdfConverter;
    
    /**
     * Generate Excel using YAML configuration (recommended approach)
     * 
     * POST /api/excel/generate-from-config
     * {
     *   "configName": "enrollment-summary-excel.yml",
     *   "payload": {...}
     * }
     */
    @PostMapping("/generate-from-config")
    public ResponseEntity<byte[]> generateFromConfig(@RequestBody ExcelConfigRequest request) {
        try {
            // Load configuration from YAML
            ExcelMergeConfig config = excelConfigService.loadConfig(request.getConfigName());
            
            // Apply preprocessing if specified
            Map<String, Object> processedPayload = request.getPayload();
            if (config.getPreprocessingRules() != null) {
                processedPayload = payloadPreProcessor.preProcess(
                    request.getPayload(),
                    "preprocessing/" + config.getPreprocessingRules()
                );
            }
            
            // Generate Excel based on config
            byte[] excelBytes;
            if (config.getTableMappings() != null && !config.getTableMappings().isEmpty()) {
                // Has table mappings - convert to service format
                List<ExcelTemplateService.TableMapping> tableMappings = 
                    convertTableMappings(config.getTableMappings());
                excelBytes = excelTemplateService.fillExcelWithTables(
                    config.getTemplatePath(),
                    tableMappings,
                    processedPayload
                );
            } else {
                // Simple cell mappings only
                excelBytes = excelTemplateService.fillExcelTemplate(
                    config.getTemplatePath(),
                    config.getCellMappings(),
                    processedPayload
                );
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "generated.xlsx");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate Excel from template using cell mappings (simple key-value approach)
     * 
     * POST /api/excel/generate
     * {
     *   "templatePath": "enrollment-summary.xlsx",
     *   "cellMappings": {
     *     "A1": "applicationId",
     *     "B2": "primary.firstName",
     *     "C2": "primary.lastName"
     *   },
     *   "payload": {...}
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateExcel(@RequestBody ExcelGenerationRequest request) {
        try {
            byte[] excelBytes = excelTemplateService.fillExcelTemplate(
                request.getTemplatePath(),
                request.getCellMappings(),
                request.getPayload()
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "generated.xlsx");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate Excel with preprocessing (for complex structures)
     * 
     * POST /api/excel/generate-with-preprocessing
     * {
     *   "templatePath": "enrollment-summary.xlsx",
     *   "preprocessingRules": "standard-enrollment-rules.yml",
     *   "cellMappings": {
     *     "A1": "applicationId",
     *     "B2": "primary.firstName",
     *     "D10": "dependent1.firstName",
     *     "D11": "dependent2.firstName"
     *   },
     *   "payload": {...}
     * }
     */
    @PostMapping("/generate-with-preprocessing")
    public ResponseEntity<byte[]> generateExcelWithPreprocessing(@RequestBody ExcelPreprocessedRequest request) {
        try {
            // Apply preprocessing
            Map<String, Object> preprocessedPayload = payloadPreProcessor.preProcess(
                request.getPayload(),
                "preprocessing/" + request.getPreprocessingRules()
            );
            
            // Generate Excel
            byte[] excelBytes = excelTemplateService.fillExcelTemplate(
                request.getTemplatePath(),
                request.getCellMappings(),
                preprocessedPayload
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "generated.xlsx");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate Excel with table/list data (for repeating rows)
     * 
     * POST /api/excel/generate-with-tables
     * {
     *   "templatePath": "dependent-list.xlsx",
     *   "tableMappings": [
     *     {
     *       "sheetName": "Dependents",
     *       "startRow": 2,
     *       "sourcePath": "allDependents",
     *       "columnMappings": {
     *         "0": "firstName",
     *         "1": "lastName",
     *         "2": "dateOfBirth",
     *         "3": "ssn"
     *       }
     *     }
     *   ],
     *   "payload": {...}
     * }
     */
    @PostMapping("/generate-with-tables")
    public ResponseEntity<byte[]> generateExcelWithTables(@RequestBody ExcelTableRequest request) {
        try {
            byte[] excelBytes = excelTemplateService.fillExcelWithTables(
                request.getTemplatePath(),
                request.getTableMappings(),
                request.getPayload()
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "generated.xlsx");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Combined: Preprocessing + cell mappings + table data
     * 
     * POST /api/excel/generate-complete
     */
    @PostMapping("/generate-complete")
    public ResponseEntity<byte[]> generateCompleteExcel(@RequestBody ExcelCompleteRequest request) {
        try {
            // Apply preprocessing if specified
            Map<String, Object> processedPayload = request.getPayload();
            if (request.getPreprocessingRules() != null) {
                processedPayload = payloadPreProcessor.preProcess(
                    request.getPayload(),
                    "preprocessing/" + request.getPreprocessingRules()
                );
            }
            
            // Generate Excel with tables
            byte[] excelBytes = excelTemplateService.fillExcelWithTables(
                request.getTemplatePath(),
                request.getTableMappings(),
                processedPayload
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "generated.xlsx");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Convert config TableMappingConfig to service TableMapping
     */
    private List<ExcelTemplateService.TableMapping> convertTableMappings(
            List<?> configMappings) {
        List<ExcelTemplateService.TableMapping> result = new java.util.ArrayList<>();
        
        for (Object obj : configMappings) {
            if (obj instanceof com.example.service.TableMappingConfig) {
                com.example.service.TableMappingConfig config = 
                    (com.example.service.TableMappingConfig) obj;
                ExcelTemplateService.TableMapping mapping = new ExcelTemplateService.TableMapping(
                    config.getSheetName(),
                    config.getStartRow(),
                    config.getSourcePath(),
                    config.getColumnMappings()
                );
                result.add(mapping);
            }
        }
        
        return result;
    }
    
    // ===== Request DTOs =====
    
    public static class ExcelConfigRequest {
        private String configName;
        private Map<String, Object> payload;
        
        public String getConfigName() { return configName; }
        public void setConfigName(String configName) { this.configName = configName; }
        
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    }
    
    public static class ExcelGenerationRequest {
        private String templatePath;
        private Map<String, String> cellMappings;
        private Map<String, Object> payload;
        
        // Getters and setters
        public String getTemplatePath() { return templatePath; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
        
        public Map<String, String> getCellMappings() { return cellMappings; }
        public void setCellMappings(Map<String, String> cellMappings) { this.cellMappings = cellMappings; }
        
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    }
    
    public static class ExcelPreprocessedRequest extends ExcelGenerationRequest {
        private String preprocessingRules;
        
        public String getPreprocessingRules() { return preprocessingRules; }
        public void setPreprocessingRules(String preprocessingRules) { this.preprocessingRules = preprocessingRules; }
    }
    
    public static class ExcelTableRequest {
        private String templatePath;
        private List<ExcelTemplateService.TableMapping> tableMappings;
        private Map<String, Object> payload;
        
        public String getTemplatePath() { return templatePath; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
        
        public List<ExcelTemplateService.TableMapping> getTableMappings() { return tableMappings; }
        public void setTableMappings(List<ExcelTemplateService.TableMapping> tableMappings) { this.tableMappings = tableMappings; }
        
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    }
    
    public static class ExcelCompleteRequest extends ExcelTableRequest {
        private String preprocessingRules;
        
        public String getPreprocessingRules() { return preprocessingRules; }
        public void setPreprocessingRules(String preprocessingRules) { this.preprocessingRules = preprocessingRules; }
    }
    
    // ========== EXCEL TO PDF CONVERSION ENDPOINTS ==========
    
    /**
     * Generate Excel and convert to PDF
     * POST /api/excel/generate-as-pdf
     * 
     * Same as /generate but returns PDF instead of Excel
     */
    @PostMapping("/generate-as-pdf")
    public ResponseEntity<byte[]> generateExcelAsPdf(@RequestBody ExcelGenerationRequest request) {
        try {
            // First generate Excel
            byte[] excelData = excelTemplateService.fillExcelTemplate(
                request.getTemplatePath(),
                request.getCellMappings(),
                request.getPayload()
            );
            
            // Convert to PDF
            byte[] pdfData = excelToPdfConverter.convertToPdf(excelData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "enrollment.pdf");
            
            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
            
        } catch (UnsupportedOperationException e) {
            // Conversion not available - return helpful error
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(("Excel-to-PDF conversion not available. " + e.getMessage()).getBytes());
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Generate Excel from config and convert to PDF
     * POST /api/excel/generate-from-config-as-pdf
     */
    @PostMapping("/generate-from-config-as-pdf")
    public ResponseEntity<byte[]> generateFromConfigAsPdf(@RequestBody ExcelConfigRequest request) {
        try {
            ExcelMergeConfig config = excelConfigService.loadConfig(request.getConfigName());
            
            byte[] excelData = excelTemplateService.fillExcelTemplate(
                config.getTemplatePath(),
                config.getCellMappings(),
                request.getPayload()
            );
            
            byte[] pdfData = excelToPdfConverter.convertToPdf(excelData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "enrollment.pdf");
            
            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
            
        } catch (UnsupportedOperationException e) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(("Conversion not available. " + e.getMessage()).getBytes());
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Check Excel-to-PDF conversion capabilities
     * GET /api/excel/conversion-info
     */
    @GetMapping("/conversion-info")
    public ResponseEntity<ExcelToPdfConverter.ConversionInfo> getConversionInfo() {
        return ResponseEntity.ok(excelToPdfConverter.getConversionInfo());
    }
}
