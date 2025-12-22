#!/bin/bash

# Test Composition System

set -e

echo "=========================================="
echo "PDF Template Composition System Tests"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080/api/document/generate"
OUTPUT_DIR="output"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

# Test 1: Basic Composition
echo "Test 1: Basic Composition (base only)"
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d @test-composition-request.json \
  -o "$OUTPUT_DIR/01-basic-composition.pdf" \
  -w "\n  HTTP %{http_code}, Size: %{size_download} bytes\n" \
  -s

if [ -f "$OUTPUT_DIR/01-basic-composition.pdf" ] && [ -s "$OUTPUT_DIR/01-basic-composition.pdf" ]; then
    echo "  ✅ Basic composition successful"
else
    echo "  ❌ Basic composition failed"
    exit 1
fi
echo ""

# Test 2: Field-Level Override
echo "Test 2: Field-Level Override (partial overrides)"
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d @test-field-override-request.json \
  -o "$OUTPUT_DIR/02-field-override.pdf" \
  -w "\n  HTTP %{http_code}, Size: %{size_download} bytes\n" \
  -s

if [ -f "$OUTPUT_DIR/02-field-override.pdf" ] && [ -s "$OUTPUT_DIR/02-field-override.pdf" ]; then
    echo "  ✅ Field override successful"
else
    echo "  ❌ Field override failed"
    exit 1
fi
echo ""

echo "=========================================="
echo "All Composition Tests Passed! ✅"
echo "=========================================="
echo ""
echo "Generated PDFs:"
ls -lh "$OUTPUT_DIR"/*.pdf | awk '{print "  " $9 " - " $5}'
echo ""

echo "Test Summary:"
echo "  1. Base composition: Loads base template and generates PDF"
echo "  2. Field overrides: Overrides specific fields while inheriting others"
echo ""

echo "How to view PDFs:"
echo "  - Download from workspace: output/*.pdf"
echo "  - Or use: xdg-open output/01-basic-composition.pdf"
echo ""

echo "Next Steps:"
echo "  1. Add more component files (products, markets, states)"
echo "  2. Test multi-component composition"
echo "  3. Test conditional sections with payload data"
echo "  4. Verify deep merge semantics with complex nested structures"
