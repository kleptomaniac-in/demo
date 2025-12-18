# cURL Examples for PDF Generation Service

Quick reference for testing all endpoints using cURL commands.

## 🌐 Variables

```bash
export BASE_URL="http://localhost:8080"
export OUTPUT_DIR="/workspaces/demo/output"
mkdir -p "$OUTPUT_DIR"
```

## 📄 PDF Generation Endpoints

### 1. Generate Simple PDF
```bash
curl -X POST "$BASE_URL/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "templateName": "invoice",
    "clientService": "demo",
    "payload": {
      "customer": {"name": "ACME Corp"},
      "invoice": {
        "number": "INV-100",
        "date": "2025-12-02",
        "total": "$123.45"
      }
    }
  }' \
  -o "$OUTPUT_DIR/invoice.pdf"
```

### 2. Generate Enrollment PDF (Simple)
```bash
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical", "dental"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "memberName": "John Doe",
      "memberId": "12345",
      "planName": "Gold PPO"
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-simple.pdf"
```

### 3. Generate Enrollment PDF (Complex with Applicants)
```bash
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical", "dental"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "application": {
        "applicationId": "APP-2025-001",
        "applicants": [
          {
            "relationship": "PRIMARY",
            "demographic": {
              "firstName": "John",
              "lastName": "Smith",
              "dateOfBirth": "1980-05-15"
            }
          },
          {
            "relationship": "SPOUSE",
            "demographic": {
              "firstName": "Jane",
              "lastName": "Smith"
            }
          },
          {
            "relationship": "DEPENDENT",
            "demographic": {
              "firstName": "Emily",
              "lastName": "Smith",
              "dateOfBirth": "2015-03-10"
            }
          }
        ],
        "addresses": [
          {
            "type": "MAILING",
            "street": "123 Main St",
            "city": "Los Angeles",
            "state": "CA",
            "zipCode": "90001"
          }
        ],
        "proposedProducts": [
          {
            "productId": "MED-GOLD-2025",
            "productType": "medical"
          }
        ]
      }
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-complex.pdf"
```

### 4. Preview Config Selection
```bash
curl -X POST "$BASE_URL/api/enrollment/preview-config" \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  }' | jq .
```

### 5. Generate PDF with Multi-Template Configuration
```bash
curl -X POST "$BASE_URL/api/pdf/merge" \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "enrollment-multi-product",
    "payload": {
      "applicants": [
        {
          "relationship": "PRIMARY",
          "firstName": "John",
          "lastName": "Smith",
          "dateOfBirth": "1985-05-15"
        }
      ],
      "members": [
        {
          "name": "John Smith",
          "relationship": "PRIMARY",
          "dateOfBirth": "1985-05-15",
          "products": [
            {"type": "medical", "planName": "Gold PPO", "premium": 450.00}
          ]
        }
      ],
      "enrollment": {
        "products": ["medical"],
        "marketCategory": "individual",
        "state": "CA",
        "effectiveDate": "2026-01-01",
        "submittedDate": "2025-12-18"
      }
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-multi-template.pdf"
```

### 6. Generate Enrollment PDF per Applicant (ZIP)
```bash
curl -X POST "$BASE_URL/api/enrollment/generate-per-applicant" \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical"],
    "marketCategory": "individual",
    "state": "CA",
    "payload": {
      "applicants": [
        {
          "relationship": "PRIMARY",
          "firstName": "Alice",
          "lastName": "Williams",
          "dateOfBirth": "1990-08-20"
        },
        {
          "relationship": "SPOUSE",
          "firstName": "Bob",
          "lastName": "Williams",
          "dateOfBirth": "1988-12-10"
        }
      ],
      "members": [
        {
          "name": "Alice Williams",
          "relationship": "PRIMARY",
          "dateOfBirth": "1990-08-20",
          "products": [{"type": "medical", "planName": "Platinum HMO", "premium": 500.00}]
        },
        {
          "name": "Bob Williams",
          "relationship": "SPOUSE",
          "dateOfBirth": "1988-12-10",
          "products": [{"type": "medical", "planName": "Platinum HMO", "premium": 500.00}]
        }
      ],
      "enrollment": {
        "products": ["medical"],
        "marketCategory": "individual",
        "state": "NY",
        "effectiveDate": "2026-02-01",
        "submittedDate": "2025-12-18"
      }
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-per-applicant.zip"
```

