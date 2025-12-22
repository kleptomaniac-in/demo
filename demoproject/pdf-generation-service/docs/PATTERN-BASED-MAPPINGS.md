# Pattern-Based Field Mappings for AcroForm

## Overview

Pattern-based field mappings allow you to define repeating field structures once and automatically expand them for multiple array elements (e.g., multiple dependents, addresses, coverages). This eliminates the need to manually write repetitive field mappings.

## Configuration Structure

```yaml
sections:
  - type: "acroform"
    template: "templates/enrollment-form.pdf"
    
    # Pattern definitions
    patterns:
      - fieldPattern: "Dependent{n}_*"
        source: "applicants[relationship=DEPENDENT][{n}]"
        maxIndex: 2
        fields:
          FirstName: "demographic.firstName"
          LastName: "demographic.lastName"
          DOB: "demographic.dateOfBirth"
    
    # Explicit field mappings (optional)
    fieldMapping:
      "Primary_FirstName": "applicants[relationship=PRIMARY].demographic.firstName"
```

## Pattern Configuration

Each pattern has four properties:

### 1. `fieldPattern` (required)
The naming pattern for PDF fields. Use placeholders:
- `{n}` - Replaced with display index (1, 2, 3...)
- `*` - Replaced with field suffix

**Example:** `"Dependent{n}_*"` generates:
- `Dependent1_FirstName`
- `Dependent1_LastName`
- `Dependent2_FirstName`
- etc.

### 2. `source` (required)
The base path to the data array with index placeholder:
- `{n}` - Replaced with array index (0, 1, 2...)

**Example:** `"applicants[relationship=DEPENDENT][{n}]"`
- Index 0 → `applicants[relationship=DEPENDENT][0]`
- Index 1 → `applicants[relationship=DEPENDENT][1]`

### 3. `maxIndex` (required)
Maximum array index to generate (0-based):
- `maxIndex: 2` generates indices 0, 1, 2 (displayed as 1, 2, 3 in field names)

### 4. `fields` (required)
Map of field suffixes to data paths:
```yaml
fields:
  FirstName: "demographic.firstName"  # Suffix → Path under source
  LastName: "demographic.lastName"
```

## How Patterns Expand

Given this pattern:
```yaml
- fieldPattern: "Dependent{n}_*"
  source: "applicants[relationship=DEPENDENT][{n}]"
  maxIndex: 1  # Generates 0, 1
  fields:
    FirstName: "demographic.firstName"
    LastName: "demographic.lastName"
```

**Expands to:**
```yaml
"Dependent1_FirstName": "applicants[relationship=DEPENDENT][0].demographic.firstName"
"Dependent1_LastName": "applicants[relationship=DEPENDENT][0].demographic.lastName"
"Dependent2_FirstName": "applicants[relationship=DEPENDENT][1].demographic.firstName"
"Dependent2_LastName": "applicants[relationship=DEPENDENT][1].demographic.lastName"
```

## Multiple Patterns

You can define multiple patterns in the same section:

```yaml
patterns:
  # Pattern for dependent demographics
  - fieldPattern: "Dependent{n}_*"
    source: "applicants[relationship=DEPENDENT][{n}]"
    maxIndex: 2
    fields:
      FirstName: "demographic.firstName"
      LastName: "demographic.lastName"
      DOB: "demographic.dateOfBirth"
  
  # Pattern for dependent coverages
  - fieldPattern: "Dependent{n}_Coverage_*"
    source: "applicants[relationship=DEPENDENT][{n}].products[0]"
    maxIndex: 2
    fields:
      Type: "productType"
      PlanName: "planName"
      Premium: "premium"
  
  # Pattern for dependent addresses
  - fieldPattern: "Dependent{n}_Address_*"
    source: "applicants[relationship=DEPENDENT][{n}].mailingAddress"
    maxIndex: 2
    fields:
      Street: "street"
      City: "city"
      State: "state"
      Zip: "zipCode"
```

## Combining Patterns and Explicit Mappings

Patterns are expanded first, then explicit `fieldMapping` entries are added. Explicit mappings can override pattern-generated ones:

```yaml
patterns:
  - fieldPattern: "Dependent{n}_*"
    source: "applicants[relationship=DEPENDENT][{n}]"
    maxIndex: 2
    fields:
      FirstName: "demographic.firstName"

fieldMapping:
  # Non-repeating fields
  "Primary_FirstName": "applicants[relationship=PRIMARY].demographic.firstName"
  
  # Override pattern-generated mapping (if needed)
  "Dependent1_FirstName": "customPath.firstName"
```

## Handling Missing Data

If an array has fewer elements than `maxIndex`, missing elements are handled gracefully:
- Fields are left blank in the PDF (empty string)
- Warning message logged: `"No items match filter [relationship=DEPENDENT]"`
- No exceptions thrown

**Example:** If only 2 dependents exist but `maxIndex: 2` (generates 3):
- `Dependent1_*` → Filled with data
- `Dependent2_*` → Filled with data
- `Dependent3_*` → Left blank (empty strings)

## Benefits

✅ **Reduced configuration** - Define once, expand automatically  
✅ **Maintainable** - Add new fields in one place  
✅ **Flexible** - Mix patterns with explicit mappings  
✅ **Safe** - Graceful handling of missing data  
✅ **Compatible** - Works with filter syntax (`[relationship=DEPENDENT]`)

## Example Use Cases

1. **Multiple Dependents** (children, spouse, etc.)
2. **Multiple Addresses** (billing, mailing, residential)
3. **Multiple Coverages** (medical, dental, vision)
4. **Multiple Prior Carriers** (replacement business)
5. **Multiple Beneficiaries** (primary, contingent)

## Testing

See [test-pattern-config.yml](test-pattern-config.yml) and [test-pattern-payload.json](test-pattern-payload.json) for complete examples.

### Verification

When the application processes patterns, you'll see console output:
```
Expanded 2 patterns into 27 field mappings
Filled field: Dependent1_FirstName = Emily
Filled field: Dependent1_LastName = Smith
Filled field: Dependent2_FirstName = Michael
...
```
