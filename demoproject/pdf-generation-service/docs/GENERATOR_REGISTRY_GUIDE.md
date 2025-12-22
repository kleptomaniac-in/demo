# PdfBoxGeneratorRegistry - How It Works

## Overview

The `PdfBoxGeneratorRegistry` is a Spring component that **automatically discovers and registers** all PDFBox generators in your application. It uses Spring's dependency injection to find all implementations of the `PdfBoxGenerator` interface.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│           PdfBoxGeneratorRegistry                       │
│                                                         │
│  @Component                                             │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Map<String, PdfBoxGenerator>                    │   │
│  │   "cover-page-generator" → CoverPageGenerator  │   │
│  │   "chart-generator"      → ChartGenerator      │   │
│  │   "footer-generator"     → FooterGenerator     │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  getGenerator(name) → returns generator by name        │
└─────────────────────────────────────────────────────────┘
                          ▲
                          │ Spring auto-discovery
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
┌───────────────────┐           ┌───────────────────┐
│ CoverPageGenerator│           │  ChartGenerator   │
│                   │           │                   │
│ @Component        │           │  @Component       │
│ implements        │           │  implements       │
│ PdfBoxGenerator   │           │  PdfBoxGenerator  │
└───────────────────┘           └───────────────────┘
```

## How It Works

### 1. Auto-Discovery via Spring

```java
@Component
public class PdfBoxGeneratorRegistry {
    private final Map<String, PdfBoxGenerator> generators = new HashMap<>();
    
    // Spring automatically finds ALL beans implementing PdfBoxGenerator
    @Autowired(required = false)
    public PdfBoxGeneratorRegistry(List<PdfBoxGenerator> generatorList) {
        if (generatorList != null) {
            for (PdfBoxGenerator generator : generatorList) {
                // Register each generator by its name
                generators.put(generator.getName(), generator);
            }
        }
    }
}
```

**Key Points:**
- `@Autowired(required = false)` - Won't fail if no generators exist
- `List<PdfBoxGenerator>` - Spring injects ALL beans implementing this interface
- Each generator is stored in a map using its `getName()` value as the key

### 2. The PdfBoxGenerator Interface

```java
public interface PdfBoxGenerator {
    // Generate a PDDocument from payload data
    PDDocument generate(Map<String, Object> payload) throws IOException;
    
    // Unique name used to reference this generator in YAML config
    String getName();
}
```

### 3. Lookup by Name

```java
public PdfBoxGenerator getGenerator(String name) {
    PdfBoxGenerator generator = generators.get(name);
    if (generator == null) {
        throw new IllegalArgumentException(
            "No PDFBox generator found with name: " + name
        );
    }
    return generator;
}
```

## How to Register New Generators

### Step 1: Create Your Generator Class

```java
package com.example.generator;

import com.example.service.PdfBoxGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component  // ← This tells Spring to auto-discover this class
public class MyCustomGenerator implements PdfBoxGenerator {
    
    @Override
    public String getName() {
        return "my-custom-generator";  // ← Used in YAML config
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument doc = new PDDocument();
        
        // Your PDFBox code here
        // Access data: payload.get("key")
        
        return doc;
    }
}
```

**That's it!** Spring will automatically:
1. Find this class (because of `@Component`)
2. Pass it to `PdfBoxGeneratorRegistry` constructor
3. Register it with the name "my-custom-generator"

### Step 2: Reference in YAML Config

```yaml
sections:
  - name: "My Custom Section"
    type: pdfbox
    template: "my-custom-generator"  # ← Matches getName()
    enabled: true
```

## Complete Working Examples

### Example 1: Chart Generator

```java
package com.example.generator;

import com.example.service.PdfBoxGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class PremiumChartGenerator implements PdfBoxGenerator {
    
    @Override
    public String getName() {
        return "premium-chart-generator";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream content = new PDPageContentStream(document, page);
        
        // Draw chart title
        content.setFont(PDType1Font.HELVETICA_BOLD, 18);
        content.beginText();
        content.newLineAtOffset(200, 750);
        content.showText("Premium Summary");
        content.endText();
        
        // Extract member data from payload
        List<Map<String, Object>> members = 
            (List<Map<String, Object>>) payload.get("members");
        
        if (members != null) {
            float y = 700;
            for (Map<String, Object> member : members) {
                String name = (String) member.get("name");
                
                // Draw bar chart or whatever visualization
                content.setNonStrokingColor(Color.BLUE);
                content.addRect(100, y, 150, 20);
                content.fill();
                
                content.setFont(PDType1Font.HELVETICA, 12);
                content.beginText();
                content.newLineAtOffset(260, y + 5);
                content.setNonStrokingColor(Color.BLACK);
                content.showText(name);
                content.endText();
                
                y -= 30;
            }
        }
        
        content.close();
        return document;
    }
}
```

### Example 2: Wrapping Existing Legacy Code

If you have existing PDFBox code:

```java
package com.example.generator;

import com.example.service.PdfBoxGenerator;
import com.example.legacy.ExistingPdfService;  // Your old code
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class LegacyReportWrapper implements PdfBoxGenerator {
    
    @Autowired
    private ExistingPdfService existingService;  // Inject your legacy service
    
    @Override
    public String getName() {
        return "legacy-report-generator";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        // Call your existing code
        return existingService.generateReport(payload);
    }
}
```

### Example 3: Generator with Configuration

```java
package com.example.generator;

