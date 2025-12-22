Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/pdf/controller/EnrollmentPdfController.java#1-1), lines 1 to 120

Searched for files matching `**/complex-application-structure.json`, 2 matches

Here's how to test the controller endpoints:

## **Prerequisites**

Start the service:
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

## **Available Endpoints**

### **1. POST /api/enrollment/preview-config** - Preview Config Selection
Preview which config would be selected (without generating PDF):

```bash
curl -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  }' | jq .
```

**Expected Response:**
```json
{
  "conventionBasedConfig": "dental-medical-individual-ca.yml",
  "ruleBasedConfig": "dental-medical-individual-ca.yml",
  "dynamicComposition": {
    "base": "templates/base-payer.yml",
    "components": [
      "templates/products/dental.yml",
      "templates/products/medical.yml",
      "templates/markets/individual.yml",
      "templates/states/california.yml"
    ]
  },
  "enrollmentSummary": "Products: medical, dental, Market: individual, State: CA"
}
```

---

### **2. POST /api/enrollment/generate** - Generate PDF (Simple Payload)

Generate PDF with automatic config selection:

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
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
  -o enrollment-simple.pdf
```

---

### **3. POST /api/enrollment/generate** - Generate PDF (Complex Structure with Pre-Processing)

Using the complex application structure with PRIMARY, SPOUSE, dependents:

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical", "dental"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "application": {
        "applicationId": "APP-2025-12345",
        "submittedDate": "12/15/2025",
        "effectiveDate": "01/01/2026",
        "applicants": [
          {
            "applicantId": "A001",
            "relationship": "PRIMARY",
            "demographic": {
              "firstName": "John",
              "lastName": "Smith",
              "dateOfBirth": "05/15/1980",
              "ssn": "123-45-6789",
              "email": "john.smith@email.com"
            }
          },
          {
            "applicantId": "A002",
            "relationship": "SPOUSE",
            "demographic": {
              "firstName": "Jane",
              "lastName": "Smith",
              "dateOfBirth": "07/22/1982"
            }
          },
          {
            "applicantId": "A003",
            "relationship": "DEPENDENT",
            "demographic": {
              "firstName": "Emily",
              "dateOfBirth": "03/10/2015"
            }
          }
        ],
        "addresses": [
          {
            "type": "BILLING",
            "street": "123 Main Street",
            "city": "Los Angeles",
            "state": "CA",
            "zipCode": "90001"
          }
        ],
        "proposedProducts": [
          {
            "productType": "MEDICAL",
            "planName": "Gold PPO",
            "monthlyPremium": 450.00
          },
          {
            "productType": "DENTAL",
            "planName": "Premium Dental",
            "monthlyPremium": 85.00
          }
        ]
      }
    }
  }' \
  -o enrollment-complex.pdf
```

**Check console logs:**
```
Detected complex structure - applying configuration-driven pre-processing
Pre-processing complete using rules: preprocessing/standard-enrollment-rules.yml | hasPrimary=true, hasSpouse=true, dependents=1
```

---

### **4. POST /api/enrollment/generate** - Using External Test File

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d @- << 'EOF' \
  -o enrollment-from-file.pdf
{
  "enrollment": {
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": $(cat /workspaces/demo/demoproject/config-repo/examples/complex-application-structure.json)
}
EOF
```

Or create a complete request file:

```bash
cat > /tmp/enrollment-request.json << 'EOF'
{
  "enrollment": {
    "products": ["medical", "dental", "vision"],
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "application": {
      "applicationId": "TEST-001",
      "applicants": [
        {"relationship": "PRIMARY", "demographic": {"firstName": "Alice"}}
      ]
    }
  },
  "outputFileName": "alice-enrollment.pdf"
}
EOF

curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d @/tmp/enrollment-request.json \
  -o alice-enrollment.pdf
```

---

### **5. POST /api/enrollment/generate-with-rules** - Rule-Based Selection

Test business rules (Medicare special handling, etc.):

```bash
curl -X POST http://localhost:8080/api/enrollment/generate-with-rules \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "medicare",
      "state": "CA"
    },
    "payload": {
      "memberName": "Senior Member",
      "age": 67
    }
  }' \
  -o medicare-enrollment.pdf
```

---

## **Testing Different Scenarios**

### **Scenario A: Multi-Product Enrollment**
```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical", "dental", "vision"],
      "marketCategory": "small-group",
      "state": "TX"
    },
    "payload": {
      "groupName": "ABC Corp",
      "groupSize": 25
    }
  }' \
  -o multi-product.pdf
```

### **Scenario B: Different State**
```bash
curl -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical"],
    "marketCategory": "individual",
    "state": "NY"
  }' | jq .
```

Expected: `"medical-individual-ny.yml"`

### **Scenario C: With Custom Output Filename**
```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {"products": ["dental"], "marketCategory": "individual", "state": "CA"},
    "payload": {"memberName": "Test"},
    "outputFileName": "custom-name-2025.pdf"
  }' \
  -o custom-name-2025.pdf
```

---

## **Verify Results**

**Check PDF was created:**
```bash
ls -lh enrollment-*.pdf
```

**Open PDF:**
```bash
xdg-open enrollment-simple.pdf
# or
open enrollment-simple.pdf  # macOS
```

**Check logs:**
```bash
# In terminal running the service
# Look for:
# - "Selected config: dental-medical-individual-ca.yml"
# - "Detected complex structure - applying configuration-driven pre-processing"
# - "Pre-processing complete..."
```

---

## **Testing with Postman/Insomnia**

**Import as Postman Collection:**

```json
{
  "info": {"name": "Enrollment PDF API"},
  "item": [
    {
      "name": "Preview Config",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/enrollment/preview-config",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"products\": [\"medical\", \"dental\"],\n  \"marketCategory\": \"individual\",\n  \"state\": \"CA\"\n}"
        }
      }
    },
    {
      "name": "Generate PDF",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/enrollment/generate",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"enrollment\": {\n    \"products\": [\"medical\"],\n    \"marketCategory\": \"individual\",\n    \"state\": \"CA\"\n  },\n  \"payload\": {\n    \"memberName\": \"John Doe\"\n  }\n}"
        }
      }
    }
  ]
}
```

---

## **Common Issues**

**Issue: 404 Not Found**
- Check service is running on port 8080
- Verify URL: `http://localhost:8080/api/enrollment/...`

**Issue: 500 Internal Server Error**
- Check logs for stack trace
- Verify config file exists: `dental-medical-individual-ca.yml`
- Check preprocessing rules exist if using complex structure

**Issue: Empty PDF**
- Check console logs for "Selected config: ..."
- Verify template files exist
- Check field mappings in YAML config

---

## **Quick Test Script**

```bash
#!/bin/bash
echo "Testing Enrollment PDF Controller"

# Test 1: Preview
echo "1. Testing preview-config..."
curl -s -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{"products":["medical"],"marketCategory":"individual","state":"CA"}' \
  | jq -r '.conventionBasedConfig'

# Test 2: Simple Generate
echo "2. Generating simple PDF..."
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{"enrollment":{"products":["medical"],"marketCategory":"individual","state":"CA"},"payload":{"memberName":"Test"}}' \
  -o test.pdf

if [ -f test.pdf ]; then
  echo "✅ PDF generated: $(ls -lh test.pdf | awk '{print $5}')"
else
  echo "❌ PDF generation failed"
fi

echo "Tests complete!"
```

Save as `test-endpoints.sh`, then:
```bash
chmod +x test-endpoints.sh
./test-endpoints.sh
