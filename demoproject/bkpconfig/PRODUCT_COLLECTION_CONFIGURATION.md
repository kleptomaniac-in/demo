# Configurable Product Collection Paths - Implementation Summary

## Overview

Implemented **Option 1: YAML config-based** product collection paths, allowing each enrollment configuration to specify custom paths for extracting product information from payloads.

---

## What Changed

### 1. **PdfMergeConfig.java** - Added Configuration Fields

```java
// Product collection configuration
private List<String> productCollectionPaths;
private List<String> defaultProducts;
```

**Purpose**: Store configurable paths and fallback defaults in YAML configurations.

---

### 2. **PayloadPathExtractor.java** - New Utility Class

Created: `/src/main/java/com/example/util/PayloadPathExtractor.java`

**Features**:
- Parses path expressions like `"members[].products[].type"`
- Supports nested arrays: `"application.applicants[].products[].productType"`
- Normalizes product names (e.g., "Medical PPO" → "medical")
- Deduplicates and sorts results
- Handles fuzzy matching for product type names

**Key Methods**:
```java
List<String> extractValues(Map<String, Object> payload, String path)
List<String> extractUniqueValues(Map<String, Object> payload, List<String> paths)
List<String> extractProductTypes(Map<String, Object> payload, List<String> paths)
```

---

### 3. **EnrollmentPdfController.java** - Updated to Use Config Paths

**Changes**:
1. Added imports for `PdfMergeConfigService`, `PdfMergeConfig`, `PayloadPathExtractor`
2. Added autowired fields for `configService` and `enrollmentPdfService`
3. Updated `/api/enrollment/generate` endpoint:
   - Load config **before** product collection
   - Pass config to `enrichEnrollmentWithProducts()`
   - Re-select config if products change the selection
4. Replaced hardcoded product collection logic with `PayloadPathExtractor`
5. Added fallback to default paths if config doesn't specify custom ones

**New Methods**:
```java
collectProductsFromPayload(payload, config)  // Uses config paths
getDefaultProductCollectionPaths()           // Backward compatibility
```

---

### 4. **dental-individual-ca.yml** - Example Configuration

```yaml
# Product collection configuration
productCollectionPaths:
  - "applicants[].coverages[].productType"      # Primary path
  - "members[].products[].type"                 # Fallback #1
  - "application.applicants[].products[].type"  # Fallback #2

# Default products if payload doesn't contain any
defaultProducts:
  - "dental"

addendums:
  dependents:
    enabled: true
    maxInMainForm: 3
  coverages:
    enabled: true
    maxPerApplicant: 1
```

---

### 5. **PayloadPathExtractorTest.java** - Comprehensive Tests

Created: `/src/test/java/com/example/util/PayloadPathExtractorTest.java`

**Test Coverage** (12 tests, 0 failures):
- ✅ Simple path extraction
- ✅ Nested object paths
- ✅ Array iteration
- ✅ Nested array paths (e.g., `members[].products[].type`)
- ✅ Unique value deduplication
- ✅ Multiple path fallbacks
- ✅ Direct product type matching
- ✅ Fuzzy product name matching ("Medical PPO" → "medical")
- ✅ Null/empty path handling
- ✅ Non-existent path handling
- ✅ Complex nested enrollment structures
- ✅ Empty payload fallback

---

## How It Works

### **Request Flow**

```
1. POST /api/enrollment/generate
        ↓
2. Load preliminary config (dental-individual-ca.yml)
        ↓
3. Read productCollectionPaths from config
        ↓
4. Extract products using PayloadPathExtractor
   - Try custom paths from config
   - Fallback to default paths if no custom paths
        ↓
5. Enrich enrollment.products with collected values
        ↓
6. Re-select config (may change based on products)
        ↓
7. Generate PDF with correct config
```

---

## Example Usage Scenarios

### **Scenario 1: Custom Payload Structure (ACME Client)**

**Config**: `acme-enrollment-ca.yml`
```yaml
productCollectionPaths:
  - "applicants[].selectedCoverages[].coverageType"
  - "proposedPlans[].planType"

defaultProducts: ["medical"]
```

