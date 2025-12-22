Let me check the current implementation to see the exact flow: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/pdf/controller/EnrollmentPdfController.java#44-44), lines 44 to 95

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/pdf/controller/EnrollmentPdfController.java#265-265), lines 265 to 350

Now let me check what `generateMergedPdf` does with the processed payload: 

Searched for regex `generateMergedPdf.*processedPayload` (`**/FlexiblePdfMergeService.java`), no results

Searched codebase for "FlexiblePdfMergeService generateMergedPdf field mapping payload", 11 results

Now let me check how AcroFormFillService handles the payload: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/AcroFormFillService.java#1-1), lines 1 to 100

Perfect! Now I have a complete understanding. Let me provide a clear answer:

## **Answer: YES, preprocessing happens BEFORE field mapping**

Based on the code analysis, here's the exact flow in `/api/enrollment/generate`:

### **Execution Order:**

```
1. Load preliminary config
        ↓
2. Auto-collect products from payload
        ↓
3. Select final config
        ↓
4. ✅ PREPROCESSING (preparePayload method) ← Line 87
        ↓
5. ✅ FIELD MAPPING (generateMergedPdf → AcroFormFillService) ← Line 90
        ↓
6. PDF generation
```

### **Key Evidence from Code:**

**Line 87-90 in EnrollmentPdfController.java:**
```java
// Prepare payload with optional pre-processing for complex structures
Map<String, Object> processedPayload = preparePayload(request.getPayload());

// Generate PDF (this includes field mapping)
byte[] pdfBytes = pdfMergeService.generateMergedPdf(configName, processedPayload);
```

### **What the Preprocessing Does:**

From EnrollmentPdfController.java:

1. **Detects** if payload has nested arrays (`hasNestedArrays()`)
2. **Applies** YAML-based transformation rules to flatten structure
3. **Creates combined payload** containing:
   - **Flattened fields** for AcroForm mapping (e.g., `primary`, `spouse`, `dependent1`, `dependent2`)
   - **Original nested structure** for FreeMarker templates

Example transformation:
```java
// BEFORE preprocessing:
{
  "application": {
    "applicants": [
      {"relationship": "PRIMARY", "name": "John"},
      {"relationship": "SPOUSE", "name": "Jane"},
      {"relationship": "DEPENDENT", "name": "Jack"}
    ]
  }
}

// AFTER preprocessing:
{
  "primary": {"name": "John"},           // ← Flattened for easy mapping
  "spouse": {"name": "Jane"},            // ← Flattened for easy mapping
  "dependent1": {"name": "Jack"},        // ← Flattened for easy mapping
  "hasSpouse": true,
  "dependentCount": 1,
  "application": { ... }                 // ← Original structure preserved
}
```

### **Then Field Mapping Uses Preprocessed Data:**

When `FlexiblePdfMergeService.generateMergedPdf()` is called with the **processed payload**, it eventually calls AcroFormFillService.java which:

1. Takes field mappings like `{"primary_name": "primary.name"}`
2. Resolves paths using AcroFormFillService.java
3. **Works with the already-flattened payload** from preprocessing

### **Why This Order Matters:**

✅ **Preprocessing creates simple paths** → Field mappings can use `primary.name` instead of `applicants[relationship=PRIMARY].name`  
✅ **Cleaner YAML configs** → No complex filtering syntax needed in field mappings  
✅ **Better performance** → Path resolution is simpler and faster  
✅ **Both structures available** → FreeMarker templates still have access to original nested data

### **Summary:**

**Yes, preprocessing always happens BEFORE field mapping** in the `/api/enrollment/generate` endpoint. The preprocessed payload is what gets passed to the PDF generation services for field mapping and template rendering.