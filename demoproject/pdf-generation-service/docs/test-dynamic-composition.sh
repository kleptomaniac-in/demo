#!/bin/bash

# Test script for Dynamic Composition Fallback
# This script tests the fallback mechanism from pre-generated to dynamic configs

echo "========================================="
echo "Dynamic Composition Fallback Test Script"
echo "========================================="
echo ""

# Set base URL
BASE_URL="http://localhost:8080"

# Test 1: Pre-generated config (should exist)
echo "Test 1: Pre-generated Config (dental-individual-ca)"
echo "--------------------------------------------"
curl -X POST "$BASE_URL/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["dental"],
      "marketCategory": "individual",
      "state": "CA",
      "subscriber": {
        "firstName": "John",
        "lastName": "Doe",
        "ssn": "123-45-6789",
        "dateOfBirth": "1980-01-01"
      }
    }
  }' \
  --output /tmp/test1-dental-individual-ca.pdf \
  -w "\nHTTP Status: %{http_code}\n"
  
if [ -f /tmp/test1-dental-individual-ca.pdf ]; then
  echo "✓ PDF generated successfully using pre-generated config"
  ls -lh /tmp/test1-dental-individual-ca.pdf
else
  echo "✗ PDF generation failed"
fi
echo ""

# Test 2: Rare combination requiring dynamic composition
echo "Test 2: Rare Combination (vision-life-medicare-WY) - Dynamic Composition"
echo "--------------------------------------------"
curl -X POST "$BASE_URL/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["vision", "life"],
      "marketCategory": "medicare",
      "state": "WY",
      "subscriber": {
        "firstName": "Jane",
        "lastName": "Smith",
        "ssn": "987-65-4321",
        "dateOfBirth": "1975-05-15"
      }
    }
  }' \
  --output /tmp/test2-dynamic-composition.pdf \
  -w "\nHTTP Status: %{http_code}\n"
  
if [ -f /tmp/test2-dynamic-composition.pdf ]; then
  echo "✓ PDF generated successfully using dynamic composition"
  ls -lh /tmp/test2-dynamic-composition.pdf
else
  echo "✗ PDF generation failed"
fi
echo ""

# Test 3: Another rare combination
echo "Test 3: Multi-Product Rare (dental-vision-large-group-MT) - Dynamic"
echo "--------------------------------------------"
curl -X POST "$BASE_URL/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["dental", "vision"],
      "marketCategory": "large-group",
      "state": "MT",
      "subscriber": {
        "firstName": "Bob",
        "lastName": "Johnson",
        "ssn": "555-12-3456",
        "dateOfBirth": "1990-12-31"
      }
    }
  }' \
  --output /tmp/test3-multi-dynamic.pdf \
  -w "\nHTTP Status: %{http_code}\n"
  
if [ -f /tmp/test3-multi-dynamic.pdf ]; then
  echo "✓ PDF generated successfully using dynamic composition"
  ls -lh /tmp/test3-multi-dynamic.pdf
else
  echo "✗ PDF generation failed"
fi
echo ""

# Test 4: Common combination (should use pre-generated)
echo "Test 4: Common Combination (medical-individual-ca) - Pre-generated"
echo "--------------------------------------------"
curl -X POST "$BASE_URL/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["medical"],
      "marketCategory": "individual",
      "state": "CA",
      "subscriber": {
        "firstName": "Alice",
        "lastName": "Williams",
        "ssn": "111-22-3333",
        "dateOfBirth": "1985-03-20"
      }
    }
  }' \
  --output /tmp/test4-medical-individual-ca.pdf \
  -w "\nHTTP Status: %{http_code}\n"
  
if [ -f /tmp/test4-medical-individual-ca.pdf ]; then
  echo "✓ PDF generated successfully using pre-generated config"
  ls -lh /tmp/test4-medical-individual-ca.pdf
else
  echo "✗ PDF generation failed"
fi
echo ""

# Summary
echo "========================================="
echo "Test Summary"
echo "========================================="
echo "Generated PDFs:"
ls -lh /tmp/test*.pdf 2>/dev/null || echo "No PDFs generated"
echo ""
echo "Check server logs for messages like:"
echo "  ✓ Loaded pre-generated config: <name>"
echo "  ✗ Config file not found: <name>"
echo "  → Falling back to dynamic composition..."
echo ""
