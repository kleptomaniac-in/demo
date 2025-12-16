package com.example.service;

import java.util.Map;

/**
 * Configuration for table/list data mapping in Excel templates
 */
public class TableMappingConfig {
    private String sheetName;
    private int startRow;
    private String sourcePath;
    private Map<Integer, String> columnMappings; // column index → field path
    
    // Getters and setters
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    
    public int getStartRow() { return startRow; }
    public void setStartRow(int startRow) { this.startRow = startRow; }
    
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    
    public Map<Integer, String> getColumnMappings() { return columnMappings; }
    public void setColumnMappings(Map<Integer, String> columnMappings) { this.columnMappings = columnMappings; }
}
