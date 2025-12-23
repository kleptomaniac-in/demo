# Excel YAML Configuration - Quick Reference

## File Structure

```
config-repo/
├── excel/
│   ├── enrollment-summary-excel.yml         # Simple cell mappings
│   ├── dependent-list-excel.yml            # Table mappings
│   ├── enrollment-complete-excel.yml       # Cell + table mappings
│   ├── enrollment-summary-composed-excel.yml # Composition
│   └── components/
│       ├── base-excel.yml                  # Base component
│       ├── dependents-excel.yml            # Dependent mappings
│       └── medical-excel.yml               # Medical product mappings
```

## Configuration Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `description` | String | No | Human-readable description |
| `version` | String | No | Config version |
| `templatePath` | String | Yes | Excel template filename |
| `preprocessingRules` | String | No | YAML preprocessing rules filename |
| `cellMappings` | Map | No* | Cell reference → payload path |
| `tableMappings` | List | No* | Table/list configurations |
| `composition.base` | String | No | Base config file path |
| `composition.components` | List | No | Component config file paths |

*At least one of `cellMappings` or `tableMappings` required

---

## Example 1: Simple Cell Mappings

**Config:** `excel/enrollment-summary-excel.yml`
```yaml
templatePath: "enrollment-summary.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"

cellMappings:
  ApplicationId: "applicationId"
  PrimaryFirstName: "primary.firstName"
  PrimaryLastName: "primary.lastName"
  Dependent1FirstName: "dependent1.firstName"
  MedicalPlanName: "medical.planName"
```

**Request:**
```bash
POST /api/excel/generate-from-config
{
  "configName": "excel/enrollment-summary-excel.yml",
  "payload": {...}
}
```

---

## Example 2: Table Mappings

**Config:** `excel/dependent-list-excel.yml`
```yaml
templatePath: "dependent-list.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"

tableMappings:
  - sheetName: "Dependents"
    startRow: 1
    sourcePath: "allDependents"
    columnMappings:
      0: "firstName"
      1: "lastName"
      2: "dateOfBirth"
```

---

## Example 3: Combined (Cell + Table)

**Config:** `excel/enrollment-complete-excel.yml`
```yaml
templatePath: "enrollment-complete.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"

cellMappings:
  Summary!A1: "applicationId"
  Summary!B2: "primary.firstName"
  Summary!B3: "dependent1.firstName"

tableMappings:
  - sheetName: "All Dependents"
    startRow: 1
    sourcePath: "allDependents"
    columnMappings:
      0: "firstName"
      1: "lastName"
```

---

## Example 4: Composition

**Base:** `excel/components/base-excel.yml`
```yaml
cellMappings:
  ApplicationId: "applicationId"
  PrimaryFirstName: "primary.firstName"
```

**Component:** `excel/components/dependents-excel.yml`
```yaml
cellMappings:
  Dependent1FirstName: "dependent1.firstName"
  Dependent2FirstName: "dependent2.firstName"
```

**Composed:** `excel/enrollment-summary-composed-excel.yml`
```yaml
composition:
  base: "excel/components/base-excel.yml"
  components:
    - "excel/components/dependents-excel.yml"

templatePath: "enrollment-summary.xlsx"

cellMappings:
  # Additional mappings merged with base + components
  TotalMonthlyPremium: "totalMonthlyPremium"
```

---

## Cell Reference Formats

### Named Ranges (Recommended)
```yaml
cellMappings:
  ApplicationId: "applicationId"        # Uses named range "ApplicationId"
  PrimaryName: "primary.firstName"      # Uses named range "PrimaryName"
```

### Direct Cell References
```yaml
cellMappings:
  A1: "applicationId"                   # Cell A1 on Sheet 1
  B2: "primary.firstName"               # Cell B2 on Sheet 1
```

### Sheet-Specific References
```yaml
cellMappings:
  Summary!A1: "applicationId"           # Cell A1 on "Summary" sheet
  Details!B5: "primary.firstName"       # Cell B5 on "Details" sheet
```

---

## Table Mapping Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `sheetName` | String | Excel sheet name | "Dependents" |
| `startRow` | Integer | First data row (0-indexed) | 1 |
| `sourcePath` | String | Payload path to array | "allDependents" |
| `columnMappings` | Map | Column index → field path | `{0: "firstName"}` |

---

## Path Resolution (Same as PDF)

### Simple Property
```yaml
"applicationId"                           # payload.applicationId
```

### Nested Object
```yaml
"primary.firstName"                       # payload.primary.firstName
```

