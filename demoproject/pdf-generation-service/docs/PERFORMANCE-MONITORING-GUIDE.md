# Performance Monitoring Guide

## Overview

The PDF Generation Service includes comprehensive performance monitoring to help you identify and optimize slow-executing areas. The system tracks timing for each phase of PDF generation from request receipt to final PDF delivery.

---

## What Gets Measured

### Automatic Phase Tracking

Every PDF generation request is automatically tracked with timing for:

1. **Load Config** - Time to load and parse YAML configuration
2. **Apply Global Enrichers** - Time to run payload enrichers
3. **Resolve Sections** - Time to evaluate conditional sections
4. **Generate Section PDFs** - Individual timing for each section:
   - FreeMarker template rendering
   - AcroForm filling
   - PDFBox generation
5. **Merge Documents** - Time to merge all section PDFs
6. **Add Page Numbers** - Time to add page numbering
7. **Add Header** - Time to render header on all pages
8. **Add Footer** - Time to render footer on all pages
9. **Add Bookmarks** - Time to create PDF bookmarks
10. **Serialize to Bytes** - Time to convert final PDF to byte array

### Metrics Included

For each phase:
- **Duration (ms)** - Execution time in milliseconds
- **Percentage** - Percentage of total execution time
- **Details** - Phase-specific metadata (pages generated, file size, etc.)

---

## How to Use

### 1. Automatic Logging (Default)

Performance metrics are **automatically logged** to the console/log file after every PDF generation:

```
=== Performance Metrics: PDF Generation ===
Config: enrollment-multi-product
Total Duration: 1245ms

Phase Breakdown:
  Load Config                   :     15ms (  1.2%)
  Apply Global Enrichers        :     45ms (  3.6%)
  Resolve Sections              :      5ms (  0.4%)
  Generate Section PDFs         :    890ms ( 71.5%)
  Section: enrollment-cover     :    250ms ( 20.1%) - {type=freemarker, pages=1}
  Section: enrollment-form      :    420ms ( 33.7%) - {type=acroform, pages=1}
  Section: coverage-summary     :    180ms ( 14.5%) - {type=pdfbox, pages=1}
  Section: terms                :     40ms (  3.2%) - {type=freemarker, pages=1}
  Merge Documents               :     50ms (  4.0%) - {totalPages=4}
  Add Page Numbers              :     35ms (  2.8%)
  Add Header                    :     60ms (  4.8%)
  Add Footer                    :     45ms (  3.6%)
  Add Bookmarks                 :     10ms (  0.8%)
  Serialize to Bytes            :     90ms (  7.2%) - {sizeBytes=245678}
=====================================
```

### 2. REST API with Metrics

Use the `/api/document/generate-with-metrics` endpoint to get both the PDF **and** performance metrics in the response:

**Request:**
```bash
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "enrollment-multi-product",
    "payload": { 
      "applicationNumber": "APP-2025-001",
      "applicants": [...]
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "pdfSizeBytes": 245678,
  "pdfBase64": "JVBERi0xLj...",
  "performanceMetrics": {
    "operation": "PDF Generation",
    "configName": "enrollment-multi-product",
    "startTime": "2025-12-22T10:30:45.123Z",
    "endTime": "2025-12-22T10:30:46.368Z",
    "totalDurationMs": 1245,
    "phases": {
      "Load Config": {
        "durationMs": 15,
        "percentage": 1.2
      },
      "Generate Section PDFs": {
        "durationMs": 890,
        "percentage": 71.5
      },
      "Section: enrollment-form": {
        "durationMs": 420,
        "percentage": 33.7,
        "details": {
          "type": "acroform",
          "pages": 1
        }
      },
      ...
    }
  }
}
```

### 3. Extract and Decode PDF

The response includes the PDF as Base64. To save it:

```bash
# Using jq to extract and save
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d @request.json \
  | jq -r '.pdfBase64' | base64 -d > output.pdf
```

---

## Identifying Bottlenecks

### Common Performance Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| **"Load Config" > 50ms** | Config not cached | Enable cache warming |
| **"Section: acroform" > 200ms** | Template not cached | Enable cache warming for AcroForm templates |
| **"Section: freemarker" > 300ms** | Complex template | Simplify template, use cached includes |
| **"Merge Documents" > 100ms** | Large page count | Consider splitting PDFs |
| **"Add Header/Footer" > 100ms** | Applied to many pages | Optimize header/footer content |
| **"Serialize to Bytes" > 200ms** | Very large PDF | Check image compression, page count |

### Optimization Checklist

✅ **Enable Cache Warming**
```yaml
app:
  cache-warming:
    enabled: true  # Preload configs and templates at startup
```

✅ **Cache Frequently Used Configs**
```yaml
app:
  cache-warming:
    config-patterns: enrollment-*.yml,dental-*.yml,medical-*.yml
```

✅ **Monitor Section-Level Performance**
- Look for individual sections taking >30% of total time
- Focus optimization efforts on the slowest sections

✅ **Check Template Complexity**
- FreeMarker templates with many loops/conditionals
- AcroForm PDFs with 100+ fields
- PDFBox generators with complex table rendering

✅ **Review Payload Enrichers**
- Enrichers should complete in <50ms
- Consider caching enricher results if they're deterministic

---

## Performance Targets

### Development Environment

