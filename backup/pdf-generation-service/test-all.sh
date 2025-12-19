#!/bin/bash

echo "=========================================="
echo "PDF Merge Service - Test Suite"
echo "=========================================="
echo ""

# Create output directory
mkdir -p output

# Health check
echo "1. Health check..."
HEALTH=$(curl -s http://localhost:8080/api/pdf/health)
if [ $? -eq 0 ]; then
  echo "   ✓ Service is running"
  echo "   Response: $HEALTH"
else
  echo "   ✗ Service is not responding"
  echo "   Make sure the service is running: mvn spring-boot:run"
  exit 1
fi
echo ""

# Test 1: Simple merge
echo "2. Testing simple merge request..."
curl -s -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/01-simple-merge-request.json \
  --output output/healthcare-report.pdf

if [ -f output/healthcare-report.pdf ] && [ -s output/healthcare-report.pdf ]; then
  SIZE=$(ls -lh output/healthcare-report.pdf | awk '{print $5}')
  echo "   ✓ Generated healthcare-report.pdf ($SIZE)"
else
  echo "   ✗ Failed to generate healthcare-report.pdf"
fi
echo ""

# Test 2: Minimal merge
echo "3. Testing minimal merge request..."
curl -s -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/02-minimal-merge-request.json \
  --output output/simple-report.pdf

if [ -f output/simple-report.pdf ] && [ -s output/simple-report.pdf ]; then
  SIZE=$(ls -lh output/simple-report.pdf | awk '{print $5}')
  echo "   ✓ Generated simple-report.pdf ($SIZE)"
else
  echo "   ✗ Failed to generate simple-report.pdf"
fi
echo ""

# Test 3: With conditionals
echo "4. Testing with conditionals request..."
curl -s -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/03-with-conditionals-request.json \
  --output output/detailed-report.pdf

if [ -f output/detailed-report.pdf ] && [ -s output/detailed-report.pdf ]; then
  SIZE=$(ls -lh output/detailed-report.pdf | awk '{print $5}')
  echo "   ✓ Generated detailed-report.pdf ($SIZE)"
else
  echo "   ✗ Failed to generate detailed-report.pdf"
fi
echo ""

echo "=========================================="
echo "Test Summary"
echo "=========================================="
ls -lh output/*.pdf 2>/dev/null | awk '{print $9, "-", $5}'
echo ""
echo "All tests completed. Check output/ directory for generated PDFs."
