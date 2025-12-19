# Automatic Product Collection Feature

## Overview

The `/api/enrollment/generate` endpoint now **automatically collects products** from all applicants/members in the payload if the `enrollment.products` array is not explicitly provided or is empty.

This eliminates the need for the client to manually aggregate products across all family members.

## How It Works

### Before (Manual Approach)
```json
{
  "enrollment": {
    "products": ["medical", "dental", "vision"],  // Client must calculate this
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "members": [...]
  }
}
```

### After (Automatic Collection)
```json
{
  "enrollment": {
    "marketCategory": "individual",
    "state": "CA"
    // products array omitted - will be auto-collected!
  },
  "payload": {
    "members": [
      {
        "name": "Alice",
        "products": [
          {"type": "medical", "planName": "Gold PPO"}
        ]
      },
      {
        "name": "Bob",
        "products": [
          {"type": "dental", "planName": "Basic Dental"},
          {"type": "vision", "planName": "Vision Plus"}
        ]
      }
    ]
  }
}
```

**Result:** System automatically detects `["dental", "medical", "vision"]` and selects `dental-medical-vision-individual-ca.yml` config.

## Supported Payload Structures

The auto-collection logic examines multiple common payload structures:

### 1. Root-Level Members Array
```json
{
  "enrollment": {...},
  "payload": {
    "members": [
      {
        "products": [
          {"type": "medical", ...},
          {"type": "dental", ...}
        ]
      }
    ]
  }
}
```

### 2. Nested Application Structure
```json
{
  "enrollment": {...},
  "payload": {
    "application": {
      "applicants": [
        {
          "products": [
            {"productType": "MEDICAL", ...}
          ]
        }
      ],
      "proposedProducts": [
        {"productType": "DENTAL", ...}
      ]
    }
  }
}
```

### 3. Enrollment-Level Members
```json
{
  "enrollment": {...},
  "payload": {
    "enrollment": {
      "members": [
        {
          "products": [
            {"type": "vision", ...}
          ]
        }
      ]
    }
  }
}
```

## Product Type Detection

The system recognizes multiple field name variations:
- `type` → "medical"
- `productType` → "MEDICAL"
- `product_type` → "medical"
- `name` → "Medical PPO" (extracts "medical")

All product types are normalized to lowercase: `["dental", "medical", "vision"]`

## Real-World Example

### Scenario: Family with Mixed Product Selections
- **Alice** (PRIMARY): Medical only
- **Bob** (SPOUSE): Dental + Vision
- **Charlie** (DEPENDENT): Medical + Dental

### Request (No Products Specified)
```json
{
  "enrollment": {
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "members": [
      {
        "name": "Alice Johnson",
        "relationship": "PRIMARY",
        "products": [
          {"type": "medical", "planName": "Platinum HMO", "premium": 500.00}
        ]
      },
      {
        "name": "Bob Johnson",
        "relationship": "SPOUSE",
        "products": [
          {"type": "dental", "planName": "Premium Dental", "premium": 60.00},
          {"type": "vision", "planName": "Vision Plus", "premium": 20.00}
        ]
      },
      {
        "name": "Charlie Johnson",
        "relationship": "DEPENDENT",
        "products": [
          {"type": "medical", "planName": "Platinum HMO", "premium": 300.00},
          {"type": "dental", "planName": "Premium Dental", "premium": 40.00}
        ]
      }
    ]
  }
}
```

### System Processing
```
1. Scan Alice's products: medical
2. Scan Bob's products: dental, vision
3. Scan Charlie's products: medical, dental
4. Union of all: [dental, medical, vision] (sorted)
5. Select config: dental-medical-vision-individual-ca.yml
6. Generate PDF with all three product sections
```

### Console Output
```
Auto-collected products from payload: [dental, medical, vision]
Selected config: dental-medical-vision-individual-ca.yml
```

## Explicit vs Auto-Collected

### When Products ARE Provided
```json
{
  "enrollment": {
    "products": ["medical"],  // Explicit list provided
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "members": [...]
  }
}
```
**Result:** Uses explicit `["medical"]` only, ignores payload.

Console: `Using explicitly provided products: [medical]`

### When Products Are Empty/Missing
```json
{
  "enrollment": {
    "products": [],  // Empty or omitted
    "marketCategory": "individual",
    "state": "CA"
  },
  "payload": {
    "members": [...]
  }
}
```
**Result:** Scans payload and auto-collects products.

Console: `Auto-collected products from payload: [dental, medical, vision]`

## Benefits

1. **Simplified Integration**: Clients don't need to write aggregation logic
2. **Single Source of Truth**: Product data lives in member records only
3. **Less Error-Prone**: No risk of mismatch between member products and enrollment products
4. **Flexible**: Still supports explicit products array for override cases

## Testing

### Test Auto-Collection
```bash
# Example request (products omitted)
curl -X POST "http://localhost:8080/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d @test-auto-product-collection.json \
  -o output.pdf

# Check logs
grep "Auto-collected" /tmp/service.log
# Expected: Auto-collected products from payload: [dental, medical, vision]
```

### Test Explicit Override
```bash
# Example request (products specified)
curl -X POST "http://localhost:8080/api/enrollment/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollment": {
      "products": ["medical"],
      "marketCategory": "individual",
      "state": "CA"
    },
    "payload": {...}
  }' \
  -o output.pdf

# Check logs  
grep "Using explicitly" /tmp/service.log
# Expected: Using explicitly provided products: [medical]
```

## Implementation Details

**Location:** `EnrollmentPdfController.enrichEnrollmentWithProducts()`

**Algorithm:**
1. Check if `enrollment.products` is provided and non-empty → use as-is
2. Otherwise, scan payload for members/applicants arrays
3. Extract all product types from nested structures
4. Deduplicate using `Set<String>`
5. Sort alphabetically for consistent config selection
6. Update `enrollment.products` with collected list

**Endpoints Using This Feature:**
- `POST /api/enrollment/generate`
- `POST /api/enrollment/generate-with-rules`

---

**Note:** This feature ensures the **union** of all products across all family members is used for config selection, generating a comprehensive PDF that includes sections for all selected products.
