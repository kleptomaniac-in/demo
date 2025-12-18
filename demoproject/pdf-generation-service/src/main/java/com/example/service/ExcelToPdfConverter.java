package com.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for converting Excel files to PDF format
 * Pure Java implementation using Apache POI + PDFBox (no LibreOffice or iText required)
 */
@Service
public class ExcelToPdfConverter {
    
    private static final float CELL_WIDTH = 80f;
    private static final float CELL_HEIGHT = 18f;
    private static final float MARGIN = 40f;
    private static final float FONT_SIZE = 9f;
    
    /**
     * Convert Excel byte array to PDF using Apache POI + PDFBox
     * No external dependencies like LibreOffice required
     * 
     * @param excelData Excel file as byte array
     * @return PDF as byte array
     * @throws IOException if conversion fails
     */
    public byte[] convertToPdf(byte[] excelData) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelData);
             Workbook workbook = new XSSFWorkbook(bis);
             PDDocument document = new PDDocument()) {
            
            // Convert each sheet to a PDF page
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                convertSheetToPage(document, sheet);
            }
            
            // Save to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new IOException("Failed to convert Excel to PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert an Excel sheet to a PDF page
     */
    private void convertSheetToPage(PDDocument document, Sheet sheet) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;
            
            // Add sheet title
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Sheet: " + sheet.getSheetName());
            contentStream.endText();
            
            yPosition -= 30;
            
            // Get used range
            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();
            
            if (firstRow < 0) return; // Empty sheet
            
            // Process each row
            for (int rowNum = firstRow; rowNum <= lastRow && yPosition > MARGIN; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                float xPosition = MARGIN;
                int lastCell = row.getLastCellNum();
                
                // Draw cells in this row
                for (int cellNum = 0; cellNum < lastCell && xPosition < page.getMediaBox().getWidth() - MARGIN; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    String cellValue = getCellValueAsString(cell);
                    
                    // Draw cell border
                    contentStream.setStrokingColor(Color.LIGHT_GRAY);
                    contentStream.addRect(xPosition, yPosition - CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
                    contentStream.stroke();
                    
                    // Draw cell text
                    if (cellValue != null && !cellValue.isEmpty()) {
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                        contentStream.newLineAtOffset(xPosition + 2, yPosition - CELL_HEIGHT + 5);
                        
                        // Truncate if too long
                        String displayText = cellValue.length() > 15 ? cellValue.substring(0, 12) + "..." : cellValue;
                        contentStream.showText(displayText);
                        contentStream.endText();
                    }
                    
                    xPosition += CELL_WIDTH;
                }
                
                yPosition -= CELL_HEIGHT;
                
                // Check if we need a new page
                if (yPosition < MARGIN + 50 && rowNum < lastRow) {
                    // Start new page for remaining rows
                    contentStream.close();
                    convertRemainingRows(document, sheet, rowNum + 1, lastRow);
                    return;
                }
            }
        }
    }
    
    /**
     * Handle remaining rows on a new page
     */
    private void convertRemainingRows(PDDocument document, Sheet sheet, int startRow, int endRow) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - MARGIN;
            
            for (int rowNum = startRow; rowNum <= endRow && yPosition > MARGIN; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                float xPosition = MARGIN;
                int lastCell = row.getLastCellNum();
                
                for (int cellNum = 0; cellNum < lastCell && xPosition < page.getMediaBox().getWidth() - MARGIN; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    String cellValue = getCellValueAsString(cell);
                    
                    contentStream.setStrokingColor(Color.LIGHT_GRAY);
                    contentStream.addRect(xPosition, yPosition - CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
                    contentStream.stroke();
                    
                    if (cellValue != null && !cellValue.isEmpty()) {
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                        contentStream.newLineAtOffset(xPosition + 2, yPosition - CELL_HEIGHT + 5);
                        String displayText = cellValue.length() > 15 ? cellValue.substring(0, 12) + "..." : cellValue;
                        contentStream.showText(displayText);
                        contentStream.endText();
                    }
                    
                    xPosition += CELL_WIDTH;
                }
                
                yPosition -= CELL_HEIGHT;
            }
        }
    }
    
    /**
     * Extract cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
            default:
                return "";
        }
    }
    
    /**
     * Get information about conversion capabilities
     */
    public ConversionInfo getConversionInfo() {
        return new ConversionInfo(
            true,
            "Apache POI + PDFBox (Pure Java)",
            "Ready to convert - no external dependencies required"
        );
    }
    
    /**
     * Information about conversion capabilities
     */
    public static class ConversionInfo {
        private final boolean available;
        private final String method;
        private final String message;
        
        public ConversionInfo(boolean available, String method, String message) {
            this.available = available;
            this.method = method;
            this.message = message;
        }
        
        public boolean isAvailable() { return available; }
        public String getMethod() { return method; }
        public String getMessage() { return message; }
    }
}
