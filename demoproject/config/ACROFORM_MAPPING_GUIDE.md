# AcroForm PDF Field Mapping Guide

## Overview

AcroForm PDFs have **pre-defined form fields** that must be filled with data. Unlike FreeMarker templates or PDFBox generators, you can't change the structure—you must map your payload data to the existing PDF form fields.

## Key Differences

| Type | Structure | Mapping Required? |
|------|-----------|-------------------|
| **AcroForm** | Pre-defined fields | ✅ **YES** - Explicit mapping |
| **FreeMarker** | Template with placeholders | ❌ No - Direct access |
| **PDFBox** | Programmatic | ❌ No - Direct access |

## Field Mapping Syntax

### YAML Configuration

```yaml
sections:
  - name: enrollment-form
    type: acroform  # Specify acroform type
    template: enrollment-application.pdf  # Path to AcroForm PDF
    enabled: true
    fieldMapping:  # Map PDF fields → Payload paths
      "PDFFieldName": "payload.path"
```

### Mapping Rules

**1. Simple Fields**
```yaml
fieldMapping:
  "MemberName": "member.name"
  "MemberId": "member.id"
```

Maps to payload:
```json
{
  "member": {
    "name": "Jane Doe",
    "id": "M12345"
  }
}
```

**2. Nested Fields**
```yaml
fieldMapping:
  "StreetAddress": "member.address.street"
  "City": "member.address.city"
  "State": "member.address.state"
```

Maps to payload:
```json
{
  "member": {
    "address": {
      "street": "123 Main St",
      "city": "Los Angeles",
      "state": "CA"
    }
  }
}
```

**3. Array Fields**
```yaml
fieldMapping:
  "DependentName1": "dependents[0].name"
  "DependentDOB1": "dependents[0].dateOfBirth"
  "DependentName2": "dependents[1].name"
  "DependentDOB2": "dependents[1].dateOfBirth"
```

Maps to payload:
```json
{
  "dependents": [
    {
      "name": "Child One",
      "dateOfBirth": "03/15/2015"
    },
    {
      "name": "Child Two",
      "dateOfBirth": "07/22/2018"
    }
  ]
}
```

**4. Boolean Fields (Checkboxes)**
```yaml
fieldMapping:
  "AcceptTerms": "acceptance.terms"
  "OptInMarketing": "acceptance.marketing"
```

Converts boolean to checkbox:
- `true` → "Yes"
- `false` → "No"

```json
{
  "acceptance": {
    "terms": true,        // → "Yes"
    "marketing": false    // → "No"
  }
}
```

**5. Date Fields**
```yaml
fieldMapping:
  "SignatureDate": "enrollment.signedDate"
  "EffectiveDate": "enrollment.effectiveDate"
```

Auto-formatted as `MM/dd/yyyy`:
```json
{
  "enrollment": {
    "signedDate": "2025-12-12",  // → "12/12/2025"
    "effectiveDate": "01/01/2026"  // → "01/01/2026"
  }
}
```

## Complete Example

### AcroForm PDF Fields
Your PDF has these fields:
- `MemberFirstName`
- `MemberLastName`
- `DateOfBirth`
- `SSN`
- `StreetAddress`
- `City`
- `State`
- `ZipCode`
- `MedicalPlanSelected`
- `DentalPlanSelected`
- `SignatureDate`

### YAML Configuration

```yaml
# acroform-enrollment-ca.yml
pdfMerge:
  sections:
    - name: enrollment-application
      type: acroform
      template: enrollment-application.pdf
      enabled: true
      fieldMapping:
        # Personal Information
        "MemberFirstName": "member.firstName"
        "MemberLastName": "member.lastName"
        "DateOfBirth": "member.dateOfBirth"
        "SSN": "member.ssn"
        
        # Address
        "StreetAddress": "member.address.street"
        "City": "member.address.city"
        "State": "member.address.state"
        "ZipCode": "member.address.zip"
        
        # Plan Selection
        "MedicalPlanSelected": "selectedPlans.medical"
        "DentalPlanSelected": "selectedPlans.dental"
        
        # Signature
        "SignatureDate": "enrollment.signedDate"
```

### Payload Data

```json
{
  "member": {
    "firstName": "Jane",
    "lastName": "Doe",
    "dateOfBirth": "05/15/1985",
    "ssn": "123-45-6789",
    "address": {
      "street": "123 Main Street",
      "city": "Los Angeles",
      "state": "CA",
      "zip": "90001"
    }
  },
  "selectedPlans": {
    "medical": "Gold PPO",
    "dental": "Premium Dental"
  },
  "enrollment": {
    "signedDate": "12/12/2025"
  }
}
```

### API Request

```bash
curl -X POST http://localhost:8080/api/pdf/merge \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "acroform-enrollment-ca.yml",
    "payload": { ... payload above ... }
  }' \
  -o filled-enrollment.pdf
```

## Mixed Document Types

You can combine AcroForm, FreeMarker, and PDFBox sections in one document:

```yaml
# mixed-document.yml
composition:
  base: templates/base-payer.yml

pdfMerge:
  sections:
    # Cover page (PDFBox)
    - name: cover-page
      type: pdfbox
      template: cover-page-generator
      enabled: true
    
    # Enrollment form (AcroForm)
    - name: enrollment-form
      type: acroform
      template: enrollment-application.pdf
      fieldMapping:
        "MemberName": "member.name"
        "MemberId": "member.id"
    
    # Plan details (FreeMarker)
    - name: plan-details
      type: freemarker
      template: plan-details.ftl
      enabled: true
    
    # State disclosure (AcroForm)
    - name: state-disclosure
      type: acroform
      template: ca-disclosure.pdf
      fieldMapping:
        "CompanyName": "companyName"
        "PlanName": "plans.medical.name"
```

