# Multi-Template PDF Generation - Complete Example with Headers & Footers

## Overview
This example demonstrates how to generate a PDF from **multiple template types** in a single document with **unified headers, footers, page numbers, and payload enrichers**:
- **FreeMarker** templates (HTML to PDF)
- **AcroForm** templates (PDF forms with field mapping)
- **PDFBox** generators (Programmatic PDF generation)
- **Payload Enrichers** (Extract logic from PDFBox code for reusability)
- **Headers & Footers** (Consistent across all page types)
- **Page Numbers** (Automatic "Page X of Y" numbering)

### Why Payload Enrichers?
The **CoverageSummaryEnricher** extracts business logic from the PDFBox generator, providing:
- ✅ **Reusable calculations** (ages, premiums, benefit counts)
- ✅ **Consistent data formatting** (dates, currencies)
- ✅ **Separation of concerns** (business logic vs. rendering)
- ✅ **Testable transformations** (unit test enrichers independently)
- ✅ **Template flexibility** (same enriched data for FreeMarker, PDFBox, AcroForm)

## Generated Output
✅ **4-page PDF** combining all three template types with headers, footers, and page numbers

**File Size**: 18KB  
**Features**: Headers, footers, page numbers, bookmarks, continuous numbering

## Visual Layout

```
┌─────────────────────────────────────────────────┐
│ Header: Health Insurance | App: APP-123 | Date │ ← From page 2 onwards
├─────────────────────────────────────────────────┤
│                                                 │
│              PAGE CONTENT                       │
│         (FreeMarker, AcroForm, or PDFBox)      │
│                                                 │
├─────────────────────────────────────────────────┤
│ Footer: Confidential | Page 1 of 4 | © Company │ ← On all pages
└─────────────────────────────────────────────────┘
```

## Configuration

**File**: `config-repo/multi-template.yml`

```yaml
pdfMerge:
  # ===== PAYLOAD ENRICHERS =====
  # Transform/enrich payload before template rendering
  enrichers:
    - coverageSummary     # Extracts logic from PDFBox code
                          # - Calculates ages from DOB
                          # - Aggregates premiums
                          # - Formats dates
                          # - Counts benefits/carriers
  
  settings:
    addBookmarks: true
    pageNumbering: "continuous"
  
  # ===== HEADER CONFIGURATION =====
  header:
    enabled: true
    height: 50              # Reserve 50 points for header
    startPage: 2            # Skip cover page (page 1)
    content:
      left:
        text: "Health Insurance Enrollment"
        font: "Helvetica"
        fontSize: 10
      center:
        text: "Application: {applicationNumber}"
        font: "Helvetica-Bold"
        fontSize: 11
      right:
        text: "Effective: {effectiveDate}"
        font: "Helvetica"
        fontSize: 10
    border:
      enabled: true
      color: "#003366"      # Dark blue
      thickness: 1
  
  # ===== FOOTER CONFIGURATION =====
  footer:
    enabled: true
    height: 40              # Reserve 40 points for footer
    startPage: 1            # Apply to all pages including cover
    content:
      left:
        text: "Confidential"
        font: "Helvetica-Oblique"
        fontSize: 8
      center:
        text: "Page {current} of {total}"  # Automatic page numbering
        font: "Helvetica-Bold"
        fontSize: 10
      right:
        text: "© 2025 {companyInfo.name}"  # From payload
        font: "Helvetica"
        fontSize: 8
    border:
      enabled: true
      color: "#CCCCCC"      # Light gray
      thickness: 1
  
  sections:
    # Page 1: FreeMarker Template
    - name: "cover-page"
      type: "freemarker"
      template: "templates/enrollment-cover.ftl"
      enabled: true

    # Page 2: AcroForm Template
    - name: "enrollment-form"
      type: "acroform"
      template: "templates/enrollment-form-base.pdf"
      enabled: true
      fieldMapping:
        "ApplicationNumber": "applicationNumber"
        "EffectiveDate": "effectiveDate"
        "TotalPremium": "totalPremium"
        "Primary_FirstName": "applicants[relationship=PRIMARY].demographic.firstName"
        "Primary_LastName": "applicants[relationship=PRIMARY].demographic.lastName"
        # ... more mappings

    # Page 3: PDFBox Generator
    - name: "coverage-summary"
      type: "pdfbox"
      template: "CoverageSummaryGenerator"
      enabled: true

    # Page 4: FreeMarker Template
    - name: "terms-and-conditions"
      type: "freemarker"
      template: "templates/terms-and-conditions.ftl"
      enabled: true

  bookmarks:
    - title: "Cover Page"
      page: 1
    - title: "Enrollment Form"
      page: 2
    - title: "Coverage Summary"
      page: 3
    - title: "Terms and Conditions"
      page: 4
```

