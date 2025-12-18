# Field Transformation Functions - Quick Reference

## Syntax
```yaml
fieldMappings:
  FieldName: "#{functionName(arg1, arg2, ...)}"
```

## String Operations
| Function | Example | Result |
|----------|---------|--------|
| `concat` | `#{concat(firstName, ' ', lastName)}` | `"John Doe"` |
| `uppercase` | `#{uppercase(email)}` | `"JOHN@EXAMPLE.COM"` |
| `lowercase` | `#{lowercase(email)}` | `"john@example.com"` |
| `capitalize` | `#{capitalize(name)}` | `"John Doe"` |
| `substring` | `#{substring(text, 0, 5)}` | `"Hello"` |
| `replace` | `#{replace(phone, '-', '')}` | `"5551234567"` |
| `trim` | `#{trim(name)}` | `"John"` (no spaces) |

## Masking & Security
| Function | Example | Result |
|----------|---------|--------|
| `mask` | `#{mask(ssn, 'XXX-XX-', 4)}` | `"XXX-XX-6789"` |
| `maskEmail` | `#{maskEmail(email)}` | `"j***@example.com"` |
| `maskPhone` | `#{maskPhone(phone)}` | `"XXX-XXX-4567"` |

## Date & Number Formatting
| Function | Example | Result |
|----------|---------|--------|
| `formatDate` | `#{formatDate(dob, 'MMM dd, yyyy')}` | `"Jan 15, 2026"` |
| `parseDate` | `#{parseDate(date, 'MM/dd/yyyy')}` | `"2026-01-15"` |
| `formatNumber` | `#{formatNumber(amount, 2)}` | `"1,234.50"` |
| `formatCurrency` | `#{formatCurrency(premium)}` | `"$450.00"` |

## Conditional Functions
| Function | Example | Result |
|----------|---------|--------|
| `coalesce` | `#{coalesce(email1, email2, 'N/A')}` | First non-empty |
| `default` | `#{default(middleName, 'N/A')}` | Value or default |
| `ifEmpty` | `#{ifEmpty(nickname, firstName)}` | Fallback value |

## Common Patterns

### Name Formatting
```yaml
# Full name uppercase
"FullNameUpper": "#{uppercase(#{concat(firstName, ' ', lastName)})}"

# Capitalized full name with middle initial
"DisplayName": "#{capitalize(#{concat(firstName, ' ', #{substring(middleName, 0, 1)}, '. ', lastName)})}"

# Last name, first name
"SortName": "#{concat(#{uppercase(lastName)}, ', ', #{capitalize(firstName)})}"
```

### Contact Information
```yaml
# Masked email
"Email": "#{maskEmail(personalEmail)}"

# Cleaned phone (digits only)
"PhoneDigits": "#{replace(phone, '-', '')}"

# Formatted phone
"PhoneDisplay": "#{concat('(', #{substring(phone, 0, 3)}, ') ', #{substring(phone, 3, 6)}, '-', #{substring(phone, 6)})}"

# Preferred contact (with fallback)
"Contact": "#{coalesce(mobilePhone, homePhone, email)}"
```

### Security & Privacy
```yaml
# Masked SSN
"SSN": "#{mask(ssn, 'XXX-XX-', 4)}"

# Masked credit card
"CreditCard": "#{mask(cardNumber, '****-****-****-', 4)}"

# Partial account number
"Account": "#{concat('Account ending in ', #{substring(accountNum, -4)})}"
```

### Dates
```yaml
# Long format
"DOB_Long": "#{formatDate(dateOfBirth, 'MMMM dd, yyyy')}"

# Short format
"DOB_Short": "#{formatDate(dateOfBirth, 'MM/dd/yy')}"

# ISO format
"DOB_ISO": "#{parseDate(dateOfBirth, 'MM/dd/yyyy')}"
```

### Currency & Numbers
```yaml
# Currency
"Premium": "#{formatCurrency(monthlyPremium)}"

# Number with decimals
"Rate": "#{formatNumber(interestRate, 3)}"

# Total calculation display
"Summary": "#{concat('Medical: ', #{formatCurrency(medicalPremium)}, ' + Dental: ', #{formatCurrency(dentalPremium)}, ' = Total: ', #{formatCurrency(totalPremium)})}"
```

