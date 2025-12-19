# Curl Test Examples for PDF Generation with Addendums

## Prerequisites

1. Start the service:
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

2. Service should be running on: `http://localhost:8080`

---

## Test 1: Dependent Overflow (6 Dependents)

**Scenario:** Family with 6 dependents → First 3 in form, Last 3 in addendum

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -o enrollment-6-dependents.pdf \
  -d '{
  "enrollment": {
    "state": "CA",
    "marketCategory": "INDIVIDUAL",
    "products": ["DENTAL"]
  },
  "payload": {
    "enrollmentData": {
      "groupNumber": "GRP-12345",
      "effectiveDate": "2024-01-01",
      "planType": "FAMILY"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "firstName": "John",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "PRIMARY",
          "dateOfBirth": "1980-05-15",
          "gender": "M",
          "ssn": "123-45-6789"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 50.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A002",
        "firstName": "Mary",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "SPOUSE",
          "dateOfBirth": "1982-08-20",
          "gender": "F",
          "ssn": "987-65-4321"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 45.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A003",
        "firstName": "Tommy",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2010-03-10",
          "gender": "M",
          "ssn": "111-11-1111"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 30.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A004",
        "firstName": "Sarah",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2012-07-22",
          "gender": "F",
          "ssn": "222-22-2222"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 30.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A005",
        "firstName": "Billy",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2014-11-05",
          "gender": "M",
          "ssn": "333-33-3333"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 30.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A006",
        "firstName": "Emma",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2016-02-14",
          "gender": "F",
          "ssn": "444-44-4444"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 30.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A007",
        "firstName": "Jake",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2018-09-30",
          "gender": "M",
          "ssn": "555-55-5555"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 30.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A008",
        "firstName": "Lily",
        "lastName": "Smith",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2020-12-25",
          "gender": "F",
          "ssn": "666-66-6666"
        },
        "coverages": [
          {
            "productType": "DENTAL",
            "premium": 30.00,
            "carrier": "Delta Dental"
          }
        ]
      }
    ]
  }
}'

echo "✓ PDF saved to: enrollment-6-dependents.pdf"
echo "  Expected: 2 pages (main form + dependent addendum)"
```

---

## Test 2: Coverage Overflow (Multiple Coverages per Person)

**Scenario:** PRIMARY with 3 coverages, SPOUSE with 2 coverages → Overflow in addendum

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -o enrollment-multiple-coverages.pdf \
  -d '{
  "enrollment": {
    "state": "CA",
    "marketCategory": "INDIVIDUAL",
    "products": ["DENTAL", "MEDICAL", "VISION"]
  },
  "payload": {
    "enrollmentData": {
      "groupNumber": "GRP-99999",
      "effectiveDate": "2024-03-01",
      "planType": "FAMILY"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "firstName": "John",
        "lastName": "Doe",
        "demographic": {
          "relationshipType": "PRIMARY",
          "dateOfBirth": "1975-04-12",
          "gender": "M",
          "ssn": "555-12-3456"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 500.00,
            "carrier": "Blue Cross Blue Shield"
          },
          {
            "productType": "DENTAL",
            "premium": 50.00,
            "carrier": "Delta Dental"
          },
          {
            "productType": "VISION",
            "premium": 25.00,
            "carrier": "VSP Vision Care"
          }
        ]
      },
      {
        "applicantId": "A002",
        "firstName": "Jane",
        "lastName": "Doe",
        "demographic": {
          "relationshipType": "SPOUSE",
          "dateOfBirth": "1977-09-22",
          "gender": "F",
          "ssn": "555-98-7654"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 450.00,
            "carrier": "Blue Cross Blue Shield"
          },
          {
            "productType": "DENTAL",
            "premium": 45.00,
            "carrier": "Delta Dental"
          }
        ]
      },
      {
        "applicantId": "A003",
        "firstName": "Jimmy",
        "lastName": "Doe",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2015-06-18",
          "gender": "M",
          "ssn": "555-11-2233"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 200.00,
            "carrier": "Blue Cross Blue Shield"
          }
        ]
      }
    ]
  }
}'

echo "✓ PDF saved to: enrollment-multiple-coverages.pdf"
echo "  Expected: 2 pages (main form + coverage addendum)"
```

---

## Test 3: BOTH Overflows (6 Dependents + Multiple Coverages)

