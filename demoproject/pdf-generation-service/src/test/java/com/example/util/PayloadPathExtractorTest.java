package com.example.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PayloadPathExtractorTest {

    @Test
    void testSimplePath() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productType", "MEDICAL");
        
        List<String> result = PayloadPathExtractor.extractValues(payload, "productType");
        
        assertEquals(1, result.size());
        assertEquals("MEDICAL", result.get(0));
    }

    @Test
    void testNestedPath() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("state", "CA");
        payload.put("enrollment", enrollment);
        
        List<String> result = PayloadPathExtractor.extractValues(payload, "enrollment.state");
        
        assertEquals(1, result.size());
        assertEquals("CA", result.get(0));
    }

    @Test
    void testArrayPath() {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> members = new ArrayList<>();
        
        Map<String, Object> member1 = new HashMap<>();
        member1.put("name", "John");
        members.add(member1);
        
        Map<String, Object> member2 = new HashMap<>();
        member2.put("name", "Jane");
        members.add(member2);
        
        payload.put("members", members);
        
        List<String> result = PayloadPathExtractor.extractValues(payload, "members[].name");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("John"));
        assertTrue(result.contains("Jane"));
    }

    @Test
    void testNestedArrayPath() {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> members = new ArrayList<>();
        
        Map<String, Object> member1 = new HashMap<>();
        List<Map<String, Object>> products1 = new ArrayList<>();
        Map<String, Object> product1 = new HashMap<>();
        product1.put("type", "MEDICAL");
        products1.add(product1);
        member1.put("products", products1);
        members.add(member1);
        
        Map<String, Object> member2 = new HashMap<>();
        List<Map<String, Object>> products2 = new ArrayList<>();
        Map<String, Object> product2 = new HashMap<>();
        product2.put("type", "DENTAL");
        products2.add(product2);
        Map<String, Object> product3 = new HashMap<>();
        product3.put("type", "VISION");
        products2.add(product3);
        member2.put("products", products2);
        members.add(member2);
        
        payload.put("members", members);
        
        List<String> result = PayloadPathExtractor.extractValues(payload, "members[].products[].type");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("MEDICAL"));
        assertTrue(result.contains("DENTAL"));
        assertTrue(result.contains("VISION"));
    }

    @Test
    void testExtractUniqueValues() {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> members = new ArrayList<>();
        
        Map<String, Object> member1 = new HashMap<>();
        List<Map<String, Object>> products1 = new ArrayList<>();
        Map<String, Object> product1 = new HashMap<>();
        product1.put("type", "MEDICAL");
        products1.add(product1);
        member1.put("products", products1);
        members.add(member1);
        
        Map<String, Object> member2 = new HashMap<>();
        List<Map<String, Object>> products2 = new ArrayList<>();
        Map<String, Object> product2 = new HashMap<>();
        product2.put("type", "Medical");  // Duplicate with different case
        products2.add(product2);
        member2.put("products", products2);
        members.add(member2);
        
        payload.put("members", members);
        
        List<String> paths = Arrays.asList("members[].products[].type");
        List<String> result = PayloadPathExtractor.extractUniqueValues(payload, paths);
        
        assertEquals(1, result.size());
        assertEquals("medical", result.get(0));  // Should be lowercase and deduplicated
    }

    @Test
    void testMultiplePaths() {
        Map<String, Object> payload = new HashMap<>();
        
        // Path 1: members[].products[].type
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> member1 = new HashMap<>();
        List<Map<String, Object>> products1 = new ArrayList<>();
        Map<String, Object> product1 = new HashMap<>();
        product1.put("type", "MEDICAL");
        products1.add(product1);
        member1.put("products", products1);
        members.add(member1);
        payload.put("members", members);
        
        // Path 2: application.proposedProducts[].productType
        Map<String, Object> application = new HashMap<>();
        List<Map<String, Object>> proposedProducts = new ArrayList<>();
        Map<String, Object> proposed1 = new HashMap<>();
        proposed1.put("productType", "DENTAL");
        proposedProducts.add(proposed1);
        application.put("proposedProducts", proposedProducts);
        payload.put("application", application);
        
        List<String> paths = Arrays.asList(
            "members[].products[].type",
            "application.proposedProducts[].productType"
        );
        
        List<String> result = PayloadPathExtractor.extractUniqueValues(payload, paths);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("medical"));
        assertTrue(result.contains("dental"));
    }

    @Test
    void testExtractProductTypes_DirectMatch() {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> products = new ArrayList<>();
        
        Map<String, Object> product1 = new HashMap<>();
        product1.put("type", "MEDICAL");
        products.add(product1);
        
        Map<String, Object> product2 = new HashMap<>();
        product2.put("type", "dental");
        products.add(product2);
        
        payload.put("products", products);
        
        List<String> paths = Arrays.asList("products[].type");
        List<String> result = PayloadPathExtractor.extractProductTypes(payload, paths);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("medical"));
        assertTrue(result.contains("dental"));
    }

    @Test
    void testExtractProductTypes_FuzzyMatch() {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> products = new ArrayList<>();
        
        Map<String, Object> product1 = new HashMap<>();
        product1.put("name", "Medical PPO Plan");
        products.add(product1);
        
        Map<String, Object> product2 = new HashMap<>();
        product2.put("name", "Dental DPPO");
        products.add(product2);
        
        Map<String, Object> product3 = new HashMap<>();
        product3.put("name", "Vision Plan");
        products.add(product3);
        
        payload.put("products", products);
        
        List<String> paths = Arrays.asList("products[].name");
        List<String> result = PayloadPathExtractor.extractProductTypes(payload, paths);
        
        assertEquals(3, result.size());
        assertTrue(result.contains("medical"));
        assertTrue(result.contains("dental"));
        assertTrue(result.contains("vision"));
    }

    @Test
    void testNullOrEmptyPath() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("field", "value");
        
        List<String> result1 = PayloadPathExtractor.extractValues(payload, null);
        assertTrue(result1.isEmpty());
        
        List<String> result2 = PayloadPathExtractor.extractValues(payload, "");
        assertTrue(result2.isEmpty());
    }

    @Test
    void testNonExistentPath() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("field", "value");
        
        List<String> result = PayloadPathExtractor.extractValues(payload, "nonexistent.field");
        assertTrue(result.isEmpty());
    }

    @Test
    void testComplexNestedStructure() {
        // Simulates real enrollment payload structure
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> application = new HashMap<>();
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // Applicant 1: PRIMARY with 2 products
        Map<String, Object> applicant1 = new HashMap<>();
        applicant1.put("relationship", "PRIMARY");
        List<Map<String, Object>> products1 = new ArrayList<>();
        
        Map<String, Object> prod1 = new HashMap<>();
        prod1.put("productType", "MEDICAL");
        products1.add(prod1);
        
        Map<String, Object> prod2 = new HashMap<>();
        prod2.put("productType", "DENTAL");
        products1.add(prod2);
        
        applicant1.put("products", products1);
        applicants.add(applicant1);
        
        // Applicant 2: SPOUSE with 1 product
        Map<String, Object> applicant2 = new HashMap<>();
        applicant2.put("relationship", "SPOUSE");
        List<Map<String, Object>> products2 = new ArrayList<>();
        
        Map<String, Object> prod3 = new HashMap<>();
        prod3.put("productType", "VISION");
        products2.add(prod3);
        
        applicant2.put("products", products2);
        applicants.add(applicant2);
        
        application.put("applicants", applicants);
        payload.put("application", application);
        
        List<String> paths = Arrays.asList("application.applicants[].products[].productType");
        List<String> result = PayloadPathExtractor.extractProductTypes(payload, paths);
        
        assertEquals(3, result.size());
        assertTrue(result.contains("dental"));
        assertTrue(result.contains("medical"));
        assertTrue(result.contains("vision"));
    }

    @Test
    void testDefaultProductsFallback() {
        // Empty payload
        Map<String, Object> payload = new HashMap<>();
        
        List<String> paths = Arrays.asList("members[].products[].type");
        List<String> result = PayloadPathExtractor.extractProductTypes(payload, paths);
        
        assertTrue(result.isEmpty());  // No products found
    }
}
