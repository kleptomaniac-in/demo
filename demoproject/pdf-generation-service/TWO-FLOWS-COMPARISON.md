# Two PDF Generation Flows - Quick Reference

## Flow Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│                    ENROLLMENT FLOW                               │
│            (Dynamic Config Selection)                            │
└─────────────────────────────────────────────────────────────────┘

Request:
{
  "enrollment": {
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": { /* application data */ }
}
         │
         ▼
POST /api/enrollment/generate
         │
         ▼
ConfigSelectionService.selectConfigByConvention()
         │
         ▼
Build config name: "dental-medical-individual-ca.yml"
         │
         ▼
Load composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california.yml      ← May include AcroForms
         │
         ▼
Merge all component sections
         │
         ▼
Generate PDF with state-mandated forms


════════════════════════════════════════════════════════════════════


┌─────────────────────────────────────────────────────────────────┐
│                   DIRECT TEMPLATE FLOW                           │
│              (Explicit Config Name)                              │
└─────────────────────────────────────────────────────────────────┘

Request:
{
  "configName": "benefit-summary.yml",
  "payload": { /* benefit data */ }
}
         │
         ▼
POST /api/document/generate
         │
         ▼
Load config directly: "benefit-summary.yml"
         │
         ▼
Read sections from config:
  sections:
    - name: cover
      type: pdfbox
    - name: plan-comparison
      type: freemarker
    - name: disclosure-form
      type: acroform                        ← AcroForm if needed
    - name: detailed-benefits
      type: freemarker
         │
         ▼
Generate each section
         │
         ▼
Merge sections into final PDF


```

---

## When to Use Each

### Enrollment Flow (`/api/enrollment/generate`)

**✅ Use When:**
- Generating enrollment/application PDFs
- Need automatic state-specific forms
- Config depends on products, market, state
- Want dynamic component composition
- Require consistent state compliance

**📋 Example Documents:**
- Member enrollment applications
- Group enrollment submissions
- Medicare enrollment forms
- State-mandated disclosure packages
- Compliance-heavy documents

**🔧 Key Feature:** Automatic selection + composition based on business parameters

---

### Direct Template Flow (`/api/document/generate`)

**✅ Use When:**
- Generating benefit summaries
- Creating proposals or quotes
- Producing marketing materials
- Building custom reports
- You know the exact template to use
- No enrollment parameters needed

**📋 Example Documents:**
- Benefit summaries
- Premium quotes
- Proposal documents
- Coverage illustrations
- Plan comparison sheets
- Marketing brochures
- Custom reports

**🔧 Key Feature:** Direct control with simple template + data approach

---

## Side-by-Side Comparison

| Aspect | Enrollment Flow | Direct Template Flow |
|--------|----------------|---------------------|
| **Endpoint** | `/api/enrollment/generate` | `/api/document/generate` |
| **Config Selection** | Automatic (products/market/state) | Manual (specify name) |
| **Use Case** | Enrollments, applications | Summaries, quotes, proposals |
| **Complexity** | Higher (dynamic composition) | Lower (direct config) |
| **Required Input** | Enrollment parameters | Config name only |
| **Composition** | Multi-component (base + products + market + state) | Single config file |
| **State Forms** | Automatic inclusion | Include if needed |
| **AcroForm Support** | ✅ Yes (via components) | ✅ Yes (in config) |
| **FreeMarker Support** | ✅ Yes | ✅ Yes |
| **PDFBox Support** | ✅ Yes | ✅ Yes |

---

## Example Requests

### Enrollment Flow

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
      "applicantName": "John Doe",
      "selectedPlans": { /* ... */ }
    }
  }' \
  -o enrollment-application.pdf
```

**Result:** PDF with sections from dental.yml + medical.yml + individual.yml + california.yml

---

### Direct Template Flow

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "benefit-summary.yml",
    "payload": {
      "prospectName": "John Doe",
      "plans": [ /* ... */ ]
    }
  }' \
  -o benefit-summary.pdf
```

**Result:** PDF with sections from benefit-summary.yml only

---

## AcroForm Usage in Both Flows

### Enrollment Flow - AcroForms in Components

**File:** `templates/states/california.yml`

```yaml
pdfMerge:
  sections:
    - name: ca-dmhc-disclosure
      type: acroform
      template: ca-dmhc-form.pdf
      fieldMapping:
        "MemberName": "primaryApplicant.fullName"
        "PlanName": "selectedPlans.medical.planName"
```

**Automatically included** when `state: "CA"` in enrollment request

---

### Direct Flow - AcroForms in Config

**File:** `benefit-summary.yml`

```yaml
pdfMerge:
  sections:
    - name: disclosure-form
      type: acroform
      template: benefit-summary-disclosure.pdf
      fieldMapping:
        "ProspectName": "prospect.fullName"
        "AgentName": "agent.name"
