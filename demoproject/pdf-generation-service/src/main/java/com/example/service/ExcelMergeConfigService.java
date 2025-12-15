package com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service for loading Excel merge configurations from YAML files.
 * 
 * Supports:
 * - Simple configuration loading
 * - Composition (base + components)
 * - Deep merging of configurations
 */
@Service
public class ExcelMergeConfigService {
    
    @Value("${config.repo.path:../config-repo}")
    private String configRepoPath;
    
    /**
     * Load Excel configuration from YAML file
     * 
     * @param configName Name of config file (e.g., "enrollment-summary-excel.yml")
     * @return Parsed ExcelMergeConfig
     */
    public ExcelMergeConfig loadConfig(String configName) {
        try {
            // Load the main configuration
            Map<String, Object> data = loadYamlFile(configName);
            
            // Check if this is a composition
            if (data.containsKey("composition")) {
                Map<String, Object> composition = (Map<String, Object>) data.get("composition");
                return loadComposedConfig(composition, data);
            }
            
            // Regular config without composition
            return parseExcelMergeConfig(data);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Excel merge config: " + configName, e);
        }
    }
    
    /**
     * Load YAML file from file system
     */
    private Map<String, Object> loadYamlFile(String configName) throws Exception {
        // Build config path
        String configPath = configRepoPath + "/excel/" + configName;
        
        if (!Files.exists(Paths.get(configPath))) {
            // Try without excel/ subdirectory
            configPath = configRepoPath + "/" + configName;
        }
        
        if (!Files.exists(Paths.get(configPath))) {
            // Try current working directory
            configPath = configName;
        }
        
        if (!Files.exists(Paths.get(configPath))) {
            throw new RuntimeException("Config file not found: " + configName);
        }
        
        System.out.println("Loading Excel config from: " + configPath);
        
        try (InputStream inputStream = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            return yaml.load(inputStream);
        }
    }
    
    /**
     * Load composed configuration (base + components)
     */
    private ExcelMergeConfig loadComposedConfig(Map<String, Object> composition, Map<String, Object> overrides) throws Exception {
        // Start with base config
        String basePath = (String) composition.get("base");
        Map<String, Object> merged = new HashMap<>();
        
        if (basePath != null) {
            System.out.println("Loading base Excel config: " + basePath);
            Map<String, Object> baseData = loadYamlFile(basePath);
            merged = deepMerge(merged, baseData);
        }
        
        // Apply component configs in order
        List<String> components = (List<String>) composition.get("components");
        if (components != null) {
            for (String componentPath : components) {
                System.out.println("Loading component Excel config: " + componentPath);
                Map<String, Object> componentData = loadYamlFile(componentPath);
                merged = deepMerge(merged, componentData);
            }
        }
        
        // Apply final overrides from the composed file itself
        Map<String, Object> overridesClean = new HashMap<>(overrides);
        overridesClean.remove("composition");
        merged = deepMerge(merged, overridesClean);
        
        return parseExcelMergeConfig(merged);
    }
    
    /**
     * Deep merge two maps (recursive)
     */
    private Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new HashMap<>(base);
        
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            String key = entry.getKey();
            Object overrideValue = entry.getValue();
            
            if (overrideValue instanceof Map && result.get(key) instanceof Map) {
                // Both are maps, merge recursively
                result.put(key, deepMerge(
                    (Map<String, Object>) result.get(key),
                    (Map<String, Object>) overrideValue
                ));
            } else if (overrideValue instanceof List && result.get(key) instanceof List) {
                // Both are lists, merge intelligently
                result.put(key, mergeLists(
                    (List<?>) result.get(key),
                    (List<?>) overrideValue
                ));
            } else {
                // Override replaces base
                result.put(key, overrideValue);
            }
        }
        
        return result;
    }
    
    /**
     * Merge lists (append or replace based on context)
     */
    private List<?> mergeLists(List<?> base, List<?> override) {
        // For Excel configs, we typically replace lists
        // (e.g., tableMappings should be complete in each config)
        return override;
    }
    
    /**
     * Parse YAML map into ExcelMergeConfig object
     */
    private ExcelMergeConfig parseExcelMergeConfig(Map<String, Object> data) {
        ExcelMergeConfig config = new ExcelMergeConfig();
        
        // Parse top-level fields
        if (data.containsKey("templatePath")) {
            config.setTemplatePath((String) data.get("templatePath"));
        }
        
        if (data.containsKey("preprocessingRules")) {
            config.setPreprocessingRules((String) data.get("preprocessingRules"));
        }
        
        if (data.containsKey("description")) {
            config.setDescription((String) data.get("description"));
        }
        
        if (data.containsKey("version")) {
            config.setVersion((String) data.get("version"));
        }
        
        // Parse cellMappings
        if (data.containsKey("cellMappings")) {
            Map<String, String> cellMappings = (Map<String, String>) data.get("cellMappings");
            config.setCellMappings(cellMappings);
        }
        
        // Parse tableMappings
        if (data.containsKey("tableMappings")) {
            List<Map<String, Object>> tableMappingsData = (List<Map<String, Object>>) data.get("tableMappings");
            List<TableMappingConfig> tableMappings = new ArrayList<>();
            
            for (Map<String, Object> tableMappingData : tableMappingsData) {
                TableMappingConfig tableMapping = new TableMappingConfig();
                
                if (tableMappingData.containsKey("sheetName")) {
                    tableMapping.setSheetName((String) tableMappingData.get("sheetName"));
                }
                
                if (tableMappingData.containsKey("startRow")) {
                    tableMapping.setStartRow((Integer) tableMappingData.get("startRow"));
                }
                
                if (tableMappingData.containsKey("sourcePath")) {
                    tableMapping.setSourcePath((String) tableMappingData.get("sourcePath"));
                }
                
                if (tableMappingData.containsKey("columnMappings")) {
                    // Convert string keys to integers
                    Map<?, String> rawColumnMappings = (Map<?, String>) tableMappingData.get("columnMappings");
                    Map<Integer, String> columnMappings = new HashMap<>();
                    
                    for (Map.Entry<?, String> entry : rawColumnMappings.entrySet()) {
                        Integer columnIndex;
                        if (entry.getKey() instanceof Integer) {
                            columnIndex = (Integer) entry.getKey();
                        } else {
                            columnIndex = Integer.parseInt(entry.getKey().toString());
                        }
                        columnMappings.put(columnIndex, entry.getValue());
                    }
                    
                    tableMapping.setColumnMappings(columnMappings);
                }
                
                tableMappings.add(tableMapping);
            }
            
            config.setTableMappings(tableMappings);
        }
        
        return config;
    }
}
