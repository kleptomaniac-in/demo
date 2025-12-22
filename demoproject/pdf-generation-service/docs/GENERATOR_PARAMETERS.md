# PDFBox Generator Parameters - Design Options

## Option 1: Use Payload Directly (Current & Recommended)

### ✅ Advantages
- Simple and straightforward
- No extra configuration needed
- Generators have access to all data
- Easy to understand and maintain

### YAML Config
```yaml
sections:
  - name: "Cover Page"
    type: pdfbox
    template: "cover-page-generator"
    enabled: true
```

### Generator Implementation
```java
@Component
public class CoverPageGenerator implements PdfBoxGenerator {
    
    @Override
    public String getName() {
        return "cover-page-generator";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        // Access any field from the request payload
        String title = (String) payload.get("title");
        String companyName = (String) payload.get("companyName");
        
        List<Map<String, Object>> members = 
            (List<Map<String, Object>>) payload.get("members");
        
        // Use the data to generate PDF
        PDDocument doc = new PDDocument();
        // ... create cover page with title and companyName
        return doc;
    }
}
```

### Request JSON
```json
{
  "configName": "pdf-merge-config.yml",
  "payload": {
    "title": "Healthcare Report 2025",
    "companyName": "Acme Corp",
    "members": [...]
  }
}
```

---

## Option 2: Section-Specific Parameters (If Needed)

### ✅ Advantages
- Configure same generator differently in different sections
- Reusable generators with different behaviors
- Section-specific settings (theme, formatting, etc.)

### ❌ Disadvantages
- More complex configuration
- Requires code changes to support parameters
- Two sources of data (payload + parameters)

### Enhanced YAML Config
```yaml
sections:
  - name: "Executive Summary Cover"
    type: pdfbox
    template: "cover-page-generator"
    enabled: true
    parameters:
      theme: "executive"
      fontSize: 28
      showLogo: true
      backgroundColor: "#003366"
      
  - name: "Detailed Report Cover"
    type: pdfbox
    template: "cover-page-generator"
    enabled: true
    parameters:
      theme: "detailed"
      fontSize: 20
      showLogo: false
      backgroundColor: "#FFFFFF"
```

### Code Changes Required

#### 1. Update SectionConfig Class
```java
class SectionConfig {
    private String name;
    private String type;
    private String template;
    private boolean enabled;
    private String insertAfter;
    private Map<String, Object> parameters;  // ← ADD THIS

    // ... existing getters/setters ...
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { 
        this.parameters = parameters; 
    }
}
```

#### 2. Update PdfBoxGenerator Interface
```java
public interface PdfBoxGenerator {
    // Option A: Add parameters to existing method
    PDDocument generate(Map<String, Object> payload, Map<String, Object> parameters) 
        throws IOException;
    
    // Option B: Keep existing and add overload (better for backward compatibility)
    default PDDocument generate(Map<String, Object> payload, Map<String, Object> parameters) 
        throws IOException {
        return generate(payload);  // Fallback to simple version
    }
    
    PDDocument generate(Map<String, Object> payload) throws IOException;
    
    String getName();
}
```

#### 3. Update FlexiblePdfMergeService
```java
private PDDocument generateSectionPdf(SectionConfig section, Map<String, Object> payload) 
    throws IOException {
    
    if ("freemarker".equals(section.getType())) {
        // ... existing FreeMarker code ...
        
    } else if ("pdfbox".equals(section.getType())) {
        PdfBoxGenerator generator = pdfBoxRegistry.getGenerator(section.getTemplate());
        
        // Pass parameters if available
        Map<String, Object> parameters = section.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            return generator.generate(payload, parameters);
        } else {
            return generator.generate(payload);
        }
        
    } else {
        throw new IllegalArgumentException("Unknown section type: " + section.getType());
    }
}
```

#### 4. Update Generator Implementation
```java
@Component
public class CoverPageGenerator implements PdfBoxGenerator {
    
    @Override
    public String getName() {
        return "cover-page-generator";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        // Default parameters
        return generate(payload, Map.of(
            "theme", "default",
            "fontSize", 24,
            "showLogo", true
        ));
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload, Map<String, Object> parameters) 
        throws IOException {
        
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        PDPageContentStream content = new PDPageContentStream(doc, page);
        
        // Use parameters from YAML config
        String theme = (String) parameters.getOrDefault("theme", "default");
        int fontSize = (int) parameters.getOrDefault("fontSize", 24);
        boolean showLogo = (boolean) parameters.getOrDefault("showLogo", true);
        String bgColor = (String) parameters.getOrDefault("backgroundColor", "#FFFFFF");
        
        // Use payload data
        String title = (String) payload.get("title");
        String companyName = (String) payload.get("companyName");
        
        // Apply theme-based styling
        if ("executive".equals(theme)) {
            // Executive theme styling
            content.setNonStrokingColor(parseColor(bgColor));
            content.addRect(0, 0, page.getMediaBox().getWidth(), 
                           page.getMediaBox().getHeight());
            content.fill();
        }
        
        // Draw title with configured fontSize
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, fontSize);
        content.newLineAtOffset(100, 700);
        content.showText(title);
        content.endText();
        
        if (showLogo) {
            // Draw logo
        }
        
        content.close();
        return doc;
    }
    
    private Color parseColor(String hex) {
        // Parse hex color
    }
}
```

