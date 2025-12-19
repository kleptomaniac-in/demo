package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service for generating addendum pages for overflow dependents.
 * 
 * When an enrollment form has more than 3 dependents, the first 3 are filled
 * in the main AcroForm template, and dependents 4+ are listed in this addendum.
 */
@Service
public class DependentAddendumService {
    
    private static final int MAX_DEPENDENTS_IN_FORM = 3;
    private static final float MARGIN = 50;
    private static final float TITLE_FONT_SIZE = 16;
    private static final float HEADER_FONT_SIZE = 12;
    private static final float BODY_FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 15;
    
    /**
     * Generate addendum PDF for overflow dependents (4th dependent onward).
     * 
     * @param applicants All applicants from the payload
     * @param enrollmentData Enrollment metadata (group number, effective date, etc.)
     * @return PDF bytes of the addendum, or empty byte array if no overflow
     */
    public byte[] generateDependentAddendum(List<Map<String, Object>> applicants, 
                                           Map<String, Object> enrollmentData) throws IOException {
        // Extract dependents
        List<Map<String, Object>> dependents = applicants.stream()
            .filter(a -> {
                Map<String, Object> demo = (Map<String, Object>) a.get("demographic");
                if (demo == null) return false;
                String relType = (String) demo.get("relationshipType");
                return "DEPENDENT".equals(relType) || "CHILD".equals(relType);
            })
            .toList();
        
        // Check if we need an addendum
        if (dependents.size() <= MAX_DEPENDENTS_IN_FORM) {
            return new byte[0]; // No addendum needed
        }
        
        // Get overflow dependents (4th onward)
        List<Map<String, Object>> overflowDependents = dependents.subList(MAX_DEPENDENTS_IN_FORM, dependents.size());
        
        // Generate addendum PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPosition = page.getMediaBox().getHeight() - MARGIN;
            
            // Title
            yPosition = drawTitle(contentStream, yPosition);
            yPosition -= 20; // Extra space after title
            
            // Enrollment info
            yPosition = drawEnrollmentInfo(contentStream, enrollmentData, yPosition);
            yPosition -= 20;
            
            // Table header
            yPosition = drawTableHeader(contentStream, yPosition);
            yPosition -= 5;
            
            // Dependent rows
            int dependentNumber = MAX_DEPENDENTS_IN_FORM + 1; // Start from 4
            for (Map<String, Object> dependent : overflowDependents) {
                yPosition = drawDependentRow(contentStream, dependent, dependentNumber, yPosition);
                dependentNumber++;
                
                // Check if we need a new page
                if (yPosition < MARGIN + 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    PDPageContentStream newStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - MARGIN;
                    yPosition = drawTableHeader(newStream, yPosition);
                    yPosition -= 5;
                    return generateAddendumRecursive(document, newStream, overflowDependents.subList(
                        dependentNumber - MAX_DEPENDENTS_IN_FORM - 1, overflowDependents.size()
                    ), dependentNumber, yPosition);
                }
            }
            
            // IMPORTANT: Close stream before saving
            contentStream.close();
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Helper to continue rendering on new pages if needed
     */
    private byte[] generateAddendumRecursive(PDDocument document, PDPageContentStream contentStream,
                                            List<Map<String, Object>> remainingDependents,
                                            int startNumber, float yPosition) throws IOException {
        for (Map<String, Object> dependent : remainingDependents) {
            yPosition = drawDependentRow(contentStream, dependent, startNumber, yPosition);
            startNumber++;
            
            if (yPosition < MARGIN + 50 && startNumber - MAX_DEPENDENTS_IN_FORM - 1 < remainingDependents.size()) {
                contentStream.close();
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                PDPageContentStream newStream = new PDPageContentStream(document, page);
                yPosition = page.getMediaBox().getHeight() - MARGIN;
                yPosition = drawTableHeader(newStream, yPosition);
                return generateAddendumRecursive(document, newStream,
                    remainingDependents.subList(startNumber - MAX_DEPENDENTS_IN_FORM - 1, remainingDependents.size()),
                    startNumber, yPosition);
            }
        }
        
        contentStream.close();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return outputStream.toByteArray();
    }
    
    private float drawTitle(PDPageContentStream contentStream, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("ADDENDUM - ADDITIONAL DEPENDENTS");
        contentStream.endText();
        return yPosition - LINE_HEIGHT * 1.5f;
    }
    
    private float drawEnrollmentInfo(PDPageContentStream contentStream, 
                                    Map<String, Object> enrollmentData, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, BODY_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        
        String groupNumber = (String) enrollmentData.getOrDefault("groupNumber", "N/A");
        String effectiveDate = (String) enrollmentData.getOrDefault("effectiveDate", "N/A");
        
        contentStream.showText("Group Number: " + groupNumber);
        contentStream.newLineAtOffset(0, -LINE_HEIGHT);
        contentStream.showText("Effective Date: " + effectiveDate);
        contentStream.endText();
        
        return yPosition - (LINE_HEIGHT * 2);
    }
    
    private float drawTableHeader(PDPageContentStream contentStream, float yPosition) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
        
        // Draw header background
        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        contentStream.addRect(MARGIN, yPosition - LINE_HEIGHT, 
                             PDRectangle.LETTER.getWidth() - (2 * MARGIN), LINE_HEIGHT + 5);
        contentStream.fill();
        
        // Draw header text
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 5, yPosition - LINE_HEIGHT + 3);
        contentStream.showText("#");
        contentStream.newLineAtOffset(20, 0);
        contentStream.showText("Name");
        contentStream.newLineAtOffset(150, 0);
        contentStream.showText("Date of Birth");
        contentStream.newLineAtOffset(100, 0);
        contentStream.showText("Gender");
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText("SSN");
        contentStream.endText();
        
