**Yes, absolutely!** That's exactly what PayloadEnrichers are designed for - data transformations. Let me show you how to do each of those scenarios:

## 1. **Convert firstName to Uppercase**

```java
@Component
public class FieldFormattingEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Uppercase transformation
        if (payload.containsKey("firstName")) {
            String firstName = payload.get("firstName").toString();
            enriched.put("firstNameUpper", firstName.toUpperCase());
        }
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "fieldFormatting";
    }
}
```

**Usage in templates**:
```java
// PDFBox
String name = payload.get("firstNameUpper");  // "JOHN"

// FreeMarker
${firstNameUpper}  // JOHN
```

---

## 2. **Concatenate Multiple Fields**

```java
@Override
public Map<String, Object> enrich(Map<String, Object> payload) {
    Map<String, Object> enriched = new HashMap<>(payload);
    
    // Concatenate name fields
    String firstName = (String) payload.get("firstName");
    String middleName = (String) payload.get("middleName");
    String lastName = (String) payload.get("lastName");
    
    // Full name
    enriched.put("fullName", firstName + " " + lastName);
    
    // Full name with middle initial
    if (middleName != null && !middleName.isEmpty()) {
        enriched.put("fullNameWithMiddle", 
            firstName + " " + middleName.charAt(0) + ". " + lastName);
    }
    
    // Concatenate address
    String street = (String) payload.get("street");
    String city = (String) payload.get("city");
    String state = (String) payload.get("state");
    String zip = (String) payload.get("zip");
    
    enriched.put("fullAddress", 
        street + ", " + city + ", " + state + " " + zip);
    
    return enriched;
}
```

**Result**:
```javascript
{
  firstName: "John",
  middleName: "Michael",
  lastName: "Doe",
  fullName: "John Doe",              // ← Concatenated
  fullNameWithMiddle: "John M. Doe",  // ← Concatenated with initial
  fullAddress: "123 Main St, San Francisco, CA 94102"  // ← Concatenated
}
```

---

## 3. **Format Dates**

Your `DateFormattingEnricher.java` **already does this**! You can see it on lines 60-76:

```java
// Already in your code!
private String formatDateLong(String isoDate) {
    LocalDate date = LocalDate.parse(isoDate, INPUT_FORMAT);
    return date.format(DISPLAY_FORMAT);  // "December 15, 2025"
}

private String formatDateShort(String isoDate) {
    LocalDate date = LocalDate.parse(isoDate, INPUT_FORMAT);
    return date.format(SHORT_FORMAT);  // "12/15/2025"
}
```

**Add more date formats**:
```java
private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy");
private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

formattedDates.put("effectiveDateMonthYear", formatMonthYear(date));  // "December 2025"
formattedDates.put("effectiveDateISO", date);  // "2025-12-15"
```

---

## 4. **Mask Sensitive Fields**

```java
@Component
public class DataMaskingEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Mask SSN: 123-45-6789 → ***-**-6789
        if (payload.containsKey("ssn")) {
            String ssn = payload.get("ssn").toString();
            enriched.put("ssnMasked", maskSSN(ssn));
        }
        
        // Mask credit card: 1234-5678-9012-3456 → ****-****-****-3456
        if (payload.containsKey("creditCard")) {
            String cc = payload.get("creditCard").toString();
            enriched.put("creditCardMasked", maskCreditCard(cc));
        }
        
        // Mask email: john.doe@example.com → j***@example.com
        if (payload.containsKey("email")) {
            String email = payload.get("email").toString();
            enriched.put("emailMasked", maskEmail(email));
        }
        
        // Mask phone: (555) 123-4567 → (***) ***-4567
        if (payload.containsKey("phone")) {
            String phone = payload.get("phone").toString();
            enriched.put("phoneMasked", maskPhone(phone));
        }
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "dataMasking";
    }
    
    private String maskSSN(String ssn) {
        if (ssn == null || ssn.length() < 4) return "***-**-****";
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }
    
    private String maskCreditCard(String cc) {
        if (cc == null || cc.length() < 4) return "****-****-****-****";
        String lastFour = cc.replaceAll("[^0-9]", "");
        lastFour = lastFour.substring(lastFour.length() - 4);
        return "****-****-****-" + lastFour;
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***.com";
        String[] parts = email.split("@");
        String local = parts[0];
        return local.charAt(0) + "***@" + parts[1];
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "(***) ***-****";
        String digits = phone.replaceAll("[^0-9]", "");
        String lastFour = digits.substring(digits.length() - 4);
        return "(***) ***-" + lastFour;
    }
}
```

