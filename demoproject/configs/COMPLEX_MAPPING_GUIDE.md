# Complex AcroForm Mapping Guide
## Role-Based Filtering, Multiple Addresses, and Overflow Handling

## Problem Statement

You have a complex application structure:
- **Applicants Array**: PRIMARY, SPOUSE, multiple DEPENDENTs
- **Nested Demographics**: firstName, lastName, DOB, SSN within each applicant
- **Multiple Addresses**: BILLING and MAILING types
- **Multiple Products**: MEDICAL, DENTAL, VISION
- **Current Coverages**: Per applicant and product type
- **PDF Limitations**: Only 3 dependent slots (need overflow handling)

## Solution: Filter-Based Path Syntax

### Syntax Overview

```yaml
# Filter by field value
"PDFField": "array[fieldName=VALUE].property"

# Filter then index
"PDFField": "array[fieldName=VALUE][0].property"

# Multiple filters
"PDFField": "array[field1=VALUE1][field2=VALUE2].property"
```

## Filter Syntax Examples

### 1. Role-Based Applicant Filtering

**Get PRIMARY applicant:**
```yaml
"Primary_FirstName": "application.applicants[relationship=PRIMARY].demographic.firstName"
```

Maps to:
```json
{
  "application": {
    "applicants": [
      {
        "relationship": "PRIMARY",
        "demographic": { "firstName": "John" }
      },
      { "relationship": "SPOUSE", ... },
      { "relationship": "DEPENDENT", ... }
    ]
  }
}
```
Result: `"John"`

**Get SPOUSE applicant:**
```yaml
"Spouse_Email": "application.applicants[relationship=SPOUSE].demographic.email"
```

### 2. Filtering + Indexing for Dependents

**Get first DEPENDENT:**
```yaml
"Dependent1_FirstName": "application.applicants[relationship=DEPENDENT][0].demographic.firstName"
```

Process:
1. Filter array to only `relationship=DEPENDENT` items → `[dep1, dep2, dep3, dep4]`
2. Take index `[0]` → `dep1`
3. Navigate to `.demographic.firstName`

**Get second DEPENDENT:**
```yaml
"Dependent2_DOB": "application.applicants[relationship=DEPENDENT][1].demographic.dateOfBirth"
```

**Get third DEPENDENT:**
```yaml
"Dependent3_SSN": "application.applicants[relationship=DEPENDENT][2].demographic.ssn"
```

### 3. Address Type Filtering

**BILLING address:**
```yaml
"Billing_Street": "application.addresses[type=BILLING].street"
"Billing_City": "application.addresses[type=BILLING].city"
"Billing_State": "application.addresses[type=BILLING].state"
```

**MAILING address:**
```yaml
"Mailing_Street": "application.addresses[type=MAILING].street"
"Mailing_City": "application.addresses[type=MAILING].city"
```

### 4. Product Type Filtering

**MEDICAL plan:**
```yaml
"Medical_PlanName": "application.proposedProducts[productType=MEDICAL].planName"
"Medical_Premium": "application.proposedProducts[productType=MEDICAL].monthlyPremium"
```

**DENTAL plan:**
```yaml
"Dental_PlanName": "application.proposedProducts[productType=DENTAL].planName"
"Dental_Premium": "application.proposedProducts[productType=DENTAL].monthlyPremium"
```

**VISION plan:**
```yaml
"Vision_PlanName": "application.proposedProducts[productType=VISION].planName"
"Vision_Premium": "application.proposedProducts[productType=VISION].monthlyPremium"
```

### 5. Multiple Filter Conditions

**Current coverage for specific applicant AND product:**
```yaml
# Primary's MEDICAL coverage
"PriorMedical_Carrier": "application.currentCoverages[applicantId=A001][productType=MEDICAL].carrierName"

# Primary's DENTAL coverage
"PriorDental_Carrier": "application.currentCoverages[applicantId=A001][productType=DENTAL].carrierName"

# Spouse's MEDICAL coverage
"SpousePriorMedical_Carrier": "application.currentCoverages[applicantId=A002][productType=MEDICAL].carrierName"
```

Process for `[applicantId=A001][productType=MEDICAL]`:
1. Filter to `applicantId=A001` → subset 1
2. Filter subset 1 to `productType=MEDICAL` → final match
3. Navigate to `.carrierName`

## Complete Mapping Example

