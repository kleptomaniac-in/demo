Here are the key limitations of the pure Java approach (Apache POI + PDFBox) compared to LibreOffice:

## Visual Formatting & Styling

**LibreOffice:**
- ✅ Preserves all colors (cell backgrounds, fonts, borders)
- ✅ Font styles (bold, italic, underline, strikethrough)
- ✅ Multiple fonts and sizes
- ✅ Cell borders (all styles: solid, dashed, double, etc.)
- ✅ Text alignment (left, center, right, justify)
- ✅ Text wrapping and rotation
- ✅ Conditional formatting
- ✅ Number formatting (currency, percentages, dates)

**Apache POI + PDFBox (Current):**
- ❌ No colors (renders as black/white/gray)
- ❌ Single font (Helvetica only)
- ❌ Basic borders only (light gray lines)
- ❌ Left-aligned text only
- ❌ No text wrapping (truncates with "...")
- ❌ No conditional formatting
- ❌ Basic number/text rendering

## Complex Content

**LibreOffice:**
- ✅ Charts and graphs (fully rendered)
- ✅ Images and logos
- ✅ Shapes and drawings
- ✅ Embedded objects
- ✅ Formulas (calculated and displayed)
- ✅ Hyperlinks
- ✅ Comments

**Apache POI + PDFBox:**
- ❌ No charts (skipped)
- ❌ No images
- ❌ No shapes
- ❌ No embedded objects
- ⚠️ Formula results only (if calculated by POI)
- ❌ No hyperlinks
- ❌ No comments

## Layout & Structure

**LibreOffice:**
- ✅ Merged cells rendered correctly
- ✅ Respects column widths
- ✅ Respects row heights
- ✅ Print area honored
- ✅ Page breaks respected
- ✅ Headers and footers
- ✅ Margins and page setup
- ✅ Landscape/portrait orientation
- ✅ Scaling to fit pages

**Apache POI + PDFBox:**
- ❌ Merged cells not handled (shows in first cell only)
- ❌ Fixed cell width (80pt regardless of actual width)
- ❌ Fixed cell height (18pt)
- ❌ Ignores print area
- ⚠️ Basic pagination (when height exceeded)
- ❌ No headers/footers
- ⚠️ Fixed margins
- ⚠️ Portrait A4 only
- ❌ No scaling

## Cell Content Quality

**LibreOffice:**
- ✅ Long text wraps in cells
- ✅ Rich text formatting within cells
- ✅ Proper date/time formatting
- ✅ Currency symbols and formatting
- ✅ Decimal precision respected
- ✅ Scientific notation

**Apache POI + PDFBox:**
- ❌ Long text truncated ("Hello Wor...")
- ❌ Plain text only
- ⚠️ Basic date display
- ⚠️ Numbers shown as-is
- ⚠️ Raw numeric values
- ⚠️ No special formatting

## Performance & Resources

**LibreOffice:**
- ⚠️ External process (heavy)
- ⚠️ Requires installation (~500MB)
- ⚠️ Slower (30s timeout per file)
- ⚠️ Temp file management
- ⚠️ Process spawn overhead
- ⚠️ Memory intensive
- ⚠️ Platform dependent

**Apache POI + PDFBox:**
- ✅ In-process (lightweight)
- ✅ No installation (pure Java)
- ✅ Fast (~100ms typical)
- ✅ No temp files
- ✅ No external processes
- ✅ Low memory footprint
- ✅ Platform independent

## Use Cases

**LibreOffice is better for:**
- 📊 Reports with charts/graphs
- 🎨 Heavily formatted documents
- 📷 Documents with images
- 💼 Professional presentations
- 📈 Complex dashboards
- 🎯 Exact Excel → PDF conversion

**Apache POI + PDFBox is better for:**
- 📝 Simple data tables
- ⚡ High-volume processing
- 🐳 Docker/containerized environments
- ☁️ Serverless/cloud functions
- 🔒 Security-sensitive environments
- ⚙️ Automated data exports

## Recommendation

**Use LibreOffice if:**
- Visual appearance is critical
- You have charts, images, or complex formatting
- Converting existing Excel files as-is
- Low volume (< 100 PDFs/day)

**Use Apache POI + PDFBox if:**
- You generate Excel programmatically (templates with data)
- Simple tabular data is sufficient
- High volume / performance is critical
- Running in restricted environments (containers, cloud)
- You don't want external dependencies

**Best of both worlds:**
Use **FreeMarker HTML templates → PDF** for the main PDF generation, and only use Excel for actual spreadsheet outputs that users can edit.