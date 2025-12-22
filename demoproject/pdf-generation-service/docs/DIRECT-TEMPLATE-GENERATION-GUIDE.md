# Direct Template Generation Guide
## For Benefit Summaries, Proposals, Quotes, and Non-Enrollment Documents

---

## Overview

For scenarios where you don't need enrollment-based configuration selection (products/market/state), use **direct template generation**:

- **Benefit Summaries** - Show plan benefits comparison
- **Proposal Documents** - Present plan options to prospects
- **Quote Letters** - Premium quotes for specific plans
- **Coverage Illustrations** - Detailed benefit examples
- **Marketing Materials** - Plan brochures, fact sheets
- **Reports** - Any custom multi-page PDF with mixed section types

### Key Difference from Enrollment Flow

| Enrollment Flow | Direct Template Flow |
|----------------|---------------------|
| Config selected based on products/market/state | You specify config name directly |
| `/api/enrollment/generate` | `/api/document/generate` |
| Requires enrollment parameters | Just config name + payload |
| Dynamic composition based on rules | Static template configuration |

---

## API Endpoint

### POST `/api/document/generate` - Direct Template Generation

**Purpose:** Generate PDF using a specific template configuration name

**URL:** `http://localhost:8080/api/document/generate`

**Method:** `POST`

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "configName": "benefit-summary.yml",
  "payload": {
    // Your data here
  },
  "outputFileName": "benefit-summary.pdf"  // Optional
}
```

**Response:**
- **Success (200):** PDF file (application/pdf)
- **Error (500):** Error message

---

## Configuration Structure

### Basic Template Configuration

**File:** `config-repo/benefit-summary.yml`

```yaml
pdfMerge:
  settings:
    pageNumbering: "bottom-center"
    addBookmarks: true
  
  header:
    content:
      left:
        text: "Benefit Summary | {companyName}"
        fontSize: 10
  
  footer:
    content:
      center:
        text: "Page {currentPage} of {totalPages} | © 2025 {companyName}"
        fontSize: 8
  
  sections:
    # Cover page using PDFBox generator
    - name: cover-page
      type: pdfbox
      template: benefit-summary-cover-generator
      enabled: true
    
    # Plan comparison using FreeMarker
    - name: plan-comparison
      type: freemarker
      template: templates/benefit-summary/plan-comparison.ftl
      enabled: true
    
    # State-specific disclosure using AcroForm
    - name: disclosure-form
      type: acroform
      template: benefit-summary-disclosure.pdf
      enabled: true
      fieldMapping:
        "ProspectName": "prospect.fullName"
        "ProspectEmail": "prospect.email"
        "PresentationDate": "today"
        "AgentName": "agent.name"
        "AgentLicense": "agent.licenseNumber"
    
    # Detailed benefits using FreeMarker
    - name: detailed-benefits
      type: freemarker
      template: templates/benefit-summary/detailed-benefits.ftl
      enabled: true
    
    # Contact information using PDFBox
    - name: contact-info
      type: pdfbox
      template: contact-info-generator
      enabled: true
  
  bookmarks:
    - section: cover-page
      title: "Cover"
      level: 1
    - section: plan-comparison
      title: "Plan Comparison"
      level: 1
    - section: disclosure-form
      title: "Disclosure"
      level: 1
    - section: detailed-benefits
      title: "Detailed Benefits"
      level: 1
    - section: contact-info
      title: "Contact Information"
      level: 1