### Payload Structure
```json
{
  "application": {
    "applicants": [
      { "relationship": "PRIMARY", "demographic": {...} },
      { "relationship": "SPOUSE", "demographic": {...} },
      { "relationship": "DEPENDENT", "demographic": {...} },  // Dep 1
      { "relationship": "DEPENDENT", "demographic": {...} },  // Dep 2
      { "relationship": "DEPENDENT", "demographic": {...} },  // Dep 3
      { "relationship": "DEPENDENT", "demographic": {...} }   // Dep 4 (overflow)
    ],
    "addresses": [
      { "type": "BILLING", ... },
      { "type": "MAILING", ... }
    ],
    "proposedProducts": [
      { "productType": "MEDICAL", ... },
      { "productType": "DENTAL", ... },
      { "productType": "VISION", ... }
    ]
  }
}
```

### Field Mapping (Main Form - First 3 Dependents)
```yaml
fieldMapping:
  # PRIMARY
  "Primary_FirstName": "application.applicants[relationship=PRIMARY].demographic.firstName"
  "Primary_LastName": "application.applicants[relationship=PRIMARY].demographic.lastName"
  
  # SPOUSE
  "Spouse_FirstName": "application.applicants[relationship=SPOUSE].demographic.firstName"
  "Spouse_LastName": "application.applicants[relationship=SPOUSE].demographic.lastName"
  
  # DEPENDENT 1
  "Dependent1_FirstName": "application.applicants[relationship=DEPENDENT][0].demographic.firstName"
  "Dependent1_DOB": "application.applicants[relationship=DEPENDENT][0].demographic.dateOfBirth"
  
  # DEPENDENT 2
  "Dependent2_FirstName": "application.applicants[relationship=DEPENDENT][1].demographic.firstName"
  "Dependent2_DOB": "application.applicants[relationship=DEPENDENT][1].demographic.dateOfBirth"
  
  # DEPENDENT 3
  "Dependent3_FirstName": "application.applicants[relationship=DEPENDENT][2].demographic.firstName"
  "Dependent3_DOB": "application.applicants[relationship=DEPENDENT][2].demographic.dateOfBirth"
  
  # BILLING vs MAILING
  "Billing_Street": "application.addresses[type=BILLING].street"
  "Mailing_Street": "application.addresses[type=MAILING].street"
  
  # MEDICAL vs DENTAL vs VISION
  "Medical_Plan": "application.proposedProducts[productType=MEDICAL].planName"
  "Dental_Plan": "application.proposedProducts[productType=DENTAL].planName"
  "Vision_Plan": "application.proposedProducts[productType=VISION].planName"
```

## Overflow Handling: 4+ Dependents

### Problem
PDF has only 3 dependent slots. If there are 4+ dependents, the extras must go to an addendum page.

### Solution: Two-Section Approach

**Section 1: AcroForm (Main Form)**
- Maps first 3 dependents to PDF fields
- If < 3 dependents, extra fields stay empty

**Section 2: FreeMarker Addendum**
- Dynamically generates page for remaining dependents
- Only included if needed

### Configuration
```yaml
sections:
  # Main form: Fixed 3 slots
  - name: enrollment-application
    type: acroform
    template: enrollment-form.pdf
    fieldMapping:
      "Dependent1_FirstName": "application.applicants[relationship=DEPENDENT][0].demographic.firstName"
      "Dependent2_FirstName": "application.applicants[relationship=DEPENDENT][1].demographic.firstName"
      "Dependent3_FirstName": "application.applicants[relationship=DEPENDENT][2].demographic.firstName"
  
  # Addendum: Dynamic remaining dependents
  - name: additional-dependents
    type: freemarker
    template: additional-dependents-addendum.ftl
    enabled: true
```

### FreeMarker Addendum Template

**File: `additional-dependents-addendum.ftl`**
```html
<#-- Filter to DEPENDENT relationship -->
<#assign allDependents = application.applicants?filter(a -> a.relationship == "DEPENDENT")>
<#assign additionalDependents = allDependents[3..]>  <#-- Skip first 3 -->

<#if (additionalDependents?size > 0)>
<html>
<head><style>
  table { border-collapse: collapse; width: 100%; }
  th, td { border: 1px solid black; padding: 8px; text-align: left; }
</style></head>
<body>
  <h2>Additional Dependents Addendum</h2>
  <p>The following dependents could not fit on the main application form:</p>
  
  <table>
    <tr>
      <th>Dependent #</th>
      <th>First Name</th>
      <th>Last Name</th>
      <th>Date of Birth</th>
      <th>SSN</th>
      <th>Gender</th>
    </tr>
    <#list additionalDependents as dependent>
    <tr>
      <td>${dependent?index + 4}</td>  <#-- Start at 4 -->
      <td>${dependent.demographic.firstName}</td>
      <td>${dependent.demographic.lastName}</td>
      <td>${dependent.demographic.dateOfBirth}</td>
      <td>${dependent.demographic.ssn}</td>
      <td>${dependent.demographic.gender}</td>
    </tr>
    </#list>
  </table>
</body>
</html>
</#if>
```

