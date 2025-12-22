# Mapping Same Value to Multiple AcroForm Fields

## Overview

You can map the same payload value to multiple different PDF form fields. This is useful when you need to show the same information in multiple places on the form (e.g., name in header and footer, application number on every page, etc.).

## Configuration Approaches

### 1. Explicit Field Mappings (Simple Approach)

Just map multiple PDF field names to the same payload path:

```yaml
sections:
  - type: "acroform"
    template: "templates/enrollment-form.pdf"
    fieldMapping:
      # Show application number in 3 different places
      "AppNumber_Page1": "applicationNumber"
      "AppNumber_Page2": "applicationNumber"
      "AppNumber_Summary": "applicationNumber"
      
      # Show effective date in header and footer
      "EffectiveDate_Header": "effectiveDate"
      "EffectiveDate_Footer": "effectiveDate"
      
      # Show applicant name in multiple sections
      "ApplicantName_Section1": "applicants[relationship=PRIMARY].demographic.firstName"
      "ApplicantName_Section2": "applicants[relationship=PRIMARY].demographic.firstName"
      "ApplicantName_Signature": "applicants[relationship=PRIMARY].demographic.firstName"
```

### 2. Pattern-Based Mappings with Same Source

When using patterns, you can define multiple field suffixes that point to the same data:

```yaml
sections:
  - type: "acroform"
    template: "templates/form.pdf"
    patterns:
      - fieldPattern: "Dependent{n}_*"
        source: "applicants[relationship=DEPENDENT][{n}]"
        maxIndex: 2
        fields:
          # Different field suffixes, same source path
          Name: "demographic.firstName"
          NameConfirmation: "demographic.firstName"  # Same as Name
          SignatureName: "demographic.firstName"     # Same as Name
          
          # Each dependent field set will have all three fields showing same name
```

**This generates:**
```yaml
"Dependent1_Name": "applicants[relationship=DEPENDENT][0].demographic.firstName"
"Dependent1_NameConfirmation": "applicants[relationship=DEPENDENT][0].demographic.firstName"
"Dependent1_SignatureName": "applicants[relationship=DEPENDENT][0].demographic.firstName"
"Dependent2_Name": "applicants[relationship=DEPENDENT][1].demographic.firstName"
"Dependent2_NameConfirmation": "applicants[relationship=DEPENDENT][1].demographic.firstName"
"Dependent2_SignatureName": "applicants[relationship=DEPENDENT][1].demographic.firstName"
# ... and so on
```

### 3. Multiple Patterns Sharing Same Data Source

You can have different patterns that reference the same source data:

```yaml
patterns:
  # Detail section showing name
  - fieldPattern: "DetailSection{n}_*"
    source: "applicants[relationship=PRIMARY][{n}]"
    maxIndex: 0
    fields:
      ApplicantName: "demographic.firstName"
  
  # Summary section also showing same name
  - fieldPattern: "SummarySection{n}_*"
    source: "applicants[relationship=PRIMARY][{n}]"  # Same source
    maxIndex: 0
    fields:
      ApplicantName: "demographic.firstName"  # Same path
```

## Common Use Cases

### Use Case 1: Application Number on Every Page

```yaml
fieldMapping:
  "AppNumber_Page1_Header": "applicationNumber"
  "AppNumber_Page1_Footer": "applicationNumber"
  "AppNumber_Page2_Header": "applicationNumber"
  "AppNumber_Page2_Footer": "applicationNumber"
  "AppNumber_Page3_Header": "applicationNumber"
  "AppNumber_Page3_Footer": "applicationNumber"
```

**Payload:**
```json
{
  "applicationNumber": "APP-2025-12345"
}
```

**Result:** All 6 fields show "APP-2025-12345"

### Use Case 2: Name in Multiple Locations

