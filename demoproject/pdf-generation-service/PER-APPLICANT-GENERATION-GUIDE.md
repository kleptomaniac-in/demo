# Per-Applicant PDF Generation Guide
## Separate Forms for Each Applicant with Individual Product Selections

---

## Overview

Your scenario: **Multiple applicants in an enrollment, each can select different products/plans, need separate forms per applicant**

### Example Family Enrollment
- **Primary**: John (medical + dental)
- **Spouse**: Jane (medical + vision)  
- **Dependent 1**: Emily (medical only)
- **Dependent 2**: Michael (medical + dental + vision)

**Question:** How do you generate individual forms for each applicant with their specific product selections?

---

## Two Approaches

### Approach 1: Loop-Based Generation (Single API Call)
Generate all applicant PDFs in one API call, loop over applicants server-side

### Approach 2: Per-Applicant API Calls (Client-Side Loop)
Call API once per applicant, client controls the loop

---

## Approach 1: Loop-Based Generation (Recommended)

### How It Works

```
Single API Request
    ↓
Payload contains all applicants
    ↓
Server iterates over applicants
    ↓
For each applicant:
  - Determine products/plans selected
  - Select appropriate config based on products
  - Generate PDF for this applicant
    ↓
Return: Multiple PDFs (one per applicant)
```

### Configuration Structure

**Input Payload:**

```json
{
  "enrollment": {
    "marketCategory": "individual",
    "state": "CA",
    "generatePerApplicant": true
  },
  "applicants": [
    {
      "applicantId": "A001",
      "relationship": "PRIMARY",
      "firstName": "John",
      "lastName": "Smith",
      "dateOfBirth": "1980-05-15",
      "selectedProducts": ["medical", "dental"],
      "selectedPlans": {
        "medical": {
          "planId": "GOLD_PPO",
          "planName": "Gold PPO",
          "premium": 450.00
        },
        "dental": {
          "planId": "PREMIUM_DENTAL",
          "planName": "Premium Dental",
          "premium": 45.00
        }
      }
    },
    {
      "applicantId": "A002",
      "relationship": "SPOUSE",
      "firstName": "Jane",
      "lastName": "Smith",
      "dateOfBirth": "1982-07-22",
      "selectedProducts": ["medical", "vision"],
      "selectedPlans": {
        "medical": {
          "planId": "SILVER_HMO",
          "planName": "Silver HMO",
          "premium": 350.00
        },
        "vision": {
          "planId": "BASIC_VISION",
          "planName": "Basic Vision",
          "premium": 15.00
        }
      }
    },
    {
      "applicantId": "A003",
      "relationship": "DEPENDENT",
      "firstName": "Emily",
      "lastName": "Smith",
      "dateOfBirth": "2015-03-10",
      "selectedProducts": ["medical"],
      "selectedPlans": {
        "medical": {
          "planId": "GOLD_PPO",
          "planName": "Gold PPO",
          "premium": 250.00
        }
      }
    },
    {
      "applicantId": "A004",
      "relationship": "DEPENDENT",
      "firstName": "Michael",
      "lastName": "Smith",
      "dateOfBirth": "2017-08-05",
      "selectedProducts": ["medical", "dental", "vision"],
      "selectedPlans": {
        "medical": {
          "planId": "GOLD_PPO",
          "planName": "Gold PPO",
          "premium": 250.00
        },
        "dental": {
          "planId": "PREMIUM_DENTAL",
          "planName": "Premium Dental",
          "premium": 30.00
        },
        "vision": {
          "planId": "BASIC_VISION",
          "planName": "Basic Vision",
          "premium": 10.00
        }
      }
    }
  ]
}
```

### Service Implementation

