# Multi-Page Composition with Mixed Template Types

## ✅ Yes! You Can Define Multi-Page PDFs with Mixed Template Types

The current implementation in `FlexiblePdfMergeService` fully supports compositions with multiple pages, where each page can use:
- **FreeMarker** templates (HTML-based)
- **PDFBox** generators (programmatic)
- **AcroForm** templates (fillable PDF forms)

---

## 📋 Configuration Structure

### Basic Multi-Type Composition

```yaml
pdfMerge:
  sections:
    # Page 1: PDFBox-generated cover page
    - name: cover-page
      type: pdfbox
      template: cover-page-generator
      enabled: true
    
    # Page 2-3: FreeMarker HTML template
    - name: member-details
      type: freemarker
      template: templates/member-healthcare-plans.ftl
      enabled: true
    
    # Page 4: AcroForm fillable PDF
    - name: state-disclosure
      type: acroform
      template: ca-dmhc-disclosure-form.pdf
      enabled: true
      fieldMapping:
        "MemberName": "member.fullName"
        "MemberId": "member.memberId"
        "PlanName": "selectedPlans.medical.planName"
    
    # Page 5-6: Another FreeMarker template
    - name: coverage-details
      type: freemarker
      template: templates/products/medical-coverage-details.ftl
      enabled: true
```

---

## 🎯 Section Types Explained

### 1. **FreeMarker Type** (`type: freemarker`)

**Description:** HTML-based templates rendered to PDF via FreeMarker → HTML → PDF conversion

**Configuration:**
```yaml
- name: member-info
  type: freemarker
  template: templates/member-info.ftl  # Path to .ftl file
  enabled: true
```

**Template Example** (`member-info.ftl`):
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        .member-card { border: 1px solid #ccc; padding: 20px; }
    </style>
</head>
<body>
    <div class="member-card">
        <h1>Member Information</h1>
        <p>Name: ${payload.member.firstName} ${payload.member.lastName}</p>
        <p>Member ID: ${payload.member.memberId}</p>
        <p>Plan: ${payload.selectedPlans.medical.planName}</p>
    </div>
</body>
</html>
```

**Payload Access:** `${payload.fieldName}`

---

### 2. **PDFBox Type** (`type: pdfbox`)

**Description:** Programmatically generated PDFs using Java code

**Configuration:**
```yaml
- name: cover-page
  type: pdfbox
  template: cover-page-generator  # Name of registered generator
  enabled: true
```

**Generator Implementation:**
```java
@Component("cover-page-generator")
public class CoverPageGenerator implements PdfBoxGenerator {
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);
        
        PDPageContentStream content = new PDPageContentStream(doc, page);
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 24);
        content.newLineAtOffset(100, 700);
        content.showText("Enrollment Application");
        content.endText();
        content.close();
        
        return doc;
    }
}
```

**Registration:** Spring auto-detects `@Component` with generator name

---

### 3. **AcroForm Type** (`type: acroform`)

**Description:** Fillable PDF forms with form fields mapped to payload data

**Configuration:**
```yaml
- name: state-disclosure
  type: acroform
  template: ca-dmhc-disclosure-form.pdf  # Path to PDF with form fields
  enabled: true
  fieldMapping:
    # PDF Field Name: payload.path
    "MemberName": "member.fullName"
    "MemberId": "member.memberId"
    "DateOfBirth": "member.dateOfBirth"
    "PlanName": "selectedPlans.medical.planName"
    "MonthlyPremium": "selectedPlans.medical.premium"
    "AcknowledgeTerms": "acknowledgments.terms"
```

**Key Points:**
- PDF must have form fields defined (created in Adobe Acrobat or similar)
- Field names in PDF must match keys in `fieldMapping`
- Supports text fields, checkboxes, radio buttons, dropdowns

---

## 📝 AcroForm Field Mapping Details

### Field Mapping Syntax

```yaml
fieldMapping:
  "PDFFieldName": "payload.path.to.value"
