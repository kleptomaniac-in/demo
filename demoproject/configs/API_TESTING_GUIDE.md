# API Testing Guide - Complex Enrollment Endpoints

## Prerequisites

1. **Start the service:**
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

2. **Wait for startup** - Look for:
```
Started PdfGenerationServiceApplication in X seconds
```

3. **Test data file available:**
```bash
ls /workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json
```

## Available Endpoints

### 1. Preview Flattened Payload
**Endpoint:** `POST /api/enrollment-complex/preview-flattened`  
**Purpose:** See how pre-processor transforms nested arrays  
**Use Case:** Debug field mappings, verify data structure

### 2. Get Applicant Summary
**Endpoint:** `POST /api/enrollment-complex/applicant-summary`  
**Purpose:** Quick overview of applicants (PRIMARY, SPOUSE, dependents)  
**Use Case:** UI display, validation before PDF generation

### 3. Generate PDF
**Endpoint:** `POST /api/enrollment-complex/generate`  
**Purpose:** Generate complete enrollment PDF with overflow handling  
**Use Case:** Production PDF generation

---

## Test 1: Preview Flattened Payload

**What it does:** Shows how nested arrays get transformed into flat structure for easy field mapping.

### Command:
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/preview-flattened \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  | jq .
```

### Alternative (pretty print with Python):
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/preview-flattened \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  | python3 -m json.tool
```

### Expected Output:
```json
{
  "applicationId": "APP-2025-12345",
  "submittedDate": "12/12/2025",
  "effectiveDate": "01/01/2026",
  
  "primary": {
    "applicantId": "A001",
    "relationship": "PRIMARY",
    "demographic": {
      "firstName": "John",
      "lastName": "Smith",
      "dateOfBirth": "05/15/1980",
      "email": "john.smith@email.com"
    }
  },
  
  "spouse": {
    "applicantId": "A002",
    "relationship": "SPOUSE",
    "demographic": {
      "firstName": "Jane",
      "lastName": "Smith"
    }
  },
  
  "dependent1": {
    "demographic": { "firstName": "Emily", "dateOfBirth": "03/10/2015" }
  },
  "dependent2": {
    "demographic": { "firstName": "Michael", "dateOfBirth": "08/05/2017" }
  },
  "dependent3": {
    "demographic": { "firstName": "Sarah", "dateOfBirth": "11/30/2019" }
  },
  
  "additionalDependents": [
    {
      "demographic": { "firstName": "David", "dateOfBirth": "02/14/2021" }
    }
  ],
  
  "billing": {
    "type": "BILLING",
    "street": "123 Main Street",
    "city": "Los Angeles",
    "state": "CA",
    "zipCode": "90001"
  },
  
  "mailing": {
    "type": "MAILING",
    "street": "456 Oak Avenue",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94102"
  },
  
  "medical": {
    "productType": "MEDICAL",
    "planName": "Gold PPO",
    "monthlyPremium": 450.0
  },
  "dental": {
    "productType": "DENTAL",
    "planName": "Premium Dental",
    "monthlyPremium": 85.0
  },
  "vision": {
    "productType": "VISION",
    "planName": "Basic Vision",
    "monthlyPremium": 25.0
  },
  
  "hasSpouse": true,
  "hasMedical": true,
  "hasDental": true,
  "hasVision": true,
  "dependentCount": 4,
  "primaryDependentCount": 3,
  "additionalDependentCount": 1
}
```

### What to Check:
- ✅ `primary` object exists with John Smith's data
- ✅ `spouse` object exists with Jane Smith's data
- ✅ `dependent1`, `dependent2`, `dependent3` filled with first 3 kids
- ✅ `additionalDependents` array contains 4th child (David)
- ✅ `billing` and `mailing` separated correctly
- ✅ `medical`, `dental`, `vision` separated by product type
- ✅ Counts: `dependentCount=4`, `primaryDependentCount=3`, `additionalDependentCount=1`

---

## Test 2: Get Applicant Summary

**What it does:** Returns quick overview for UI/validation.

### Command:
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/applicant-summary \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  | jq .
```

### Expected Output:
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

### What to Check:
- ✅ `hasPrimary: true`
- ✅ `hasSpouse: true`
- ✅ `needsAddendum: true` (because 4 dependents > 3 slots)
- ✅ `primaryName` shows correct name

### Use Case:
```javascript
// Frontend validation
const summary = await fetch('/api/enrollment-complex/applicant-summary', {...});
if (summary.needsAddendum) {
  alert(`This application has ${summary.additionalDependentCount} additional dependents. 
         They will be shown on a separate addendum page.`);
}
```

---

## Test 3: Generate PDF

**What it does:** Generates complete PDF with main form + addendum (if needed).

### Command:
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d @/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json \
  -o enrollment-with-overflow.pdf
```

