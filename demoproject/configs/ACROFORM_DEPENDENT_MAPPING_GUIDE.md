# AcroForm Dependent Mapping Guide

## Overview

When using AcroForm-based PDF templates with dependent applicants, you have **two approaches** depending on whether you use preprocessing or direct mapping.

---

## Approach 1: Direct Mapping (No Preprocessing)

Map directly to the original nested payload structure using filter and index syntax.

### YAML Configuration:
```yaml
pdfMerge:
  sections:
    - name: enrollment-application
      type: acroform
      template: enrollment-form.pdf
      fieldMapping:
        # DEPENDENT 1 - Use [relationship=DEPENDENT][0]
        "Dependent1_FirstName": "application.applicants[relationship=DEPENDENT][0].firstName"
        "Dependent1_LastName": "application.applicants[relationship=DEPENDENT][0].lastName"
        "Dependent1_DOB": "application.applicants[relationship=DEPENDENT][0].dateOfBirth"
        "Dependent1_SSN": "application.applicants[relationship=DEPENDENT][0].ssn"
        
        # DEPENDENT 2 - Use [relationship=DEPENDENT][1]
        "Dependent2_FirstName": "application.applicants[relationship=DEPENDENT][1].firstName"
        "Dependent2_LastName": "application.applicants[relationship=DEPENDENT][1].lastName"
        "Dependent2_DOB": "application.applicants[relationship=DEPENDENT][1].dateOfBirth"
        "Dependent2_SSN": "application.applicants[relationship=DEPENDENT][1].ssn"
        
        # DEPENDENT 3 - Use [relationship=DEPENDENT][2]
        "Dependent3_FirstName": "application.applicants[relationship=DEPENDENT][2].firstName"
        "Dependent3_LastName": "application.applicants[relationship=DEPENDENT][2].lastName"
        "Dependent3_DOB": "application.applicants[relationship=DEPENDENT][2].dateOfBirth"
        "Dependent3_SSN": "application.applicants[relationship=DEPENDENT][2].ssn"
```

### How It Works:

**AcroFormFillService.resolveValue()** processes the path:

1. **Filter Phase:** `applicants[relationship=DEPENDENT]`
   - Filters array to get only dependents
   - Returns: `[{dependent1}, {dependent2}, {dependent3}, {dependent4}]`

2. **Index Phase:** `[0]`, `[1]`, `[2]`
   - Selects specific dependent by position
   - `[0]` = first dependent
   - `[1]` = second dependent
   - `[2]` = third dependent

3. **Property Access:** `.firstName`
   - Extracts the specific field

### Input Payload:
```json
{
  "application": {
    "applicants": [
      {"applicantId": "A001", "relationship": "PRIMARY", "firstName": "John"},
      {"applicantId": "A002", "relationship": "SPOUSE", "firstName": "Jane"},
      {"applicantId": "A003", "relationship": "DEPENDENT", "firstName": "Child1"},
      {"applicantId": "A004", "relationship": "DEPENDENT", "firstName": "Child2"},
      {"applicantId": "A005", "relationship": "DEPENDENT", "firstName": "Child3"},
      {"applicantId": "A006", "relationship": "DEPENDENT", "firstName": "Child4"}
    ]
  }
}
```

### Result:
```
PDF Field "Dependent1_FirstName" = "Child1"
PDF Field "Dependent2_FirstName" = "Child2"
PDF Field "Dependent3_FirstName" = "Child3"
```

**Note:** Dependent 4 is NOT mapped (only first 3 fit on main form)

---

## Approach 2: Preprocessed Mapping (Recommended) ⭐

Use `ConfigurablePayloadPreProcessor` with `mode: "indexed"` to create flat structure first, then use simple dot notation for mapping.

### Step 1: Preprocessing Rules
**File:** `standard-enrollment-rules.yml`

```yaml
arrayFilters:
  # Extract first 3 dependents with overflow handling
  - sourcePath: "application.applicants"
    filterField: "relationship"
    filterValue: "DEPENDENT"
    targetKey: "dependent"
    mode: "indexed"      # Creates dependent1, dependent2, dependent3
    maxItems: 3          # Limit to 3, rest go to overflow
```

### Step 2: Preprocessed Payload Structure

