# Static/Fixed Values in AcroForm Fields

## Overview

You can set fixed/static values for PDF form fields that don't come from the payload. This is useful for:
- Form titles and headers
- Version numbers
- Fixed disclaimers and legal text
- Department names
- Static labels
- Form dates

## Usage

Use the `static:` prefix in the field mapping path, followed by the literal value you want to display.

### Syntax

```yaml
fieldMapping:
  "FieldName": "static:Your Fixed Text Here"
```

## Configuration Examples

### Example 1: Basic Static Values

```yaml
sections:
  - type: "acroform"
    template: "templates/enrollment-form.pdf"
    fieldMapping:
      # Static form metadata
      "FormTitle": "static:Health Insurance Enrollment Form"
      "FormVersion": "static:v2.0"
      "FormDate": "static:2025-12-18"
      "Department": "static:Human Resources"
      
      # Dynamic data from payload
      "ApplicantName": "applicants[relationship=PRIMARY].demographic.firstName"
      "ApplicationNumber": "applicationNumber"
```

### Example 2: Static Values with Patterns

```yaml
patterns:
  - fieldPattern: "Dependent{n}_*"
    source: "applicants[relationship=DEPENDENT][{n}]"
    maxIndex: 2
    fields:
      # Dynamic fields from payload
      FirstName: "demographic.firstName"
      LastName: "demographic.lastName"
      DOB: "demographic.dateOfBirth"
      
      # Static fields - same value for all dependents
      RelationshipLabel: "static:Dependent"
      FormType: "static:Enrollment"
      SectionTitle: "static:Dependent Information"
```

**This expands to:**
```yaml
"Dependent1_FirstName": "applicants[relationship=DEPENDENT][0].demographic.firstName"
"Dependent1_LastName": "applicants[relationship=DEPENDENT][0].demographic.lastName"
"Dependent1_RelationshipLabel": "static:Dependent"  # Same for all
"Dependent1_FormType": "static:Enrollment"          # Same for all
"Dependent2_FirstName": "applicants[relationship=DEPENDENT][1].demographic.firstName"
"Dependent2_RelationshipLabel": "static:Dependent"  # Same for all
# ... and so on
```

### Example 3: Disclaimers and Legal Text

```yaml
fieldMapping:
  "Disclaimer1": "static:I hereby certify that the information provided is true and accurate."
  "Disclaimer2": "static:I understand that false statements may result in denial of coverage."
  "LegalNotice": "static:This form is subject to the terms and conditions outlined in the policy."
  "PrivacyNotice": "static:Your information will be protected in accordance with HIPAA regulations."
```

### Example 4: Mixed Static and Dynamic

```yaml
sections:
  - type: "acroform"
    template: "templates/form.pdf"
    fieldMapping:
      # Page 1 header - static
      "Page1_Title": "static:Enrollment Application"
      "Page1_Version": "static:v2.0"
      
      # Page 1 content - dynamic
      "Page1_ApplicantName": "applicants[relationship=PRIMARY].demographic.firstName"
      "Page1_ApplicationDate": "applicationDate"
      
      # Page 2 header - static (repeated)
      "Page2_Title": "static:Enrollment Application"
      "Page2_Version": "static:v2.0"
      
      # Page 2 content - dynamic
      "Page2_SpouseName": "applicants[relationship=SPOUSE].demographic.firstName"
```

### Example 5: Contact Information

```yaml
fieldMapping:
  # Company contact info - static
  "CompanyName": "static:Acme Insurance Company"
  "CompanyAddress": "static:123 Main Street, Suite 100, City, ST 12345"
  "CompanyPhone": "static:(555) 123-4567"
  "CompanyEmail": "static:info@acmeinsurance.com"
  "CompanyWebsite": "static:www.acmeinsurance.com"
  
  # Applicant info - dynamic
  "ApplicantName": "applicants[relationship=PRIMARY].demographic.firstName"
  "ApplicantPhone": "applicants[relationship=PRIMARY].contactInfo.phone"
```

### Example 6: Currency and Numeric Static Values

```yaml
fieldMapping:
  "ApplicationFee": "static:$50.00"
  "ProcessingTime": "static:5-7 business days"
  "MinimumAge": "static:18 years"
  "MaxCoverage": "static:$1,000,000"
```

## Special Characters Support

Static values support all special characters:

```yaml
fieldMapping:
  "Address": "static:123 Main St, Suite 100, City, ST 12345"
  "Phone": "static:(555) 123-4567"
  "Email": "static:info@example.com"
  "Currency": "static:$1,000.00"
  "Percentage": "static:25%"
  "Date": "static:12/18/2025"
```

## Real-World Use Cases

### Use Case 1: Form Header/Footer

```yaml
fieldMapping:
  # Header on every page
  "Header_Page1": "static:Health Insurance Enrollment - Page 1 of 3"
  "Header_Page2": "static:Health Insurance Enrollment - Page 2 of 3"
  "Header_Page3": "static:Health Insurance Enrollment - Page 3 of 3"
  
  # Footer on every page
  "Footer_Page1": "static:© 2025 Acme Insurance. All rights reserved."
  "Footer_Page2": "static:© 2025 Acme Insurance. All rights reserved."
  "Footer_Page3": "static:© 2025 Acme Insurance. All rights reserved."
```

### Use Case 2: Section Labels

```yaml
patterns:
  - fieldPattern: "Section{n}_*"
    source: "sections[{n}]"
    maxIndex: 3
    fields:
      Title: "static:Section Information"  # Same title for all sections
      Instructions: "static:Please complete all required fields"
      Content: "description"  # Dynamic from payload
```

### Use Case 3: Signature Block

```yaml
fieldMapping:
  # Static labels
  "SignatureLabel": "static:Applicant Signature"
  "DateLabel": "static:Date"
  "WitnessLabel": "static:Witness Signature"
  
  # Dynamic values
  "ApplicantName": "applicants[relationship=PRIMARY].demographic.firstName"
  "SignatureDate": "applicationDate"
```

### Use Case 4: Multiple Language Support

```yaml
fieldMapping:
  # English static text
  "InstructionsEN": "static:Please complete all sections in ink"
  "SignatureEN": "static:Applicant Signature"
  
  # Spanish static text
  "InstructionsES": "static:Por favor complete todas las secciones con tinta"
  "SignatureES": "static:Firma del Solicitante"
```

## How It Works

1. **Configuration**: Field mapping contains `static:` prefix
   ```yaml
   "FieldName": "static:Fixed Value"
   ```

2. **Pattern Expansion**: Static values are preserved during expansion
   - Regular paths get source prefix: `applicants[0].demographic.firstName`
   - Static paths remain unchanged: `static:Fixed Value`

3. **Value Resolution**: When filling the PDF:
   - Detects `static:` prefix
   - Returns everything after the prefix as literal string
   - No payload lookup performed

4. **PDF Output**: Static value is written to the field as-is

## Comparison: Static vs Dynamic

| Aspect | Dynamic Value | Static Value |
|--------|--------------|--------------|
| Configuration | `"Field": "payload.path"` | `"Field": "static:Fixed Text"` |
| Data Source | From JSON payload | From configuration |
| Can Change | Yes, per request | No, always the same |
| Null Handling | Returns empty string if null | Always has value |
| Use Case | User data, transaction data | Form metadata, labels |

## Benefits

✅ **No payload required** - Static fields work even if payload is empty  
✅ **Consistent values** - Same text every time  
✅ **Simpler payloads** - Don't need to send form metadata in every request  
✅ **Versioning** - Easy to update form version in one place  
✅ **Localization** - Support multiple languages with different configs  

## Performance

Static values have **better performance** than dynamic values:
- No payload lookup required
- No path parsing or traversal
- Direct string assignment
- Minimal overhead

## Testing

Tests confirm this functionality:
- ✅ Static values in explicit mappings
- ✅ Static values in pattern-based mappings
- ✅ Mixed static and dynamic fields
- ✅ Static values with special characters
- ✅ Static values preserved during pattern expansion

See: `PatternBasedMappingTest.testStaticValues()` and related integration tests

## Example Payload

With static values, your payload can focus on actual data:

```json
{
  "applicationNumber": "APP-2025-12345",
  "applicationDate": "2025-12-18",
  "applicants": [
    {
      "relationship": "PRIMARY",
      "demographic": {
        "firstName": "John",
        "lastName": "Smith"
      }
    }
  ]
}
```

**You don't need to include:**
- Form titles
- Version numbers
- Static disclaimers
- Company information
- Section labels

These are all handled by `static:` values in the configuration!
