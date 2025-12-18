package com.example.service;

import com.example.pdfgeneration.function.FunctionExpressionResolver;
import com.example.pdfgeneration.function.FunctionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for pattern-based field mappings in AcroForm
 */
class PatternBasedMappingTest {

    private AcroFormFillService service;
    private FunctionRegistry functionRegistry;
    private FunctionExpressionResolver functionResolver;

    @BeforeEach
    void setUp() {
        functionRegistry = new FunctionRegistry();
        functionResolver = new FunctionExpressionResolver(functionRegistry);
        service = new AcroFormFillService(functionResolver);
    }

    @Test
    @DisplayName("Single pattern expands correctly for multiple indices")
    void testSinglePatternExpansion() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Dependent{n}_*");
        pattern.setSource("applicants[relationship=DEPENDENT][{n}]");
        pattern.setMaxIndex(2); // 0, 1, 2
        
        Map<String, String> fields = new HashMap<>();
        fields.put("FirstName", "demographic.firstName");
        fields.put("LastName", "demographic.lastName");
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(6, expanded.size(), "Should generate 3 indices × 2 fields = 6 mappings");
        
        // Check Dependent1 (index 0)
        assertEquals("applicants[relationship=DEPENDENT][0].demographic.firstName", 
                     expanded.get("Dependent1_FirstName"));
        assertEquals("applicants[relationship=DEPENDENT][0].demographic.lastName", 
                     expanded.get("Dependent1_LastName"));
        
        // Check Dependent2 (index 1)
        assertEquals("applicants[relationship=DEPENDENT][1].demographic.firstName", 
                     expanded.get("Dependent2_FirstName"));
        assertEquals("applicants[relationship=DEPENDENT][1].demographic.lastName", 
                     expanded.get("Dependent2_LastName"));
        