**Original:**
```json
{
  "application": {
    "applicants": [
      {"relationship": "DEPENDENT", "firstName": "Child1", "age": 16},
      {"relationship": "DEPENDENT", "firstName": "Child2", "age": 14},
      {"relationship": "DEPENDENT", "firstName": "Child3", "age": 12},
      {"relationship": "DEPENDENT", "firstName": "Child4", "age": 10}
    ]
  }
}
```

**After Preprocessing:**
```json
{
  "applicationId": "APP-12345",
  "primary": {...},
  "spouse": {...},
  "dependent1": {"relationship": "DEPENDENT", "firstName": "Child1", "age": 16},
  "dependent2": {"relationship": "DEPENDENT", "firstName": "Child2", "age": 14},
  "dependent3": {"relationship": "DEPENDENT", "firstName": "Child3", "age": 12},
  "dependentOverflow": [
    {"relationship": "DEPENDENT", "firstName": "Child4", "age": 10}
  ],
  "billing": {...},
  "mailing": {...},
  "medical": {...}
}
```

### Step 3: Simple Field Mapping

**File:** `acroform-enrollment-config.yml`

```yaml
pdfMerge:
  sections:
    - name: enrollment-application
      type: acroform
      template: enrollment-form.pdf
      fieldMapping:
        # Application level
        "ApplicationId": "applicationId"
        "SubmittedDate": "submittedDate"
        
        # Primary applicant (simple dot notation)
        "Primary_FirstName": "primary.firstName"
        "Primary_LastName": "primary.lastName"
        "Primary_DOB": "primary.dateOfBirth"
        "Primary_SSN": "primary.ssn"
        
        # Spouse applicant
        "Spouse_FirstName": "spouse.firstName"
        "Spouse_LastName": "spouse.lastName"
        "Spouse_DOB": "spouse.dateOfBirth"
        
        # DEPENDENT 1 (simple dot notation!)
        "Dependent1_FirstName": "dependent1.firstName"
        "Dependent1_LastName": "dependent1.lastName"
        "Dependent1_DOB": "dependent1.dateOfBirth"
        "Dependent1_SSN": "dependent1.ssn"
        "Dependent1_Age": "dependent1.age"
        
        # DEPENDENT 2
        "Dependent2_FirstName": "dependent2.firstName"
        "Dependent2_LastName": "dependent2.lastName"
        "Dependent2_DOB": "dependent2.dateOfBirth"
        "Dependent2_SSN": "dependent2.ssn"
        "Dependent2_Age": "dependent2.age"
        
        # DEPENDENT 3
        "Dependent3_FirstName": "dependent3.firstName"
        "Dependent3_LastName": "dependent3.lastName"
        "Dependent3_DOB": "dependent3.dateOfBirth"
        "Dependent3_SSN": "dependent3.ssn"
        "Dependent3_Age": "dependent3.age"
        
        # Overflow indicator
        "AdditionalDependentCount": "additionalDependentCount"
        
        # Billing address
        "Billing_Street": "billing.street"
        "Billing_City": "billing.city"
        "Billing_State": "billing.state"
        "Billing_ZipCode": "billing.zipCode"
        
        # Mailing address
        "Mailing_Street": "mailing.street"
        "Mailing_City": "mailing.city"
        
        # Medical plan
        "Medical_PlanName": "medical.planName"
        "Medical_Premium": "medical.monthlyPremium"
        
        # Dental plan
        "Dental_PlanName": "dental.planName"
        "Dental_Premium": "dental.monthlyPremium"
```

### Result:
```
PDF Field "Dependent1_FirstName" = "Child1"
PDF Field "Dependent2_FirstName" = "Child2"
PDF Field "Dependent3_FirstName" = "Child3"
PDF Field "AdditionalDependentCount" = "1"
```

**Advantages:**
- ✅ Much simpler field paths (`dependent1.firstName` vs `application.applicants[relationship=DEPENDENT][0].firstName`)
- ✅ Automatic overflow handling (`dependentOverflow` array)
- ✅ Calculated fields available (`additionalDependentCount`)
- ✅ Consistent structure across templates
- ✅ Easier to maintain
- ✅ Multi-client support via YAML rules

---

## Comparison: Direct vs Preprocessed

