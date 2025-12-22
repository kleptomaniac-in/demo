# Excel Template Generation Guide

## Overview

Excel template support mirrors the PDF template system with two main approaches:

1. **Cell Mapping Approach** (similar to AcroForm PDFs)
   - Map cell references (A1, B2) or named ranges to payload paths
   - Best for forms, summary sheets, single-record reports

2. **Table/List Approach** (for repeating data)
   - Populate multiple rows from array data
   - Best for lists, grids, tabular data

---

## Architecture Comparison

| Feature | PDF Templates | Excel Templates |
|---------|---------------|-----------------|
| **Simple Mapping** | AcroForm fields | Cell references (A1, B2) or Named ranges |
| **Repeating Data** | FreeMarker loops | Table mappings (row population) |
| **Preprocessing** | ✅ Supported | ✅ Supported |
| **Path Resolution** | `member.name`, `items[0].value` | ✅ Same syntax |
| **Filtering** | `items[status=ACTIVE]` | ✅ Same syntax |
| **Composition** | ✅ Via FlexiblePdfMergeService | ⚠️ Manual (multiple sheets) |

---

## **NEW: YAML Configuration Approach (Recommended)** ⭐

### Overview

Just like PDF templates, Excel templates now support **YAML configuration files**. This keeps configuration separate from data and enables:

- ✅ Configuration versioning
- ✅ Reusability across requests
- ✅ Composition (base + components)
- ✅ Easier maintenance
- ✅ No hardcoded mappings in requests

### YAML Configuration Structure

**File:** `config-repo/excel/enrollment-summary-excel.yml`

```yaml
description: "Enrollment Summary Excel - Simple Form"
version: "1.0"

templatePath: "enrollment-summary.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"

cellMappings:
  # Application Info
  ApplicationId: "applicationId"
  SubmittedDate: "submittedDate"
  
  # Primary Applicant
  PrimaryFirstName: "primary.firstName"
  PrimaryLastName: "primary.lastName"
  PrimaryDOB: "primary.dateOfBirth"
  
  # Dependents
  Dependent1FirstName: "dependent1.firstName"
  Dependent2FirstName: "dependent2.firstName"
  
  # Products
  MedicalPlanName: "medical.planName"
  DentalPlanName: "dental.planName"
```

### API Request (Much Simpler!)

**Endpoint:** `POST /api/excel/generate-from-config`

```json
{
  "configName": "excel/enrollment-summary-excel.yml",
  "payload": {
    "application": {
      "applicationId": "APP-12345",
      "applicants": [...],
      "proposedProducts": [...]
    }
  }
}
```

**Before (Old Way):**
```json
{
  "templatePath": "enrollment-summary.xlsx",
  "preprocessingRules": "standard-enrollment-rules.yml",
  "cellMappings": {
    "ApplicationId": "applicationId",
    "PrimaryFirstName": "primary.firstName",
    ... 50+ lines of mappings ...
  },
  "payload": {...}
}
```

**After (New Way):**
```json
{
  "configName": "excel/enrollment-summary-excel.yml",
  "payload": {...}
}
```

### Configuration with Table Mappings

**File:** `config-repo/excel/dependent-list-excel.yml`

```yaml
description: "Dependent List Excel - Table Format"
templatePath: "dependent-list.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"

cellMappings:
  ApplicationId: "applicationId"
  TotalDependents: "dependentCount"

tableMappings:
  - sheetName: "Dependents"
    startRow: 1
    sourcePath: "allDependents"
    columnMappings:
      0: "firstName"
      1: "lastName"
      2: "dateOfBirth"
      3: "ssn"
      4: "age"
```

### Composition Support (Like PDF)

**Base Component:** `excel/components/base-excel.yml`
```yaml
cellMappings:
  ApplicationId: "applicationId"
  PrimaryFirstName: "primary.firstName"
  PrimaryLastName: "primary.lastName"
```

**Dependent Component:** `excel/components/dependents-excel.yml`
```yaml
cellMappings:
  Dependent1FirstName: "dependent1.firstName"
  Dependent2FirstName: "dependent2.firstName"
  Dependent3FirstName: "dependent3.firstName"
```

