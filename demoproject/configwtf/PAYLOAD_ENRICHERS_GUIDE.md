# Extracting Logic from PDFBox Code - Guide

## Problem

You have existing PDFBox-based code with complex business logic (calculations, formatting, data transformations) and you want to:
1. **Extract the logic/data** from PDFBox code
2. **Reuse that logic** with FreeMarker templates
3. **Avoid duplicating** the logic in templates

## Solution: Payload Enrichers

**Payload Enrichers** are Spring components that transform/enrich the payload **before** it reaches the template.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Generation Flow                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Original Payload                                         │
│     { products, applicants, ... }                            │
│                  │                                           │
│                  ▼                                           │
│  2. PayloadEnricher(s)                                       │
│     - Extract data from existing PDFBox logic                │
│     - Perform calculations                                   │
│     - Format dates/numbers                                   │
│     - Add derived fields                                     │
│                  │                                           │
│                  ▼                                           │
│  3. Enriched Payload                                         │
│     { products, applicants, premiumCalculations,             │
│       formattedDates, calculatedAges, ... }                  │
│                  │                                           │
│                  ▼                                           │
│  4. FreeMarker Template                                      │
│     Uses enriched data to render HTML                        │
│                  │                                           │
│                  ▼                                           │
│  5. PDF Output                                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Steps

### Step 1: Create PayloadEnricher

```java
package com.example.service.enrichers;

import com.example.service.PayloadEnricher;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PremiumCalculationEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // YOUR EXISTING PDFBOX LOGIC HERE
        // Extract it from PdfBoxGenerator.generate() method
        BigDecimal total = calculatePremiumTotal(payload);
        BigDecimal discount = calculateDiscount(payload);
        
        // Add calculated fields
        Map<String, Object> calculations = new HashMap<>();
        calculations.put("total", total);
        calculations.put("discount", discount);
        calculations.put("final", total.subtract(discount));
        
        enriched.put("premiumCalculations", calculations);
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "premiumCalculation";  // Used in YAML config
    }
    
    // Copy your existing PDFBox calculation methods here
    private BigDecimal calculatePremiumTotal(Map<String, Object> payload) {
        // Your existing logic
    }
    
    private BigDecimal calculateDiscount(Map<String, Object> payload) {
        // Your existing logic
    }
}
```

### Step 2: Configure in YAML

```yaml
pdfMerge:
  sections:
    - name: enrollment-summary
      type: freemarker
      template: enrollment-summary.ftl
      enabled: true
      payloadEnrichers:
        - premiumCalculation    # Name from getName()
        - dateFormatting        # Can apply multiple enrichers
```

### Step 3: Use Enriched Data in FreeMarker Template

```html
<!-- enrollment-summary.ftl -->
<html>
<body>
  <h1>Premium Summary</h1>
  
  <!-- Access enriched data -->
  <p>Monthly Total: $${payload.premiumCalculations.total}</p>
  <p>Discount: $${payload.premiumCalculations.discount}</p>
  <p>Final Premium: $${payload.premiumCalculations.final}</p>
  
  <!-- Original payload still available -->
  <p>Primary Name: ${payload.primary.firstName} ${payload.primary.lastName}</p>
</body>
</html>
```

---

## Example: Extracting Logic from PDFBox Generator

### Before: PDFBox Generator with Business Logic

```java
@Component
public class InvoicePdfBoxGenerator implements PdfBoxGenerator {
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        
        // COMPLEX CALCULATION LOGIC
        BigDecimal subtotal = calculateSubtotal(payload);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.08"));
        BigDecimal total = subtotal.add(tax);
        
        // RENDER WITH PDFBOX
        PDPageContentStream content = new PDPageContentStream(doc, page);
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 12);
        content.newLineAtOffset(50, 700);
        content.showText("Subtotal: $" + subtotal);
        content.newLineAtOffset(0, -20);
        content.showText("Tax: $" + tax);
        content.newLineAtOffset(0, -20);
        content.showText("Total: $" + total);
        content.endText();
        content.close();
        
        return doc;
    }
    
    private BigDecimal calculateSubtotal(Map<String, Object> payload) {
        // Complex logic here
    }
}
```

### After: Extract Logic → Enricher + FreeMarker

**1. Create Enricher (Extract Calculation Logic):**

```java
@Component
public class InvoiceCalculationEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // REUSE THE SAME CALCULATION LOGIC
        BigDecimal subtotal = calculateSubtotal(payload);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.08"));
        BigDecimal total = subtotal.add(tax);
        
        // Add to payload
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("subtotal", subtotal.doubleValue());
        invoice.put("tax", tax.doubleValue());
        invoice.put("total", total.doubleValue());
        
        enriched.put("invoiceCalculations", invoice);
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "invoiceCalculation";
    }
    
    // Same calculation method from PDFBox generator
    private BigDecimal calculateSubtotal(Map<String, Object> payload) {
        // Complex logic here (copied from PDFBox generator)
    }
}
```

**2. FreeMarker Template (Render with HTML):**

```html
<!-- invoice.ftl -->
<html>
<head>
  <style>
    .invoice-line { margin: 10px 0; }
  </style>
</head>
<body>
  <h1>Invoice</h1>
  
  <div class="invoice-line">
    Subtotal: $${payload.invoiceCalculations.subtotal?string["0.00"]}
  </div>
  <div class="invoice-line">
    Tax (8%): $${payload.invoiceCalculations.tax?string["0.00"]}
  </div>
  <div class="invoice-line">
    <strong>Total: $${payload.invoiceCalculations.total?string["0.00"]}</strong>
  </div>
</body>
</html>
```