```java
@Service
public class PerApplicantPdfGeneratorService {
    
    @Autowired
    private FlexiblePdfMergeService pdfMergeService;
    
    @Autowired
    private ConfigSelectionService configSelectionService;
    
    /**
     * Generate separate PDF for each applicant based on their selected products
     */
    public Map<String, byte[]> generatePerApplicantPdfs(PerApplicantEnrollmentRequest request) {
        Map<String, byte[]> applicantPdfs = new HashMap<>();
        
        for (ApplicantWithSelections applicant : request.getApplicants()) {
            // 1. Build enrollment submission for config selection
            EnrollmentSubmission enrollment = new EnrollmentSubmission();
            enrollment.setProducts(applicant.getSelectedProducts());
            enrollment.setMarketCategory(request.getEnrollment().getMarketCategory());
            enrollment.setState(request.getEnrollment().getState());
            
            // 2. Select config based on applicant's products
            String configName = configSelectionService.selectConfigByConvention(enrollment);
            
            // 3. Build payload for this specific applicant
            Map<String, Object> applicantPayload = buildApplicantPayload(
                applicant, 
                request.getEnrollment()
            );
            
            // 4. Generate PDF for this applicant
            try {
                byte[] pdf = pdfMergeService.generateMergedPdf(configName, applicantPayload);
                String filename = String.format("%s-%s-enrollment.pdf", 
                    applicant.getFirstName(), 
                    applicant.getLastName());
                applicantPdfs.put(filename, pdf);
                
                log.info("Generated PDF for applicant {} ({}) using config {}", 
                    applicant.getApplicantId(), 
                    applicant.getFirstName(), 
                    configName);
                    
            } catch (IOException e) {
                log.error("Failed to generate PDF for applicant " + applicant.getApplicantId(), e);
                throw new RuntimeException("PDF generation failed for " + applicant.getApplicantId(), e);
            }
        }
        
        return applicantPdfs;
    }
    
    /**
     * Build payload for individual applicant
     */
    private Map<String, Object> buildApplicantPayload(
            ApplicantWithSelections applicant, 
            EnrollmentInfo enrollment) {
        
        Map<String, Object> payload = new HashMap<>();
        
        // Applicant information
        payload.put("applicantId", applicant.getApplicantId());
        payload.put("relationship", applicant.getRelationship());
        payload.put("firstName", applicant.getFirstName());
        payload.put("lastName", applicant.getLastName());
        payload.put("dateOfBirth", applicant.getDateOfBirth());
        payload.put("fullName", applicant.getFirstName() + " " + applicant.getLastName());
        
        // Selected products and plans
        payload.put("selectedProducts", applicant.getSelectedProducts());
        payload.put("selectedPlans", applicant.getSelectedPlans());
        
        // Calculate total premium
        double totalPremium = applicant.getSelectedPlans().values().stream()
            .mapToDouble(plan -> (Double) plan.get("premium"))
            .sum();
        payload.put("totalMonthlyPremium", totalPremium);
        
        // Enrollment metadata
        payload.put("marketCategory", enrollment.getMarketCategory());
        payload.put("state", enrollment.getState());
        payload.put("enrollmentDate", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        payload.put("effectiveDate", enrollment.getEffectiveDate());
        
        return payload;
    }
}
```

### Controller Endpoint

```java
@RestController
@RequestMapping("/api/enrollment")
public class PerApplicantEnrollmentController {
    
    @Autowired
    private PerApplicantPdfGeneratorService pdfGeneratorService;
    
    /**
     * Generate individual enrollment PDFs for each applicant
     * Returns a ZIP file containing all applicant PDFs
     * 
     * POST /api/enrollment/generate-per-applicant
     */
    @PostMapping("/generate-per-applicant")
    public ResponseEntity<byte[]> generatePerApplicant(
            @RequestBody PerApplicantEnrollmentRequest request) {
        
        try {
            // Generate PDFs for all applicants
            Map<String, byte[]> applicantPdfs = pdfGeneratorService.generatePerApplicantPdfs(request);
            
            // Package as ZIP
            byte[] zipBytes = createZipFile(applicantPdfs);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", "enrollment-forms.zip");
            headers.setContentLength(zipBytes.length);
            
            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error generating per-applicant PDFs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Alternative: Return JSON with Base64-encoded PDFs
     */
    @PostMapping("/generate-per-applicant-json")
    public ResponseEntity<Map<String, String>> generatePerApplicantJson(
            @RequestBody PerApplicantEnrollmentRequest request) {
        
        Map<String, byte[]> applicantPdfs = pdfGeneratorService.generatePerApplicantPdfs(request);
        
        // Convert to Base64
        Map<String, String> base64Pdfs = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : applicantPdfs.entrySet()) {
            String base64 = Base64.getEncoder().encodeToString(entry.getValue());
            base64Pdfs.put(entry.getKey(), base64);
        }
        
        return ResponseEntity.ok(base64Pdfs);
    }
    
    private byte[] createZipFile(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
```

### Request Example

