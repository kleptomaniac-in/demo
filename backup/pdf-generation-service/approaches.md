Your understanding is **absolutely correct**! Here's why:

## 1. AcroForm PDF Templates - **NEED Mapping** ✅

AcroForm PDFs have predefined field names that must be mapped from your payload:

```yaml
# Mapping required for AcroForm
sections:
  - name: "Insurance Application Form"
    type: acroform
    template: "insurance-form.pdf"
    fieldMapping:
      # AcroForm Field → Payload Path
      "applicant_name": "payload.customer.fullName"
      "dob": "payload.customer.dateOfBirth"
      "address_line1": "payload.customer.address.street"
      "policy_number": "payload.policyId"
```

**Why mapping needed:**
- Form has fixed field names like `applicant_name`
- Your payload structure is different: `customer.fullName`
- Need explicit mapping to connect them

---

## 2. FreeMarker Templates - **NO Granular Mapping Needed** ✅

FreeMarker accesses data structures directly:

```yaml
sections:
  - name: "Healthcare Member Plans"
    type: freemarker
    template: "member-healthcare-plans.ftl"
    # No field mapping needed!
```

**Why mapping NOT needed:**
```html
<!-- FreeMarker template accesses payload directly -->
<#list payload.members as member>
  <tr>
    <td>${member.name}</td>
    <#list member.medicalPlans as plan>
      <td>${plan.planName}: $${plan.basePremium}</td>
    </#list>
  </tr>
</#list>
```

The template itself defines the data access paths. Just pass the whole payload.

---

## 3. PDFBox Custom Generators - **NO Mapping Needed** ✅

Generators access payload programmatically:

```yaml
sections:
  - name: "Custom Chart"
    type: pdfbox
    template: "chart-generator"
    # No field mapping needed!
```

**Why mapping NOT needed:**
```java
@Override
public PDDocument generate(Map<String, Object> payload) {
    // Code directly accesses payload
    String companyName = (String) payload.get("companyName");
    List<Map<String, Object>> members = 
        (List<Map<String, Object>>) payload.get("members");
    
    // Generate PDF using the data
}
```

The generator code defines how to extract and use data.

---

## Recommended Architecture

### Support All Three Types:

```yaml
pdfMerge:
  sections:
    # Type 1: AcroForm - WITH field mapping
    - name: "Application Form"
      type: acroform
      template: "forms/insurance-app.pdf"
      fieldMapping:
        "applicant_name": "customer.fullName"
        "policy_type": "policy.type"
        "premium_amount": "policy.premium"
        
    # Type 2: FreeMarker - NO field mapping
    - name: "Member Details"
      type: freemarker
      template: "member-details.ftl"
      # Template accesses payload.* directly
      
    # Type 3: PDFBox - NO field mapping
    - name: "Premium Chart"
      type: pdfbox
      template: "chart-generator"
      # Generator code accesses payload directly
      
    # Type 4: Mixed - AcroForm with computed fields
    - name: "Summary Form"
      type: acroform
      template: "forms/summary.pdf"
      fieldMapping:
        "total_premium": "policy.premium"
        # Could also support expressions:
        "annual_premium": "policy.premium * 12"
```

### Implementation Structure:

```java
private PDDocument generateSectionPdf(SectionConfig section, Map<String, Object> payload) 
    throws IOException {
    
    switch (section.getType()) {
        case "acroform":
            // Load existing PDF form
            PDDocument formDoc = PDDocument.load(new File(section.getTemplate()));
            
            // Apply field mappings
            PDAcroForm acroForm = formDoc.getDocumentCatalog().getAcroForm();
            Map<String, String> fieldMapping = section.getFieldMapping();
            
            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String fieldName = entry.getKey();
                String payloadPath = entry.getValue();
                
                Object value = resolvePath(payload, payloadPath);
                PDField field = acroForm.getField(fieldName);
                if (field != null) {
                    field.setValue(String.valueOf(value));
                }
            }
            
            return formDoc;
            
        case "freemarker":
            // Pass entire payload wrapped in model
            Map<String, Object> model = new HashMap<>();
            model.put("payload", payload);
            
            String html = freemarkerService.processTemplateFromLocation(
                section.getTemplate(), model
            );
            byte[] pdfBytes = htmlPdfService.renderHtmlToPdf(html);
            return PDDocument.load(new ByteArrayInputStream(pdfBytes));
            
        case "pdfbox":
            // Pass payload directly to generator
            PdfBoxGenerator generator = pdfBoxRegistry.getGenerator(
                section.getTemplate()
            );
            return generator.generate(payload);
            
        default:
            throw new IllegalArgumentException("Unknown type: " + section.getType());
    }
}

// Helper to resolve nested paths like "customer.address.street"
private Object resolvePath(Map<String, Object> data, String path) {
    String[] parts = path.split("\\.");
    Object current = data;
    
    for (String part : parts) {
        if (current instanceof Map) {
            current = ((Map<?, ?>) current).get(part);
        } else {
            return null;
        }
    }
    
    return current;
}
```

### Update SectionConfig Class:

```java
class SectionConfig {
    private String name;
    private String type; // "acroform", "freemarker", "pdfbox"
    private String template;
    private boolean enabled;
    private String insertAfter;
    private Map<String, String> fieldMapping;  // Only for acroform

    // Getters/setters...
    
    public Map<String, String> getFieldMapping() { 
        return fieldMapping; 
    }
    
    public void setFieldMapping(Map<String, String> fieldMapping) { 
        this.fieldMapping = fieldMapping; 
    }
}
```

---

## Summary

Your understanding is **100% correct**:

| Type | Needs Mapping? | Why |
|------|---------------|-----|
| **AcroForm** | ✅ YES | Fixed field names need mapping to payload structure |
| **FreeMarker** | ❌ NO | Template accesses `payload.*` directly with full path |
| **PDFBox** | ❌ NO | Code accesses `payload.get()` programmatically |

**Benefits of this approach:**
- ✅ Flexible - Supports forms, templates, and custom generators
- ✅ Simple - Only map when needed (AcroForms)
- ✅ Powerful - FreeMarker handles complex structures naturally
- ✅ Extensible - PDFBox gives full programmatic control

This is the ideal architecture for your mixed use case!