### Check the PDF:
```bash
# Open the PDF
xdg-open enrollment-with-overflow.pdf

# Or check file size
ls -lh enrollment-with-overflow.pdf
```

### Expected Result:

**Page 1 - Main Enrollment Form (AcroForm):**
- PRIMARY: John Smith (all fields)
- SPOUSE: Jane Smith (all fields)
- DEPENDENT 1: Emily Smith
- DEPENDENT 2: Michael Smith
- DEPENDENT 3: Sarah Smith
- BILLING: 123 Main Street, Los Angeles, CA 90001
- MAILING: 456 Oak Avenue, San Francisco, CA 94102
- MEDICAL: Gold PPO ($450/month)
- DENTAL: Premium Dental ($85/month)
- VISION: Basic Vision ($25/month)

**Page 2 - Additional Dependents Addendum (FreeMarker):**
- Table showing Dependent #4: David Smith
- Coverage details for David

---

## Test 4: Custom Payload (Inline JSON)

Test without external file:

### Minimal Application (No Spouse, No Dependents):
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicationId": "APP-MIN-001",
      "submittedDate": "12/12/2025",
      "effectiveDate": "01/01/2026",
      "applicants": [
        {
          "applicantId": "A001",
          "relationship": "PRIMARY",
          "demographic": {
            "firstName": "Alice",
            "lastName": "Johnson",
            "dateOfBirth": "01/01/1990",
            "ssn": "111-22-3333",
            "gender": "F",
            "email": "alice@example.com",
            "phone": "555-111-2222"
          }
        }
      ],
      "addresses": [
        {
          "type": "BILLING",
          "street": "789 Elm Street",
          "city": "Sacramento",
          "state": "CA",
          "zipCode": "95814"
        }
      ],
      "proposedProducts": [
        {
          "productType": "MEDICAL",
          "planName": "Bronze HMO",
          "monthlyPremium": 350.0
        }
      ]
    }
  }' \
  -o minimal-enrollment.pdf
```

**Expected:**
- Only PRIMARY fields filled
- SPOUSE and DEPENDENT fields empty
- No addendum page

### With Spouse (No Dependents):
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicationId": "APP-002",
      "applicants": [
        {
          "relationship": "PRIMARY",
          "demographic": {
            "firstName": "Bob",
            "lastName": "Williams"
          }
        },
        {
          "relationship": "SPOUSE",
          "demographic": {
            "firstName": "Carol",
            "lastName": "Williams"
          }
        }
      ],
      "addresses": [
        {
          "type": "BILLING",
          "street": "100 Main St",
          "city": "Dallas",
          "state": "TX"
        }
      ],
      "proposedProducts": [
        {
          "productType": "MEDICAL",
          "planName": "Silver Plan"
        }
      ]
    }
  }' \
  -o couple-enrollment.pdf
```

---

## Test 5: Verify Pre-Processing Logic

### Test Different Scenarios:

#### A. No Spouse:
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/applicant-summary \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicants": [
        {"relationship": "PRIMARY", "demographic": {"firstName": "Test"}}
      ]
    }
  }' | jq .
```
**Expected:** `"hasSpouse": false`

#### B. Exactly 3 Dependents:
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/applicant-summary \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicants": [
        {"relationship": "PRIMARY"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"}
      ]
    }
  }' | jq .
```
**Expected:** 
```json
{
  "dependentCount": 3,
  "primaryDependentCount": 3,
  "additionalDependentCount": 0,
  "needsAddendum": false
}
```

#### C. 6 Dependents (Overflow):
```bash
curl -X POST http://localhost:8080/api/enrollment-complex/applicant-summary \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicants": [
        {"relationship": "PRIMARY"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"},
        {"relationship": "DEPENDENT"}
      ]
    }
  }' | jq .
```
**Expected:**
```json
{
  "dependentCount": 6,
  "primaryDependentCount": 3,
  "additionalDependentCount": 3,
  "needsAddendum": true
}
```

---

## Test 6: Custom Config File

Use different YAML configuration:

```bash
curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "examples/custom-enrollment-config.yml",
    "application": {
      ...application data...
    }
  }' \
  -o custom-enrollment.pdf
```

---

## Troubleshooting

### Issue: 400 Bad Request

**Cause:** Missing required fields in JSON

