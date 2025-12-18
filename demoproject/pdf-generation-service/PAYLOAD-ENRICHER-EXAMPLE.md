# Payload Enricher Integration - Multi-Template Example

## Overview

This demonstrates **extracting logic from PDFBox code** using **Payload Enrichers**, making business logic reusable across all template types (FreeMarker, AcroForm, PDFBox).

## What Was Changed

### Before: Logic Embedded in PDFBox Generator
```java
// CoverageSummaryGenerator.java - OLD
public PDDocument generate(Map<String, Object> payload) {
    // Extract applicants
    List<Map<String, Object>> applicants = payload.get("applicants");
    
    // Manual data extraction and formatting
    String name = demographic.get("firstName") + " " + demographic.get("lastName");
    String dob = demographic.get("dateOfBirth");
    
    // Draw to PDF - mixing business logic with rendering
    drawText(contentStream, "• " + name + " (" + relationship + ")", x, y);
    drawText(contentStream, "  Date of Birth: " + dob, x, y);
}
```

**Problems**:
- ❌ Business logic (name formatting, date parsing) mixed with rendering
- ❌ Not reusable by FreeMarker or AcroForm templates
- ❌ Hard to test logic independently
- ❌ Duplicated if other templates need same calculations

### After: Logic Extracted to Enricher
```java
// CoverageSummaryEnricher.java - NEW
@Component
public class CoverageSummaryEnricher implements PayloadEnricher {
    public Map<String, Object> enrich(Map<String, Object> payload) {
        // Extract and transform data
        int age = calculateAge(dateOfBirth);
        String displayName = firstName + " " + lastName;
        double totalPremium = aggregatePremiums(products);
        
        // Add enriched data to payload
        enriched.put("calculatedAge", age);
        enriched.put("displayName", displayName);
        enriched.put("totalApplicantPremium", totalPremium);
        
        return enriched;
    }
}
```

```java
// CoverageSummaryGenerator.java - SIMPLIFIED
public PDDocument generate(Map<String, Object> payload) {
    // Use pre-calculated enriched data
    Map<String, Object> coverageSummary = payload.get("coverageSummary");
    
    String displayName = coverageSummary.get("displayName");
    Integer age = coverageSummary.get("calculatedAge");
    
    // Pure rendering - no business logic
    drawText(contentStream, "• " + displayName + " (Age " + age + ")", x, y);
}
```

**Benefits**:
- ✅ Business logic separated from rendering
- ✅ Reusable by all template types
- ✅ Testable independently
- ✅ Single source of truth

## Configuration

**File**: `config-repo/multi-template.yml`

```yaml
pdfMerge:
  # Register the enricher
  enrichers:
    - coverageSummary  # References CoverageSummaryEnricher by name
  
  sections:
    - type: "freemarker"    # Can use enriched data
    - type: "acroform"      # Can use enriched data
    - type: "pdfbox"        # Uses enriched data (simplified logic)
```

## What the Enricher Does

### Input (Original Payload)
```json
{
  "applicants": [{
    "demographic": {
      "firstName": "John",
      "lastName": "Doe",
      "dateOfBirth": "1985-03-15"
    },
    "relationship": "PRIMARY",
    "products": [
      { "premium": "350.00" },
      { "premium": "100.00" }
    ]
  }],
  "effectiveDate": "2026-01-01"
}
```

### Output (Enriched Payload)
```json
{
  "applicants": [...],  // Original data preserved
  "effectiveDate": "2026-01-01",
  
  "coverageSummary": {  // NEW: Enriched data added
    "enrichedApplicants": [{
      "displayName": "John Doe",           // Formatted
      "calculatedAge": 39,                 // Calculated from DOB
      "displayRelationship": "Primary",    // Normalized
      "totalApplicantPremium": "450.00"   // Aggregated
    }],
    "formattedEffectiveDate": "January 1, 2026",  // Human-readable
    "applicantCount": 1,
    "totalBenefits": 12,
    "totalCarriers": 2,
    "daysUntilEffective": 14
  }
}
```

## Real-World Use Cases