```

---

## Complete Examples

### Example 1: Benefit Summary Document

#### Scenario
Generate a benefit summary comparing 3 medical plans for a prospect

#### Request

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "benefit-summary.yml",
    "payload": {
      "companyName": "HealthCare Plus",
      "prospect": {
        "fullName": "John Smith",
        "email": "john.smith@example.com",
        "phone": "(555) 123-4567",
        "address": {
          "street": "123 Main St",
          "city": "Los Angeles",
          "state": "CA",
          "zip": "90001"
        }
      },
      "agent": {
        "name": "Sarah Johnson",
        "licenseNumber": "CA-12345",
        "email": "sarah.j@healthcareplus.com",
        "phone": "(555) 987-6543"
      },
      "today": "2025-12-17",
      "plans": [
        {
          "planId": "GOLD_PPO",
          "planName": "Gold PPO",
          "type": "PPO",
          "monthlyPremium": 450.00,
          "deductible": 1000.00,
          "oopMax": 5000.00,
          "primaryCareVisit": 25.00,
          "specialistVisit": 50.00,
          "emergencyRoom": 250.00,
          "networkType": "Broad PPO Network"
        },
        {
          "planId": "SILVER_HMO",
          "planName": "Silver HMO",
          "type": "HMO",
          "monthlyPremium": 350.00,
          "deductible": 2000.00,
          "oopMax": 6000.00,
          "primaryCareVisit": 20.00,
          "specialistVisit": 40.00,
          "emergencyRoom": 200.00,
          "networkType": "HMO Network"
        },
        {
          "planId": "BRONZE_EPO",
          "planName": "Bronze EPO",
          "type": "EPO",
          "monthlyPremium": 280.00,
          "deductible": 3000.00,
          "oopMax": 7000.00,
          "primaryCareVisit": 35.00,
          "specialistVisit": 65.00,
          "emergencyRoom": 300.00,
          "networkType": "EPO Network"
        }
      ]
    },
    "outputFileName": "benefit-summary-john-smith.pdf"
  }' \
  -o benefit-summary-john-smith.pdf
```

#### Configuration File

**File:** `config-repo/benefit-summary.yml`

```yaml
pdfMerge:
  settings:
    pageNumbering: "bottom-center"
    addBookmarks: true
  
  header:
    content:
      left:
        text: "Benefit Summary | {companyName}"
        fontSize: 10
  
  footer:
    content:
      center:
        text: "© 2025 {companyName} | For {prospect.fullName}"
        fontSize: 8
  
  sections:
    - name: cover-page
      type: pdfbox
      template: benefit-summary-cover-generator
      enabled: true
    
    - name: plan-comparison
      type: freemarker
      template: templates/benefit-summary/plan-comparison.ftl
      enabled: true
    
    - name: detailed-benefits
      type: freemarker
      template: templates/benefit-summary/detailed-benefits.ftl
      enabled: true
  
  bookmarks:
    - section: cover-page
      title: "Cover"
      level: 1
    - section: plan-comparison
      title: "Plan Comparison"
      level: 1
    - section: detailed-benefits
      title: "Detailed Benefits"
      level: 1
```

#### FreeMarker Template

**File:** `config-repo/templates/benefit-summary/plan-comparison.ftl`

```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }
        h1 {
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th {
            background-color: #3498db;
            color: white;
            padding: 12px;
            text-align: left;
        }
        td {
            padding: 10px;
            border-bottom: 1px solid #ddd;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
        }
        .highlight {
            background-color: #ffffcc;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <h1>Medical Plan Comparison</h1>
    
    <p><strong>Prepared for:</strong> ${payload.prospect.fullName}</p>
    <p><strong>Date:</strong> ${payload.today}</p>
    
    <table>
        <thead>
            <tr>
                <th>Benefit</th>
                <#list payload.plans as plan>
                <th>${plan.planName}</th>
                </#list>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td><strong>Plan Type</strong></td>
                <#list payload.plans as plan>
                <td>${plan.type}</td>
                </#list>
            </tr>
            <tr class="highlight">
                <td><strong>Monthly Premium</strong></td>
                <#list payload.plans as plan>
                <td>$${plan.monthlyPremium?string["0.00"]}</td>
                </#list>
            </tr>
            <tr>
                <td><strong>Annual Deductible</strong></td>
                <#list payload.plans as plan>
                <td>$${plan.deductible?string["0.00"]}</td>
                </#list>
            </tr>
            <tr>
                <td><strong>Out-of-Pocket Maximum</strong></td>
                <#list payload.plans as plan>
                <td>$${plan.oopMax?string["0.00"]}</td>
                </#list>
            </tr>
            <tr>
                <td><strong>Primary Care Visit</strong></td>
                <#list payload.plans as plan>
                <td>$${plan.primaryCareVisit?string["0.00"]}</td>
                </#list>
            </tr>
            <tr>
                <td><strong>Specialist Visit</strong></td>
                <#list payload.plans as plan>
                <td>$${plan.specialistVisit?string["0.00"]}</td>
                </#list>
            </tr>
            <tr>
                <td><strong>Emergency Room</strong></td>
                <#list payload.plans as plan>
                <td>$${plan.emergencyRoom?string["0.00"]}</td>
                </#list>
            </tr>
            <tr>
                <td><strong>Network Type</strong></td>
                <#list payload.plans as plan>
                <td>${plan.networkType}</td>
                </#list>
            </tr>
        </tbody>
    </table>
    
    <p style="margin-top: 30px; font-size: 12px; color: #666;">
        <strong>Note:</strong> This is a summary comparison. Please refer to the 
        detailed benefits section for complete plan information.
    </p>
</body>
</html>
```