**Composed Configuration:** `excel/enrollment-summary-composed-excel.yml`
```yaml
composition:
  base: "excel/components/base-excel.yml"
  components:
    - "excel/components/dependents-excel.yml"
    - "excel/components/medical-excel.yml"

templatePath: "enrollment-summary.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"

# Additional mappings (merged with base + components)
cellMappings:
  DentalPlanName: "dental.planName"
  TotalMonthlyPremium: "totalMonthlyPremium"
```

**Result:** Base + components + overrides are deep-merged automatically!

### Testing with Config

```bash
# Simple cell mappings
curl -X POST http://localhost:8080/api/excel/generate-from-config \
  -H "Content-Type: application/json" \
  -d @request-excel-from-config.json \
  -o output.xlsx

# Table mappings
curl -X POST http://localhost:8080/api/excel/generate-from-config \
  -H "Content-Type: application/json" \
  -d @request-excel-table-from-config.json \
  -o output.xlsx

# Composed configuration
curl -X POST http://localhost:8080/api/excel/generate-from-config \
  -H "Content-Type: application/json" \
  -d @request-excel-composed-from-config.json \
  -o output.xlsx
```

---

## Approach 1: Cell Mapping (Key-Value Style)

### Use Cases:
- Enrollment summary forms
- Invoice templates
- Single-applicant reports
- Fixed-layout forms

### Example Template Structure

**Excel Template:** `enrollment-summary.xlsx`

```
┌─────────┬────────────────────────────────┐
│   A     │       B        │       C       │
├─────────┼────────────────┼───────────────┤
│ 1  │ Application ID:  │ [APP_ID]     │  ← Cell B1
│ 2  │ Primary Name:    │ [FIRST]      │  ← Cell B2
│ 3  │                  │ [LAST]       │  ← Cell C2
│ 4  │ DOB:             │ [DOB]        │  ← Cell B4
│ 5  │ Spouse Name:     │ [SP_FIRST]   │
│ 6  │ Dependent 1:     │ [DEP1_NAME]  │
│ 7  │ Dependent 2:     │ [DEP2_NAME]  │
│ 8  │ Medical Plan:    │ [MED_PLAN]   │
│ 9  │ Premium:         │ [PREMIUM]    │
└─────────┴────────────────┴───────────────┘
```

### API Request

**Endpoint:** `POST /api/excel/generate`

```json
{
  "templatePath": "enrollment-summary.xlsx",
  "cellMappings": {
    "B1": "applicationId",
    "B2": "primary.firstName",
    "C2": "primary.lastName",
    "B4": "primary.dateOfBirth",
    "B5": "spouse.firstName",
    "B6": "dependent1.firstName",
    "B7": "dependent2.firstName",
    "B8": "medical.planName",
    "B9": "medical.monthlyPremium"
  },
  "payload": {
    "applicationId": "APP-12345",
    "primary": {
      "firstName": "John",
      "lastName": "Doe",
      "dateOfBirth": "1980-05-15"
    },
    "spouse": {
      "firstName": "Jane"
    },
    "dependent1": {
      "firstName": "Child1"
    },
    "dependent2": {
      "firstName": "Child2"
    },
    "medical": {
      "planName": "Platinum PPO",
      "monthlyPremium": 650.00
    }
  }
}
```

### Result

```
Application ID:  APP-12345
Primary Name:    John Doe
DOB:             05/15/1980
Spouse Name:     Jane
Dependent 1:     Child1
Dependent 2:     Child2
Medical Plan:    Platinum PPO
Premium:         $650.00
```

---

## Approach 2: With Preprocessing

### Use Case: Complex nested structures need flattening first

**Endpoint:** `POST /api/excel/generate-with-preprocessing`

```json
{
  "templatePath": "enrollment-summary.xlsx",
  "preprocessingRules": "standard-enrollment-rules.yml",
  "cellMappings": {
    "B1": "applicationId",
    "B2": "primary.firstName",
    "C2": "primary.lastName",
    "B6": "dependent1.firstName",
    "B7": "dependent2.firstName",
    "B8": "dependent3.firstName",
    "B10": "medical.planName",
    "B11": "dental.planName"
  },
  "payload": {
    "application": {
      "applicationId": "APP-12345",
      "applicants": [
        {"relationship": "PRIMARY", "firstName": "John", "lastName": "Doe"},
        {"relationship": "SPOUSE", "firstName": "Jane"},
        {"relationship": "DEPENDENT", "firstName": "Child1"},
        {"relationship": "DEPENDENT", "firstName": "Child2"},
        {"relationship": "DEPENDENT", "firstName": "Child3"}
      ],
      "proposedProducts": [
        {"productType": "MEDICAL", "planName": "Platinum PPO"},
        {"productType": "DENTAL", "planName": "Gold Dental"}
      ]
    }
  }
}
```

