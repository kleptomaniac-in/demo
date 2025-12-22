# Template System Architecture Summary

## Overview

Unified configuration-driven template system supporting both PDF and Excel with consistent patterns.

---

## System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     Template System                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐              ┌──────────────────┐        │
│  │   PDF Templates  │              │ Excel Templates  │        │
│  └──────────────────┘              └──────────────────┘        │
│           │                                  │                   │
│           ├─ AcroForm (field mapping)       ├─ Cell mapping    │
│           ├─ FreeMarker (HTML → PDF)        ├─ Table mapping   │
│           └─ PDFBox (programmatic)          └─ Named ranges    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          Common Services (Shared)                         │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  • ConfigurablePayloadPreProcessor                       │  │
│  │  • Path Resolution (dots, arrays, filters)               │  │
│  │  • YAML Configuration Loading                            │  │
│  │  • Composition Engine (base + components)                │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Configuration Architecture

### PDF Configuration

**Location:** `config-repo/*.yml`

```yaml
# PDF Merge Config
pdfMerge:
  sections:
    - name: enrollment-application
      type: acroform
      template: enrollment-form.pdf
      fieldMapping:
        "Primary_FirstName": "primary.firstName"
        "Dependent1_FirstName": "dependent1.firstName"
```

### Excel Configuration

**Location:** `config-repo/excel/*.yml`

```yaml
# Excel Merge Config
templatePath: "enrollment-summary.xlsx"
preprocessingRules: "standard-enrollment-rules.yml"
cellMappings:
  PrimaryFirstName: "primary.firstName"
  Dependent1FirstName: "dependent1.firstName"
tableMappings:
  - sheetName: "Dependents"
    sourcePath: "allDependents"
    columnMappings:
      0: "firstName"
```

---

## Parallel Features

| Feature | PDF Implementation | Excel Implementation |
|---------|-------------------|---------------------|
| **Configuration** | `PdfMergeConfig.java` | `ExcelMergeConfig.java` |
| **Config Service** | `PdfMergeConfigService.java` | `ExcelMergeConfigService.java` |
| **Simple Mapping** | `AcroFormFillService.java` | Cell mappings |
| **Repeating Data** | FreeMarker loops | Table mappings |
| **Preprocessing** | ✅ Shared service | ✅ Shared service |
| **Path Resolution** | ✅ Shared logic | ✅ Shared logic |
| **Composition** | ✅ base + components | ✅ base + components |
| **API Endpoint** | `/api/pdf/generate-from-config` | `/api/excel/generate-from-config` |

---

## Request Flow

### PDF Generation
```
1. Client → POST /api/pdf/generate-from-config
   Body: { configName, payload }

2. Controller → PdfMergeConfigService.loadConfig()
   Loads YAML, applies composition

3. Controller → ConfigurablePayloadPreProcessor.preProcess()
   (if preprocessingRules specified)

4. Controller → FlexiblePdfMergeService.generateMergedPdf()
   Generates PDF sections, merges, adds headers/footers

5. Response → PDF bytes
```

### Excel Generation
```
1. Client → POST /api/excel/generate-from-config
   Body: { configName, payload }

2. Controller → ExcelMergeConfigService.loadConfig()
   Loads YAML, applies composition

3. Controller → ConfigurablePayloadPreProcessor.preProcess()
   (if preprocessingRules specified)

4. Controller → ExcelTemplateService.fillExcelTemplate()
   or fillExcelWithTables()
   Fills cells/tables with data

5. Response → Excel bytes
```

---

## Common Patterns

### 1. Cell/Field Mapping

**PDF (AcroForm):**
```yaml
fieldMapping:
  "PDF_Field_Name": "payload.path"
```

**Excel:**
```yaml
cellMappings:
  CellReference: "payload.path"
```

### 2. Repeating Data

**PDF (FreeMarker):**
```freemarker
<#list allDependents as dep>
  ${dep.firstName}
</#list>
```

**Excel (Table Mapping):**
```yaml
tableMappings:
  - sourcePath: "allDependents"
    columnMappings:
      0: "firstName"
```

### 3. Preprocessing

**Both use same rules:**
```yaml
arrayFilters:
  - sourcePath: "application.applicants"
    filterField: "relationship"
    filterValue: "DEPENDENT"
    targetKey: "dependent"
    mode: "indexed"
    maxItems: 3
```

**Result:**
```
dependent1, dependent2, dependent3, dependentOverflow
```

### 4. Path Resolution

**Both support same syntax:**
```
"member.name"                           → payload.member.name
"items[0].value"                        → payload.items[0].value
"items[status=ACTIVE].value"            → First item where status=ACTIVE
"items[type=X][category=Y].value"       → First item matching both filters
```