---

### Example 2: Premium Quote Letter

#### Scenario
Generate a quote letter with premium details and AcroForm signature page

#### Request

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "premium-quote-letter.yml",
    "payload": {
      "companyName": "HealthCare Plus",
      "quoteNumber": "Q-2025-12345",
      "quoteDate": "2025-12-17",
      "expirationDate": "2026-01-17",
      "prospect": {
        "fullName": "Jane Doe",
        "email": "jane.doe@example.com",
        "phone": "(555) 222-3333"
      },
      "agent": {
        "name": "Michael Brown",
        "title": "Licensed Insurance Agent",
        "licenseNumber": "CA-98765",
        "email": "michael.b@healthcareplus.com",
        "phone": "(555) 444-5555"
      },
      "quotedPlan": {
        "planName": "Platinum PPO",
        "planType": "PPO",
        "monthlyPremium": 650.00,
        "enrollmentFee": 0.00,
        "annualPremium": 7800.00
      },
      "acknowledgments": {
        "understandPremium": false,
        "agreeToTerms": false
      }
    },
    "outputFileName": "premium-quote-Q-2025-12345.pdf"
  }' \
  -o premium-quote-Q-2025-12345.pdf
```

#### Configuration File

**File:** `config-repo/premium-quote-letter.yml`

```yaml
pdfMerge:
  sections:
    # Quote letter content (FreeMarker)
    - name: quote-letter
      type: freemarker
      template: templates/quotes/premium-quote-letter.ftl
      enabled: true
    
    # Signature and acknowledgment form (AcroForm)
    - name: signature-acknowledgment
      type: acroform
      template: premium-quote-acknowledgment.pdf
      enabled: true
      fieldMapping:
        # Quote Information
        "QuoteNumber": "quoteNumber"
        "QuoteDate": "quoteDate"
        "ExpirationDate": "expirationDate"
        
        # Prospect Information
        "ProspectName": "prospect.fullName"
        "ProspectEmail": "prospect.email"
        "ProspectPhone": "prospect.phone"
        
        # Plan Information
        "PlanName": "quotedPlan.planName"
        "MonthlyPremium": "quotedPlan.monthlyPremium"
        "AnnualPremium": "quotedPlan.annualPremium"
        
        # Acknowledgment Checkboxes
        "UnderstandPremium": "acknowledgments.understandPremium"
        "AgreeToTerms": "acknowledgments.agreeToTerms"
        
        # Signature Fields (leave blank for signing)
        "ProspectSignature": ""
        "SignatureDate": ""
        
        # Agent Information
        "AgentName": "agent.name"
        "AgentLicense": "agent.licenseNumber"
    
    # Terms and conditions (FreeMarker)
    - name: terms-conditions
      type: freemarker
      template: templates/quotes/terms-and-conditions.ftl
      enabled: true
