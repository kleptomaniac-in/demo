# Multi-Template Type PDF Generation - Testing Guide

This example demonstrates combining all three template types (FreeMarker, AcroForm, and PDFBox) in a single PDF document.

## Document Structure

The generated PDF will have 4 sections:

1. **Cover Page (FreeMarker)** - HTML-based cover page with application info
2. **Enrollment Form (AcroForm)** - PDF form with fillable fields using patterns and static values
3. **Coverage Summary (PDFBox)** - Programmatically generated summary page
4. **Terms & Conditions (FreeMarker)** - HTML-based legal terms

## Files

- **Configuration:** `test-multi-template-config.yml`
- **Payload:** `test-multi-template-payload.json`
- **FreeMarker Templates:**
  - `src/main/resources/templates/enrollment-cover.ftl`
  - `src/main/resources/templates/terms-and-conditions.ftl`
- **AcroForm Template:** `templates/enrollment-form.pdf` (you'll need to provide this)
- **PDFBox Generator:** `CoverageSummaryGenerator` (registered generator)

## Prerequisites

### 1. AcroForm Template

You need to create or provide a PDF form template with the following fields:

**Static Fields:**
- FormTitle
- FormVersion
- FormDate

**Application Fields:**
- ApplicationNumber
- EffectiveDate
- TotalPremium

**Primary Applicant Fields:**
- Primary_FirstName, Primary_LastName, Primary_DOB, Primary_Gender, Primary_SSN
- Primary_Street, Primary_City, Primary_State, Primary_Zip
- Primary_Coverage_Type, Primary_Coverage_Plan, Primary_Coverage_Premium

**Spouse Fields:**
- Spouse_FirstName, Spouse_LastName, Spouse_DOB

**Dependent Fields (Pattern-based):**
- Dependent1_FirstName, Dependent1_LastName, Dependent1_DOB, Dependent1_Gender, Dependent1_RelationshipLabel
- Dependent2_FirstName, Dependent2_LastName, Dependent2_DOB, Dependent2_Gender, Dependent2_RelationshipLabel
- Dependent3_FirstName, Dependent3_LastName, Dependent3_DOB, Dependent3_Gender, Dependent3_RelationshipLabel

### 2. PDFBox Generator

Ensure you have a `CoverageSummaryGenerator` registered in your PDFBox registry. Example:

```java
@Component
public class CoverageSummaryGenerator implements PdfBoxGenerator {
    
    @Override
    public byte[] generate(Map<String, Object> payload) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Coverage Summary");
                contentStream.endText();
                
                // Add coverage details...
                List<?> applicants = (List<?>) payload.get("applicants");
                float y = 700;
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Enrolled Members: " + applicants.size());
                contentStream.endText();
                
                y -= 30;
                for (Object app : applicants) {
                    Map<?, ?> applicant = (Map<?, ?>) app;
                    Map<?, ?> demo = (Map<?, ?>) applicant.get("demographic");
                    String name = demo.get("firstName") + " " + demo.get("lastName");
                    String relationship = (String) applicant.get("relationship");
                    
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.newLineAtOffset(70, y);
                    contentStream.showText("• " + name + " (" + relationship + ")");
                    contentStream.endText();
                    
                    y -= 20;
                }
                
                // Add total premium
                y -= 30;
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Total Monthly Premium: $" + payload.get("totalPremium"));
                contentStream.endText();
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
```

## Testing

### Method 1: Direct API Call

```bash
# Start the application
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run

# In another terminal, make the request
curl -X POST http://localhost:8080/api/pdf/generate \
  -H "Content-Type: application/json" \
  -d @test-multi-template-payload.json \
  --output multi-template-output.pdf

# View the generated PDF
xdg-open multi-template-output.pdf
```

### Method 2: Using Config Server

If your configuration is stored in the config server:

```bash
# Upload configuration to config server
# Place test-multi-template-config.yml in config-repo/

# Generate PDF
curl -X POST http://localhost:8080/api/pdf/generate/multi-template \
  -H "Content-Type: application/json" \
  -d @test-multi-template-payload.json \
  --output multi-template-output.pdf
```

### Method 3: Programmatic Test

Create a test class:

```java
@SpringBootTest
public class MultiTemplateTest {
    
    @Autowired
    private FlexiblePdfMergeService pdfMergeService;
    
    @Test
    public void testMultiTemplateGeneration() throws Exception {
        // Load configuration
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PdfMergeConfig config = mapper.readValue(
            new File("test-multi-template-config.yml"),
            PdfMergeConfig.class
        );
        
        // Load payload
        Map<String, Object> payload = new ObjectMapper().readValue(
            new File("test-multi-template-payload.json"),
            new TypeReference<Map<String, Object>>() {}
        );
        
        // Generate PDF
        byte[] pdfBytes = pdfMergeService.generatePdf(config, payload);
        
        // Save to file
        Files.write(Paths.get("multi-template-output.pdf"), pdfBytes);
        
        // Verify
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
```

## What to Verify

### Page 1: Cover Page (FreeMarker)
✅ Title: "Health Insurance Enrollment Application"
✅ Plan Year: 2026
✅ Application Number: APP-2025-12345
✅ Application Date: 2025-12-18
✅ Effective Date: 2026-01-01
✅ Primary Applicant: John Smith
✅ Company info in footer

### Page 2: Enrollment Form (AcroForm)
✅ Static Fields:
  - FormTitle: "Health Insurance Enrollment Form"
  - FormVersion: "v2.0"
  - FormDate: "2025-12-18"

✅ Primary Applicant:
  - Name: John Smith
  - DOB: 1985-03-15
  - Gender: Male
  - SSN: ***-**-1234
  - Address: 123 Main Street, Springfield, IL 62701
  - Coverage: Gold Plus Plan (MEDICAL) - $450.00

✅ Spouse:
  - Name: Jane Smith
  - DOB: 1987-07-22

✅ Dependents (Pattern-based with static labels):
  - Dependent1: Emily Smith (Female, DOB: 2010-05-10) - Label: "Dependent"
  - Dependent2: Michael Smith (Male, DOB: 2012-09-18) - Label: "Dependent"
  - Dependent3: Sarah Smith (Female, DOB: 2015-12-03) - Label: "Dependent"

### Page 3: Coverage Summary (PDFBox)
✅ Title: "Coverage Summary"
✅ Enrolled Members: 5
✅ List of all applicants with relationships
✅ Total Monthly Premium: $850.00

### Page 4: Terms & Conditions (FreeMarker)
✅ Enrollment terms section
✅ Effective date: 2026-01-01
✅ Premium amount: $850.00
✅ List of all 5 covered individuals
✅ Cancellation policy
✅ Certification statement
✅ Privacy notice
✅ Company contact information
✅ Agent information (Robert Johnson)
✅ Footer with copyright and application number

## Features Demonstrated

1. **FreeMarker Templates**
   - HTML to PDF conversion
   - Variable substitution
   - Conditional rendering (`<#if>`)
   - List iteration (`<#list>`)
   - CSS styling

2. **AcroForm**
   - Static values (`static:` prefix)
   - Pattern-based field mappings
   - Filter syntax for array access
   - Mixed dynamic and static fields
   - Null-safe field resolution

3. **PDFBox**
   - Programmatic PDF generation
   - Custom layout and formatting
   - Dynamic content based on payload
   - List rendering

4. **Combined Features**
   - Multiple template types in one document
   - Shared payload across all sections
   - Consistent data presentation
   - Bookmarks for navigation
   - Professional multi-page document

## Troubleshooting

### Issue: AcroForm template not found
**Solution:** Ensure `templates/enrollment-form.pdf` exists in the resources folder

### Issue: Missing AcroForm fields
**Solution:** Verify all field names in the template match the configuration exactly

### Issue: PDFBox generator not found
**Solution:** Ensure `CoverageSummaryGenerator` is registered in the PDFBox registry

### Issue: FreeMarker template error
**Solution:** Check FreeMarker syntax and verify all variables exist in the payload

### Issue: Empty dependent fields
**Solution:** Normal behavior if fewer than 3 dependents - pattern generates all mappings but empty data returns null (blank fields)

## Expected Output

**File:** `multi-template-output.pdf`
**Pages:** 4
**Size:** ~50-100 KB (depending on template complexity)

**Content Verification:**
- All 5 applicants should appear across different sections
- Static form metadata should be consistent
- Coverage details should match payload
- Terms should reference correct dates and amounts
- Bookmarks should allow easy navigation

## Next Steps

1. **Customize Templates:** Modify FreeMarker templates for your branding
2. **Create AcroForm:** Design PDF form in Adobe Acrobat or similar tool
3. **Enhance PDFBox:** Add more sophisticated layouts and charts
4. **Add Validation:** Implement payload validation before generation
5. **Error Handling:** Add custom error pages for missing data
6. **Conditional Sections:** Use conditionalSections to show/hide based on data
