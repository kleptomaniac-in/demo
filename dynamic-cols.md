kleptomaniac-in: If we have content depending on which we may need a 2 column, 3 column or even single row or multiple rows layout how could I do that without creating multiple freemarker template files?


GitHub Copilot: I'll help you create a dynamic layout system in FreeMarker that can handle different column layouts without needing multiple template files. Let me first examine your current template structure. 

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

kleptomaniac-in: I am getting error, "2025-12-02T18:34:34.495Z ERROR 44465 --- [pdf-generation-service] [nio-8080-exec-1] freemarker.runtime                       : Error executing FreeMarker template

freemarker.core.InvalidReferenceException: The following has evaluated to null or missing:
==> item  [in template "templates/macros.ftl" at line 57, column 20]

----
Tip: If the failing expression is known to legally refer to something that's sometimes null or missing, either specify a default value like myOptionalVar!myDefault, or use <#if myOptionalVar??>when-present<#else>when-missing</#if>. (These only cover the last step of the expression; to cover the whole expression, use parenthesis: (myOptionalVar.foo)!myDefault, (myOptionalVar.foo)??
----

----
FTL stack trace ("~" means nesting-related):
        - Failed at: #nested item = it  [in template "templates/macros.ftl" in macro "columnGrid" at line 57, column 11]
        - Reached through: @m.columnGrid items=invoice.items col...  [in template "templates/invoice_dynamic_layout.ftl" at line 18, column 3]
        ~ Reached through: #nested  [in template "templates/layouts/detailed/layout.ftl" in macro "layout" at line 19, column 5]
        ~ Reached through: @layout.layout title="Invoice ${invoi...  [in template "templates/invoice_dynamic_layout.ftl" at line 13, column 1]
----
        at freemarker.core.InvalidReferenceException.getInstance(InvalidReferenceException.java:134) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.EvalUtil.compare(EvalUtil.java:198) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.EvalUtil.compare(EvalUtil.java:115) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.ComparisonExpression.evalToBoolean(ComparisonExpression.java:78) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.BooleanExpression._eval(BooleanExpression.java:30) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Expression.eval(Expression.java:101) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.BodyInstruction$Context.<init>(BodyInstruction.java:132) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.BodyInstruction.accept(BodyInstruction.java:59) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.visit(Environment.java:371) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.IteratorBlock$IterationContext.executedNestedContentForCollOrSeqListing(IteratorBlock.java:321) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.IteratorBlock$IterationContext.executeNestedContent(IteratorBlock.java:271) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.IteratorBlock$IterationContext.accept(IteratorBlock.java:244) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.visitIteratorBlock(Environment.java:645) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.IteratorBlock.acceptWithResult(IteratorBlock.java:108) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.IteratorBlock.accept(IteratorBlock.java:94) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.visit(Environment.java:335) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.visit(Environment.java:377) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.invokeMacroOrFunctionCommonPart(Environment.java:877) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.invokeMacro(Environment.java:813) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.UnifiedCall.accept(UnifiedCall.java:84) ~[freemarker-2.3.32.jar:2.3.32]
        at freemarker.core.Environment.visit(Environment.java:371) ~[freem"

GitHub Copilot: The error is in how the `columnGrid` macro is being called. The nested content syntax is incorrect. Let me fix the template: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/macros.ftl#30-30), lines 30 to 63

I see the issue. The macro expects `<#nested item=it />` but you need to access it as a loop variable. Let me fix the templates: 

Using "Replace String in File"


```

```
Wait, that's not right either. Let me check how the macro actually works and fix it properly: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_page_grid.ftl)

I see - your current working template doesn't use the macro. The macro has a complex nested structure. Let me simplify and create a working version: 

Using "Replace String in File"


```

```


Using "Replace String in File"


```

```
Fixed! The issue was with the macro invocation syntax. I've simplified the templates to use direct FreeMarker logic instead of the complex nested macro syntax. Now try again:

```bash
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_3column_layout.json \
  --output invoice_3col.pdf
```

The templates now:
- Use standard `<#list>` loops instead of the complex nested macro
- Support both **grid** and **table** modes
- Handle dynamic column counts (1, 2, 3, or more)
- Work with multiple sections having different layouts

Made changes.

kleptomaniac-in: Can you explain how it works to better understand it

GitHub Copilot: Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_dynamic_layout.ftl#1-1), lines 1 to 60

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/request_3column_layout.json)

## How the Dynamic Layout Works

### **1. Configuration Extraction (Lines 8-10)**