```

**How It Works:**
1. Left side: **Exact field name** from the PDF form
2. Right side: **Dot-notation path** to data in payload
3. Service resolves nested paths automatically

### Example with Nested Data

**Payload:**
```json
{
  "application": {
    "applicationId": "APP-001",
    "applicants": [
      {
        "relationship": "PRIMARY",
        "demographic": {
          "firstName": "John",
          "lastName": "Smith",
          "dateOfBirth": "1980-05-15"
        }
      }
    ],
    "selectedPlans": {
      "medical": {
        "planName": "Gold PPO",
        "premium": 450.00
      }
    }
  }
}
```

**Field Mapping:**
```yaml
fieldMapping:
  # Simple paths
  "ApplicationId": "application.applicationId"
  
  # Nested objects
  "PrimaryFirstName": "application.applicants[0].demographic.firstName"
  "PrimaryLastName": "application.applicants[0].demographic.lastName"
  "DateOfBirth": "application.applicants[0].demographic.dateOfBirth"
  
  # Deep nesting
  "PlanName": "application.selectedPlans.medical.planName"
  "MonthlyPremium": "application.selectedPlans.medical.premium"
```

### Supported Field Types

| PDF Field Type | Mapping Example | Notes |
|----------------|-----------------|-------|
| **Text Field** | `"FieldName": "member.name"` | Any string value |
| **Number Field** | `"Premium": "plan.premium"` | Numbers formatted as strings |
| **Date Field** | `"DOB": "member.dateOfBirth"` | Accepts various date formats |
| **Checkbox** | `"Acknowledge": "acknowledgments.terms"` | "Yes"/"No" or true/false |
| **Radio Button** | `"Gender": "demographic.gender"` | Value matches radio option |
| **Dropdown** | `"State": "address.state"` | Value must match dropdown option |

### Array Access

**Access Array Elements:**
```yaml
fieldMapping:
  "Dependent1Name": "dependents[0].firstName"
  "Dependent2Name": "dependents[1].firstName"
  "Dependent3Name": "dependents[2].firstName"
```

**Using Preprocessor (Recommended):**
```yaml
# In preprocessing rules
- type: "extract"
  sourceKey: "applicants"
  targetKey: "primaryApplicant"
  filterField: "relationship"
  filterValue: "PRIMARY"
  maxItems: 1

# Then in fieldMapping
fieldMapping:
  "PrimaryName": "primaryApplicant.demographic.firstName"
```

---

## 🔧 Complete Example: Mixed Template Configuration

### Scenario: Healthcare Enrollment Application

**File:** `dental-medical-individual-ca-mixed.yml`

```yaml
# Composed configuration with multiple template types
base: templates/base-payer.yml
components:
  - templates/products/medical.yml
  - templates/products/dental.yml
  - templates/markets/individual.yml
  - templates/states/california.yml