| Aspect | Direct Mapping | Preprocessed Mapping ⭐ |
|--------|----------------|------------------------|
| **Field Path** | `application.applicants[relationship=DEPENDENT][0].firstName` | `dependent1.firstName` |
| **Complexity** | High (filter + index syntax) | Low (simple dot notation) |
| **Overflow** | Manual handling | Automatic (`dependentOverflow`) |
| **Calculated Fields** | Not available | Available (`additionalDependentCount`) |
| **Multi-Client** | Hardcoded paths | YAML rules per client |
| **Performance** | Filters at render time | Pre-filtered once |
| **Maintenance** | Harder (complex paths) | Easier (simple paths) |

---

## Handling Missing Dependents

### Problem:
What if only 1 dependent exists but PDF has 3 fields?

### Solution 1: Direct Mapping
```yaml
# PDF has Dependent1, Dependent2, Dependent3 fields
# If only 1 dependent exists:
"Dependent1_FirstName": "application.applicants[relationship=DEPENDENT][0].firstName"  # ✅ "Child1"
"Dependent2_FirstName": "application.applicants[relationship=DEPENDENT][1].firstName"  # ❌ null (index out of bounds)
"Dependent3_FirstName": "application.applicants[relationship=DEPENDENT][2].firstName"  # ❌ null
```

**AcroFormFillService** handles this gracefully:
- Returns `null` for out-of-bounds indices
- Skips filling field if value is `null`
- PDF field remains empty

### Solution 2: Preprocessed Mapping
```yaml
# After preprocessing with 1 dependent:
{
  "dependent1": {"firstName": "Child1"},
  // dependent2 does not exist
  // dependent3 does not exist
}
```

Field mappings:
```yaml
"Dependent1_FirstName": "dependent1.firstName"  # ✅ "Child1"
"Dependent2_FirstName": "dependent2.firstName"  # ❌ null (key doesn't exist)
"Dependent3_FirstName": "dependent3.firstName"  # ❌ null
```

**Result:** Empty fields for dependent2 and dependent3 ✅

---

## Advanced: Conditional Dependent Filtering

### Use Case: Only map adult dependents (18+) to main form

**Preprocessing Rules:**
```yaml
arrayFilters:
  # Adult dependents only (18+)
  - sourcePath: "application.applicants"
    conditions:
      - field: "relationship"
        operator: "equals"
        value: "DEPENDENT"
      - field: "age"
        operator: "greaterThanOrEqual"
        value: 18
    conditionLogic: "AND"
    targetKey: "adultDependent"
    mode: "indexed"
    maxItems: 3
  
  # Minor dependents for separate section
  - sourcePath: "application.applicants"
    conditions:
      - field: "relationship"
        operator: "equals"
        value: "DEPENDENT"
      - field: "age"
        operator: "lessThan"
        value: 18
    conditionLogic: "AND"
    targetKey: "minorDependent"
    mode: "indexed"
    maxItems: 5
```

**Field Mapping:**
```yaml
fieldMapping:
  # Adult Dependents Section
  "AdultDependent1_FirstName": "adultDependent1.firstName"
  "AdultDependent1_Age": "adultDependent1.age"
  "AdultDependent2_FirstName": "adultDependent2.firstName"
  "AdultDependent2_Age": "adultDependent2.age"
  
  # Minor Dependents Section
  "MinorDependent1_FirstName": "minorDependent1.firstName"
  "MinorDependent1_Age": "minorDependent1.age"
  "MinorDependent2_FirstName": "minorDependent2.firstName"
  "MinorDependent2_Age": "minorDependent2.age"
```

---

## Complete Example: Enrollment Form with Overflow

### Input Payload (5 Dependents):
```json
{
  "application": {
    "applicants": [
      {"applicantId": "A001", "relationship": "PRIMARY", "firstName": "John"},
      {"applicantId": "A002", "relationship": "SPOUSE", "firstName": "Jane"},
      {"applicantId": "A003", "relationship": "DEPENDENT", "firstName": "Child1", "age": 19},
      {"applicantId": "A004", "relationship": "DEPENDENT", "firstName": "Child2", "age": 17},
      {"applicantId": "A005", "relationship": "DEPENDENT", "firstName": "Child3", "age": 15},
      {"applicantId": "A006", "relationship": "DEPENDENT", "firstName": "Child4", "age": 13},
      {"applicantId": "A007", "relationship": "DEPENDENT", "firstName": "Child5", "age": 11}
    ]
  }
}
```

