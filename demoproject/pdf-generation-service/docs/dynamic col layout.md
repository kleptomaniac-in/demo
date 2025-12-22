I'll help you create a dynamic layout system in FreeMarker that can handle different column layouts without needing multiple template files. Let me first examine your current template structure. 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/request_invoice_detailed.json)

Searched for files matching `**/*.ftl`, 9 matches

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_page_grid.ftl)

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/macros.ftl#1-1), lines 1 to 50

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/macros.ftl#51-51), lines 51 to 63

Perfect! I can see you already have a `columnGrid` macro in your macros.ftl file. Now let me show you how to use this dynamic approach in your templates. Here's the solution:

## Dynamic Layout Strategy

You can use **conditional logic** and **macros** in a single FreeMarker template to handle different column layouts. Here are the key techniques:

### 1. **Using the Existing `columnGrid` Macro**

Your macros.ftl already has a powerful `columnGrid` macro that accepts:
- `columns` parameter (1, 2, 3, or more)
- `gap` for spacing
- `mode` ("grid" or "table")

Here's how to use it: 

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_dynamic_layout.ftl)

### 2. **Advanced: Conditional Section Layouts**

For even more flexibility, you can create sections with different layouts in the same template: 

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_multi_section.ftl)

### 3. **Sample JSON Payloads**

Now let me create example JSON files showing how to use different layouts: 

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_3column_layout.json)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_single_column.json)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_multi_section.json)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_table_layout.json)

## Summary

You can handle dynamic layouts **without multiple template files** using these strategies:

### **Key Techniques:**

1. **Pass column count in payload**: `"columns": "3"` controls grid columns
2. **Use the `columnGrid` macro**: Already exists in your macros.ftl with support for:
   - Variable column counts (1, 2, 3, or more)
   - Grid or table mode
   - Custom gap spacing
3. **Conditional sections**: Use `<#if>` to show/hide sections based on data
4. **Per-section layouts**: Different sections can have different column counts

### **What I Created:**

1. **`invoice_dynamic_layout.ftl`** - Simple dynamic layout using macro parameters
2. **`invoice_multi_section.ftl`** - Advanced multi-section with different layouts per section
3. **4 example JSON files** demonstrating:
   - 3-column grid layout
   - Single column layout
   - Multi-section with 2-column items + 3-column details
   - Table-based 2-column layout

### **Test It:**

```bash
# 3 columns
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_3column_layout.json \
  --output invoice_3col.pdf

# Single column
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_single_column.json \
  --output invoice_1col.pdf

# Multi-section
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_multi_section.json \
  --output invoice_multi.pdf
```

The payload controls everything - one template handles all layouts!

Made changes.