Result:
- **1-3 dependents**: Only main form generated
- **4+ dependents**: Main form (3 deps) + Addendum page (remaining)

## Implementation: Enhanced Path Resolution

### Current AcroFormFillService Enhancement Needed

The existing service needs to support filter syntax:

```java
// Enhanced resolveValue method
private Object resolveValue(Object current, String path) {
    // Check for filter syntax: array[field=value]
    Pattern filterPattern = Pattern.compile("\\[([^=]+)=([^\\]]+)\\]");
    Matcher filterMatcher = filterPattern.matcher(path);
    
    if (filterMatcher.find()) {
        String filterField = filterMatcher.group(1);
        String filterValue = filterMatcher.group(2);
        
        // Filter array
        if (current instanceof List) {
            List<?> filtered = ((List<?>) current).stream()
                .filter(item -> matchesFilter(item, filterField, filterValue))
                .collect(Collectors.toList());
            
            // Continue with filtered array
            // ...
        }
    }
    
    // ... rest of existing logic
}
```

**I'll implement this enhancement next if you confirm this approach works for your use case.**

## Conditional Field Mapping

### Show/Hide Based on Data

**Example: Show spouse fields only if spouse exists**

Prepare payload with conditional flags:
```json
{
  "application": {
    "hasSpouse": true,  // Calculated field
    "applicants": [...]
  }
}
```

Use in checkboxes:
```yaml
fieldMapping:
  "HasSpouse_Checkbox": "application.hasSpouse"  # true → "Yes"
```

## Pre-Processing Strategy

For complex scenarios, pre-process payload before mapping:

### Example: Java Pre-Processor

```java
@Service
public class ApplicationPreProcessor {
    
    public Map<String, Object> preprocessForPdf(Application application) {
        Map<String, Object> processed = new HashMap<>();
        
        // Extract by role
        processed.put("primaryApplicant", 
            application.getApplicants().stream()
                .filter(a -> "PRIMARY".equals(a.getRelationship()))
                .findFirst().orElse(null));
        
        processed.put("spouseApplicant",
            application.getApplicants().stream()
                .filter(a -> "SPOUSE".equals(a.getRelationship()))
                .findFirst().orElse(null));
        
        // Get first 3 dependents
        List<Applicant> dependents = application.getApplicants().stream()
            .filter(a -> "DEPENDENT".equals(a.getRelationship()))
            .collect(Collectors.toList());
        
        processed.put("dependent1", dependents.size() > 0 ? dependents.get(0) : null);
        processed.put("dependent2", dependents.size() > 1 ? dependents.get(1) : null);
        processed.put("dependent3", dependents.size() > 2 ? dependents.get(2) : null);
        processed.put("additionalDependents", 
            dependents.size() > 3 ? dependents.subList(3, dependents.size()) : Collections.emptyList());
        
        // Extract by type
        processed.put("billingAddress",
            application.getAddresses().stream()
                .filter(a -> "BILLING".equals(a.getType()))
                .findFirst().orElse(null));
        
        processed.put("mailingAddress",
            application.getAddresses().stream()
                .filter(a -> "MAILING".equals(a.getType()))
                .findFirst().orElse(null));
        
        // Products by type
        Map<String, Product> productsByType = application.getProposedProducts().stream()
            .collect(Collectors.toMap(Product::getProductType, p -> p));
        
        processed.put("medicalPlan", productsByType.get("MEDICAL"));
        processed.put("dentalPlan", productsByType.get("DENTAL"));
        processed.put("visionPlan", productsByType.get("VISION"));
        
        return processed;
    }
}
```

### Simplified Mapping After Pre-Processing

```yaml
fieldMapping:
  # Direct access (no filtering needed)
  "Primary_FirstName": "primaryApplicant.demographic.firstName"
  "Spouse_FirstName": "spouseApplicant.demographic.firstName"
  "Dependent1_FirstName": "dependent1.demographic.firstName"
  "Dependent2_FirstName": "dependent2.demographic.firstName"
  "Dependent3_FirstName": "dependent3.demographic.firstName"
  
  "Billing_Street": "billingAddress.street"
  "Mailing_Street": "mailingAddress.street"
  
  "Medical_Plan": "medicalPlan.planName"
  "Dental_Plan": "dentalPlan.planName"
  "Vision_Plan": "visionPlan.planName"
```

## Comparison: Filter Syntax vs Pre-Processing

| Approach | Pros | Cons |
|----------|------|------|
| **Filter Syntax** | • Configuration-only<br>• No Java code changes<br>• Reusable patterns | • More complex path resolution<br>• Needs service enhancement |
| **Pre-Processing** | • Simpler mappings<br>• Better performance<br>• Easier debugging | • Requires Java code<br>• Less flexible<br>• Coupled to structure |