```

---

### Example 3: Multi-Product Proposal

#### Scenario
Proposal document with Medical, Dental, and Vision plans, including AcroForm enrollment form

#### Request

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "multi-product-proposal.yml",
    "payload": {
      "companyName": "HealthCare Plus",
      "proposalNumber": "PROP-2025-001",
      "proposalDate": "2025-12-17",
      "prospect": {
        "fullName": "Robert Johnson",
        "businessName": "Johnson Consulting LLC",
        "numberOfEmployees": 25
      },
      "medicalPlans": [
        {"planName": "Gold PPO", "premium": 450.00},
        {"planName": "Silver HMO", "premium": 350.00}
      ],
      "dentalPlans": [
        {"planName": "Premium Dental", "premium": 45.00}
      ],
      "visionPlans": [
        {"planName": "Vision Care Plus", "premium": 15.00}
      ]
    },
    "outputFileName": "proposal-PROP-2025-001.pdf"
  }' \
  -o proposal-PROP-2025-001.pdf
```

#### Configuration File

**File:** `config-repo/multi-product-proposal.yml`

```yaml
pdfMerge:
  settings:
    pageNumbering: "bottom-right"
    addBookmarks: true
  
  sections:
    # Executive summary (PDFBox)
    - name: executive-summary
      type: pdfbox
      template: proposal-executive-summary-generator
      enabled: true
    
    # Medical plans section (FreeMarker)
    - name: medical-plans
      type: freemarker
      template: templates/proposals/medical-plans-section.ftl
      enabled: true
    
    # Dental plans section (FreeMarker)
    - name: dental-plans
      type: freemarker
      template: templates/proposals/dental-plans-section.ftl
      enabled: true
    
    # Vision plans section (FreeMarker)
    - name: vision-plans
      type: freemarker
      template: templates/proposals/vision-plans-section.ftl
      enabled: true
    
    # Enrollment form (AcroForm)
    - name: enrollment-form
      type: acroform
      template: group-enrollment-form.pdf
      enabled: true
      fieldMapping:
        "ProposalNumber": "proposalNumber"
        "BusinessName": "prospect.businessName"
        "ContactName": "prospect.fullName"
        "NumberOfEmployees": "prospect.numberOfEmployees"
        "MedicalPlan": "medicalPlans[0].planName"
        "DentalPlan": "dentalPlans[0].planName"
        "VisionPlan": "visionPlans[0].planName"
  
  bookmarks:
    - section: executive-summary
      title: "Executive Summary"
      level: 1
    - section: medical-plans
      title: "Medical Plans"
      level: 1
    - section: dental-plans
      title: "Dental Plans"
      level: 1
    - section: vision-plans
      title: "Vision Plans"
      level: 1
    - section: enrollment-form
      title: "Enrollment Form"
      level: 1
```

---

### Example 4: Coverage Illustration with Conditional Sections

#### Scenario
Detailed coverage illustration with conditional AcroForm sections based on age

#### Configuration File

**File:** `config-repo/coverage-illustration.yml`

```yaml
pdfMerge:
  sections:
    # Main illustration (FreeMarker)
    - name: coverage-overview
      type: freemarker
      template: templates/illustrations/coverage-overview.ftl
      enabled: true
    
    # Benefit examples (FreeMarker)
    - name: benefit-examples
      type: freemarker
      template: templates/illustrations/benefit-examples.ftl
      enabled: true
  
  # Conditional sections based on payload data
  conditionalSections:
    # Medicare supplement disclosure (only if age >= 65)
    - condition: "payload.prospect.age >= 65"
      sections:
        - name: medicare-supplement-disclosure
          type: acroform
          template: medicare-supplement-disclosure.pdf
          insertAfter: benefit-examples
          fieldMapping:
            "BeneficiaryName": "prospect.fullName"
            "DateOfBirth": "prospect.dateOfBirth"
            "MedicareNumber": "prospect.medicareNumber"
    
    # Pediatric dental notice (only if has children)
    - condition: "payload.hasChildren"
      sections:
        - name: pediatric-dental-notice
          type: freemarker
          template: templates/illustrations/pediatric-dental-notice.ftl
          insertAfter: benefit-examples
```

