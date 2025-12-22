You're absolutely right! There would be significant duplication of common fields (address, enrollment date, application ID, etc.) if generating completely separate PDFs per applicant.

Let me clarify the **two different scenarios** and which approach makes sense:

---

## Scenario 1: Single Family Enrollment PDF (Recommended for Most Cases)

**When:** All applicants are part of one enrollment, sharing common info

**Approach:** Generate **ONE PDF** showing all applicants with their individual product selections

### Example Structure

**One PDF with:**
- **Section 1: Application Information** (shared)
  - Application ID, enrollment date, effective date
  - Primary applicant information
  - Billing/mailing addresses
  
- **Section 2: Applicant Coverage Summary** (dynamic table/list)
  - Primary: Medical (Gold PPO), Dental (Premium)
  - Spouse: Medical (Silver HMO), Vision (Basic)
  - Dependent 1: Medical (Gold PPO)
  - Dependent 2: Medical (Gold PPO), Dental (Premium), Vision (Basic)

- **Section 3: Product Details** (only for selected products)
  - Medical coverage details (if anyone selected)
  - Dental coverage details (if anyone selected)
  - Vision coverage details (if anyone selected)

- **Section 4: State-Specific Forms** (shared AcroForm)
  - All applicants sign on one form
  - Fields for Primary signature, Spouse signature, etc.

### Configuration Approach

**Select config based on FAMILY's combined products:**

```javascript
// Determine all products selected by ANY applicant
const allProducts = [...new Set(
  applicants.flatMap(a => a.selectedProducts)
)].sort(); // e.g., ["dental", "medical", "vision"]

// Select config for family
const configName = `${allProducts.join('-')}-${market}-${state}.yml`;
// Example: "dental-medical-vision-individual-ca.yml"
```

**Single Request:**

```json
{
  "enrollment": {
    "products": ["medical", "dental", "vision"],  // Combined from all applicants
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "applicationId": "APP-001",
    "enrollmentDate": "2025-12-18",
    "effectiveDate": "2026-01-01",
    
    "billingAddress": {
      "street": "123 Main St",
      "city": "Los Angeles",
      "state": "CA"
    },
    
    "applicants": [
      {
        "relationship": "PRIMARY",
        "firstName": "John",
        "lastName": "Smith",
        "dateOfBirth": "1980-05-15",
        "selectedProducts": ["medical", "dental"],
        "selectedPlans": {
          "medical": {"planName": "Gold PPO", "premium": 450},
          "dental": {"planName": "Premium", "premium": 45}
        }
      },
      {
        "relationship": "SPOUSE",
        "firstName": "Jane",
        "lastName": "Smith",
        "dateOfBirth": "1982-07-22",
        "selectedProducts": ["medical", "vision"],
        "selectedPlans": {
          "medical": {"planName": "Silver HMO", "premium": 350},
          "vision": {"planName": "Basic", "premium": 15}
        }
      },
      {
        "relationship": "DEPENDENT",
        "firstName": "Emily",
        "dateOfBirth": "2015-03-10",
        "selectedProducts": ["medical"],
        "selectedPlans": {
          "medical": {"planName": "Gold PPO", "premium": 250}
        }
      }
    ]
  }
}
```

**FreeMarker Template Example:**

```html
<!-- templates/applicant-coverage-summary.ftl -->
<h2>Applicant Coverage Summary</h2>

<table>
  <thead>
    <tr>
      <th>Applicant</th>
      <th>Relationship</th>
      <th>Date of Birth</th>
      <th>Products Selected</th>
      <th>Monthly Premium</th>
    </tr>
  </thead>
  <tbody>
    <#list payload.applicants as applicant>
    <tr>
      <td>${applicant.firstName} ${applicant.lastName}</td>
      <td>${applicant.relationship}</td>
      <td>${applicant.dateOfBirth}</td>
      <td>
        <#list applicant.selectedPlans as productType, plan>
          ${plan.planName}<#if productType?has_next>, </#if>
        </#list>
      </td>
      <td>
        <#assign total = 0>
        <#list applicant.selectedPlans as productType, plan>
          <#assign total = total + plan.premium>
        </#list>
        $${total?string["0.00"]}
      </td>
    </tr>
    </#list>
    
    <tr style="font-weight: bold;">
      <td colspan="4">Family Total</td>
      <td>
        <#assign familyTotal = 0>
        <#list payload.applicants as applicant>
          <#list applicant.selectedPlans as productType, plan>
            <#assign familyTotal = familyTotal + plan.premium>
          </#list>
        </#list>
        $${familyTotal?string["0.00"]}
      </td>
    </tr>
  </tbody>
</table>
```

**Result:** **ONE PDF** with all applicants, no duplication of common fields

