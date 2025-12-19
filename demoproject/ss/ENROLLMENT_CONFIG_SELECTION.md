# Enrollment-Based Config Selection Guide

## Problem Statement

For healthcare enrollment submissions where:
- Members can select **multiple products** (Medical, Dental, Vision)
- Each product can have **multiple plans**
- Need to generate **one PDF** showing all selected products

**How do we automatically select the right template configuration?**

## Solution: Config Selection Service

The `ConfigSelectionService` automatically determines which template to use based on enrollment parameters.

## Selection Strategies

### 1. Convention-Based (Recommended)

Builds config filename from enrollment parameters using alphabetically sorted products.

**Example:**
```
Products: [medical, dental]
Market: individual
State: CA

→ Config: dental-medical-individual-ca.yml
```

**Naming Convention:**
```
{sorted-products}-{market}-{state}.yml
```

### 2. Dynamic Composition

Generates composition structure at runtime based on selected products.

**Example:**
```yaml
# Generated dynamically
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california.yml
```

### 3. Rule-Based Selection

Uses business rules for special cases.

**Examples:**
- Medicare members → Medicare-specific templates
- Large groups (>1000) → Enterprise templates  
- Specific product combinations → Custom templates

## API Endpoints

### Preview Config Selection
See which config would be selected without generating PDF:

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
  "ruleBasedConfig": "dental-medical-individual-ca.yml",
  "dynamicComposition": {
    "composition": {
      "base": "templates/base-payer.yml",
      "components": [
        "templates/products/dental.yml",
        "templates/products/medical.yml",
        "templates/markets/individual.yml",
        "templates/states/california.yml"
      ]
    }
  },
  "enrollmentSummary": "Products: medical, dental, Market: individual, State: CA"
}
```

### Generate Enrollment PDF
Generate PDF with automatic config selection:

**Endpoint:** `POST /api/enrollment/generate`

**Request:**
```json
{
  "enrollment": {
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA",
    "plansByProduct": {
      "medical": ["GOLD_PPO"],
      "dental": ["PREMIUM_DENTAL"]
    }
  },
  "outputFileName": "enrollment.pdf",
  "payload": {
    "companyName": "HealthCorp",
    "enrollmentDate": "2025-12-12",
    "members": [
      {
        "memberId": "E12345",
        "name": "Jane Doe",
        "medical": {
          "planName": "Gold PPO",
          "premium": 450.00
        },
        "dental": {
          "planName": "Premium Dental",
          "premium": 45.00
        }
      }
    ]
  }
}
```

## Real-World Examples

### Example 1: Medical Only
```json
{
  "products": ["medical"],
  "marketCategory": "individual",
  "state": "CA"
}
→ medical-individual-ca.yml
```

### Example 2: Medical + Dental
```json
{
  "products": ["medical", "dental"],
  "marketCategory": "individual",
  "state": "CA"
}
→ dental-medical-individual-ca.yml
(alphabetically sorted: dental before medical)
```

### Example 3: Full Benefits (Medical + Dental + Vision)
```json
{
  "products": ["medical", "dental", "vision"],
  "marketCategory": "small-group",
  "state": "TX"
}
→ dental-medical-vision-small-group-tx.yml
```

### Example 4: Medicare
```json
{
  "products": ["medical"],
  "marketCategory": "medicare",
  "state": "CA"
}
→ medicare-advantage-ca.yml (via rule-based selection)
```

## Config File Structure

For multi-product enrollments, the config file includes sections for ALL selected products:

```yaml
# dental-medical-individual-ca.yml
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml    # Dental sections
    - templates/products/medical.yml   # Medical sections
    - templates/markets/individual.yml
    - templates/states/california.yml