## Components

### 1. Payload Enricher (NEW!)
**Location**: `src/main/java/com/example/service/enrichers/CoverageSummaryEnricher.java`

Extracts business logic from PDFBox code into a reusable component:

```java
@Component
public class CoverageSummaryEnricher implements PayloadEnricher {
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        // Calculate ages from DOB
        // Aggregate premiums per applicant
        // Count benefits and carriers
        // Format dates (2026-01-01 → "January 1, 2026")
        // Add enrichedApplicants, enrichedCoverages to payload
        return enriched;
    }
}
```

**Benefits**:
- ✅ **Logic extracted from PDFBox** - No duplication in multiple templates
- ✅ **Reusable across templates** - FreeMarker, PDFBox, AcroForm all use same enriched data
- ✅ **Testable** - Unit test enrichers independently
- ✅ **Maintainable** - Business logic in one place

**What it enriches**:
| Original | Enriched | Use Case |
|----------|----------|----------|
| `dateOfBirth: "1985-03-15"` | `calculatedAge: 39` | Show age instead of DOB |
| `firstName`, `lastName` | `displayName: "John Doe"` | Formatted names |
| Multiple `products[].premium` | `totalApplicantPremium: "450.00"` | Per-applicant totals |
| Raw date `2026-01-01` | `formattedEffectiveDate: "January 1, 2026"` | Human-readable dates |
| List of coverages | `totalBenefits: 12`, `totalCarriers: 2` | Summary counts |

### 2. FreeMarker Templates
**Location**: `src/main/resources/templates/*.ftl`

**enrollment-cover.ftl**: Cover page with application information
**terms-and-conditions.ftl**: Legal terms with dynamic content

### 3. AcroForm Template  
**Location**: `config-repo/acroforms/templates/enrollment-form-base.pdf`

PDF with form fields that get filled with data using field mappings.

**Field Mapping Features**:
- Direct field mapping: `"FieldName": "payload.path"`
- Array filtering: `applicants[relationship=PRIMARY]`
- Nested object access: `.demographic.firstName`

### 3. PDFBox Generator
**Location**: `src/main/java/com/example/generator/CoverageSummaryGenerator.java`

```java
@Component("CoverageSummaryGenerator")
public class CoverageSummaryGenerator implements PdfBoxGenerator {
    @Override
    public String getName() {
        return "CoverageSummaryGenerator";
    }

    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        // Programmatically create PDF content
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        
        // Draw content using PDFBox API
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // ... drawing code
        }
        
        return document;
    }
}
```

## API Usage

### Request Format
```bash
curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "multi-template",
    "outputFileName": "enrollment.pdf",
    "payload": {
      "applicationNumber": "APP-2025-12345",
      "effectiveDate": "2026-01-01",
      "applicants": [...],
      "coverages": [...],
      "companyInfo": {...}
    }
  }' \
  --output enrollment.pdf
```

### Test Files
- **Config**: `config-repo/multi-template.yml`
- **Request**: `test-multi-template-request.json`
- **Payload**: `test-multi-template-payload.json` (embedded in request)

## Test Command
```bash
cd /workspaces/demo/demoproject/pdf-generation-service

curl -X POST http://localhost:8080/api/document/generate \
  -H "Content-Type: application/json" \
  -d @test-multi-template-request.json \
  --output multi-template-output.pdf
```

## Expected Output
```
multi-template-output.pdf: PDF document, version 1.4, 4 page(s)
File size: ~18KB
```

**Page Layout**:
- **Page 1**: Cover page (NO header, footer with page numbers)
- **Page 2**: Enrollment form (header + footer)
- **Page 3**: Coverage summary (header + footer)
- **Page 4**: Terms & conditions (header + footer)

## Key Features Demonstrated

### 1. **Headers & Footers**
- **Three-column layout**: Left, center, right positioning
- **Variable substitution**: `{applicationNumber}`, `{effectiveDate}`, `{companyInfo.name}`
- **Built-in variables**: `{current}` (current page), `{total}` (total pages)
- **Page control**: Headers start from page 2, footers on all pages
- **Borders**: Visual separation lines with custom colors

### 2. **Automatic Page Numbers**
```yaml
center:
  text: "Page {current} of {total}"  # Displays: "Page 2 of 4"
```

### 3. **Dynamic Content in Headers/Footers**
Headers and footers pull data directly from your payload:
```json
{
  "applicationNumber": "APP-2025-12345",
  "effectiveDate": "2026-01-01",
  "companyInfo": {
    "name": "Acme Insurance Company"
  }
}
```

Results in:
- Header center: "Application: APP-2025-12345"
- Header right: "Effective: 2026-01-01"
- Footer right: "© 2025 Acme Insurance Company"

