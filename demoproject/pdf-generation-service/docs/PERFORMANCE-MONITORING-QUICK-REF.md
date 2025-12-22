# Performance Monitoring Quick Reference

## Quick Start

### 1. View Performance Logs

Performance metrics are **automatically logged** after every PDF generation:

```bash
# Start the application and watch logs
mvn spring-boot:run

# Look for output like:
=== Performance Metrics: PDF Generation ===
Config: enrollment-multi-product
Total Duration: 845ms

Phase Breakdown:
  Load Config                   :     12ms (  1.4%)
  Section: enrollment-form      :    380ms ( 45.0%)
  Merge Documents               :     45ms (  5.3%)
  ...
```

### 2. Get Metrics via REST API

```bash
# Generate PDF with performance metrics
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "enrollment-multi-product",
    "payload": {...}
  }' | jq '.performanceMetrics'
```

### 3. Identify Bottlenecks

```bash
# Find slowest phase
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d @request.json | \
  jq '.performanceMetrics.phases | to_entries | sort_by(.value.durationMs) | reverse | .[0:3]'
```

---

## Common Optimizations

### Problem: Slow Config Loading (>30ms)

**Solution:** Enable cache warming

```yaml
# application.yml
app:
  cache-warming:
    enabled: true
    config-patterns: "enrollment-*.yml,dental-*.yml"
```

**Expected Result:** 30ms → 2ms (93% reduction)

---

### Problem: Slow AcroForm Filling (>200ms)

**Solution 1:** Cache the template

```yaml
app:
  cache-warming:
    enabled: true
    acroform-patterns: "*.pdf"
```

**Solution 2:** Reduce field count or use patterns

```yaml
# Instead of 50 individual field mappings:
patterns:
  - source: "applicants[relationship=PRIMARY]"
    prefix: "Primary_"
    fields: ["firstName", "lastName", "dateOfBirth"]
```

**Expected Result:** 420ms → 85ms (80% reduction)

---

### Problem: Slow FreeMarker Rendering (>300ms)

**Solution 1:** Preload templates

```yaml
app:
  cache-warming:
    enabled: true
    freemarker-patterns: "*.ftl"
```

**Solution 2:** Simplify template logic

```ftl
<!-- Before: O(n²) -->
<#list applicants as applicant>
  <#list coverages as coverage>
    ... expensive operation ...
  </#list>
</#list>

<!-- After: O(n) with preprocessing -->
<#list enrichedData as item>
  ... direct rendering ...
</#list>
```

**Expected Result:** 280ms → 95ms (66% reduction)

---

### Problem: Large PDF Serialization (>200ms)

**Solution:** Check PDF size and compression

```bash
# View PDF size from metrics
curl ... | jq '.performanceMetrics.phases."Serialize to Bytes".details.sizeBytes'

# If >2MB, optimize:
# 1. Compress images in templates
# 2. Remove unnecessary bookmarks/annotations
# 3. Use PDF/A compression
```

---

## Performance Targets

| Phase | Dev (Cold) | Dev (Warm) | Prod (Warm) |
|-------|------------|------------|-------------|
| **Load Config** | <30ms | <10ms | <5ms |
| **AcroForm Section** | <200ms | <100ms | <50ms |
| **FreeMarker Section** | <300ms | <150ms | <100ms |
| **4-Page PDF Total** | <1000ms | <500ms | <300ms |

---

## API Endpoints

### Generate PDF Only

```bash
POST /api/document/generate
Content-Type: application/json

{
  "configName": "enrollment-multi-product",
  "payload": {...},
  "outputFileName": "enrollment.pdf"
}

Response: PDF binary (application/pdf)
```

### Generate PDF with Metrics

```bash
POST /api/document/generate-with-metrics
Content-Type: application/json

{
  "configName": "enrollment-multi-product",
  "payload": {...}
}

Response: JSON with pdfBase64 and performanceMetrics
```

---

## Useful Commands

### Extract Slowest Phases

```bash
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d @request.json | \
  jq '.performanceMetrics.phases | 
      to_entries | 
      map({phase: .key, ms: .value.durationMs, pct: .value.percentage}) | 
      sort_by(.ms) | 
      reverse | 
      .[0:5]'
```

Output:
```json
[
  {"phase": "Section: enrollment-form", "ms": 420, "pct": 33.7},
  {"phase": "Generate Section PDFs", "ms": 890, "pct": 71.5},
  {"phase": "Serialize to Bytes", "ms": 90, "pct": 7.2},
  {"phase": "Add Header", "ms": 60, "pct": 4.8},
  {"phase": "Merge Documents", "ms": 50, "pct": 4.0}
]
```

### Compare Before/After Optimization

```bash
# Before
curl ... | jq '.performanceMetrics.totalDurationMs' > before.txt

# Apply optimization (e.g., enable cache warming)

# After
curl ... | jq '.performanceMetrics.totalDurationMs' > after.txt

# Calculate improvement
echo "Before: $(cat before.txt)ms, After: $(cat after.txt)ms"
```

### Save PDF from Metrics Response

```bash
curl -X POST http://localhost:8080/api/document/generate-with-metrics \
  -H "Content-Type: application/json" \
  -d @request.json | \
  jq -r '.pdfBase64' | base64 -d > output.pdf
```

---

## Monitoring Checklist

✅ Cache warming enabled  
✅ Config patterns configured for common configs  
✅ Template patterns configured  
✅ Log level set to INFO  
✅ Baseline metrics collected  
✅ Performance targets defined  
✅ Slow phases identified  
✅ Optimizations prioritized by impact  

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| No metrics in logs | Check `logging.level.com.example.monitoring: INFO` |
| Metrics incomplete | Verify try/finally blocks in services |
| Metrics slow down app | Disable if overhead >5% (rare) |
| Wrong thread metrics | Ensure `clear()` called in finally |

---

## Next Steps

1. **Baseline:** Run metrics on current production config
2. **Identify:** Find phases taking >20% of total time
3. **Optimize:** Apply solutions from this guide
4. **Measure:** Compare before/after metrics
5. **Iterate:** Repeat for next slowest phase

For detailed information, see [PERFORMANCE-MONITORING-GUIDE.md](PERFORMANCE-MONITORING-GUIDE.md)
