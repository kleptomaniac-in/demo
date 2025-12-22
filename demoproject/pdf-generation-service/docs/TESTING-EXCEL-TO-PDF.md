# Testing Excel to PDF Conversion

The application is now running on port 8080. Here's how to test the Excel-to-PDF conversion:

## 1. Check Conversion Info
See what conversion method is available:

```bash
curl http://localhost:8080/api/excel/conversion-info
```

Expected output:
```json
{
  "available": true,
  "method": "Apache POI + PDFBox (Pure Java)",
  "message": "Ready to convert - no external dependencies required"
}
```

## 2. Generate Excel and Convert to PDF

Test with the sample request file:

```bash
curl -X POST http://localhost:8080/api/excel/generate-as-pdf \
  -H "Content-Type: application/json" \
  -d @test-excel-to-pdf.json \
  --output test-output.pdf
```

This will:
1. Generate an Excel file from the template
2. Convert it to PDF using Apache POI + PDFBox
3. Save the result as `test-output.pdf`

## 3. View the PDF

```bash
# If you have a PDF viewer
xdg-open test-output.pdf

# Or check the file was created
ls -lh test-output.pdf
```

## 4. Test with Config-based Generation

```bash
# First, check what configs are available
curl http://localhost:8080/api/excel/configs

# Then generate using a config
curl -X POST http://localhost:8080/api/excel/generate-from-config-as-pdf \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "invoice-config.yml",
    "payload": {
      "member": {
        "name": "Jane Smith",
        "memberId": "M67890"
      },
      "plan": {
        "name": "Platinum Plan",
        "premium": "$750.00"
      }
    }
  }' \
  --output config-test.pdf
```

## 5. Test Cache Performance

Run the same request multiple times to see caching in action:

```bash
# First request (cache miss - slower)
time curl -X POST http://localhost:8080/api/excel/generate-as-pdf \
  -H "Content-Type: application/json" \
  -d @test-excel-to-pdf.json \
  --output test1.pdf

# Second request (cache hit - faster)
time curl -X POST http://localhost:8080/api/excel/generate-as-pdf \
  -H "Content-Type: application/json" \
  -d @test-excel-to-pdf.json \
  --output test2.pdf
```

## 6. Check Cache Statistics

```bash
curl http://localhost:8080/api/admin/cache/stats
```

Look for:
- `acroformTemplates` - Cached PDF form templates
- `pdfConfigs` - Cached configuration files
- Hit rates should increase with repeated requests

## What to Expect

The PDF will contain:
- Sheet name as title
- Excel cells rendered as a grid with borders
- Cell values (text, numbers, dates, etc.)
- Multiple sheets (if present) on separate pages
- Auto-pagination if content exceeds one page

**Note:** The output won't be as polished as LibreOffice conversion (no colors, formatting, images), but it's:
- ✅ Fast
- ✅ Pure Java (no external dependencies)
- ✅ Reliable
- ✅ Works everywhere

## Troubleshooting

**If the endpoint returns an error:**
1. Check the application logs in the terminal
2. Verify the Excel template exists at the specified path
3. Ensure the cellMappings match actual cells in the template

**If the PDF looks wrong:**
- The basic renderer has limitations (no complex formatting)
- For production-quality PDFs, consider using FreeMarker templates instead
- Or enhance the ExcelToPdfConverter with more formatting support

## Alternative: Just Get the Excel File

If you only want the Excel file (not converted to PDF):

```bash
curl -X POST http://localhost:8080/api/excel/generate \
  -H "Content-Type: application/json" \
  -d @test-excel-to-pdf.json \
  --output test-output.xlsx
```

Then convert it manually with your preferred tool.
