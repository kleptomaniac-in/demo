# Enhanced Filter Syntax for AcroForm Field Mappings

The `AcroFormFillService` now supports advanced filter syntax in YAML configurations for complex array and nested object scenarios.

## Supported Syntax

### 1. Simple Property Access
```yaml
fieldMapping:
  "MemberName": "member.name"
  "MemberId": "member.memberId"
```

### 2. Array Index Access
```yaml
fieldMapping:
  "Dependent1Name": "dependents[0].name"
  "Dependent2Name": "dependents[1].name"
  "Dependent3DOB": "dependents[2].dateOfBirth"
```

### 3. Filter by Field Value (NEW)
```yaml
fieldMapping:
  # Filter applicants array by relationship field
  "PrimaryFirstName": "applicants[relationship=PRIMARY].demographic.firstName"
  "PrimaryLastName": "applicants[relationship=PRIMARY].demographic.lastName"
  "PrimaryDOB": "applicants[relationship=PRIMARY].demographic.dateOfBirth"
  
  "SpouseFirstName": "applicants[relationship=SPOUSE].demographic.firstName"
  "SpouseLastName": "applicants[relationship=SPOUSE].demographic.lastName"
```

**Behavior:** Returns the **first** item in the array where `relationship` equals the specified value.

### 4. Filter + Index (NEW)
```yaml
fieldMapping:
  # Filter for dependents, then use index to get specific one
  "Dependent1FirstName": "applicants[relationship=DEPENDENT][0].demographic.firstName"
  "Dependent2FirstName": "applicants[relationship=DEPENDENT][1].demographic.firstName"
  "Dependent3FirstName": "applicants[relationship=DEPENDENT][2].demographic.firstName"
```

**Behavior:** First filters the array, then applies the index to the filtered results.

### 5. Multiple Filters (NEW)
```yaml
fieldMapping:
  # Filter by multiple criteria
  "PriorMedicalCarrier": "currentCoverages[applicantId=A001][productType=MEDICAL].carrierName"
  "PriorDentalCarrier": "currentCoverages[applicantId=A001][productType=DENTAL].carrierName"
  
  # Filter addresses by type
  "BillingStreet": "addresses[type=BILLING].street"
  "BillingCity": "addresses[type=BILLING].city"
  "BillingState": "addresses[type=BILLING].state"
  "BillingZip": "addresses[type=BILLING].zipCode"
  
  "MailingStreet": "addresses[type=MAILING].street"
  "MailingCity": "addresses[type=MAILING].city"
```

**Behavior:** Applies filters sequentially. Each filter narrows down the list further.

### 6. Nested Filters
```yaml
fieldMapping:
  # Complex nested filtering
  "PrimaryMedicalPlan": "enrollment.applicants[relationship=PRIMARY].proposedProducts[productType=MEDICAL].planName"
  "SpouseDentalPlan": "enrollment.applicants[relationship=SPOUSE].proposedProducts[productType=DENTAL].planName"
```

## Complete Example

### Sample Payload