### With Array Access
```yaml
# Primary applicant
"Primary_Name": "#{uppercase(#{concat(applicants[0].firstName, ' ', applicants[0].lastName)})}"

# Filtered array
"Primary_Email": "#{lowercase(applicants[relationship=PRIMARY].email)}"

# From enriched data
"Primary_Age": "#{concat(coverageSummary.enrichedApplicants[0].calculatedAge, ' years old')}"
```

### Nested Functions (Advanced)
```yaml
# Multiple transformations
"ProcessedEmail": "#{lowercase(#{trim(email)})}"

# Complex formatting
"FullAddress": "#{concat(#{capitalize(street)}, ', ', #{capitalize(city)}, ', ', #{uppercase(state)}, ' ', zip)}"

# Conditional with formatting
"Display": "#{capitalize(#{ifEmpty(nickname, firstName)})}"
```

## Test with CURL

```bash
curl -X POST http://localhost:8080/api/pdf/merge \
  -H "Content-Type: application/json" \
  -d @test-enrollment-functions-request.json \
  --output enrollment-functions-demo.pdf
```

## Test Payloads

### Minimal Test
```json
{
  "firstName": "john",
  "lastName": "doe",
  "email": "JOHN@EXAMPLE.COM",
  "ssn": "123-45-6789"
}
```

### Expected Transformations
- concat → "john doe"
- uppercase(email) → "JOHN@EXAMPLE.COM"
- lowercase(email) → "john@example.com"
- mask(ssn) → "XXX-XX-6789"

## Function Composition

Functions can be nested:

```yaml
# Level 1: Single function
"Email": "#{lowercase(email)}"

# Level 2: Nested functions
"NameUpper": "#{uppercase(#{concat(firstName, lastName)})}"

# Level 3: Multiple nesting
"Formatted": "#{capitalize(#{trim(#{lowercase(fullName)})})}"
```

**Best Practice:** Keep nesting to 2-3 levels maximum for readability.

## Error Handling

Functions gracefully handle:
- ✅ Null values → Returns empty string
- ✅ Missing fields → Returns empty string
- ✅ Invalid format → Returns original value
- ✅ Type mismatch → Automatic conversion

```yaml
# Safe even if middleName is null
"FullName": "#{concat(firstName, ' ', middleName, ' ', lastName)}"
# Result: "John  Doe" (extra space, but no error)

# Use default to handle nulls
"FullName": "#{concat(firstName, ' ', #{default(middleName, '')}, ' ', lastName)}"
# Result: "John Doe" (clean)
```

## Performance Tips

1. **Use enrichers for expensive operations**
   - ✅ Age calculation → Use enricher
   - ✅ Premium aggregation → Use enricher
   - ✅ Simple formatting → Use function

2. **Avoid repeated calculations**
   ```yaml
   # ❌ Bad - calculates multiple times
   "Field1": "#{uppercase(#{concat(firstName, lastName)})}"
   "Field2": "#{uppercase(#{concat(firstName, lastName)})}"
   
   # ✅ Good - calculate once in enricher, use result
   "Field1": "enrichedData.fullNameUpper"
   "Field2": "enrichedData.fullNameUpper"
   ```

3. **Keep functions simple**
   - Prefer 1-2 arguments over 5+
   - Use meaningful variable names in payload
   - Document complex transformations

## Quick Debugging

### 1. Test function individually
```yaml
# Start simple
"Test": "#{uppercase(firstName)}"

# Then add complexity
"Test": "#{uppercase(#{concat(firstName, lastName)})}"
```

### 2. Check function name spelling
```yaml
# ✅ Correct
"Email": "#{lowercase(email)}"

# ❌ Wrong
"Email": "#{lowerCase(email)}"  # Case doesn't matter, but typos do
```

### 3. Verify argument count
```yaml
# ✅ Correct - mask needs 3 args
"SSN": "#{mask(ssn, 'XXX-XX-', 4)}"

# ❌ Wrong - missing arguments
"SSN": "#{mask(ssn)}"
```

## See Documentation

- [FIELD-TRANSFORMATION-FUNCTIONS.md](FIELD-TRANSFORMATION-FUNCTIONS.md) - Complete guide
- [enrollment-functions-demo.yml](config-repo/enrollment-functions-demo.yml) - Example config
- [test-enrollment-functions-request.json](test-enrollment-functions-request.json) - Test payload
