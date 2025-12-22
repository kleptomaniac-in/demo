# Granular Component Quick Reference

## YES! Further Splitting Makes Perfect Sense ✅

### What Was Created

**6 Granular Components:**
1. `components/company-info.yml` - Company branding (5 fields)
2. `components/application-metadata.yml` - Application tracking (6 fields)  
3. `components/primary-applicant.yml` - Primary applicant info (11 fields)
4. `components/spouse.yml` - Spouse info (7 fields) **OPTIONAL**
5. `components/dependents.yml` - Dependent children (20 fields) **OPTIONAL**
6. `components/premium-payment.yml` - Financial info (6 fields)

**New Composed Base:**
- `base-payer-acroform-v2.yml` - Composes from above components

### Key Benefits

| Benefit | Example |
|---------|---------|
| **Selective Inclusion** | Include spouse.yml only for family plans |
| **Better Organization** | All spouse fields in ONE file |
| **Easier Maintenance** | Update spouse fields in one place → affects all forms |
| **Team Collaboration** | Dev A works on spouse.yml, Dev B on dependents.yml (no conflicts!) |
| **Clearer Intent** | Composition shows exactly what form includes/excludes |

### Usage Examples

#### Individual with Spouse Only (No Kids)
```yaml
composition:
  base: base-payer-acroform-v2.yml
  components:
    - components/spouse.yml          ✓ Include
    # NO dependents.yml               ✗ Skip kids
```
**Result:** 28 base + 7 spouse = 35 fields (saved 20 dependent fields)

#### Medicare Individual (Single Coverage)
```yaml
composition:
  base: base-payer-acroform-v2.yml
  # NO spouse.yml                     ✗ Skip spouse
  # NO dependents.yml                 ✗ Skip kids
```
**Result:** 28 fields only (saved 27 unnecessary family fields!)

#### Full Family Enrollment
```yaml
composition:
  base: base-payer-acroform-v2.yml
  components:
    - components/spouse.yml          ✓ Include
    - components/dependents.yml      ✓ Include
```
**Result:** 28 + 7 + 20 = 55 fields (complete family coverage)

#### Employee-Only Group Plan
```yaml
composition:
  base: base-payer-acroform-v2.yml
  components:
    - markets/small-group-acroform-v2.yml
  # NO spouse or dependents (handled separately in group enrollment)
```
**Result:** 28 + 9 = 37 fields (clean employee-only form)

### Component Reusability

```
primary-applicant.yml → Used by ALL forms (100%)
spouse.yml → Used by Individual/Family, COBRA (40%)
dependents.yml → Used by Individual/Family (40%)
company-info.yml → Used by ALL forms (100%)
```

### Architecture Pattern

```
Old: Monolithic base (55 fields always included)
     ❌ Spouse/dependent fields on every form, even if not needed

New: Composed base (28 core fields)
     ✅ Add spouse/dependents only when needed
     ✅ Smaller, focused components
     ✅ Single responsibility per file
```

### Maintenance Example

**Task:** Change spouse email to lowercase

**Before (Monolithic):**
1. Open 55-field monolithic file
2. Search through mixed fields
3. Find spouse email among clutter
4. Edit and save

**After (Granular):**
1. Open `spouse.yml` (only 7 fields!)
2. All spouse fields visible immediately
3. Edit `Spouse_Email` field
4. Save → Automatically affects all forms using spouse.yml

### Files Structure

```
templates/
├── components/                   ◄── NEW directory
│   ├── company-info.yml         (Company branding)
│   ├── application-metadata.yml (Application tracking)
│   ├── primary-applicant.yml    (Primary fields - CORE)
│   ├── spouse.yml               (Spouse fields - OPTIONAL)
│   ├── dependents.yml           (Kids fields - OPTIONAL)
│   └── premium-payment.yml      (Financial fields - CORE)
│
├── base-payer-acroform.yml      (Old monolithic - still works)
└── base-payer-acroform-v2.yml   (New composed from components)
```

### Migration Path

**Backward Compatible:**
- Old `base-payer-acroform.yml` still works
- Gradually migrate to v2 as needed
- No breaking changes

**Progressive Enhancement:**
```yaml
# Start with composed base
composition:
  base: base-payer-acroform-v2.yml

# Add components as needed
composition:
  base: base-payer-acroform-v2.yml
  components:
    - components/spouse.yml

# Full composition
composition:
  base: base-payer-acroform-v2.yml
  components:
    - components/spouse.yml
    - components/dependents.yml
    - products/medical-acroform.yml
    - states/california-acroform-fields.yml
```

## Summary

✅ **Granular components = Better architecture**  
✅ **Selective inclusion based on form needs**  
✅ **Easier maintenance (update once, affects all)**  
✅ **Better organization (single responsibility)**  
✅ **Team-friendly (no merge conflicts)**  
✅ **Proven pattern (React, microservices, atomic design)**  
✅ **Near-zero performance overhead**  

## Recommendation

**YES - Further split base into granular components!**

The benefits (selective inclusion, maintainability, clarity) far outweigh the minor overhead of managing a few extra files. This is a best practice pattern proven in modern software architecture.

**Core components** (always needed):
- company-info.yml
- application-metadata.yml  
- primary-applicant.yml
- premium-payment.yml

**Optional components** (include as needed):
- spouse.yml (only for family/spouse coverage)
- dependents.yml (only for dependent coverage)

See full documentation: `COMPONENT_GRANULARITY_GUIDE.md`