### Array Index
```yaml
"dependents[0].name"                      # payload.dependents[0].name
```

### Array Filter
```yaml
"applicants[relationship=PRIMARY].firstName"
```

### Multiple Filters
```yaml
"coverages[applicantId=A001][productType=MEDICAL].carrierName"
```

---

## Preprocessing Integration

When `preprocessingRules` is specified, the controller automatically:

1. Loads preprocessing rules from `preprocessing/{rulesFile}`
2. Applies transformations to flatten nested structures
3. Uses preprocessed payload for Excel generation

**Example:**
```yaml
preprocessingRules: "standard-enrollment-rules.yml"
```

**Transforms:**
```
application.applicants[relationship=DEPENDENT] → dependent1, dependent2, dependent3
application.proposedProducts[productType=MEDICAL] → medical
```

**Enables simple mappings:**
```yaml
cellMappings:
  Dependent1FirstName: "dependent1.firstName"   # Instead of complex path
```

---

## Composition Rules

### Deep Merge Behavior

**Base:**
```yaml
cellMappings:
  A1: "field1"
  A2: "field2"
```

**Component:**
```yaml
cellMappings:
  A2: "field2_override"   # Overrides base
  A3: "field3"            # Adds new
```

**Result:**
```yaml
cellMappings:
  A1: "field1"            # From base
  A2: "field2_override"   # Overridden
  A3: "field3"            # Added
```

### List Merge Behavior

Lists are **replaced**, not merged:
```yaml
# Base
tableMappings:
  - sheetName: "Sheet1"

# Component
tableMappings:
  - sheetName: "Sheet2"

# Result: Only Sheet2 (component replaces base)
```

---

## API Usage Examples

### With Config File
```bash
curl -X POST http://localhost:8080/api/excel/generate-from-config \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "excel/enrollment-summary-excel.yml",
    "payload": {...}
  }' \
  -o output.xlsx
```

### Legacy (Direct Mappings in Request)
```bash
curl -X POST http://localhost:8080/api/excel/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templatePath": "template.xlsx",
    "cellMappings": {...},
    "payload": {...}
  }' \
  -o output.xlsx
```

---

## Best Practices

### ✅ DO:
1. **Use YAML configs** for production
2. **Use named ranges** in Excel templates
3. **Use composition** to avoid duplication
4. **Use preprocessing** for complex structures
5. **Version your configs** (use `version` field)
6. **Document configs** (use `description` field)

### ❌ DON'T:
1. Don't hardcode mappings in requests
2. Don't duplicate mappings across configs
3. Don't use complex filter paths (preprocess instead)
4. Don't mix cell references and named ranges
5. Don't forget to handle missing data

---

## Troubleshooting

### Config Not Found
```
Error: Config file not found: excel/my-config.yml
```
**Solution:** Check file path in `config-repo/excel/` directory

### Template Not Found
```
Error: Template not found: my-template.xlsx
```
**Solution:** Ensure template exists in `config-repo/excel-templates/` or configured path

### Cell Not Found
```
Warning: Cell not found: InvalidName
```
**Solution:** Verify named range exists in Excel template or use correct cell reference

### Missing Data
```
Warning: Field not found in payload: dependent4.firstName
```
**Solution:** Normal - cell remains empty if data missing

---

## Migration Guide

### From Request-Based to Config-Based

**Before:**
```json
{
  "templatePath": "enrollment-summary.xlsx",
  "preprocessingRules": "standard-enrollment-rules.yml",
  "cellMappings": {
    "ApplicationId": "applicationId",
    "PrimaryFirstName": "primary.firstName",
    ... 50+ lines ...
  },
  "payload": {...}
}
```

**After:**

1. Create config file: `config-repo/excel/enrollment-summary-excel.yml`
```yaml
templatePath: "enrollment-summary.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"
cellMappings:
  ApplicationId: "applicationId"
  PrimaryFirstName: "primary.firstName"
  ... (all mappings here)
```

2. Update request:
```json
{
  "configName": "excel/enrollment-summary-excel.yml",
  "payload": {...}
}
```

3. Update endpoint:
```
POST /api/excel/generate-from-config
```

---

## Summary

**Configuration-based Excel generation provides:**
- ✅ Clean separation of config and data
- ✅ Version-controlled configurations
- ✅ Reusable components via composition
- ✅ Same preprocessing support as PDF
- ✅ Same path resolution as PDF
- ✅ Backward compatible with request-based approach

**Use YAML configs for production, request-based for quick testing only.**