**Fix:** Ensure `application` key exists:
```json
{
  "application": {  ← Must have this wrapper
    "applicants": [...]
  }
}
```

### Issue: 500 Internal Server Error

**Check logs:**
```bash
# In the terminal running the service, look for stack trace
```

**Common causes:**
1. Config file not found: `examples/preprocessed-enrollment-mapping.yml`
2. Pre-processor service not autowired
3. Null pointer when accessing nested data

**Debug with preview:**
```bash
# Use preview endpoint first to see if pre-processing works
curl -X POST .../preview-flattened -d @payload.json | jq .
```

### Issue: PDF Generated but Fields Empty

**Check flattened payload:**
```bash
curl -X POST .../preview-flattened -d @payload.json | jq .
```

**Verify:**
- Does `primary` exist in flattened output?
- Does `primary.demographic.firstName` have a value?
- Check YAML field mapping: `"Primary_FirstName": "primary.demographic.firstName"`

### Issue: No Addendum Generated (4+ Dependents)

**Check:**
1. FreeMarker template exists: `src/main/resources/templates/additional-dependents-addendum.ftl`
2. Section enabled in YAML:
   ```yaml
   - name: additional-dependents-addendum
     type: freemarker
     enabled: true  ← Must be true
   ```
3. Original `application` passed to FreeMarker:
   ```java
   fullPayload.put("application", applicationData);
   ```

### Issue: Spouse Fields Not Filling

**Verify spouse exists:**
```bash
curl .../preview-flattened ... | jq '.spouse'
```

**Should return:**
```json
{
  "relationship": "SPOUSE",
  "demographic": {...}
}
```

If `null`, check input has SPOUSE applicant.

---

## Performance Testing

### Test with Multiple Requests:
```bash
# Test 10 requests
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/enrollment-complex/generate \
    -H "Content-Type: application/json" \
    -d @complex-application-structure.json \
    -o "enrollment-$i.pdf" &
done
wait
```

### Measure Response Time:
```bash
time curl -X POST http://localhost:8080/api/enrollment-complex/generate \
  -H "Content-Type: application/json" \
  -d @complex-application-structure.json \
  -o enrollment.pdf
```

**Expected:** < 500ms for typical enrollment

---

## Integration Testing Script

Create `test-enrollment-api.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/enrollment-complex"
TEST_FILE="/workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json"

echo "=== Testing Complex Enrollment API ==="

# Test 1: Applicant Summary
echo -e "\n1. Testing Applicant Summary..."
curl -X POST "$BASE_URL/applicant-summary" \
  -H "Content-Type: application/json" \
  -d @"$TEST_FILE" \
  -s | jq .

# Test 2: Preview Flattened
echo -e "\n2. Testing Preview Flattened..."
curl -X POST "$BASE_URL/preview-flattened" \
  -H "Content-Type: application/json" \
  -d @"$TEST_FILE" \
  -s | jq 'keys'

# Test 3: Generate PDF
echo -e "\n3. Generating PDF..."
curl -X POST "$BASE_URL/generate" \
  -H "Content-Type: application/json" \
  -d @"$TEST_FILE" \
  -o enrollment-test.pdf \
  -w "\nHTTP Status: %{http_code}\n"

if [ -f enrollment-test.pdf ]; then
  SIZE=$(stat -f%z enrollment-test.pdf 2>/dev/null || stat -c%s enrollment-test.pdf)
  echo "PDF generated successfully. Size: $SIZE bytes"
else
  echo "ERROR: PDF not generated"
fi

echo -e "\n=== Tests Complete ==="
```

Run:
```bash
chmod +x test-enrollment-api.sh
./test-enrollment-api.sh
```

---

## Success Criteria

✅ **Preview Flattened:** Returns JSON with `primary`, `spouse`, `dependent1-3`, `billing`, `mailing`, `medical`, `dental`, `vision`

✅ **Applicant Summary:** Shows correct counts and `needsAddendum` flag

✅ **Generate PDF:** Returns PDF file (Content-Type: application/pdf)

✅ **Main Form:** First 3 dependents filled in AcroForm

✅ **Addendum:** 4th+ dependents shown on separate page

✅ **Empty Fields:** Optional fields (spouse, dependents) remain empty if not provided

✅ **Performance:** Response < 500ms for typical enrollment

---

## Next Steps

1. **Create actual AcroForm template** (`enrollment-application-with-dependents.pdf`) with proper field names
2. **Test with real data** from your system
3. **Add validation** for required fields
4. **Implement error handling** for missing data
5. **Add logging** for debugging production issues

Need help with any of these? Let me know!