### 4. **Mixed Template Types**
Combine different PDF generation approaches in a single document

### 5. **AcroForm Field Mapping**
- Filter arrays by field values
- Access nested objects
- Pattern-based field generation (supports `{n}` placeholders)

### 6. **PDFBox Programmatic Generation**
Full control over PDF content using PDFBox API

### 7. **FreeMarker HTML Templates**
- Dynamic content injection
- Conditional rendering
- Loops and data iteration

### 8. **Document Settings**
- Bookmarks for navigation
- Continuous page numbering
- Enable/disable sections

## Header/Footer Configuration Details

### Available Variables

**Built-in Variables**:
- `{current}` - Current page number (1, 2, 3, ...)
- `{total}` - Total number of pages
- `{date}` - Current date (YYYY-MM-DD format)

**Payload Variables**:
Any field from your JSON payload:
- `{applicationNumber}` → `payload.applicationNumber`
- `{effectiveDate}` → `payload.effectiveDate`
- `{companyInfo.name}` → `payload.companyInfo.name`

### Font Options
- `Helvetica` (default, clean sans-serif)
- `Helvetica-Bold` (bold sans-serif)
- `Helvetica-Oblique` (italic sans-serif)
- `Times-Roman` (classic serif)
- `Times-Bold` (bold serif)
- `Courier` (monospace)

### Border Styles
```yaml
border:
  enabled: true
  color: "#003366"    # Hex color (dark blue)
  thickness: 1        # Line thickness in points
```

**Common Colors**:
- `#000000` - Black
- `#003366` - Dark blue
- `#CCCCCC` - Light gray
- `#666666` - Dark gray

### Page Control Examples

**Skip cover page (common pattern)**:
```yaml
header:
  startPage: 2  # Header from page 2 onwards
footer:
  startPage: 1  # Footer on all pages
```

**Headers/footers on all pages**:
```yaml
header:
  startPage: 1
footer:
  startPage: 1
```

**No headers/footers on first two pages**:
```yaml
header:
  startPage: 3
footer:
  startPage: 3
```

## Benefits of Unified Headers/Footers

### 1. **Consistency Across All Sources**
Headers and footers look identical whether the page comes from:
- FreeMarker HTML templates
- PDFBox programmatic generation
- AcroForm PDF forms

### 2. **Centralized Management**
Change header/footer design once in YAML config - applies to all pages

### 3. **No Template Changes Required**
Your FreeMarker templates and PDFBox generators don't need any header/footer code

### 4. **Dynamic Page Numbers**
Automatic "Page X of Y" calculation - no manual tracking needed

### 5. **Selective Application**
- Bookmarks for navigation
- Continuous page numbering
- Enable/disable sections

## Creating AcroForm Templates

### Method 1: Using PDFBox (as demonstrated)
See `CreateAcroFormPDF.java` for programmatic creation

### Method 2: Using Adobe Acrobat
1. Create PDF layout
2. Add form fields with specific names
3. Save as PDF with AcroForm

### Method 3: Using LibreOffice
1. Create form in Writer
2. Export as PDF with "Create PDF Form" option

## Directory Structure
```
demoproject/
├── config-repo/
│   ├── multi-template.yml          # Configuration
│   └── acroforms/
│       └── templates/
│           └── enrollment-form-base.pdf  # AcroForm template
├── pdf-generation-service/
│   └── src/main/
│       ├── java/com/example/
│       │   └── generator/
│       │       └── CoverageSummaryGenerator.java  # PDFBox generator
│       └── resources/
│           └── templates/
│               ├── enrollment-cover.ftl        # FreeMarker
│               └── terms-and-conditions.ftl    # FreeMarker
```

## Troubleshooting

### Issue: "PDF does not contain an AcroForm"
**Solution**: Ensure the AcroForm PDF has actual form fields defined

### Issue: "No PDFBox generator found"
**Solution**: Verify the generator class is annotated with `@Component` and the name matches

### Issue: "Config not found"
**Solution**: Check that `.yml` file is in `config-repo/` directory

### Issue: "acroforms/templates/*.pdf not found"
**Solution**: Place AcroForm PDFs in `config-repo/acroforms/templates/`

## Benefits of Multi-Template Approach

1. **Reusability**: Use existing PDF forms without recreation
2. **Flexibility**: Mix static content, dynamic content, and programmatic generation
3. **Maintainability**: Update individual sections independently
4. **Performance**: Cache templates and generators separately
5. **Designer-Friendly**: Non-developers can create FreeMarker templates and PDF forms

## Advanced Features