#### Request

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "coverage-illustration.yml",
    "payload": {
      "prospect": {
        "fullName": "Mary Williams",
        "age": 67,
        "dateOfBirth": "1958-03-15",
        "medicareNumber": "1AB2-CD3-EF45"
      },
      "hasChildren": false
    }
  }' \
  -o coverage-illustration-mary-williams.pdf
```

**Result:** PDF includes Medicare supplement AcroForm (age >= 65), but NOT pediatric notice (hasChildren = false)

---

## Advanced Features

### 1. Dynamic Section Ordering

```yaml
pdfMerge:
  sections:
    - name: intro
      type: freemarker
      template: intro.ftl
    
    - name: section-b
      type: freemarker
      template: section-b.ftl
      insertAfter: section-a  # Will be inserted after section-a
    
    - name: section-a
      type: freemarker
      template: section-a.ftl
```

**Result:** Order will be: intro → section-a → section-b

### 2. Payload Enrichers for AcroForm Fields

```yaml
pdfMerge:
  sections:
    - name: quote-form
      type: acroform
      template: quote-form.pdf
      payloadEnrichers:
        - "TotalPremiumCalculator"
        - "DateFormatter"
      fieldMapping:
        "TotalMonthlyPremium": "enriched.totalPremium"
        "FormattedDate": "enriched.formattedDate"
```

**Enricher Implementation:**

```java
@Component("TotalPremiumCalculator")
public class TotalPremiumCalculator implements PayloadEnricher {
    @Override
    public void enrich(Map<String, Object> payload) {
        double medical = (double) payload.getOrDefault("medicalPremium", 0.0);
        double dental = (double) payload.getOrDefault("dentalPremium", 0.0);
        double vision = (double) payload.getOrDefault("visionPremium", 0.0);
        double total = medical + dental + vision;
        
        Map<String, Object> enriched = (Map) payload.computeIfAbsent("enriched", k -> new HashMap<>());
        enriched.put("totalPremium", String.format("%.2f", total));
    }
}
```

### 3. Disable Sections Dynamically

```yaml
pdfMerge:
  sections:
    - name: optional-section
      type: freemarker
      template: optional.ftl
      enabled: true  # Can be overridden by payload
```

**Request with Section Disabled:**

```json
{
  "configName": "my-template.yml",
  "payload": {
    "sectionOverrides": {
      "optional-section": {
        "enabled": false
      }
    }
  }
}
```

---

## Directory Organization

### Recommended Structure

```
config-repo/
│
├── benefit-summary.yml                 # Benefit summary config
├── premium-quote-letter.yml            # Quote letter config
├── multi-product-proposal.yml          # Proposal config
├── coverage-illustration.yml           # Illustration config
│
├── templates/
│   ├── benefit-summary/
│   │   ├── plan-comparison.ftl
│   │   └── detailed-benefits.ftl
│   │
│   ├── quotes/
│   │   ├── premium-quote-letter.ftl
│   │   └── terms-and-conditions.ftl
│   │
│   ├── proposals/
│   │   ├── medical-plans-section.ftl
│   │   ├── dental-plans-section.ftl
│   │   └── vision-plans-section.ftl
│   │
│   └── illustrations/
│       ├── coverage-overview.ftl
│       └── benefit-examples.ftl
│
└── acroforms/
    ├── benefit-summary-disclosure.pdf
    ├── premium-quote-acknowledgment.pdf
    ├── group-enrollment-form.pdf
    └── medicare-supplement-disclosure.pdf
