# Field Transformation Functions Guide

## Overview

Field transformation functions provide reusable operations that can be used in YAML configurations and template field mappings. These functions enable common transformations like concatenation, masking, case conversion, and formatting without writing custom code.

## Syntax

Functions use the syntax: `#{functionName(arg1, arg2, ...)}`

```yaml
fieldMappings:
  FullName: "#{concat(firstName, ' ', lastName)}"
  SSN: "#{mask(ssn, 'XXX-XX-', 4)}"
  Email: "#{lowercase(email)}"
```

## Features

- ✅ **Reusable across templates** - Use in FreeMarker, AcroForm, PDFBox configs
- ✅ **Composable** - Nest functions: `#{uppercase(#{concat(firstName, lastName)})}`
- ✅ **Type-safe** - Automatic type conversion and validation
- ✅ **Extensible** - Add custom functions by implementing `FieldTransformationFunction`
- ✅ **YAML-friendly** - Works directly in configuration files

## Available Functions

### String Operations

#### concat()
Concatenates multiple strings together.

**Syntax:** `#{concat(str1, str2, ...)}`

**Examples:**
```yaml
FullName: "#{concat(firstName, ' ', lastName)}"
Address: "#{concat(street, ', ', city, ', ', state, ' ', zip)}"
Label: "#{concat('Account #', accountNumber)}"
```

**Result:**
- `concat('John', ' ', 'Doe')` → `"John Doe"`
- `concat('123 ', 'Main St')` → `"123 Main St"`

---

#### uppercase()
Converts string to uppercase.

**Syntax:** `#{uppercase(text)}`

**Examples:**
```yaml
StateCode: "#{uppercase(state)}"
EmailUpper: "#{uppercase(email)}"
```

**Result:**
- `uppercase('california')` → `"CALIFORNIA"`
- `uppercase('test@example.com')` → `"TEST@EXAMPLE.COM"`

---

#### lowercase()
Converts string to lowercase.

**Syntax:** `#{lowercase(text)}`

**Examples:**
```yaml
EmailLower: "#{lowercase(email)}"
Username: "#{lowercase(firstName)}"
```

**Result:**
- `lowercase('JOHN')` → `"john"`
- `lowercase('Test@EXAMPLE.COM')` → `"test@example.com"`

---

#### capitalize()
Capitalizes the first letter of each word.

**Syntax:** `#{capitalize(text)}`

**Examples:**
```yaml
DisplayName: "#{capitalize(fullName)}"
Title: "#{capitalize(jobTitle)}"
```

**Result:**
- `capitalize('john doe')` → `"John Doe"`
- `capitalize('software ENGINEER')` → `"Software Engineer"`

---

#### substring()
Extracts a substring from a string.

**Syntax:** `#{substring(text, start)}` or `#{substring(text, start, end)}`

**Examples:**
```yaml
FirstInitial: "#{substring(firstName, 0, 1)}"
Last4SSN: "#{substring(ssn, 5)}"
AreaCode: "#{substring(phone, 0, 3)}"
```

**Result:**
- `substring('Hello', 0, 1)` → `"H"`
- `substring('123-45-6789', 7)` → `"6789"`

---

#### replace()
Replaces all occurrences of a substring.

**Syntax:** `#{replace(text, oldStr, newStr)}`

**Examples:**
```yaml
PhoneDigits: "#{replace(phone, '-', '')}"
CleanedText: "#{replace(description, '_', ' ')}"
```

**Result:**
- `replace('555-123-4567', '-', '')` → `"5551234567"`
- `replace('hello_world', '_', ' ')` → `"hello world"`

---

#### trim()
Removes whitespace from both ends.

**Syntax:** `#{trim(text)}`

**Examples:**
```yaml
CleanName: "#{trim(firstName)}"
CleanEmail: "#{trim(email)}"
```

**Result:**
- `trim('  John  ')` → `"John"`
- `trim(' test@example.com ')` → `"test@example.com"`

---

### Masking & Security

#### mask()
Masks a string showing only the last N characters.