**Request**:
```json
{
  "enrollment": {"marketCategory": "individual", "state": "CA"},
  "payload": {
    "applicants": [{
      "selectedCoverages": [
        {"coverageType": "DENTAL"},
        {"coverageType": "VISION"}
      ]
    }]
  }
}
```

**Result**: Extracts `["dental", "vision"]` from custom path → Selects `dental-vision-individual-ca.yml`

---

### **Scenario 2: Standard Structure (Uses Defaults)**

**Config**: `dental-individual-ca.yml` (no custom paths)

**Request**:
```json
{
  "enrollment": {"marketCategory": "individual", "state": "CA"},
  "payload": {
    "members": [{
      "products": [{"type": "DENTAL"}]
    }]
  }
}
```

**Result**: Uses default paths → Finds `["dental"]` → Selects `dental-individual-ca.yml`

---

### **Scenario 3: Payload with No Products (Fallback to Defaults)**

**Config**: `dental-individual-ca.yml`
```yaml
defaultProducts:
  - "dental"
```

**Request**:
```json
{
  "enrollment": {"marketCategory": "individual", "state": "CA"},
  "payload": {
    "applicants": [{"name": "John Doe"}]
  }
}
```

**Result**: No products found → Uses `defaultProducts: ["dental"]` → Selects `dental-individual-ca.yml`

---

## Benefits

| Benefit | Description |
|---------|-------------|
| **Client Flexibility** | Each client can have different payload structures |
| **No Code Changes** | Add new payload structures via YAML configuration |
| **Clear Documentation** | Each config file documents its expected payload structure |
| **Backward Compatible** | Falls back to default paths if no custom paths specified |
| **Testable** | Comprehensive test coverage for path extraction logic |
| **Maintainable** | Centralized path configuration in YAML files |

---

## Configuration Options

### **Per-Config Settings** (in YAML files)

```yaml
# Custom paths to search for products
productCollectionPaths:
  - "path.to.products[].field"
  - "fallback.path[].products[].type"

# Fallback if no products found in payload
defaultProducts:
  - "product1"
  - "product2"
```

### **Default Paths** (Hardcoded Fallbacks)

If config doesn't specify `productCollectionPaths`, these defaults are used:
```java
"members[].products[].type"
"application.applicants[].products[].productType"
"application.proposedProducts[].type"
"enrollment.members[].products[].type"
"applicants[].coverages[].productType"
"members[].products[].productType"
"members[].products[].name"
```

---

## Path Syntax

**Supported patterns**:
- `field` → Navigate to child object
- `.` → Object property accessor
- `[]` → Array iterator
- `field[]` → Iterate over array
- `field1.field2[].field3` → Nested combination

**Examples**:
- `"productType"` → Extract from root level
- `"enrollment.state"` → Navigate to nested object
- `"members[].name"` → Iterate array, extract field
- `"members[].products[].type"` → Nested array iteration

---

## Testing

**Run tests**:
```bash
mvn test -Dtest=PayloadPathExtractorTest
```

**Results**:
```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Migration Guide

### **For Existing Configs**

No action needed! Existing configs without `productCollectionPaths` will use default paths (backward compatible).

### **For New Client-Specific Configs**

1. Create new YAML config (e.g., `client-enrollment-ca.yml`)
2. Add `productCollectionPaths` section:
   ```yaml
   productCollectionPaths:
     - "your.custom.path[].products[].type"
   ```
3. Optionally add `defaultProducts` as fallback
4. Test with client's actual payload structure

---

## Summary

✅ **Configuration-driven** product collection  
✅ **No code changes** needed for new payload structures  
✅ **Flexible paths** support complex nested structures  
✅ **Backward compatible** with existing configurations  
✅ **Fully tested** with 12 comprehensive tests  
✅ **Production ready** with proper error handling and fallbacks

The implementation successfully achieves **Option 1: YAML config-based** approach, providing maximum flexibility while maintaining simplicity and backward compatibility! 🚀
