# AcroForm Sections with Dynamic Configuration Selection

## Overview

Your system **already supports** dynamic configuration selection based on:
- **Products** (medical, dental, vision)
- **Market Category** (individual, medicare, small-group, large-group)
- **State** (CA, TX, NY, etc.)

AcroForm sections work seamlessly with this system through **component composition**.

---

## How It Works

### 1. Configuration Selection Flow

```
Enrollment Request
    ↓
Extract: products, marketCategory, state
    ↓
ConfigSelectionService.selectConfigByConvention()
    ↓
Build config name: {products}-{market}-{state}.yml
    ↓
Load config with composition (base + components)
    ↓
Components can include AcroForm sections
    ↓
Generate PDF with all sections
```

### 2. Architecture

```yaml
# Top-level config: dental-medical-individual-ca.yml
base: templates/base-payer.yml
components:
  - templates/products/dental.yml       # Dental-specific sections
  - templates/products/medical.yml      # Medical-specific sections
  - templates/markets/individual.yml    # Individual market sections
  - templates/states/california.yml     # CA-specific sections (includes AcroForm!)
```

**Key Insight:** Each component file can define sections of **any type** (freemarker, pdfbox, or acroform). The composition system merges all sections from all components.

---

## Adding AcroForm Sections by State

### Scenario: California Requires AcroForm Disclosure

**File:** `config-repo/templates/states/california.yml`

```yaml
# California state-specific configurations and requirements

pdfMerge:
  sections:
    # FreeMarker sections (existing)
    - name: ca-dmhc-disclosure
      type: freemarker
      template: templates/states/california-dmhc-disclosure.ftl
      enabled: true
    
    # AcroForm section (NEW - state-mandated fillable form)
    - name: ca-dmhc-acroform
      type: acroform
      template: ca-dmhc-form-2025.pdf
      enabled: true
      fieldMapping:
        # Company information
        "PayerName": "companyName"
        "PayerLicenseNumber": "companyLicenseCA"
        "EffectiveDate": "effectiveDate"
        
        # Member information
        "MemberFirstName": "primaryApplicant.firstName"
        "MemberLastName": "primaryApplicant.lastName"
        "MemberDOB": "primaryApplicant.dateOfBirth"
        "MemberID": "memberId"
        
        # Plan information
        "PlanName": "selectedPlans.medical.planName"
        "PlanType": "selectedPlans.medical.type"
        "MonthlyPremium": "totalMonthlyPremium"
        
        # Acknowledgments (checkboxes)
        "NetworkRestrictionsAck": "acknowledgments.networkRestrictions"
        "AppealProcessAck": "acknowledgments.appealProcess"
        "GrievanceRightsAck": "acknowledgments.grievanceRights"
        "LanguageAssistanceAck": "acknowledgments.languageAssistance"
    
    # Another FreeMarker section
    - name: ca-benefit-mandates
      type: freemarker
      template: templates/states/california-benefit-mandates.ftl
      enabled: true
  
  bookmarks:
    - section: ca-dmhc-disclosure
      title: "California DMHC Disclosure"
      level: 1
    - section: ca-dmhc-acroform
      title: "California State Form"
      level: 1
    - section: ca-benefit-mandates
      title: "California Benefit Mandates"
      level: 1
```

**Result:** Any enrollment for California (any product, any market) will include this AcroForm section!

---

## Adding AcroForm Sections by Market Category

### Scenario: Medicare Requires CMS Forms

**File:** `config-repo/templates/markets/medicare.yml`

