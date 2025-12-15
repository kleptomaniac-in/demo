package com.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for filling Excel templates with data.
 * 
 * Supports two approaches:
 * 1. Named Cell approach (similar to AcroForm): cellMappings map names to paths
 * 2. Table/List approach: populate repeating rows with array data
 */
@Service
public class ExcelTemplateService {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    
    /**
     * Fill Excel template using cell mappings (similar to AcroForm field mappings)
     * 
     * @param templatePath Path to Excel template (.xlsx)
     * @param cellMappings Map of cell reference (A1, B2) or named range → payload path
     * @param payload Data to fill into template
     * @return Filled Excel as byte array
     */
    public byte[] fillExcelTemplate(String templatePath, Map<String, String> cellMappings, Map<String, Object> payload) throws IOException {
        try (InputStream templateStream = loadTemplate(templatePath);
             Workbook workbook = new XSSFWorkbook(templateStream)) {
            
            // Process each mapping
            for (Map.Entry<String, String> mapping : cellMappings.entrySet()) {
                String cellRef = mapping.getKey();
                String payloadPath = mapping.getValue();
                
                // Resolve value from payload
                Object value = resolveValue(payload, payloadPath);
                
                if (value != null) {
                    setCellValue(workbook, cellRef, value);
                }
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Fill Excel template with table/list data (for repeating rows)
     * 
     * @param templatePath Path to Excel template
     * @param tableMappings Configuration for table data
     * @param payload Data to fill
     * @return Filled Excel as byte array
     */
    public byte[] fillExcelWithTables(String templatePath, List<TableMapping> tableMappings, Map<String, Object> payload) throws IOException {
        try (InputStream templateStream = loadTemplate(templatePath);
             Workbook workbook = new XSSFWorkbook(templateStream)) {
            
            for (TableMapping tableMapping : tableMappings) {
                fillTable(workbook, tableMapping, payload);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Load Excel template from file system
     */
    private InputStream loadTemplate(String templatePath) throws IOException {
        String fullPath = "../config-repo/excel-templates/" + templatePath;
        
        if (!Files.exists(Paths.get(fullPath))) {
            fullPath = "excel-templates/" + templatePath;
        }
        
        if (!Files.exists(Paths.get(fullPath))) {
            throw new IOException("Template not found: " + templatePath);
        }
        
        return new FileInputStream(fullPath);
    }
    
    /**
     * Set value in a cell using cell reference (A1) or named range
     */
    private void setCellValue(Workbook workbook, String cellRef, Object value) {
        Sheet sheet = null;
        Cell cell = null;
        
        // Try as named range first
        Name namedRange = workbook.getName(cellRef);
        if (namedRange != null) {
            String formula = namedRange.getRefersToFormula();
            CellReference ref = new CellReference(formula);
            sheet = workbook.getSheet(ref.getSheetName());
            if (sheet != null) {
                Row row = sheet.getRow(ref.getRow());
                if (row == null) {
                    row = sheet.createRow(ref.getRow());
                }
                cell = row.getCell(ref.getCol());
                if (cell == null) {
                    cell = row.createCell(ref.getCol());
                }
            }
        } else {
            // Try as direct cell reference (A1, B2, Sheet1!C3)
            CellReference ref = new CellReference(cellRef);
            
            // If no sheet specified, use first sheet
            if (ref.getSheetName() == null) {
                sheet = workbook.getSheetAt(0);
            } else {
                sheet = workbook.getSheet(ref.getSheetName());
            }
            
            if (sheet != null) {
                Row row = sheet.getRow(ref.getRow());
                if (row == null) {
                    row = sheet.createRow(ref.getRow());
                }
                cell = row.getCell(ref.getCol());
                if (cell == null) {
                    cell = row.createCell(ref.getCol());
                }
            }
        }
        
        if (cell != null) {
            setCellValueTyped(cell, value);
        } else {
            System.err.println("Warning: Cell not found: " + cellRef);
        }
    }
    
    /**
     * Set cell value with type conversion
     */
    private void setCellValueTyped(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            // Apply date format
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            CreationHelper createHelper = cell.getSheet().getWorkbook().getCreationHelper();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yyyy"));
            cell.setCellStyle(cellStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }
    
    /**
     * Fill table with repeating row data
     */
    private void fillTable(Workbook workbook, TableMapping tableMapping, Map<String, Object> payload) {
        // Resolve array data from payload
        Object arrayData = resolveValue(payload, tableMapping.getSourcePath());
        
        if (!(arrayData instanceof List)) {
            return; // No data or not an array
        }
        
        List<?> dataList = (List<?>) arrayData;
        
        // Get sheet
        Sheet sheet = workbook.getSheet(tableMapping.getSheetName());
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        
        // Get template row (to copy formatting)
        Row templateRow = sheet.getRow(tableMapping.getStartRow());
        
        // Fill data rows
        int currentRow = tableMapping.getStartRow();
        for (Object item : dataList) {
            if (!(item instanceof Map)) {
                continue;
            }
            
            Map<String, Object> itemMap = (Map<String, Object>) item;
            Row row = sheet.getRow(currentRow);
            if (row == null) {
                row = sheet.createRow(currentRow);
            }
            
            // Fill each column
            for (Map.Entry<Integer, String> columnMapping : tableMapping.getColumnMappings().entrySet()) {
                int columnIndex = columnMapping.getKey();
                String fieldPath = columnMapping.getValue();
                
                Object value = resolveValue(itemMap, fieldPath);
                
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                
                setCellValueTyped(cell, value);
            }
            
            currentRow++;
        }
    }
    
    /**
     * Resolve value from payload using path notation
     * Examples:
     *   "memberName" → payload.get("memberName")
     *   "member.name" → payload.get("member").get("name")
     *   "members[0].name" → payload.get("members").get(0).get("name")
     *   "applicants[relationship=DEPENDENT][0].firstName"
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
            
            // Handle array notation with filter: items[status=ACTIVE]
            if (part.contains("[") && part.contains("=")) {
                String arrayName = part.substring(0, part.indexOf('['));
                String filterExpr = part.substring(part.indexOf('[') + 1, part.indexOf(']'));
                
                // Get array
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(arrayName);
                }
                
                if (current instanceof List) {
                    // Apply filter
                    String[] filterParts = filterExpr.split("=");
                    String filterField = filterParts[0];
                    String filterValue = filterParts[1];
                    
                    List<?> list = (List<?>) current;
                    current = list.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> (Map<?, ?>) item)
                        .filter(item -> filterValue.equals(String.valueOf(item.get(filterField))))
                        .findFirst()
                        .orElse(null);
                }
            }
            // Handle simple array index: items[0]
            else if (part.contains("[") && !part.contains("=")) {
                String arrayName = part.substring(0, part.indexOf('['));
                String indexStr = part.substring(part.indexOf('[') + 1, part.indexOf(']'));
                
                // Get array
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(arrayName);
                }
                
                // Get by index
                if (current instanceof List) {
                    try {
                        int index = Integer.parseInt(indexStr);
                        List<?> list = (List<?>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null; // Index out of bounds
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            // Simple property access
            else {
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
     * Configuration for table/list data mapping
     */
    public static class TableMapping {
        private String sheetName;
        private int startRow;
        private String sourcePath;
        private Map<Integer, String> columnMappings; // column index → field path
        
        public TableMapping() {}
        
        public TableMapping(String sheetName, int startRow, String sourcePath, Map<Integer, String> columnMappings) {
            this.sheetName = sheetName;
            this.startRow = startRow;
            this.sourcePath = sourcePath;
            this.columnMappings = columnMappings;
        }
        
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
}
