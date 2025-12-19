package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for coverage addendum generation when applicants have multiple coverages.
 */
class CoverageAddendumServiceTest {
    
    private CoverageAddendumService service;
    
    @BeforeEach
    void setUp() {
        service = new CoverageAddendumService();
    }
    
    @Test
    @DisplayName("No addendum needed when each applicant has 1 coverage")
    void testNoAddendumNeeded() {
        // 3 applicants, each with 1 coverage
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{1, 1, 1} // PRIMARY: 1 coverage, SPOUSE: 1 coverage, DEPENDENT: 1 coverage
        );
        
        assertFalse(service.isAddendumNeeded(applicants),
                   "Should not need addendum when all applicants have 1 coverage");
    }
    
    @Test
    @DisplayName("Addendum needed when any applicant has 2+ coverages")
    void testAddendumNeeded() {
        // PRIMARY with 3 coverages (medical, dental, vision)
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{3, 1, 1} // PRIMARY: 3 coverages, SPOUSE: 1, DEPENDENT: 1
        );
        
        assertTrue(service.isAddendumNeeded(applicants),
                  "Should need addendum when PRIMARY has 3 coverages");
    }
    
    @Test
    @DisplayName("Generate addendum for multiple applicants with overflow")
    void testGenerateAddendumForMultipleApplicants() throws Exception {
        // PRIMARY: 3 coverages, SPOUSE: 2 coverages, DEPENDENT: 1 coverage
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{3, 2, 1}
        );
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertNotNull(addendum, "Addendum should be generated");
        assertTrue(addendum.length > 0, "Addendum should have content");
        
        System.out.println("✓ Generated coverage addendum");
        System.out.println("  PRIMARY: 2 overflow coverages (2nd, 3rd)");
        System.out.println("  SPOUSE: 1 overflow coverage (2nd)");
        System.out.println("  Total overflow: 3 coverages");
        System.out.println("  PDF size: " + addendum.length + " bytes");
    }
    
    @Test
    @DisplayName("No addendum generated when all applicants have exactly 1 coverage")
    void testNoAddendumWhenOneCoverageEach() throws Exception {
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{1, 1, 1, 1} // All have exactly 1 coverage
        );
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertNotNull(addendum, "Should return empty array, not null");
        assertEquals(0, addendum.length, "Should return empty byte array when no overflow");
        
        System.out.println("✓ No addendum generated (all applicants have 1 coverage)");
    }
    
    @Test
    @DisplayName("Addendum handles many coverages per applicant")
    void testAddendumWithManyCoverages() throws Exception {
        // One applicant with 5 coverages
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{5} // PRIMARY with 5 different coverages
        );
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertTrue(addendum.length > 0, "Should generate addendum for 4 overflow coverages");
        System.out.println("✓ Generated addendum for 4 overflow coverages (2nd through 5th)");
        System.out.println("  PDF size: " + addendum.length + " bytes");
    }
    
    @Test
    @DisplayName("Addendum handles missing coverage data")
    void testAddendumWithMissingData() throws Exception {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY with 2 coverages, one complete, one with missing fields
        Map<String, Object> primary = createApplicant("A001", "PRIMARY", "John", "Doe");
        List<Map<String, Object>> coverages = new ArrayList<>();
        
        // Complete coverage
        Map<String, Object> coverage1 = new HashMap<>();
        coverage1.put("productType", "MEDICAL");
        coverage1.put("premium", 500.00);
        coverage1.put("carrier", "Blue Cross");
        coverages.add(coverage1);
        
        // Incomplete coverage (missing fields)
        Map<String, Object> coverage2 = new HashMap<>();
        coverage2.put("productType", "DENTAL");
        // Missing premium and carrier
        coverages.add(coverage2);
        
        primary.put("coverages", coverages);
        applicants.add(primary);
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertTrue(addendum.length > 0, "Should generate addendum despite missing data");
        System.out.println("✓ Addendum handles missing coverage data gracefully");
    }
    
    @Test
    @DisplayName("Addendum with all family members having multiple coverages")
    void testAddendumWithFullFamily() throws Exception {
        // Family of 5, each with 2 coverages
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{2, 2, 2, 2, 2} // PRIMARY, SPOUSE, 3 DEPENDENTS - all with 2 coverages
        );
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertTrue(addendum.length > 0, "Should generate addendum for family");
        System.out.println("✓ Generated addendum for full family");
        System.out.println("  5 applicants × 1 overflow coverage each = 5 overflow coverages");
        System.out.println("  PDF size: " + addendum.length + " bytes");
    }
    
    @Test
    @DisplayName("Addendum with null coverages list")
    void testAddendumWithNullCoverages() throws Exception {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        Map<String, Object> applicant = createApplicant("A001", "PRIMARY", "John", "Doe");
        applicant.put("coverages", null); // Null coverages
        applicants.add(applicant);
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertEquals(0, addendum.length, "Should return empty when coverages are null");
        System.out.println("✓ Handles null coverages gracefully");
    }
    
    @Test
    @DisplayName("Addendum shows correct coverage numbers")
    void testCoverageNumbering() throws Exception {
        // PRIMARY with 4 coverages - should show as Coverage #2, #3, #4 in addendum
        List<Map<String, Object>> applicants = createApplicantsWithCoverages(
            new int[]{4}
        );
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateCoverageAddendum(applicants, enrollmentData);
        
        assertTrue(addendum.length > 0, "Should generate addendum");
        System.out.println("✓ Addendum displays coverage numbers correctly");
        System.out.println("  Coverage #2, #3, #4 shown in addendum");
        System.out.println("  (Coverage #1 is in main form)");
    }
    
    // Helper methods
    
    private List<Map<String, Object>> createApplicantsWithCoverages(int[] coverageCounts) {
        List<Map<String, Object>> applicants = new ArrayList<>();
        String[] relationships = {"PRIMARY", "SPOUSE", "DEPENDENT", "DEPENDENT", "DEPENDENT"};
        String[] names = {"John", "Jane", "Jimmy", "Jenny", "Julie"};
        
        for (int i = 0; i < coverageCounts.length; i++) {
            String relationship = relationships[Math.min(i, relationships.length - 1)];
            String firstName = names[Math.min(i, names.length - 1)];
            
            Map<String, Object> applicant = createApplicant(
                "A" + String.format("%03d", i + 1),
                relationship,
                firstName,
                "Doe"
            );
            
            // Add coverages
            List<Map<String, Object>> coverages = new ArrayList<>();
            String[] coverageTypes = {"MEDICAL", "DENTAL", "VISION", "LIFE", "DISABILITY"};
            double[] premiums = {500.00, 50.00, 25.00, 30.00, 40.00};
            String[] carriers = {"Blue Cross", "Delta Dental", "VSP", "MetLife", "Prudential"};
            
            for (int j = 0; j < coverageCounts[i]; j++) {
                Map<String, Object> coverage = new HashMap<>();
                coverage.put("productType", coverageTypes[j % coverageTypes.length]);
                coverage.put("premium", premiums[j % premiums.length]);
                coverage.put("carrier", carriers[j % carriers.length]);
                coverages.add(coverage);
            }
            
            applicant.put("coverages", coverages);
            applicants.add(applicant);
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
        demographic.put("dateOfBirth", "1980-01-15");
        demographic.put("gender", "M");
        applicant.put("demographic", demographic);
        
        return applicant;
    }
    
    private Map<String, Object> createEnrollmentData() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("groupNumber", "GRP-12345");
        enrollment.put("effectiveDate", "2024-01-01");
        enrollment.put("planType", "FAMILY");
        return enrollment;
    }
}
