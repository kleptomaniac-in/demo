# Dynamic Configuration Selection with AcroForms - Quick Reference

## Configuration Selection Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Enrollment Request                          │
│  {                                                               │
│    "products": ["medical", "dental"],                           │
│    "marketCategory": "individual",                              │
│    "state": "CA"                                                │
│  }                                                               │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              ConfigSelectionService                              │
│                                                                  │
│  1. Sort products alphabetically: ["dental", "medical"]         │
│  2. Build config name:                                          │
│     "dental-medical-individual-ca.yml"                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│         Load Config: dental-medical-individual-ca.yml           │
│                                                                  │
│  base: templates/base-payer.yml                                 │
│  components:                                                     │
│    - templates/products/dental.yml                              │
│    - templates/products/medical.yml                             │
│    - templates/markets/individual.yml                           │
│    - templates/states/california.yml                            │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Component Composition                           │
│                                                                  │
│  Merge sections from all component files:                       │
│                                                                  │
│  FROM base-payer.yml:                                           │
│    ├─ cover-page (pdfbox)                                       │
│    └─ footer/header config                                      │
│                                                                  │
│  FROM dental.yml:                                               │
│    ├─ dental-coverage (freemarker)                              │
│    ├─ dental-network (freemarker)                               │
│    └─ dental-acknowledgment-form (ACROFORM) ⭐                  │
│                                                                  │
│  FROM medical.yml:                                              │
│    ├─ medical-coverage (freemarker)                             │
│    ├─ provider-network (freemarker)                             │
│    └─ prescription-coverage (freemarker)                        │
│                                                                  │
│  FROM individual.yml:                                           │
│    ├─ individual-mandate (freemarker)                           │
│    └─ cost-sharing (freemarker)                                 │
│                                                                  │
│  FROM california.yml:                                           │
│    ├─ ca-dmhc-disclosure (freemarker)                           │
│    ├─ ca-dmhc-acroform (ACROFORM) ⭐                            │
│    └─ ca-benefit-mandates (freemarker)                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│           FlexiblePdfMergeService.generateMergedPdf()           │
│                                                                  │
│  For each section:                                              │
│    ├─ If type == "freemarker" → FreemarkerService              │
│    ├─ If type == "pdfbox" → PdfBoxGenerator                    │
│    └─ If type == "acroform" → AcroFormFillService              │
│                                   ↓                              │
│                    ┌──────────────┴──────────────┐              │
│                    │  AcroFormFillService        │              │
│                    │  1. Load PDF template       │              │
│                    │  2. Get form fields         │              │
│                    │  3. Map payload → fields    │              │
│                    │  4. Fill form               │              │
│                    │  5. Flatten (optional)      │              │
│                    └─────────────────────────────┘              │
│                                                                  │
│  Merge all section PDFs into final document                     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Final PDF Output                            │
│                                                                  │
│  Page 1:  Cover Page                        [PDFBox]            │
│  Page 2:  Dental Coverage Details           [FreeMarker]        │
│  Page 3:  Dental Network Info               [FreeMarker]        │
│  Page 4:  Dental Acknowledgment Form        [AcroForm] ⭐       │
│  Page 5:  Medical Coverage Details          [FreeMarker]        │
│  Page 6:  Provider Network                  [FreeMarker]        │
│  Page 7:  Prescription Coverage             [FreeMarker]        │
│  Page 8:  Individual Mandate Notice         [FreeMarker]        │
│  Page 9:  Cost Sharing Details              [FreeMarker]        │
│  Page 10: CA DMHC Disclosure (HTML)         [FreeMarker]        │
│  Page 11: CA DMHC Disclosure Form           [AcroForm] ⭐       │
│  Page 12: CA Benefit Mandates               [FreeMarker]        │
│                                                                  │
│  📄 Final PDF has 12 pages with 2 AcroForm sections!            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Files Structure

```
config-repo/
│
├── dental-medical-individual-ca.yml    ← TOP-LEVEL CONFIG
│   (References: base + 4 components)
│
├── templates/
│   │
│   ├── base-payer.yml                  ← BASE
│   │   sections:
│   │     - cover-page (pdfbox)
│   │
│   ├── products/
│   │   ├── dental.yml                  ← PRODUCT COMPONENT
│   │   │   sections:
│   │   │     - dental-coverage (freemarker)
│   │   │     - dental-network (freemarker)
│   │   │     - dental-acknowledgment-form (acroform) ⭐
│   │   │
│   │   └── medical.yml                 ← PRODUCT COMPONENT
│   │       sections:
│   │         - medical-coverage (freemarker)
│   │         - provider-network (freemarker)
│   │         - prescription-coverage (freemarker)
│   │
│   ├── markets/
│   │   ├── individual.yml              ← MARKET COMPONENT
│   │   │   sections:
│   │   │     - individual-mandate (freemarker)
│   │   │     - cost-sharing (freemarker)
│   │   │
│   │   └── medicare.yml                ← MARKET COMPONENT
│   │       sections:
│   │         - medicare-part-c (freemarker)
│   │         - cms-enrollment-form (acroform) ⭐
│   │         - medicare-part-d (freemarker)
│   │
│   └── states/
│       ├── california.yml              ← STATE COMPONENT
│       │   sections:
│       │     - ca-dmhc-disclosure (freemarker)
│       │     - ca-dmhc-acroform (acroform) ⭐
│       │     - ca-benefit-mandates (freemarker)
│       │
│       └── texas.yml                   ← STATE COMPONENT
│           sections:
│             - tx-disclosure (freemarker)
│             - tx-state-form (acroform) ⭐
│
└── acroforms/                          ← ACROFORM PDF TEMPLATES
    ├── dental-acknowledgment-signature.pdf
    ├── ca-dmhc-form-2025.pdf
    ├── cms-10802-medicare-advantage-enrollment.pdf
    └── tx-disclosure-form.pdf
```