```yaml
# Medicare market-specific configurations

pdfMerge:
  sections:
    # Medicare-specific information
    - name: medicare-part-c-details
      type: freemarker
      template: templates/markets/medicare-part-c-details.ftl
      enabled: true
    
    # CMS-required AcroForm (enrollment form)
    - name: cms-enrollment-form
      type: acroform
      template: cms-10802-medicare-advantage-enrollment.pdf
      enabled: true
      fieldMapping:
        # Beneficiary Information (Section 1)
        "BeneficiaryLastName": "primaryApplicant.lastName"
        "BeneficiaryFirstName": "primaryApplicant.firstName"
        "BeneficiaryMI": "primaryApplicant.middleInitial"
        "BeneficiaryDOB": "primaryApplicant.dateOfBirth"
        "BeneficiaryGender": "primaryApplicant.gender"
        "PhoneNumber": "primaryApplicant.phone"
        
        # Medicare Information (Section 2)
        "MedicareNumber": "primaryApplicant.medicareNumber"
        "PartAEffectiveDate": "primaryApplicant.medicare.partAEffectiveDate"
        "PartBEffectiveDate": "primaryApplicant.medicare.partBEffectiveDate"
        
        # Residence Address (Section 3)
        "ResidenceStreet": "primaryApplicant.address.street"
        "ResidenceCity": "primaryApplicant.address.city"
        "ResidenceState": "primaryApplicant.address.state"
        "ResidenceZip": "primaryApplicant.address.zip"
        "ResidenceCounty": "primaryApplicant.address.county"
        
        # Plan Information (Section 4)
        "PlanName": "selectedPlans.medical.planName"
        "PlanContractNumber": "selectedPlans.medical.contractNumber"
        "PlanPBP": "selectedPlans.medical.pbp"
        "EffectiveDate": "effectiveDate"
        
        # Additional Coverage (Section 5)
        "HasMedicaid": "primaryApplicant.medicare.hasMedicaid"
        "HasEmployerCoverage": "primaryApplicant.hasEmployerCoverage"
        
        # Election Confirmation (Section 6)
        "ConfirmElection": "acknowledgments.confirmMedicareElection"
        
        # Signature (Section 7)
        "ApplicantSignature": "primaryApplicant.fullName"
        "SignatureDate": "today"
    
    # Medicare Part D prescription details
    - name: medicare-part-d-details
      type: freemarker
      template: templates/markets/medicare-part-d-details.ftl
      enabled: true
  
  bookmarks:
    - section: medicare-part-c-details
      title: "Medicare Advantage Details"
      level: 1
    - section: cms-enrollment-form
      title: "CMS Enrollment Form"
      level: 1
    - section: medicare-part-d-details
      title: "Prescription Drug Coverage (Part D)"
      level: 1
```

**Result:** Any Medicare enrollment (any state, any product) will include the CMS AcroForm!

---

## Adding AcroForm Sections by Product

### Scenario: Dental Plans Require Signature Form

**File:** `config-repo/templates/products/dental.yml`

```yaml
# Dental product-specific sections and configurations

pdfMerge:
  sections:
    # Dental coverage details
    - name: dental-coverage
      type: freemarker
      template: templates/products/dental-coverage-details.ftl
      enabled: true
    
    # Dental network information
    - name: dental-network
      type: freemarker
      template: templates/products/dental-network.ftl
      enabled: true
    
    # Dental-specific acknowledgment form (AcroForm)
    - name: dental-acknowledgment-form
      type: acroform
      template: dental-acknowledgment-signature.pdf
      enabled: true
      fieldMapping:
        "MemberName": "primaryApplicant.fullName"
        "MemberID": "memberId"
        "DentalPlanName": "selectedPlans.dental.planName"
        "AnnualMaximum": "selectedPlans.dental.annualMaximum"
        "NetworkType": "selectedPlans.dental.networkType"
        
        # Acknowledgments
        "UnderstandPreventiveBenefits": "acknowledgments.dental.preventive"
        "UnderstandOrthodonticLimits": "acknowledgments.dental.orthodontic"
        "UnderstandWaitingPeriods": "acknowledgments.dental.waitingPeriods"
        "UnderstandMissingToothClause": "acknowledgments.dental.missingTooth"
        
        # Signature
        "ApplicantSignature": "primaryApplicant.fullName"
        "SignatureDate": "today"
  
  bookmarks:
    - section: dental-coverage
      title: "Dental Coverage Details"
      level: 1
    - section: dental-network
      title: "Dental Provider Network"
      level: 1
    - section: dental-acknowledgment-form
      title: "Dental Plan Acknowledgment"
      level: 1
```

**Result:** Any enrollment including dental products will include this AcroForm!

---

## Real-World Examples

### Example 1: Medical Individual California

**Request:**
```json
{
  "enrollment": {
    "products": ["medical"],
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "companyName": "HealthCare Plus",
    "companyLicenseCA": "CA-12345",
    "primaryApplicant": {
      "firstName": "John",
      "lastName": "Smith",
      "dateOfBirth": "1980-05-15",
      "fullName": "John Smith"
    },
    "selectedPlans": {
      "medical": {
        "planName": "Gold PPO",
        "type": "PPO",
        "premium": 450.00
      }
    },
    "acknowledgments": {
      "networkRestrictions": true,
      "appealProcess": true,
      "grievanceRights": true
    }
  }
}
```

**Config Selected:** `medical-individual-ca.yml`

