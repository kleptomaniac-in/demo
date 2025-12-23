# FreeMarker vs AcroForm vs PDFBox: When to Use What

## Quick Decision Matrix

| Scenario | Best Approach | Why |
|----------|---------------|-----|
| **Fixed form fields** | AcroForm | Native PDF form, fillable, fastest |
| **Simple data tables** | PDFBox API | Precise control, lightweight |
| **Complex layouts** | FreeMarker | Flexible, maintainable, dynamic |
| **Long text content** | FreeMarker | Automatic wrapping, formatting |
| **Varying structure** | FreeMarker | Conditional rendering |
| **Designer edits** | FreeMarker | HTML/CSS skills sufficient |
| **Multi-language** | FreeMarker | Template reuse, content separation |

---

## Scenario 1: Regulatory Disclosure Documents

### ❌ **BAD: Using PDFBox API**

```java
// Nightmare to maintain!
PDPageContentStream stream = new PDPageContentStream(document, page);
stream.setFont(PDType1Font.HELVETICA, 10);

// Hard-coded line breaks
stream.showText("IMPORTANT NOTICE: This health insurance plan may not cover all");
yPos -= 12;
stream.showText("medical expenses. Coverage limitations apply. Please review the");
yPos -= 12;
stream.showText("Summary of Benefits and Coverage (SBC) document for details...");
// 200+ lines of this...

// What if text changes? What if it needs bold? What if Spanish version?
```

**Problems:**
- ❌ Hard to update text
- ❌ Manual line breaks
- ❌ No styling flexibility
- ❌ Can't reuse for multiple languages
- ❌ Lawyers can't edit (they're not Java developers!)

### ✅ **GOOD: Using FreeMarker**