| Phase | Good | Acceptable | Slow |
|-------|------|------------|------|
| Load Config | <10ms | 10-30ms | >30ms |
| AcroForm Section | <100ms | 100-200ms | >200ms |
| FreeMarker Section | <150ms | 150-300ms | >300ms |
| PDFBox Section | <100ms | 100-200ms | >200ms |
| Total (4-page PDF) | <500ms | 500-1000ms | >1000ms |

### Production Environment (with warm caches)

| Phase | Good | Acceptable | Slow |
|-------|------|------------|------|
| Load Config | <5ms | 5-10ms | >10ms |
| AcroForm Section | <50ms | 50-100ms | >100ms |
| FreeMarker Section | <100ms | 100-200ms | >200ms |
| PDFBox Section | <75ms | 75-150ms | >150ms |
| Total (4-page PDF) | <300ms | 300-600ms | >600ms |

---

## Advanced: Custom Instrumentation

### Add Custom Phase Tracking

You can add custom phase tracking in your own services:

```java
@Service
public class MyCustomService {
    
    @Autowired
    private PerformanceMonitoringContext performanceMonitor;
    
    public void doComplexOperation(Map<String, Object> data) {
        performanceMonitor.startPhase("Custom Operation");
        
        try {
            // Your complex logic here
            processData(data);
            
            performanceMonitor.endPhase("Custom Operation", Map.of(
                "itemsProcessed", data.size()
            ));
        } catch (Exception e) {
            performanceMonitor.addWarning("Custom operation failed: " + e.getMessage());
            throw e;
        }
    }
}
```

### Thread-Local Context

Performance monitoring uses ThreadLocal storage, ensuring:
- Thread-safe operation in concurrent environments
- Automatic cleanup after each request
- No cross-contamination between requests

---

## Troubleshooting

### Metrics Not Appearing

**Problem:** No performance logs showing up

**Solution:**
1. Check log level is INFO or higher:
   ```yaml
   logging:
     level:
       com.example.monitoring: INFO
   ```

2. Verify PerformanceMonitoringContext is autowired:
   ```java
   @Autowired
   private PerformanceMonitoringContext performanceMonitor;
   ```

### Incomplete Metrics

**Problem:** Some phases missing from output

**Solution:**
- Ensure `startPhase()` and `endPhase()` are paired
- Check for exceptions interrupting the flow
- Verify `complete()` and `clear()` are called in finally block

### Metrics Showing in Wrong Request

**Problem:** Thread pool reuse causing cross-contamination

**Solution:**
- Ensure `clear()` is called in finally block
- Check that monitoring context isn't shared across threads

---

## Example Analysis Session

### Step 1: Generate PDF with Metrics

```bash
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d @test-request.json > metrics.json
```

### Step 2: Analyze the Output

```bash
jq '.performanceMetrics.phases' metrics.json
```

### Step 3: Identify Slowest Phase

```bash
jq '.performanceMetrics.phases | to_entries | 
    sort_by(.value.durationMs) | reverse | .[0]' metrics.json
```

Output:
```json
{
  "key": "Section: enrollment-form",
  "value": {
    "durationMs": 420,
    "percentage": 33.7,
    "details": {
      "type": "acroform",
      "pages": 1
    }
  }
}
```

### Step 4: Optimize

Based on the analysis:
1. AcroForm filling is taking 420ms (33.7%)
2. Enable cache warming for this template
3. Review field mappings for efficiency

### Step 5: Re-test and Compare

```bash
# After optimization
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d @test-request.json | jq '.performanceMetrics.totalDurationMs'
```

Expected improvement: 420ms → 50ms (88% reduction)

---

## Best Practices

### ✅ DO

1. **Always enable cache warming in production**
2. **Monitor trends over time** - Track average durations
3. **Set performance budgets** - Alert if phases exceed thresholds
4. **Focus on high-impact optimizations** - Target phases >20% of total time
5. **Test with realistic data** - Use production-like payloads

### ❌ DON'T

1. **Don't optimize prematurely** - Measure first
2. **Don't ignore warnings** - They indicate potential issues
3. **Don't compare dev vs prod** - Different hardware, cache states
4. **Don't forget to warm caches** - Cold cache tests are misleading
5. **Don't track everything** - Focus on business-critical operations

---

## Integration with Monitoring Tools

### Prometheus/Grafana

Export metrics to Prometheus for dashboards:

```java
@Component
public class MetricsExporter {
    private final MeterRegistry meterRegistry;
    
    public void export(PerformanceMetrics metrics) {
        Timer.builder("pdf.generation.duration")
            .tag("config", metrics.getConfigName())
            .register(meterRegistry)
            .record(metrics.getTotalDurationMs(), TimeUnit.MILLISECONDS);
    }
}
```

### Application Insights / New Relic

Add custom events for APM tools:

```java
TelemetryClient telemetry = new TelemetryClient();
telemetry.trackEvent("PdfGeneration", 
    Map.of("config", configName, "duration", totalMs));
```

---

## Summary

The performance monitoring system gives you:

✅ **Visibility** - See exactly where time is spent  
✅ **Actionable Data** - Identify optimization opportunities  
✅ **Production Ready** - Minimal overhead, thread-safe  
✅ **Easy Integration** - Automatic tracking with optional custom phases  

Use the `/api/document/generate-with-metrics` endpoint during development and testing to fine-tune your PDF generation performance!