### 7. Merge Multiple PDFs
```bash
curl -X POST "$BASE_URL/api/pdf/merge" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePaths": [
      "output1.pdf",
      "output2.pdf"
    ]
  }' \
  -o "$OUTPUT_DIR/merged.pdf"
```

### 6. Complex Enrollment - Generate
```bash
curl -X POST "$BASE_URL/api/enrollment-complex/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "group",
      "state": "NY"
    },
    "payload": {
      "application": {
        "applicationId": "GRP-001",
        "applicants": [
          {
            "relationship": "PRIMARY",
            "demographic": {
              "firstName": "Alice",
              "lastName": "Johnson"
            }
          }
        ]
      }
    }
  }' \
  -o "$OUTPUT_DIR/group-enrollment.pdf"
```

### 7. Preview Flattened Payload
```bash
curl -X POST "$BASE_URL/api/enrollment-complex/preview-flattened" \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicationId": "TEST-001",
      "applicants": [
        {"relationship": "PRIMARY", "demographic": {"firstName": "Bob"}}
      ]
    }
  }' | jq .
```

### 8. Get Applicant Summary
```bash
curl -X POST "$BASE_URL/api/enrollment-complex/applicant-summary" \
  -H "Content-Type: application/json" \
  -d '{
    "application": {
      "applicants": [
        {"relationship": "PRIMARY", "demographic": {"firstName": "John"}},
        {"relationship": "SPOUSE", "demographic": {"firstName": "Jane"}},
        {"relationship": "DEPENDENT", "demographic": {"firstName": "Junior"}}
      ]
    }
  }' | jq .
```

## 📊 Excel Generation Endpoints

### 1. Generate Excel - Simple
```bash
curl -X POST "$BASE_URL/api/excel/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "templatePath": "enrollment-summary.xlsx",
    "cellMappings": {
      "ApplicationId": "applicationId",
      "PrimaryFirstName": "firstName",
      "PrimaryLastName": "lastName",
      "MedicalPlanName": "planName"
    },
    "payload": {
      "applicationId": "APP-12345",
      "firstName": "John",
      "lastName": "Smith",
      "planName": "Gold PPO"
    }
  }' \
  -o "$OUTPUT_DIR/enrollment.xlsx"
```

### 2. Generate Excel - From Config
```bash
curl -X POST "$BASE_URL/api/excel/generate-from-config" \
  -H "Content-Type: application/json" \
  -d '{
    "configPath": "excel-configs/enrollment-simple.yml",
    "payload": {
      "applicationId": "APP-67890",
      "memberName": "Jane Doe"
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-from-config.xlsx"
```

### 3. Generate Excel - With Preprocessing
```bash
curl -X POST "$BASE_URL/api/excel/generate-with-preprocessing" \
  -H "Content-Type: application/json" \
  -d '{
    "templatePath": "enrollment-summary.xlsx",
    "cellMappings": {
      "ApplicationId": "applicationId",
      "TotalApplicants": "totalApplicants"
    },
    "preprocessingRules": "preprocessing/excel-rules.yml",
    "payload": {
      "application": {
        "applicationId": "APP-001",
        "applicants": [
          {"name": "John"},
          {"name": "Jane"},
          {"name": "Junior"}
        ]
      }
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-preprocessed.xlsx"
```

### 4. Generate Excel - With Tables
```bash
curl -X POST "$BASE_URL/api/excel/generate-with-tables" \
  -H "Content-Type: application/json" \
  -d '{
    "templatePath": "enrollment-with-tables.xlsx",
    "cellMappings": {
      "ApplicationId": "applicationId"
    },
    "tableMappings": [
      {
        "sheetName": "Applicants",
        "startRow": 5,
        "dataPath": "applicants",
        "columnMappings": {
          "A": "firstName",
          "B": "lastName",
          "C": "relationship"
        }
      }
    ],
    "payload": {
      "applicationId": "APP-TBL-001",
      "applicants": [
        {"firstName": "John", "lastName": "Doe", "relationship": "PRIMARY"},
        {"firstName": "Jane", "lastName": "Doe", "relationship": "SPOUSE"}
      ]
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-with-tables.xlsx"
```

