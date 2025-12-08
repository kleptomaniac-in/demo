# Flexible PDF Merge Configuration

## Overview
This system allows you to configure PDF generation by combining FreeMarker HTML pages and PDFBox dynamic pages using YAML configuration.

## Architecture

```
YAML Config → PdfMergeConfigService → FlexiblePdfMergeService
                                      ├── FreeMarker + openhtmltopdf
                                      └── PDFBox Generators
                                      ↓
                                   Merged PDF
```

## YAML Configuration Structure

### Basic Section
```yaml
pdfMerge:
  sections:
    - name: "Section Name"
      type: freemarker | pdfbox
      template: "template-name"
      enabled: true | false
```

### Conditional Sections
```yaml
conditionalSections:
  - condition: "payload.fieldName"
    sections:
      - name: "Conditional Section"
        type: freemarker
        template: "template.ftl"
        insertAfter: "Existing Section Name"
```

### Page Numbering
```yaml
pageNumbering:
  startPage: 2
  format: "Page {current} of {total}"
  position: "bottom-center"  # top-left, top-center, top-right, bottom-left, bottom-center, bottom-right
  font: "Helvetica"
  fontSize: 9
```

### Bookmarks
```yaml
bookmarks:
  - section: "Section Name"
    title: "Bookmark Title"
    level: 1  # Nesting level
```

## Usage

### 1. Create YAML Configuration
Place your config in `config-repo/pdf-merge-config.yml`

### 2. Implement PDFBox Generators
For each legacy PDFBox generator:

```java
@Component
public class YourGenerator implements PdfBoxGenerator {
    @Override
    public String getName() {
        return "your-generator-name";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        // Your existing PDFBox code here
        PDDocument doc = new PDDocument();
        // ... generate pages
        return doc;
    }
}
```

### 3. Call the Service
```java
@Autowired
private FlexiblePdfMergeService mergeService;

public byte[] generateReport(Map<String, Object> payload) throws IOException {
    return mergeService.generateMergedPdf("pdf-merge-config.yml", payload);
}
```

### 4. Test via REST API
```bash
curl -X POST http://localhost:8080/generate \
  -H "Content-Type: application/json" \
  -d @request_merged_pdf.json \
  --output merged-report.pdf
```

## Examples

### Example 1: Healthcare Report
```yaml
sections:
  - name: "Cover"
    type: pdfbox
    template: "cover-page-generator"
  - name: "Member Plans"
    type: freemarker
    template: "member-healthcare-plans.ftl"
  - name: "Charts"
    type: pdfbox
    template: "chart-generator"
```

### Example 2: Conditional Sections
```yaml
conditionalSections:
  - condition: "payload.includeAppendix"
    sections:
      - name: "Appendix"
        type: freemarker
        template: "appendix.ftl"
        insertAfter: "Main Content"
```

## Benefits

1. **Flexibility**: Change document structure without code changes
2. **Reusability**: Mix and match sections across different report types
3. **Conditional Logic**: Include/exclude sections based on runtime data
4. **Configuration-Driven**: Business users can modify document structure
5. **Legacy Integration**: Seamlessly integrate existing PDFBox code

## Migration Path

### Step 1: Wrap Existing PDFBox Code
```java
@Component
public class LegacyReportGenerator implements PdfBoxGenerator {
    @Autowired
    private YourExistingService existingService;
    
    @Override
    public PDDocument generate(Map<String, Object> payload) {
        // Call your existing code
        return existingService.generatePdf(payload);
    }
}
```

### Step 2: Create YAML Config
Define which pages use FreeMarker and which use PDFBox

### Step 3: Gradually Migrate
Move complex layout pages to FreeMarker while keeping dynamic/chart pages in PDFBox

## Advanced Features

### Custom Condition Evaluator
Extend `evaluateCondition()` to support SpEL or complex logic:

```java
private boolean evaluateCondition(String condition, Map<String, Object> payload) {
    // Use Spring Expression Language
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression(condition);
    return exp.getValue(payload, Boolean.class);
}
```

### Dynamic Section Ordering
```yaml
sections:
  - name: "Section A"
    order: 10
  - name: "Section B"  
    order: 5  # Will appear first
```

### Multi-Config Support
```java
// Load different configs for different report types
mergeService.generateMergedPdf("invoice-config.yml", data);
mergeService.generateMergedPdf("statement-config.yml", data);
```
