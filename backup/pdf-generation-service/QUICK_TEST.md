# Quick Test Guide

## Start Service
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

## Test Methods

### Option 1: Automated Test Script (Easiest)
```bash
cd /workspaces/demo/pdf-generation-service
./test-all.sh
```

### Option 2: Manual cURL Commands

**Health Check:**
```bash
curl http://localhost:8080/api/pdf/health
```

**Test 1 - Simple Healthcare Report:**
```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/01-simple-merge-request.json \
  --output healthcare-report.pdf
```

**Test 2 - Minimal Report:**
```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/02-minimal-merge-request.json \
  --output simple-report.pdf
```

**Test 3 - Detailed Report with Conditionals:**
```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-requests/03-with-conditionals-request.json \
  --output detailed-report.pdf
```

### Option 3: VS Code REST Client
1. Open `test-requests.http` in VS Code
2. Click "Send Request" above any test
3. Response will show in VS Code panel

### Option 4: Postman
1. Import request:
   - Method: POST
   - URL: `http://localhost:8080/api/document/generate`
   - Headers: `Content-Type: application/json`
   - Body: Paste from any `test-requests/*.json` file
2. Click Send
3. Save response to file

## Expected Output
- Status: 200 OK
- Content-Type: application/pdf
- File size: ~20-100 KB (depending on data)

## View Results
```bash
ls -lh output/*.pdf
open output/healthcare-report.pdf  # macOS
xdg-open output/healthcare-report.pdf  # Linux
```

## Verify PDF Contents
Open generated PDF and check:
- ✓ Cover page (PDFBox)
- ✓ Header (pages 2+): Company name | Report title | Date
- ✓ Member healthcare plans table (FreeMarker)
- ✓ Footer (all pages): Confidential | Page numbers | Copyright
- ✓ Bookmarks (if configured)

## Troubleshooting
See `test-requests/TEST_INSTRUCTIONS.md` for detailed troubleshooting guide.
