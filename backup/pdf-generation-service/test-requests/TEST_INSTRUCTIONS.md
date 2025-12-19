# PDF Merge Service - Testing Instructions

## Prerequisites

1. **Start the service:**
   ```bash
   cd /workspaces/demo/demoproject/pdf-generation-service
   mvn spring-boot:run
   ```
   
2. **Verify service is running:**
   ```bash
   curl http://localhost:8080/api/pdf/health
   ```
   Expected response:
   ```json
   {"status":"UP","service":"FlexiblePdfMergeService"}
   ```

## Test Endpoints

### 1. Simple Merge Request
Tests basic PDF merge with FreeMarker HTML pages and PDFBox pages.

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/01-simple-merge-request.json \
  --output output/healthcare-report.pdf
```

**What it tests:**
- Basic header/footer rendering
- FreeMarker template processing (member healthcare plans)
- PDFBox generator integration (cover page, charts)
- Page numbering
- Variable substitution (companyName, date)

---

### 2. Minimal Merge Request
Tests with minimal data to verify the system handles sparse data correctly.

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/02-minimal-merge-request.json \
  --output output/simple-report.pdf
```

**What it tests:**
- Single member with minimal plans
- Empty dental and vision plan arrays
- Conditional sections NOT triggered (includeDetailedBreakdown=false)
- Basic document structure

---

### 3. With Conditionals Request
Tests conditional sections based on payload flags.

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/03-with-conditionals-request.json \
  --output output/detailed-report.pdf
```

**What it tests:**
- Conditional sections (includeDetailedBreakdown=true, includeLegalDisclosures=true)
- Multiple members with many plans
- Complex variable substitution (reportId, reportTitle)
- Full document with all optional sections

---

## Alternative: Using VS Code REST Client

If you have the REST Client extension installed, create a file `test-requests.http`:

```http
### Health Check
GET http://localhost:8080/api/pdf/health

### Simple Merge Request
POST http://localhost:8080/api/document/generate
Content-Type: application/json

< test-requests/01-simple-merge-request.json

### Minimal Merge Request
POST http://localhost:8080/api/document/generate
Content-Type: application/json

< test-requests/02-minimal-merge-request.json

### With Conditionals Request
POST http://localhost:8080/api/document/generate
Content-Type: application/json

< test-requests/03-with-conditionals-request.json
```

Click "Send Request" above each test to execute.

---

## Test Using Postman

1. **Import Collection:**
   - Create new request
   - Method: `POST`
   - URL: `http://localhost:8080/api/document/generate`
   - Headers: `Content-Type: application/json`
   - Body: Copy content from any test request JSON file
   - Click "Send"
   - Save response as PDF file

2. **Import all tests as collection:**
   ```bash
   # Generate Postman collection
   cat > test-requests/postman-collection.json << 'EOF'
   {
     "info": {
       "name": "PDF Merge Service Tests",
       "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
     },
     "item": [
       {
         "name": "Health Check",
         "request": {
           "method": "GET",
           "header": [],
           "url": "http://localhost:8080/api/pdf/health"
         }
       },
       {
         "name": "Simple Merge",
         "request": {
           "method": "POST",
           "header": [{"key": "Content-Type", "value": "application/json"}],
           "body": {
             "mode": "raw",
             "raw": "{{simple-merge-request}}"
           },
           "url": "http://localhost:8080/api/document/generate"
         }
       }
     ]
   }
   EOF
   ```

---

## Expected Results

### Successful Response
- **Status Code:** `200 OK`
- **Content-Type:** `application/pdf`
- **Content-Disposition:** `attachment; filename="<specified-filename>.pdf"`
- **Body:** Binary PDF data

### Verify PDF Contents
Open the generated PDF and verify:
1. **Cover page** (PDFBox generated)
2. **Headers on pages 2+:**
   - Left: Company name
   - Center: Report title
   - Right: Current date