**Syntax:** `#{mask(value, maskPattern, visibleCount)}`

**Examples:**
```yaml
SSN: "#{mask(ssn, 'XXX-XX-', 4)}"
CreditCard: "#{mask(cardNumber, '****-****-****-', 4)}"
AccountNumber: "#{mask(accountNum, '***', 3)}"
```

**Result:**
- `mask('123-45-6789', 'XXX-XX-', 4)` → `"XXX-XX-6789"`
- `mask('4111111111111111', '****-****-****-', 4)` → `"****-****-****-1111"`

---

#### maskEmail()
Masks an email address showing only first character and domain.

**Syntax:** `#{maskEmail(email)}`

**Examples:**
```yaml
MaskedEmail: "#{maskEmail(personalEmail)}"
```

**Result:**
- `maskEmail('john.doe@example.com')` → `"j***@example.com"`
- `maskEmail('alice@test.org')` → `"a***@test.org"`

---

#### maskPhone()
Masks a phone number showing only last 4 digits.

**Syntax:** `#{maskPhone(phone)}`

**Examples:**
```yaml
MaskedPhone: "#{maskPhone(phoneNumber)}"
```

**Result:**
- `maskPhone('555-123-4567')` → `"XXX-XXX-4567"`
- `maskPhone('(555) 123-4567')` → `"XXX-XXX-4567"`

---

### Date Formatting

#### formatDate()
Formats a date string to a specified format.

**Syntax:** `#{formatDate(date, outputFormat)}` or `#{formatDate(date, outputFormat, inputFormat)}`

**Date Format Patterns:**
- `yyyy-MM-dd` → 2026-01-15
- `MM/dd/yyyy` → 01/15/2026
- `MMM dd, yyyy` → Jan 15, 2026
- `MMMM d, yyyy` → January 15, 2026
- `E, MMM dd` → Wed, Jan 15

**Examples:**
```yaml
FormattedDOB: "#{formatDate(dateOfBirth, 'MMM dd, yyyy')}"
EffectiveDate: "#{formatDate(effectiveDate, 'MMMM d, yyyy')}"
ShortDate: "#{formatDate(submittedDate, 'MM/dd/yy')}"
```

**Result:**
- `formatDate('2026-01-15', 'MMM dd, yyyy')` → `"Jan 15, 2026"`
- `formatDate('01/15/2026', 'MMMM d, yyyy')` → `"January 15, 2026"`

---

#### parseDate()
Parses a date string and returns it in ISO format (yyyy-MM-dd).

**Syntax:** `#{parseDate(date)}` or `#{parseDate(date, inputFormat)}`

**Examples:**
```yaml
ISODate: "#{parseDate(dateOfBirth, 'MM/dd/yyyy')}"
StandardDate: "#{parseDate(submittedDate)}"
```

**Result:**
- `parseDate('01/15/2026', 'MM/dd/yyyy')` → `"2026-01-15"`
- `parseDate('Jan 15, 2026')` → `"2026-01-15"`

---

### Number & Currency Formatting

#### formatNumber()
Formats a number with specified decimal places.

**Syntax:** `#{formatNumber(value, decimalPlaces)}`

**Examples:**
```yaml
FormattedAmount: "#{formatNumber(amount, 2)}"
Percentage: "#{formatNumber(rate, 1)}"
WholeNumber: "#{formatNumber(count, 0)}"
```

**Result:**
- `formatNumber(1234.5, 2)` → `"1,234.50"`
- `formatNumber(99.9, 1)` → `"99.9"`
- `formatNumber(1000000, 0)` → `"1,000,000"`

---

#### formatCurrency()
Formats a number as US currency with $ symbol.

**Syntax:** `#{formatCurrency(amount)}`

**Examples:**
```yaml
TotalPremium: "#{formatCurrency(grandTotalPremium)}"
MedicalCost: "#{formatCurrency(medicalPremium)}"
```

**Result:**
- `formatCurrency(1234.5)` → `"$1,234.50"`
- `formatCurrency(99)` → `"$99.00"`
- `formatCurrency(1000000)` → `"$1,000,000.00"`

