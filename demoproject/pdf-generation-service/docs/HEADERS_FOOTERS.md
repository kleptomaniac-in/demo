# Common Headers and Footers Configuration

## Overview
The flexible PDF merge system now supports common headers and footers that appear consistently across ALL pages, regardless of whether they come from FreeMarker templates or PDFBox generators.

## Features
✅ **Unified across all sources** - Same header/footer on FreeMarker and PDFBox pages  
✅ **Three-column layout** - Left, center, and right positioning  
✅ **Variable substitution** - Dynamic content from payload  
✅ **Optional borders** - Visual separation lines  
✅ **Page-specific control** - Skip cover pages or specific pages  
✅ **Customizable fonts** - Multiple font options and sizes

## YAML Configuration

### Basic Header Configuration
```yaml
header:
  enabled: true
  height: 50              # Height reserved for header (in points)
  startPage: 2            # Start from page 2 (1-based, skip cover)
  content:
    left:
      text: "{companyName}"
      font: "Helvetica"
      fontSize: 10
    center:
      text: "Healthcare Plan Report"
      font: "Helvetica-Bold"
      fontSize: 12
    right:
      text: "{date}"
      font: "Helvetica"
      fontSize: 10
  border:
    enabled: true
    color: "#CCCCCC"
    thickness: 1
```

### Basic Footer Configuration
```yaml
footer:
  enabled: true
  height: 40
  startPage: 1           # Apply to all pages
  content:
    left:
      text: "Confidential"
      font: "Helvetica"
      fontSize: 8
    center:
      text: "Page {current} of {total}"
      font: "Helvetica"
      fontSize: 9
    right:
      text: "© 2025 {companyName}"
      font: "Helvetica"
      fontSize: 8
  border:
    enabled: true
    color: "#CCCCCC"
    thickness: 1
```

## Variable Substitution

### Built-in Variables
- `{current}` - Current page number
- `{total}` - Total number of pages
- `{date}` - Current date (YYYY-MM-DD format)

### Payload Variables
Any key in your payload can be used:
```yaml
text: "{companyName}"         # From payload.companyName
text: "{reportTitle}"         # From payload.reportTitle
text: "Report ID: {reportId}" # Mixed text and variable
```

### Example Payload
```json
{
  "companyName": "Acme Corporation",
  "reportTitle": "Q4 2025 Healthcare Report",
  "reportId": "RPT-2025-001",
  "members": [...]
}
```

## Font Options

Available fonts:
- `Helvetica` (default)
- `Helvetica-Bold`
- `Helvetica-Oblique`
- `Times-Roman`
- `Times-Bold`
- `Courier`

## Border Configuration

```yaml
border:
  enabled: true
  color: "#CCCCCC"    # Hex color code
  thickness: 1        # Line thickness in points
```

Common colors:
- `#000000` - Black
- `#CCCCCC` - Light gray
- `#666666` - Dark gray
- `#0066CC` - Blue

## Page Control

### Skip Cover Page
```yaml
header:
  startPage: 2  # Header appears from page 2 onwards

footer:
  startPage: 2  # Footer appears from page 2 onwards
```

### Apply to All Pages
```yaml
header:
  startPage: 1  # Header appears on all pages
```

## Complete Example

```yaml
pdfMerge:
  settings:
    pageNumbering: continuous
    addBookmarks: true
    
  # Header configuration
  header:
    enabled: true
    height: 60
    startPage: 2
    content:
      left:
        text: "{companyName}"
        font: "Helvetica"
        fontSize: 10
      center:
        text: "{reportTitle}"
        font: "Helvetica-Bold"
        fontSize: 14
      right:
        text: "Generated: {date}"
        font: "Helvetica"
        fontSize: 9
    border:
      enabled: true
      color: "#0066CC"
      thickness: 2
      
  # Footer configuration
  footer:
    enabled: true
    height: 40
    startPage: 1
    content:
      left:
        text: "Confidential - Internal Use Only"
        font: "Helvetica-Oblique"
        fontSize: 8
      center:
        text: "Page {current} of {total}"
        font: "Helvetica-Bold"
        fontSize: 10
      right:
        text: "© {companyName}"
        font: "Helvetica"
        fontSize: 8
    border:
      enabled: true
      color: "#CCCCCC"
      thickness: 1
      
  sections:
    - name: "Cover Page"
      type: pdfbox
      template: "cover-page-generator"
      
    - name: "Healthcare Member Plans"
      type: freemarker
      template: "member-healthcare-plans.ftl"
      
    - name: "Premium Charts"
      type: pdfbox
      template: "chart-generator"
```

## Test Request Example

```json
{
  "templateName": "healthcare-report",
  "mergeConfig": "pdf-merge-config.yml",
  "payload": {
    "companyName": "Acme Corporation",
    "reportTitle": "2025 Healthcare Plan Report",
    "reportId": "RPT-2025-Q4",
    "includeDetailedBreakdown": true,
    "members": [
      {
        "name": "John Doe",
        "medicalPlans": [...],
        "dentalPlans": [...],
        "visionPlans": [...]
      }
    ]
  }
}
```

## How It Works

1. **Generate Sections**: FreeMarker and PDFBox pages are generated independently
2. **Merge Documents**: All pages are combined into a single PDDocument
3. **Apply Headers/Footers**: System overlays headers and footers on ALL pages using PDFBox content streams
4. **Variable Replacement**: Dynamic content from payload is substituted
5. **Page Numbering**: Built-in variables like {current} and {total} are calculated

## Benefits

### 1. **Consistency**
Same header/footer across all pages, regardless of source technology

### 2. **Centralized Configuration**
Change headers/footers in YAML without modifying FreeMarker templates or PDFBox code

### 3. **Dynamic Content**
Headers and footers can display data from the request payload

### 4. **No Template Changes**
Existing FreeMarker templates and PDFBox generators don't need modification

### 5. **Selective Application**
Control which pages get headers/footers (e.g., skip cover pages)

## Advanced Scenarios

### Different Headers for Different Sections
```yaml
# In future enhancement, you could add section-specific headers:
sections:
  - name: "Cover"
    type: pdfbox
    template: "cover-generator"
    header: none
    
  - name: "Content"
    type: freemarker
    template: "content.ftl"
    header: default
    
  - name: "Appendix"
    type: freemarker
    template: "appendix.ftl"
    header: appendix-header
```

### Conditional Headers
```yaml
# Could be enhanced to support conditional logic:
header:
  enabled: true
  condition: "payload.includeHeader"
  content:
    center:
      text: "{reportTitle}"
```

## Troubleshooting

### Headers/Footers Not Appearing
- Check `enabled: true` in configuration
- Verify `startPage` is within document page range
- Ensure payload variables exist (e.g., `{companyName}`)

### Overlapping Content
- Adjust `height` parameter to reserve more space
- Modify your FreeMarker templates to use CSS margins:
  ```css
  @page {
    margin-top: 60px;    /* Match header height */
    margin-bottom: 50px; /* Match footer height */
  }
  ```

### Variable Not Replaced
- Ensure variable name matches payload key exactly
- Variables are case-sensitive
- Check payload structure in request JSON

## Migration from Template-Based Headers

### Before (in FreeMarker template):
```html
<div class="header">
  <span>Company Name</span>
  <span>Report Title</span>
</div>
```

### After (in YAML config):
```yaml
header:
  enabled: true
  content:
    left:
      text: "{companyName}"
    center:
      text: "{reportTitle}"
```

**Benefits**: Remove repetitive header code from multiple templates, centralize styling, ensure consistency.
