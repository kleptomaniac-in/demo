Let me check how payload enrichers work with AcroForm sections: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/FlexiblePdfMergeService.java#180-180), lines 180 to 220

Perfect! Yes, **payload enrichers ARE used with AcroForm sections too**. Let me provide a detailed explanation:

## **✅ YES - Payload Enrichers Work with AcroForm Templates!**

### **How It Works**

From the code (lines 154-207), enrichers are applied **before ANY section type** is processed:

```java
private PDDocument generateSectionPdf(SectionConfig section, Map<String, Object> payload) {
    // Apply payload enrichers if specified
    Map<String, Object> enrichedPayload = payload;
    if (section.getPayloadEnrichers() != null && !section.getPayloadEnrichers().isEmpty()) {
        enrichedPayload = payloadEnricherRegistry.applyEnrichers(
            section.getPayloadEnrichers(), 
            payload
        );
    }
    
    if ("freemarker".equals(section.getType())) {
        // Uses enrichedPayload
    } else if ("pdfbox".equals(section.getType())) {
        // Uses enrichedPayload
    } else if ("acroform".equals(section.getType())) {
        // ✅ ALSO uses enrichedPayload!
        byte[] filledPdf = acroFormFillService.fillAcroForm(
            section.getTemplate(), 
            allFieldMappings, 
            enrichedPayload  // ← Enriched payload passed here
        );
    }
}
```

---

## **📋 Why Use Enrichers with AcroForm?**

### **Use Case 1: Calculated Fields**

**Without Enricher:**
```yaml
sections:
  - name: enrollment-form
    type: acroform
    template: enrollment.pdf
    fieldMapping:
      # ❌ PROBLEM: Can't calculate in field mapping
      "total_premium": "???"  # How to sum multiple premiums?
      "applicant_age": "???"  # How to calculate from DOB?
```

**With Enricher:**
```yaml
sections:
  - name: enrollment-form
    type: acroform
    template: enrollment.pdf
    payloadEnrichers:
      - premiumCalculation  # ← Adds calculated totals
      - coverageSummary     # ← Adds calculated ages
    fieldMapping:
      "total_premium": "premiumCalculations.finalMonthly"     # ✅ Use enriched data
      "applicant_age": "coverageSummary.enrichedApplicants[0].calculatedAge"  # ✅ Use enriched data
```

### **Use Case 2: Formatted Values**

**Enricher:**
```java
@Component
public class DateFormattingEnricher implements PayloadEnricher {
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Format dates for AcroForm display
        String effectiveDate = (String) payload.get("effectiveDate");
        enriched.put("formattedEffectiveDate", formatDate(effectiveDate, "MM/dd/yyyy"));
        enriched.put("formattedEffectiveDateLong", formatDate(effectiveDate, "MMMM dd, yyyy"));
        
        return enriched;
    }
}
```

**YAML Config:**
```yaml
sections:
  - name: enrollment-acroform
    type: acroform
    template: enrollment.pdf
    payloadEnrichers:
      - dateFormatting
    fieldMapping:
      "effective_date": "formattedEffectiveDate"          # → "01/01/2026"
      "effective_date_long": "formattedEffectiveDateLong" # → "January 01, 2026"
```

### **Use Case 3: Conditional/Derived Values**

**Enricher:**
```java
@Component
public class EligibilityEnricher implements PayloadEnricher {
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Business logic for eligibility determination
        boolean isEligible = determineEligibility(payload);
        String eligibilityStatus = isEligible ? "APPROVED" : "PENDING REVIEW";
        
        enriched.put("isEligible", isEligible);
        enriched.put("eligibilityStatus", eligibilityStatus);
        
        return enriched;
    }
}
```

**YAML Config:**
```yaml
sections:
  - name: enrollment-form
    type: acroform
    template: enrollment.pdf
    payloadEnrichers:
      - eligibilityEnricher
    fieldMapping:
      "eligibility_status": "eligibilityStatus"  # → "APPROVED" or "PENDING REVIEW"
      "approval_indicator": "isEligible"         # → true/false
```

---

## **🔄 Complete Flow with AcroForm**