**Result**:
```javascript
{
  ssn: "123-45-6789",
  ssnMasked: "***-**-6789",           // ← Masked
  
  creditCard: "1234-5678-9012-3456",
  creditCardMasked: "****-****-****-3456",  // ← Masked
  
  email: "john.doe@example.com",
  emailMasked: "j***@example.com",     // ← Masked
  
  phone: "(555) 123-4567",
  phoneMasked: "(***) ***-4567"        // ← Masked
}
```

---

## 5. **Complete Example Combining All Transformations**

```java
@Component
public class ComprehensiveEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // 1. UPPERCASE transformation
        if (payload.containsKey("firstName")) {
            enriched.put("firstNameUpper", 
                payload.get("firstName").toString().toUpperCase());
        }
        
        // 2. CONCATENATION
        String firstName = (String) payload.get("firstName");
        String lastName = (String) payload.get("lastName");
        enriched.put("fullName", firstName + " " + lastName);
        
        // 3. DATE FORMATTING
        if (payload.containsKey("dateOfBirth")) {
            String dob = payload.get("dateOfBirth").toString();
            LocalDate date = LocalDate.parse(dob);
            enriched.put("dobFormatted", 
                date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        }
        
        // 4. MASKING
        if (payload.containsKey("ssn")) {
            String ssn = payload.get("ssn").toString();
            enriched.put("ssnMasked", "***-**-" + ssn.substring(ssn.length() - 4));
        }
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "comprehensive";
    }
}
```

**Configuration**:
```yaml
pdfMerge:
  enrichers:
    - comprehensive  # Applies all transformations
```

**Input**:
```json
{
  "firstName": "john",
  "lastName": "doe",
  "dateOfBirth": "1985-03-15",
  "ssn": "123-45-6789"
}
```

**Output** (enriched):
```json
{
  "firstName": "john",
  "lastName": "doe",
  "dateOfBirth": "1985-03-15",
  "ssn": "123-45-6789",
  
  "firstNameUpper": "JOHN",                    // ← Uppercase
  "fullName": "john doe",                      // ← Concatenated
  "dobFormatted": "March 15, 1985",           // ← Formatted
  "ssnMasked": "***-**-6789"                  // ← Masked
}
```

---

## When to Use PayloadEnrichers

✅ **Perfect for PayloadEnrichers**:
- Data transformations (uppercase, lowercase, trim)
- Field concatenation (fullName, fullAddress)
- Date/number formatting
- Masking/redacting sensitive data
- Calculations (age, totals, percentages)
- Business logic (flags, categories, statuses)
- Data validation and normalization

❌ **Not for PayloadEnrichers**:
- PDF rendering (use PDFBox generators)
- HTML rendering (use FreeMarker templates)
- External API calls (use services, then enrich the response)
- Database queries (fetch data first, then enrich)

---

## Real-World Example

Here's how you'd use it in the enrollment scenario:

```java
@Component
public class EnrollmentDataEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Transform applicant data
        if (payload.containsKey("applicant")) {
            Map<String, Object> applicant = (Map) payload.get("applicant");
            
            // Uppercase for headers
            String firstName = (String) applicant.get("firstName");
            String lastName = (String) applicant.get("lastName");
            enriched.put("applicantNameUpper", 
                (firstName + " " + lastName).toUpperCase());
            
            // Concatenate full name
            enriched.put("applicantFullName", firstName + " " + lastName);
            
            // Format DOB
            String dob = (String) applicant.get("dateOfBirth");
            enriched.put("dobDisplay", formatDate(dob));
            
            // Mask SSN for display
            String ssn = (String) applicant.get("ssn");
            enriched.put("ssnDisplay", "***-**-" + ssn.substring(7));
        }
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "enrollmentData";
    }
}
```

**Use in PDFBox**:
```java
drawText("APPLICANT: " + payload.get("applicantNameUpper"));  // "APPLICANT: JOHN DOE"
drawText("DOB: " + payload.get("dobDisplay"));                // "DOB: March 15, 1985"
drawText("SSN: " + payload.get("ssnDisplay"));                // "SSN: ***-**-6789"
```

**Use in FreeMarker**:
```html
<h1>${applicantNameUpper}</h1>           <!-- JOHN DOE -->
<p>Date of Birth: ${dobDisplay}</p>      <!-- March 15, 1985 -->
<p>SSN: ${ssnDisplay}</p>                <!-- ***-**-6789 -->
```

---

## Summary

✅ **YES!** PayloadEnrichers are perfect for:
1. ✅ **Uppercase/Lowercase** - `firstNameUpper`, `emailLower`
2. ✅ **Concatenation** - `fullName`, `fullAddress`, `displayLabel`
3. ✅ **Date Formatting** - Already in your `DateFormattingEnricher`!
4. ✅ **Masking** - SSN, credit cards, emails, phones

**Key principle**: If it's a **data transformation** (not rendering), use a PayloadEnricher!