pdfMerge:
  settings:
    pageNumbering: "bottom-center"
    addBookmarks: true
  
  sections:
    # ===== PAGE 1: PDFBox Generated Cover =====
    - name: cover-page
      type: pdfbox
      template: enrollment-cover-generator
      enabled: true
    
    # ===== PAGE 2-3: FreeMarker Member Info =====
    - name: member-plans
      type: freemarker
      template: templates/member-healthcare-plans.ftl
      enabled: true
    
    # ===== PAGE 4: AcroForm State Disclosure =====
    - name: ca-state-disclosure
      type: acroform
      template: ca-dmhc-disclosure-2025.pdf
      enabled: true
      fieldMapping:
        # Company info
        "PayerName": "companyName"
        "PayerLicense": "companyLicenseNumber"
        "EffectiveDate": "effectiveDate"
        
        # Member info (using preprocessed fields)
        "MemberFirstName": "primaryApplicant.firstName"
        "MemberLastName": "primaryApplicant.lastName"
        "MemberDOB": "primaryApplicant.dateOfBirth"
        "MemberID": "memberId"
        
        # Plan info
        "MedicalPlanName": "selectedMedicalPlan.name"
        "MedicalPlanType": "selectedMedicalPlan.type"
        "MonthlyPremium": "totalMonthlyPremium"
        
        # Checkboxes
        "NetworkRestrictionAck": "acknowledgments.networkRestrictions"
        "AppealProcessAck": "acknowledgments.appealProcess"
        "GrievanceRightsAck": "acknowledgments.grievanceRights"
        "LanguageAssistanceAck": "acknowledgments.languageAssistance"
    
    # ===== PAGE 5-6: FreeMarker Medical Details =====
    - name: medical-coverage
      type: freemarker
      template: templates/products/medical-coverage-details.ftl
      enabled: true
    
    # ===== PAGE 7-8: FreeMarker Dental Details =====
    - name: dental-coverage
      type: freemarker
      template: templates/products/dental-coverage-details.ftl
      enabled: true
    
    # ===== PAGE 9: AcroForm Signature Page =====
    - name: signature-page
      type: acroform
      template: enrollment-signature-form.pdf
      enabled: true
      fieldMapping:
        "ApplicantName": "primaryApplicant.fullName"
        "SignatureDate": "today"
        "ApplicationNumber": "applicationId"
        "AgentName": "agent.name"
        "AgentLicense": "agent.licenseNumber"
  
  # Bookmarks for navigation
  bookmarks:
    - section: cover-page
      title: "Application Cover"
      level: 1
    - section: member-plans
      title: "Member Information"
      level: 1
    - section: ca-state-disclosure
      title: "California State Disclosure"
      level: 1
    - section: medical-coverage
      title: "Medical Coverage Details"
      level: 1
    - section: dental-coverage
      title: "Dental Coverage Details"
      level: 1
    - section: signature-page
      title: "Signature Page"
      level: 1
```

---

## 🎨 Creating AcroForm PDF Templates

### Method 1: Adobe Acrobat Pro

1. Create PDF in any tool (Word, InDesign, etc.)
2. Open in Adobe Acrobat Pro
3. Tools → Prepare Form
4. Add form fields:
   - Text Field → Name it (e.g., "MemberName")
   - Checkbox → Name it (e.g., "AcknowledgeTerms")
   - Date Field → Name it (e.g., "DateOfBirth")
5. Save as PDF
6. Place in `config-repo/acroforms/` directory

### Method 2: LibreOffice

1. Create form in LibreOffice Writer
2. Insert → Form Controls → Text Box/Check Box
3. Set field names in Properties
4. Export as PDF (check "Create PDF form")
5. Place in `config-repo/acroforms/`

### Method 3: PDFBox Programmatically

```java
PDDocument doc = new PDDocument();
PDPage page = new PDPage();
doc.addPage(page);

PDAcroForm acroForm = new PDAcroForm(doc);
doc.getDocumentCatalog().setAcroForm(acroForm);

// Add text field
PDTextField textField = new PDTextField(acroForm);
textField.setPartialName("MemberName");
acroForm.getFields().add(textField);

doc.save("template.pdf");
```

### Verifying Field Names

```bash
# Use PDFBox command line
java -jar pdfbox-app.jar PDFDebugger template.pdf

# Or check programmatically
PDDocument doc = PDDocument.load(new File("template.pdf"));
PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
for (PDField field : form.getFields()) {
    System.out.println("Field: " + field.getFullyQualifiedName());
}
```

---

## 🔄 Dynamic Section Enabling

### Conditional Sections

```yaml
sections:
  - name: spouse-info
    type: freemarker
    template: templates/spouse-details.ftl
    enabled: true  # Always evaluated at runtime
    condition: "payload.hasSpouse"  # Optional condition

conditionalSections:
  - condition: "payload.includeDetailedBreakdown"
    sections:
      - name: detailed-breakdown
        type: freemarker
        template: templates/detailed-breakdown.ftl
        insertAfter: member-plans
```

### Section Ordering

```yaml
sections:
  - name: section-a
    type: freemarker
    template: a.ftl
  
  - name: section-c
    type: acroform
    template: c.pdf
    insertAfter: section-b  # Will be inserted after section-b if it exists
