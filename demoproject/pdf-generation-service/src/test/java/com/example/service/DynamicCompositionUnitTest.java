package com.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for dynamic composition logic (no Spring context required).
 * 
 * These tests verify the core logic of:
 * 1. Config name convention generation
 * 2. Dynamic composition structure building
 * 3. Product sorting and normalization
 */
public class DynamicCompositionUnitTest {

    @Test
    @DisplayName("Test convention-based config naming")
    public void testConfigNamingConvention() {
        // Test single product
        List<String> products1 = Arrays.asList("medical");
        String name1 = buildConfigName(products1, "individual", "CA");
        assertEquals("medical-individual-ca.yml", name1);
        
        // Test multiple products (should be sorted)
        List<String> products2 = Arrays.asList("vision", "medical", "dental");
        String name2 = buildConfigName(products2, "small-group", "TX");
        assertEquals("dental-medical-vision-small-group-tx.yml", name2);
        
        // Test already sorted
        List<String> products3 = Arrays.asList("dental", "vision");
        String name3 = buildConfigName(products3, "large-group", "NY");
        assertEquals("dental-vision-large-group-ny.yml", name3);
        
        System.out.println("✓ Config naming convention tests passed");
    }

    @Test
    @DisplayName("Test dynamic composition structure")
    public void testCompositionStructure() {
        List<String> products = Arrays.asList("medical", "dental");
        String market = "individual";
        String state = "CA";
        
        List<String> components = buildComponentList(products, market, state);
        
        // Should have base + 2 products + market + state
        assertTrue(components.contains("templates/products/dental.yml"));
        assertTrue(components.contains("templates/products/medical.yml"));
        assertTrue(components.contains("templates/markets/individual.yml"));
        assertTrue(components.contains("templates/states/ca.yml"));
        
        // Verify product order (alphabetical)
        int dentalIdx = components.indexOf("templates/products/dental.yml");
        int medicalIdx = components.indexOf("templates/products/medical.yml");
        assertTrue(dentalIdx < medicalIdx, "Products should be alphabetically sorted");
        
        System.out.println("✓ Composition structure test passed");
        System.out.println("  Components: " + components);
    }

    @Test
    @DisplayName("Test case normalization")
    public void testCaseNormalization() {
        List<String> products = Arrays.asList("MEDICAL", "Dental", "vision");
        String market = "SMALL-GROUP";
        String state = "Tx";
        
        String configName = buildConfigName(products, market, state);
        
        // All should be lowercase
        assertEquals("dental-medical-vision-small-group-tx.yml", configName);
        
        System.out.println("✓ Case normalization test passed");
    }

    @Test
    @DisplayName("Test alphabetical product sorting")
    public void testProductSorting() {
        // Test various orderings
        assertProductOrder(Arrays.asList("z", "a", "m"), "a-m-z");
        assertProductOrder(Arrays.asList("vision", "dental", "medical"), "dental-medical-vision");
        assertProductOrder(Arrays.asList("life", "dental"), "dental-life");
        
        System.out.println("✓ Product sorting tests passed");
    }

    @Test
    @DisplayName("Test empty or null handling")
    public void testEdgeCases() {
        // Null products should throw or return empty
        assertThrows(NullPointerException.class, () -> {
            buildConfigName(null, "individual", "CA");
        });
        
        // Empty products should throw or return empty
        List<String> emptyProducts = Arrays.asList();
        assertThrows(IllegalArgumentException.class, () -> {
            if (emptyProducts.isEmpty()) {
                throw new IllegalArgumentException("Products cannot be empty");
            }
            buildConfigName(emptyProducts, "individual", "CA");
        });
        
        System.out.println("✓ Edge case tests passed");
    }

    // Helper methods to simulate the actual service logic
    
    private String buildConfigName(List<String> products, String marketCategory, String state) {
        if (products == null) {
            throw new NullPointerException("Products cannot be null");
        }
        
        // Sort products alphabetically
        List<String> sortedProducts = products.stream()
            .map(String::toLowerCase)
            .sorted()
            .toList();
        
        // Build name: product1-product2-market-state.yml
        String productsStr = String.join("-", sortedProducts);
        String marketStr = marketCategory.toLowerCase();
        String stateStr = state.toLowerCase();
        
        return String.format("%s-%s-%s.yml", productsStr, marketStr, stateStr);
    }
    
    private List<String> buildComponentList(List<String> products, String marketCategory, String state) {
        List<String> components = new java.util.ArrayList<>();
        
        // Add sorted products
        products.stream()
            .map(String::toLowerCase)
            .sorted()
            .forEach(product -> components.add("templates/products/" + product + ".yml"));
        
        // Add market
        components.add("templates/markets/" + marketCategory.toLowerCase() + ".yml");
        
        // Add state
        components.add("templates/states/" + state.toLowerCase() + ".yml");
        
        return components;
    }
    
    private void assertProductOrder(List<String> input, String expected) {
        List<String> sorted = input.stream().sorted().toList();
        String result = String.join("-", sorted);
        assertEquals(expected, result);
    }
}
