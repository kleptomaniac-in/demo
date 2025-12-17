# Postman Collection for PDF Generation Service

Complete API testing collection for the PDF and Excel Generation Service.

## 📦 Files Included

- **postman-collection.json** - Complete API collection with all endpoints
- **postman-environment-local.json** - Local development environment (localhost:8080)
- **postman-environment-dev.json** - Remote/dev environment template

## 🚀 Quick Start

### Import into Postman

1. **Import Collection:**
   - Open Postman
   - Click **Import** button
   - Select `postman-collection.json`
   - Collection appears in your workspace

2. **Import Environment:**
   - Click **Import** again
   - Select `postman-environment-local.json`
   - Select environment from dropdown (top-right)

3. **Start Testing:**
   - Ensure your service is running: `mvn spring-boot:run`
   - Select any request from the collection
   - Click **Send**

### Command Line Usage (Newman)

Install Newman (Postman CLI):
```bash
npm install -g newman
```

Run entire collection:
```bash
newman run postman-collection.json -e postman-environment-local.json
```

Run specific folder:
```bash
newman run postman-collection.json -e postman-environment-local.json --folder "PDF Generation"
```

## 📋 Available Endpoints

### PDF Generation (9 endpoints)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/generate` | POST | Generate simple PDF from template |
| `/api/enrollment/generate` | POST | Generate enrollment PDF (simple) |
| `/api/enrollment/generate` | POST | Generate enrollment PDF (complex applicants) |
| `/api/enrollment/preview-config` | POST | Preview config selection |
| `/api/pdf/merge` | POST | Merge multiple PDFs |
| `/api/enrollment-complex/generate` | POST | Complex enrollment generation |
| `/api/enrollment-complex/preview-flattened` | POST | Preview payload flattening |
| `/api/enrollment-complex/applicant-summary` | POST | Get applicant summary |

### Excel Generation (5 endpoints)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/excel/generate` | POST | Generate Excel from template |
| `/api/excel/generate-from-config` | POST | Generate using YAML config |
| `/api/excel/generate-with-preprocessing` | POST | Generate with data preprocessing |
| `/api/excel/generate-with-tables` | POST | Generate with dynamic tables |
| `/api/excel/generate-complete` | POST | Generate with all features |

### Health & Debug (3 endpoints)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/pdf/health` | GET | PDF service health check |
| `/actuator/health` | GET | Spring Boot actuator |
| `/internal/mapping-order` | GET | Debug request mappings |

## 💡 Usage Tips

### Saving Response Files

Postman automatically saves binary responses (PDF/Excel). To save to disk:

1. After sending request, click **Save Response**
2. Choose location (e.g., `/workspaces/demo/output/`)
3. File is saved as `enrollment.pdf` or `output.xlsx`

### Testing Simple vs Complex Payloads

**Simple Payload** (flat structure):
```json
{
  "enrollment": {...},
  "payload": {
    "memberName": "John Doe",
    "memberId": "12345"
  }
}
```

**Complex Payload** (nested with applicants):
```json
{
  "enrollment": {...},
  "payload": {
    "application": {
      "applicants": [
        {"relationship": "PRIMARY", ...},
        {"relationship": "SPOUSE", ...}
      ]
    }
  }
}
```

### Excel Template Requirements

Excel templates **must have named ranges** defined:
- Create template in Excel/LibreOffice
- Define names for cells (Formulas → Define Name)
- Use names in `cellMappings` (e.g., "ApplicationId", "PrimaryFirstName")

A template with named ranges is available at:
`config-repo/excel-templates/enrollment-summary.xlsx`

## 🔧 Environment Variables

| Variable | Local Value | Description |
|----------|-------------|-------------|
| `baseUrl` | `http://localhost:8080` | Service base URL |
| `outputPath` | `/workspaces/demo/output` | Where to save files |

## 📝 Example Test Scenarios

### Scenario 1: Generate Simple Enrollment PDF
```bash
# Request: "Generate Enrollment PDF (Simple)"
# Expected: 23KB PDF with 11 pages
# Contains: Cover, member info, medical/dental coverage, CA disclosures
```

### Scenario 2: Generate Complex Enrollment PDF
```bash
# Request: "Generate Enrollment PDF (Complex)"
# Expected: 23KB PDF with 11 pages
# Preprocessing separates PRIMARY, SPOUSE, DEPENDENTS
# Handles 4+ dependents with overflow calculation
```

### Scenario 3: Generate Excel File
```bash
# Request: "Generate Excel - Simple"
# Expected: 6-7KB .xlsx file
# Populates named cells with data from payload
```

### Scenario 4: Preview Config Selection
```bash
# Request: "Preview Enrollment Config"
# Expected: JSON showing selected config file
# Example: "dental-medical-individual-ca.yml"
```

## 🐛 Troubleshooting

### Empty PDF/Excel Files

**Problem:** Response is 0 bytes
**Solutions:**
- Check logs: `tail -100 /tmp/spring-*.log`
- Verify templates exist in `config-repo/` or `src/main/resources/`
- For Excel: Ensure template has named ranges defined
- Check preprocessing rules if using complex payloads

### 500 Internal Server Error

**Problem:** Template not found
**Solutions:**
- Excel templates: `config-repo/excel-templates/<name>.xlsx`
- PDF templates: `config-repo/templates/<name>.ftl`
- Check `templatePath` or `configPath` in request body

### ClassCastException in Logs

**Problem:** Type mismatch in preprocessing
**Solutions:**
- Verify YAML rules use correct types (strings vs numbers)
- For subtract operations: values can be keys (strings) or literals (numbers)
- Check that JSON numeric values are handled as Number type

## 📚 Additional Resources

- **Sample Request Files:** Check `request*.json` files in service directory
- **Config Examples:** See `config-repo/templates/` for YAML configs
- **Complex Application Structure:** `config-repo/examples/complex-application-structure.json`

## 🔗 Quick Links

- **Service README:** [README.md](README.md)
- **Template Documentation:** [templates/README.md](src/main/resources/templates/README.md)
- **API Documentation:** Run service and visit `/swagger-ui.html` (if enabled)

---

**Version:** 1.0.0  
**Last Updated:** December 16, 2025  
**Maintainer:** PDF Generation Service Team