```

**Explicitly defined** in the template configuration

---

## Decision Tree

```
Do you have enrollment parameters
(products, market, state)?
    │
    ├─ YES ──→ Do you need automatic
    │          state-specific forms?
    │              │
    │              ├─ YES ──→ Use ENROLLMENT FLOW
    │              │          /api/enrollment/generate
    │              │
    │              └─ NO ───→ Either flow works
    │                         (enrollment flow preferred
    │                          for consistency)
    │
    └─ NO ───→ Use DIRECT TEMPLATE FLOW
               /api/document/generate
```

---

## Configuration Flexibility

### Both Flows Support:

✅ **Mixed Section Types**
- FreeMarker (HTML templates)
- PDFBox (programmatic generation)
- AcroForm (fillable PDFs)

✅ **Multiple Sections**
- Unlimited sections per document
- Any combination of types

✅ **Conditional Sections**
- Based on payload data
- Dynamic inclusion/exclusion

✅ **Headers & Footers**
- Custom headers
- Page numbering
- Dynamic content

✅ **Bookmarks**
- Navigation within PDF
- Hierarchical structure

✅ **Payload Enrichers**
- Data preprocessing
- Calculated fields
- Data transformation

---

## Common Patterns

### Pattern 1: Enrollment with State Forms (Enrollment Flow)

```
Products: Medical + Dental
Market: Individual
State: California

→ Auto-selected config: dental-medical-individual-ca.yml
→ Components: base + dental + medical + individual + california
→ California component includes CA-mandated AcroForm
→ Result: Enrollment PDF with all required state forms
```

---

### Pattern 2: Benefit Summary with Signature (Direct Flow)

```
Template: benefit-summary.yml

Sections:
  1. Cover page (PDFBox)
  2. Plan comparison table (FreeMarker)
  3. Detailed benefits (FreeMarker)
  4. Signature form (AcroForm)

→ Direct config load
→ Result: Professional benefit summary with e-signature form
```

---

### Pattern 3: Quote Letter (Direct Flow)

```
Template: premium-quote-letter.yml

Sections:
  1. Quote letter (FreeMarker)
  2. Premium breakdown (FreeMarker)
  3. Acknowledgment form (AcroForm)
  4. Terms & conditions (FreeMarker)

→ Simple config + payload
→ Result: Quote letter ready for client signature
```

---

## Real-World Scenarios

### Scenario 1: Medicare Enrollment

**Flow:** Enrollment Flow

**Why:** 
- Requires CMS-mandated forms
- State-specific disclosures
- Automatic form inclusion based on state

**Request:**
```json
{
  "enrollment": {
    "products": ["medical"],
    "marketCategory": "medicare",
    "state": "CA"
  }
}
```

**Auto-included:**
- CMS enrollment form (from medicare.yml)
- CA state disclosure (from california.yml)
- Medicare-specific sections

---

### Scenario 2: Sales Proposal

**Flow:** Direct Template Flow

**Why:**
- Custom template for proposals
- No enrollment parameters
- Specific sections for sales process

**Request:**
```json
{
  "configName": "sales-proposal.yml",
  "payload": {
    "prospect": { /* ... */ },
    "quotedPlans": [ /* ... */ ]
  }
}
```

**Sections:**
- Executive summary
- Plan options
- Pricing tables
- Proposal acceptance form (AcroForm)

---

### Scenario 3: Benefit Comparison

**Flow:** Direct Template Flow

**Why:**
- Marketing/education document
- No enrollment data
- Generic comparison template

**Request:**
```json
{
  "configName": "benefit-comparison.yml",
  "payload": {
    "plans": [ /* ... */ ]
  }
}
```

**Sections:**
- Plan comparison table
- Benefit highlights
- Contact information

---

## Quick Start Examples

### Generate Enrollment PDF

```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {"products": ["medical"], "marketCategory": "individual", "state": "CA"},
    "payload": {"applicantName": "Test User"}
  }' \
  -o enrollment.pdf
```

---

### Generate Benefit Summary

```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "benefit-summary.yml",
    "payload": {"prospectName": "Test User"}
  }' \
  -o benefit-summary.pdf
```

---

## Summary Table

| Document Type | Recommended Flow | Endpoint | Key Benefit |
|--------------|------------------|----------|-------------|
| Enrollment Application | Enrollment | `/api/enrollment/generate` | Automatic state forms |
| Medicare Enrollment | Enrollment | `/api/enrollment/generate` | CMS compliance |
| Group Application | Enrollment | `/api/enrollment/generate` | Market-specific rules |
| **Benefit Summary** | **Direct** | `/api/document/generate` | Simple & flexible |
| **Premium Quote** | **Direct** | `/api/document/generate` | Direct control |
| **Proposal Document** | **Direct** | `/api/document/generate` | Custom template |
| **Coverage Illustration** | **Direct** | `/api/document/generate` | Marketing focused |
| **Marketing Brochure** | **Direct** | `/api/document/generate` | No enrollment data |

---

## Key Takeaway

**Both flows support AcroForms, FreeMarker, and PDFBox sections!**

- **Enrollment Flow** = Automatic config selection based on business parameters
- **Direct Template Flow** = Simple template + data approach

**Choose based on whether you need automatic config selection or direct template control.**