```json
{
  "enrollment": {
    "applicants": [
      {
        "applicantId": "A001",
        "relationship": "PRIMARY",
        "demographic": {
          "firstName": "John",
          "lastName": "Smith",
          "dateOfBirth": "1980-05-15",
          "gender": "M"
        },
        "proposedProducts": [
          {
            "productType": "MEDICAL",
            "planName": "Gold PPO",
            "premium": 450.00
          },
          {
            "productType": "DENTAL",
            "planName": "Platinum Dental",
            "premium": 75.00
          }
        ]
      },
      {
        "applicantId": "A002",
        "relationship": "SPOUSE",
        "demographic": {
          "firstName": "Jane",
          "lastName": "Smith",
          "dateOfBirth": "1982-08-22",
          "gender": "F"
        },
        "proposedProducts": [
          {
            "productType": "MEDICAL",
            "planName": "Gold PPO",
            "premium": 450.00
          }
        ]
      },
      {
        "applicantId": "A003",
        "relationship": "DEPENDENT",
        "demographic": {
          "firstName": "Tim",
          "lastName": "Smith",
          "dateOfBirth": "2010-03-10",
          "gender": "M"
        },
        "proposedProducts": [
          {
            "productType": "MEDICAL",
            "planName": "Gold PPO",
            "premium": 350.00
          }
        ]
      },
      {
        "applicantId": "A004",
        "relationship": "DEPENDENT",
        "demographic": {
          "firstName": "Amy",
          "lastName": "Smith",
          "dateOfBirth": "2012-11-18",
          "gender": "F"
        },
        "proposedProducts": [
          {
            "productType": "MEDICAL",
            "planName": "Gold PPO",
            "premium": 350.00
          }
        ]
      }
    ],
    "addresses": [
      {
        "type": "BILLING",
        "street": "123 Main St",
        "city": "Los Angeles",
        "state": "CA",
        "zipCode": "90001"
      },
      {
        "type": "MAILING",
        "street": "PO Box 456",
        "city": "Los Angeles",
        "state": "CA",
        "zipCode": "90002"
      }
    ],
    "currentCoverages": [
      {
        "applicantId": "A001",
        "productType": "MEDICAL",
        "carrierName": "Blue Cross",
        "policyNumber": "BC123456"
      },
      {
        "applicantId": "A001",
        "productType": "DENTAL",
        "carrierName": "Delta Dental",
        "policyNumber": "DD789012"
      }
    ]
  }
}
```

### YAML Configuration with Enhanced Filters

```yaml
pdfMerge:
  sections:
    - name: enrollment-form
      type: acroform
      template: ca-enrollment-form.pdf
      fieldMapping:
        # Primary applicant (filter by relationship)
        "Primary_FirstName": "enrollment.applicants[relationship=PRIMARY].demographic.firstName"
        "Primary_LastName": "enrollment.applicants[relationship=PRIMARY].demographic.lastName"
        "Primary_DOB": "enrollment.applicants[relationship=PRIMARY].demographic.dateOfBirth"
        "Primary_Gender": "enrollment.applicants[relationship=PRIMARY].demographic.gender"
        
        # Primary's product selections (nested filters)
        "Primary_Medical_Plan": "enrollment.applicants[relationship=PRIMARY].proposedProducts[productType=MEDICAL].planName"
        "Primary_Medical_Premium": "enrollment.applicants[relationship=PRIMARY].proposedProducts[productType=MEDICAL].premium"
        "Primary_Dental_Plan": "enrollment.applicants[relationship=PRIMARY].proposedProducts[productType=DENTAL].planName"
        "Primary_Dental_Premium": "enrollment.applicants[relationship=PRIMARY].proposedProducts[productType=DENTAL].premium"
        
        # Spouse (filter by relationship)
        "Spouse_FirstName": "enrollment.applicants[relationship=SPOUSE].demographic.firstName"
        "Spouse_LastName": "enrollment.applicants[relationship=SPOUSE].demographic.lastName"
        "Spouse_DOB": "enrollment.applicants[relationship=SPOUSE].demographic.dateOfBirth"
        
        # Dependents (filter + index)
        "Dependent1_FirstName": "enrollment.applicants[relationship=DEPENDENT][0].demographic.firstName"
        "Dependent1_LastName": "enrollment.applicants[relationship=DEPENDENT][0].demographic.lastName"
        "Dependent1_DOB": "enrollment.applicants[relationship=DEPENDENT][0].demographic.dateOfBirth"
        
        "Dependent2_FirstName": "enrollment.applicants[relationship=DEPENDENT][1].demographic.firstName"
        "Dependent2_LastName": "enrollment.applicants[relationship=DEPENDENT][1].demographic.lastName"
        "Dependent2_DOB": "enrollment.applicants[relationship=DEPENDENT][1].demographic.dateOfBirth"
        
        # Addresses (filter by type)
        "Billing_Street": "enrollment.addresses[type=BILLING].street"
        "Billing_City": "enrollment.addresses[type=BILLING].city"
        "Billing_State": "enrollment.addresses[type=BILLING].state"
        "Billing_Zip": "enrollment.addresses[type=BILLING].zipCode"
        
        "Mailing_Street": "enrollment.addresses[type=MAILING].street"
        "Mailing_City": "enrollment.addresses[type=MAILING].city"
        "Mailing_State": "enrollment.addresses[type=MAILING].state"
        "Mailing_Zip": "enrollment.addresses[type=MAILING].zipCode"
        
        # Prior coverage (multiple filters)
        "Prior_Medical_Carrier": "enrollment.currentCoverages[applicantId=A001][productType=MEDICAL].carrierName"
        "Prior_Medical_Policy": "enrollment.currentCoverages[applicantId=A001][productType=MEDICAL].policyNumber"
        "Prior_Dental_Carrier": "enrollment.currentCoverages[applicantId=A001][productType=DENTAL].carrierName"
        "Prior_Dental_Policy": "enrollment.currentCoverages[applicantId=A001][productType=DENTAL].policyNumber"
```

