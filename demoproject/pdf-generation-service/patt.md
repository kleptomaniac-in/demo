Searched for text `sections:` (`**/*.yml`), 20 results

Read [](file:///workspaces/demo/demoproject/wtf/pdf-merge-config.yml#1-1), lines 1 to 100

Searched for text `type: acroform` (`**/*.java`), no results

Summarized conversation history

Searched for regex `FlexiblePdfMerge|SectionType|PdfSection` (`**/*.java`), 10 results

Read [](file:///workspaces/demo/demoproject/wtf/pdf-merge-config.yml#101-101), lines 101 to 116

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/FlexiblePdfMergeService.java#1-1), lines 1 to 100

Perfect! Yes, your understanding is **100% correct**. Let me show you how this works:

## Section-Based PDF Composition

Yes, when you have a YAML config with sections where each section has a `type` (pdfbox, freemarker, or acroform), each section generates its own PDF pages independently, and then all are merged into a single final PDF.

This is handled by FlexiblePdfMergeService.java: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/FlexiblePdfMergeService.java#101-101), lines 101 to 200

## The Three Types of PDF Merging

You now understand all three patterns:

### 1. **Template Merge** (Multiple AcroForm templates)
- **When**: Combining reusable form templates
- **How**: Fill each AcroForm separately → Merge filled PDFs
- **Example**: Combine application form + supplemental forms

### 2. **Addendum Merge** (Overflow data)
- **When**: Handling data that exceeds form capacity
- **How**: Fill main form → Generate overflow pages programmatically → Append
- **Example**: 3 dependents fit in form, remaining 5 go to addendum page

### 3. **Section-Based Composition** (Mixed generators) ← **Current**
- **When**: Creating complex documents with varied content types
- **How**: Each section generates pages independently → Merge all outputs
- **Example**: Cover page (pdfbox) + Member plans (freemarker) + Enrollment form (acroform)

## How Section-Based Composition Works 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/FlexiblePdfMergeService.java#200-200), lines 200 to 250

## Execution Flow

From pdf-merge-config.yml:

```yaml
sections:
  - name: "Cover Page"
    type: pdfbox                    # ← Programmatic generation
    template: "cover-page-generator"
    
  - name: "Healthcare Member Plans"
    type: freemarker                # ← HTML template → PDF
    template: "member-healthcare-plans.ftl"
    
  - name: "Premium Summary Chart"
    type: pdfbox                    # ← Dynamic chart generation
    template: "premium-chart-generator"
    
  - name: "Terms and Conditions"
    type: freemarker                # ← Static HTML content
    template: "terms-conditions.ftl"
    
  - name: "Enrollment Form"
    type: acroform                  # ← Fillable PDF form
    template: "enrollment-form.pdf"
    fieldMapping: {...}
```

**Step-by-Step Process:**

1. **Section Generation** - Each section generates independently:
   - `pdfbox`: Runs Java code to programmatically create PDF pages
   - `freemarker`: Renders HTML template → Converts HTML to PDF
   - `acroform`: Fills form fields → Flattens (or keeps fillable)

2. **Collection** - All section PDFs collected in memory

3. **Merging** - Pages from all sections combined sequentially

4. **Post-Processing** - Applied to merged document:
   - Page numbers (with start page control)
   - Common headers/footers
   - Bookmarks/table of contents

5. **Output** - Single final PDF

## Key Differences from Other Merge Types

| Aspect | Template Merge | Addendum Merge | Section-Based |
|--------|---------------|----------------|---------------|
| **Source types** | AcroForms only | AcroForm + Generated | **Mixed: pdfbox/freemarker/acroform** |
| **Purpose** | Combine reusable forms | Handle overflow data | **Build complex multi-type documents** |
| **Fill timing** | Fill each before merge | Fill main, generate addendum | **Fill/generate each section independently** |
| **When to use** | Multiple templates | Data exceeds capacity | **Need charts + forms + HTML content** |

Your understanding is exactly right! Each section type uses its appropriate generator, produces PDF pages, and all are merged into the final document with unified headers, footers, and page numbering.