### **Example Configuration**

```yaml
# dental-individual-ca.yml
sections:
  - name: enrollment-application
    type: acroform
    template: templates/enrollment-form.pdf
    payloadEnrichers:
      - enrollmentContext      # Adds product flags, market info
      - premiumCalculation     # Calculates totals and discounts
      - coverageSummary        # Calculates ages, formats names
    fieldMapping:
      # Primary Applicant
      "primary_name": "coverageSummary.enrichedApplicants[0].displayName"
      "primary_age": "coverageSummary.enrichedApplicants[0].calculatedAge"
      "primary_dob": "coverageSummary.enrichedApplicants[0].formattedDOB"
      
      # Premium Information
      "monthly_premium": "premiumCalculations.finalMonthly"
      "annual_premium": "premiumCalculations.finalAnnual"
      "discount_amount": "premiumCalculations.discount"
      "savings_percent": "premiumCalculations.savingsPercent"
      
      # Product Selections
      "has_medical": "enrollmentContext.hasMedical"
      "has_dental": "enrollmentContext.hasDental"
      "has_vision": "enrollmentContext.hasVision"
      "product_list": "enrollmentContext.productList"
      
      # Market Information
      "market_type": "enrollmentContext.marketDisplay"
      "state_name": "enrollmentContext.stateName"
```

### **Execution Flow**

```
1. Original Payload
   { applicants: [...], enrollment: {...} }
        ↓
2. Preprocessing (if needed)
   Flattens complex structures
        ↓
3. Section Processing (AcroForm)
        ↓
   3a. Apply Enrichers Sequentially:
       ├─ enrollmentContext → Adds product flags
       ├─ premiumCalculation → Adds calculated totals
       └─ coverageSummary → Adds formatted data
        ↓
   3b. Enriched Payload Created:
       {
         applicants: [...],           // Original
         enrollment: {...},            // Original
         enrollmentContext: {...},     // NEW: from enricher
         premiumCalculations: {...},   // NEW: from enricher
         coverageSummary: {...}        // NEW: from enricher
       }
        ↓
   3c. AcroFormFillService Uses Enriched Payload:
       - Resolves "premiumCalculations.finalMonthly"
       - Resolves "coverageSummary.enrichedApplicants[0].calculatedAge"
       - Resolves "enrollmentContext.hasMedical"
        ↓
4. Filled AcroForm PDF Generated
```

---

## **💡 Real-World Example**

### **Enricher Implementation**

```java
@Component
public class EnrollmentAcroFormEnricher implements PayloadEnricher {
    
    @Override
    public String getName() {
        return "enrollmentAcroForm";
    }
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        Map<String, Object> acroformData = new HashMap<>();
        
        // Calculate age from DOB
        List<Map<String, Object>> applicants = 
            (List<Map<String, Object>>) payload.get("applicants");
        
        if (applicants != null && !applicants.isEmpty()) {
            Map<String, Object> primary = applicants.stream()
                .filter(a -> "PRIMARY".equals(a.get("relationshipType")))
                .findFirst()
                .orElse(null);
            
            if (primary != null) {
                Map<String, Object> demographic = 
                    (Map<String, Object>) primary.get("demographic");
                String dob = (String) demographic.get("dateOfBirth");
                
                acroformData.put("primaryAge", calculateAge(dob));
                acroformData.put("primaryFullName", 
                    demographic.get("firstName") + " " + demographic.get("lastName"));
            }
        }
        
        // Calculate premium total
        List<Map<String, Object>> coverages = 
            (List<Map<String, Object>>) payload.get("coverages");
        
        if (coverages != null) {
            double total = coverages.stream()
                .mapToDouble(c -> Double.parseDouble(c.get("premium").toString()))
                .sum();
            
            acroformData.put("totalPremium", String.format("%.2f", total));
        }
        
        // Add checkbox values
        acroformData.put("acceptsTerms", "Yes");
        acroformData.put("electronicDelivery", "Yes");
        
        enriched.put("acroformData", acroformData);
        return enriched;
    }
    
    private int calculateAge(String dob) {
        // Age calculation logic
        return 45; // Simplified
    }
}
```

