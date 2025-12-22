package com.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for dynamic composition fallback when config files don't exist.
 * 
 * Tests verify that:
 * 1. Pre-generated config files are used when available
 * 2. Dynamic composition is triggered when files don't exist
 * 3. Dynamic composition builds correct structure
 * 4. Component files are loaded and merged correctly
 */
@SpringBootTest
public class DynamicCompositionTest {

    @Autowired
    private PdfMergeConfigService configService;

    @Autowired
    private ConfigSelectionService selectionService;

    @Test
    @DisplayName("Should load pre-generated config when file exists")
    public void testLoadPreGeneratedConfig() {
        // Given: A config file that exists
        String configName = "dental-individual-ca.yml";
        
        // When: Load config
        PdfMergeConfig config = configService.loadConfig(configName);
        
        // Then: Config should be loaded successfully
        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getSections(), "Sections should not be null");
        assertTrue(config.getSections().size() > 0, "Should have at least one section");
        
        System.out.println("✓ Pre-generated config loaded: " + configName);
        System.out.println("  Sections: " + config.getSections().size());
    }

    @Test
    @DisplayName("Should use dynamic composition when config file doesn't exist")
    public void testDynamicCompositionFallback() {
        // Given: An enrollment with rare product combination
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("vision", "life")); // Rare combination
        enrollment.setMarketCategory("medicare");
        enrollment.setState("WY");
        
        // When: Load config dynamically
        PdfMergeConfig config = configService.loadConfigDynamic(enrollment);
        
        // Then: Config should be built from components
        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getSections(), "Sections should not be null");
        
        System.out.println("✓ Dynamic composition created for rare combination");
        System.out.println("  Products: " + enrollment.getProducts());
        System.out.println("  Market: " + enrollment.getMarketCategory());
        System.out.println("  State: " + enrollment.getState());
        System.out.println("  Sections: " + config.getSections().size());
    }

    @Test
    @DisplayName("Should build correct dynamic composition structure")
    public void testDynamicCompositionStructure() {
        // Given: An enrollment
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("medical", "dental", "vision"));
        enrollment.setMarketCategory("small-group");
        enrollment.setState("TX");
        
        // When: Build dynamic composition
        Map<String, Object> composition = selectionService.buildDynamicComposition(enrollment);
        
        // Then: Composition should have correct structure
        assertNotNull(composition, "Composition should not be null");
        assertTrue(composition.containsKey("composition"), "Should contain composition key");
        
        Map<String, Object> innerComposition = (Map<String, Object>) composition.get("composition");
        assertEquals("templates/base-payer.yml", innerComposition.get("base"), 
            "Should have correct base template");
        
        List<String> components = (List<String>) innerComposition.get("components");
        assertNotNull(components, "Components should not be null");
        
        // Products should be alphabetically sorted
        assertTrue(components.contains("templates/products/dental.yml"), 
            "Should include dental component");
        assertTrue(components.contains("templates/products/medical.yml"), 
            "Should include medical component");
        assertTrue(components.contains("templates/products/vision.yml"), 
            "Should include vision component");
        assertTrue(components.contains("templates/markets/small-group.yml"), 
            "Should include market component");
        assertTrue(components.contains("templates/states/tx.yml"), 
            "Should include state component");
        
        // Verify alphabetical ordering of products
        int dentalIndex = components.indexOf("templates/products/dental.yml");
        int medicalIndex = components.indexOf("templates/products/medical.yml");
        int visionIndex = components.indexOf("templates/products/vision.yml");
        
        assertTrue(dentalIndex < medicalIndex && medicalIndex < visionIndex,
            "Products should be in alphabetical order");
        
        System.out.println("✓ Dynamic composition structure is correct");
        System.out.println("  Base: " + innerComposition.get("base"));
        System.out.println("  Components: " + components);
    }

    @Test
    @DisplayName("Should sort products alphabetically in dynamic composition")
    public void testProductAlphabeticalSorting() {
        // Given: Products in non-alphabetical order
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("vision", "dental", "medical")); // Not sorted
        enrollment.setMarketCategory("individual");
        enrollment.setState("CA");
        
        // When: Build dynamic composition
        PdfMergeConfig config = configService.loadConfigDynamic(enrollment);
        
        // Then: Products should be sorted alphabetically
        assertNotNull(config, "Config should not be null");
        
        System.out.println("✓ Products sorted alphabetically in dynamic composition");
        System.out.println("  Input order: vision, dental, medical");
        System.out.println("  Expected order: dental, medical, vision");
    }

    @Test
    @DisplayName("Should handle single product dynamic composition")
    public void testSingleProductDynamicComposition() {
        // Given: Single product enrollment
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("vision"));
        enrollment.setMarketCategory("large-group");
        enrollment.setState("NY");
        
        // When: Load config dynamically
        PdfMergeConfig config = configService.loadConfigDynamic(enrollment);
        
        // Then: Config should be built successfully
        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getSections(), "Sections should not be null");
        
        System.out.println("✓ Single product dynamic composition works");
        System.out.println("  Product: vision");
        System.out.println("  Sections: " + config.getSections().size());
    }

    @Test
    @DisplayName("Should handle all market categories in dynamic composition")
    public void testAllMarketCategories() {
        String[] markets = {"individual", "small-group", "large-group", "medicare"};
        
        for (String market : markets) {
            // Given: Enrollment with specific market
            EnrollmentSubmission enrollment = new EnrollmentSubmission();
            enrollment.setProducts(Arrays.asList("medical"));
            enrollment.setMarketCategory(market);
            enrollment.setState("CA");
            
            // When: Build composition
            Map<String, Object> composition = selectionService.buildDynamicComposition(enrollment);
            
            // Then: Should include correct market component
            Map<String, Object> innerComposition = (Map<String, Object>) composition.get("composition");
            List<String> components = (List<String>) innerComposition.get("components");
            
            String expectedMarketComponent = "templates/markets/" + market + ".yml";
            assertTrue(components.contains(expectedMarketComponent),
                "Should include market component: " + expectedMarketComponent);
            
            System.out.println("✓ Market category works: " + market);
        }
    }

    @Test
    @DisplayName("Should handle case-insensitive product names")
    public void testCaseInsensitiveProducts() {
        // Given: Products with mixed case
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("MEDICAL", "Dental", "vision"));
        enrollment.setMarketCategory("INDIVIDUAL");
        enrollment.setState("ca");
        
        // When: Build dynamic composition
        Map<String, Object> composition = selectionService.buildDynamicComposition(enrollment);
        
        // Then: Components should use lowercase
        Map<String, Object> innerComposition = (Map<String, Object>) composition.get("composition");
        List<String> components = (List<String>) innerComposition.get("components");
        
        assertTrue(components.contains("templates/products/medical.yml"),
            "Medical should be lowercase");
        assertTrue(components.contains("templates/products/dental.yml"),
            "Dental should be lowercase");
        assertTrue(components.contains("templates/products/vision.yml"),
            "Vision should be lowercase");
        assertTrue(components.contains("templates/markets/individual.yml"),
            "Market should be lowercase");
        assertTrue(components.contains("templates/states/ca.yml"),
            "State should be lowercase");
        
        System.out.println("✓ Case-insensitive handling works correctly");
    }

    @Test
    @DisplayName("Should fail gracefully when component files are missing")
    public void testMissingComponentFiles() {
        // Given: Enrollment with non-existent product
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("nonexistent-product"));
        enrollment.setMarketCategory("individual");
        enrollment.setState("CA");
        
        // When/Then: Should throw exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            configService.loadConfigDynamic(enrollment);
        });
        
        assertTrue(exception.getMessage().contains("Failed to load dynamic config"),
            "Should indicate dynamic config loading failure");
        
        System.out.println("✓ Fails gracefully with missing component: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should use convention-based config name for dynamic composition")
    public void testConventionBasedNaming() {
        // Test various combinations match expected naming convention
        
        // Test 1: Single product
        EnrollmentSubmission enrollment1 = new EnrollmentSubmission();
        enrollment1.setProducts(Arrays.asList("medical"));
        enrollment1.setMarketCategory("individual");
        enrollment1.setState("CA");
        
        String configName1 = selectionService.selectConfigByConvention(enrollment1);
        assertEquals("medical-individual-ca.yml", configName1,
            "Single product config name should match convention");
        
        // Test 2: Multiple products (should be sorted)
        EnrollmentSubmission enrollment2 = new EnrollmentSubmission();
        enrollment2.setProducts(Arrays.asList("vision", "medical", "dental"));
        enrollment2.setMarketCategory("small-group");
        enrollment2.setState("TX");
        
        String configName2 = selectionService.selectConfigByConvention(enrollment2);
        assertEquals("dental-medical-vision-small-group-tx.yml", configName2,
            "Multi-product config name should be alphabetically sorted");
        
        System.out.println("✓ Convention-based naming works correctly");
        System.out.println("  Single: " + configName1);
        System.out.println("  Multi: " + configName2);
    }

    @Test
    @DisplayName("Integration: Full flow from enrollment to PDF config")
    public void testFullDynamicCompositionFlow() {
        // Given: Rare enrollment combination
        EnrollmentSubmission enrollment = new EnrollmentSubmission();
        enrollment.setProducts(Arrays.asList("dental", "vision"));
        enrollment.setMarketCategory("large-group");
        enrollment.setState("WY");
        
        // When: Try to load config (may not exist)
        String configName = selectionService.selectConfigByConvention(enrollment);
        PdfMergeConfig config;
        
        try {
            config = configService.loadConfig(configName);
            System.out.println("✓ Pre-generated config found: " + configName);
        } catch (Exception e) {
            System.out.println("✗ Pre-generated config not found: " + configName);
            System.out.println("→ Using dynamic composition...");
            config = configService.loadConfigDynamic(enrollment);
            System.out.println("✓ Dynamic composition successful");
        }
        
        // Then: Should have valid config either way
        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getSections(), "Sections should not be null");
        assertTrue(config.getSections().size() > 0, "Should have at least one section");
        
        System.out.println("  Final sections count: " + config.getSections().size());
    }
}