**Composition:**
```yaml
base: templates/base-payer.yml
components:
  - templates/products/medical.yml      # Medical FreeMarker sections
  - templates/markets/individual.yml    # Individual market sections
  - templates/states/california.yml     # CA sections + CA AcroForm!
```

**Resulting PDF Sections:**
1. Cover page (PDFBox - from base)
2. Medical coverage details (FreeMarker - from medical.yml)
3. Provider network (FreeMarker - from medical.yml)
4. Prescription coverage (FreeMarker - from medical.yml)
5. Individual mandate notice (FreeMarker - from individual.yml)
6. Cost sharing details (FreeMarker - from individual.yml)
7. **CA DMHC disclosure form (AcroForm - from california.yml)** ⭐
8. CA benefit mandates (FreeMarker - from california.yml)

---

### Example 2: Medical + Dental Medicare California

**Request:**
```json
{
  "enrollment": {
    "products": ["medical", "dental"],
    "marketCategory": "medicare",
    "state": "CA"
  },
  "payload": {
    "primaryApplicant": {
      "firstName": "Mary",
      "lastName": "Johnson",
      "dateOfBirth": "1950-03-10",
      "medicareNumber": "1AB2-CD3-EF45",
      "medicare": {
        "partAEffectiveDate": "2015-04-01",
        "partBEffectiveDate": "2015-04-01",
        "hasMedicaid": false
      }
    },
    "selectedPlans": {
      "medical": {
        "planName": "Medicare Advantage PPO",
        "contractNumber": "H1234",
        "pbp": "001",
        "premium": 0.00
      },
      "dental": {
        "planName": "Senior Dental Plan",
        "annualMaximum": 2000.00
      }
    }
  }
}
```

**Config Selected:** `dental-medical-medicare-ca.yml`

**Composition:**
```yaml
base: templates/base-payer.yml
components:
  - templates/products/dental.yml       # Includes dental AcroForm
  - templates/products/medical.yml      
  - templates/markets/medicare.yml      # Includes CMS AcroForm!
  - templates/states/california.yml     # Includes CA AcroForm!
```

**Resulting PDF Sections:**
1. Cover page (PDFBox)
2. Medical coverage (FreeMarker)
3. Dental coverage (FreeMarker)
4. **Dental acknowledgment form (AcroForm - from dental.yml)** ⭐
5. Medicare Part C details (FreeMarker)
6. **CMS Medicare enrollment form (AcroForm - from medicare.yml)** ⭐
7. Medicare Part D details (FreeMarker)
8. **CA DMHC disclosure form (AcroForm - from california.yml)** ⭐
9. CA benefit mandates (FreeMarker)

**Result:** PDF with **3 AcroForm sections** automatically included based on products/market/state!

---

### Example 3: Dental Small-Group Texas

**Request:**
```json
{
  "enrollment": {
    "products": ["dental"],
    "marketCategory": "small-group",
    "state": "TX"
  }
}
```

**Config Selected:** `dental-small-group-tx.yml`

**Composition:**
```yaml
base: templates/base-payer.yml
components:
  - templates/products/dental.yml       # Includes dental AcroForm
  - templates/markets/small-group.yml   
  - templates/states/texas.yml          # May include TX-specific AcroForm
```

**Resulting PDF Sections:**
1. Cover page (PDFBox)
2. Dental coverage (FreeMarker)
3. **Dental acknowledgment form (AcroForm - from dental.yml)** ⭐
4. Small group requirements (FreeMarker)
5. Texas-specific sections (FreeMarker or AcroForm if defined)

---

## Component Organization Strategy

### Directory Structure

```
config-repo/
├── templates/
│   ├── base-payer.yml           # Base template (common sections)
│   │
│   ├── products/
│   │   ├── medical.yml          # Medical-specific sections
│   │   ├── dental.yml           # Dental + dental AcroForm
│   │   ├── vision.yml           # Vision sections
│   │   └── prescription.yml     # Prescription coverage
│   │
│   ├── markets/
│   │   ├── individual.yml       # Individual market sections
│   │   ├── medicare.yml         # Medicare + CMS AcroForms!
│   │   ├── small-group.yml      # Small group sections
│   │   └── large-group.yml      # Large group sections
│   │
│   └── states/
│       ├── california.yml       # CA sections + CA AcroForms!
│       ├── texas.yml            # TX sections + TX AcroForms
│       ├── newyork.yml          # NY sections + NY AcroForms
│       └── florida.yml          # FL sections + FL AcroForms
│
├── acroforms/                   # AcroForm PDF templates
│   ├── ca-dmhc-form-2025.pdf
│   ├── cms-10802-medicare-advantage-enrollment.pdf
│   ├── dental-acknowledgment-signature.pdf
│   ├── tx-disclosure-form.pdf
│   └── states/
│       ├── california/
│       │   └── ca-dmhc-form-2025.pdf
│       ├── texas/
│       └── newyork/
│
└── dental-medical-individual-ca.yml  # Top-level composed config
```