#### 5. Update PdfMergeConfigService
```java
private PdfMergeConfig parsePdfMergeConfig(Map<String, Object> data) {
    // ... existing code ...
    
    if (pdfMerge.containsKey("sections")) {
        List<Map<String, Object>> sections = 
            (List<Map<String, Object>>) pdfMerge.get("sections");
        List<SectionConfig> sectionConfigs = new ArrayList<>();
        
        for (Map<String, Object> section : sections) {
            SectionConfig sectionConfig = new SectionConfig();
            sectionConfig.setName((String) section.get("name"));
            sectionConfig.setType((String) section.get("type"));
            sectionConfig.setTemplate((String) section.get("template"));
            sectionConfig.setEnabled((Boolean) section.getOrDefault("enabled", true));
            
            // ← ADD THIS: Parse parameters if present
            if (section.containsKey("parameters")) {
                Map<String, Object> params = 
                    (Map<String, Object>) section.get("parameters");
                sectionConfig.setParameters(params);
            }
            
            sectionConfigs.add(sectionConfig);
        }
        
        config.setSections(sectionConfigs);
    }
    
    // ... rest of code ...
}
```

---

## Option 3: Hybrid Approach (Best of Both)

Combine both approaches for maximum flexibility:

### YAML Config
```yaml
sections:
  # Simple usage - just payload
  - name: "Healthcare Member Plans"
    type: freemarker
    template: "member-healthcare-plans.ftl"
    enabled: true
    
  # Parameterized usage for customization
  - name: "Executive Cover"
    type: pdfbox
    template: "cover-page-generator"
    enabled: true
    parameters:
      theme: "executive"
      showWatermark: true
      
  # Same generator, different parameters
  - name: "Internal Cover"
    type: pdfbox
    template: "cover-page-generator"
    enabled: true
    parameters:
      theme: "internal"
      showWatermark: false
```

### Generator with Smart Defaults
```java
@Component
public class CoverPageGenerator implements PdfBoxGenerator {
    
    @Override
    public PDDocument generate(Map<String, Object> payload, Map<String, Object> parameters) 
        throws IOException {
        
        // Merge parameters with sensible defaults
        Map<String, Object> config = new HashMap<>();
        config.put("theme", "default");
        config.put("fontSize", 24);
        config.put("showLogo", true);
        config.put("showWatermark", false);
        
        // Override with provided parameters
        if (parameters != null) {
            config.putAll(parameters);
        }
        
        // You can also check payload for generator-specific fields
        if (payload.containsKey("coverTheme")) {
            config.put("theme", payload.get("coverTheme"));
        }
        
        // Generate using merged config
        return generateWithConfig(payload, config);
    }
    
    private PDDocument generateWithConfig(Map<String, Object> payload, 
                                         Map<String, Object> config) {
        // Implementation
    }
}
```

---

## Recommendation

### Use **Option 1** (Current Design) if:
- ✅ You have simple requirements
- ✅ Each generator has one purpose
- ✅ Configuration comes from payload
- ✅ You want to keep it simple

### Use **Option 2** (Parameters) if:
- ✅ You need to reuse same generator with different styling
- ✅ You want section-specific behavior (themes, formatting)
- ✅ You need to override defaults per section
- ✅ You have complex layout variations

### Use **Option 3** (Hybrid) if:
- ✅ You want maximum flexibility
- ✅ Some generators are simple, others need customization
- ✅ You want backward compatibility
- ✅ You're building a framework for others to use

---

## Current Implementation Summary

**Your current implementation uses Option 1:**
- ✅ Simple and working
- ✅ Payload passed directly to generators
- ✅ No parameters section needed
- ✅ Generators access `payload.get("key")` directly

**To add parameters support (Option 2):**
1. Add `parameters` field to `SectionConfig`
2. Update `PdfBoxGenerator` interface (add overload)
3. Update `FlexiblePdfMergeService.generateSectionPdf()`
4. Update `PdfMergeConfigService` to parse parameters
5. Update generators to accept parameters

**Estimated effort:** 1-2 hours for full implementation

Unless you have a specific need for section-specific parameters, **stick with Option 1** - it's simpler and sufficient for most use cases.