**Preprocessing automatically creates:**
```json
{
  "applicationId": "APP-12345",
  "primary": {"firstName": "John", "lastName": "Doe"},
  "spouse": {"firstName": "Jane"},
  "dependent1": {"firstName": "Child1"},
  "dependent2": {"firstName": "Child2"},
  "dependent3": {"firstName": "Child3"},
  "medical": {"planName": "Platinum PPO"},
  "dental": {"planName": "Gold Dental"}
}
```

Then simple paths work: `dependent1.firstName` instead of `application.applicants[relationship=DEPENDENT][0].firstName`

---

## Approach 3: Table/List Data (Repeating Rows)

### Use Cases:
- Dependent lists
- Coverage history
- Product comparisons
- Transaction logs

### Example Template Structure

**Excel Template:** `dependent-list.xlsx`

**Sheet: "Dependents"**
```
┌────┬─────────────┬────────────┬──────────────┬─────────┐
│ A  │      B      │      C     │      D       │    E    │
├────┼─────────────┼────────────┼──────────────┼─────────┤
│ 1  │ First Name  │ Last Name  │ Date of Birth│   SSN   │  ← Header row
│ 2  │ [DATA ROW]  │            │              │         │  ← Start row (template)
│ 3  │             │            │              │         │
│ 4  │             │            │              │         │
└────┴─────────────┴────────────┴──────────────┴─────────┘
```

### API Request

**Endpoint:** `POST /api/excel/generate-with-tables`

```json
{
  "templatePath": "dependent-list.xlsx",
  "tableMappings": [
    {
      "sheetName": "Dependents",
      "startRow": 1,
      "sourcePath": "allDependents",
      "columnMappings": {
        "0": "firstName",
        "1": "lastName",
        "2": "dateOfBirth",
        "3": "ssn"
      }
    }
  ],
  "payload": {
    "allDependents": [
      {"firstName": "Child1", "lastName": "Doe", "dateOfBirth": "2010-03-15", "ssn": "123-45-6789"},
      {"firstName": "Child2", "lastName": "Doe", "dateOfBirth": "2012-07-20", "ssn": "234-56-7890"},
      {"firstName": "Child3", "lastName": "Doe", "dateOfBirth": "2015-11-10", "ssn": "345-67-8901"},
      {"firstName": "Child4", "lastName": "Doe", "dateOfBirth": "2018-02-28", "ssn": "456-78-9012"}
    ]
  }
}
```

### Result

```
┌─────────────┬────────────┬──────────────┬─────────────┐
│ First Name  │ Last Name  │ Date of Birth│   SSN       │
├─────────────┼────────────┼──────────────┼─────────────┤
│ Child1      │ Doe        │ 03/15/2010   │ 123-45-6789 │
│ Child2      │ Doe        │ 07/20/2012   │ 234-56-7890 │
│ Child3      │ Doe        │ 11/10/2015   │ 345-67-8901 │
│ Child4      │ Doe        │ 02/28/2018   │ 456-78-9012 │
└─────────────┴────────────┴──────────────┴─────────────┘
```

---

## Approach 4: Combined (Preprocessing + Tables)

### Use Case: Complex nested structure with repeating data

**Endpoint:** `POST /api/excel/generate-complete`

```json
{
  "templatePath": "enrollment-report.xlsx",
  "preprocessingRules": "standard-enrollment-rules.yml",
  "tableMappings": [
    {
      "sheetName": "Summary",
      "startRow": 10,
      "sourcePath": "allDependents",
      "columnMappings": {
        "1": "firstName",
        "2": "lastName",
        "3": "age"
      }
    }
  ],
  "payload": {
    "application": {
      "applicationId": "APP-12345",
      "applicants": [
        {"relationship": "PRIMARY", "firstName": "John"},
        {"relationship": "SPOUSE", "firstName": "Jane"},
        {"relationship": "DEPENDENT", "firstName": "Child1", "age": 15},
        {"relationship": "DEPENDENT", "firstName": "Child2", "age": 13},
        {"relationship": "DEPENDENT", "firstName": "Child3", "age": 11}
      ]
    }
  }
}
```