### 5. Composition

**PDF:**
```yaml
composition:
  base: "base-pdf.yml"
  components:
    - "medical-pdf.yml"
    - "dental-pdf.yml"
```

**Excel:**
```yaml
composition:
  base: "excel/components/base-excel.yml"
  components:
    - "excel/components/dependents-excel.yml"
```

**Both use deep merge algorithm!**

---

## API Endpoints

### PDF Endpoints

| Endpoint | Purpose |
|----------|---------|
| `POST /api/pdf/generate-merged` | Legacy: Config name in request |
| `POST /api/enrollment/generate` | Convention-based config selection |
| `POST /api/enrollment/generate-with-rules` | Rule-based config selection |

### Excel Endpoints

| Endpoint | Purpose |
|----------|---------|
| `POST /api/excel/generate-from-config` ⭐ | **Recommended: YAML config** |
| `POST /api/excel/generate` | Legacy: Mappings in request |
| `POST /api/excel/generate-with-preprocessing` | Legacy: With preprocessing |
| `POST /api/excel/generate-with-tables` | Legacy: Table data |
| `POST /api/excel/generate-complete` | Legacy: Complete request |

---

## Configuration Files

### Directory Structure
```
config-repo/
├── base-application.yml                 # PDF base config
├── medical-individual-ca.yml            # PDF product config
├── enrollment-summary-excel.yml         # Excel config (legacy location)
├── preprocessing/
│   ├── standard-enrollment-rules.yml
│   └── client-b-rules.yml
├── excel/
│   ├── enrollment-summary-excel.yml     # Excel config (new location)
│   ├── dependent-list-excel.yml
│   ├── enrollment-complete-excel.yml
│   └── components/
│       ├── base-excel.yml
│       ├── dependents-excel.yml
│       └── medical-excel.yml
├── acroforms/
│   └── enrollment-application.pdf
└── excel-templates/
    ├── enrollment-summary.xlsx
    └── dependent-list.xlsx
```

---

## Type Support

### PDF
- ✅ AcroForm field types (Text, Date, Checkbox, Radio)
- ✅ Type conversion (Boolean→Yes/No, Date→MM/dd/yyyy)
- ✅ HTML rendering (via FreeMarker + OpenHTMLToPDF)

### Excel
- ✅ Excel cell types (Text, Number, Date, Boolean, Formula)
- ✅ Type conversion (Boolean→TRUE/FALSE, Date→MM/dd/yyyy)
- ✅ Preserves Excel formatting

---

## Advanced Features

### PDF-Specific
- Multi-section merging
- Page numbers
- Headers/Footers
- Bookmarks
- Table of contents
- Conditional sections

### Excel-Specific
- Multiple sheets
- Named ranges
- Formulas preserved
- Formatting preserved
- Cell styles maintained

### Shared Features
- ✅ Preprocessing with array filtering (AND/OR logic)
- ✅ 10 filter operators
- ✅ Composition with deep merge
- ✅ Path resolution with filters
- ✅ Multi-client support via YAML rules
- ✅ Overflow handling (4+ dependents)

---

## Migration Path

### Phase 1: Configuration Files (✅ Complete)
- Created YAML config models
- Created config loading services
- Added composition support

### Phase 2: API Updates (✅ Complete)
- Added config-based endpoints
- Maintained backward compatibility
- Updated controllers

### Phase 3: Documentation (✅ Complete)
- Excel template guide
- Excel config quick reference
- Architecture summary
- Example requests

### Phase 4: Deprecation (Future)
- Mark request-based endpoints as deprecated
- Migrate existing clients to config-based approach
- Remove legacy endpoints (after grace period)

---

## Benefits of Unified Architecture

1. **Consistency**
   - Same patterns for PDF and Excel
   - Same preprocessing rules
   - Same path resolution
   - Same composition approach

2. **Maintainability**
   - Configuration in version control
   - No hardcoded mappings in code
   - Reusable components

3. **Flexibility**
   - Easy to add new templates
   - Multi-client support
   - Dynamic configuration

4. **Developer Experience**
   - Learn once, use for both
   - Consistent API patterns
   - Clear documentation

---

## Summary

**The system now provides:**
- ✅ Unified configuration approach (PDF + Excel)
- ✅ YAML-based configs with composition
- ✅ Shared preprocessing and path resolution
- ✅ Backward compatible with request-based approach
- ✅ Production-ready with comprehensive documentation

**Recommended Usage:**
- Use YAML configs for all production templates
- Use composition to avoid duplication
- Use preprocessing for complex structures
- Use request-based endpoints only for quick testing
