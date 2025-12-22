# Cache Warming Guide

## Overview

Cache warming preloads frequently-used configs and templates into memory at application startup, eliminating the "cold start" penalty on first requests.

**Without cache warming:**
```
Application starts → First request arrives → Load config (15ms) + Load template (50ms) = 65ms delay
```

**With cache warming:**
```
Application starts → Preload all configs & templates (3s) → First request arrives → Cache hit (<1ms)
```

---

## What Gets Preloaded

Cache warming preloads **all template types**:

1. **YAML Configs** - PDF merge configuration files (`*.yml`)
2. **YAML Components** - Shared component files (`templates/components/*.yml`)
3. **AcroForm Templates** - PDF form templates (`*.pdf` in `acroforms/`)
4. **FreeMarker Templates** - HTML/text templates (`*.ftl` in `templates/`)

All templates are loaded in parallel using configurable thread pools.

| Benefit | Description |
|---------|-------------|
| **Instant Production Readiness** | Application is fully warm and ready to serve requests immediately after startup |
| **Consistent Response Times** | All requests are fast (no first-request penalty) |
| **Better Health Checks** | Kubernetes readiness probes pass immediately |
| **Fail-Fast Validation** | Invalid configs/templates detected at startup, not at runtime |
| **Improved User Experience** | No user experiences slow "first request" |

---

## Configuration

### Enable/Disable Cache Warming

**application.yml:**
```yaml
app:
  cache-warming:
    enabled: true  # false = disable cache warming
```

**Environment Variable:**
```bash
APP_CACHE_WARMING_ENABLED=false
```

---

### Configure What to Preload

**Config Patterns** - Which YAML config files to preload:
```yaml
app:
  cache-warming:
    config-patterns: >
      dental-individual-*.yml,
      dental-small-group-*.yml,
      medical-individual-*.yml,
      vision-*.yml,
      enrollment-*.yml
```

**AcroForm Template Patterns** - Which PDF form templates to preload:
```yaml
app:
  cache-warming:
    acroform-patterns: "*.pdf"  # All PDF files in acroforms/ directory
```

**FreeMarker Template Patterns** - Which FreeMarker templates to preload:
```yaml
app:
  cache-warming:
    freemarker-patterns: "*.ftl"  # All FTL files in templates/ directory
```

**Pattern Syntax:**
- `*.yml` - All YAML files
- `*.pdf` - All PDF files (AcroForm templates)
- `*.ftl` - All FreeMarker templates
- `dental-*.yml` - All dental configs
- `*-individual-ca.yml` - All individual California configs
- `enrollment-multi-*.yml` - All multi-product enrollment configs
- `templates/invoice/*.ftl` - All FTL files in invoice subdirectory

---

### Performance Tuning

**Parallel Threads** - More threads = faster preloading:
```yaml
app:
  cache-warming:
    parallel-threads: 4  # Default: 4
```

**Recommended values:**
- Development: 2 threads (lighter CPU usage)
- Production: 4-8 threads (faster startup)
- High-volume production: 8-12 threads (balance with available CPU cores)

**Preload Components** - Whether to preload shared YAML component files:
```yaml
app:
  cache-warming:
    preload-components: true  # Default: true
```

Set to `false` if you don't use component composition.

---

## Startup Logs