**Scenario:** Large family with multiple coverages → Both addendums

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -o enrollment-both-overflows.pdf \
  -d '{
  "enrollment": {
    "state": "TX",
    "marketCategory": "SMALL_GROUP",
    "products": ["DENTAL", "MEDICAL"]
  },
  "payload": {
    "enrollmentData": {
      "groupNumber": "GRP-FAMILY-2024",
      "effectiveDate": "2024-01-15",
      "planType": "FAMILY_LARGE"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "firstName": "Robert",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "PRIMARY",
          "dateOfBirth": "1978-01-10",
          "gender": "M",
          "ssn": "444-55-6666"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 550.00,
            "carrier": "Aetna"
          },
          {
            "productType": "DENTAL",
            "premium": 60.00,
            "carrier": "Cigna Dental"
          }
        ]
      },
      {
        "applicantId": "A002",
        "firstName": "Linda",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "SPOUSE",
          "dateOfBirth": "1980-05-22",
          "gender": "F",
          "ssn": "444-77-8888"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 500.00,
            "carrier": "Aetna"
          },
          {
            "productType": "DENTAL",
            "premium": 55.00,
            "carrier": "Cigna Dental"
          }
        ]
      },
      {
        "applicantId": "A003",
        "firstName": "Alex",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2008-03-15",
          "gender": "M",
          "ssn": "444-11-2222"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 180.00,
            "carrier": "Aetna"
          }
        ]
      },
      {
        "applicantId": "A004",
        "firstName": "Sophie",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2010-07-20",
          "gender": "F",
          "ssn": "444-33-4444"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 180.00,
            "carrier": "Aetna"
          }
        ]
      },
      {
        "applicantId": "A005",
        "firstName": "Michael",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2012-11-08",
          "gender": "M",
          "ssn": "444-55-6677"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 180.00,
            "carrier": "Aetna"
          }
        ]
      },
      {
        "applicantId": "A006",
        "firstName": "Olivia",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2014-02-14",
          "gender": "F",
          "ssn": "444-88-9900"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 180.00,
            "carrier": "Aetna"
          }
        ]
      },
      {
        "applicantId": "A007",
        "firstName": "Ethan",
        "lastName": "Johnson",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2016-09-30",
          "gender": "M",
          "ssn": "444-11-3344"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 180.00,
            "carrier": "Aetna"
          }
        ]
      }
    ]
  }
}'

echo "✓ PDF saved to: enrollment-both-overflows.pdf"
echo "  Expected: 3 pages (main form + dependent addendum + coverage addendum)"
```

---

## Test 4: No Overflows (Within Limits)

**Scenario:** 3 dependents, 1 coverage each → No addendums

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -o enrollment-no-overflow.pdf \
  -d '{
  "enrollment": {
    "state": "CA",
    "marketCategory": "INDIVIDUAL",
    "products": ["MEDICAL"]
  },
  "payload": {
    "enrollmentData": {
      "groupNumber": "GRP-SMALL",
      "effectiveDate": "2024-02-01",
      "planType": "FAMILY"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "firstName": "David",
        "lastName": "Lee",
        "demographic": {
          "relationshipType": "PRIMARY",
          "dateOfBirth": "1985-06-10",
          "gender": "M",
          "ssn": "333-44-5555"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 400.00,
            "carrier": "Kaiser Permanente"
          }
        ]
      },
      {
        "applicantId": "A002",
        "firstName": "Sarah",
        "lastName": "Lee",
        "demographic": {
          "relationshipType": "SPOUSE",
          "dateOfBirth": "1987-11-15",
          "gender": "F",
          "ssn": "333-66-7777"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 380.00,
            "carrier": "Kaiser Permanente"
          }
        ]
      },
      {
        "applicantId": "A003",
        "firstName": "Emma",
        "lastName": "Lee",
        "demographic": {
          "relationshipType": "DEPENDENT",
          "dateOfBirth": "2018-04-20",
          "gender": "F",
          "ssn": "333-88-9999"
        },
        "coverages": [
          {
            "productType": "MEDICAL",
            "premium": 150.00,
            "carrier": "Kaiser Permanente"
          }
        ]
      }
    ]
  }
}'

echo "✓ PDF saved to: enrollment-no-overflow.pdf"
echo "  Expected: 1 page (main form only, no addendums)"
```

---

## Test 5: Extreme Case (10 Dependents, Multiple Coverages)