```freemarker
<#assign columnCount = (payload.columns?default("2"))?number>
<#assign layoutMode = payload.layoutMode?default("grid")>
<#assign gridGap = payload.gap?default("10px")>
```

**What happens:**
- Reads `payload.columns` from JSON (e.g., `"3"`) → converts to number `3`
- Reads `payload.layoutMode` (e.g., `"grid"`) → defaults to `"grid"` if missing
- Reads `payload.gap` (e.g., `"15px"`) → defaults to `"10px"` if missing

**From JSON:**
```json
"columns": "3",
"layoutMode": "grid",
"gap": "15px"
```

---

### **2. Layout Mode Selection (Line 17)**

```freemarker
<#if layoutMode == "table">
  <!-- table HTML -->
<#else>
  <!-- CSS Grid HTML -->
</#if>
```

**Two rendering modes:**

#### **Mode A: Table Layout** (lines 19-40)
Used when `layoutMode = "table"`

```freemarker
<#list invoice.items?chunk(columnCount) as row>
```

**The magic: `?chunk(columnCount)`**
- Splits items into groups of size `columnCount`
- If you have 5 items and `columnCount=3`: `[[item1, item2, item3], [item4, item5]]`

**Example with 5 items, 3 columns:**
```
Row 1: [Web Development, API Integration, Testing]
Row 2: [Documentation, Support]
```

**Padding logic (lines 33-37):**
```freemarker
<#if (row?size < columnCount)>
  <#list 1..(columnCount - row?size) as _>
    <td>&nbsp;</td>
  </#list>
</#if>
```
- If Row 2 has only 2 items but needs 3 columns → adds 1 empty `<td>` cell
- Ensures all rows have the same number of columns

---

#### **Mode B: CSS Grid Layout** (lines 42-54)
Used when `layoutMode = "grid"` (default)

```freemarker
<div style="display:grid; grid-template-columns:repeat(${columnCount}, 1fr); gap:${gridGap};">
```

**CSS Grid magic:**
- `repeat(3, 1fr)` → creates 3 equal-width columns
- `gap:15px` → adds 15px space between items
- **No manual row management needed** - browser handles wrapping automatically

**With 4 items and 3 columns:**
```
┌────────────┬────────────┬────────────┐
│   Item 1   │   Item 2   │   Item 3   │
├────────────┴────────────┴────────────┤
│   Item 4   │            │            │
└────────────┴────────────┴────────────┘
```

---

### **3. Data Flow Example**

**JSON Request:**
```json
{
  "payload": {
    "columns": "3",
    "layoutMode": "grid",
    "gap": "15px"
  }
}
```

**Processing Steps:**

1. **Line 8:** `columnCount = 3`
2. **Line 9:** `layoutMode = "grid"`
3. **Line 10:** `gridGap = "15px"`
4. **Line 17:** Condition checks `"grid" == "table"` → FALSE → goes to ELSE block
5. **Line 44:** Generates CSS:
   ```html
   <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:15px;">
   ```
6. **Lines 45-52:** Loops through each item and creates a `<div>` for each

**Final HTML output:**
```html
<div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:15px;">
  <div style="border:1px solid #ddd; padding:8px;">
    <div><strong>Web Development</strong></div>
    <div>$200.00</div>
    <div style="font-size:0.9em; color:#666;">Qty: 1</div>
  </div>
  <div><!-- API Integration --></div>
  <div><!-- Testing --></div>
  <div><!-- Documentation --></div>
</div>
```

---

### **4. Key FreeMarker Techniques**

**`?default("value")`** - Provides fallback values:
```freemarker
${payload.columns?default("2")}  // Returns "2" if columns is null/missing
```

**`?number`** - Converts string to number:
```freemarker
${"3"?number}  // "3" → 3
```

**`?chunk(n)`** - Splits list into groups:
```freemarker
[1,2,3,4,5]?chunk(2) → [[1,2], [3,4], [5]]
```

**`?size`** - Gets list/array length:
```freemarker
[1,2,3]?size  // Returns 3
```

**`??`** - Checks if value exists:
```freemarker
<#if item.quantity??>  // Only renders if quantity is present
```

---

### **5. Why This Approach Works**

✅ **Single template** handles 1, 2, 3, or any number of columns  
✅ **Two layout modes** (grid for modern, table for compatibility)  
✅ **JSON controls everything** - no code changes needed  
✅ **Graceful defaults** - works even with minimal JSON  
✅ **Optional fields** - handles missing data with `??` checks

**To change from 3 columns to 5 columns:** Just change `"columns": "5"` in JSON - no template edits needed!