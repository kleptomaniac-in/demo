POST /api/enrollment/preview-config - Preview Config Selection

curl -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  }' | jq .

response:
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


2. POST /api/enrollment/generate - Generate PDF (Simple Payload)

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

   POST /api/enrollment/generate - Generate PDF (Complex Structure with Pre-Processing)
Using the complex application structure with PRIMARY, SPOUSE, dependents:

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

POST /api/enrollment/generate - Using External Test File

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


POST /api/enrollment/generate-with-rules - Rule-Based Selection
Test business rules (Medicare special handling, etc.):

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

Testing Different Scenarios
Scenario A: Multi-Product Enrollment

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

Scenario B: Different State
curl -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical"],
    "marketCategory": "individual",
    "state": "NY"
  }' | jq .

Scenario C: With Custom Output Filename
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {"products": ["dental"], "marketCategory": "individual", "state": "CA"},
    "payload": {"memberName": "Test"},
    "outputFileName": "custom-name-2025.pdf"
  }' \
  -o custom-name-2025.pdf

  