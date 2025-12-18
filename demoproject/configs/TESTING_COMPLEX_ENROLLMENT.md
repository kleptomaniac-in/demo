# Testing Complex Enrollment with Pre-Processing

## Overview

This guide shows how to test the complex enrollment PDF generation with role-based filtering and overflow handling.

## Test Scenario

- **PRIMARY**: John Smith
- **SPOUSE**: Jane Smith
- **DEPENDENTs**: 4 children (Emily, Michael, Sarah, David)
- **BILLING** address in Los Angeles
- **MAILING** address in San Francisco
- **Products**: MEDICAL, DENTAL, VISION
- **Expected Output**: Main form with 3 dependents + Addendum page with 4th dependent

## Step 1: Start the Service

```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

## Step 2: Preview Flattened Payload

See how the pre-processor transforms the nested structure:

```bash
curl -X POST http://localhost:8080/api/enrollment-complex/preview-flattened \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  | jq .
```

**Expected Output:**
```json
{
  "applicationId": "APP-2025-12345",
  "primary": {
    "relationship": "PRIMARY",
    "demographic": {
      "firstName": "John",
      "lastName": "Smith",
      ...
    }
  },
  "spouse": {
    "relationship": "SPOUSE",
    "demographic": {
      "firstName": "Jane",
      ...
    }
  },
  "dependent1": { "demographic": { "firstName": "Emily" } },
  "dependent2": { "demographic": { "firstName": "Michael" } },
  "dependent3": { "demographic": { "firstName": "Sarah" } },
  "additionalDependents": [
    { "demographic": { "firstName": "David" } }
  ],
  "billing": { "street": "123 Main Street", "city": "Los Angeles" },
  "mailing": { "street": "456 Oak Avenue", "city": "San Francisco" },
  "medical": { "planName": "Gold PPO", "monthlyPremium": 450.00 },
  "dental": { "planName": "Premium Dental", "monthlyPremium": 85.00 },
  "vision": { "planName": "Basic Vision", "monthlyPremium": 25.00 },
  "hasSpouse": true,
  "dependentCount": 4,
  "primaryDependentCount": 3,
  "additionalDependentCount": 1
}
```

Notice how:
- Applicants separated by role (primary, spouse, dependent1-3)
- Addresses separated by type (billing, mailing)
- Products separated by type (medical, dental, vision)
- Overflow tracked (additionalDependentCount: 1)

## Step 3: Get Applicant Summary

```bash
curl -X POST http://localhost:8080/api/enrollment-complex/applicant-summary \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  | jq .
```

**Expected Output:**
```json
{
  "hasPrimary": true,
  "hasSpouse": true,
  "dependentCount": 4,
  "primaryDependentCount": 3,
  "additionalDependentCount": 1,
  "needsAddendum": true,
  "primaryName": "John Smith"
}
```

## Step 4: Generate PDF

```bash
curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  -o enrollment-with-overflow.pdf
```

**Expected Result:**
- Main PDF form with:
  - PRIMARY: John Smith (all fields filled)
  - SPOUSE: Jane Smith (all fields filled)
  - DEPENDENT 1: Emily Smith
  - DEPENDENT 2: Michael Smith
  - DEPENDENT 3: Sarah Smith
  - BILLING: 123 Main Street, Los Angeles, CA
  - MAILING: 456 Oak Avenue, San Francisco, CA
  - MEDICAL: Gold PPO ($450/month)
  - DENTAL: Premium Dental ($85/month)
  - VISION: Basic Vision ($25/month)

- Addendum page with:
  - DEPENDENT #4: David Smith (with all demographics)
  - Coverage table showing which products cover David

## Step 5: Test with Different Scenarios

### Scenario A: Only Primary (No Spouse, No Dependents)

Create `minimal-application.json`:
```json
{
  "application": {
    "applicationId": "APP-MIN-001",
    "applicants": [
      {
        "relationship": "PRIMARY",
        "demographic": {
          "firstName": "Alice",
          "lastName": "Johnson",
          "dateOfBirth": "01/01/1990"
        }
      }
    ],
    "addresses": [
      { "type": "BILLING", "street": "789 Elm St", "city": "Sacramento", "state": "CA" }
    ],
    "proposedProducts": [
      { "productType": "MEDICAL", "planName": "Bronze HMO" }
    ]
  }
}
```

Test:
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d @minimal-application.json \
  -o minimal-enrollment.pdf
```

**Expected**: Only PRIMARY fields filled, spouse/dependent fields empty, no addendum.

### Scenario B: Primary + Spouse + 2 Dependents (Within Limit)

Modify `complex-application-structure.json` to remove 2 dependents (keep only Emily and Michael).