```bash
curl -X POST http://localhost:8080/api/enrollment/generate-per-applicant \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "marketCategory": "individual",
      "state": "CA",
      "effectiveDate": "2026-01-01"
    },
    "applicants": [
      {
        "applicantId": "A001",
        "relationship": "PRIMARY",
        "firstName": "John",
        "lastName": "Smith",
        "dateOfBirth": "1980-05-15",
        "selectedProducts": ["medical", "dental"],
        "selectedPlans": {
          "medical": {"planId": "GOLD_PPO", "planName": "Gold PPO", "premium": 450.00},
          "dental": {"planId": "PREMIUM_DENTAL", "planName": "Premium Dental", "premium": 45.00}
        }
      },
      {
        "applicantId": "A002",
        "relationship": "SPOUSE",
        "firstName": "Jane",
        "lastName": "Smith",
        "dateOfBirth": "1982-07-22",
        "selectedProducts": ["medical", "vision"],
        "selectedPlans": {
          "medical": {"planId": "SILVER_HMO", "planName": "Silver HMO", "premium": 350.00},
          "vision": {"planId": "BASIC_VISION", "planName": "Basic Vision", "premium": 15.00}
        }
      }
    ]
  }' \
  -o enrollment-forms.zip
```

**Result:** ZIP file containing:
- `John-Smith-enrollment.pdf` (medical + dental sections from `dental-medical-individual-ca.yml`)
- `Jane-Smith-enrollment.pdf` (medical + vision sections from `medical-vision-individual-ca.yml`)

### Config Selection Flow

```
Applicant: John (medical + dental)
    ↓
Products: ["medical", "dental"]
Market: "individual"
State: "CA"
    ↓
Config selected: dental-medical-individual-ca.yml
    ↓
Components loaded:
  - templates/products/dental.yml
  - templates/products/medical.yml
  - templates/markets/individual.yml
  - templates/states/california.yml
    ↓
PDF generated with John's data

═══════════════════════════════════════════════

Applicant: Jane (medical + vision)
    ↓
Products: ["medical", "vision"]
Market: "individual"
State: "CA"
    ↓
Config selected: medical-vision-individual-ca.yml
    ↓
Components loaded:
  - templates/products/medical.yml
  - templates/products/vision.yml
  - templates/markets/individual.yml
  - templates/states/california.yml
    ↓
PDF generated with Jane's data
```

---

## Approach 2: Per-Applicant API Calls (Client-Side Loop)

### How It Works

Client application loops over applicants and calls API once per applicant

### Implementation

**Client-Side Loop (JavaScript Example):**

```javascript
async function generateApplicantEnrollments(applicants, enrollmentInfo) {
  const pdfs = [];
  
  for (const applicant of applicants) {
    // Build request for this applicant
    const request = {
      enrollment: {
        products: applicant.selectedProducts,
        marketCategory: enrollmentInfo.marketCategory,
        state: enrollmentInfo.state
      },
      payload: {
        applicantId: applicant.applicantId,
        relationship: applicant.relationship,
        firstName: applicant.firstName,
        lastName: applicant.lastName,
        dateOfBirth: applicant.dateOfBirth,
        selectedPlans: applicant.selectedPlans,
        effectiveDate: enrollmentInfo.effectiveDate
      }
    };
    
    // Call API for this applicant
    const response = await fetch('/api/enrollment/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });
    
    const pdfBlob = await response.blob();
    pdfs.push({
      filename: `${applicant.firstName}-${applicant.lastName}-enrollment.pdf`,
      blob: pdfBlob
    });
  }
  
  return pdfs;
}

// Usage
const applicants = [
  {
    applicantId: 'A001',
    relationship: 'PRIMARY',
    firstName: 'John',
    lastName: 'Smith',
    dateOfBirth: '1980-05-15',
    selectedProducts: ['medical', 'dental'],
    selectedPlans: { /* ... */ }
  },
  {
    applicantId: 'A002',
    relationship: 'SPOUSE',
    firstName: 'Jane',
    lastName: 'Smith',
    dateOfBirth: '1982-07-22',
    selectedProducts: ['medical', 'vision'],
    selectedPlans: { /* ... */ }
  }
];

const pdfs = await generateApplicantEnrollments(applicants, {
  marketCategory: 'individual',
  state: 'CA',
  effectiveDate: '2026-01-01'
});

// Download or display PDFs
pdfs.forEach(pdf => {
  const url = URL.createObjectURL(pdf.blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = pdf.filename;
  link.click();
});
```

### cURL Example (Multiple Calls)

