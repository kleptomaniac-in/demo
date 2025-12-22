#!/bin/bash
# Quick test script to verify all main endpoints

BASE_URL="http://localhost:8080"
OUTPUT_DIR="/workspaces/demo/output"
mkdir -p "$OUTPUT_DIR"

echo "🧪 Testing PDF Generation Service Endpoints..."
echo "=============================================="
echo ""

# Test 1: Health Check
echo "1️⃣  Testing Health Check..."
curl -s "$BASE_URL/actuator/health" | jq . || echo "❌ Health check failed"
echo ""

# Test 2: Simple Enrollment PDF
echo "2️⃣  Testing Simple Enrollment PDF..."
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "memberName": "Test User",
      "memberId": "TEST-001"
    }
  }' \
  -o "$OUTPUT_DIR/test-simple-enrollment.pdf" -w "\nHTTP Status: %{http_code}\n"
ls -lh "$OUTPUT_DIR/test-simple-enrollment.pdf"
echo ""

# Test 3: Complex Enrollment PDF
echo "3️⃣  Testing Complex Enrollment PDF..."
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
        "applicationId": "APP-TEST-002",
        "applicants": [
          {
            "relationship": "PRIMARY",
            "demographic": {
              "firstName": "Alice",
              "lastName": "Test"
            }
          }
        ]
      }
    }
  }' \
  -o "$OUTPUT_DIR/test-complex-enrollment.pdf" -w "\nHTTP Status: %{http_code}\n"
ls -lh "$OUTPUT_DIR/test-complex-enrollment.pdf"
echo ""

# Test 4: Excel Generation
echo "4️⃣  Testing Excel Generation..."
curl -X POST "$BASE_URL/api/excel/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "templatePath": "enrollment-summary.xlsx",
    "cellMappings": {
      "ApplicationId": "applicationId",
      "PrimaryFirstName": "firstName",
      "PrimaryLastName": "lastName"
    },
    "payload": {
      "applicationId": "EXCEL-TEST-001",
      "firstName": "Bob",
      "lastName": "Tester"
    }
  }' \
  -o "$OUTPUT_DIR/test-enrollment.xlsx" -w "\nHTTP Status: %{http_code}\n"
ls -lh "$OUTPUT_DIR/test-enrollment.xlsx"
echo ""

# Test 5: Preview Config
echo "5️⃣  Testing Config Preview..."
curl -X POST "$BASE_URL/api/enrollment/preview-config" \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  }' | jq .
echo ""

echo "=============================================="
echo "✅ Test Complete! Check files in: $OUTPUT_DIR"
echo ""
echo "Generated files:"
ls -lh "$OUTPUT_DIR"/test-*