**Expected**: Main form with 2 dependents filled, Dependent3 fields empty, no addendum.

### Scenario C: Primary + 10 Dependents (Large Overflow)

Add 6 more dependents to the JSON.

**Expected**: Main form with 3 dependents, addendum with 7 remaining dependents.

## Field Mapping Verification

### Check AcroForm PDF Fields

If you have the actual AcroForm PDF template, list its fields:

```bash
# Using PDFBox command line (if available)
java -jar pdfbox-app.jar GetFields enrollment-application-with-dependents.pdf
```

Or use the discovery endpoint (if implemented):
```bash
curl http://localhost:8080/api/acroform/fields?template=enrollment-application-with-dependents.pdf
```

### Expected Field Names in PDF

Based on the mapping in `preprocessed-enrollment-mapping.yml`:

**Primary Fields:**
- `Primary_FirstName`, `Primary_LastName`, `Primary_MiddleName`
- `Primary_DOB`, `Primary_SSN`, `Primary_Gender`
- `Primary_Email`, `Primary_Phone`

**Spouse Fields:**
- `Spouse_FirstName`, `Spouse_LastName`, etc.

**Dependent Fields:**
- `Dependent1_FirstName`, `Dependent1_LastName`, `Dependent1_DOB`, etc.
- `Dependent2_FirstName`, `Dependent2_LastName`, `Dependent2_DOB`, etc.
- `Dependent3_FirstName`, `Dependent3_LastName`, `Dependent3_DOB`, etc.

**Address Fields:**
- `Billing_Street`, `Billing_City`, `Billing_State`, `Billing_ZipCode`
- `Mailing_Street`, `Mailing_City`, `Mailing_State`, `Mailing_ZipCode`

**Product Fields:**
- `Medical_PlanName`, `Medical_Premium`, `Medical_Deductible`
- `Dental_PlanName`, `Dental_Premium`, `Dental_Deductible`
- `Vision_PlanName`, `Vision_Premium`, `Vision_Deductible`

## Troubleshooting

### Issue: Spouse fields not filling

**Check:**
1. Is there a SPOUSE in the applicants array?
   ```bash
   curl -X POST http://localhost:8080/api/enrollment-complex/applicant-summary ...
   ```
   Should show `"hasSpouse": true`

2. Verify payload has SPOUSE:
   ```json
   {
     "applicants": [
       { "relationship": "PRIMARY", ... },
       { "relationship": "SPOUSE", ... }  // Must exist
     ]
   }
   ```

### Issue: Only 2 dependents showing instead of 3

**Check:** Are there actually 3 DEPENDENTs in the array?

Preview flattened payload:
```bash
curl -X POST .../preview-flattened ...
```

Should show: `dependent1`, `dependent2`, `dependent3`

### Issue: No addendum generated for 4th dependent

**Verify:**
1. FreeMarker template exists: `additional-dependents-addendum.ftl`
2. Section is enabled in config:
   ```yaml
   - name: additional-dependents-addendum
     type: freemarker
     enabled: true  # Must be true
   ```
3. Original `application` structure passed to FreeMarker:
   ```java
   fullPayload.put("application", applicationData);
   ```

### Issue: Billing address showing in Mailing fields

**Check filter logic:**
```yaml
"Billing_Street": "billing.street"   # Pre-processed
"Mailing_Street": "mailing.street"   # Pre-processed
```

Pre-processor should separate by type:
```java
addresses.stream()
    .filter(a -> "BILLING".equals(a.get("type")))
    .findFirst()
    .ifPresent(billing -> flattened.put("billing", billing));
```

## Success Criteria

✅ Primary applicant fields filled correctly
✅ Spouse fields filled when spouse present
✅ First 3 dependents mapped to fixed slots
✅ 4th+ dependents appear in addendum
✅ BILLING and MAILING addresses separated correctly
✅ MEDICAL, DENTAL, VISION products mapped correctly
✅ Addendum only generated when needed (4+ dependents)
✅ Empty fields when optional data missing (e.g., no spouse)

## Performance Notes

**Pre-Processing Overhead:**
- Minimal: ~1-2ms for typical application with 6 applicants
- Saves complex filtering logic in AcroForm field resolution
- One-time cost at PDF generation time

**Alternative:** Cache flattened payload if generating multiple PDFs from same application.

## Summary

**Pre-Processing Approach:**
- ✅ Simple field mappings (`"Primary_FirstName": "primary.demographic.firstName"`)
- ✅ No complex filter syntax needed
- ✅ Better performance (one-time transformation)
- ✅ Easier debugging (preview flattened payload)
- ✅ Automatic overflow handling

**Result:** Clean, maintainable solution for complex enrollment structures.