### Pattern-Based Field Mapping
```yaml
patterns:
  - fieldPattern: "Dependent{n}_*"
    source: "applicants[relationship=DEPENDENT][{n}]"
    maxIndex: 2
    fields:
      FirstName: "demographic.firstName"
      LastName: "demographic.lastName"
      DOB: "demographic.dateOfBirth"
```

Expands to:
- `Dependent0_FirstName` → `applicants[relationship=DEPENDENT][0].demographic.firstName`
- `Dependent1_FirstName` → `applicants[relationship=DEPENDENT][1].demographic.firstName`
- `Dependent2_FirstName` → `applicants[relationship=DEPENDENT][2].demographic.firstName`

### Static Values
```yaml
fieldMapping:
  "FormTitle": "static:Health Insurance Enrollment Form"
  "FormVersion": "static:v2.0"
```

### Conditional Sections
```yaml
conditionalSections:
  - condition: "payload.hasSpouse == true"
    sections:
      - name: "spouse-details"
        type: "freemarker"
        template: "templates/spouse-section.ftl"
        insertAfter: "cover-page"
```

## Summary

✅ **4-page PDF generated successfully**
- Page 1: FreeMarker cover page
- Page 2: AcroForm enrollment form (with 15 form fields filled)
- Page 3: PDFBox coverage summary (programmatically generated)
- Page 4: FreeMarker terms and conditions

**File Size**: 15KB  
**Format**: PDF 1.4  
**Features**: Bookmarks, continuous numbering, mixed content types

## Header/Footer Configuration Details

### Available Variables

**Built-in Variables**:
- `{current}` - Current page number (1, 2, 3, ...)
- `{total}` - Total number of pages
- `{date}` - Current date (YYYY-MM-DD format)

**Payload Variables**:
Any field from your JSON payload:
- `{applicationNumber}` → `payload.applicationNumber`
- `{effectiveDate}` → `payload.effectiveDate`
- `{companyInfo.name}` → `payload.companyInfo.name`

### Font Options
- `Helvetica` (default, clean sans-serif)
- `Helvetica-Bold` (bold sans-serif)
- `Helvetica-Oblique` (italic sans-serif)
- `Times-Roman` (classic serif)
- `Times-Bold` (bold serif)
- `Courier` (monospace)

### Border Styles
```yaml
border:
  enabled: true
  color: "#003366"    # Hex color (dark blue)
  thickness: 1        # Line thickness in points
```

**Common Colors**:
- `#000000` - Black
- `#003366` - Dark blue
- `#CCCCCC` - Light gray
- `#666666` - Dark gray

### Page Control Examples

**Skip cover page (common pattern)**:
```yaml
header:
  startPage: 2  # Header from page 2 onwards
footer:
  startPage: 1  # Footer on all pages
```

**Headers/footers on all pages**:
```yaml
header:
  startPage: 1
footer:
  startPage: 1
```

**No headers/footers on first two pages**:
```yaml
header:
  startPage: 3
footer:
  startPage: 3
```

## Benefits of Unified Headers/Footers

### 1. **Consistency Across All Sources**
Headers and footers look identical whether the page comes from FreeMarker, PDFBox, or AcroForm

### 2. **Centralized Management**
Change header/footer design once in YAML config - applies to all pages

### 3. **No Template Changes Required**
Your FreeMarker templates and PDFBox generators don't need any header/footer code

### 4. **Dynamic Page Numbers**
Automatic "Page X of Y" calculation - no manual tracking needed

### 5. **Selective Application**
Control which pages get headers/footers (e.g., skip cover pages)

## Complete Example Result

✅ **18KB PDF with 4 pages, headers, footers, and page numbers**

**Page Layout**:
- **Page 1**: Cover (footer only) - "Page 1 of 4"
- **Page 2**: Enrollment form (header + footer) - "Page 2 of 4"  
- **Page 3**: Coverage summary (header + footer) - "Page 3 of 4"
- **Page 4**: Terms (header + footer) - "Page 4 of 4"

**Header** (pages 2-4):
- Left: "Health Insurance Enrollment"
- Center: "Application: APP-2025-12345"
- Right: "Effective: 2026-01-01"
- Dark blue border (#003366)

**Footer** (all pages):
- Left: "Confidential"
- Center: "Page X of 4"
- Right: "© 2025 Acme Insurance Company"
- Light gray border (#CCCCCC)

## Troubleshooting Headers/Footers

### Headers/Footers Not Appearing
1. Check `enabled: true` in config
2. Verify `startPage` ≤ total pages
3. Restart service after config changes

### Variables Not Replaced
1. Variable names are case-sensitive
2. Use dot notation for nested: `{companyInfo.name}`
3. Verify payload structure matches

### Content Overlaps
1. Increase header/footer `height`
2. Add CSS margins to FreeMarker templates
3. Adjust PDFBox Y-positions

