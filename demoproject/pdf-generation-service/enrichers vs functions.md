Let me check the function expressions feature: 

Searched codebase for "FunctionExpressionResolver function expression field mapping formatDate uppercase mask", 10 results

Summarized conversation historyPerfect! Now let me read the full guide: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/FIELD-TRANSFORMATION-FUNCTIONS.md#1-1), lines 1 to 100

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/FIELD-TRANSFORMATION-FUNCTIONS.md#200-200), lines 200 to 340

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/FIELD-TRANSFORMATION-FUNCTIONS.md#400-400), lines 400 to 500

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/FIELD-TRANSFORMATION-FUNCTIONS.md#580-580), lines 580 to 682

Perfect! Now let me create a comprehensive comparison guide:

## When to Use Functions vs Enrichers

Both field transformation **functions** and **payload enrichers** transform data, but they serve different purposes and operate at different levels:

### **Quick Decision Guide**

| Use **Functions** when: | Use **Enrichers** when: |
|------------------------|-------------------------|
| Single field transformation | Cross-field calculations |
| Simple formatting (dates, currency) | Complex business logic |
| Masking/security (SSN, email, phone) | Data aggregation across arrays |
| String manipulation (concat, case) | Deriving new data structures |
| Per-field basis | Logic reused across multiple fields |
| Template-specific formatting | Section/document-level transformations |

---

### **Key Differences**

| Aspect | Functions | Enrichers |
|--------|-----------|-----------|
| **Scope** | Single field mapping | Entire payload |
| **Timing** | During field resolution (last step) | Before section generation (early step) |
| **Syntax** | `#{functionName(args)}` | YAML section: `payloadEnrichers: [...]` |
| **Reusability** | Per field mapping | Per section, shared across all fields |
| **Complexity** | Simple transformations | Complex calculations & business logic |
| **Performance** | Called per field | Called once per section |
| **Implementation** | Built-in + custom functions | Spring @Component classes |
| **Access to Payload** | Limited (args + field references) | Full payload context |
| **Output** | Single string value | Enriched payload (new fields/structures) |

---

### **Execution Order**

```
1. Preprocessing (flatten complex structures)
2. Product Collection (extract product types)
3. Config Selection (choose YAML config)
4. ✅ Enrichers Applied (calculate/transform payload)
   ↓
5. Section Generation:
   - FreeMarker: Uses enriched payload
   - PDFBox: Uses enriched payload
   - AcroForm: 
     a. Field mappings resolved
     b. ✅ Functions applied to individual field values
     c. Final values set in PDF
```

**Key Insight:** Enrichers run first (step 4), functions run last (step 5b)

---

### **Use Case Examples**

#### ✅ **Use Functions For:**

**1. Formatting existing values**
```yaml
fieldMappings:
  "DOB": "#{formatDate(dateOfBirth, 'MM/dd/yyyy')}"
  "Premium": "#{formatCurrency(monthlyPremium)}"
  "StateCode": "#{uppercase(state)}"
```

**2. Masking sensitive data**
```yaml
fieldMappings:
  "SSN": "#{mask(ssn, 'XXX-XX-', 4)}"  # 123-45-6789 → XXX-XX-6789
  "Email": "#{maskEmail(email)}"       # john@example.com → j***@example.com
  "Phone": "#{maskPhone(phone)}"       # 555-123-4567 → XXX-XXX-4567
```

**3. Simple string operations**
```yaml
fieldMappings:
  "FullName": "#{concat(firstName, ' ', lastName)}"
  "CleanEmail": "#{lowercase(#{trim(email)})}"
  "DisplayName": "#{capitalize(fullName)}"
```

**4. Providing defaults**
```yaml
fieldMappings:
  "MiddleName": "#{default(middleName, 'N/A')}"
  "PreferredContact": "#{coalesce(mobilePhone, homePhone, '555-0000')}"
```

---

#### ✅ **Use Enrichers For:**

**1. Calculating ages from dates**
```java
// CoverageSummaryEnricher.java
applicant.put("calculatedAge", calculateAge(dob));
```

```yaml
# YAML config
sections:
  - type: ACROFORM
    payloadEnrichers: [coverageSummary]
    fieldMappings:
      # Use pre-calculated age instead of function
      "Age": "coverageSummary.enrichedApplicants[0].calculatedAge"
```

