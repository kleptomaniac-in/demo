# Enrollment Integration with Multi-Template PDF Generation

## Overview

The multi-template PDF generation system is now integrated with enrollment requests, supporting **products**, **market category**, and **state** as parameters. This enables dynamic PDF generation based on enrollment context.

## Key Features

✅ **Product-Based Generation** - Medical, Dental, Vision, Life  
✅ **Market Category Aware** - Individual, Small Group, Large Group  
✅ **State-Specific Content** - CA disclosures, NY regulations, etc.  
✅ **Multi-Product Pricing** - Automatic aggregation per product type  
✅ **Conditional Sections** - Include/exclude pages based on context  
✅ **Enriched Headers/Footers** - Display market, state, products dynamically

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Enrollment Request Flow                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Enrollment Request                                       │
│     {                                                        │
│       enrollment: {                                          │
│         products: ["medical", "dental"],                     │
│         marketCategory: "individual",                        │
│         state: "CA"                                          │
│       },                                                     │
│       payload: { ... }                                       │
│     }                                                        │
│                  │                                           │
│                  ▼                                           │
│  2. EnrollmentContextEnricher                                │
│     - Extract product selections                             │
│     - Format market category                                 │
│     - Add state-specific flags                               │
│     - Aggregate pricing by product                           │
│                  │                                           │
│                  ▼                                           │
│  3. CoverageSummaryEnricher                                  │
│     - Calculate ages                                         │
│     - Format dates                                           │
│     - Create display names                                   │
│                  │                                           │
│                  ▼                                           │
│  4. Enriched Payload                                         │
│     {                                                        │
│       enrollmentContext: {                                   │
│         selectedProducts: ["medical", "dental"],             │
│         marketDisplay: "Individual & Family",                │
│         stateFullName: "California",                         │
│         hasMedical: true,                                    │
│         hasDental: true,                                     │
│         requiresCADisclosures: true                          │
│       },                                                     │
│       productSummary: {                                      │
│         medicalPremiumTotal: "450.00",                       │
│         dentalPremiumTotal: "45.00",                         │
│         grandTotalPremium: "495.00"                          │
│       },                                                     │
│       coverageSummary: { ... }                               │
│     }                                                        │
│                  │                                           │
│                  ▼                                           │
│  5. Multi-Template PDF Generation                            │
│     - FreeMarker templates (uses all enriched data)          │
│     - AcroForm (field mapping with enriched values)          │
│     - PDFBox (simplified using pre-calculated data)          │
│                  │                                           │
│                  ▼                                           │
│  6. Generated PDF                                            │
│     enrollment-test.pdf (13KB, 4 pages)                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Configuration

**File**: `config-repo/enrollment-multi-product.yml`

### Enrichers Section
```yaml
pdfMerge:
  enrichers:
    - enrollmentContext    # NEW: Extracts product/market/state context
    - coverageSummary      # Existing: Calculates coverage data
```

### Headers with Enrollment Context
```yaml
  header:
    enabled: true
    startPage: 2
    content:
      left:
        text: "{enrollmentContext.marketDisplay} Enrollment"  # "Individual & Family Enrollment"
      center:
        text: "{enrollmentContext.stateFullName} | Products: {enrollmentContext.productsDisplay}"  # "California | Products: medical, dental"
      right:
        text: "Application: {applicationNumber}"
```

### Footers with State Info
```yaml
  footer:
    content:
      left:
        text: "State: {enrollmentContext.state}"  # "State: CA"
      center:
        text: "Page {current} of {total}"
```

### Sections with AcroForm Field Mapping
```yaml
  sections:
    - name: "enrollment-form"
      type: "acroform"
      template: "templates/enrollment-form-base.pdf"
      fieldMapping:
        "ApplicationNumber": "applicationNumber"
        "TotalPremium": "productSummary.grandTotalPremium"  # Uses enriched total
```

## Request Format