---

### Conditional & Default Values

#### coalesce()
Returns the first non-null, non-empty value.

**Syntax:** `#{coalesce(value1, value2, ..., defaultValue)}`

**Examples:**
```yaml
ContactEmail: "#{coalesce(workEmail, personalEmail, email)}"
PhoneNumber: "#{coalesce(mobilePhone, homePhone, '555-0000')}"
```

**Result:**
- `coalesce(null, '', 'default')` → `"default"`
- `coalesce('', 'value2', 'value3')` → `"value2"`

---

#### default()
Returns a default value if the input is null or empty.

**Syntax:** `#{default(value, defaultValue)}`

**Examples:**
```yaml
MiddleName: "#{default(middleName, 'N/A')}"
Nickname: "#{default(nickname, 'Not Provided')}"
```

**Result:**
- `default('', 'N/A')` → `"N/A"`
- `default('John', 'N/A')` → `"John"`

---

#### ifEmpty()
Returns a replacement value if the input is empty.

**Syntax:** `#{ifEmpty(value, replacement)}`

**Examples:**
```yaml
DisplayName: "#{ifEmpty(nickname, firstName)}"
State: "#{ifEmpty(state, 'Unknown')}"
```

**Result:**
- `ifEmpty('', 'fallback')` → `"fallback"`
- `ifEmpty('value', 'fallback')` → `"value"`

---

## Usage in Different Contexts

### 1. AcroForm Field Mappings

Use functions directly in field mappings:

```yaml
sections:
  - type: ACROFORM
    templatePath: "enrollment-form.pdf"
    fieldMappings:
      # String operations
      "FullName": "#{concat(firstName, ' ', lastName)}"
      "UpperEmail": "#{uppercase(email)}"
      
      # Masking
      "SSN": "#{mask(ssn, 'XXX-XX-', 4)}"
      "Phone": "#{maskPhone(phoneNumber)}"
      
      # Dates
      "DOB": "#{formatDate(dateOfBirth, 'MM/dd/yyyy')}"
      
      # Currency
      "Premium": "#{formatCurrency(monthlyPremium)}"
      
      # Defaults
      "MiddleName": "#{default(middleName, 'N/A')}"
```

### 2. With Enriched Data

Combine functions with enriched payload data:

```yaml
enrichers:
  - enrollmentContext
  - coverageSummary

sections:
  - type: ACROFORM
    fieldMappings:
      # Use enriched data with functions
      "ApplicantAge": "coverageSummary.enrichedApplicants[0].calculatedAge"
      "AgeDisplay": "#{concat(coverageSummary.enrichedApplicants[0].calculatedAge, ' years old')}"
      
      # Product flags
      "ProductList": "#{concat('Medical: ', enrollmentContext.hasMedical, ', Dental: ', enrollmentContext.hasDental)}"
      
      # State info
      "StateDisplay": "#{concat(enrollmentContext.stateFullName, ' (', enrollmentContext.state, ')')}"
```

### 3. Nested Functions

Combine multiple functions:

```yaml
fieldMappings:
  # Uppercase a concatenated string
  "FullNameUpper": "#{uppercase(#{concat(firstName, ' ', lastName)})}"
  
  # Capitalize and trim
  "CleanName": "#{capitalize(#{trim(fullName)})}"
  
  # Format then uppercase
  "FormattedDateUpper": "#{uppercase(#{formatDate(date, 'MMM dd, yyyy')})}"
  
  # Multiple transformations
  "ProcessedEmail": "#{lowercase(#{trim(email)})}"
```

### 4. With Array Access

Access array elements with functions:

```yaml
fieldMappings:
  # Primary applicant
  "Primary_FullName": "#{concat(applicants[0].firstName, ' ', applicants[0].lastName)}"
  "Primary_SSN": "#{mask(applicants[0].ssn, 'XXX-XX-', 4)}"
  
  # Spouse
  "Spouse_FullName": "#{concat(applicants[1].firstName, ' ', applicants[1].lastName)}"
  "Spouse_Age": "#{concat(applicants[1].age, ' years')}"
  
  # From filtered arrays
  "Primary_Name": "#{concat(applicants[relationship=PRIMARY].firstName, ' ', applicants[relationship=PRIMARY].lastName)}"
```