**2. Aggregating premiums across products**
```java
// PremiumCalculationEnricher.java
calculations.put("grandTotalMonthly", 
    medicalTotal + dentalTotal + visionTotal + discounts);
```

```yaml
sections:
  - type: ACROFORM
    payloadEnrichers: [premiumCalculation]
    fieldMappings:
      "TotalPremium": "premiumCalculations.grandTotalMonthly"
      "MedicalTotal": "premiumCalculations.medicalTotalMonthly"
```

**3. Creating derived data structures**
```java
// EnrollmentContextEnricher.java
enriched.put("primaryApplicant", 
    applicants.stream()
        .filter(a -> "PRIMARY".equals(a.get("relationship")))
        .findFirst()
        .orElse(null));
```

```yaml
sections:
  - type: ACROFORM
    payloadEnrichers: [enrollmentContext]
    fieldMappings:
      "PrimaryName": "primaryApplicant.fullName"
      "PrimarySSN": "#{mask(primaryApplicant.ssn, 'XXX-XX-', 4)}"
      # ↑ Notice: Enricher provides primaryApplicant, function masks it
```

**4. Complex business rules**
```java
// CustomDiscountEnricher.java
if (familySize > 3 && hasAllProducts) {
    discount = basePremium * 0.15; // 15% family discount
} else if (isAutoPayEnabled) {
    discount = basePremium * 0.05; // 5% autopay discount
}
enriched.put("appliedDiscount", discount);
```

---

### **Combining Functions + Enrichers**

**Most Powerful Pattern:** Enrichers calculate, functions format

```yaml
sections:
  - type: ACROFORM
    templatePath: "enrollment-form.pdf"
    
    # Step 1: Enrichers calculate complex data
    payloadEnrichers:
      - coverageSummary      # Calculates ages
      - premiumCalculation   # Aggregates premiums
      - enrollmentContext    # Extracts primary applicant
    
    # Step 2: Functions format enriched data
    fieldMappings:
      # Enricher calculated age, function formats display
      "PrimaryAge": "#{concat(coverageSummary.enrichedApplicants[0].calculatedAge, ' years')}"
      
      # Enricher aggregated premium, function formats currency
      "TotalPremium": "#{formatCurrency(premiumCalculations.grandTotalMonthly)}"
      
      # Enricher extracted primary, function masks SSN
      "PrimarySSN": "#{mask(primaryApplicant.ssn, 'XXX-XX-', 4)}"
      
      # Enricher provides data, function formats name
      "PrimaryName": "#{uppercase(#{concat(primaryApplicant.firstName, ' ', primaryApplicant.lastName)})}"
```

**Why this works:**
1. Enricher runs once → Calculates age for all applicants (expensive operation)
2. Function runs per field → Just formats the pre-calculated value (cheap operation)
3. Result: Better performance + cleaner separation of concerns

---

### **When NOT to Use Each**

#### ❌ **Don't Use Functions For:**

- **Complex calculations:** Age calculation, premium totals, discounts
  - **Why:** Functions execute per field, inefficient for reused calculations
  - **Use enricher instead:** Calculate once, reference multiple times

- **Cross-field logic:** Comparing values across different fields
  - **Why:** Functions don't have access to full payload context
  - **Use enricher instead:** Full payload access for complex comparisons

- **Creating new data structures:** Building arrays, filtering, grouping
  - **Why:** Functions return single string values
  - **Use enricher instead:** Enrichers can add entire objects to payload

- **Business rules requiring conditionals:** Multi-step if/else logic
  - **Why:** Functions are for simple transformations
  - **Use enricher instead:** Java code supports complex logic

#### ❌ **Don't Use Enrichers For:**

- **Simple formatting:** Date formats, currency symbols
  - **Why:** Overkill for simple operations
  - **Use function instead:** Built-in formatDate, formatCurrency

- **Single-use transformations:** One field needs uppercase
  - **Why:** Writing Java class for one field is excessive
  - **Use function instead:** `#{uppercase(field)}`

- **Template-specific display:** Different templates format same data differently
  - **Why:** Enrichers apply to all sections, not template-specific
  - **Use function instead:** Apply formatting per field mapping

---

### **Performance Comparison**