**Endpoint**: `POST /api/document/generate`

**Request Body**:
```json
{
  "configName": "enrollment-multi-product",
  "payload": {
    "enrollment": {
      "products": ["medical", "dental"],
      "marketCategory": "individual",
      "state": "CA",
      "plansByProduct": {
        "medical": ["GOLD_PPO"],
        "dental": ["PREMIUM_DENTAL"]
      }
    },
    "applicationNumber": "APP-2025-CA-00789",
    "effectiveDate": "2026-01-01",
    "applicants": [
      {
        "relationship": "PRIMARY",
        "demographic": {
          "firstName": "Sarah",
          "lastName": "Johnson",
          "dateOfBirth": "1987-06-15"
        },
        "products": [
          {
            "productType": "MEDICAL",
            "planName": "Gold PPO",
            "premium": "450.00"
          },
          {
            "productType": "DENTAL",
            "planName": "Premium Dental",
            "premium": "45.00"
          }
        ]
      }
    ],
    "members": [
      {
        "memberId": "M12345",
        "name": "Sarah Johnson",
        "medical": {
          "planName": "Gold PPO",
          "premium": 450.00
        },
        "dental": {
          "planName": "Premium Dental",
          "premium": 45.00
        }
      }
    ]
  }
}
```

## Enriched Data Output

### EnrollmentContextEnricher Adds:
```javascript
enrollmentContext: {
  selectedProducts: ["medical", "dental"],
  hasMultipleProducts: true,
  productCount: 2,
  hasMedical: true,
  hasDental: true,
  hasVision: false,
  productsDisplay: "medical, dental",
  
  marketCategory: "individual",
  marketDisplay: "Individual & Family",
  isIndividual: true,
  isSmallGroup: false,
  isLargeGroup: false,
  
  state: "CA",
  stateFullName: "California",
  requiresCADisclosures: true,
  requiresNYRegulations: false,
  
  totalPlansSelected: 2
}
```

### Product Pricing Aggregation:
```javascript
productSummary: {
  medicalPremiumTotal: "450.00",
  medicalMemberCount: 1,
  dentalPremiumTotal: "45.00",
  dentalMemberCount: 1,
  grandTotalPremium: "495.00"
}
```

## Use Cases

### Use Case 1: Individual Market, Single Product (Medical Only)
```json
{
  "enrollment": {
    "products": ["medical"],
    "marketCategory": "individual",
    "state": "NY"
  }
}
```
**Result**:
- Header: "Individual & Family Enrollment"
- Footer: "State: NY"
- `hasMedical = true`, `hasDental = false`
- No dental sections included

### Use Case 2: Small Group, Multi-Product
```json
{
  "enrollment": {
    "products": ["medical", "dental", "vision"],
    "marketCategory": "small-group",
    "state": "TX"
  }
}
```
**Result**:
- Header: "Small Group (2-50) Enrollment"
- Products Display: "medical, dental, vision"
- All product totals calculated separately

### Use Case 3: California with State Disclosures
```json
{
  "enrollment": {
    "products": ["medical"],
    "marketCategory": "individual",
    "state": "CA"
  }
}
```
**Result**:
- `requiresCADisclosures = true`
- Can conditionally include CA DMHC disclosure pages
- Headers show "California" full name

## Benefits

### 1. **Dynamic Content Based on Context**
- Headers/footers show relevant market and state information
- Product-specific pages only appear when selected
- State compliance documents automatically included

### 2. **Simplified Template Logic**
```ftl
<!-- FreeMarker Template -->
<#if enrollmentContext.hasMedical>
  <h2>Medical Coverage</h2>
  Premium: $${productSummary.medicalPremiumTotal}
</#if>

<#if enrollmentContext.requiresCADisclosures>
  <p>California-specific disclosure text...</p>
</#if>
```