---

## Scenario 2: Separate PDFs Per Applicant (Less Common)

**When:** Each applicant needs standalone document (e.g., separate signature requirements, individual compliance)

**Issue:** As you noted, common fields duplicated across PDFs

**Solutions:**

### Option A: Minimal Applicant-Specific PDFs

Only include essential applicant info + product selections, reference main enrollment

```json
// Applicant-specific payload (minimal)
{
  "applicantId": "A001",
  "applicantName": "John Smith",
  "relationship": "PRIMARY",
  "dateOfBirth": "1980-05-15",
  
  "referenceApplicationId": "APP-001",  // Reference to main enrollment
  
  "selectedProducts": ["medical", "dental"],
  "selectedPlans": {
    "medical": {"planName": "Gold PPO", "premium": 450},
    "dental": {"planName": "Premium", "premium": 45}
  }
}
```

**PDF Content:**
- Applicant information
- Their product selections
- Reference: "Part of Application APP-001"
- Signature line

**No duplication of:** addresses, enrollment dates, other applicants' info

### Option B: Accept Duplication for Standalone Forms

Include ALL information in each PDF for completeness

**Use Case:** Each applicant must have complete standalone enrollment form

**Trade-off:** Duplication accepted for regulatory/business requirements

---

## Recommended Approach: Single Family PDF

**Configuration:** dental-medical-vision-individual-ca.yml

```yaml
pdfMerge:
  sections:
    # Shared application information
    - name: application-header
      type: freemarker
      template: templates/enrollment/application-header.ftl
      enabled: true
    
    # Address information (once)
    - name: address-information
      type: freemarker
      template: templates/enrollment/addresses.ftl
      enabled: true
    
    # Applicant coverage summary (all applicants with their products)
    - name: applicant-coverage-summary
      type: freemarker
      template: templates/enrollment/applicant-coverage-summary.ftl
      enabled: true
    
    # Product details (only for products selected by ANY applicant)
    - name: medical-coverage
      type: freemarker
      template: templates/products/medical-coverage-details.ftl
      enabled: true
      condition: "payload.applicants.any(a => a.selectedProducts.contains('medical'))"
    
    - name: dental-coverage
      type: freemarker
      template: templates/products/dental-coverage-details.ftl
      enabled: true
      condition: "payload.applicants.any(a => a.selectedProducts.contains('dental'))"
    
    - name: vision-coverage
      type: freemarker
      template: templates/products/vision-coverage-details.ftl
      enabled: true
      condition: "payload.applicants.any(a => a.selectedProducts.contains('vision'))"
    
    # Signature form (one form with fields for all applicants)
    - name: family-signature-form
      type: acroform
      template: family-enrollment-signature.pdf
      enabled: true
      fieldMapping:
        # Application info (once)
        "ApplicationId": "applicationId"
        "EnrollmentDate": "enrollmentDate"
        "BillingStreet": "billingAddress.street"
        "BillingCity": "billingAddress.city"
        
        # Applicant fields
        "Primary_Name": "applicants[relationship=PRIMARY].firstName"
        "Primary_DOB": "applicants[relationship=PRIMARY].dateOfBirth"
        "Primary_Signature": ""
        
        "Spouse_Name": "applicants[relationship=SPOUSE].firstName"
        "Spouse_DOB": "applicants[relationship=SPOUSE].dateOfBirth"
        "Spouse_Signature": ""
        
        # Product selections summary
        "Primary_Products": "applicants[relationship=PRIMARY].selectedProducts"
        "Spouse_Products": "applicants[relationship=SPOUSE].selectedProducts"
```

---

## Comparison

| Aspect | Single Family PDF | Per-Applicant PDFs |
|--------|------------------|-------------------|
| **Common Fields** | Included once | Duplicated in each PDF |
| **Data Efficiency** | High | Low (duplication) |
| **PDF Count** | 1 | N (one per applicant) |
| **Use Case** | Family enrollment | Individual compliance docs |
| **Complexity** | Lower | Higher |
| **Signature** | All on one form | Separate forms |
| **Maintenance** | Easier | More complex |

---

## Summary

**Your concern is valid!** For most enrollment scenarios:

✅ **Use Single Family PDF** with:
- Common fields included once (no duplication)
- Dynamic table/list showing all applicants and their product selections
- Product details included only if any applicant selected them
- One signature form with fields for all applicants
- Config selection based on combined products from all applicants

❌ **Avoid Per-Applicant PDFs** unless:
- Regulatory requirement for separate forms
- Different signature/submission processes per applicant
- Applicants enrolling at different times

**The existing system supports both approaches, but single family PDF is more efficient and avoids the duplication issue you identified.**