**Recommendation**: Use **pre-processing** for complex, application-wide scenarios like yours. More maintainable and performant.

## Complete Working Example

### Step 1: Create Pre-Processor

```java
@Service
public class EnrollmentApplicationPreProcessor {
    
    public Map<String, Object> prepareForPdfMapping(Application application) {
        Map<String, Object> flattened = new HashMap<>();
        
        // Application level
        flattened.put("applicationId", application.getApplicationId());
        flattened.put("submittedDate", application.getSubmittedDate());
        flattened.put("effectiveDate", application.getEffectiveDate());
        
        // Separate applicants by role
        application.getApplicants().stream()
            .filter(a -> "PRIMARY".equals(a.getRelationship()))
            .findFirst()
            .ifPresent(p -> flattened.put("primary", p));
        
        application.getApplicants().stream()
            .filter(a -> "SPOUSE".equals(a.getRelationship()))
            .findFirst()
            .ifPresent(s -> flattened.put("spouse", s));
        
        // First 3 dependents
        List<Applicant> deps = application.getApplicants().stream()
            .filter(a -> "DEPENDENT".equals(a.getRelationship()))
            .collect(Collectors.toList());
        
        if (deps.size() > 0) flattened.put("dependent1", deps.get(0));
        if (deps.size() > 1) flattened.put("dependent2", deps.get(1));
        if (deps.size() > 2) flattened.put("dependent3", deps.get(2));
        if (deps.size() > 3) flattened.put("additionalDependents", deps.subList(3, deps.size()));
        
        // Addresses by type
        application.getAddresses().stream()
            .filter(a -> "BILLING".equals(a.getType()))
            .findFirst()
            .ifPresent(a -> flattened.put("billing", a));
        
        application.getAddresses().stream()
            .filter(a -> "MAILING".equals(a.getType()))
            .findFirst()
            .ifPresent(a -> flattened.put("mailing", a));
        
        // Products by type
        application.getProposedProducts().forEach(p -> {
            switch (p.getProductType()) {
                case "MEDICAL": flattened.put("medical", p); break;
                case "DENTAL": flattened.put("dental", p); break;
                case "VISION": flattened.put("vision", p); break;
            }
        });
        
        return flattened;
    }
}
```

### Step 2: Use in Controller

```java
@PostMapping("/api/enrollment/generate-pdf")
public ResponseEntity<byte[]> generateEnrollmentPdf(@RequestBody Application application) {
    
    // Pre-process
    Map<String, Object> flattenedPayload = preprocessor.prepareForPdfMapping(application);
    
    // Generate PDF
    byte[] pdfBytes = pdfService.generatePdf("enrollment-application.yml", flattenedPayload);
    
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdfBytes);
}
```

### Step 3: Simplified Mapping

```yaml
# enrollment-application.yml
fieldMapping:
  "Primary_FirstName": "primary.demographic.firstName"
  "Spouse_FirstName": "spouse.demographic.firstName"
  "Dependent1_FirstName": "dependent1.demographic.firstName"
  "Dependent2_FirstName": "dependent2.demographic.firstName"
  "Dependent3_FirstName": "dependent3.demographic.firstName"
  
  "Billing_Street": "billing.street"
  "Mailing_Street": "mailing.street"
  
  "Medical_Plan": "medical.planName"
  "Dental_Plan": "dental.planName"
  "Vision_Plan": "vision.planName"
```

## Summary

### For Your Complex Requirement:

**1. Role-Based Applicants**
```yaml
"Primary_FirstName": "application.applicants[relationship=PRIMARY].demographic.firstName"
"Spouse_FirstName": "application.applicants[relationship=SPOUSE].demographic.firstName"
"Dependent1_FirstName": "application.applicants[relationship=DEPENDENT][0].demographic.firstName"
```

**2. Address Types**
```yaml
"Billing_City": "application.addresses[type=BILLING].city"
"Mailing_City": "application.addresses[type=MAILING].city"
```

**3. Product Types**
```yaml
"Medical_Plan": "application.proposedProducts[productType=MEDICAL].planName"
"Dental_Plan": "application.proposedProducts[productType=DENTAL].planName"
"Vision_Plan": "application.proposedProducts[productType=VISION].planName"
```

**4. Multiple Filters**
```yaml
"PriorMedical": "application.currentCoverages[applicantId=A001][productType=MEDICAL].carrierName"
```

**5. Overflow (4+ Dependents)**
- Main form: AcroForm with 3 slots
- Addendum: FreeMarker template for extras

**Recommendation**: Implement **pre-processor** for cleaner, more maintainable solution.

See complete example in [complex-enrollment-mapping.yml](examples/complex-enrollment-mapping.yml)