### 5. Generate Excel - Complete (All Features)
```bash
curl -X POST "$BASE_URL/api/excel/generate-complete" \
  -H "Content-Type: application/json" \
  -d '{
    "templatePath": "enrollment-complete.xlsx",
    "cellMappings": {
      "ApplicationId": "applicationId"
    },
    "tableMappings": [
      {
        "sheetName": "Details",
        "startRow": 10,
        "dataPath": "applicants",
        "columnMappings": {
          "A": "name",
          "B": "age"
        }
      }
    ],
    "preprocessingRules": "preprocessing/complete-rules.yml",
    "payload": {
      "application": {
        "applicationId": "APP-COMP-001",
        "applicants": [
          {"name": "Alice", "dateOfBirth": "1990-01-01"}
        ]
      }
    }
  }' \
  -o "$OUTPUT_DIR/enrollment-complete.xlsx"
```

## 🏥 Health & Debug Endpoints

### Health Check (PDF Service)
```bash
curl "$BASE_URL/api/pdf/health"
```

### Actuator Health (Spring Boot)
```bash
curl "$BASE_URL/actuator/health" | jq .
```

### Debug Mapping Order
```bash
curl "$BASE_URL/internal/mapping-order"
```

## 🔍 Testing Tips

### Save with Verbose Output
```bash
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d @request.json \
  -o output.pdf \
  -w "\nHTTP: %{http_code}\nSize: %{size_download} bytes\nTime: %{time_total}s\n"
```

### Check Response Headers
```bash
curl -I -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Use External File
```bash
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d @request-enrollment.json \
  -o output.pdf
```

### With Shell Substitution (Load Complex JSON)
```bash
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  --data @- <<EOF \
  -o output.pdf
{
  "enrollment": {...},
  "payload": $(cat config-repo/examples/complex-application-structure.json)
}
EOF
```

### Batch Testing
```bash
for state in CA NY TX FL; do
  curl -X POST "$BASE_URL/api/enrollment/generate" \
    -H "Content-Type: application/json" \
    -d "{
      \"enrollment\": {
        \"products\": [\"medical\"],
        \"marketCategory\": \"individual\",
        \"state\": \"$state\"
      },
      \"payload\": {\"memberName\": \"Test $state\"}
    }" \
    -o "$OUTPUT_DIR/enrollment-$state.pdf"
  echo "Generated enrollment-$state.pdf"
done
```

## 📦 View Generated Files

### PDF Files
```bash
# List all PDFs
ls -lh "$OUTPUT_DIR"/*.pdf

# View PDF info
pdfinfo "$OUTPUT_DIR/enrollment.pdf"

# Convert to text
pdftotext "$OUTPUT_DIR/enrollment.pdf" -

# Convert to PNG
pdftoppm "$OUTPUT_DIR/enrollment.pdf" "$OUTPUT_DIR/page" -png
```

### Excel Files
```bash
# List all Excel files
ls -lh "$OUTPUT_DIR"/*.xlsx

# View as ZIP
unzip -l "$OUTPUT_DIR/enrollment.xlsx"

# Extract sheet data
unzip -p "$OUTPUT_DIR/enrollment.xlsx" xl/worksheets/sheet1.xml | xmllint --format -

# Convert to CSV (requires ssconvert)
ssconvert "$OUTPUT_DIR/enrollment.xlsx" "$OUTPUT_DIR/enrollment.csv"
```

## 🐛 Debugging

### Check Logs
```bash
tail -100 /tmp/spring-*.log | grep -E "Error|Exception"
```

### Verify Empty Response
```bash
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d @request.json \
  -o output.pdf \
  -v 2>&1 | grep "< HTTP\|< Content-Length"
  
ls -lh output.pdf
file output.pdf
```

### Test with Minimal Payload
```bash
# Absolute minimal test
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {"test": "data"}
  }' \
  -o test.pdf && ls -lh test.pdf
```

---

**Tip:** Run the automated test script for a quick verification:
```bash
./test-postman-requests.sh
```
