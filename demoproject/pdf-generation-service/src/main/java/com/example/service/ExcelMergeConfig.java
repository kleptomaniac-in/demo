package com.example.service;

import java.util.List;
import java.util.Map;

/**
 * Configuration model for Excel template generation.
 * 
 * Supports:
 * - Cell mappings (simple key-value)
 * - Table mappings (repeating rows)
 * - Preprocessing rules
 * - Composition (base + components)
 */
public class ExcelMergeConfig {
    
    // Composition support (similar to PDF)
    private String base;
    private List<String> components;
    
    // Template path
    private String templatePath;
    
    // Preprocessing
    private String preprocessingRules;
    
    // Cell mappings: cell reference/named range → payload path
    private Map<String, String> cellMappings;
    
    // Table mappings for repeating data
    private List<TableMappingConfig> tableMappings;
    
    // Metadata
    private String description;
    private String version;
    
    // Getters and setters for composition
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    
    public List<String> getComponents() { return components; }
    public void setComponents(List<String> components) { this.components = components; }
    
    // Getters and setters
    public String getTemplatePath() { return templatePath; }
    public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
    
    public String getPreprocessingRules() { return preprocessingRules; }
    public void setPreprocessingRules(String preprocessingRules) { this.preprocessingRules = preprocessingRules; }
    
    public Map<String, String> getCellMappings() { return cellMappings; }
    public void setCellMappings(Map<String, String> cellMappings) { this.cellMappings = cellMappings; }
    
    public List<TableMappingConfig> getTableMappings() { return tableMappings; }
    public void setTableMappings(List<TableMappingConfig> tableMappings) { this.tableMappings = tableMappings; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