**Successful cache warming:**
```
2025-12-22 10:15:30.123  INFO  CacheWarmingService : === Cache Warming Started ===
2025-12-22 10:15:30.456  INFO  CacheWarmingService : Preloading YAML components...
2025-12-22 10:15:30.789  INFO  CacheWarmingService : Discovered 15 component files
2025-12-22 10:15:31.012  INFO  CacheWarmingService : Preloaded 15 component files
2025-12-22 10:15:31.234  INFO  CacheWarmingService : Discovering config files with patterns: dental-*.yml,medical-*.yml
2025-12-22 10:15:31.456  INFO  CacheWarmingService : Discovered 8 config files matching patterns
2025-12-22 10:15:31.678  INFO  CacheWarmingService : Preloading 8 configs with 4 threads...
2025-12-22 10:15:32.901  INFO  CacheWarmingService : Preloaded 8 configs
2025-12-22 10:15:32.123  INFO  CacheWarmingService : Discovering AcroForm template files with patterns: *.pdf
2025-12-22 10:15:32.345  INFO  CacheWarmingService : Discovered 5 AcroForm template files matching patterns
2025-12-22 10:15:32.567  INFO  CacheWarmingService : Preloading 5 AcroForm templates with 4 threads...
2025-12-22 10:15:33.789  INFO  CacheWarmingService : Preloaded 5 AcroForm templates
2025-12-22 10:15:33.012  INFO  CacheWarmingService : Discovering FreeMarker template files with patterns: *.ftl
2025-12-22 10:15:33.234  INFO  CacheWarmingService : Discovered 3 FreeMarker template files matching patterns
2025-12-22 10:15:33.456  INFO  CacheWarmingService : Preloading 3 FreeMarker templates with 4 threads...
2025-12-22 10:15:33.678  INFO  CacheWarmingService : Preloaded 3 FreeMarker templates
2025-12-22 10:15:33.012  INFO  CacheWarmingService : === Cache Warming Completed in 2889ms ===
2025-12-22 10:15:33.234  INFO  CacheWarmingService :   - Configs preloaded: 8
2025-12-22 10:15:33.456  INFO  CacheWarmingService :   - AcroForm templates preloaded: 5
2025-12-22 10:15:33.678  INFO  CacheWarmingService :   - FreeMarker templates preloaded: 3
2025-12-22 10:15:33.901  INFO  CacheWarmingService :   - Application is now production-ready with warm caches
2025-12-22 10:15:33.456  WARN  CacheWarmingService :   ✗ Failed to load FreeMarker template templates/old-invoice.ftl: Template not found
```

**With warnings (non-critical):**
```
2025-12-22 10:15:32.901  WARN  CacheWarmingService :   ✗ Failed to load config dental-old-config.yml: File not found
```
Note: Warnings don't prevent application startup. The application continues with cold caches for failed items.

---

## Performance Impact

### Startup Time

| Configuration | Startup Time | First Request | Notes |
|--------------|--------------|---------------|-------|
| **Cache warming OFF** | ~2s | 35-65ms | Fast startup, slow first request |
| **Cache warming ON (4 threads)** | ~4-6s | <1ms | Slower startup, instant requests |
| **Cache warming ON (8 threads)** | ~3-4s | <1ms | Faster preload with more CPU |

### Memory Usage

- **Without warming:** Gradual increase as requests come in
- **With warming:** Immediate spike at startup, then stable

**Example memory usage:**
- AppYAML configs cached: +5MB
- 10 AcroForm templates cached: +15MB
- 5 FreeMarker templates cached: +2MB
- **Total:** ~222ched: +15MB
- **Total:** ~220MB (stable)

---

## Production Recommendations

### ✅ **Enable Cache Warming When:**

1. **Production environments** - User experience matters
2. **High-volume APIs** - Every millisecond counts
3. **Kubernetes deployments** - Readiness probes need fast response
4. **SLA requirements** - <100ms response time guarantee
5. **Known config set** - You know which configs/templates are used

### ❌ **Disable Cache Warming When:**

1. **Development** - Faster startup during coding
2. **Testing** - Fresh state for each test run
3. **Dynamic configs** - Configs created on-the-fly
4. **Large config sets** - 100+ configs (startup too slow)
5. **CI/CD builds** - Fast startup for tests

---

## Environment-Specific Configuration

### Development (Fast Startup)

```yaml
app:
  cache-warming:
    enabled: false  # Disable for fast restarts during development
```

### Staging (Validate Preloading)

```yaml
app:
  cache-warming:
    enabled: true
    config-patterns: "*-individual-*.yml"  # Only critical configs
    parallel-threads: 2  # Lighter resource usage
```

### Production (Full Preloading)

```yaml
app:
  cache-warming:
    enabled: true
    config-patterns: >
      dental-*.yml,
      medical-*.yml,
      vision-*.yml,
      enrollment-*.yml
    parallel-threads: 8  # Faster preload
    preload-components: true  # All components
```

---

## Monitoring Cache Warming

### Check Startup Logs

Look for the summary message:
```
=== Cache Warming Completed in 2889ms ===
  - Configs preloaded: 8
  - Templates preloaded: 5
```

### Verify Cache Stats

**Endpoint:** `GET /api/admin/cache/stats`