```

---

## 📍 File Locations

### Directory Structure

```
config-repo/
├── acroforms/                    # AcroForm PDF templates
│   ├── ca-dmhc-disclosure-2025.pdf
│   ├── enrollment-signature-form.pdf
│   └── state-specific-forms/
│       ├── california/
│       ├── texas/
│       └── newyork/
├── templates/                    # FreeMarker templates
│   ├── member-healthcare-plans.ftl
│   ├── products/
│   │   ├── medical-coverage-details.ftl
│   │   └── dental-coverage-details.ftl
│   └── states/
│       ├── california.ftl
│       └── texas.ftl
└── src/main/java/.../generators/ # PDFBox generators
    ├── CoverPageGenerator.java
    ├── EnrollmentCoverGenerator.java
    └── SummaryReportGenerator.java
```

### Path Resolution

**AcroForm Templates:**
```yaml
template: ca-form.pdf
# Resolves to: config-repo/acroforms/ca-form.pdf
```

**FreeMarker Templates:**
```yaml
template: templates/member-info.ftl
# Resolves to: config-repo/templates/member-info.ftl
```

**PDFBox Generators:**
```yaml
template: cover-page-generator
# Looks for Spring bean named "cover-page-generator"
```

---

## 🧪 Testing Your Configuration

### Test with Simple Payload

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
      "companyName": "HealthCare Plus",
      "memberId": "M12345",
      "primaryApplicant": {
        "firstName": "John",
        "lastName": "Smith",
        "dateOfBirth": "1980-05-15",
        "fullName": "John Smith"
      },
      "selectedMedicalPlan": {
        "name": "Gold PPO",
        "type": "PPO",
        "premium": 450.00
      },
      "totalMonthlyPremium": 450.00,
      "acknowledgments": {
        "networkRestrictions": true,
        "appealProcess": true,
        "grievanceRights": true
      }
    }
  }' \
  -o mixed-template-test.pdf
```

### Verify Sections Generated

```bash
# Check PDF page count
pdfinfo mixed-template-test.pdf | grep Pages

# Check bookmarks
pdftk mixed-template-test.pdf dump_data | grep Bookmark

# Extract specific page for inspection
pdftk mixed-template-test.pdf cat 4 output page4.pdf
```

---

## 🐛 Troubleshooting

### Issue: "PDF does not contain an AcroForm"

**Cause:** PDF file doesn't have form fields

**Solution:**
1. Open PDF in Adobe Acrobat
2. Check if form fields exist (Tools → Prepare Form)
3. Recreate with form fields or use different template type

### Issue: "Field not found: MemberName"

**Cause:** Field name in `fieldMapping` doesn't match PDF field name

**Solution:**
```java
// List all fields in PDF
PDDocument doc = PDDocument.load(new File("template.pdf"));
PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
for (PDField field : form.getFields()) {
    System.out.println(field.getFullyQualifiedName());
}
```

### Issue: Empty values in form fields

**Cause:** Payload path doesn't resolve correctly

**Solution:**
1. Check payload structure matches path
2. Use preprocessing to flatten nested data
3. Add debug logging:
```yaml
fieldMapping:
  "TestField": "debug.path.to.value"
```

### Issue: Section not appearing in final PDF

**Causes:**
- `enabled: false`
- Conditional not met
- Template file not found
- Error in section generation (check logs)

**Debug:**
```bash
# Check logs
tail -100 /tmp/spring-*.log | grep -A 5 "section-name"

# Test section independently
curl POST /api/pdf/test-section -d '{"section": "section-name", "payload": {...}}'
```

---

## 📚 Related Documentation

- [FlexiblePdfMergeService.java](src/main/java/com/example/service/FlexiblePdfMergeService.java) - Main composition engine
- [AcroFormFillService.java](src/main/java/com/example/service/AcroFormFillService.java) - AcroForm field mapping
- [PdfMergeConfig.java](src/main/java/com/example/service/PdfMergeConfig.java) - Configuration classes
- [base-payer.yml](../config-repo/templates/base-payer.yml) - Base configuration example
- [california-acroform.yml](../config-repo/templates/states/california-acroform.yml) - AcroForm example

---

**Summary:** Yes, you can mix FreeMarker, PDFBox, and AcroForm templates in a single multi-page PDF. Each section type has its own configuration syntax, and AcroForm field mappings use dot-notation paths to map PDF form fields to payload data.