### Preprocessing (mode: indexed, maxItems: 3):
```json
{
  "primary": {"firstName": "John"},
  "spouse": {"firstName": "Jane"},
  "dependent1": {"firstName": "Child1", "age": 19},
  "dependent2": {"firstName": "Child2", "age": 17},
  "dependent3": {"firstName": "Child3", "age": 15},
  "dependentOverflow": [
    {"firstName": "Child4", "age": 13},
    {"firstName": "Child5", "age": 11}
  ],
  "dependentCount": 5,
  "additionalDependentCount": 2
}
```

### Main Form Field Mapping:
```yaml
sections:
  # Main enrollment form (first 3 dependents)
  - name: enrollment-application
    type: acroform
    template: enrollment-form.pdf
    fieldMapping:
      "Primary_FirstName": "primary.firstName"
      "Spouse_FirstName": "spouse.firstName"
      "Dependent1_FirstName": "dependent1.firstName"
      "Dependent1_Age": "dependent1.age"
      "Dependent2_FirstName": "dependent2.firstName"
      "Dependent2_Age": "dependent2.age"
      "Dependent3_FirstName": "dependent3.firstName"
      "Dependent3_Age": "dependent3.age"
      "DependentCount": "dependentCount"
      "AdditionalDependentsNote": "additionalDependentCount"
```

### Main Form Result:
```
Primary_FirstName = "John"
Spouse_FirstName = "Jane"
Dependent1_FirstName = "Child1"
Dependent1_Age = "19"
Dependent2_FirstName = "Child2"
Dependent2_Age = "17"
Dependent3_FirstName = "Child3"
Dependent3_Age = "15"
DependentCount = "5"
AdditionalDependentsNote = "2"  ← Shows "2 additional dependents on addendum"
```

### Addendum Template (FreeMarker):
```yaml
  # Addendum page for overflow dependents
  - name: additional-dependents
    type: freemarker
    template: dependents-addendum.ftl
    enabled: ${dependentOverflow?? && (dependentOverflow?size > 0)}
```

**File:** `dependents-addendum.ftl`
```freemarker
<h1>Additional Dependents</h1>

<table>
  <tr>
    <th>Name</th>
    <th>Age</th>
  </tr>
  <#list dependentOverflow as dep>
  <tr>
    <td>${dep.firstName}</td>
    <td>${dep.age}</td>
  </tr>
  </#list>
</table>
```

### Addendum Result:
```
Additional Dependents
┌─────────┬─────┐
│ Name    │ Age │
├─────────┼─────┤
│ Child4  │ 13  │
│ Child5  │ 11  │
└─────────┴─────┘
```

---

## Best Practices

### ✅ DO:
1. **Use preprocessing** for complex structures
2. **Use `mode: indexed`** with `maxItems` for overflow handling
3. **Map overflow to FreeMarker** addendum pages
4. **Use simple dot notation** in field mappings
5. **Add calculated fields** (counts, flags) in preprocessing
6. **Test with missing dependents** (0, 1, 2, 3, 4+ dependents)

### ❌ DON'T:
1. Don't use complex filter syntax in field mappings
2. Don't hardcode dependent limits in templates
3. Don't ignore overflow scenarios
4. Don't mix preprocessing and direct mapping approaches
5. Don't forget to handle null values gracefully

---

## Summary

**For AcroForm PDF templates with dependents:**

1. **Preprocessing** transforms:
   ```
   applicants[5 items] → dependent1, dependent2, dependent3, dependentOverflow[2]
   ```

2. **Field Mapping** uses simple paths:
   ```
   "Dependent1_FirstName": "dependent1.firstName"
   ```

3. **Overflow** handled automatically:
   - First 3 → Main form (AcroForm)
   - Remaining → Addendum page (FreeMarker)

4. **Missing dependents** gracefully skip empty fields

5. **Advanced filtering** supports age-based, status-based, or custom criteria

**Result:** Clean, maintainable, multi-client-ready AcroForm field mappings! ✨