```bash
# Generate PDF for Primary (John - medical + dental)
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical", "dental"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "applicantId": "A001",
      "relationship": "PRIMARY",
      "firstName": "John",
      "lastName": "Smith",
      "selectedPlans": {
        "medical": {"planName": "Gold PPO", "premium": 450.00},
        "dental": {"planName": "Premium Dental", "premium": 45.00}
      }
    }
  }' \
  -o john-smith-enrollment.pdf

# Generate PDF for Spouse (Jane - medical + vision)
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical", "vision"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "applicantId": "A002",
      "relationship": "SPOUSE",
      "firstName": "Jane",
      "lastName": "Smith",
      "selectedPlans": {
        "medical": {"planName": "Silver HMO", "premium": 350.00},
        "vision": {"planName": "Basic Vision", "premium": 15.00}
      }
    }
  }' \
  -o jane-smith-enrollment.pdf

# Generate PDF for Dependent (Emily - medical only)
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {
      "applicantId": "A003",
      "relationship": "DEPENDENT",
      "firstName": "Emily",
      "lastName": "Smith",
      "selectedPlans": {
        "medical": {"planName": "Gold PPO", "premium": 250.00}
      }
    }
  }' \
  -o emily-smith-enrollment.pdf
```

---

## Comparison: Loop-Based vs Per-Applicant Calls

| Aspect | Loop-Based (Approach 1) | Per-Applicant Calls (Approach 2) |
|--------|------------------------|----------------------------------|
| **API Calls** | 1 | N (one per applicant) |
| **Network Overhead** | Low | Higher |
| **Server Load** | Single transaction | Multiple transactions |
| **Client Complexity** | Lower | Higher (client loops) |
| **Progress Tracking** | Server-side | Client-side (easier) |
| **Error Handling** | All-or-nothing | Per-applicant retry |
| **Result Format** | ZIP file or JSON | Individual PDFs |
| **Best For** | Batch processing | Interactive UI |

---

## Advanced: Conditional Sections per Applicant

### Scenario: Add AcroForm Only for Adults

**Configuration:** `templates/states/california.yml`

```yaml
pdfMerge:
  sections:
    - name: ca-dmhc-disclosure
      type: freemarker
      template: templates/states/california-dmhc-disclosure.ftl
      enabled: true
  
  conditionalSections:
    # AcroForm signature only for applicants >= 18 years old
    - condition: "payload.age >= 18"
      sections:
        - name: applicant-signature-form
          type: acroform
          template: applicant-signature-form.pdf
          insertAfter: ca-dmhc-disclosure
          fieldMapping:
            "ApplicantName": "fullName"
            "ApplicantDOB": "dateOfBirth"
            "SignatureDate": "enrollmentDate"
```

**Payload Processing:**

```java
private Map<String, Object> buildApplicantPayload(ApplicantWithSelections applicant) {
    Map<String, Object> payload = new HashMap<>();
    
    // ... basic fields
    
    // Calculate age for conditional sections
    LocalDate birthDate = LocalDate.parse(applicant.getDateOfBirth());
    LocalDate today = LocalDate.now();
    int age = Period.between(birthDate, today).getYears();
    payload.put("age", age);
    
    return payload;
}
```

**Result:**
- Adults (18+): PDF includes signature AcroForm
- Children (<18): PDF without signature form

---

## Advanced: Different Templates by Relationship

### Scenario: Different Forms for Primary vs Dependents

**Service Logic:**

```java
public byte[] generateApplicantPdf(ApplicantWithSelections applicant, EnrollmentInfo enrollment) {
    String configName;
    
    // Select different config based on relationship
    if ("PRIMARY".equals(applicant.getRelationship()) || 
        "SPOUSE".equals(applicant.getRelationship())) {
        // Adults get full enrollment form
        configName = selectAdultEnrollmentConfig(applicant, enrollment);
    } else {
        // Dependents get simplified form
        configName = selectDependentEnrollmentConfig(applicant, enrollment);
    }
    
    Map<String, Object> payload = buildApplicantPayload(applicant, enrollment);
    
    return pdfMergeService.generateMergedPdf(configName, payload);
}

private String selectAdultEnrollmentConfig(ApplicantWithSelections applicant, EnrollmentInfo enrollment) {
    // Use full enrollment flow with config selection
    EnrollmentSubmission enrollmentSubmission = new EnrollmentSubmission();
    enrollmentSubmission.setProducts(applicant.getSelectedProducts());
    enrollmentSubmission.setMarketCategory(enrollment.getMarketCategory());
    enrollmentSubmission.setState(enrollment.getState());
    
    return configSelectionService.selectConfigByConvention(enrollmentSubmission);
}

private String selectDependentEnrollmentConfig(ApplicantWithSelections applicant, EnrollmentInfo enrollment) {
    // Use simplified template for dependents
    String products = String.join("-", applicant.getSelectedProducts().stream()
        .sorted()
        .collect(Collectors.toList()));
    
    return String.format("dependent-%s-%s.yml", products, enrollment.getState().toLowerCase());
}
```

