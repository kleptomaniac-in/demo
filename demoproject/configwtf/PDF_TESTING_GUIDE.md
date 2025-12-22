# PDF Generation Testing Guide

Complete guide for testing and verifying PDF generation with addendums.

## Table of Contents
1. [Quick Start](#quick-start)
2. [Integration Tests](#integration-tests)
3. [Manual Testing](#manual-testing)
4. [Visual Verification Checklist](#visual-verification-checklist)
5. [Performance Testing](#performance-testing)

---

## Quick Start

### Option 1: Run Integration Tests (Automated)

```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn test -Dtest=PdfGenerationIntegrationTest
```

**Output:** PDFs generated in `output/test-pdfs/`

### Option 2: Run Manual Test (Quick Generation)

```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn exec:java -Dexec.mainClass="com.example.service.ManualPdfGenerationTest"
```

**Output:** PDFs generated in `output/manual-test/`

---

## Integration Tests

### PdfGenerationIntegrationTest

Location: `src/test/java/com/example/service/PdfGenerationIntegrationTest.java`

**7 Comprehensive Test Cases:**

#### 1. testGenerateDependentAddendum
- **Scenario:** 6 dependents (3 overflow)
- **Output:** `dependent-addendum-6deps.pdf`
- **Verifies:** 
  - Dependent addendum table format
  - Data for dependents 4-6
  - SSN masking (***-**-XXXX)
  - Name, DOB, Gender, SSN columns

#### 2. testGenerateCoverageAddendum
- **Scenario:** 3 applicants with varying coverage counts
  - PRIMARY: 3 coverages (2 overflow)
  - SPOUSE: 2 coverages (1 overflow)
  - DEPENDENT: 1 coverage (0 overflow)
- **Output:** `coverage-addendum-5overflow.pdf`
- **Verifies:**
  - Coverage addendum table format
  - Per-applicant overflow handling
  - Coverage numbering (#2, #3)
  - All table columns

#### 3. testMergedPdfWithBothAddendums
- **Scenario:** Complete enrollment with both overflow types
  - 2 adults with multiple coverages
  - 5 dependents (2 overflow)
- **Output:** `complete-enrollment-with-both-addendums.pdf`
- **Verifies:**
  - Page 1: Main form
  - Page 2: Dependent addendum
  - Page 3: Coverage addendum
  - PDF merging works correctly

#### 4. testLargeDependentAddendum
- **Scenario:** 13 dependents (10 overflow)
- **Output:** `dependent-addendum-10overflow.pdf`
- **Verifies:**
  - Multi-page addendum support
  - Large data handling
  - Pagination works correctly

#### 5. testManyCoveragesPerApplicant
- **Scenario:** PRIMARY with 5 coverages
- **Output:** `coverage-addendum-4overflow-one-person.pdf`
- **Verifies:**
  - Single applicant with many coverages
  - Coverage type variety (Medical, Dental, Vision, Life, Disability)
  - Premium formatting

#### 6. testNoAddendums
- **Scenario:** 4 applicants within limits
- **Verifies:**
  - No addendum generation when not needed
  - isAddendumNeeded() returns false
  - Edge case: exactly at limits

### Running Specific Tests

```bash
# Run all integration tests
mvn test -Dtest=PdfGenerationIntegrationTest

# Run specific test
mvn test -Dtest=PdfGenerationIntegrationTest#testMergedPdfWithBothAddendums

# Run with detailed output
mvn test -Dtest=PdfGenerationIntegrationTest -X
```

---

## Manual Testing

### ManualPdfGenerationTest

Location: `src/test/java/com/example/service/ManualPdfGenerationTest.java`

**Run as standalone Java application:**

```bash
cd /workspaces/demo/demoproject/pdf-generation-service

# Compile
mvn compile test-compile

# Run
mvn exec:java -Dexec.mainClass="com.example.service.ManualPdfGenerationTest"
```

**Generated PDFs:**

| File | Description | Scenario |
|------|-------------|----------|
| `dependent-addendum.pdf` | Dependent overflow | 6 deps (3 overflow) |
| `coverage-addendum.pdf` | Coverage overflow | 3 applicants, mixed |
| `merged-complete.pdf` | Complete enrollment | 3 pages total |
| `large-family.pdf` | Large dependent set | 13 deps (10 overflow) |
| `complex-coverage.pdf` | Many coverages | 9 overflow total |

### Output Location

```
output/
└── manual-test/
    ├── dependent-addendum.pdf
    ├── coverage-addendum.pdf
    ├── merged-complete.pdf
    ├── large-family.pdf
    └── complex-coverage.pdf
```

---

## Visual Verification Checklist

### Dependent Addendum Verification

Open: `dependent-addendum.pdf`

**Check:**
- [ ] Title: "Dependent Addendum"
- [ ] Group Number displayed
- [ ] Effective Date displayed
- [ ] Table headers:
  - [ ] # (Dependent number)
  - [ ] Name
  - [ ] Date of Birth
  - [ ] Gender
  - [ ] SSN
- [ ] Data rows:
  - [ ] Starts at dependent #4
  - [ ] Names are full (First Last)
  - [ ] DOB format: YYYY-MM-DD
  - [ ] Gender: M or F
  - [ ] SSN masked: ***-**-1111 (last 4 only)
- [ ] Alignment:
  - [ ] Text is readable
  - [ ] Columns are properly aligned
  - [ ] No text overflow/truncation
- [ ] Multi-page (if >20 dependents):
  - [ ] Headers repeat on each page
  - [ ] Data flows continuously

### Coverage Addendum Verification

Open: `coverage-addendum.pdf`

**Check:**
- [ ] Title: "Coverage Addendum"
- [ ] Group Number displayed
- [ ] Effective Date displayed
- [ ] Table headers:
  - [ ] Applicant
  - [ ] Relationship
  - [ ] Coverage #
  - [ ] Type
  - [ ] Premium
  - [ ] Carrier
- [ ] Data rows:
  - [ ] Applicant name correct
  - [ ] Relationship type (PRIMARY, SPOUSE, DEPENDENT)
  - [ ] Coverage # starts at 2 (first is in main form)
  - [ ] Type (MEDICAL, DENTAL, VISION, etc.)
  - [ ] Premium formatted: $XXX.XX
  - [ ] Carrier name (may be truncated if long)
- [ ] Grouping:
  - [ ] All coverages for one applicant shown together
  - [ ] Separated from next applicant's coverages
- [ ] Alignment:
  - [ ] Text is readable
  - [ ] Currency aligned right
  - [ ] No overlap

### Merged PDF Verification

Open: `merged-complete.pdf`

**Check:**
- [ ] Page 1: Main enrollment form
  - [ ] Has enrollment data
  - [ ] References addendums ("see addendum...")
- [ ] Page 2: Dependent addendum
  - [ ] Separate page
  - [ ] Table format correct
- [ ] Page 3: Coverage addendum
  - [ ] Separate page
  - [ ] Table format correct
- [ ] Navigation:
  - [ ] All pages accessible
  - [ ] Page numbers correct
  - [ ] Can scroll through all pages

### Data Accuracy Verification

**Compare with test data:**

```java
// Example: Verify dependent #4
Input:
  firstName: "Emma"
  lastName: "Smith"
  dob: "2016-02-14"
  gender: "F"
  ssn: "444-44-4444"

Expected in PDF:
  #: 4
  Name: Emma Smith
  DOB: 2016-02-14
  Gender: F
  SSN: ***-**-4444
```

---

## Performance Testing

### Generate PDFs with Timing

```bash
# Run with performance metrics
mvn test -Dtest=PdfGenerationIntegrationTest 2>&1 | grep "PDF size"
```

**Expected Performance:**

| Operation | Time | Size |
|-----------|------|------|
| Dependent addendum (3 overflow) | ~5-10ms | ~5-10KB |
| Coverage addendum (5 overflow) | ~5-10ms | ~5-10KB |
| Merge 3 PDFs | ~2-5ms | ~20-30KB |
| **Total** | **~15-25ms** | **~30-50KB** |

### Stress Test: Large Data

```bash
# Test with 50 dependents (47 overflow)
# Modify testLargeDependentAddendum to create 50 dependents
mvn test -Dtest=PdfGenerationIntegrationTest#testLargeDependentAddendum
```

**Expected:**
- Multi-page addendum (3-4 pages)
- Time: ~20-30ms
- Size: ~20-40KB

---

## REST API Testing

### Using curl

```bash
# Test endpoint (if REST controller implemented)
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "groupNumber": "GRP-12345",
      "effectiveDate": "2024-01-01"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "firstName": "John",
        "lastName": "Doe",
        "demographic": {
          "relationshipType": "PRIMARY",
          "dateOfBirth": "1980-05-15",
          "gender": "M",
          "ssn": "123-45-6789"
        },
        "coverages": [
          { "productType": "MEDICAL", "premium": 500.00, "carrier": "Blue Cross" }
        ]
      }
    ]
  }' \
  --output enrollment.pdf

# Open generated PDF
xdg-open enrollment.pdf
```

### Using test-requests.http

Create `test-requests.http`:

```http
### Generate enrollment with overflow
POST http://localhost:8080/api/enrollment/generate
Content-Type: application/json

{
  "enrollmentData": {
    "groupNumber": "GRP-12345",
    "effectiveDate": "2024-01-01"
  },
  "applicants": [
    {
      "applicantId": "A001",
      "firstName": "John",
      "lastName": "Doe",
      "demographic": {
        "relationshipType": "PRIMARY",
        "dateOfBirth": "1980-05-15",
        "gender": "M",
        "ssn": "123-45-6789"
      },
      "coverages": [
        { "productType": "MEDICAL", "premium": 500.00, "carrier": "Blue Cross" },
        { "productType": "DENTAL", "premium": 50.00, "carrier": "Delta Dental" }
      ]
    },
    {
      "applicantId": "A002",
      "firstName": "Jane",
      "lastName": "Doe",
      "demographic": {
        "relationshipType": "SPOUSE",
        "dateOfBirth": "1982-08-20",
        "gender": "F",
        "ssn": "987-65-4321"
      },
      "coverages": [
        { "productType": "MEDICAL", "premium": 450.00, "carrier": "Blue Cross" }
      ]
    },
    {
      "applicantId": "A003",
      "firstName": "Tommy",
      "lastName": "Doe",
      "demographic": {
        "relationshipType": "DEPENDENT",
        "dateOfBirth": "2010-03-10",
        "gender": "M",
        "ssn": "111-11-1111"
      },
      "coverages": [
        { "productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross" }
      ]
    },
    {
      "applicantId": "A004",
      "firstName": "Sarah",
      "lastName": "Doe",
      "demographic": {
        "relationshipType": "DEPENDENT",
        "dateOfBirth": "2012-07-22",
        "gender": "F",
        "ssn": "222-22-2222"
      },
      "coverages": [
        { "productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross" }
      ]
    },
    {
      "applicantId": "A005",
      "firstName": "Billy",
      "lastName": "Doe",
      "demographic": {
        "relationshipType": "DEPENDENT",
        "dateOfBirth": "2014-11-05",
        "gender": "M",
        "ssn": "333-33-3333"
      },
      "coverages": [
        { "productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross" }
      ]
    },
    {
      "applicantId": "A006",
      "firstName": "Emma",
      "lastName": "Doe",
      "demographic": {
        "relationshipType": "DEPENDENT",
        "dateOfBirth": "2016-02-14",
        "gender": "F",
        "ssn": "444-44-4444"
      },
      "coverages": [
        { "productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross" }
      ]
    }
  ],
  "addendumOptions": "ALL"
}
```

---

## Troubleshooting

### PDFs Not Generated

**Check:**
1. Output directory exists: `ls -la output/test-pdfs/`
2. Permissions: `chmod -R 755 output/`
3. Disk space: `df -h`

### Empty PDFs

**Check:**
1. Test data has overflow (>3 deps or >1 coverage per applicant)
2. Services properly initialized
3. No exceptions in test output

### Corrupted PDFs

**Validate PDF:**
```bash
# Check PDF structure
pdfinfo output/test-pdfs/dependent-addendum.pdf

# Try opening with multiple viewers
evince output/test-pdfs/dependent-addendum.pdf
xpdf output/test-pdfs/dependent-addendum.pdf
```

### Test Failures

**Debug:**
```bash
# Run with debug output
mvn test -Dtest=PdfGenerationIntegrationTest -X

# Check specific test
mvn test -Dtest=PdfGenerationIntegrationTest#testGenerateDependentAddendum -X
```

---

## Continuous Integration

### Add to CI Pipeline

```yaml
# .github/workflows/test.yml
- name: Run PDF Generation Tests
  run: mvn test -Dtest=PdfGenerationIntegrationTest

- name: Archive PDFs
  uses: actions/upload-artifact@v3
  with:
    name: test-pdfs
    path: output/test-pdfs/
    retention-days: 7
```

---

## Summary

**Three Ways to Test:**

1. **Automated Tests (Recommended):**
   ```bash
   mvn test -Dtest=PdfGenerationIntegrationTest
   ```
   - 7 comprehensive test cases
   - Automatic PDF generation
   - Output: `output/test-pdfs/`

2. **Manual Test (Quick):**
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.service.ManualPdfGenerationTest"
   ```
   - 5 sample PDFs
   - Easy to run
   - Output: `output/manual-test/`

3. **Visual Verification:**
   - Open generated PDFs
   - Use verification checklists above
   - Check formatting, data accuracy, alignment

**Next Steps:**
1. Run tests
2. Open PDFs
3. Verify against checklists
4. Report any issues
