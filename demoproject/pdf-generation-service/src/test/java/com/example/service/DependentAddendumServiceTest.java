package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for dependent addendum generation when more than 3 dependents exist.
 */
class DependentAddendumServiceTest {
    
    private DependentAddendumService service;
    
    @BeforeEach
    void setUp() {
        service = new DependentAddendumService();
    }
    
    @Test
    @DisplayName("No addendum needed when 3 or fewer dependents")
    void testNoAddendumNeeded() {
        List<Map<String, Object>> applicants = createApplicants(1, 1, 3); // PRIMARY, SPOUSE, 3 DEPENDENTS
        
        assertFalse(service.isAddendumNeeded(applicants), 
                   "Should not need addendum for 3 dependents");
    }
    
    @Test
    @DisplayName("Addendum needed when more than 3 dependents")
    void testAddendumNeeded() {
        List<Map<String, Object>> applicants = createApplicants(1, 1, 5); // PRIMARY, SPOUSE, 5 DEPENDENTS
        
        assertTrue(service.isAddendumNeeded(applicants),
                  "Should need addendum for 5 dependents");
    }
    
    @Test
    @DisplayName("Generate addendum for 2 overflow dependents")
    void testGenerateAddendumFor2Overflow() throws Exception {
        // Create 5 dependents (3 in form, 2 in addendum)
        List<Map<String, Object>> applicants = createApplicants(1, 1, 5);
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateDependentAddendum(applicants, enrollmentData);
        
        assertNotNull(addendum, "Addendum should be generated");
        assertTrue(addendum.length > 0, "Addendum should have content");
        
        System.out.println("✓ Generated addendum for 2 overflow dependents");
        System.out.println("  PDF size: " + addendum.length + " bytes");
    }
    
    @Test
    @DisplayName("Generate addendum for 7 overflow dependents")
    void testGenerateAddendumFor7Overflow() throws Exception {
        // Create 10 dependents (3 in form, 7 in addendum)
        List<Map<String, Object>> applicants = createApplicants(1, 1, 10);
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateDependentAddendum(applicants, enrollmentData);
        
        assertNotNull(addendum, "Addendum should be generated");
        assertTrue(addendum.length > 0, "Addendum should have content");
        
        System.out.println("✓ Generated addendum for 7 overflow dependents");
        System.out.println("  PDF size: " + addendum.length + " bytes");
    }
    
    @Test
    @DisplayName("No addendum generated when exactly 3 dependents")
    void testNoAddendumWhen3Dependents() throws Exception {
        List<Map<String, Object>> applicants = createApplicants(1, 1, 3);
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateDependentAddendum(applicants, enrollmentData);
        
        assertNotNull(addendum, "Should return empty array, not null");
        assertEquals(0, addendum.length, "Should return empty byte array for 3 or fewer dependents");
        
        System.out.println("✓ No addendum generated for 3 dependents (within limit)");
    }
    
    @Test
    @DisplayName("Addendum handles missing demographic data")
    void testAddendumWithMissingData() throws Exception {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // Add PRIMARY
        applicants.add(createApplicant("A001", "PRIMARY", "John", "Doe"));
        
        // Add 4 dependents, some with missing data
        for (int i = 1; i <= 4; i++) {
            Map<String, Object> dependent = new HashMap<>();
            dependent.put("applicantId", "A00" + (i + 1));
            dependent.put("firstName", "Child" + i);
            dependent.put("lastName", "Doe");
            
            // Only add demographic for some
            if (i % 2 == 0) {
                Map<String, Object> demo = new HashMap<>();
                demo.put("relationshipType", "DEPENDENT");
                demo.put("dateOfBirth", "2015-01-0" + i);
                demo.put("gender", i % 2 == 0 ? "F" : "M");
                dependent.put("demographic", demo);
            } else {
                // Missing demographic - should still work
                Map<String, Object> demo = new HashMap<>();
                demo.put("relationshipType", "DEPENDENT");
                dependent.put("demographic", demo);
            }
            
            applicants.add(dependent);
        }
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateDependentAddendum(applicants, enrollmentData);
        
        assertTrue(addendum.length > 0, "Should generate addendum despite missing data");
        System.out.println("✓ Addendum handles missing demographic data gracefully");
    }
    
    @Test
    @DisplayName("Addendum with only dependents (no PRIMARY/SPOUSE)")
    void testAddendumWithOnlyDependents() throws Exception {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // Create 5 dependents only
        for (int i = 1; i <= 5; i++) {
            applicants.add(createApplicant("A00" + i, "DEPENDENT", "Child" + i, "Doe"));
        }
        
        Map<String, Object> enrollmentData = createEnrollmentData();
        
        byte[] addendum = service.generateDependentAddendum(applicants, enrollmentData);
        
        assertTrue(addendum.length > 0, "Should generate addendum for 5 dependents");
        System.out.println("✓ Addendum works with only dependents (no PRIMARY/SPOUSE)");
    }
    
    // Helper methods
    
    private List<Map<String, Object>> createApplicants(int primaryCount, int spouseCount, int dependentCount) {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        int idCounter = 1;
        
        // Add PRIMARY applicants
        for (int i = 0; i < primaryCount; i++) {
            applicants.add(createApplicant("A" + String.format("%03d", idCounter++), 
                                          "PRIMARY", "Primary" + i, "Member"));
        }
        
        // Add SPOUSE applicants
        for (int i = 0; i < spouseCount; i++) {
            applicants.add(createApplicant("A" + String.format("%03d", idCounter++), 
                                          "SPOUSE", "Spouse" + i, "Member"));
        }
        
        // Add DEPENDENT applicants
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
    
    private Map<String, Object> createEnrollmentData() {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("groupNumber", "GRP-12345");
        enrollment.put("effectiveDate", "2024-01-01");
        enrollment.put("planType", "FAMILY");
        return enrollment;
    }
}