```

---

## Comparison: Enrollment Flow vs Direct Template Flow

### Enrollment Flow (Dynamic Selection)

**Use Case:** Enrollment applications, formal submissions

**Endpoint:** `/api/enrollment/generate`

**Request:**
```json
{
  "enrollment": {
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": { /* data */ }
}
```

**Config Selection:** Automatic based on products/market/state
- Example: `dental-medical-individual-ca.yml`
- Composition: `base.yml + dental.yml + medical.yml + individual.yml + california.yml`

**Pros:**
- Automatic config selection
- Consistent for same parameters
- Handles state-specific requirements automatically

**Cons:**
- Requires enrollment parameters
- More complex for simple documents

---

### Direct Template Flow (Explicit Config)

**Use Case:** Benefit summaries, proposals, quotes, illustrations, reports

**Endpoint:** `/api/document/generate`

**Request:**
```json
{
  "configName": "benefit-summary.yml",
  "payload": { /* data */ }
}
```

**Config Selection:** Explicit - you specify exactly which config

**Pros:**
- Simple and straightforward
- Direct control over template
- No enrollment parameters needed
- Perfect for non-enrollment documents

**Cons:**
- Must specify config name
- No automatic composition

---

## When to Use Which Approach

### Use Enrollment Flow (`/api/enrollment/generate`) When:
✅ Generating enrollment applications
✅ Need automatic state-specific forms
✅ Config depends on products/market/state
✅ Want automatic composition of components

### Use Direct Template Flow (`/api/document/generate`) When:
✅ Generating benefit summaries
✅ Creating proposals or quotes
✅ Producing marketing materials
✅ Building custom reports
✅ You know exactly which template to use
✅ No need for dynamic config selection

---

## Testing

### Test Benefit Summary

```bash
# Create test payload
cat > /tmp/benefit-summary-payload.json <<'EOF'
{
  "configName": "benefit-summary.yml",
  "payload": {
    "companyName": "HealthCare Plus",
    "prospect": {
      "fullName": "Test User",
      "email": "test@example.com"
    },
    "plans": [
      {"planName": "Gold PPO", "monthlyPremium": 450.00},
      {"planName": "Silver HMO", "monthlyPremium": 350.00}
    ]
  },
  "outputFileName": "test-benefit-summary.pdf"
}
EOF

# Generate PDF
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @/tmp/benefit-summary-payload.json \
  -o test-benefit-summary.pdf

# Verify PDF
pdfinfo test-benefit-summary.pdf
```

### Test with AcroForm Section

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "premium-quote-letter.yml",
    "payload": {
      "quoteNumber": "TEST-001",
      "prospect": {"fullName": "Test User"},
      "quotedPlan": {"planName": "Test Plan", "monthlyPremium": 100.00}
    }
  }' \
  -o test-quote.pdf

# Check if AcroForm page exists
pdftk test-quote.pdf dump_data_fields
```

---

## Summary

### Direct Template Generation Flow

```
Your Application
    ↓
POST /api/document/generate
    {
      "configName": "benefit-summary.yml",
      "payload": { ... }
    }
    ↓
FlexiblePdfMergeService
    ↓
Load: benefit-summary.yml
    ↓
For each section:
  - FreeMarker → FreemarkerService
  - PDFBox → PdfBoxGenerator
  - AcroForm → AcroFormFillService
    ↓
Merge all section PDFs
    ↓
Return final PDF
```

### Key Advantages

1. **Simplicity** - Just specify config name + payload
2. **Flexibility** - Any document type, not just enrollments
3. **Mixed Sections** - FreeMarker, PDFBox, and AcroForm in same document
4. **Direct Control** - You choose the exact template to use
5. **No Dependencies** - Don't need enrollment parameters

### Perfect For

- ✅ Benefit summaries comparing plans
- ✅ Premium quotes for prospects
- ✅ Proposal documents for sales
- ✅ Coverage illustrations
- ✅ Marketing materials
- ✅ Custom reports
- ✅ Any multi-page PDF with mixed content types

**You have complete control over the template while still leveraging the powerful multi-section composition with AcroForms!**
