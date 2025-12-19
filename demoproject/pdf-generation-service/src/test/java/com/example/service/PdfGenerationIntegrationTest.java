package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PDF generation with addendums.
 * These tests generate actual PDFs that can be manually inspected.
 * 
 * PDFs are saved to: output/test-pdfs/
 */
class PdfGenerationIntegrationTest {
    
    private DependentAddendumService dependentAddendumService;
    private CoverageAddendumService coverageAddendumService;
    private PdfMergerService pdfMergerService;
    private EnrollmentPdfService enrollmentPdfService;
    
    // Output directory for generated PDFs
    private static final String OUTPUT_DIR = "output/test-pdfs";
    
    @BeforeEach
    void setUp() throws IOException {
        dependentAddendumService = new DependentAddendumService();
        coverageAddendumService = new CoverageAddendumService();
        pdfMergerService = new PdfMergerService();
        enrollmentPdfService = new EnrollmentPdfService();
        
        // Wire up services manually (no Spring context in test)
        java.lang.reflect.Field acroFormField = getField(EnrollmentPdfService.class, "acroFormService");
        java.lang.reflect.Field dependentField = getField(EnrollmentPdfService.class, "dependentAddendumService");
        java.lang.reflect.Field coverageField = getField(EnrollmentPdfService.class, "coverageAddendumService");
        java.lang.reflect.Field mergerField = getField(EnrollmentPdfService.class, "pdfMergerService");
        
        try {
            acroFormField.setAccessible(true);
            dependentField.setAccessible(true);
            coverageField.setAccessible(true);
            mergerField.setAccessible(true);
            
            acroFormField.set(enrollmentPdfService, new AcroFormFillService(null));
            dependentField.set(enrollmentPdfService, dependentAddendumService);
            coverageField.set(enrollmentPdfService, coverageAddendumService);
            mergerField.set(enrollmentPdfService, pdfMergerService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to wire services", e);
        }
        
        // Create output directory
        Files.createDirectories(Paths.get(OUTPUT_DIR));
    }
    
    @Test
    @DisplayName("Generate dependent addendum PDF - manual inspection")
    void testGenerateDependentAddendum() throws IOException {
        System.out.println("\n=== Test: Dependent Addendum ===");
        
        // Create family with 6 dependents (3 overflow)
        List<Map<String, Object>> applicants = createFamily(1, 1, 6);
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        // Generate addendum
        byte[] pdf = dependentAddendumService.generateDependentAddendum(applicants, enrollmentData);
        
        // Save to file
        String filename = "dependent-addendum-6deps.pdf";
        Path outputPath = savePdf(pdf, filename);
        
        // Assertions
        assertTrue(pdf.length > 0, "PDF should be generated");
        assertTrue(Files.exists(outputPath), "PDF file should exist");
        
        System.out.println("✓ Generated: " + outputPath);
        System.out.println("  Total applicants: " + applicants.size());
        System.out.println("  Dependents in form: 3");
        System.out.println("  Dependents in addendum: 3");
        System.out.println("  PDF size: " + pdf.length + " bytes");
        System.out.println("\n  👉 Open this file to verify the dependent addendum format");
    }
    
    @Test
    @DisplayName("Generate coverage addendum PDF - manual inspection")
    void testGenerateCoverageAddendum() throws IOException {
        System.out.println("\n=== Test: Coverage Addendum ===");
        
        // Create applicants with multiple coverages
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 3 coverages
        Map<String, Object> primary = createApplicant("A001", "PRIMARY", "John", "Doe");
        primary.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 500.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 50.00, "Delta Dental"),
            createCoverage("VISION", 25.00, "VSP Vision Care")
        ));
        applicants.add(primary);
        
        // SPOUSE with 2 coverages
        Map<String, Object> spouse = createApplicant("A002", "SPOUSE", "Jane", "Doe");
        spouse.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 450.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 45.00, "Delta Dental")
        ));
        applicants.add(spouse);
        
        // DEPENDENT with 1 coverage (no overflow)
        Map<String, Object> dep = createApplicant("A003", "DEPENDENT", "Jimmy", "Doe");
        dep.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 200.00, "Blue Cross Blue Shield")
        ));
        applicants.add(dep);
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        // Generate addendum
        byte[] pdf = coverageAddendumService.generateCoverageAddendum(applicants, enrollmentData);
        
        // Save to file
        String filename = "coverage-addendum-5overflow.pdf";
        Path outputPath = savePdf(pdf, filename);
        
        // Assertions
        assertTrue(pdf.length > 0, "PDF should be generated");
        assertTrue(Files.exists(outputPath), "PDF file should exist");
        
        System.out.println("✓ Generated: " + outputPath);
        System.out.println("  PRIMARY: 3 coverages (2 overflow)");
        System.out.println("  SPOUSE: 2 coverages (1 overflow)");
        System.out.println("  DEPENDENT: 1 coverage (0 overflow)");
        System.out.println("  Total overflow coverages: 3");
        System.out.println("  PDF size: " + pdf.length + " bytes");
        System.out.println("\n  👉 Open this file to verify the coverage addendum format");
    }
    
    @Test
    @DisplayName("Generate merged PDF with both addendums")
    void testMergedPdfWithBothAddendums() throws IOException {
        System.out.println("\n=== Test: Merged PDF (Both Addendums) ===");
        
        // Create comprehensive test data
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 3 coverages
        Map<String, Object> primary = createApplicant("A001", "PRIMARY", "John", "Doe");
        primary.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 500.00, "Blue Cross"),
            createCoverage("DENTAL", 50.00, "Delta Dental"),
            createCoverage("VISION", 25.00, "VSP")
        ));
        applicants.add(primary);
        
        // SPOUSE with 2 coverages
        Map<String, Object> spouse = createApplicant("A002", "SPOUSE", "Jane", "Doe");
        spouse.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 450.00, "Blue Cross"),
            createCoverage("DENTAL", 45.00, "Delta Dental")
        ));
        applicants.add(spouse);
        
        // 5 dependents (2 overflow) each with 1 coverage
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> dep = createApplicant("A" + String.format("%03d", i + 2), 
                                                     "DEPENDENT", "Child" + i, "Doe");
            dep.put("coverages", Arrays.asList(
                createCoverage("MEDICAL", 200.00, "Blue Cross")
            ));
            applicants.add(dep);
        }
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        // Generate both addendums
        byte[] dependentAddendum = dependentAddendumService.generateDependentAddendum(applicants, enrollmentData);
        byte[] coverageAddendum = coverageAddendumService.generateCoverageAddendum(applicants, enrollmentData);
        
        // Create a simple "main form" (in real scenario, this would be from AcroFormFillService)
        byte[] mockMainForm = createSimplePdf("MAIN ENROLLMENT FORM\n\nThis would be the filled AcroForm");
        
        // Merge all three
        List<byte[]> documents = Arrays.asList(mockMainForm, dependentAddendum, coverageAddendum);
        byte[] mergedPdf = pdfMergerService.mergePdfs(documents);
        
        // Save to file
        String filename = "complete-enrollment-with-both-addendums.pdf";
        Path outputPath = savePdf(mergedPdf, filename);
        
        // Assertions
        assertTrue(mergedPdf.length > 0, "Merged PDF should be generated");
        assertTrue(mergedPdf.length > dependentAddendum.length, "Merged should be larger than individual");
        assertTrue(mergedPdf.length > coverageAddendum.length, "Merged should be larger than individual");
        assertTrue(Files.exists(outputPath), "PDF file should exist");
        
        System.out.println("✓ Generated: " + outputPath);
        System.out.println("  Page 1: Main enrollment form (mock)");
        System.out.println("  Page 2: Dependent addendum (2 overflow dependents)");
        System.out.println("  Page 3: Coverage addendum (3 overflow coverages)");
        System.out.println("  Total pages: 3");
        System.out.println("  PDF size: " + mergedPdf.length + " bytes");
        System.out.println("\n  👉 Open this file to see the complete enrollment with both addendums");
    }
    
    @Test
    @DisplayName("Generate PDF with 10 overflow dependents (multi-page)")
    void testLargeDependentAddendum() throws IOException {
        System.out.println("\n=== Test: Large Dependent Addendum ===");
        
        // Create family with 13 dependents (10 overflow)
        List<Map<String, Object>> applicants = createFamily(1, 1, 13);
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] pdf = dependentAddendumService.generateDependentAddendum(applicants, enrollmentData);
        String filename = "dependent-addendum-10overflow.pdf";
        Path outputPath = savePdf(pdf, filename);
        
        assertTrue(pdf.length > 0, "PDF should be generated");
        
        System.out.println("✓ Generated: " + outputPath);
        System.out.println("  Total dependents: 13");
        System.out.println("  In form: 3");
        System.out.println("  In addendum: 10");
        System.out.println("  Expected pages: 1-2 (should span multiple pages)");
        System.out.println("  PDF size: " + pdf.length + " bytes");
        System.out.println("\n  👉 Open to verify multi-page addendum");
    }
    
    @Test
    @DisplayName("Generate PDF with many coverages per applicant")
    void testManyCoveragesPerApplicant() throws IOException {
        System.out.println("\n=== Test: Many Coverages Per Applicant ===");
        
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 5 coverages
        Map<String, Object> primary = createApplicant("A001", "PRIMARY", "John", "Doe");
        List<Map<String, Object>> coverages = new ArrayList<>();
        coverages.add(createCoverage("MEDICAL", 500.00, "Blue Cross Blue Shield"));
        coverages.add(createCoverage("DENTAL", 50.00, "Delta Dental"));
        coverages.add(createCoverage("VISION", 25.00, "VSP Vision Care"));
        coverages.add(createCoverage("LIFE", 30.00, "MetLife Insurance"));
        coverages.add(createCoverage("DISABILITY", 40.00, "Prudential"));
        primary.put("coverages", coverages);
        applicants.add(primary);
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] pdf = coverageAddendumService.generateCoverageAddendum(applicants, enrollmentData);
        String filename = "coverage-addendum-4overflow-one-person.pdf";
        Path outputPath = savePdf(pdf, filename);
        
        assertTrue(pdf.length > 0, "PDF should be generated");
        
        System.out.println("✓ Generated: " + outputPath);
        System.out.println("  PRIMARY coverages: 5");
        System.out.println("  In form: 1 (Medical)");
        System.out.println("  In addendum: 4 (Dental, Vision, Life, Disability)");
        System.out.println("  PDF size: " + pdf.length + " bytes");
        System.out.println("\n  👉 Open to see all overflow coverages for one person");
    }
    
    @Test
    @DisplayName("No addendums generated when within limits")
    void testNoAddendums() throws IOException {
        System.out.println("\n=== Test: No Addendums (Within Limits) ===");
        
        // Create family within limits
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 1 coverage
        Map<String, Object> primary = createApplicant("A001", "PRIMARY", "John", "Doe");
        primary.put("coverages", Arrays.asList(createCoverage("MEDICAL", 500.00, "Blue Cross")));
        applicants.add(primary);
        
        // SPOUSE with 1 coverage
        Map<String, Object> spouse = createApplicant("A002", "SPOUSE", "Jane", "Doe");
        spouse.put("coverages", Arrays.asList(createCoverage("MEDICAL", 450.00, "Blue Cross")));
        applicants.add(spouse);
        
        // 2 dependents (within limit) with 1 coverage each
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> dep = createApplicant("A00" + (i + 2), "DEPENDENT", "Child" + i, "Doe");
            dep.put("coverages", Arrays.asList(createCoverage("MEDICAL", 200.00, "Blue Cross")));
            applicants.add(dep);
        }
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        // Check if addendums are needed
        boolean dependentNeeded = dependentAddendumService.isAddendumNeeded(applicants);
        boolean coverageNeeded = coverageAddendumService.isAddendumNeeded(applicants);
        
        assertFalse(dependentNeeded, "Dependent addendum should not be needed");
        assertFalse(coverageNeeded, "Coverage addendum should not be needed");
        
        System.out.println("✓ Test passed: No addendums needed");
        System.out.println("  Total applicants: 4 (PRIMARY, SPOUSE, 2 DEPENDENTS)");
        System.out.println("  Dependents: 2 (within limit of 3)");
        System.out.println("  Coverages per person: 1 (within limit)");
        System.out.println("\n  ✓ Form would contain all data, no addendum pages");
    }
    
    // Helper methods
    
    private List<Map<String, Object>> createFamily(int primaryCount, int spouseCount, int dependentCount) {
        List<Map<String, Object>> applicants = new ArrayList<>();
        int idCounter = 1;
        
        for (int i = 0; i < primaryCount; i++) {
            applicants.add(createApplicant("A" + String.format("%03d", idCounter++), 
                                          "PRIMARY", "Primary" + i, "Member"));
        }
        
        for (int i = 0; i < spouseCount; i++) {
            applicants.add(createApplicant("A" + String.format("%03d", idCounter++), 
                                          "SPOUSE", "Spouse" + i, "Member"));
        }
        
        for (int i = 0; i < dependentCount; i++) {
            applicants.add(createApplicant("A" + String.format("%03d", idCounter++), 
                                          "DEPENDENT", "Child" + (i + 1), "Member"));
        }
        
        return applicants;
    }
    
    private Map<String, Object> createApplicant(String id, String relationshipType,
                                               String firstName, String lastName) {
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("applicantId", id);
        applicant.put("firstName", firstName);
        applicant.put("lastName", lastName);
        
        Map<String, Object> demographic = new HashMap<>();
        demographic.put("relationshipType", relationshipType);
        demographic.put("dateOfBirth", "2015-05-10");
        demographic.put("gender", "M");
        demographic.put("ssn", "123-45-" + id.substring(1));
        applicant.put("demographic", demographic);
        
        return applicant;
    }
    
    private Map<String, Object> createCoverage(String productType, double premium, String carrier) {
        Map<String, Object> coverage = new HashMap<>();
        coverage.put("productType", productType);
        coverage.put("premium", premium);
        coverage.put("carrier", carrier);
        return coverage;
    }
    
    private Map<String, Object> createEnrollmentData() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("groupNumber", "GRP-12345");
        enrollment.put("effectiveDate", "2024-01-01");
        enrollment.put("planType", "FAMILY");
        return enrollment;
    }
    
    private Path savePdf(byte[] pdfBytes, String filename) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR, filename);
        Files.write(outputPath, pdfBytes);
        return outputPath;
    }
    
    private byte[] createSimplePdf(String text) throws IOException {
        // Create a simple PDF for testing
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        doc.addPage(page);
        
        try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = 
                new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
            contentStream.beginText();
            contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(50, 700);
            
            // Split text by newlines and show each line (PDFBox doesn't support \n in showText)
            String[] lines = text.split("\n");
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
            }
            
            contentStream.endText();
        }
        
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        doc.save(out);
        doc.close();
        return out.toByteArray();
    }
    
    private java.lang.reflect.Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field not found: " + fieldName, e);
        }
    }
}
