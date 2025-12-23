# CSS Optimization Automation

This directory contains tools to automatically enforce CSS optimization best practices for all FreeMarker templates.

## 🎯 Overview

Three-layer approach to ensure all templates (current and future) follow CSS optimization standards:

1. **📚 Reusable Style Library** - Pre-optimized CSS components
2. **🔧 Template Generator** - Scaffolds new templates with best practices
3. **✅ Automated Linter** - Validates templates against optimization rules

## 🚀 Quick Start

### Creating a New Template

```bash
# Generate an enrollment template
./generate-template.sh my-enrollment enrollment

# Generate a terms/legal template  
./generate-template.sh my-terms terms

# Generate a simple template
./generate-template.sh my-document simple

# Generate custom template (with style foundation)
./generate-template.sh my-custom custom
```

### Linting Templates

```bash
# Check specific files
python3 lint-templates.py src/main/resources/templates/my-template.ftl

# Check all templates
python3 lint-templates.py --all
```

### Using Style Library in Existing Templates

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

## 📚 Documentation

- **[FreeMarker Style Guide](docs/FREEMARKER_STYLE_GUIDE.md)** - Complete CSS optimization standards
- **[styles.ftl](src/main/resources/templates/styles.ftl)** - Reusable style macro library

## 🔧 Tools Reference

### 1. Style Library (`styles.ftl`)

Pre-optimized CSS macros you can import:

| Macro | CSS Lines | Use Case |
|-------|-----------|----------|
| `@completeEnrollmentStyles` | ~30 | Full enrollment documents |
| `@minimalStyles` | ~15 | Simple forms/addendums |
| `@termsStyles` | ~12 | Terms & conditions |
| `@basePageStyles` | 2 | Page setup only |
| `@headingStyles` | 3 | Headings only |
| `@tableStyles` | 5 | Tables only |
| `@summaryBoxStyles` | 4 | Summary boxes |
| `@productSectionStyles` | 3 | Product sections |

**Benefits:**
- ✅ 75% smaller CSS than hand-written
- ✅ Consistent styling across templates
- ✅ Automatically optimized (shorthand, consolidation)
- ✅ No expensive properties (position:fixed)

### 2. Template Generator (`generate-template.sh`)

Creates scaffolded templates with optimizations baked in:

```bash
./generate-template.sh <name> <type>
```

**Types:**
- `enrollment` - Full enrollment doc with summary boxes, tables, sections
- `terms` - Legal/terms document with justified text
- `simple` - Minimal template with basic table
- `custom` - Foundation with style macros, add your own CSS

**Output:**
- ✅ Pre-imports `styles.ftl`
- ✅ Uses appropriate style macro
- ✅ Includes common FreeMarker patterns
- ✅ Ready to customize with your data

### 3. Template Linter (`lint-templates.py`)

Validates templates against optimization rules:

**Checks:**
- ❌ **ERROR:** `position: fixed` usage (10-30x performance penalty)
- ⚠️ **WARNING:** Verbose margin/padding (use shorthand)
- ⚠️ **WARNING:** CSS > 50 lines (use style macros)
- ℹ️ **INFO:** Using `background-color` instead of `background`
- ℹ️ **INFO:** Not importing `styles.ftl`

**Usage:**
```bash
# Check specific files
python3 lint-templates.py file1.ftl file2.ftl

# Check all templates
python3 lint-templates.py --all

# Use in CI/CD
python3 lint-templates.py --all || exit 1
```

## 📊 Performance Impact

Templates using these tools achieve:

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| CSS Size | 40-84 lines | 10-18 lines | **70-79% smaller** |
| Render Time | 100-543ms | 13-22ms | **10-40x faster** |
| Parser Load | High | Low | **Faster startup** |

**Real Examples:**
- `enrollment-cover.ftl`: 543ms → 13ms (97.6% faster)
- `product-selection-summary.ftl`: ~200ms → ~20ms (90% faster)
- `terms-and-conditions.ftl`: ~100ms → ~22ms (78% faster)

## 🎓 Best Practices Summary

### ✅ DO

```css
/* Consolidated selectors */
h1, h2 { color: #003366; }

/* Shorthand properties */
margin: 20px 0;
padding: 15px 10px;
background: #f5f5f5;
font: bold 18px Arial;

/* Simple layouts */
.footer { margin-top: 200px; }
```

### ❌ DON'T

```css
/* Separate selectors */
h1 { color: #003366; }
h2 { color: #003366; }

/* Longhand properties */
margin-top: 20px;
margin-bottom: 0;
padding-top: 15px;
padding-bottom: 10px;
background-color: #f5f5f5;

/* Expensive properties */
.footer { position: fixed; bottom: 0; }  /* 10-30x SLOWER! */
```

## 🔄 Workflow Integration

### For New Templates

1. Run generator: `./generate-template.sh my-doc enrollment`
2. Edit template with your content/logic
3. Lint before commit: `python3 lint-templates.py my-doc.ftl`
4. Test performance (should be < 50ms warm cache)

### For Existing Templates

1. Add import: `<#import "styles.ftl" as styles>`
2. Replace `<style>` with: `<@styles.completeEnrollmentStyles />`
3. Or optimize custom CSS:
   - Consolidate selectors
   - Use shorthand properties
   - Remove `position: fixed`
4. Lint: `python3 lint-templates.py your-template.ftl`
5. Verify performance improved

### CI/CD Integration

Add to your build pipeline:

```bash
# In your CI script or pre-commit hook
python3 lint-templates.py --all || {
    echo "❌ Template linting failed"
    exit 1
}
```

## 📈 Measuring Success

Check template performance in logs:

```
Section: enrollment-cover     :     13ms ( 24.1%) - {type=freemarker, pages=1}
Section: terms                :     22ms ( 40.7%) - {type=freemarker, pages=1}
```

**Targets:**
- ✅ **< 50ms per page** (warm cache)
- ✅ **< 20 lines CSS**
- ✅ **Zero linter errors**

## 🤝 Contributing

When adding new features to the style library:

1. Add macro to `styles.ftl`
2. Update `FREEMARKER_STYLE_GUIDE.md`
3. Consider adding generator template type
4. Add linting rule if needed
5. Document performance impact

---

**Questions?** See [docs/FREEMARKER_STYLE_GUIDE.md](docs/FREEMARKER_STYLE_GUIDE.md) for complete documentation.