        return yPosition - LINE_HEIGHT - 10;
    }
    
    private float drawDependentRow(PDPageContentStream contentStream, 
                                  Map<String, Object> dependent, 
                                  int number, float yPosition) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA, BODY_FONT_SIZE);
        
        // Extract data
        String firstName = (String) dependent.getOrDefault("firstName", "");
        String lastName = (String) dependent.getOrDefault("lastName", "");
        String fullName = (firstName + " " + lastName).trim();
        
        Map<String, Object> demographic = (Map<String, Object>) dependent.get("demographic");
        String dob = demographic != null ? (String) demographic.getOrDefault("dateOfBirth", "N/A") : "N/A";
        String gender = demographic != null ? (String) demographic.getOrDefault("gender", "N/A") : "N/A";
        String ssn = demographic != null ? maskSSN((String) demographic.get("ssn")) : "N/A";
        
        // Draw row
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 5, yPosition);
        contentStream.showText(String.valueOf(number));
        contentStream.newLineAtOffset(20, 0);
        contentStream.showText(fullName);
        contentStream.newLineAtOffset(150, 0);
        contentStream.showText(dob);
        contentStream.newLineAtOffset(100, 0);
        contentStream.showText(gender);
        contentStream.newLineAtOffset(80, 0);
        contentStream.showText(ssn);
        contentStream.endText();
        
        // Draw separator line
        contentStream.setStrokingColor(0.8f, 0.8f, 0.8f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, yPosition - 5);
        contentStream.lineTo(PDRectangle.LETTER.getWidth() - MARGIN, yPosition - 5);
        contentStream.stroke();
        
        return yPosition - LINE_HEIGHT - 5;
    }
    
    private String maskSSN(String ssn) {
        if (ssn == null || ssn.length() < 4) {
            return "***-**-****";
        }
        // Show last 4 digits only
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
    
    /**
     * Check if addendum is needed based on dependent count.
     */
    public boolean isAddendumNeeded(List<Map<String, Object>> applicants) {
        long dependentCount = applicants.stream()
            .filter(a -> {
                Map<String, Object> demo = (Map<String, Object>) a.get("demographic");
                if (demo == null) return false;
                String relType = (String) demo.get("relationshipType");
                return "DEPENDENT".equals(relType) || "CHILD".equals(relType);
            })
            .count();
        
        return dependentCount > MAX_DEPENDENTS_IN_FORM;
    }
}
