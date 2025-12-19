package com.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manual test class for generating sample PDFs.
 * 
 * Run this as a regular Java application to generate sample PDFs in output/manual-test/
 * Then open the PDFs to verify they look correct.
 */
public class ManualPdfGenerationTest {
    
    private static final String OUTPUT_DIR = "output/manual-test";
    
    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(80));
            System.out.println("PDF GENERATION MANUAL TEST");
            System.out.println("=".repeat(80));
            System.out.println();
            
            // Create output directory
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            
            // Initialize services
            DependentAddendumService dependentService = new DependentAddendumService();
            CoverageAddendumService coverageService = new CoverageAddendumService();
            PdfMergerService mergerService = new PdfMergerService();
            
            // Test 1: Dependent Addendum
            System.out.println("Test 1: Generating Dependent Addendum...");
            testDependentAddendum(dependentService);
            
            // Test 2: Coverage Addendum
            System.out.println("\nTest 2: Generating Coverage Addendum...");
            testCoverageAddendum(coverageService);
            
            // Test 3: Merged PDF
            System.out.println("\nTest 3: Generating Merged PDF with Both Addendums...");
            testMergedPdf(dependentService, coverageService, mergerService);
            
            // Test 4: Large Family
            System.out.println("\nTest 4: Generating Large Family (10 overflow dependents)...");
            testLargeFamily(dependentService);
            
            // Test 5: Complex Coverage
            System.out.println("\nTest 5: Generating Complex Coverage Scenario...");
            testComplexCoverage(coverageService);
            
            // Summary
            System.out.println("\n" + "=".repeat(80));
            System.out.println("ALL PDFs GENERATED SUCCESSFULLY!");
            System.out.println("=".repeat(80));
            System.out.println("\n📁 Output directory: " + new java.io.File(OUTPUT_DIR).getAbsolutePath());
            System.out.println("\n👉 Open the PDFs in the output directory to verify:");
            System.out.println("   1. dependent-addendum.pdf - Table of overflow dependents");
            System.out.println("   2. coverage-addendum.pdf - Table of overflow coverages");
            System.out.println("   3. merged-complete.pdf - Complete enrollment with both addendums");
            System.out.println("   4. large-family.pdf - 10 overflow dependents (multi-page)");
            System.out.println("   5. complex-coverage.pdf - Multiple applicants with multiple coverages");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDependentAddendum(DependentAddendumService service) throws IOException {
        // Create family with 6 dependents (3 overflow)
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY
        applicants.add(createPerson("A001", "PRIMARY", "John", "Smith", "1980-05-15", "M", "123-45-6789"));
        
        // SPOUSE
        applicants.add(createPerson("A002", "SPOUSE", "Mary", "Smith", "1982-08-20", "F", "987-65-4321"));
        
        // 6 DEPENDENTS
        applicants.add(createPerson("A003", "DEPENDENT", "Tommy", "Smith", "2010-03-10", "M", "111-11-1111"));
        applicants.add(createPerson("A004", "DEPENDENT", "Sarah", "Smith", "2012-07-22", "F", "222-22-2222"));
        applicants.add(createPerson("A005", "DEPENDENT", "Billy", "Smith", "2014-11-05", "M", "333-33-3333"));
        applicants.add(createPerson("A006", "DEPENDENT", "Emma", "Smith", "2016-02-14", "F", "444-44-4444"));
        applicants.add(createPerson("A007", "DEPENDENT", "Jake", "Smith", "2018-09-30", "M", "555-55-5555"));
        applicants.add(createPerson("A008", "DEPENDENT", "Lily", "Smith", "2020-12-25", "F", "666-66-6666"));
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] pdf = service.generateDependentAddendum(applicants, enrollmentData);
        savePdf(pdf, "dependent-addendum.pdf");
        
        System.out.println("  ✓ Generated dependent-addendum.pdf");
        System.out.println("    - Total dependents: 6");
        System.out.println("    - In form: 3 (Tommy, Sarah, Billy)");
        System.out.println("    - In addendum: 3 (Emma, Jake, Lily)");
    }
    
    private static void testCoverageAddendum(CoverageAddendumService service) throws IOException {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 3 coverages
        Map<String, Object> primary = createPerson("A001", "PRIMARY", "John", "Smith", "1980-05-15", "M", "123-45-6789");
        primary.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 500.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 50.00, "Delta Dental"),
            createCoverage("VISION", 25.00, "VSP Vision Care")
        ));
        applicants.add(primary);
        
        // SPOUSE with 2 coverages
        Map<String, Object> spouse = createPerson("A002", "SPOUSE", "Mary", "Smith", "1982-08-20", "F", "987-65-4321");
        spouse.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 450.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 45.00, "Delta Dental")
        ));
        applicants.add(spouse);
        
        // DEPENDENT with 1 coverage
        Map<String, Object> dep = createPerson("A003", "DEPENDENT", "Tommy", "Smith", "2010-03-10", "M", "111-11-1111");
        dep.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 200.00, "Blue Cross Blue Shield")
        ));
        applicants.add(dep);
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] pdf = service.generateCoverageAddendum(applicants, enrollmentData);
        savePdf(pdf, "coverage-addendum.pdf");
        
        System.out.println("  ✓ Generated coverage-addendum.pdf");
        System.out.println("    - PRIMARY: 3 coverages (2 overflow: Dental, Vision)");
        System.out.println("    - SPOUSE: 2 coverages (1 overflow: Dental)");
        System.out.println("    - DEPENDENT: 1 coverage (0 overflow)");
    }
    
    private static void testMergedPdf(DependentAddendumService dependentService,
                                     CoverageAddendumService coverageService,
                                     PdfMergerService mergerService) throws IOException {
        // Create comprehensive test data
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 2 coverages
        Map<String, Object> primary = createPerson("A001", "PRIMARY", "John", "Smith", "1980-05-15", "M", "123-45-6789");
        primary.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 500.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 50.00, "Delta Dental")
        ));
        applicants.add(primary);
        
        // SPOUSE with 2 coverages
        Map<String, Object> spouse = createPerson("A002", "SPOUSE", "Mary", "Smith", "1982-08-20", "F", "987-65-4321");
        spouse.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 450.00, "Blue Cross Blue Shield"),
            createCoverage("VISION", 30.00, "VSP Vision Care")
        ));
        applicants.add(spouse);
        
        // 5 DEPENDENTS (2 overflow)
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> dep = createPerson(
                "A" + String.format("%03d", i + 2),
                "DEPENDENT",
                "Child" + i,
                "Smith",
                "201" + i + "-0" + i + "-10",
                i % 2 == 0 ? "F" : "M",
                i + "11-11-111" + i
            );
            dep.put("coverages", Arrays.asList(
                createCoverage("MEDICAL", 200.00, "Blue Cross Blue Shield")
            ));
            applicants.add(dep);
        }
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        // Generate both addendums
        byte[] dependentAddendum = dependentService.generateDependentAddendum(applicants, enrollmentData);
        byte[] coverageAddendum = coverageService.generateCoverageAddendum(applicants, enrollmentData);
        
        // Create mock main form
        byte[] mainForm = createSimplePdf(
            "HEALTH INSURANCE ENROLLMENT FORM\n\n" +
            "Group Number: GRP-12345\n" +
            "Effective Date: 2024-01-01\n\n" +
            "PRIMARY APPLICANT: John Smith\n" +
            "SPOUSE: Mary Smith\n" +
            "DEPENDENTS: 3 listed below (see addendum for additional dependents)\n\n" +
            "COVERAGES: First coverage per applicant listed below (see addendum for additional coverages)\n\n" +
            "Note: This is a mock main form. In production, this would be a filled AcroForm."
        );
        
        // Merge all
        byte[] merged = mergerService.mergePdfs(Arrays.asList(mainForm, dependentAddendum, coverageAddendum));
        savePdf(merged, "merged-complete.pdf");
        
        System.out.println("  ✓ Generated merged-complete.pdf");
        System.out.println("    - Page 1: Main enrollment form (mock)");
        System.out.println("    - Page 2: Dependent addendum (2 overflow)");
        System.out.println("    - Page 3: Coverage addendum (2 overflow)");
    }
    
    private static void testLargeFamily(DependentAddendumService service) throws IOException {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY
        applicants.add(createPerson("A001", "PRIMARY", "John", "Smith", "1980-05-15", "M", "123-45-6789"));
        
        // SPOUSE
        applicants.add(createPerson("A002", "SPOUSE", "Mary", "Smith", "1982-08-20", "F", "987-65-4321"));
        
        // 13 DEPENDENTS (10 overflow)
        for (int i = 1; i <= 13; i++) {
            applicants.add(createPerson(
                "A" + String.format("%03d", i + 2),
                "DEPENDENT",
                "Child" + i,
                "Smith",
                "201" + (i % 10) + "-0" + (i % 12 + 1) + "-" + String.format("%02d", i),
                i % 2 == 0 ? "F" : "M",
                String.format("%03d-%02d-%04d", i, i, 1000 + i)
            ));
        }
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] pdf = service.generateDependentAddendum(applicants, enrollmentData);
        savePdf(pdf, "large-family.pdf");
        
        System.out.println("  ✓ Generated large-family.pdf");
        System.out.println("    - Total dependents: 13");
        System.out.println("    - In form: 3");
        System.out.println("    - In addendum: 10 (should span multiple pages)");
    }
    
    private static void testComplexCoverage(CoverageAddendumService service) throws IOException {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 5 coverages
        Map<String, Object> primary = createPerson("A001", "PRIMARY", "John", "Smith", "1980-05-15", "M", "123-45-6789");
        primary.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 500.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 50.00, "Delta Dental"),
            createCoverage("VISION", 25.00, "VSP Vision Care"),
            createCoverage("LIFE", 30.00, "MetLife Insurance Company"),
            createCoverage("DISABILITY", 40.00, "Prudential Insurance")
        ));
        applicants.add(primary);
        
        // SPOUSE with 4 coverages
        Map<String, Object> spouse = createPerson("A002", "SPOUSE", "Mary", "Smith", "1982-08-20", "F", "987-65-4321");
        spouse.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 450.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 45.00, "Delta Dental"),
            createCoverage("VISION", 20.00, "VSP Vision Care"),
            createCoverage("LIFE", 25.00, "MetLife Insurance Company")
        ));
        applicants.add(spouse);
        
        // DEPENDENT with 3 coverages
        Map<String, Object> dep = createPerson("A003", "DEPENDENT", "Tommy", "Smith", "2010-03-10", "M", "111-11-1111");
        dep.put("coverages", Arrays.asList(
            createCoverage("MEDICAL", 200.00, "Blue Cross Blue Shield"),
            createCoverage("DENTAL", 30.00, "Delta Dental"),
            createCoverage("VISION", 15.00, "VSP Vision Care")
        ));
        applicants.add(dep);
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] pdf = service.generateCoverageAddendum(applicants, enrollmentData);
        savePdf(pdf, "complex-coverage.pdf");
        
        System.out.println("  ✓ Generated complex-coverage.pdf");
        System.out.println("    - PRIMARY: 5 coverages (4 overflow)");
        System.out.println("    - SPOUSE: 4 coverages (3 overflow)");
        System.out.println("    - DEPENDENT: 3 coverages (2 overflow)");
        System.out.println("    - Total overflow: 9 coverages");
    }
    
    // Helper methods
    
    private static Map<String, Object> createPerson(String id, String relationshipType,
                                                    String firstName, String lastName,
                                                    String dob, String gender, String ssn) {
        Map<String, Object> applicant = new HashMap<>();
        applicant.put("applicantId", id);
        applicant.put("firstName", firstName);
        applicant.put("lastName", lastName);
        
        Map<String, Object> demographic = new HashMap<>();
        demographic.put("relationshipType", relationshipType);
        demographic.put("dateOfBirth", dob);
        demographic.put("gender", gender);
        demographic.put("ssn", ssn);
        applicant.put("demographic", demographic);
        
        return applicant;
    }
    
    private static Map<String, Object> createCoverage(String productType, double premium, String carrier) {
        Map<String, Object> coverage = new HashMap<>();
        coverage.put("productType", productType);
        coverage.put("premium", premium);
        coverage.put("carrier", carrier);
        return coverage;
    }
    
    private static Map<String, Object> createEnrollmentData() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("groupNumber", "GRP-12345");
        enrollment.put("effectiveDate", "2024-01-01");
        enrollment.put("planType", "FAMILY");
        return enrollment;
    }
    
    private static void savePdf(byte[] pdfBytes, String filename) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR, filename);
        Files.write(outputPath, pdfBytes);
    }
    
    private static byte[] createSimplePdf(String text) throws IOException {
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        doc.addPage(page);
        
        try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = 
                new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
            contentStream.beginText();
            contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(50, 700);
            
            // Split text by newlines and show each line
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
}
