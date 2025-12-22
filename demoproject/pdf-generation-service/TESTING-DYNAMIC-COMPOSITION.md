# Dynamic Composition Fallback - Testing Guide

## Overview

This document describes how to test the dynamic composition fallback feature, which allows the system to automatically generate PDF configurations when pre-generated YAML files don't exist.

## Test Strategy

The system supports two config loading strategies:
1. **Pre-generated configs** (fast, cached) - for common combinations
2. **Dynamic composition** (flexible, automatic) - for rare combinations

## Automated Tests

### Unit Tests (No Spring Context)

**File**: `src/test/java/com/example/service/DynamicCompositionUnitTest.java`

Tests core logic without Spring:
- ✅ Config naming convention
- ✅ Alphabetical product sorting
- ✅ Case normalization
- ✅ Component list building
- ✅ Edge case handling

**Run**:
```bash
mvn test -Dtest=DynamicCompositionUnitTest
```

### Integration Tests (Full Spring Context)

**File**: `src/test/java/com/example/service/DynamicCompositionTest.java`

Tests with full application context:
- ✅ Load pre-generated config when file exists
- ✅ Use dynamic composition when file doesn't exist
- ✅ Build correct dynamic composition structure
- ✅ Sort products alphabetically
- ✅ Handle single/multiple products
- ✅ Support all market categories
- ✅ Handle case-insensitive names
- ✅ Fail gracefully on missing components
- ✅ Full flow integration test

**Run**:
```bash
mvn test -Dtest=DynamicCompositionTest
```

## Manual Testing

### Prerequisites

1. Start the PDF generation service:
```bash
cd /workspaces/demo/demoproject/pdf-generation-service
mvn spring-boot:run
```

2. Wait for startup (cache warming will preload common configs)

### Test Script

**File**: `test-dynamic-composition.sh`

Automated script that tests 4 scenarios:
1. Pre-generated config (dental-individual-ca)
2. Rare combination requiring dynamic (vision-life-medicare-WY)
3. Multi-product rare combination (dental-vision-large-group-MT)
4. Common combination (medical-individual-ca)

**Run**:
```bash
./test-dynamic-composition.sh
```

**Expected Output**:
```
Test 1: ✓ PDF generated using pre-generated config
Test 2: ✓ PDF generated using dynamic composition
Test 3: ✓ PDF generated using dynamic composition
Test 4: ✓ PDF generated using pre-generated config
```

### Individual curl Tests

#### Test 1: Pre-generated Config (Should Exist)
```bash
curl -X POST "http://localhost:8080/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["dental"],
      "marketCategory": "individual",
      "state": "CA"
    }
  }' \
  --output dental-individual-ca.pdf
```

**Expected Server Log**:
```
✓ Loaded pre-generated config: dental-individual-ca.yml
```

#### Test 2: Dynamic Composition (Rare Combination)
```bash
curl -X POST "http://localhost:8080/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["vision", "life"],
      "marketCategory": "medicare",
      "state": "WY"
    }
  }' \
  --output dynamic-composition.pdf
```

**Expected Server Log**:
```
✗ Config file not found: life-vision-medicare-wy.yml
→ Falling back to dynamic composition...
Building dynamic composition from enrollment:
  Base: templates/base-payer.yml
  Products (sorted): [life, vision]
  Market: medicare
  State: WY
✓ Dynamic composition completed successfully
```

#### Test 3: Multi-Product Dynamic
```bash
curl -X POST "http://localhost:8080/api/pdf/generate-enrollment" \
  -H "Content-Type: application/json" \
  -d '{
    "enrollmentData": {
      "products": ["medical", "dental", "vision"],
      "marketCategory": "small-group",
      "state": "AK"
    }
  }' \
  --output multi-product-dynamic.pdf
```

**Expected Server Log**:
```
✗ Config file not found: dental-medical-vision-small-group-ak.yml
→ Falling back to dynamic composition...
Building dynamic composition from enrollment:
  Base: templates/base-payer.yml
  Products (sorted): [dental, medical, vision]
  Market: small-group
  State: AK
✓ Dynamic composition completed successfully
```