---

## Conditional AcroForm Sections

### Scenario: AcroForm Only for Certain Conditions

**File:** `config-repo/templates/states/california.yml`

```yaml
pdfMerge:
  sections:
    # Always-included FreeMarker sections
    - name: ca-dmhc-disclosure
      type: freemarker
      template: templates/states/california-dmhc-disclosure.ftl
      enabled: true
  
  # Conditional AcroForm sections
  conditionalSections:
    # AcroForm only for individual market with medical coverage
    - condition: "payload.marketCategory == 'individual' && payload.products.contains('medical')"
      sections:
        - name: ca-individual-medical-form
          type: acroform
          template: ca-individual-medical-disclosure.pdf
          insertAfter: ca-dmhc-disclosure
          fieldMapping:
            "MemberName": "primaryApplicant.fullName"
            "PlanName": "selectedPlans.medical.planName"
            "AcknowledgeACA": "acknowledgments.acaCompliance"
    
    # AcroForm only for small-group with 50+ employees
    - condition: "payload.marketCategory == 'small-group' && payload.groupSize >= 50"
      sections:
        - name: ca-shop-exchange-form
          type: acroform
          template: ca-shop-exchange-eligibility.pdf
          insertAfter: ca-dmhc-disclosure
          fieldMapping:
            "EmployerName": "employer.name"
            "EmployerEIN": "employer.ein"
            "NumberOfEmployees": "employer.totalEmployees"
            "NumberEnrolling": "enrollment.enrollingEmployees"
    
    # AcroForm only for Medicare Advantage
    - condition: "payload.marketCategory == 'medicare' && payload.products.contains('medical')"
      sections:
        - name: ca-medicare-supplement-form
          type: acroform
          template: ca-medicare-supplement-notice.pdf
          insertAfter: medicare-part-c-details
          fieldMapping:
            "BeneficiaryName": "primaryApplicant.fullName"
            "MedicareNumber": "primaryApplicant.medicareNumber"
            "PlanType": "selectedPlans.medical.type"
```

---

## Field Mapping with Dynamic Values

### Using Payload Enrichers for AcroForm Fields

**Problem:** AcroForm needs data not directly in payload structure

**Solution:** Use payload enrichers to prepare data

**Example:**

```yaml
# config-repo/templates/states/california.yml
pdfMerge:
  sections:
    - name: ca-dmhc-acroform
      type: acroform
      template: ca-dmhc-form-2025.pdf
      enabled: true
      payloadEnrichers:
        - "FullNameEnricher"       # Combines firstName + lastName
        - "PremiumCalculator"      # Calculates total premiums
        - "DateFormatter"          # Formats dates for form fields
      fieldMapping:
        # Uses enriched fields
        "MemberFullName": "enriched.fullName"
        "TotalMonthlyPremium": "enriched.totalPremium"
        "FormattedDOB": "enriched.formattedDateOfBirth"
        
        # Original fields
        "MemberFirstName": "primaryApplicant.firstName"
        "PlanName": "selectedPlans.medical.planName"
```

**Enricher Implementation:**

```java
@Component("FullNameEnricher")
public class FullNameEnricher implements PayloadEnricher {
    @Override
    public void enrich(Map<String, Object> payload) {
        Map<String, Object> applicant = (Map) payload.get("primaryApplicant");
        String fullName = applicant.get("firstName") + " " + applicant.get("lastName");
        
        Map<String, Object> enriched = (Map) payload.computeIfAbsent("enriched", k -> new HashMap<>());
        enriched.put("fullName", fullName);
    }
}
```

---

## Testing Configuration Selection with AcroForms

### Preview Config Selection

**Endpoint:** `POST /api/enrollment/preview-config`

**Request:**
```json
{
  "products": ["medical", "dental"],
  "marketCategory": "individual",
  "state": "CA"
}
```

