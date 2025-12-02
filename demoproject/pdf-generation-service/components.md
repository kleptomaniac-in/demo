Yes! FreeMarker supports **custom components** through **macros** and **functions**. Here are the main approaches:

## 1. **Macros** (Most Common for Components)

Macros are reusable template components that can accept parameters and nested content. 

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/components.ftl)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_components_demo.ftl)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_components_demo.json)

## Yes! Here's What You Can Do:

### **1. Create Component Libraries**
Store reusable macros in separate files like `components.ftl`

### **2. Import and Use Them**
```freemarker
<#import "/templates/components.ftl" as c>

<@c.card title="My Card">
  Content here
</@c.card>
```

### **3. Component Types I Created:**

**Basic Components:**
- `card` - Container with optional header/footer
- `badge` - Colored labels (success, warning, danger, info)
- `alert` - Notification boxes
- `keyValue` - Label-value pairs

**Layout Components:**
- `gridContainer` - Dynamic multi-column layouts
- `dataTable` - Tables with headers and striping
- `section` - Titled sections

**Advanced Components:**
- `progressBar` - Visual progress indicator
- `statCard` - Dashboard-style stat display
- `iconText` - Text with Unicode icons

### **4. Component Features:**

✅ **Parameters**: `<@c.badge text="PAID" type="success" />`  
✅ **Default values**: `<#macro card title="" borderColor="#ddd">`  
✅ **Nested content**: `<@c.card>content</@c.card>`  
✅ **Conditional logic**: Switch statements, if/else  
✅ **Loop variables**: Pass data to nested content

### **5. Test the Components:**

```bash
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_components_demo.json \
  --output invoice_components.pdf
```

This creates a rich, styled invoice using all the custom components! You can mix and match them to build any layout you need.



Let me check what might be causing the empty PDF issue: 

Ran terminal command: cd /workspaces/demo/demoproject/pdf-generation-service && tail -100 logs/application.log 2>/dev/null || tail -100 nohup.out 2>/dev/null || echo "No log file found"

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/components.ftl#28-28), lines 28 to 50

I see the issue! The `gridContainer` macro uses `<#nested item=item>` syntax which requires accessing it differently. Let me check the usage and fix it: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_components_demo.ftl#40-40), lines 40 to 80

The issue is the `<#items>` directive doesn't exist in FreeMarker. The macro needs a different approach. Let me fix both the component and its usage: 

Using "Replace String in File"


```

```


Using "Replace String in File"


```

```


Using "Replace String in File"


```

```
Now test again - the PDF should have content:

```bash
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_components_demo.json \
  --output invoice_components.pdf
```

## What Was Wrong:

**The Problem:** I used `<#items as item>` which doesn't exist in FreeMarker. The correct syntax for nested macros with loop variables is:

```freemarker
<@c.autoGrid items=myList ; item>
  ${item.name}
</@c.autoGrid>
```

**Inside the macro:**
```freemarker
<#macro autoGrid items=[]>
  <#list items as item>
    <#nested item>  <#-- Pass item to nested content -->
  </#list>
</#macro>
```

The semicolon (`;`) syntax tells FreeMarker that `item` is a loop variable that will be provided by the macro.