**Template: `templates/disclosures/dmhc-notice.ftl`**
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .notice { 
            background: #f5f5f5; 
            border-left: 4px solid #d32f2f;
            padding: 20px;
            margin: 20px 0;
        }
        h2 { color: #d32f2f; font-size: 14pt; }
        p { font-size: 10pt; line-height: 1.5; }
        .important { font-weight: bold; }
    </style>
</head>
<body>
    <div class="notice">
        <h2>${disclosure.title}</h2>
        <p class="important">${disclosure.importantNotice}</p>
        
        <#list disclosure.paragraphs as paragraph>
            <p>${paragraph}</p>
        </#list>
        
        <#if disclosure.contactInfo??>
            <p>For questions, contact: ${disclosure.contactInfo.phone}</p>
        </#if>
    </div>
</body>
</html>
```

**Data: `disclosures/dmhc-ca.json`**
```json
{
  "title": "California DMHC Notice",
  "importantNotice": "This plan is regulated by the California Department of Managed Health Care.",
  "paragraphs": [
    "Coverage limitations apply. Please review the Summary of Benefits...",
    "You have the right to file a grievance with the plan...",
    "If unresolved, you may contact DMHC at 1-888-HMO-2219..."
  ],
  "contactInfo": {
    "phone": "1-888-466-2219"
  }
}
```

**Benefits:**
- ✅ Legal team can update JSON
- ✅ Designers can style with CSS
- ✅ Automatic text wrapping
- ✅ Same template for multiple states (just change data)
- ✅ Version control friendly

---

## Scenario 2: Dynamic Plan Comparison Tables

### ❌ **BAD: Using AcroForm**

AcroForm requires **fixed fields**. What if you have 2 plans? 5 plans? 10 plans?

```
Form has fields:
  Plan1_Name, Plan1_Premium, Plan1_Deductible
  Plan2_Name, Plan2_Premium, Plan2_Deductible
  Plan3_Name, Plan3_Premium, Plan3_Deductible

Problem: What if user has 6 plans? Need new form template!
```

### ✅ **GOOD: Using FreeMarker**

**Template: `templates/plan-comparison.ftl`**
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th { background: #1976d2; color: white; padding: 12px; text-align: left; }
        td { padding: 10px; border: 1px solid #ddd; }
        tr:nth-child(even) { background: #f9f9f9; }
        .currency { text-align: right; font-weight: bold; }
    </style>
</head>
<body>
    <h1>Health Plan Options</h1>
    
    <table>
        <thead>
            <tr>
                <th>Plan Name</th>
                <th>Monthly Premium</th>
                <th>Deductible</th>
                <th>Max Out-of-Pocket</th>
                <#if showNetworkColumn>
                    <th>Network</th>
                </#if>
            </tr>
        </thead>
        <tbody>
            <#list plans as plan>
                <tr>
                    <td>${plan.name}</td>
                    <td class="currency">$${plan.premium?string["0.00"]}</td>
                    <td class="currency">$${plan.deductible?string["0.00"]}</td>
                    <td class="currency">$${plan.maxOOP?string["0.00"]}</td>
                    <#if showNetworkColumn>
                        <td>${plan.network}</td>
                    </#if>
                </tr>
            </#list>
        </tbody>
    </table>
    
    <#if footnotes??>
        <div class="footnotes">
            <#list footnotes as note>
                <p><small>${note}</small></p>
            </#list>
        </div>
    </#if>
</body>
</html>
```

**Data:**
```json
{
  "plans": [
    {"name": "Gold PPO", "premium": 450.00, "deductible": 1000, "maxOOP": 5000, "network": "Broad"},
    {"name": "Silver HMO", "premium": 350.00, "deductible": 2000, "maxOOP": 6500, "network": "Standard"},
    {"name": "Bronze EPO", "premium": 250.00, "deductible": 5000, "maxOOP": 8000, "network": "Limited"}
  ],
  "showNetworkColumn": true,
  "footnotes": ["Rates shown are for non-tobacco users", "Subsidies may apply"]
}
```

**Benefits:**
- ✅ Works with ANY number of plans
- ✅ Conditional columns
- ✅ Automatic formatting ($450.00)
- ✅ Styling consistent
- ✅ Easy to add/remove data

---

## Scenario 3: Multi-Language Support

### ❌ **BAD: Using PDFBox API**

```java
// English version
if (language.equals("en")) {
    stream.showText("Coverage Summary");
    stream.showText("Your plan includes:");
} else if (language.equals("es")) {
    stream.showText("Resumen de Cobertura");
    stream.showText("Su plan incluye:");
}
// Duplicated logic everywhere!
```

### ✅ **GOOD: Using FreeMarker**

**Template: `templates/coverage-summary.ftl`**
```html
<!DOCTYPE html>
<html>
<body>
    <h1>${i18n.coverageSummary}</h1>
    <p>${i18n.planIncludes}</p>
    
    <ul>
        <#list benefits as benefit>
            <li>${benefit.translatedName}: ${benefit.coverage}</li>
        </#list>
    </ul>
    
    <p><strong>${i18n.effectiveDate}:</strong> ${effectiveDate}</p>
</body>
</html>
```

**English Data: `i18n/en.json`**
```json
{
  "i18n": {
    "coverageSummary": "Coverage Summary",
    "planIncludes": "Your plan includes:",
    "effectiveDate": "Effective Date"
  }
}
```

**Spanish Data: `i18n/es.json`**
```json
{
  "i18n": {
    "coverageSummary": "Resumen de Cobertura",
    "planIncludes": "Su plan incluye:",
    "effectiveDate": "Fecha de Vigencia"
  }
}
```

**Benefits:**
- ✅ One template, multiple languages
- ✅ Translators edit JSON (no code)
- ✅ Easy to add new languages
- ✅ No code duplication

---

## Scenario 4: Conditional Content Based on State Regulations

### Problem
California requires certain disclosures, Texas requires others, New York has different rules.

### ✅ **Solution: FreeMarker**

**Template: `templates/state-specific.ftl`**
```html
<!DOCTYPE html>
<html>
<body>
    <h1>Important Notices</h1>
    
    <#-- California-specific content -->
    <#if state == "CA">
        <div class="ca-dmhc">
            <h2>California DMHC Notice</h2>
            <p>This plan is regulated by the California Department of Managed Health Care...</p>
        </div>
    </#if>
    
    <#-- Texas-specific content -->
    <#if state == "TX">
        <div class="tx-tdi">
            <h2>Texas Department of Insurance Notice</h2>
            <p>This plan is regulated by the Texas Department of Insurance...</p>
        </div>
    </#if>
    
    <#-- ACA mandates (all states) -->
    <div class="aca-notice">
        <h2>Federal Requirements</h2>
        <p>Under the Affordable Care Act...</p>
    </div>
    
    <#-- New York balance billing protection -->
    <#if state == "NY">
        <div class="ny-balance-billing">
            <h2>Balance Billing Protection</h2>
            <p>New York law protects you from balance billing...</p>
        </div>
    </#if>
</body>
</html>
```

**Benefits:**
- ✅ One template handles all states
- ✅ Easy to add new state rules
- ✅ Clear conditional logic
- ✅ No code changes needed

---

## Scenario 5: Provider Directory with Complex Filtering

### ✅ **FreeMarker Shines Here**

**Template: `templates/provider-directory.ftl`**
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        .provider { margin: 15px 0; padding: 10px; border: 1px solid #ddd; }
        .specialty { color: #1976d2; font-weight: bold; }
        .distance { color: #666; font-style: italic; }
        .accepting { color: green; }
        .not-accepting { color: red; }
    </style>
</head>
<body>
    <h1>Provider Directory</h1>
    <p>Showing providers within ${searchRadius} miles of ${zipCode}</p>
    
    <#-- Group by specialty -->
    <#assign specialties = providers?group_by("specialty")>
    
    <#list specialties?keys as specialty>
        <h2>${specialty}</h2>
        
        <#assign specialtyProviders = specialties[specialty]>
        <#list specialtyProviders?sort_by("distance") as provider>
            <#if provider.acceptingNewPatients>
                <div class="provider">
                    <h3>${provider.name}, ${provider.credentials}</h3>
                    <p class="specialty">${provider.specialty}</p>
                    <p>${provider.address.street}, ${provider.address.city}</p>
                    <p class="distance">Distance: ${provider.distance} miles</p>
                    <p class="accepting">✓ Accepting New Patients</p>
                    
                    <#if provider.languages??>
                        <p>Languages: ${provider.languages?join(", ")}</p>
                    </#if>
                    
                    <#if provider.hospitalAffiliations?has_content>
                        <p><strong>Hospital Affiliations:</strong></p>
                        <ul>
                            <#list provider.hospitalAffiliations as hospital>
                                <li>${hospital.name}</li>
                            </#list>
                        </ul>
                    </#if>
                </div>
            </#if>
        </#list>
    </#list>
</body>
</html>
```

**Benefits:**
- ✅ Automatic grouping
- ✅ Sorting by distance
- ✅ Conditional rendering (accepting patients)
- ✅ Nested data structures
- ✅ Would be nightmare in PDFBox!

---

## Implementation Pattern: YAML Config + FreeMarker

### Config: `templates/benefits-summary.yml`

```yaml
pdfMerge:
  sections:
    - name: medical-benefits
      type: freemarker
      template: templates/medical-benefits.ftl
      enabled: true
      dataEnrichers:
        - benefits-calculator
        - cost-sharing-formatter
      
    - name: prescription-coverage
      type: freemarker
      template: templates/prescription-coverage.ftl
      enabled: "${payload.hasPrescriptionCoverage}"
      
    - name: provider-network
      type: freemarker
      template: templates/provider-network.ftl
      enabled: true
```

### Efficient Data Enrichers

**Java Enricher:**
```java
@Component("benefits-calculator")
public class BenefitsCalculatorEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        List<Map<String, Object>> plans = (List) payload.get("plans");
        
        for (Map<String, Object> plan : plans) {
            Double premium = (Double) plan.get("premium");
            Double deductible = (Double) plan.get("deductible");
            
            // Calculate annual out-of-pocket
            plan.put("annualPremium", premium * 12);
            plan.put("totalFirstYearCost", (premium * 12) + deductible);
            
            // Format for display
            plan.put("premiumFormatted", formatCurrency(premium));
            plan.put("deductibleFormatted", formatCurrency(deductible));
        }
        
        return payload;
    }
}
```

**Use in Template:**
```html
<p>Monthly Premium: ${plan.premiumFormatted}</p>
<p>Annual Premium: ${plan.annualPremium?string["$0,000.00"]}</p>
<p>First Year Total Cost: ${plan.totalFirstYearCost?string["$0,000.00"]}</p>
```

---

## When to Use Each Technology

### Use **AcroForm** when:
- ✅ Fixed form fields (government forms, applications)
- ✅ Need fillable PDFs
- ✅ Simple field mapping (name, SSN, date)
- ✅ Form already exists as PDF
- ✅ Performance critical (fastest option)

**Example:** Enrollment application, W-4 form, claim forms

### Use **PDFBox API** when:
- ✅ Simple tables/lists
- ✅ Precise positioning needed
- ✅ Programmatic drawing (charts, diagrams)
- ✅ Small, static content
- ✅ Performance matters

**Example:** Addendum pages, simple data tables, ID cards

### Use **FreeMarker** when:
- ✅ Complex layouts
- ✅ Varying content length
- ✅ Conditional sections
- ✅ Rich formatting needed
- ✅ Non-developers need to edit
- ✅ Multi-language support
- ✅ Rapid changes expected

**Example:** Benefit summaries, disclosures, provider directories, plan comparisons

---

## Performance Optimization Tips

### 1. Cache Compiled Templates
```java
@Configuration
public class FreeMarkerConfig {
    
    @Bean
    public freemarker.template.Configuration freeMarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        
        // Enable caching
        cfg.setTemplateUpdateDelayMilliseconds(3600000); // 1 hour
        cfg.setCacheStorage(new MruCacheStorage(50, 200));
        
        return cfg;
    }
}
```

### 2. Reuse Template Instances
```java
// ❌ BAD - Creates new template each time
Template template = cfg.getTemplate("benefits.ftl");

// ✅ GOOD - Cache template
private final Template benefitsTemplate;

@PostConstruct
public void init() {
    this.benefitsTemplate = cfg.getTemplate("benefits.ftl");
}
```

### 3. Use Data Enrichers Efficiently
```java
// ❌ BAD - Enriches data in template
<#assign total = 0>
<#list items as item>
    <#assign total = total + item.price>
</#list>

// ✅ GOOD - Pre-calculate in enricher
payload.put("total", calculateTotal(items));
```

### 4. Minimize HTML Complexity
```html
<!-- ❌ BAD - Too many nested divs -->
<div><div><div><div><p>Text</p></div></div></div></div>

<!-- ✅ GOOD - Simple structure -->
<p>Text</p>
```

---

## Real-World Example: Complete Implementation

### Scenario: Medicare Supplement Plan Comparison

**YAML Config:**
```yaml
pdfMerge:
  sections:
    - name: cover-page
      type: freemarker
      template: templates/medicare/cover.ftl
      
    - name: plan-comparison
      type: freemarker
      template: templates/medicare/comparison.ftl
      dataEnrichers:
        - medicare-cost-calculator
        
    - name: state-mandates
      type: freemarker
      template: templates/medicare/state-mandates.ftl
      enabled: "${state == 'CA' || state == 'NY'}"
```

**Template: `templates/medicare/comparison.ftl`**
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        table { width: 100%; border-collapse: collapse; }
        th { background: #2e7d32; color: white; padding: 8px; font-size: 9pt; }
        td { padding: 6px; border: 1px solid #ccc; font-size: 8pt; }
        .covered { text-align: center; background: #e8f5e9; }
        .not-covered { text-align: center; background: #ffebee; }
        .section-header { background: #f5f5f5; font-weight: bold; }
    </style>
</head>
<body>
    <h1>Medicare Supplement Plan Comparison</h1>
    
    <table>
        <thead>
            <tr>
                <th>Benefit</th>
                <#list plans as plan>
                    <th>${plan.name}</th>
                </#list>
            </tr>
        </thead>
        <tbody>
            <#list benefitCategories as category>
                <tr class="section-header">
                    <td colspan="${plans?size + 1}">${category.name}</td>
                </tr>
                
                <#list category.benefits as benefit>
                    <tr>
                        <td>${benefit.description}</td>
                        <#list plans as plan>
                            <#assign coverage = plan.coverage[benefit.id]!"none">
                            <td class="${coverage == '100%' ? 'covered' : 'not-covered'}">
                                ${coverage}
                            </td>
                        </#list>
                    </tr>
                </#list>
            </#list>
        </tbody>
    </table>
    
    <div class="premiums">
        <h2>Monthly Premiums by Age</h2>
        <table>
            <tr>
                <th>Age</th>
                <#list plans as plan>
                    <th>${plan.name}</th>
                </#list>
            </tr>
            <#list ageGroups as age>
                <tr>
                    <td>${age.range}</td>
                    <#list plans as plan>
                        <td class="currency">
                            $${plan.premiums[age.key]?string["0.00"]}
                        </td>
                    </#list>
                </tr>
            </#list>
        </table>
    </div>
</body>
</html>
```

**This would be INSANELY complex with PDFBox API!**

---

## Conclusion

**Use the right tool for the job:**

- **AcroForm:** Fixed forms, fillable PDFs
- **PDFBox API:** Simple tables, precise positioning
- **FreeMarker:** Complex layouts, dynamic content, maintainability

**The sweet spot:** Combine all three!
- AcroForm for main enrollment form
- FreeMarker for benefit summaries, disclosures
- PDFBox for addendum pages

This gives you the best of all worlds: performance, flexibility, and maintainability.