        // Check Dependent3 (index 2)
        assertEquals("applicants[relationship=DEPENDENT][2].demographic.firstName", 
                     expanded.get("Dependent3_FirstName"));
        assertEquals("applicants[relationship=DEPENDENT][2].demographic.lastName", 
                     expanded.get("Dependent3_LastName"));
    }

    @Test
    @DisplayName("Multiple patterns expand independently")
    void testMultiplePatterns() {
        // Arrange
        FieldPattern dependentPattern = new FieldPattern();
        dependentPattern.setFieldPattern("Dependent{n}_*");
        dependentPattern.setSource("applicants[relationship=DEPENDENT][{n}]");
        dependentPattern.setMaxIndex(1); // 0, 1
        Map<String, String> dependentFields = new HashMap<>();
        dependentFields.put("Name", "demographic.firstName");
        dependentPattern.setFields(dependentFields);

        FieldPattern coveragePattern = new FieldPattern();
        coveragePattern.setFieldPattern("Coverage{n}_*");
        coveragePattern.setSource("coverages[{n}]");
        coveragePattern.setMaxIndex(1); // 0, 1
        Map<String, String> coverageFields = new HashMap<>();
        coverageFields.put("Type", "productType");
        coveragePattern.setFields(coverageFields);

        List<FieldPattern> patterns = Arrays.asList(dependentPattern, coveragePattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(4, expanded.size(), "Should generate 2 patterns × 2 indices × 1 field = 4 mappings");
        
        assertEquals("applicants[relationship=DEPENDENT][0].demographic.firstName", 
                     expanded.get("Dependent1_Name"));
        assertEquals("applicants[relationship=DEPENDENT][1].demographic.firstName", 
                     expanded.get("Dependent2_Name"));
        assertEquals("coverages[0].productType", 
                     expanded.get("Coverage1_Type"));
        assertEquals("coverages[1].productType", 
                     expanded.get("Coverage2_Type"));
    }

    @Test
    @DisplayName("Pattern with nested paths expands correctly")
    void testPatternWithNestedPaths() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Applicant{n}_*");
        pattern.setSource("applicants[{n}]");
        pattern.setMaxIndex(0); // Just 0
        
        Map<String, String> fields = new HashMap<>();
        fields.put("Street", "mailingAddress.street");
        fields.put("City", "mailingAddress.city");
        fields.put("Product", "products[0].planName");
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(3, expanded.size());
        assertEquals("applicants[0].mailingAddress.street", expanded.get("Applicant1_Street"));
        assertEquals("applicants[0].mailingAddress.city", expanded.get("Applicant1_City"));
        assertEquals("applicants[0].products[0].planName", expanded.get("Applicant1_Product"));
    }

    @Test
    @DisplayName("Empty patterns list returns empty map")
    void testEmptyPatternsList() {
        // Act
        Map<String, String> expanded = service.expandPatterns(new ArrayList<>());

        // Assert
        assertTrue(expanded.isEmpty());
    }

    @Test
    @DisplayName("Null patterns list returns empty map")
    void testNullPatternsList() {
        // Act
        Map<String, String> expanded = service.expandPatterns(null);

        // Assert
        assertTrue(expanded.isEmpty());
    }

    @Test
    @DisplayName("Pattern with maxIndex 0 generates single mapping")
    void testPatternWithMaxIndexZero() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Primary_*");
        pattern.setSource("applicants[relationship=PRIMARY][{n}]");
        pattern.setMaxIndex(0); // Only index 0
        
        Map<String, String> fields = new HashMap<>();
        fields.put("FirstName", "demographic.firstName");
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(1, expanded.size());
        assertEquals("applicants[relationship=PRIMARY][0].demographic.firstName", 
                     expanded.get("Primary_FirstName"));
    }

    @Test
    @DisplayName("Pattern with many fields expands all fields for each index")
    void testPatternWithManyFields() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Dep{n}_*");
        pattern.setSource("applicants[relationship=DEPENDENT][{n}]");
        pattern.setMaxIndex(0);
        
        Map<String, String> fields = new HashMap<>();
        fields.put("FirstName", "demographic.firstName");
        fields.put("LastName", "demographic.lastName");
        fields.put("DOB", "demographic.dateOfBirth");
        fields.put("Gender", "demographic.gender");
        fields.put("SSN", "demographic.ssn");
        fields.put("Street", "mailingAddress.street");
        fields.put("City", "mailingAddress.city");
        fields.put("State", "mailingAddress.state");
        fields.put("Zip", "mailingAddress.zipCode");
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(9, expanded.size(), "All 9 fields should be expanded");
        assertTrue(expanded.containsKey("Dep1_FirstName"));
        assertTrue(expanded.containsKey("Dep1_LastName"));
        assertTrue(expanded.containsKey("Dep1_DOB"));
        assertTrue(expanded.containsKey("Dep1_Gender"));
        assertTrue(expanded.containsKey("Dep1_SSN"));
        assertTrue(expanded.containsKey("Dep1_Street"));
        assertTrue(expanded.containsKey("Dep1_City"));
        assertTrue(expanded.containsKey("Dep1_State"));
        assertTrue(expanded.containsKey("Dep1_Zip"));
    }

    @Test
    @DisplayName("Pattern with filter syntax in source path")
    void testPatternWithFilterSyntax() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("PriorCoverage{n}_*");
        pattern.setSource("coverages[applicantId=A001][{n}]");
        pattern.setMaxIndex(1);
        
        Map<String, String> fields = new HashMap<>();
        fields.put("Carrier", "carrierName");
        fields.put("PolicyNumber", "policyNumber");
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(4, expanded.size());
        assertEquals("coverages[applicantId=A001][0].carrierName", 
                     expanded.get("PriorCoverage1_Carrier"));
        assertEquals("coverages[applicantId=A001][0].policyNumber", 
                     expanded.get("PriorCoverage1_PolicyNumber"));
        assertEquals("coverages[applicantId=A001][1].carrierName", 
                     expanded.get("PriorCoverage2_Carrier"));
        assertEquals("coverages[applicantId=A001][1].policyNumber", 
                     expanded.get("PriorCoverage2_PolicyNumber"));
    }

    @Test
    @DisplayName("Pattern with multiple filter conditions")
    void testPatternWithMultipleFilters() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("MedicalProduct{n}_*");
        pattern.setSource("applicants[relationship=DEPENDENT][{n}].products[productType=MEDICAL]");
        pattern.setMaxIndex(0);
        
        Map<String, String> fields = new HashMap<>();
        fields.put("PlanName", "planName");
        fields.put("Premium", "premium");
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(2, expanded.size());
        assertEquals("applicants[relationship=DEPENDENT][0].products[productType=MEDICAL].planName", 
                     expanded.get("MedicalProduct1_PlanName"));
        assertEquals("applicants[relationship=DEPENDENT][0].products[productType=MEDICAL].premium", 
                     expanded.get("MedicalProduct1_Premium"));
    }

    @Test
    @DisplayName("Invalid pattern with null fieldPattern is skipped")
    void testInvalidPatternNullFieldPattern() {
        // Arrange
        FieldPattern invalidPattern = new FieldPattern();
        invalidPattern.setFieldPattern(null); // Invalid
        invalidPattern.setSource("applicants[{n}]");
        invalidPattern.setMaxIndex(0);
        invalidPattern.setFields(Map.of("Name", "firstName"));

        FieldPattern validPattern = new FieldPattern();
        validPattern.setFieldPattern("Valid{n}_*");
        validPattern.setSource("applicants[{n}]");
        validPattern.setMaxIndex(0);
        validPattern.setFields(Map.of("Name", "firstName"));

        List<FieldPattern> patterns = Arrays.asList(invalidPattern, validPattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(1, expanded.size(), "Only valid pattern should be expanded");
        assertTrue(expanded.containsKey("Valid1_Name"));
    }

    @Test
    @DisplayName("Invalid pattern with null source is skipped")
    void testInvalidPatternNullSource() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Test{n}_*");
        pattern.setSource(null); // Invalid
        pattern.setMaxIndex(0);
        pattern.setFields(Map.of("Name", "firstName"));

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertTrue(expanded.isEmpty(), "Invalid pattern should be skipped");
    }

    @Test
    @DisplayName("Invalid pattern with null fields is skipped")
    void testInvalidPatternNullFields() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Test{n}_*");
        pattern.setSource("applicants[{n}]");
        pattern.setMaxIndex(0);
        pattern.setFields(null); // Invalid

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertTrue(expanded.isEmpty(), "Invalid pattern should be skipped");
    }

    @Test
    @DisplayName("Pattern display index starts at 1 while array index starts at 0")
    void testIndexNumbering() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Item{n}_*");
        pattern.setSource("items[{n}]");
        pattern.setMaxIndex(2);
        pattern.setFields(Map.of("Value", "value"));

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertTrue(expanded.containsKey("Item1_Value"), "Display index should start at 1");
        assertTrue(expanded.containsKey("Item2_Value"));
        assertTrue(expanded.containsKey("Item3_Value"));
        
        assertEquals("items[0].value", expanded.get("Item1_Value"), "Array index should start at 0");
        assertEquals("items[1].value", expanded.get("Item2_Value"));
        assertEquals("items[2].value", expanded.get("Item3_Value"));
    }

    @Test
    @DisplayName("Large maxIndex generates correct number of mappings")
    void testLargeMaxIndex() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Item{n}_*");
        pattern.setSource("items[{n}]");
        pattern.setMaxIndex(9); // 0-9 = 10 items
        pattern.setFields(Map.of("Value", "value"));

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(10, expanded.size(), "Should generate 10 mappings (indices 0-9)");
        assertTrue(expanded.containsKey("Item1_Value"));
        assertTrue(expanded.containsKey("Item10_Value"));
        assertEquals("items[9].value", expanded.get("Item10_Value"));
    }

    @Test
    @DisplayName("Same value can be mapped to multiple fields")
    void testSameValueToMultipleFields() {
        // Arrange
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Section{n}_*");
        pattern.setSource("applicants[{n}]");  // Use proper array indexing
        pattern.setMaxIndex(2); // 3 sections showing same data
        
        Map<String, String> fields = new HashMap<>();
        fields.put("Name", "fullName");
        fields.put("NameConfirm", "fullName");  // Same value, different field
        fields.put("SignatureName", "fullName"); // Same value again
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(9, expanded.size(), "3 indices × 3 fields = 9 mappings");
        
        // Verify all three fields in Section 1 point to same path
        assertEquals("applicants[0].fullName", expanded.get("Section1_Name"));
        assertEquals("applicants[0].fullName", expanded.get("Section1_NameConfirm"));
        assertEquals("applicants[0].fullName", expanded.get("Section1_SignatureName"));
        
        // Verify Section 2 also maps correctly
        assertEquals("applicants[1].fullName", expanded.get("Section2_Name"));
        assertEquals("applicants[1].fullName", expanded.get("Section2_NameConfirm"));
        assertEquals("applicants[1].fullName", expanded.get("Section2_SignatureName"));
        
        // Key point: Multiple PDF fields (Name, NameConfirm, SignatureName) all map to same source path
        // This demonstrates that the same value can be shown in multiple places in the PDF
    }

    @Test
    @DisplayName("Multiple patterns can reference same source data")
    void testMultiplePatternsShareSameSource() {
        // Arrange - Two different field patterns for same data
        FieldPattern detailPattern = new FieldPattern();
        detailPattern.setFieldPattern("Detail{n}_*");
        detailPattern.setSource("applicants[relationship=PRIMARY][{n}]");
        detailPattern.setMaxIndex(0);
        detailPattern.setFields(Map.of("Name", "demographic.firstName"));

        FieldPattern summaryPattern = new FieldPattern();
        summaryPattern.setFieldPattern("Summary{n}_*");
        summaryPattern.setSource("applicants[relationship=PRIMARY][{n}]");  // Same source
        summaryPattern.setMaxIndex(0);
        summaryPattern.setFields(Map.of("Name", "demographic.firstName"));  // Same path

        List<FieldPattern> patterns = Arrays.asList(detailPattern, summaryPattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(2, expanded.size());
        
        // Both patterns resolve to same data path
        assertEquals("applicants[relationship=PRIMARY][0].demographic.firstName", 
                     expanded.get("Detail1_Name"));
        assertEquals("applicants[relationship=PRIMARY][0].demographic.firstName", 
                     expanded.get("Summary1_Name"));
    }

    @Test
    @DisplayName("Static values can be defined with static: prefix")
    void testStaticValues() {
        // Arrange - Pattern that includes static values
        FieldPattern pattern = new FieldPattern();
        pattern.setFieldPattern("Section{n}_*");
        pattern.setSource("applicants[{n}]");
        pattern.setMaxIndex(1);
        
        Map<String, String> fields = new HashMap<>();
        fields.put("Name", "demographic.firstName");
        fields.put("FormType", "static:Enrollment Form");  // Static value
        fields.put("Version", "static:v2.0");              // Static value
        pattern.setFields(fields);

        List<FieldPattern> patterns = Collections.singletonList(pattern);

        // Act
        Map<String, String> expanded = service.expandPatterns(patterns);

        // Assert
        assertEquals(6, expanded.size(), "2 indices × 3 fields = 6");
        
        // Verify static values are preserved in mapping
        assertEquals("applicants[0].demographic.firstName", expanded.get("Section1_Name"));
        assertEquals("static:Enrollment Form", expanded.get("Section1_FormType"));
        assertEquals("static:v2.0", expanded.get("Section1_Version"));
        
        assertEquals("applicants[1].demographic.firstName", expanded.get("Section2_Name"));
        assertEquals("static:Enrollment Form", expanded.get("Section2_FormType"));
        assertEquals("static:v2.0", expanded.get("Section2_Version"));
    }
}
