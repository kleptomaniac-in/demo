# AcroForm Field Mapping Composition - Quick Reference

## YES! Field Mappings Support Full Composition & Override

✅ **Base Level** - `templates/base-payer-acroform.yml`  
✅ **Product Level** - `templates/products/medical-acroform.yml`  
✅ **Market Level** - `templates/markets/individual-acroform.yml`  
✅ **State Level** - `templates/states/california-acroform-fields.yml`  
✅ **Template Level** - Final overrides in composed YAML  

## Quick Example

```yaml
# medical-individual-ca-acroform.yml
composition:
  base: templates/base-payer-acroform.yml
  components:
    - templates/products/medical-acroform.yml
    - templates/markets/individual-acroform.yml
    - templates/states/california-acroform-fields.yml
```

**Result**: All field mappings from all 4 files are merged!

## Field Mapping Levels

| Level | Example Fields | Override Behavior |
|-------|---------------|-------------------|
| **Base** | ApplicationNumber, Primary_FirstName, TotalPremium | Defines common fields |
| **Product** | MedicalPlanName, Primary_SSN (masked) | Adds product fields, can override base |
| **Market** | HouseholdIncome, EmployerName, Dependents | Adds market fields, can override base+product |
| **State** | CA_LicenseNumber, TX_Mandates, EffectiveDate format | Adds compliance fields, can override all |
| **Template** | FormTitle, custom overrides | Highest priority, overrides everything |

## Override Examples

### Override Calculation
```yaml
# Base: Grand total
"TotalPremium": "#{formatCurrency(productSummary.grandTotalPremium)}"

# Market component: Employee portion only
"TotalPremium": "#{formatCurrency(enrollment.employeeContribution)}"
```

### Override Format
```yaml
# Base: MM/dd/yyyy
"EffectiveDate": "#{formatDate(enrollment.effectiveDate, 'MM/dd/yyyy')}"

# State: California requires MMM dd, yyyy
"EffectiveDate": "#{formatDate(enrollment.effectiveDate, 'MMM dd, yyyy')}"
```

### Add Security
```yaml
# Base: Full SSN
"Primary_SSN": "applicants[0].ssn"

# Product: Masked SSN
"Primary_SSN": "#{mask(applicants[0].ssn, 'XXX-XX-', 4)}"
```

## Component Files Created

📁 **Base**
- `templates/base-payer-acroform.yml` - 20+ common fields

📁 **Products**
- `templates/products/medical-acroform.yml` - 10+ medical fields
- `templates/products/dental-acroform.yml` - 8+ dental fields

📁 **Markets**
- `templates/markets/individual-acroform.yml` - 15+ individual fields
- `templates/markets/small-group-acroform.yml` - 12+ employer fields

📁 **States**
- `templates/states/california-acroform-fields.yml` - 10+ CA compliance
- `templates/states/texas-acroform-fields.yml` - 8+ TX compliance

📁 **Examples**
- `medical-individual-ca-acroform.yml` - Complete example
- `dental-small-group-tx-acroform.yml` - Complete example

## How It Works

1. **Load base config** → Get common 20 fields
2. **Merge product component** → Add 10 fields, override 1 = 29 fields total
3. **Merge market component** → Add 15 fields, override 1 = 43 fields total
4. **Merge state component** → Add 10 fields, override 2 = 51 fields total
5. **Apply template overrides** → Override 2 = 51 fields total (2 changed)

**Result**: 51 field mappings from 5 files, with intelligent overrides!

## Testing

```bash
# Generate PDF with composed field mappings
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-medical-individual-ca-request.json \
  --output result.pdf

# Check which fields were filled
tail -f /tmp/service-output.log | grep "Filled field"
```

## Benefits

- 🔧 **Maintainable**: Update CA fields once, affects all CA forms
- 🔄 **Reusable**: Same base for 600+ form variations
- 🎯 **Flexible**: Override any field at any level
- ✅ **Tested**: Deep merge algorithm proven in production
- 🚀 **Fast**: Composition happens once at config load

## See Also

- `ACROFORM_FIELD_MAPPING_COMPOSITION.md` - Full documentation
- `COMPOSITION_GUIDE.md` - General composition system
- `ACROFORM_MAPPING_GUIDE.md` - Field mapping syntax reference

---

**Answer**: YES! Field mappings support full composition hierarchy with override capability at base, product, market, state, and template levels. ✅