```yaml
fieldMapping:
  "PrimaryName_CoverPage": "applicants[relationship=PRIMARY].demographic.firstName"
  "PrimaryName_Section1": "applicants[relationship=PRIMARY].demographic.firstName"
  "PrimaryName_Section2": "applicants[relationship=PRIMARY].demographic.firstName"
  "PrimaryName_Signature": "applicants[relationship=PRIMARY].demographic.firstName"
```

**Payload:**
```json
{
  "applicants": [
    {
      "relationship": "PRIMARY",
      "demographic": {
        "firstName": "John"
      }
    }
  ]
}
```

**Result:** All 4 fields show "John"

### Use Case 3: Dependent Information in Multiple Formats

```yaml
patterns:
  - fieldPattern: "Dependent{n}_*"
    source: "applicants[relationship=DEPENDENT][{n}]"
    maxIndex: 2
    fields:
      # Basic info section
      Name_Basic: "demographic.firstName"
      DOB_Basic: "demographic.dateOfBirth"
      
      # Detail section (same data)
      Name_Detail: "demographic.firstName"
      DOB_Detail: "demographic.dateOfBirth"
      
      # Summary section (same data again)
      Name_Summary: "demographic.firstName"
      DOB_Summary: "demographic.dateOfBirth"
```

**Expands to:**
- `Dependent1_Name_Basic`, `Dependent1_Name_Detail`, `Dependent1_Name_Summary` → all show same name
- `Dependent1_DOB_Basic`, `Dependent1_DOB_Detail`, `Dependent1_DOB_Summary` → all show same DOB
- Same pattern for Dependent2 and Dependent3

### Use Case 4: Date Stamps Throughout Document

```yaml
fieldMapping:
  "EffectiveDate_Page1": "effectiveDate"
  "EffectiveDate_Page2": "effectiveDate"
  "EffectiveDate_Page3": "effectiveDate"
  "EffectiveDate_Signature": "effectiveDate"
  "PolicyStartDate": "effectiveDate"  # Same date, different context
```

### Use Case 5: Parent Info for All Dependents

```yaml
patterns:
  - fieldPattern: "Dependent{n}_*"
    source: "applicants[relationship=DEPENDENT][{n}]"
    maxIndex: 2
    fields:
      ChildFirstName: "demographic.firstName"
      ChildLastName: "demographic.lastName"
      
      # Note: In current implementation, you'd need to use the full path
      # to reference parent data from within each dependent's context
```

For parent data shared across all dependents, use explicit mappings:

```yaml
fieldMapping:
  # Parent info repeated for each dependent section
  "Dependent1_ParentName": "applicants[relationship=PRIMARY].demographic.firstName"
  "Dependent2_ParentName": "applicants[relationship=PRIMARY].demographic.firstName"
  "Dependent3_ParentName": "applicants[relationship=PRIMARY].demographic.firstName"
```

## Important Notes

1. **No Duplication in Payload**: The same value is read once from the payload and written to multiple PDF fields. No data duplication in source.

2. **Independent Fields**: Each PDF field is filled independently, even though they share the same source value.

3. **Works with Filters**: You can use filter syntax with shared values:
   ```yaml
   "Field1": "applicants[relationship=PRIMARY].demographic.firstName"
   "Field2": "applicants[relationship=PRIMARY].demographic.firstName"
   ```

4. **Null Handling**: If the source value is null/missing, all fields pointing to it will be empty (blank string).

5. **Pattern + Explicit**: You can combine both approaches - patterns can generate some mappings, and explicit mappings can add or override others.

## Testing

Tests confirm this functionality:
- ✅ Multiple explicit mappings to same path
- ✅ Pattern fields with duplicate source paths
- ✅ Multiple patterns sharing same data source
- ✅ Combination of pattern and explicit mappings

See: `PatternBasedMappingTest` and `PatternBasedMappingIntegrationTest`

## Performance Consideration

Mapping the same value to multiple fields has minimal performance impact:
- Value is resolved once from payload
- Same resolved value is written to each field
- No additional payload parsing or path resolution overhead