### 5. Complex Examples

Real-world scenarios:

```yaml
fieldMappings:
  # Full address
  "FullAddress": "#{concat(street, ', ', city, ', ', uppercase(state), ' ', zip)}"
  
  # Contact info with masking
  "ContactDisplay": "#{concat(#{maskEmail(email)}, ' | ', #{maskPhone(phone)})}"
  
  # Formatted premium with total
  "PremiumSummary": "#{concat('Medical: ', #{formatCurrency(medicalPremium)}, ' + Dental: ', #{formatCurrency(dentalPremium)}, ' = ', #{formatCurrency(totalPremium)})}"
  
  # Person details
  "PersonInfo": "#{concat(#{capitalize(fullName)}, ', Age ', age, ', DOB: ', #{formatDate(dob, 'MM/dd/yyyy')})}"
  
  # Conditional contact
  "PreferredContact": "#{coalesce(mobilePhone, homePhone, #{concat('Email: ', email)})}"
```

## Field Path References

Functions support dot notation and array access:

```yaml
# Simple field
"Name": "#{uppercase(firstName)}"

# Nested field
"City": "#{uppercase(address.city)}"

# Array index
"FirstName": "#{capitalize(applicants[0].firstName)}"

# Filtered array
"PrimaryEmail": "#{lowercase(applicants[relationship=PRIMARY].email)}"

# Multiple filters
"MedicalCarrier": "#{uppercase(coverages[applicantId=A001][productType=MEDICAL].carrier)}"

# Enriched data
"Age": "#{concat(coverageSummary.enrichedApplicants[0].calculatedAge, ' years')}"
```

## Testing Functions

### Test Request Example

```json
{
  "configName": "enrollment-functions-demo",
  "payload": {
    "applicants": [
      {
        "firstName": "john",
        "lastName": "doe",
        "middleName": "",
        "email": "JOHN.DOE@EXAMPLE.COM",
        "phone": "555-123-4567",
        "ssn": "123-45-6789",
        "dateOfBirth": "1985-05-15"
      }
    ],
    "enrollment": {
      "effectiveDate": "2026-01-01",
      "submittedDate": "2025-12-18"
    },
    "premiums": {
      "medicalPremium": 450.00,
      "dentalPremium": 45.00,
      "totalPremium": 495.00
    }
  }
}
```

### Expected Transformations

| Function | Input | Output |
|----------|-------|--------|
| `concat(firstName, ' ', lastName)` | john, doe | john doe |
| `uppercase(email)` | JOHN.DOE@EXAMPLE.COM | JOHN.DOE@EXAMPLE.COM |
| `lowercase(email)` | JOHN.DOE@EXAMPLE.COM | john.doe@example.com |
| `capitalize(firstName)` | john | John |
| `mask(ssn, 'XXX-XX-', 4)` | 123-45-6789 | XXX-XX-6789 |
| `maskEmail(email)` | john.doe@example.com | j***@example.com |
| `maskPhone(phone)` | 555-123-4567 | XXX-XXX-4567 |
| `formatDate(dateOfBirth, 'MMM dd, yyyy')` | 1985-05-15 | May 15, 1985 |
| `formatCurrency(totalPremium)` | 495.00 | $495.00 |
| `default(middleName, 'N/A')` | (empty) | N/A |

## Creating Custom Functions

To add your own transformation function:

1. **Create Function Class:**

```java
package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

public class ReverseFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        String value = args[0].toString();
        return new StringBuilder(value).reverse().toString();
    }
    
    @Override
    public String getName() {
        return "reverse";
    }
    
    @Override
    public String getDescription() {
        return "Reverses a string: reverse('hello') -> 'olleh'";
    }
}
```

2. **Register in FunctionRegistry:**