### 3. **Reusable Across Markets**
Same configuration works for:
- Individual & Family plans
- Small Group (2-50 employees)
- Large Group (51+ employees)

Just change `marketCategory` parameter!

### 4. **State Compliance Made Easy**
```java
// Enricher automatically sets flags
enrollmentContext.put("requiresCADisclosures", "CA".equalsIgnoreCase(state));
enrollmentContext.put("requiresNYRegulations", "NY".equalsIgnoreCase(state));
```

### 5. **Multi-Product Pricing**
Automatic aggregation per product type:
```
Medical: $450.00 (1 member)
Dental:  $45.00 (1 member)
Total:   $495.00
```

## Testing

**Test Command**:
```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-enrollment-multi-product-request.json \
  --output enrollment-test.pdf
```

**Expected Result**:
```
✅ 13KB PDF, 4 pages
Page 1: Enrollment Cover (FreeMarker)
Page 2: Application Form (AcroForm with enriched field values)
Page 3: Coverage Summary (PDFBox using pre-calculated data)
Page 4: Terms & Conditions (FreeMarker)

Headers show: "Individual & Family Enrollment | California | Products: medical, dental"
Footers show: "State: CA | Page X of 4"
```

## Comparison: Before vs. After

### Before (Without Enrichers)
```java
// PDFBox Generator - Hard-coded logic
if (payload.containsKey("products")) {
    List<String> products = (List<String>) payload.get("products");
    if (products.contains("medical")) {
        drawText("Medical Coverage Included");
    }
}

// FreeMarker Template - Complex logic
<#if payload.enrollment.products??>
  <#list payload.enrollment.products as product>
    <#if product == "medical">
      Medical Selected
    </#if>
  </#list>
</#if>
```

### After (With EnrollmentContextEnricher)
```java
// PDFBox Generator - Clean
Boolean hasMedical = (Boolean) enriched.get("enrollmentContext").get("hasMedical");
if (hasMedical) {
    drawText("Medical Coverage Included");
}

// FreeMarker Template - Simple
<#if enrollmentContext.hasMedical>
  Medical Selected
</#if>
```

## Integration with Existing Enrollment System

The multi-template system can be used directly with your existing `EnrollmentPdfController`:

```java
@PostMapping("/api/enrollment/generate")
public ResponseEntity<byte[]> generateEnrollmentPdf(@RequestBody EnrollmentPdfRequest request) {
    // The enrollment structure is already compatible!
    Map<String, Object> payload = new HashMap<>();
    payload.put("enrollment", request.getEnrollment());  // Contains products, market, state
    payload.put("applicants", request.getApplicants());
    payload.put("members", request.getMembers());
    
    // Use multi-template merge with enrollment-specific config
    byte[] pdfBytes = pdfMergeService.generateMergedPdf("enrollment-multi-product", payload);
    
    return ResponseEntity.ok().body(pdfBytes);
}
```

## Summary

✅ **Products** - medical, dental, vision extracted and available as flags  
✅ **Market Category** - formatted display names, conditional logic  
✅ **State** - full names, compliance flags, state-specific sections  
✅ **Multi-Product Pricing** - automatic aggregation per product type  
✅ **Enriched Headers/Footers** - dynamic content from enrollment context  
✅ **Simplified Templates** - business logic extracted to enrichers  
✅ **Reusable Configuration** - works across all market types and states

**Generated PDF**: 13KB, 4 pages, all enrollment context applied!

See also:
- [MULTI-TEMPLATE-COMPLETE-EXAMPLE.md](MULTI-TEMPLATE-COMPLETE-EXAMPLE.md) - Full multi-template guide
- [PAYLOAD-ENRICHER-EXAMPLE.md](PAYLOAD-ENRICHER-EXAMPLE.md) - Payload enricher patterns
- [HEADERS_FOOTERS.md](/workspaces/demo/demoproject/config-repo/HEADERS_FOOTERS.md) - Headers/footers configuration