## Filter Execution Flow

### Example: `"applicants[relationship=DEPENDENT][1].demographic.firstName"`

1. **Start with:** `enrollment` object
2. **Navigate:** `applicants` → Get the applicants array
3. **Apply Filter 1:** `[relationship=DEPENDENT]` → Filter array to only dependents
   - Result: `[A003, A004]` (two dependents)
4. **Apply Filter 2:** `[1]` → Get index 1 from filtered array
   - Result: `A004` (Amy)
5. **Navigate:** `demographic.firstName` → "Amy"
6. **Final value:** `"Amy"`

### Example: `"currentCoverages[applicantId=A001][productType=MEDICAL].carrierName"`

1. **Navigate:** `enrollment.currentCoverages` → Get array
2. **Apply Filter 1:** `[applicantId=A001]` → Filter to A001's coverages only
3. **Apply Filter 2:** `[productType=MEDICAL]` → Further filter to medical coverage
4. **Navigate:** `carrierName` → "Blue Cross"

## Error Handling

The implementation includes robust error handling:

- **No matching items:** Returns null, logs warning
- **Index out of bounds:** Returns null, logs warning
- **Invalid filter syntax:** Returns null, logs warning
- **Non-list object:** Returns null, logs warning

## Performance Considerations

- Filters are applied sequentially (not optimized for very large arrays)
- Each filter iterates through the current list
- For best performance, apply most selective filter first
- Consider preprocessing for very complex filtering logic

## Migration from Preprocessor Approach

You can now choose either approach:

### Option 1: Preprocessor (Recommended for very complex logic)
```java
// Java code flattens the structure
primary = applicants.filter(a -> a.relationship == PRIMARY)
```
```yaml
# Simple YAML mapping
"Primary_FirstName": "primary.demographic.firstName"
```

### Option 2: Filter in YAML (Now Supported)
```yaml
# No preprocessing needed, filter in config
"Primary_FirstName": "applicants[relationship=PRIMARY].demographic.firstName"
```

### Option 3: Hybrid
```java
// Preprocess only very complex logic
primary = applicants.filter(a -> a.relationship == PRIMARY)
```
```yaml
# Use filters for simpler nested arrays
"Primary_Medical_Plan": "primary.proposedProducts[productType=MEDICAL].planName"
```

## Testing the Enhancement

Test with the sample payload above:

```bash
curl -X POST http://localhost:8080/api/pdf/merge \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "enrollment-with-filters.yml",
    "payload": { ... sample payload ... }
  }' \
  --output enrollment.pdf
```

The PDF form fields should be filled correctly using the filter syntax!