### Use Case 1: Age Calculation
**Before**: Each template calculates age differently
```ftl
<!-- FreeMarker: Manual calculation -->
${.now?date?string("yyyy")?number - dateOfBirth?substring(0,4)?number}

// PDFBox: Manual calculation
int age = LocalDate.now().getYear() - Integer.parseInt(dob.substring(0, 4));
```

**After**: Enricher calculates once, all templates use same value
```ftl
<!-- FreeMarker -->
${coverageSummary.enrichedApplicants[0].calculatedAge}

// PDFBox
Integer age = (Integer) applicant.get("calculatedAge");
```

### Use Case 2: Date Formatting
**Before**: Inconsistent formats
- FreeMarker: "01/01/2026"
- PDFBox: "2026-01-01"
- AcroForm: "January 1, 2026"

**After**: Consistent format everywhere
```java
// Enricher
coverageSummary.put("formattedEffectiveDate", "January 1, 2026");

// All templates get same formatted date
```

### Use Case 3: Premium Aggregation
**Before**: Each template loops and sums
```java
// Duplicated in multiple places
double total = 0;
for (Map<String, Object> product : products) {
    total += Double.parseDouble((String) product.get("premium"));
}
```

**After**: Calculated once in enricher
```java
// Enricher does it once
enrichedApplicant.put("totalApplicantPremium", "450.00");

// All templates just display it
drawText("Total: $" + totalApplicantPremium);
```

## Testing the Enricher

### Unit Test Example
```java
@Test
public void testAgeCalculation() {
    CoverageSummaryEnricher enricher = new CoverageSummaryEnricher();
    
    Map<String, Object> payload = Map.of(
        "applicants", List.of(Map.of(
            "demographic", Map.of("dateOfBirth", "1985-03-15")
        ))
    );
    
    Map<String, Object> enriched = enricher.enrich(payload);
    Map<String, Object> summary = (Map) enriched.get("coverageSummary");
    List<Map> applicants = (List) summary.get("enrichedApplicants");
    
    assertEquals(39, applicants.get(0).get("calculatedAge"));
}
```

## Migration Guide

### Step 1: Create Enricher
```java
@Component
public class YourEnricher implements PayloadEnricher {
    @Override
    public String getName() { return "yourEnricher"; }
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        // Move logic from PDFBox generator here
        return enriched;
    }
}
```

### Step 2: Register in YAML
```yaml
pdfMerge:
  enrichers:
    - yourEnricher
```

### Step 3: Update PDFBox Generator
```java
// OLD: Direct payload access with logic
String name = demographic.get("firstName") + " " + demographic.get("lastName");

// NEW: Use enriched data
String name = coverageSummary.get("displayName");
```

### Step 4: Update Templates (Optional)
```ftl
<!-- OLD: Complex logic in template -->
${applicant.demographic.firstName} ${applicant.demographic.lastName}

<!-- NEW: Use enriched field -->
${coverageSummary.enrichedApplicants[0].displayName}
```

## Key Takeaways

1. **Separation of Concerns**: Business logic != Rendering logic
2. **DRY Principle**: Calculate once, use everywhere
3. **Testability**: Enrichers are pure functions, easy to test
4. **Flexibility**: Change logic in one place, affects all templates
5. **Performance**: Calculate expensive operations once, not per template

## Result

✅ **17KB PDF generated successfully** with:
- All 4 pages using consistent enriched data
- Age calculated from DOB (showing "Age 39" instead of raw DOB)
- Dates formatted ("January 1, 2026" instead of "2026-01-01")
- Premium totals pre-calculated
- Benefit/carrier counts available
- Consistent page sizes (US Letter)
- Headers, footers, and page numbers

**Command**: 
```bash
curl -X POST http://localhost:8080/api/pdf/merge \
  -H "Content-Type: application/json" \
  -d @test-multi-template-request.json \
  --output multi-template-with-enricher.pdf
```

See [MULTI-TEMPLATE-COMPLETE-EXAMPLE.md](MULTI-TEMPLATE-COMPLETE-EXAMPLE.md) for the full configuration.