**3. YAML Configuration:**

```yaml
pdfMerge:
  sections:
    - name: invoice
      type: freemarker
      template: invoice.ftl
      payloadEnrichers:
        - invoiceCalculation
```

---

## Multiple Enrichers

You can chain multiple enrichers:

```yaml
sections:
  - name: complete-enrollment
    type: freemarker
    template: enrollment-complete.ftl
    payloadEnrichers:
      - dateFormatting         # Applied first
      - premiumCalculation     # Applied second
      - invoiceCalculation     # Applied third
```

**Processing Order:**
1. Original payload → dateFormatting → payload with formatted dates
2. Payload with formatted dates → premiumCalculation → payload + premium data
3. Payload + premium data → invoiceCalculation → fully enriched payload
4. Fully enriched payload → FreeMarker template

---

## Built-in Example Enrichers

### 1. PremiumCalculationEnricher

**Purpose:** Calculate premium totals, discounts, savings

**Adds to payload:**
```json
{
  "premiumCalculations": {
    "monthlyTotal": 650.00,
    "annualTotal": 7800.00,
    "discount": 65.00,
    "finalMonthly": 585.00,
    "finalAnnual": 7020.00,
    "savingsPercent": 10.0
  }
}
```

**Usage in template:**
```html
<p>Your monthly premium: $${payload.premiumCalculations.finalMonthly}</p>
<p>You save ${payload.premiumCalculations.savingsPercent}%!</p>
```

### 2. DateFormattingEnricher

**Purpose:** Format dates and calculate ages

**Adds to payload:**
```json
{
  "formattedDates": {
    "submittedDateLong": "December 15, 2025",
    "submittedDateShort": "12/15/2025",
    "effectiveDateLong": "January 1, 2026"
  },
  "primary": {
    "calculatedAge": 45,
    "ageCategory": "ADULT"
  }
}
```

**Usage in template:**
```html
<p>Application submitted on ${payload.formattedDates.submittedDateLong}</p>
<p>${payload.primary.firstName} is ${payload.primary.calculatedAge} years old</p>
```

---

## Common Patterns

### Pattern 1: Extract Calculation Logic

**Use when:** You have complex calculations in PDFBox code

**Steps:**
1. Copy calculation methods from PDFBox generator
2. Create enricher that calls those methods
3. Add calculated values to payload
4. Use in FreeMarker template

### Pattern 2: Extract Formatting Logic

**Use when:** You have date/number formatting in PDFBox code

**Steps:**
1. Create enricher with formatting methods
2. Add formatted values to payload
3. Use formatted values in template

### Pattern 3: Extract Data Transformation

**Use when:** You transform/aggregate data in PDFBox code

**Steps:**
1. Create enricher that performs transformations
2. Add transformed data to payload
3. Use simplified data in template

---

## Benefits

### ✅ Advantages
1. **Reuse existing logic** - Don't rewrite calculations
2. **Separation of concerns** - Logic in Java, presentation in HTML
3. **Testable** - Unit test enrichers separately
4. **Maintainable** - Change logic in one place
5. **Flexible** - Mix and match enrichers per section
6. **Gradual migration** - Keep PDFBox generators during transition

### ⚠️ When NOT to Use
- Simple templates with no logic
- Logic already in preprocessing rules
- One-time calculations that can be in template

---

## Migration Strategy

### Phase 1: Identify Reusable Logic
- Review PDFBox generators
- Identify calculation/formatting methods
- List data transformations

### Phase 2: Create Enrichers
- Extract one logical unit per enricher
- Keep enrichers focused (single responsibility)
- Add comprehensive tests

### Phase 3: Create FreeMarker Templates
- Design HTML layout
- Use enriched data from payload
- Test with sample data

### Phase 4: Update Configurations
- Add `payloadEnrichers` to section configs
- Test end-to-end
- Compare with PDFBox output

### Phase 5: Deprecate PDFBox Generators (Optional)
- Keep for backward compatibility
- Or remove once fully migrated

---

## Testing

### Unit Test Enricher

```java
@Test
public void testPremiumCalculationEnricher() {
    PremiumCalculationEnricher enricher = new PremiumCalculationEnricher();
    
    Map<String, Object> payload = new HashMap<>();
    payload.put("medical", Map.of("monthlyPremium", 650.00));
    payload.put("dental", Map.of("monthlyPremium", 45.00));
    payload.put("vision", Map.of("monthlyPremium", 15.00));
    
    Map<String, Object> enriched = enricher.enrich(payload);
    
    Map<String, Object> calculations = 
        (Map<String, Object>) enriched.get("premiumCalculations");
    
    assertEquals(710.00, calculations.get("monthlyTotal"));
    assertEquals(71.00, calculations.get("discount")); // 10% for 3 products
    assertEquals(639.00, calculations.get("finalMonthly"));
}
```

---

## Summary

**Payload Enrichers provide a clean way to:**
- ✅ Extract logic from PDFBox code
- ✅ Reuse that logic with FreeMarker templates  
- ✅ Avoid duplication
- ✅ Maintain separation of concerns
- ✅ Enable gradual migration from PDFBox to FreeMarker

**Pattern:**
```
PDFBox Logic → Extract → PayloadEnricher → Enrich Payload → FreeMarker Template
```

**Result:** Best of both worlds - Java logic + HTML rendering!