## Verification Checklist

### Functional Tests
- [ ] Pre-generated config loads successfully
- [ ] Dynamic composition triggers when file not found
- [ ] Products are sorted alphabetically in dynamic mode
- [ ] Market category component is included
- [ ] State component is included
- [ ] PDF is generated correctly in both modes
- [ ] No errors in server logs

### Performance Tests
- [ ] Pre-generated configs load from cache (fast)
- [ ] Dynamic composition completes in reasonable time
- [ ] Component files are cached after first load
- [ ] Multiple requests for same rare combo use cache

### Edge Case Tests
- [ ] Single product enrollment works
- [ ] Multiple products (3+) work correctly
- [ ] Case-insensitive product names handled
- [ ] Invalid product names fail gracefully
- [ ] Missing component files fail with clear error

## Understanding Test Results

### Success Indicators

**Pre-generated Config**:
```
✓ Loaded pre-generated config: <config-name>.yml
Sections: 15
```

**Dynamic Composition**:
```
✗ Config file not found: <config-name>.yml
→ Falling back to dynamic composition...
Building dynamic composition from enrollment:
  Base: templates/base-payer.yml
  Products (sorted): [dental, medical, vision]
  Market: individual
  State: CA
  Components: 5
✓ Dynamic composition completed successfully
Sections: 15
```

### Failure Indicators

**Missing Component File**:
```
✗ Config file not found: <config-name>.yml
→ Falling back to dynamic composition...
ERROR: Failed to load component: templates/products/invalid-product.yml
```

**Invalid Enrollment Data**:
```
ERROR: Products list cannot be empty
ERROR: Market category is required
ERROR: State is required
```

## Comparing Pre-generated vs Dynamic

### Pre-generated Config
- ✅ Faster (loaded from cache)
- ✅ Pre-validated structure
- ❌ Requires manual creation for each combination
- 📊 Use for: Common combinations (80% of traffic)

### Dynamic Composition
- ✅ Automatic (no manual YAML creation)
- ✅ Handles any valid combination
- ❌ Slightly slower (first request per combination)
- ✅ Cached after first load
- 📊 Use for: Rare combinations (20% of traffic)

## Production Testing

### Before Deployment
1. Run all automated tests
2. Test top 20 product/market/state combinations manually
3. Test at least 5 rare combinations requiring dynamic composition
4. Monitor cache hit rates
5. Check performance metrics

### After Deployment
1. Monitor server logs for fallback frequency
2. Track dynamic composition usage (should be ~20%)
3. Identify frequently used dynamic combinations
4. Create pre-generated configs for popular dynamic combos
5. Monitor PDF generation latency

## Metrics to Track

```properties
# Add to application.yml for monitoring
management.metrics.enable.pdf.config.loading=true

Metrics:
- pdf.config.pregenerated.count - Pre-generated configs loaded
- pdf.config.dynamic.count - Dynamic compositions created
- pdf.config.cache.hit.rate - Cache effectiveness
- pdf.generation.latency - End-to-end performance
```

## Troubleshooting

### Dynamic Composition Not Triggering
**Symptom**: Always uses pre-generated configs
**Solution**: Verify config file doesn't exist in config-repo/

### Component Files Not Found
**Symptom**: "Failed to load component" error
**Solution**: Ensure component files exist:
- `templates/products/<product>.yml`
- `templates/markets/<market>.yml`
- `templates/states/<state>.yml`

### Products Not Sorted
**Symptom**: Config name has products in wrong order
**Solution**: Check ConfigSelectionService.selectConfigByConvention()

### Cache Not Working
**Symptom**: Slow repeated requests for same combo
**Solution**: Check cache configuration in application.yml

## Next Steps

1. Create base template: `templates/base-payer.yml`
2. Create product components: `templates/products/*.yml`
3. Create market components: `templates/markets/*.yml`
4. Create state components: `templates/states/*.yml`
5. Run test suite to validate
6. Monitor production usage patterns
7. Create pre-generated configs for frequently used dynamic combos