**Processing:**
1. **Preprocessing** transforms nested structure → flat structure with `allDependents` array
2. **Table mapping** populates rows 10-12 with dependent data

---

## Named Ranges (Recommended for Maintenance)

Instead of cell references like `A1`, `B2`, use Excel's **Named Ranges** feature:

### In Excel:
1. Select cell B1
2. Name it: `ApplicationId` (in Name Box)
3. Select cell B2, name it: `PrimaryFirstName`

### In API Request:
```json
{
  "cellMappings": {
    "ApplicationId": "applicationId",
    "PrimaryFirstName": "primary.firstName",
    "PrimaryLastName": "primary.lastName",
    "SpouseFirstName": "spouse.firstName"
  }
}
```

**Benefits:**
- ✅ More readable
- ✅ Template can be redesigned without changing mappings
- ✅ Self-documenting

---

## Path Resolution (Same as PDF/AcroForm)

The Excel service uses the **same path resolution** as AcroForm PDFs:

### Simple Property Access
```json
"cellMappings": {
  "A1": "memberName"           // payload.memberName
}
```

### Nested Objects
```json
"cellMappings": {
  "A1": "member.firstName"     // payload.member.firstName
}
```

### Array Index
```json
"cellMappings": {
  "A1": "dependents[0].name"   // First dependent
}
```

### Array Filter
```json
"cellMappings": {
  "A1": "applicants[relationship=PRIMARY].firstName"
}
```

### Multiple Filters
```json
"cellMappings": {
  "A1": "coverages[applicantId=A001][productType=MEDICAL].carrierName"
}
```

---

## Complete Example: Enrollment Summary with Dependents

### Template: `enrollment-complete.xlsx`

**Sheet 1: Summary**
- Cell mappings for PRIMARY, SPOUSE, up to 3 dependents
- Product information
- Calculated totals

**Sheet 2: All Dependents**
- Table with all dependents (4+)

### API Request

```json
{
  "templatePath": "enrollment-complete.xlsx",
  "preprocessingRules": "standard-enrollment-rules.yml",
  "tableMappings": [
    {
      "sheetName": "All Dependents",
      "startRow": 1,
      "sourcePath": "allDependents",
      "columnMappings": {
        "0": "firstName",
        "1": "lastName",
        "2": "dateOfBirth",
        "3": "ssn",
        "4": "relationship"
      }
    }
  ],
  "payload": {
    "application": {
      "applicationId": "APP-12345",
      "submittedDate": "2025-12-15",
      "applicants": [
        {"relationship": "PRIMARY", "firstName": "John", "lastName": "Doe"},
        {"relationship": "SPOUSE", "firstName": "Jane", "lastName": "Doe"},
        {"relationship": "DEPENDENT", "firstName": "Child1", "lastName": "Doe", "dateOfBirth": "2010-03-15", "ssn": "123-45-6789"},
        {"relationship": "DEPENDENT", "firstName": "Child2", "lastName": "Doe", "dateOfBirth": "2012-07-20", "ssn": "234-56-7890"},
        {"relationship": "DEPENDENT", "firstName": "Child3", "lastName": "Doe", "dateOfBirth": "2015-11-10", "ssn": "345-67-8901"},
        {"relationship": "DEPENDENT", "firstName": "Child4", "lastName": "Doe", "dateOfBirth": "2018-02-28", "ssn": "456-78-9012"},
        {"relationship": "DEPENDENT", "firstName": "Child5", "lastName": "Doe", "dateOfBirth": "2020-06-05", "ssn": "567-89-0123"}
      ],
      "proposedProducts": [
        {"productType": "MEDICAL", "planName": "Platinum PPO", "monthlyPremium": 650.00},
        {"productType": "DENTAL", "planName": "Gold Dental", "monthlyPremium": 45.00}
      ]
    }
  }
}
```

**Result:**