Result: One PDF with 4 sections from 3 different sources.

## Discovering PDF Field Names

If you don't know the field names in an AcroForm PDF:

### 1. Use API Endpoint

```bash
# Endpoint to discover field names (to be implemented)
curl http://localhost:8080/api/acroform/fields?template=enrollment-application.pdf
```

**Response:**
```json
{
  "template": "enrollment-application.pdf",
  "fields": [
    "MemberFirstName",
    "MemberLastName",
    "DateOfBirth",
    "SSN",
    "StreetAddress",
    ...
  ]
}
```

### 2. Use Adobe Acrobat

1. Open PDF in Adobe Acrobat
2. Tools → Prepare Form
3. View field names in form editor

### 3. Use PDFBox Command Line

```bash
java -jar pdfbox-app.jar GetFields enrollment-application.pdf
```

## Common Patterns

### Pattern 1: Member Information

```yaml
fieldMapping:
  "FirstName": "member.firstName"
  "LastName": "member.lastName"
  "DOB": "member.dateOfBirth"
  "Email": "member.email"
  "Phone": "member.phone"
```

### Pattern 2: Multiple Dependents

```yaml
fieldMapping:
  # First dependent
  "Dependent1_Name": "dependents[0].name"
  "Dependent1_DOB": "dependents[0].dateOfBirth"
  "Dependent1_Relation": "dependents[0].relationship"
  
  # Second dependent
  "Dependent2_Name": "dependents[1].name"
  "Dependent2_DOB": "dependents[1].dateOfBirth"
  "Dependent2_Relation": "dependents[1].relationship"
```

### Pattern 3: Plan Selection (Checkboxes)

```yaml
fieldMapping:
  "MedicalCoverage": "coverage.medical"      # true/false → Yes/No
  "DentalCoverage": "coverage.dental"        # true/false → Yes/No
  "VisionCoverage": "coverage.vision"        # true/false → Yes/No
```

### Pattern 4: Calculated Fields

If you need calculated fields (e.g., total premium), calculate in payload:

```json
{
  "plans": {
    "medical": { "premium": 450.00 },
    "dental": { "premium": 45.00 }
  },
  "calculated": {
    "totalPremium": "495.00"  // Pre-calculated
  }
}
```

```yaml
fieldMapping:
  "MedicalPremium": "plans.medical.premium"
  "DentalPremium": "plans.dental.premium"
  "TotalPremium": "calculated.totalPremium"
```

## State-Specific AcroForms

Use composition to add state-specific AcroForm sections:

```yaml
# templates/states/california-acroform.yml
pdfMerge:
  sections:
    - name: ca-dmhc-disclosure
      type: acroform
      template: ca-dmhc-disclosure-form.pdf
      enabled: true
      fieldMapping:
        "CompanyLicense": "companyLicenseNumber"
        "MemberName": "member.fullName"
        "PlanName": "plans.medical.name"
        "AcknowledgeRights": "acknowledgments.rights"
```

```yaml
# medical-individual-ca.yml
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california-acroform.yml  # Adds CA AcroForm
```

## Error Handling

### Field Not Found
If a PDF field doesn't exist:
```
Warning: Field not found in PDF: InvalidFieldName
```
PDF generation continues, field is skipped.

### Value Type Mismatch
Values are automatically converted:
- Numbers → Strings
- Booleans → "Yes"/"No"
- Dates → "MM/dd/yyyy"
- Objects → toString()

### Missing Payload Data
If payload path doesn't exist:
- Field is left empty
- No error thrown
- Logged as info

## Best Practices

### 1. Document Field Mappings
Add comments in YAML:
```yaml
fieldMapping:
  # Section 1: Personal Information
  "FirstName": "member.firstName"
  "LastName": "member.lastName"
  
  # Section 2: Contact Information
  "Email": "member.email"
  "Phone": "member.phone"
```

### 2. Use Consistent Payload Structure
Keep payload structure consistent across all templates:
```json
{
  "member": { ... },      // Always here
  "plans": { ... },       // Always here
  "enrollment": { ... }   // Always here
}
```

### 3. Validate PDFs Before Deployment
Test that all field mappings work before deploying new configs.

### 4. Version Control AcroForm Templates
Store AcroForm PDFs in version control alongside YAML configs.

### 5. Handle Missing Fields Gracefully
Use optional chaining in payload preparation to avoid errors.

## Limitations

### Cannot Modify Structure
❌ Cannot add new fields to AcroForm
❌ Cannot change layout
❌ Cannot resize fields
✅ Can only fill existing fields

### Flattening (Optional)
After filling, you can flatten the PDF (make fields non-editable):
```java
// In AcroFormFillService
acroForm.flatten();  // Uncomment to flatten
```

## Summary

**AcroForm Field Mapping:**
1. Define `type: acroform` in section
2. Specify `template` (path to AcroForm PDF)
3. Add `fieldMapping` (PDF field → payload path)
4. Use dot notation for nested fields
5. Use bracket notation for arrays
6. Boolean values auto-convert to Yes/No
7. Dates auto-format to MM/dd/yyyy

**Integration:**
- Works seamlessly with composition system
- Can mix with FreeMarker and PDFBox sections
- State-specific AcroForms via components
- Automatic config selection for enrollments

**Result:** Pre-existing PDF forms filled automatically with enrollment data.
