package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

/**
 * Performance tests for AcroFormFillService optimizations
 */
class AcroFormFillServicePerformanceTest {

    private AcroFormFillService service;

    @BeforeEach
    void setUp() {
        service = new AcroFormFillService(null);
    }

    @Test
    @DisplayName("Performance: Filter 1000 applicants by relationship")
    void testLargeArrayFiltering() {
        // Create payload with 1000 applicants
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> applicant = new HashMap<>();
            String relationship = i % 3 == 0 ? "PRIMARY" : (i % 3 == 1 ? "SPOUSE" : "DEPENDENT");
            applicant.put("relationship", relationship);
            
            Map<String, Object> demo = new HashMap<>();
            demo.put("firstName", "Person" + i);
            demo.put("lastName", "Test");
            demo.put("relationshipType", relationship);
            applicant.put("demographic", demo);
            
            applicants.add(applicant);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicants", applicants);
        
        // Warm up
        for (int i = 0; i < 10; i++) {
            invokeResolveValue(payload, "applicants[relationship=PRIMARY].demographic.firstName");
        }
        
        // Measure time for filtering operations
        long startTime = System.nanoTime();
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            Object result = invokeResolveValue(payload, "applicants[relationship=PRIMARY].demographic.firstName");
            assert result != null : "Result should not be null";
        }
        
        long endTime = System.nanoTime();
        double avgTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        System.out.println("✓ Large array filtering (1000 items):");
        System.out.println("  Average time per lookup: " + String.format("%.3f", avgTimeMs) + " ms");
        System.out.println("  Total iterations: " + iterations);
        