**Sheet 1 (Summary):**
```
Application ID: APP-12345
Submitted: 12/15/2025

Primary: John Doe
Spouse: Jane Doe
Dependent 1: Child1 Doe
Dependent 2: Child2 Doe
Dependent 3: Child3 Doe

Medical: Platinum PPO ($650.00)
Dental: Gold Dental ($45.00)

Total Dependents: 5
Additional (see sheet 2): 2
```

**Sheet 2 (All Dependents):**
```
┌────────────┬───────────┬───────────────┬─────────────┬─────────────┐
│ First Name │ Last Name │ Date of Birth │     SSN     │ Relationship│
├────────────┼───────────┼───────────────┼─────────────┼─────────────┤
│ Child1     │ Doe       │ 03/15/2010    │ 123-45-6789 │ DEPENDENT   │
│ Child2     │ Doe       │ 07/20/2012    │ 234-56-7890 │ DEPENDENT   │
│ Child3     │ Doe       │ 11/10/2015    │ 345-67-8901 │ DEPENDENT   │
│ Child4     │ Doe       │ 02/28/2018    │ 456-78-9012 │ DEPENDENT   │
│ Child5     │ Doe       │ 06/05/2020    │ 567-89-0123 │ DEPENDENT   │
└────────────┴───────────┴───────────────┴─────────────┴─────────────┘
```

---

## Type Conversion

Excel cells are typed. The service automatically converts:

| Payload Type | Excel Cell Type | Format |
|--------------|-----------------|--------|
| String | TEXT | As-is |
| Number | NUMERIC | 123.45 |
| Boolean | BOOLEAN | TRUE/FALSE |
| Date | DATE | MM/dd/yyyy |
| null | BLANK | (empty) |

---

## Best Practices

### ✅ DO:
1. **Use Named Ranges** for cell mappings (more maintainable)
2. **Use preprocessing** for complex nested structures
3. **Use table mappings** for repeating data (lists, grids)
4. **Test with missing data** (null values, empty arrays)
5. **Format template properly** (dates, currency, percentages)
6. **Keep one template per use case** (don't overload templates)

### ❌ DON'T:
1. Don't mix cell mapping and table mapping on same rows
2. Don't use complex filter paths (use preprocessing instead)
3. Don't hardcode dependent limits in templates
4. Don't forget overflow scenarios (4+ dependents)
5. Don't ignore Excel formula cells (service overwrites them)

---

## Comparison: When to Use PDF vs Excel

| Use Case | PDF | Excel |
|----------|-----|-------|
| **Forms** (for signing) | ✅ Best | ❌ Not suitable |
| **Reports** (read-only) | ✅ Good | ✅ Good |
| **Data analysis** | ❌ Not suitable | ✅ Best |
| **Lists/Grids** | ⚠️ Limited | ✅ Best |
| **Multi-page forms** | ✅ Best | ⚠️ Use multiple sheets |
| **Print-ready** | ✅ Best | ⚠️ Formatting varies |
| **Editable** | ❌ Static | ✅ Fully editable |
| **Calculations** | ❌ Static | ✅ Excel formulas work |

---

## API Endpoints Summary

| Endpoint | Purpose | Configuration | When to Use |
|----------|---------|---------------|-------------|
| `POST /api/excel/generate-from-config` ⭐ | **Use YAML config** | YAML file | **Recommended** - Clean, maintainable |
| `POST /api/excel/generate` | Simple cell mapping | In request | Quick testing, one-off exports |
| `POST /api/excel/generate-with-preprocessing` | Cell mapping + preprocessing | In request | Legacy support |
| `POST /api/excel/generate-with-tables` | Table/list data | In request | Legacy support |
| `POST /api/excel/generate-complete` | Preprocessing + tables | In request | Legacy support |

**Recommendation:** Use `/generate-from-config` for all production use cases. Keep mappings in YAML configs for version control and reusability.

---

## Dependencies Added

**pom.xml:**
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## Summary

**Excel template support provides:**
- ✅ Cell mapping (like AcroForm field mapping)
- ✅ Table population (repeating rows from arrays)
- ✅ Same path resolution as PDF templates
- ✅ Preprocessing support for complex structures
- ✅ Named range support for maintainability
- ✅ Type conversion (String, Number, Date, Boolean)
- ✅ Multiple sheet support

**Result:** Unified template system supporting both PDF and Excel with consistent API! 🎉