import com.example.service.PdfBoxGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ConfigurableGenerator implements PdfBoxGenerator {
    
    @Value("${pdf.logo.path:/default/logo.png}")
    private String logoPath;
    
    @Value("${pdf.watermark.enabled:false}")
    private boolean watermarkEnabled;
    
    @Override
    public String getName() {
        return "configurable-generator";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument doc = new PDDocument();
        
        // Use configuration values
        if (watermarkEnabled) {
            // Add watermark
        }
        
        // Load logo from configured path
        // ...
        
        return doc;
    }
}
```

## Testing Your Generators

### Unit Test Example

```java
package com.example.generator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MyCustomGeneratorTest {
    
    @Test
    void testGeneratorName() {
        MyCustomGenerator generator = new MyCustomGenerator();
        assertEquals("my-custom-generator", generator.getName());
    }
    
    @Test
    void testGenerate() throws Exception {
        MyCustomGenerator generator = new MyCustomGenerator();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Test Report");
        payload.put("companyName", "Test Co");
        
        PDDocument doc = generator.generate(payload);
        
        assertNotNull(doc);
        assertEquals(1, doc.getNumberOfPages());
        
        doc.close();
    }
}
```

### Integration Test

```java
package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PdfBoxGeneratorRegistryTest {
    
    @Autowired
    private PdfBoxGeneratorRegistry registry;
    
    @Test
    void testGeneratorIsRegistered() {
        assertTrue(registry.hasGenerator("my-custom-generator"));
        
        PdfBoxGenerator generator = registry.getGenerator("my-custom-generator");
        assertNotNull(generator);
        assertEquals("my-custom-generator", generator.getName());
    }
    
    @Test
    void testAllExpectedGeneratorsAreRegistered() {
        assertTrue(registry.hasGenerator("cover-page-generator"));
        assertTrue(registry.hasGenerator("premium-chart-generator"));
        // Add more assertions for your generators
    }
}
```

## Debugging Tips

### See What Generators Are Registered

Add this to your generator registry:

```java
@Component
public class PdfBoxGeneratorRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(PdfBoxGeneratorRegistry.class);
    
    @Autowired(required = false)
    public PdfBoxGeneratorRegistry(List<PdfBoxGenerator> generatorList) {
        if (generatorList != null) {
            log.info("Registering {} PDFBox generators", generatorList.size());
            for (PdfBoxGenerator generator : generatorList) {
                String name = generator.getName();
                generators.put(name, generator);
                log.info("  Registered: {} -> {}", name, generator.getClass().getSimpleName());
            }
        } else {
            log.warn("No PDFBox generators found");
        }
    }
}
```

### Check Registered Generators at Runtime

Add an endpoint to see what's registered:

```java
@RestController
@RequestMapping("/api/pdf")
public class PdfMergeController {
    
    @Autowired
    private PdfBoxGeneratorRegistry registry;
    
    @GetMapping("/generators")
    public Map<String, String> listGenerators() {
        // Return all registered generator names
        // (You'd need to add a method to registry to get all names)
    }
}
```

## Common Issues & Solutions

### Issue: Generator Not Found

**Error:** `No PDFBox generator found with name: my-generator`

**Solutions:**
1. Check `@Component` annotation exists on your class
2. Verify class is in a scanned package (e.g., `com.example.generator`)
3. Check `getName()` matches the name in YAML config
4. Look at startup logs to see if generator was registered
5. Ensure package is included in `@ComponentScan`

### Issue: Generator Not Auto-Discovered

**Possible Causes:**
- Class not annotated with `@Component`
- Class not in a scanned package
- Missing `@ComponentScan` in main application class

**Solution:** Add to main application:
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.pdf", 
    "com.example.service", 
    "com.example.generator"  // ← Make sure this is included
})
public class PdfGenerationApplication {
    // ...
}
```

### Issue: Multiple Generators with Same Name

**What Happens:** Last one registered wins

**Solution:** Ensure each generator has a unique `getName()` value

## Best Practices

1. **Use Descriptive Names**
   ```java
   // Good
   return "invoice-summary-generator";
   
   // Bad
   return "gen1";
   ```

2. **One Generator Per Responsibility**
   - Don't create one massive generator
   - Split into focused generators (cover, charts, tables, etc.)

3. **Handle Missing Data Gracefully**
   ```java
   String title = (String) payload.getOrDefault("title", "Untitled Report");
   ```

4. **Close Resources**
   ```java
   PDDocument doc = new PDDocument();
   try {
       // generate content
       return doc;
   } catch (Exception e) {
       doc.close();  // Clean up on error
       throw e;
   }
   ```

5. **Log Important Operations**
   ```java
   log.info("Generating chart for {} members", memberCount);
   ```

## Summary

The `PdfBoxGeneratorRegistry` works through **Spring's auto-discovery**:

1. ✅ Create a class implementing `PdfBoxGenerator`
2. ✅ Add `@Component` annotation
3. ✅ Implement `getName()` with unique name
4. ✅ Implement `generate()` with your PDFBox code
5. ✅ Reference the name in YAML config

**No manual registration needed!** Spring does it automatically.