**Example Configs:**

**Adult Form:** `dental-medical-individual-ca.yml`
```yaml
pdfMerge:
  sections:
    - name: cover-page
      type: pdfbox
      template: adult-enrollment-cover-generator
    
    - name: applicant-information
      type: freemarker
      template: templates/adult-applicant-info.ftl
    
    - name: medical-coverage
      type: freemarker
      template: templates/products/medical-coverage-details.ftl
    
    - name: dental-coverage
      type: freemarker
      template: templates/products/dental-coverage-details.ftl
    
    - name: signature-form
      type: acroform
      template: adult-enrollment-signature.pdf
      fieldMapping:
        "ApplicantName": "fullName"
        "ApplicantSSN": "ssn"
        "ApplicantSignature": ""
```

**Dependent Form:** `dependent-medical-ca.yml`
```yaml
pdfMerge:
  sections:
    - name: dependent-info
      type: freemarker
      template: templates/dependent-simple-info.ftl
    
    - name: medical-coverage-summary
      type: freemarker
      template: templates/products/medical-coverage-summary.ftl
    
    # No signature form for dependents
```

---

## Complete Example with AcroForms

### Scenario: Each Applicant Gets Product-Specific AcroForms

**Request:**

```json
{
  "enrollment": {
    "marketCategory": "individual",
    "state": "CA"
  },
  "applicants": [
    {
      "applicantId": "A001",
      "relationship": "PRIMARY",
      "firstName": "John",
      "lastName": "Smith",
      "selectedProducts": ["medical", "dental"]
    },
    {
      "applicantId": "A002",
      "relationship": "SPOUSE",
      "firstName": "Jane",
      "lastName": "Smith",
      "selectedProducts": ["vision"]
    }
  ]
}
```

**Generated PDFs:**

**John's PDF** (from `dental-medical-individual-ca.yml`):
1. Cover page
2. Medical coverage details (FreeMarker)
3. Dental coverage details (FreeMarker)
4. **Dental acknowledgment form (AcroForm)** - from dental.yml component
5. **CA DMHC disclosure form (AcroForm)** - from california.yml component

**Jane's PDF** (from `vision-individual-ca.yml`):
1. Cover page
2. Vision coverage details (FreeMarker)
3. **Vision benefits waiver (AcroForm)** - from vision.yml component
4. **CA DMHC disclosure form (AcroForm)** - from california.yml component

---

## Summary

### For Your Scenario: Each Applicant with Different Product Selections

**Recommended Flow:**

1. **Client sends single request** with all applicants and their product selections
2. **Server loops over applicants** (Approach 1)
3. **For each applicant:**
   - Extract their selected products
   - Use `ConfigSelectionService.selectConfigByConvention()` to get config name
   - Config name based on: `{products}-{market}-{state}.yml`
   - Load config with component composition
   - Components include product-specific sections (including AcroForms)
   - Generate PDF for this applicant
4. **Return ZIP file** with all applicant PDFs

### Key Points

✅ **Each applicant gets their own PDF**
✅ **Config auto-selected based on their products**
✅ **Product components include AcroForms automatically**
✅ **State-specific AcroForms included for all**
✅ **Single API call for all applicants (efficient)**
✅ **Support for conditional sections based on age/relationship**

### Example Result

**Family of 4 with varied selections:**

```
enrollment-forms.zip
├── John-Smith-enrollment.pdf        (medical + dental)
├── Jane-Smith-enrollment.pdf        (medical + vision)
├── Emily-Smith-enrollment.pdf       (medical only)
└── Michael-Smith-enrollment.pdf     (medical + dental + vision)
```

Each PDF automatically includes:
- Product-specific sections for their selections
- Product-specific AcroForms (if defined in component)
- State-mandated AcroForms
- Personalized with their data

**The existing config selection + component composition system handles everything automatically!**