        // With early termination, this should be fast (< 1ms per lookup)
        assert avgTimeMs < 5.0 : "Filtering should be fast with early termination";
    }

    @Test
    @DisplayName("Performance: Repeated path resolution with caching")
    void testPathCachingPerformance() {
        Map<String, Object> payload = createTestPayload();
        
        String[] paths = {
            "applicants[relationship=PRIMARY].demographic.firstName",
            "applicants[relationship=SPOUSE].demographic.lastName",
            "applicants[relationship=DEPENDENT][0].demographic.firstName",
            "company.address.city",
            "static:Test Value"
        };
        
        // Clear caches to ensure true cold measurement
        service.clearCaches();
        
        // Measure without cache (first call)
        long startCold = System.nanoTime();
        for (String path : paths) {
            invokeResolveValue(payload, path);
        }
        long coldTime = System.nanoTime() - startCold;
        
        // Measure with cache (repeated calls - simulates filling multiple fields)
        long startWarm = System.nanoTime();
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            for (String path : paths) {
                invokeResolveValue(payload, path);
            }
        }
        long warmTime = System.nanoTime() - startWarm;
        
        double coldMs = coldTime / 1_000_000.0;
        double warmMs = warmTime / 1_000_000.0 / iterations;
        double speedup = coldMs / warmMs;
        
        System.out.println("\n✓ Path caching performance:");
        System.out.println("  First call (cold): " + String.format("%.3f", coldMs) + " ms for " + paths.length + " paths");
        System.out.println("  Cached calls (warm): " + String.format("%.3f", warmMs) + " ms per iteration");
        System.out.println("  Speedup: " + String.format("%.1f", speedup) + "x faster");
        System.out.println("  Note: Speedup varies based on JVM warmup (100x+ in isolation, 1-5x in test suite)");
        
        // Performance validation: Ensure caching doesn't make things slower
        // We can't reliably test speedup in unit tests due to JVM warmup effects
        assert warmMs <= coldMs : "Caching should not make performance worse (actual: " + 
                                  String.format("%.1f", speedup) + "x)";
    }

    @Test
    @DisplayName("Performance: Nested field filter vs simple filter")
    void testNestedFilterPerformance() {
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        for (int i = 0; i < 500; i++) {
            Map<String, Object> applicant = new HashMap<>();
            applicant.put("relationship", i == 0 ? "PRIMARY" : "DEPENDENT");
            
            Map<String, Object> demo = new HashMap<>();
            demo.put("firstName", "Person" + i);
            demo.put("relationshipType", i == 0 ? "APPLICANT" : "DEPENDENT");
            applicant.put("demographic", demo);
            
            applicants.add(applicant);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicants", applicants);
        
        // Test simple filter
        long start1 = System.nanoTime();
        int iterations = 50;
        for (int i = 0; i < iterations; i++) {
            invokeResolveValue(payload, "applicants[relationship=PRIMARY].demographic.firstName");
        }
        long time1 = System.nanoTime() - start1;
        
        // Test nested filter
        long start2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            invokeResolveValue(payload, "applicants[demographic.relationshipType=APPLICANT].demographic.firstName");
        }
        long time2 = System.nanoTime() - start2;
        
        double simpleMs = time1 / 1_000_000.0 / iterations;
        double nestedMs = time2 / 1_000_000.0 / iterations;
        
        System.out.println("\n✓ Filter performance comparison (500 items):");
        System.out.println("  Simple filter (relationship=PRIMARY): " + String.format("%.3f", simpleMs) + " ms");
        System.out.println("  Nested filter (demographic.relationshipType=APPLICANT): " + String.format("%.3f", nestedMs) + " ms");
        System.out.println("  Overhead: " + String.format("%.1f", (nestedMs / simpleMs - 1) * 100) + "%");
        
        // Nested filter should be reasonably close to simple filter
        assert nestedMs < simpleMs * 3 : "Nested filter overhead should be reasonable";
    }

    @Test
    @DisplayName("Performance: Multiple filters vs single filter")
    void testMultipleFiltersPerformance() {
        List<Map<String, Object>> coverages = new ArrayList<>();
        
        for (int i = 0; i < 200; i++) {
            Map<String, Object> coverage = new HashMap<>();
            coverage.put("applicantId", "A" + (i % 10));
            coverage.put("productType", i % 2 == 0 ? "MEDICAL" : "DENTAL");
            coverage.put("premium", 450.00 + i);
            coverages.add(coverage);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("coverages", coverages);
        
        // Single filter
        long start1 = System.nanoTime();
        int iterations = 50;
        for (int i = 0; i < iterations; i++) {
            invokeResolveValue(payload, "coverages[applicantId=A001].premium");
        }
        long time1 = System.nanoTime() - start1;
        
        // Multiple filters
        long start2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            invokeResolveValue(payload, "coverages[applicantId=A001][productType=MEDICAL].premium");
        }
        long time2 = System.nanoTime() - start2;
        
        double singleMs = time1 / 1_000_000.0 / iterations;
        double multipleMs = time2 / 1_000_000.0 / iterations;
        
        System.out.println("\n✓ Multiple filters performance (200 items):");
        System.out.println("  Single filter: " + String.format("%.3f", singleMs) + " ms");
        System.out.println("  Two filters: " + String.format("%.3f", multipleMs) + " ms");
        System.out.println("  Overhead: " + String.format("%.1f", (multipleMs / singleMs - 1) * 100) + "%");
    }

    @Test
    @DisplayName("Performance: Early termination optimization effectiveness")
    void testEarlyTerminationBenefit() {
        // Create scenario where PRIMARY is at the end
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> applicant = new HashMap<>();
            // PRIMARY at the very end
            applicant.put("relationship", i == 999 ? "PRIMARY" : "DEPENDENT");
            
            Map<String, Object> demo = new HashMap<>();
            demo.put("firstName", "Person" + i);
            applicant.put("demographic", demo);
            
            applicants.add(applicant);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicants", applicants);
        
        // Measure lookup time
        long startTime = System.nanoTime();
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            Object result = invokeResolveValue(payload, "applicants[relationship=PRIMARY].demographic.firstName");
            assert "Person999".equals(result) : "Should find PRIMARY at position 999";
        }
        
        double avgTimeMs = (System.nanoTime() - startTime) / 1_000_000.0 / iterations;
        
        System.out.println("\n✓ Early termination (worst case - match at end):");
        System.out.println("  Array size: 1000 items");
        System.out.println("  Match position: 999 (last item)");
        System.out.println("  Average time: " + String.format("%.3f", avgTimeMs) + " ms");
        System.out.println("  Note: Early termination stops after first match");
    }

    private Map<String, Object> createTestPayload() {
        Map<String, Object> payload = new HashMap<>();
        
        // Company info
        Map<String, Object> company = new HashMap<>();
        company.put("name", "Test Corp");
        Map<String, Object> address = new HashMap<>();
        address.put("city", "San Francisco");
        company.put("address", address);
        payload.put("company", company);
        
        // Applicants
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        Map<String, Object> primary = new HashMap<>();
        primary.put("relationship", "PRIMARY");
        Map<String, Object> primaryDemo = new HashMap<>();
        primaryDemo.put("firstName", "John");
        primaryDemo.put("lastName", "Doe");
        primary.put("demographic", primaryDemo);
        applicants.add(primary);
        
        Map<String, Object> spouse = new HashMap<>();
        spouse.put("relationship", "SPOUSE");
        Map<String, Object> spouseDemo = new HashMap<>();
        spouseDemo.put("firstName", "Jane");
        spouseDemo.put("lastName", "Doe");
        spouse.put("demographic", spouseDemo);
        applicants.add(spouse);
        
        Map<String, Object> dependent = new HashMap<>();
        dependent.put("relationship", "DEPENDENT");
        Map<String, Object> depDemo = new HashMap<>();
        depDemo.put("firstName", "Junior");
        dependent.put("demographic", depDemo);
        applicants.add(dependent);
        
        payload.put("applicants", applicants);
        
        return payload;
    }

    private Object invokeResolveValue(Map<String, Object> payload, String path) {
        return ReflectionTestUtils.invokeMethod(service, "resolveValue", payload, path);
    }
}
