# 🧪 API Testing Resources - PDF Generation Service

Complete testing toolkit for the PDF and Excel Generation Service APIs.

## 📦 What's Included

| File | Purpose | Size |
|------|---------|------|
| **postman-collection.json** | Complete Postman collection (17 endpoints) | 18KB |
| **postman-environment-local.json** | Local environment (localhost:8080) | 380B |
| **postman-environment-dev.json** | Dev/remote environment template | 385B |
| **POSTMAN-GUIDE.md** | Comprehensive Postman usage guide | 6.2KB |
| **CURL-EXAMPLES.md** | cURL command reference | 9KB+ |
| **test-postman-requests.sh** | Automated test script | 2.8KB |

## 🚀 Quick Start Options

### Option 1: Using Postman (Recommended for GUI)

1. **Import Collection:**
   ```bash
   # In Postman:
   # Import → Select postman-collection.json
   # Import → Select postman-environment-local.json
   # Select "PDF Service - Local" environment
   # Click any request → Send
   ```

2. **Command Line (Newman):**
   ```bash
   npm install -g newman
   newman run postman-collection.json -e postman-environment-local.json
   ```

### Option 2: Using cURL (Command Line)

```bash
# Set variables
export BASE_URL="http://localhost:8080"
export OUTPUT_DIR="/workspaces/demo/output"

# Quick test
curl -X POST "$BASE_URL/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{"enrollment": {"products": ["medical"], "marketCategory": "individual", "state": "CA"}, "payload": {"memberName": "Test"}}' \
  -o output.pdf

# Or see CURL-EXAMPLES.md for all endpoints
```

### Option 3: Automated Test Script

```bash
cd /workspaces/demo/demoproject/pdf-generation-service
./test-postman-requests.sh
```

## 📋 API Endpoints Summary

### PDF Generation (9 endpoints)
- ✅ `/generate` - Simple PDF from template
- ✅ `/api/enrollment/generate` - Enrollment PDF (simple & complex)
- ✅ `/api/enrollment/preview-config` - Preview config selection
- ✅ `/api/document/generate` - Merge multiple PDFs

### Excel Generation (5 endpoints)
- ✅ `/api/excel/generate` - Basic Excel generation
- ✅ `/api/excel/generate-from-config` - Using YAML config
- ✅ `/api/excel/generate-with-preprocessing` - With data preprocessing
- ✅ `/api/excel/generate-with-tables` - With dynamic tables
- ✅ `/api/excel/generate-complete` - All features combined

### Health & Debug (3 endpoints)
- ✅ `/api/pdf/health` - Service health
- ✅ `/actuator/health` - Spring Boot health
- ✅ `/internal/mapping-order` - Debug mappings

## 💡 Common Use Cases

### 1. Generate Simple Enrollment PDF
```bash
# Postman: "Generate Enrollment PDF (Simple)"
# cURL: See CURL-EXAMPLES.md → Section 2
# Expected: 23KB, 11-page PDF
```

### 2. Generate Complex Enrollment with Applicants
```bash
# Postman: "Generate Enrollment PDF (Complex)"
# cURL: See CURL-EXAMPLES.md → Section 3
# Handles: PRIMARY, SPOUSE, DEPENDENTS, addresses, products
# Expected: 23KB, 11-page PDF with preprocessing
```

### 3. Generate Excel File
```bash
# Postman: "Generate Excel - Simple"
# cURL: See CURL-EXAMPLES.md → Excel Section 1
# Requires: Template with named ranges
# Expected: 6-7KB .xlsx file
```

### 4. Preview Configuration
```bash
# Postman: "Preview Enrollment Config"
# Returns: Selected config filename (e.g., "dental-medical-individual-ca.yml")
```

## 🔧 Setup Requirements

### Prerequisites
- ✅ Service running on `localhost:8080`
- ✅ Templates available in `config-repo/` directories
- ✅ Excel templates have **named ranges** defined

### Start Service
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

### Verify Service
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

## 📖 Documentation Guide

| Document | When to Use |
|----------|-------------|
| **POSTMAN-GUIDE.md** | Full Postman setup, tips, troubleshooting |
| **CURL-EXAMPLES.md** | Command-line testing, scripting, CI/CD |
| **README.md** | Service architecture, configuration |
| **This file** | Quick reference and overview |

## 🎯 Testing Strategy

### Step 1: Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Step 2: Simple PDF
```bash
# Use Postman: "Generate Enrollment PDF (Simple)"
# Or run: ./test-postman-requests.sh
```

### Step 3: Complex PDF
```bash
# Use Postman: "Generate Enrollment PDF (Complex)"
# Verify preprocessing works with nested applicants
```

### Step 4: Excel Generation
```bash
# Use Postman: "Generate Excel - Simple"
# Verify template with named ranges
```

### Step 5: Preview & Debug
```bash
# Use Postman: "Preview Enrollment Config"
# Use Postman: "Mapping Order Debug"
```

## 🐛 Troubleshooting Quick Reference

| Issue | Solution |
|-------|----------|
| Empty PDF/Excel | Check logs, verify templates exist |
| 500 Error | Check template path in request |
| ClassCastException | Verify YAML rules use correct types |
| Template not found (Excel) | Ensure named ranges are defined |
| Connection refused | Start service with `mvn spring-boot:run` |

**Detailed troubleshooting:** See POSTMAN-GUIDE.md → "Troubleshooting" section

## 📊 Test Coverage

### Covered Scenarios
- ✅ Simple flat payload → PDF
- ✅ Complex nested applicants → PDF with preprocessing
- ✅ Multiple products (medical, dental, vision)
- ✅ Different market categories (individual, group)
- ✅ State-specific templates (CA, NY, TX, FL)
- ✅ Excel with cell mappings
- ✅ Excel with dynamic tables
- ✅ Config selection logic
- ✅ Payload preprocessing and transformation

### Sample Data Available
- `request*.json` - Sample request files
- `config-repo/examples/complex-application-structure.json` - Full application
- Templates in `config-repo/templates/` - FreeMarker templates
- Excel template: `config-repo/excel-templates/enrollment-summary.xlsx`

## 🔗 Related Resources

- **Service Configuration:** `application.yml`
- **Preprocessing Rules:** `src/main/resources/preprocessing/standard-enrollment-rules.yml`
- **Template Configs:** `config-repo/templates/` (YAML files)
- **Log Files:** `/tmp/spring-*.log`

## 📞 Support

For issues or questions:
1. Check logs: `tail -100 /tmp/spring-*.log`
2. Verify templates: `ls -la config-repo/templates/`
3. Review POSTMAN-GUIDE.md troubleshooting section
4. Check CURL-EXAMPLES.md for debugging commands

---

**Version:** 1.0.0  
**Last Updated:** December 17, 2025  
**Total Endpoints:** 17  
**Test Coverage:** PDF Generation, Excel Generation, Health Checks, Debug Tools