---

## AcroForm Field Mapping Resolution

```
Enrollment Payload:
{
  "primaryApplicant": {
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1980-05-15"
  },
  "selectedPlans": {
    "medical": {
      "planName": "Gold PPO",
      "premium": 450.00
    }
  },
  "acknowledgments": {
    "networkRestrictions": true
  }
}

         │
         │ Field Mapping Configuration:
         │ {
         │   "MemberFirstName": "primaryApplicant.firstName",
         │   "MemberLastName": "primaryApplicant.lastName",
         │   "PlanName": "selectedPlans.medical.planName",
         │   "NetworkAck": "acknowledgments.networkRestrictions"
         │ }
         ▼

AcroFormFillService Resolution:
┌─────────────────────────────────────────────────┐
│ PDF Field Name        → Resolved Value          │
├─────────────────────────────────────────────────┤
│ "MemberFirstName"     → "John"                  │
│ "MemberLastName"      → "Smith"                 │
│ "PlanName"            → "Gold PPO"              │
│ "NetworkAck"          → "Yes" (true→"Yes")      │
└─────────────────────────────────────────────────┘

         ▼

Filled PDF Form:
┌─────────────────────────────────────────────────┐
│  California DMHC Disclosure Form                │
│                                                  │
│  Member Name: John Smith                        │
│  Plan Name: Gold PPO                            │
│  ☑ I acknowledge network restrictions           │
└─────────────────────────────────────────────────┘
```

---

## Configuration Selection Examples

### Example 1: Medical Only, Individual, California
```
Input:  {products: ["medical"], market: "individual", state: "CA"}
Config: medical-individual-ca.yml
Components: base + medical + individual + california
AcroForms: ca-dmhc-acroform (from california.yml)
```

### Example 2: Medical + Dental, Individual, California
```
Input:  {products: ["medical", "dental"], market: "individual", state: "CA"}
Config: dental-medical-individual-ca.yml
Components: base + dental + medical + individual + california
AcroForms: dental-acknowledgment-form (from dental.yml)
           ca-dmhc-acroform (from california.yml)
```

### Example 3: Medical, Medicare, California
```
Input:  {products: ["medical"], market: "medicare", state: "CA"}
Config: medical-medicare-ca.yml
Components: base + medical + medicare + california
AcroForms: cms-enrollment-form (from medicare.yml)
           ca-dmhc-acroform (from california.yml)
```

### Example 4: Medical + Dental, Medicare, Texas
```
Input:  {products: ["medical", "dental"], market: "medicare", state: "TX"}
Config: dental-medical-medicare-tx.yml
Components: base + dental + medical + medicare + texas
AcroForms: dental-acknowledgment-form (from dental.yml)
           cms-enrollment-form (from medicare.yml)
           tx-state-form (from texas.yml)
```

---

## Key Advantages

### ✅ Separation of Concerns
- **Product-specific forms** (dental acknowledgments) → `products/`
- **Market-specific forms** (Medicare CMS forms) → `markets/`
- **State-specific forms** (CA DMHC forms) → `states/`

### ✅ Automatic Composition
- System automatically includes relevant AcroForms
- No manual selection needed
- Consistent across all enrollments

### ✅ DRY Principle
- Define each AcroForm once in appropriate component
- Reused across all applicable configurations
- Example: CA form defined once, used in:
  - medical-individual-ca.yml
  - dental-medical-individual-ca.yml
  - medical-medicare-ca.yml
  - dental-medical-vision-small-group-ca.yml
  - ...all CA configs!

### ✅ Maintainability
- Update AcroForm in one place (component file)
- Changes apply to all configurations using that component
- Easy to add new states, products, or markets

### ✅ Scalability
- 3 products × 4 markets × 50 states = 600 configs
- But only ~70 component files needed
- AcroForms automatically included where relevant

---

## Quick Commands

### Preview Configuration Selection
```bash
curl -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  }'
```

### Generate PDF with AcroForms
```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "primaryApplicant": {
        "firstName": "John",
        "lastName": "Smith",
        "dateOfBirth": "1980-05-15"
      },
      "selectedPlans": {
        "medical": {
          "planName": "Gold PPO",
          "premium": 450.00
        }
      },
      "acknowledgments": {
        "networkRestrictions": true
      }
    }
  }' \
  -o enrollment.pdf
```

### Check PDF Page Count
```bash
pdfinfo enrollment.pdf | grep Pages
```

### Extract Specific AcroForm Page for Verification
```bash
pdftk enrollment.pdf cat 11 output ca-form-page.pdf
```

---

## Summary

**Answer to Your Question:**

> "Based on products, market category, and state, how would this work with multiple sections where acroform can be one such type?"

**Answer:** 

1. **ConfigSelectionService** selects config based on products + market + state
2. **Composition System** loads base + product components + market component + state component
3. **Each Component** can define sections of any type (freemarker, pdfbox, acroform)
4. **AcroForm Sections** work exactly like other section types:
   - State-specific AcroForms → defined in `states/{state}.yml`
   - Market-specific AcroForms → defined in `markets/{market}.yml`
   - Product-specific AcroForms → defined in `products/{product}.yml`
5. **Result:** All relevant AcroForms automatically included based on enrollment parameters!

**Key Insight:** AcroForm sections are just another section type. The composition system handles them identically to FreeMarker and PDFBox sections.