```java
// In FunctionRegistry.registerDefaultFunctions()
register(new ReverseFunction());
```

3. **Use in Configuration:**

```yaml
fieldMappings:
  "ReversedName": "#{reverse(firstName)}"
```

## Best Practices

### 1. Keep Functions Simple
✅ Good: `#{concat(firstName, ' ', lastName)}`  
❌ Bad: `#{concat(#{uppercase(#{trim(firstName)})}, ' ', #{lowercase(#{substring(lastName, 0, 10)})})}`

### 2. Use Enrichers for Complex Logic
If transformation requires multiple steps or business logic, create an enricher instead:

✅ Use enricher for: Age calculation, premium aggregation, complex validations  
✅ Use functions for: Formatting, masking, simple string operations

### 3. Cache Expensive Operations
Use enrichers to pre-compute expensive transformations:

```yaml
enrichers:
  - coverageSummary  # Calculates age once

fieldMappings:
  # Use pre-calculated value instead of calculating in function
  "Age": "coverageSummary.enrichedApplicants[0].calculatedAge"
```

### 4. Handle Null Values
Functions return empty string for null inputs. Use default functions:

```yaml
"MiddleName": "#{default(middleName, 'N/A')}"
"Phone": "#{coalesce(mobilePhone, homePhone, '555-0000')}"
```

### 5. Test with Real Data
Always test function expressions with realistic payload data to ensure correct output.

## Function Reference Summary

| Category | Functions |
|----------|-----------|
| **String Operations** | concat, uppercase, lowercase, capitalize, substring, replace, trim |
| **Masking** | mask, maskEmail, maskPhone |
| **Dates** | formatDate, parseDate |
| **Numbers** | formatNumber, formatCurrency |
| **Conditionals** | coalesce, default, ifEmpty |

**Total: 17 built-in functions**

## Debugging Tips

### 1. Test Functions Individually
Start with simple functions before nesting:

```yaml
# Test individually first
"FirstName": "#{uppercase(firstName)}"
"LastName": "#{uppercase(lastName)}"

# Then combine
"FullNameUpper": "#{uppercase(#{concat(firstName, ' ', lastName)})}"
```

### 2. Check Function Names
Function names are case-insensitive: `uppercase`, `UPPERCASE`, `Uppercase` all work.

### 3. Verify Arguments
Ensure you're passing the correct number and type of arguments:

```yaml
# ✅ Correct
"Date": "#{formatDate(dob, 'MM/dd/yyyy')}"

# ❌ Wrong - missing format argument
"Date": "#{formatDate(dob)}"
```

### 4. Use Debug Endpoint
Generate PDF and check AcroForm field values to see what was filled.

## Performance Considerations

- **Caching**: Functions are resolved at PDF generation time
- **Complexity**: O(n) for most functions where n is string length
- **Memory**: Functions create new strings, consider payload size
- **Optimization**: Use enrichers for calculations needed multiple times

## Migration from Hardcoded Transformations

**Before** (hardcoded in Java):
```java
String fullName = applicant.getFirstName() + " " + applicant.getLastName();
String maskedSSN = "XXX-XX-" + ssn.substring(ssn.length() - 4);
```

**After** (declarative in YAML):
```yaml
fieldMappings:
  "FullName": "#{concat(applicants[0].firstName, ' ', applicants[0].lastName)}"
  "SSN": "#{mask(applicants[0].ssn, 'XXX-XX-', 4)}"
```

**Benefits:**
- ✅ No code changes needed
- ✅ Reusable across templates
- ✅ Configurable without recompilation
- ✅ Easier to test and modify

## See Also

- [PAYLOAD-ENRICHER-EXAMPLE.md](PAYLOAD-ENRICHER-EXAMPLE.md) - For complex business logic
- [ENROLLMENT-INTEGRATION-GUIDE.md](ENROLLMENT-INTEGRATION-GUIDE.md) - Enricher integration
- [enrollment-functions-demo.yml](config-repo/enrollment-functions-demo.yml) - Example configuration