**Response:**
```json
{
  "pdfConfigs": {
    "hitRate": "100.00%",
    "hitCount": 150,
    "missCount": 0,
    "size": 8
  },
  "acroformTemplates": {
    "hitRate": "100.00%",
    "hitCount": 200,
    "missCount": 0,
    "size": 5
  }
}
```

**What to look for:**
- **100% hit rate** = Cache warming successful
- **0 miss count** = No cold starts
- **Size matches preloaded count** = All items cached

### Health Check Integration

**Example Kubernetes readiness probe:**
```yaml
readinessProbe:
  httpGet:
    path: /api/admin/cache/health
    port: 8080
  initialDelaySeconds: 10  # Wait for cache warming to complete
  periodSeconds: 5
```

**Healthy response:**
```json
{
  "status": "healthy",
  "averageHitRate": "98.50%",
  "totalCachedItems": 28,
  "caches": {
    "pdfConfigs": { "size": 8, "hitRate": "100%" },
    "yamlComponents": { "size": 15, "hitRate": "100%" },
    "acroformTemplates": { "size": 5, "hitRate": "95%" },
    "freemarkerTemplates": { "size": 3, "hitRate": "100%" }
  }
}
```

---

## Troubleshooting

### Problem: Cache warming takes too long

**Solutions:**
1. Increase `parallel-threads` to 8 or 12
2. Reduce `config-patterns` to only critical configs
3. Set `preload-components: false` if not using composition

### Problem: Out of memory during startup

**Solutions:**
1. Reduce number of configs/templates being preloaded
2. Increase JVM heap size: `-Xmx512m` → `-Xmx1024m`
3. Split preloading patterns across multiple deployments

### Problem: Some configs failing to preload

**Check logs for:**
```
✗ Failed to load config dental-old-config.yml: File not found
```

**Solutions:**
1. Remove invalid config from patterns
2. Fix broken YAML syntax in config file
3. Ensure config file exists in `config-repo/`

### Problem: First request still slow after cache warming

**Verify cache was actually warmed:**
```bash
curl http://localhost:8080/api/admin/cache/stats
```

Look for:
- `size > 0` (items were cached)
- `hitCount > 0` (cache is being used)
- `missCount = 0` (no cold starts)

If cache is empty, check:
1. Cache warming is enabled: `app.cache-warming.enabled=true`
2. Patterns match your config files
3. No errors in startup logs

---

## Advanced: Custom Preloading Logic

If you need custom preloading (e.g., preload based on database records), extend `CacheWarmingService`:

```java
@Service
@ConditionalOnProperty(name = "app.cache-warming.enabled", havingValue = "true")
public class CustomCacheWarmingService implements CommandLineRunner {
    
    @Autowired
    private PdfMergeConfigService configService;
    
    @Autowired
    private ConfigRepository configRepository;  // Your DB repository
    
    @Override
    public void run(String... args) {
        // Preload configs from database
        List<String> activeConfigs = configRepository.findActiveConfigs();
        
        activeConfigs.forEach(configName -> {
            try {
                configService.loadConfig(configName);
                log.info("Preloaded config: {}", configName);
            } catch (Exception e) {
                log.warn("Failed to preload {}: {}", configName, e.getMessage());
            }
        });
    }
}
```

---

## Comparison with Other Approaches

### Alternative 1: Lazy Loading (Current Default)

**Pros:**
- Fast application startup
- Memory usage grows gradually
- No need to know config set upfront

**Cons:**
- First request per config is slow
- Unpredictable response times
- Not production-ready immediately

### Alternative 2: Cache Warming (This Implementation)

**Pros:**
- Instant production readiness
- Consistent response times
- Fail-fast validation
- Better health checks

**Cons:**
- Slower application startup
- Higher initial memory usage
- Requires knowing config set

### Alternative 3: Eternal Cache (No Expiration)

**Not recommended:**
- Config changes never reflected
- Memory leak risk over time
- Requires manual cache eviction

---

## See Also

- [CONFIG-CACHING-GUIDE.md](CONFIG-CACHING-GUIDE.md) - How caching works
- [TEMPLATE-CACHING-SUMMARY.md](TEMPLATE-CACHING-SUMMARY.md) - What's being cached
- Cache admin endpoint: `POST /api/admin/cache/clear-all`