3. **Member healthcare plans table** (FreeMarker template)
4. **Footers on all pages:**
   - Left: "Confidential"
   - Center: Page numbers
   - Right: Copyright notice
5. **Premium chart page** (PDFBox generated - if implemented)
6. **Bookmarks** in PDF sidebar

---

## Troubleshooting

### Error: "Failed to load PDF merge config"
**Cause:** Configuration file not found  
**Solution:** Ensure `pdf-merge-config.yml` exists at:
- `/workspaces/demo/demoproject/config-repo/pdf-merge-config.yml` (default)
- Or set `config.repo.path` in `application.properties`

### Error: "No PDFBox generator found with name: X"
**Cause:** Referenced generator not implemented  
**Solution:** 
1. Check `pdf-merge-config.yml` section templates
2. Implement missing generator or disable section:
   ```yaml
   - name: "Missing Section"
     enabled: false
   ```

### Error: "Template not found: X.ftl"
**Cause:** FreeMarker template doesn't exist  
**Solution:** 
1. Create template in `src/main/resources/templates/`
2. Or use existing template name like `member-healthcare-plans.ftl`

### Headers/Footers Not Showing
**Check:**
1. `header.enabled: true` in config
2. `startPage` is within document range
3. Payload contains required variables (e.g., `companyName`)

### PDF Opens But Shows Errors
**Check service logs:**
```bash
tail -f logs/application.log
```
Look for:
- Template processing errors
- PDFBox generation exceptions
- Variable substitution warnings

---

## Performance Testing

### Load Test with Apache Bench
```bash
# Generate 100 requests with concurrency of 10
ab -n 100 -c 10 -p test-requests/01-simple-merge-request.json \
   -T application/json \
   http://localhost:8080/api/document/generate
```

### Memory Usage Test
```bash
# Monitor memory while generating PDFs
while true; do
  curl -X POST http://localhost:8080/api/document/generate \
    -H "Content-Type: application/json" \
    -d @test-requests/03-with-conditionals-request.json \
    --output /dev/null
  sleep 1
done
```

Monitor with:
```bash
jconsole
# Or
jvisualvm
```

---

## Creating Output Directory

```bash
mkdir -p /workspaces/demo/pdf-generation-service/output
```

---

## Quick Test Script

Create `test-all.sh`:
```bash
#!/bin/bash

echo "Testing PDF Merge Service..."

# Create output directory
mkdir -p output

# Health check
echo "1. Health check..."
curl -s http://localhost:8080/api/pdf/health | jq .

# Test 1: Simple merge
echo "2. Testing simple merge..."
curl -s -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/01-simple-merge-request.json \
  --output output/healthcare-report.pdf

if [ -f output/healthcare-report.pdf ]; then
  SIZE=$(ls -lh output/healthcare-report.pdf | awk '{print $5}')
  echo "   ✓ Generated healthcare-report.pdf ($SIZE)"
else
  echo "   ✗ Failed to generate healthcare-report.pdf"
fi

# Test 2: Minimal merge
echo "3. Testing minimal merge..."
curl -s -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/02-minimal-merge-request.json \
  --output output/simple-report.pdf

if [ -f output/simple-report.pdf ]; then
  SIZE=$(ls -lh output/simple-report.pdf | awk '{print $5}')
  echo "   ✓ Generated simple-report.pdf ($SIZE)"
else
  echo "   ✗ Failed to generate simple-report.pdf"
fi

# Test 3: With conditionals
echo "4. Testing with conditionals..."
curl -s -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/03-with-conditionals-request.json \
  --output output/detailed-report.pdf

if [ -f output/detailed-report.pdf ]; then
  SIZE=$(ls -lh output/detailed-report.pdf | awk '{print $5}')
  echo "   ✓ Generated detailed-report.pdf ($SIZE)"
else
  echo "   ✗ Failed to generate detailed-report.pdf"
fi

echo ""
echo "All tests completed. Check output/ directory for generated PDFs."
```

Run with:
```bash
chmod +x test-all.sh
./test-all.sh
```
