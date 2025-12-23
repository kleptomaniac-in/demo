# FreeMarker Template Style Guide

## 📐 CSS Optimization Standards

All new FreeMarker templates MUST follow these CSS optimization practices for optimal performance with OpenHTMLToPDF.

### ✅ Required Practices

#### 1. Use the Styles Library
Import the optimized styles library instead of writing custom CSS:

```freemarker
<#import "styles.ftl" as styles>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <@styles.completeEnrollmentStyles />
</head>
<body>
    <!-- Your content -->
</body>
</html>
```

#### 2. CSS Consolidation
**DO:**
```css
h1, h2 { color: #003366; }
```

**DON'T:**
```css
h1 { color: #003366; }
h2 { color: #003366; }
```

#### 3. Use Shorthand Properties
**DO:**
```css
margin: 20px 0;
padding: 15px 10px;
background: #f5f5f5;
font: bold 18px Arial;
border: 1px solid #ddd;
```

**DON'T:**
```css
margin-top: 20px;
margin-bottom: 0;
margin-left: 0;
margin-right: 0;
padding-top: 15px;
padding-bottom: 15px;
padding-left: 10px;
padding-right: 10px;
background-color: #f5f5f5;
font-size: 18px;
font-weight: bold;
font-family: Arial;
border-width: 1px;
border-style: solid;
border-color: #ddd;
```

#### 4. Avoid Expensive CSS
**NEVER USE:**
- `position: fixed` (10-30x performance penalty)
- `position: absolute` (use with caution)
- Complex `transform` properties
- Heavy `box-shadow` or `text-shadow`
- Multiple background images

**USE INSTEAD:**
- Simple margin-based layouts
- Static positioning
- Simple borders for visual separation

### 📚 Available Style Macros

| Macro | Use Case | Example |
|-------|----------|---------|
| `@styles.completeEnrollmentStyles` | Full enrollment documents | Cover, terms, product selection |
| `@styles.minimalStyles` | Simple documents | Addendums, simple forms |
| `@styles.termsStyles` | Terms & conditions | Legal documents |
| `@styles.basePageStyles` | Custom builds | When you need custom CSS |
| `@styles.headingStyles` | Headings only | Add to custom styles |
| `@styles.tableStyles` | Tables only | Add to custom styles |

### 🎨 Customizing Colors

```freemarker
<#import "styles.ftl" as styles>
<style>
<@styles.basePageStyles />
<@styles.headingStyles colorPrimary="#0066cc" />
<@styles.tableStyles colorHeader="#0066cc" />
</style>
```

### 📏 CSS Size Guidelines

Target CSS size for templates:
- **Simple templates:** < 15 lines
- **Medium templates:** 15-25 lines
- **Complex templates:** 25-50 lines

If your CSS exceeds 50 lines, consider:
1. Using style macros instead
2. Consolidating selectors
3. Using shorthand properties
4. Removing redundant declarations

### 🚀 Performance Impact

Following these guidelines results in:
- ✅ **75% smaller CSS** (faster parsing)
- ✅ **10-40x faster rendering** (no position:fixed)
- ✅ **Better maintainability** (shared styles)
- ✅ **Consistent look** (reusable components)

### 📝 Template Creation Checklist

Before creating a new FreeMarker template:

- [ ] Import `styles.ftl` at the top
- [ ] Use appropriate style macro (`@styles.completeEnrollmentStyles` etc.)
- [ ] Avoid custom CSS unless absolutely necessary
- [ ] If custom CSS needed, use consolidation & shorthand
- [ ] Never use `position: fixed`
- [ ] Test performance with warm cache (should be < 50ms per page)
- [ ] Verify CSS is < 50 lines

### 🔧 Converting Existing Templates

To optimize an existing template:

1. **Import styles library:**
   ```freemarker
   <#import "styles.ftl" as styles>
   ```

2. **Replace `<style>` block:**
   ```freemarker
   <@styles.completeEnrollmentStyles />
   ```

3. **Or keep custom CSS but optimize:**
   - Consolidate selectors: `h1, h2 { color: #003366; }`
   - Use shorthand: `margin: 20px 0` instead of `margin-top: 20px; margin-bottom: 0`
   - Remove `position: fixed`
   - Use `background:` instead of `background-color:`

### 📊 Before/After Examples

**Before (84 lines, verbose):**
```css
<style>
    @page {
        size: 8.5in 11in;
        margin: 0;
    }
    body {
        font-family: Arial, sans-serif;
        margin: 40px;
        font-size: 11px;
    }
    h1 {
        font-size: 20px;
        color: #003366;
        border-bottom: 2px solid #003366;
        padding-bottom: 10px;
        margin-bottom: 20px;
    }
    /* ... 70+ more lines ... */
</style>
```

**After (1 line, optimized):**
```freemarker
<#import "styles.ftl" as styles>
<@styles.completeEnrollmentStyles />
```

### 🎯 Real-World Performance

Templates optimized using these guidelines:

| Template | Before | After | Improvement |
|----------|--------|-------|-------------|
| enrollment-cover | 543ms | 13ms | **97.6%** |
| product-selection-summary | ~200ms | ~20ms | **90%** |
| terms-and-conditions | ~100ms | ~22ms | **78%** |

---

**Last Updated:** December 23, 2025
