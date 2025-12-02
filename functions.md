## FreeMarker Functions vs Macros

### **Functions** - Return a Value
Functions compute and **return a single value** (string, number, boolean). They cannot output HTML/text directly.

```freemarker
<#function calculateTax amount rate=0.10>
  <#return amount * rate>
</#function>

<#function formatCurrency value>
  <#return "$" + value?string("0.00")>
</#function>

<#function getDiscountedPrice price discount>
  <#return price * (1 - discount)>
</#function>

<#-- Usage -->
<#assign tax = calculateTax(100, 0.15) />
<p>Tax: ${formatCurrency(tax)}</p>
<p>Sale Price: ${formatCurrency(getDiscountedPrice(200, 0.20))}</p>
```

### **Macros** - Output Content
Macros **generate HTML/text output** directly. They don't return values.

```freemarker
<#macro card title>
  <div class="card">
    <h3>${title}</h3>
    <#nested>
  </div>
</#macro>

<#-- Usage -->
<@card title="Hello">Content</@card>
```

---

## When to Use Functions

### **1. Calculations & Business Logic**
```freemarker
<#function calculateTotal items>
  <#assign total = 0 />
  <#list items as item>
    <#assign total = total + item.price * item.quantity />
  </#list>
  <#return total>
</#function>

<#function applyDiscount subtotal discountPercent>
  <#return subtotal * (1 - discountPercent/100)>
</#function>

<#-- Usage -->
<#assign subtotal = calculateTotal(invoice.items) />
<#assign finalTotal = applyDiscount(subtotal, 10) />
<p>Subtotal: ${subtotal}</p>
<p>After 10% discount: ${finalTotal}</p>
```

### **2. Data Validation & Checking**
```freemarker
<#function isValidEmail email>
  <#return email?matches(r"^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")>
</#function>

<#function hasItems invoice>
  <#return invoice.items?? && invoice.items?size > 0>
</#function>

<#function isPastDue dueDate>
  <#-- Simple string comparison for demo -->
  <#return dueDate < "2025-12-02">
</#function>

<#-- Usage -->
<#if hasItems(invoice)>
  <p>Items: ${invoice.items?size}</p>
<#else>
  <p>No items found</p>
</#if>
```

### **3. String Formatting & Transformation**
```freemarker
<#function truncate text maxLength>
  <#if text?length > maxLength>
    <#return text?substring(0, maxLength) + "...">
  <#else>
    <#return text>
  </#if>
</#function>

<#function toUpperCase text>
  <#return text?upper_case>
</#function>

<#function formatPhone phone>
  <#-- Format: (555) 123-4567 -->
  <#if phone?length == 10>
    <#return "(${phone?substring(0,3)}) ${phone?substring(3,6)}-${phone?substring(6)}">
  <#else>
    <#return phone>
  </#if>
</#function>

<#-- Usage -->
<p>${truncate(item.description, 50)}</p>
<p>Phone: ${formatPhone("5551234567")}</p>
```

### **4. Conditional Value Selection**
```freemarker
<#function getStatusColor status>
  <#switch status?lower_case>
    <#case "paid">
      <#return "#28a745">
    <#case "pending">
      <#return "#ffc107">
    <#case "overdue">
      <#return "#dc3545">
    <#default>
      <#return "#6c757d">
  </#switch>
</#function>

<#function getPriorityIcon priority>
  <#switch priority>
    <#case "high">
      <#return "🔴">
    <#case "medium">
      <#return "🟡">
    <#case "low">
      <#return "🟢">
    <#default>
      <#return "⚪">
  </#switch>
</#function>

<#-- Usage -->
<span style="color:${getStatusColor('paid')};">PAID</span>
<span>${getPriorityIcon('high')} High Priority</span>
```

### **5. Complex Data Processing**
```freemarker
<#function groupByCategory items>
  <#assign groups = {} />
  <#list items as item>
    <#if !groups[item.category]??>
      <#assign groups = groups + {item.category: []} />
    </#if>
    <#assign groups = groups + {item.category: groups[item.category] + [item]} />
  </#list>
  <#return groups>
</#function>

<#function sumByField items fieldName>
  <#assign sum = 0 />
  <#list items as item>
    <#assign sum = sum + item[fieldName] />
  </#list>
  <#return sum>
</#function>
```

---

## Complete Example - Functions Library 

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/functions.ftl)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_functions_demo.ftl)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_freemarker.json)

Searched for files matching `**/request_freemarker.json`, 1 match

Using "Replace String in File"

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/request_freemarker.json)

Using "Replace String in File"


```

```
Perfect! Now test the functions demo:

```bash
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_freemarker.json \
  --output invoice_functions.pdf
```

## Summary: When to Use Functions

| **Use Case** | **Function** | **Macro** |
|-------------|-------------|----------|
| Calculate totals | ✓ | ✗ |
| Format currency | ✓ | ✗ |
| Validate data | ✓ | ✗ |
| Return colors/styles | ✓ | ✗ |
| Generate HTML | ✗ | ✓ |
| Reusable components | ✗ | ✓ |
| Complex logic chains | ✓ | ✗ |

**Key Difference:**
- **Functions**: `<#return value>` - pure computation
- **Macros**: `<#nested>` - output generation

**Best Practice:** Use functions for **logic/calculations**, macros for **presentation/HTML**. Combine them:
```freemarker
<#assign total = f.calculateTotal(items) />
<@c.card title="Total">
  ${f.formatCurrency(total)}
</@c.card>
```

Made changes.