### **YAML Configuration**

```yaml
sections:
  - name: enrollment-form
    type: acroform
    template: templates/enrollment-application.pdf
    payloadEnrichers:
      - enrollmentAcroForm  # ← Custom enricher for AcroForm
    fieldMapping:
      # Use enriched data
      "Applicant_Name": "acroformData.primaryFullName"
      "Applicant_Age": "acroformData.primaryAge"
      "Total_Premium": "acroformData.totalPremium"
      "Accept_Terms_Checkbox": "acroformData.acceptsTerms"
      "Electronic_Delivery_Checkbox": "acroformData.electronicDelivery"
      
      # Can still use original payload
      "Effective_Date": "enrollment.effectiveDate"
      "Group_Number": "enrollment.groupNumber"
```

---

## **🎯 Advantages for AcroForm**

| Benefit | Description |
|---------|-------------|
| **Calculated Fields** | Perform calculations once in enricher, use in multiple fields |
| **Formatted Values** | Format dates, currency, phone numbers before field filling |
| **Business Logic** | Eligibility, conditional values determined in Java (testable) |
| **Reusability** | Same enricher can be used for FreeMarker summary page + AcroForm |
| **Cleaner Configs** | Field mappings reference simple paths to enriched data |
| **Single Source of Truth** | One calculation shared across all section types |

---

## **⚖️ When to Use with AcroForm**

### **✅ Use Enrichers for AcroForm When:**

1. **Calculations Needed**
   - Totals, subtotals, percentages
   - Age from date of birth
   - Duration calculations

2. **Data Transformations**
   - Formatting dates/currency/phone
   - Normalizing values (state codes → names)
   - Concatenating fields (first + last name)

3. **Conditional Logic**
   - Eligibility determinations
   - Status calculations
   - Boolean flags for checkboxes

4. **Shared with Other Sections**
   - Same calculation used in FreeMarker summary AND AcroForm
   - Consistent data across multiple templates

### **❌ Use Direct Mapping When:**

1. **Simple Field Access** - Direct path to existing field
2. **No Transformation Needed** - Value is already in correct format
3. **Static Values** - Hardcoded strings/constants

---

## **📊 Comparison: With vs Without Enrichers**

### **Without Enricher (Limited)**

```yaml
fieldMapping:
  "primary_first": "applicants[demographic.relationshipType=PRIMARY].firstName"
  "primary_last": "applicants[demographic.relationshipType=PRIMARY].lastName"
  "primary_dob": "applicants[demographic.relationshipType=PRIMARY].demographic.dateOfBirth"
  # ❌ Can't calculate age in field mapping
  # ❌ Can't format "John Doe" from separate fields
  # ❌ Can't sum premiums
```

### **With Enricher (Powerful)**

```yaml
payloadEnrichers:
  - coverageSummary  # Calculates age, formats name, sums premiums

fieldMapping:
  "primary_name": "coverageSummary.enrichedApplicants[0].displayName"  # ✅ "John Doe"
  "primary_age": "coverageSummary.enrichedApplicants[0].calculatedAge"  # ✅ 45
  "primary_dob": "coverageSummary.enrichedApplicants[0].formattedDOB"   # ✅ "03/15/1985"
  "total_premium": "coverageSummary.totalMonthlyPremium"                # ✅ 650.00
```

---

## **📝 Summary**

**YES, Payload Enrichers work with AcroForm sections!**

**Key Points:**
- ✅ Enrichers run **before** AcroForm field filling
- ✅ Enriched data available to field mappings via paths
- ✅ Same enricher can be shared across FreeMarker, PDFBox, AND AcroForm sections
- ✅ Perfect for calculations, formatting, and business logic that AcroForm field mappings can't do alone
- ✅ Keeps business logic in testable Java code, not buried in configs

**Pattern:**
```
Preprocessing → Enrichers (calculate/format) → AcroForm Field Mapping (map enriched data to fields) → PDF
```

This gives AcroForm sections the same power as FreeMarker templates - **separation of business logic from presentation**! 🚀