**Scenario:** Show formatted age for 5 applicants

**❌ Poor: Using functions only**
```yaml
fieldMappings:
  "Age1": "#{calculateAge(applicants[0].dob)}"  # Calculate age
  "Age2": "#{calculateAge(applicants[1].dob)}"  # Calculate age again
  "Age3": "#{calculateAge(applicants[2].dob)}"  # Calculate age again
  "Age4": "#{calculateAge(applicants[3].dob)}"  # Calculate age again
  "Age5": "#{calculateAge(applicants[4].dob)}"  # Calculate age again
```
**Result:** Age calculated 5 times (inefficient!)

**✅ Better: Using enricher + functions**
```yaml
payloadEnrichers: [coverageSummary]  # Calculates all ages ONCE

fieldMappings:
  "Age1": "coverageSummary.enrichedApplicants[0].calculatedAge"
  "Age2": "coverageSummary.enrichedApplicants[1].calculatedAge"
  "Age3": "coverageSummary.enrichedApplicants[2].calculatedAge"
  "Age4": "coverageSummary.enrichedApplicants[3].calculatedAge"
  "Age5": "coverageSummary.enrichedApplicants[4].calculatedAge"
```
**Result:** Age calculated once, referenced 5 times (efficient!)

**✅ Best: Using enricher + function for formatting**
```yaml
payloadEnrichers: [coverageSummary]

fieldMappings:
  "Age1": "#{concat(coverageSummary.enrichedApplicants[0].calculatedAge, ' yrs')}"
  "Age2": "#{concat(coverageSummary.enrichedApplicants[1].calculatedAge, ' yrs')}"
  # ... formatting done by cheap function
```
**Result:** Expensive calculation once, cheap formatting per field!

---

### **Design Guidelines**

| Concern | Solution |
|---------|----------|
| **Data needs calculation** | Use enricher to calculate, store in payload |
| **Data needs formatting** | Use function to format at display time |
| **Data reused across fields** | Enricher calculates once, fields reference it |
| **Data used only once** | Function transforms it inline |
| **Complex business rule** | Enricher (Java code with if/else) |
| **Simple transformation** | Function (built-in utilities) |
| **Entire section needs it** | Enricher (section-level) |
| **One field needs it** | Function (field-level) |

---

### **Real-World Example**

**Requirement:** Display applicant info with:
- Age in years (calculated from DOB)
- Formatted premium with 15% family discount
- Masked SSN
- Full name in uppercase

**✅ Optimal Approach:**

**Java Enricher:**
```java
@Component
public class ApplicantEnricher implements PayloadEnricher {
    public Map<String, Object> enrich(Map<String, Object> payload) {
        // Calculate age once
        int age = calculateAge(payload.get("dateOfBirth"));
        
        // Calculate discounted premium once
        double basePremium = (Double) payload.get("premium");
        double discountedPremium = basePremium * 0.85; // 15% off
        
        // Add to payload
        Map<String, Object> enriched = new HashMap<>(payload);
        enriched.put("calculatedAge", age);
        enriched.put("finalPremium", discountedPremium);
        
        return enriched;
    }
}
```

**YAML Config:**
```yaml
sections:
  - type: ACROFORM
    payloadEnrichers: [applicant]
    
    fieldMappings:
      # Enricher calculated age, function formats
      "Age": "#{concat(calculatedAge, ' years')}"
      
      # Enricher calculated premium, function formats currency
      "Premium": "#{formatCurrency(finalPremium)}"
      
      # Function masks original SSN (no enricher needed)
      "SSN": "#{mask(ssn, 'XXX-XX-', 4)}"
      
      # Function formats name (no enricher needed)
      "FullName": "#{uppercase(#{concat(firstName, ' ', lastName)})}"
```

**Result:**
- Age: "35 years" ← Enricher calculated 35, function added " years"
- Premium: "$382.50" ← Enricher calculated 450 * 0.85 = 382.50, function added $
- SSN: "XXX-XX-6789" ← Function masked directly
- FullName: "JOHN DOE" ← Function concatenated and uppercased

---

### **Summary**

**Enrichers** = Complex calculations + business logic (runs **before** field mapping)  
**Functions** = Simple transformations + formatting (runs **during** field mapping)

**Golden Rule:** Calculate with enrichers, format with functions!