**Scenario:** Very large family with many coverages → Multi-page addendums

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -o enrollment-extreme.pdf \
  -d '{
  "enrollment": {
    "state": "CA",
    "marketCategory": "INDIVIDUAL",
    "products": ["MEDICAL", "DENTAL", "VISION"]
  },
  "payload": {
    "enrollmentData": {
      "groupNumber": "GRP-LARGE-FAMILY",
      "effectiveDate": "2024-01-01",
      "planType": "FAMILY_XXL"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "firstName": "John",
        "lastName": "BigFamily",
        "demographic": {
          "relationshipType": "PRIMARY",
          "dateOfBirth": "1970-01-01",
          "gender": "M",
          "ssn": "999-11-1111"
        },
        "coverages": [
          {"productType": "MEDICAL", "premium": 600.00, "carrier": "Blue Cross"},
          {"productType": "DENTAL", "premium": 70.00, "carrier": "Delta Dental"},
          {"productType": "VISION", "premium": 30.00, "carrier": "VSP"}
        ]
      },
      {
        "applicantId": "A002",
        "firstName": "Jane",
        "lastName": "BigFamily",
        "demographic": {
          "relationshipType": "SPOUSE",
          "dateOfBirth": "1972-06-15",
          "gender": "F",
          "ssn": "999-22-2222"
        },
        "coverages": [
          {"productType": "MEDICAL", "premium": 550.00, "carrier": "Blue Cross"},
          {"productType": "DENTAL", "premium": 65.00, "carrier": "Delta Dental"}
        ]
      },
      {
        "applicantId": "A003",
        "firstName": "Child1",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2005-01-10", "gender": "M", "ssn": "999-33-3333"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A004",
        "firstName": "Child2",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2007-03-12", "gender": "F", "ssn": "999-44-4444"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A005",
        "firstName": "Child3",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2009-05-14", "gender": "M", "ssn": "999-55-5555"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A006",
        "firstName": "Child4",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2011-07-16", "gender": "F", "ssn": "999-66-6666"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A007",
        "firstName": "Child5",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2013-09-18", "gender": "M", "ssn": "999-77-7777"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A008",
        "firstName": "Child6",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2015-11-20", "gender": "F", "ssn": "999-88-8888"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A009",
        "firstName": "Child7",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2017-01-22", "gender": "M", "ssn": "999-99-9999"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      },
      {
        "applicantId": "A010",
        "firstName": "Child8",
        "lastName": "BigFamily",
        "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2019-03-24", "gender": "F", "ssn": "999-00-0000"},
        "coverages": [{"productType": "MEDICAL", "premium": 200.00, "carrier": "Blue Cross"}]
      }
    ]
  }
}'

echo "✓ PDF saved to: enrollment-extreme.pdf"
echo "  Expected: 3-4 pages (main form + multi-page dependent addendum + coverage addendum)"
```

---

## Verify Generated PDFs

After running the curl commands, verify the PDFs:

```bash
# List generated files
ls -lh *.pdf

# Check PDF content (requires pdftotext or similar)
for pdf in enrollment-*.pdf; do
  echo "=== $pdf ==="
  pdfinfo "$pdf" 2>/dev/null | grep Pages || echo "pdfinfo not available"
  echo ""
done

# Or use the Java verifier
cd /workspaces/demo/demoproject/pdf-generation-service
mvn test-compile exec:java \
  -Dexec.mainClass="com.example.service.PdfContentVerifier" \
  -Dexec.classpathScope=test
```

---

## Quick Reference

| Test | Dependents | Coverages/Person | Expected Pages | Addendums |
|------|------------|------------------|----------------|-----------|
| Test 1 | 6 | 1 | 2 | Dependent only |
| Test 2 | 1 | 2-3 | 2 | Coverage only |
| Test 3 | 5 | 2 | 3 | Both |
| Test 4 | 1 | 1 | 1 | None |
| Test 5 | 8 | 2-3 | 3-4 | Both (multi-page) |

---

## Troubleshooting

### Service Not Running
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Or change port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### PDF Appears Empty
The PDFs contain content but may not render in some viewers. Try:
1. Download to local machine
2. Open with Adobe Reader, Chrome, or Firefox
3. Use the PDF verification script (see above)

### Connection Refused
Make sure:
1. Service is running (`mvn spring-boot:run`)
2. No firewall blocking localhost:8080
3. Wait for "Started PdfGenerationServiceApplication" message
