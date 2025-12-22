package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Service for generating addendum pages for overflow coverages.
 * 
 * When applicants have multiple coverages (medical, dental, vision), only the first
 * coverage for each applicant is shown in the main AcroForm. Additional coverages
 * (2nd, 3rd, etc.) are listed in this addendum.
 */
@Service
public class CoverageAddendumService {
    
    private static final int MAX_COVERAGES_IN_FORM = 1; // First coverage per applicant in main form
    private static final float MARGIN = 50;
    private static final float TITLE_FONT_SIZE = 16;
    private static final float HEADER_FONT_SIZE = 12;
    private static final float BODY_FONT_SIZE = 9;
    private static final float LINE_HEIGHT = 14;
    
    /**
     * Generate addendum PDF for overflow coverages (2nd coverage onward for each applicant).
     * 
     * @param applicants All applicants with their coverages
     * @param enrollmentData Enrollment metadata
     * @return PDF bytes of the coverage addendum, or empty byte array if no overflow
     */
    public byte[] generateCoverageAddendum(List<Map<String, Object>> applicants,
                                          Map<String, Object> enrollmentData) throws IOException {
        return generateCoverageAddendum(applicants, enrollmentData, MAX_COVERAGES_IN_FORM);
    }
    
    public byte[] generateCoverageAddendum(List<Map<String, Object>> applicants,
                                          Map<String, Object> enrollmentData,
                                          int maxPerApplicant) throws IOException {
        // Collect overflow coverages per applicant
        List<CoverageRecord> overflowCoverages = new ArrayList<>();
        
        for (Map<String, Object> applicant : applicants) {
            String applicantName = getApplicantName(applicant);
            String relationship = getRelationshipType(applicant);
            
            // Get coverages for this applicant
            List<Map<String, Object>> coverages = (List<Map<String, Object>>) applicant.get("coverages");
            if (coverages == null || coverages.size() <= maxPerApplicant) {
                continue; // No overflow for this applicant
            }
            
            // Add overflow coverages (e.g., 2nd onward if maxPerApplicant=1)
            for (int i = maxPerApplicant; i < coverages.size(); i++) {
                Map<String, Object> coverage = coverages.get(i);
                overflowCoverages.add(new CoverageRecord(
                    applicantName,
                    relationship,
                    coverage,
                    i + 1 // Coverage number (1-based)
                ));
            }
        }
        
        // Check if we need an addendum
        if (overflowCoverages.isEmpty()) {
            return new byte[0]; // No overflow coverages
        }
        
        // Generate addendum PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPosition = page.getMediaBox().getHeight() - MARGIN;
            
            // Title
            yPosition = drawTitle(contentStream, yPosition);
            yPosition -= 20;
            
            // Enrollment info
            yPosition = drawEnrollmentInfo(contentStream, enrollmentData, yPosition);
            yPosition -= 20;
            
            // Table header
            yPosition = drawTableHeader(contentStream, yPosition);
            yPosition -= 5;
            
            // Coverage rows
            for (CoverageRecord record : overflowCoverages) {
                yPosition = drawCoverageRow(contentStream, record, yPosition);
                
                // Check if we need a new page
                if (yPosition < MARGIN + 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - MARGIN;
                    yPosition = drawTableHeader(contentStream, yPosition);
                    yPosition -= 5;
                }
            }
            
            contentStream.close();
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Check if coverage addendum is needed.
     */
    public boolean isAddendumNeeded(List<Map<String, Object>> applicants) {
        return isAddendumNeeded(applicants, MAX_COVERAGES_IN_FORM);
    }
    
    public boolean isAddendumNeeded(List<Map<String, Object>> applicants, int maxPerApplicant) {
        if (applicants == null) return false;
        
        System.out.println("CoverageAddendumService: Checking if coverage addendum needed for " + applicants.size() + " applicants (MAX=" + maxPerApplicant + ")");
        
        boolean needed = applicants.stream()
            .anyMatch(applicant -> {
                List<Map<String, Object>> coverages = (List<Map<String, Object>>) applicant.get("coverages");
                boolean hasOverflow = coverages != null && coverages.size() > maxPerApplicant;
                if (hasOverflow) {
                    String name = getApplicantName(applicant);
                    System.out.println("CoverageAddendumService: Applicant '" + name + "' has " + coverages.size() + " coverages (overflow detected)");
                }
                return hasOverflow;
            });
        
        System.out.println("CoverageAddendumService: Coverage addendum needed=" + needed);
        return needed;
    }
    
    private float drawTitle(PDPageContentStream contentStream, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("ADDENDUM - ADDITIONAL COVERAGES");
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
        contentStream.showText("Applicant");
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText("Relationship");
        contentStream.newLineAtOffset(90, 0);
        contentStream.showText("Coverage #");
        contentStream.newLineAtOffset(70, 0);
        contentStream.showText("Type");
        contentStream.newLineAtOffset(70, 0);
        contentStream.showText("Premium");
        contentStream.newLineAtOffset(60, 0);
        contentStream.showText("Carrier");
        contentStream.endText();
        
        return yPosition - LINE_HEIGHT - 10;
    }
    
    private float drawCoverageRow(PDPageContentStream contentStream,
                                 CoverageRecord record, float yPosition) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA, BODY_FONT_SIZE);
        
        // Draw row
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 5, yPosition);
        contentStream.showText(truncate(record.applicantName, 18));
        contentStream.newLineAtOffset(120, 0);
        contentStream.showText(record.relationship);
        contentStream.newLineAtOffset(90, 0);
        contentStream.showText(String.valueOf(record.coverageNumber));
        contentStream.newLineAtOffset(70, 0);
        contentStream.showText(record.productType);
        contentStream.newLineAtOffset(70, 0);
        contentStream.showText(record.premium);
        contentStream.newLineAtOffset(60, 0);
        contentStream.showText(truncate(record.carrier, 15));
        contentStream.endText();
        
        // Draw separator line
        contentStream.setStrokingColor(0.8f, 0.8f, 0.8f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, yPosition - 5);
        contentStream.lineTo(PDRectangle.LETTER.getWidth() - MARGIN, yPosition - 5);
        contentStream.stroke();
        
        return yPosition - LINE_HEIGHT - 5;
    }
    
    private String getApplicantName(Map<String, Object> applicant) {
        String firstName = (String) applicant.getOrDefault("firstName", "");
        String lastName = (String) applicant.getOrDefault("lastName", "");
        return (firstName + " " + lastName).trim();
    }
    
    private String getRelationshipType(Map<String, Object> applicant) {
        Map<String, Object> demographic = (Map<String, Object>) applicant.get("demographic");
        if (demographic != null) {
            return (String) demographic.getOrDefault("relationshipType", "N/A");
        }
        return "N/A";
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Internal record for coverage data
     */
    private static class CoverageRecord {
        String applicantName;
        String relationship;
        String productType;
        String premium;
        String carrier;
        int coverageNumber;
        
        CoverageRecord(String applicantName, String relationship, 
                      Map<String, Object> coverage, int coverageNumber) {
            this.applicantName = applicantName;
            this.relationship = relationship;
            this.coverageNumber = coverageNumber;
            
            // Extract coverage details
            this.productType = (String) coverage.getOrDefault("productType", "N/A");
            
            Object premiumObj = coverage.get("premium");
            this.premium = premiumObj != null ? "$" + premiumObj.toString() : "N/A";
            
            this.carrier = (String) coverage.getOrDefault("carrier", "N/A");
        }
    }
}