# Resulting PDF will have:
# 1. Cover page
# 2. Member summary
# 3. Dental coverage details
# 4. Dental providers
# 5. Medical coverage details
# 6. Provider network
# 7. Prescription coverage
# 8. Individual mandate
# 9. CA disclosures
```

## Product Combinations Matrix

| Products Selected | Config File Pattern | Sections Included |
|-------------------|---------------------|-------------------|
| Medical | `medical-{market}-{state}.yml` | Medical only |
| Dental | `dental-{market}-{state}.yml` | Dental only |
| Vision | `vision-{market}-{state}.yml` | Vision only |
| Medical + Dental | `dental-medical-{market}-{state}.yml` | Both products |
| Medical + Vision | `medical-vision-{market}-{state}.yml` | Both products |
| Dental + Vision | `dental-vision-{market}-{state}.yml` | Both products |
| All Three | `dental-medical-vision-{market}-{state}.yml` | All products |

## Plans vs Products

**Important:** The config selection is based on **products**, not individual plans.

- **Products**: Medical, Dental, Vision (high-level categories)
- **Plans**: Gold PPO, Silver HMO, Premium Dental, etc. (specific plan choices)

**Selection Logic:**
1. ✅ Use products to select template config
2. ✅ Pass plan details in payload for template rendering
3. ❌ Don't create separate configs for each plan

**Example:**
```json
{
  "products": ["medical", "dental"],  // ← Used for config selection
  "plansByProduct": {
    "medical": ["GOLD_PPO", "SILVER_HMO"],  // ← Used in template
    "dental": ["PREMIUM_DENTAL"]             // ← Used in template
  }
}
→ dental-medical-{market}-{state}.yml
```

The template will iterate through plans in the payload and render all selected plans.

## File Management Strategy

### Pre-create Common Combinations
For frequently used combinations, pre-create config files:

```
dental-medical-individual-ca.yml     ✅ Pre-created
dental-medical-small-group-ca.yml    ✅ Pre-created
dental-medical-vision-individual-ca.yml ✅ Pre-created
```

### Dynamic Composition for Rare Combinations
For unusual combinations, use dynamic composition at runtime:

```
medical-vision-medicare-wy.yml       ⚠️ Rare, use dynamic composition
```

### Fallback Strategy
If exact config doesn't exist:
1. Try dynamic composition
2. Fall back to generic product-specific template
3. Return error if no suitable template found

## Testing

### Test Config Selection
```bash
curl -X POST http://localhost:8080/api/enrollment/preview-config \
  -H "Content-Type: application/json" \
  -d '{
    "products": ["medical", "dental"],
    "marketCategory": "individual",
    "state": "CA"
  }'
```

### Test PDF Generation
```bash
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d @test-enrollment-request.json \
  -o enrollment.pdf
```

## Benefits

### 1. Automatic Selection
No manual config file selection - system chooses based on enrollment data.

### 2. Consistency
Alphabetical sorting ensures same products → same config, regardless of order.

### 3. Scalability
- 1 config per product combination (not per plan combination)
- 3 products × 4 markets × 50 states = ~600 configs (manageable)
- With composition: ~70 component files generate all 600 combinations

### 4. Flexibility
- Convention-based for standard cases
- Rule-based for exceptions
- Dynamic composition for rare combinations

## Migration Path

### Phase 1: Core Products (Current)
Create configs for most common combinations:
- Medical-only (all markets/states)
- Dental-only (all markets/states)
- Medical + Dental (top 10 markets/states)

### Phase 2: Multi-Product
Add configs for:
- Medical + Vision
- Dental + Vision  
- Medical + Dental + Vision

### Phase 3: Dynamic Fallback
Implement dynamic composition for:
- Rare product combinations
- New states
- Special market categories

## Summary

**Config Selection Flow:**
```
Enrollment Request
    ↓
Extract: products, market, state
    ↓
Sort products alphabetically
    ↓
Build config name: {products}-{market}-{state}.yml
    ↓
Load config (or compose dynamically)
    ↓
Generate PDF with all product sections
```

**Key Principle:** One config per product combination, not per plan combination.

**Result:** Automatic, consistent, scalable config selection for enrollment PDFs.