**Response:**
```json
{
  "conventionBasedConfig": "dental-medical-individual-ca.yml",
  "composition": {
    "base": "templates/base-payer.yml",
    "components": [
      "templates/products/dental.yml",
      "templates/products/medical.yml",
      "templates/markets/individual.yml",
      "templates/states/california.yml"
    ]
  },
  "sections": [
    {"name": "cover-page", "type": "pdfbox"},
    {"name": "medical-coverage", "type": "freemarker"},
    {"name": "dental-coverage", "type": "freemarker"},
    {"name": "dental-acknowledgment-form", "type": "acroform"},
    {"name": "ca-dmhc-acroform", "type": "acroform"},
    {"name": "ca-benefit-mandates", "type": "freemarker"}
  ],
  "acroformSections": [
    {
      "name": "dental-acknowledgment-form",
      "template": "dental-acknowledgment-signature.pdf",
      "source": "templates/products/dental.yml"
    },
    {
      "name": "ca-dmhc-acroform",
      "template": "ca-dmhc-form-2025.pdf",
      "source": "templates/states/california.yml"
    }
  ]
}
```

---

## Best Practices

### 1. **Organize AcroForms by Dimension**

- **State-specific forms** → `templates/states/{state}.yml`
- **Market-specific forms** (Medicare CMS forms) → `templates/markets/{market}.yml`
- **Product-specific forms** (Dental acknowledgments) → `templates/products/{product}.yml`

### 2. **Use Consistent Field Naming**

```yaml
# Good: Consistent across all AcroForms
fieldMapping:
  "MemberFirstName": "primaryApplicant.firstName"
  "MemberLastName": "primaryApplicant.lastName"
  "MemberDOB": "primaryApplicant.dateOfBirth"

# Bad: Inconsistent naming
fieldMapping:
  "FirstName": "member.fname"          # Different path
  "ApplicantLastName": "applicant.ln"  # Different conventions
```

### 3. **Use Payload Enrichers for Complex Logic**

Don't put logic in field mappings - use enrichers:

```yaml
# Good
payloadEnrichers:
  - "TotalPremiumCalculator"
fieldMapping:
  "TotalPremium": "enriched.totalPremium"

# Bad
fieldMapping:
  "TotalPremium": "???complicated.calculation???"  # Won't work!
```

### 4. **Document Required Fields**

```yaml
sections:
  - name: ca-dmhc-acroform
    type: acroform
    template: ca-dmhc-form-2025.pdf
    enabled: true
    # Document what payload fields are required
    # REQUIRED PAYLOAD FIELDS:
    #   - primaryApplicant.firstName
    #   - primaryApplicant.lastName
    #   - primaryApplicant.dateOfBirth
    #   - selectedPlans.medical.planName
    #   - acknowledgments.networkRestrictions
    fieldMapping:
      "MemberFirstName": "primaryApplicant.firstName"
      "MemberLastName": "primaryApplicant.lastName"
      # ...
```

### 5. **Test Each Component Independently**

```bash
# Test California component with medical individual
curl -X POST http://localhost:8080/api/enrollment/generate \
  -d '{"enrollment": {"products": ["medical"], "marketCategory": "individual", "state": "CA"}, ...}'

# Test Texas component with same products/market
curl -X POST http://localhost:8080/api/enrollment/generate \
  -d '{"enrollment": {"products": ["medical"], "marketCategory": "individual", "state": "TX"}, ...}'

# Verify CA includes AcroForm, TX doesn't (or has different one)
```

---

## Summary

### How AcroForm Sections Work with Dynamic Selection

1. **Configuration Selection** based on products/market/state
2. **Component Composition** merges sections from multiple files
3. **AcroForm Sections** defined in any component file
4. **Field Mappings** resolve payload paths to PDF fields
5. **Result**: Automatic inclusion of correct AcroForms based on enrollment parameters

### Key Points

✅ **AcroForm sections work exactly like FreeMarker/PDFBox sections**
✅ **State-specific AcroForms** → Put in `templates/states/{state}.yml`
✅ **Market-specific AcroForms** (Medicare CMS) → Put in `templates/markets/{market}.yml`
✅ **Product-specific AcroForms** (Dental) → Put in `templates/products/{product}.yml`
✅ **Conditional AcroForms** → Use `conditionalSections` with business rules
✅ **Field Mapping** uses dot-notation payload paths
✅ **Multiple AcroForms** can appear in same PDF from different components

### Example Final Structure

**Request:** Medical + Dental, Individual, California

**Result PDF:**
1. Cover (PDFBox)
2. Medical sections (FreeMarker)
3. Dental sections (FreeMarker)
4. **Dental AcroForm** (from dental.yml)
5. Individual market sections (FreeMarker)
6. **CA DMHC AcroForm** (from california.yml)
7. CA-specific sections (FreeMarker)

**All automatically selected and composed based on enrollment parameters!** 🎯
