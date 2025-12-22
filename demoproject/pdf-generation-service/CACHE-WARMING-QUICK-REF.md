# Cache Warming vs Lazy Loading - Quick Reference

## Quick Decision Matrix

```
┌─────────────────────────────────────────────────────────────────┐
│  Choose CACHE WARMING when:                                     │
│  ✅ Production environment (user experience critical)           │
│  ✅ SLA requirements (<100ms response time)                     │
│  ✅ Known set of configs (dental, medical, vision)              │
│  ✅ Kubernetes with readiness probes                            │
│  ✅ High-volume API (every millisecond matters)                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Choose LAZY LOADING when:                                      │
│  ✅ Development (fast restart during coding)                    │
│  ✅ Testing (fresh state for each test)                         │
│  ✅ Dynamic configs (created on-the-fly)                        │
│  ✅ Large config set (100+ configs = slow startup)              │
│  ✅ Limited memory environment                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Performance Comparison

| Metric | Lazy Loading | Cache Warming |
|--------|--------------|---------------|
| **Application Startup** | ~2s ⚡ | ~4-6s |
| **First Request** | 35-65ms ⚠️ | <1ms ⚡ |
| **Subsequent Requests** | <1ms ⚡ | <1ms ⚡ |
| **Memory at Startup** | Low (~200MB) | High (~220MB) |
| **Memory Growth** | Gradual | Immediate |
| **Production Ready** | After first request | Immediately ⚡ |

---

## Configuration

### Enable Cache Warming (Production)

**application.yml:**
```yaml
app:
  cache-warming:
    enabled: true
    config-patterns: dental-*.yml,medical-*.yml,vision-*.yml
    template-patterns: "*.pdf"
    parallel-threads: 8
```

### Disable Cache Warming (Development)

**application.yml:**
```yaml
app:
  cache-warming:
    enabled: false  # Fast startup for development
```

**Or via environment:**
```bash
export APP_CACHE_WARMING_ENABLED=false
```

---

## Startup Log Examples

### ✅ Cache Warming Enabled

```
2025-12-22 10:15:30  INFO  CacheWarmingService : === Cache Warming Started ===
2025-12-22 10:15:31  INFO  CacheWarmingService : Preloaded 15 component files
2025-12-22 10:15:32  INFO  CacheWarmingService : Preloaded 8 configs
2025-12-22 10:15:33  INFO  CacheWarmingService : Preloaded 5 templates
2025-12-22 10:15:33  INFO  CacheWarmingService : === Cache Warming Completed in 2889ms ===
2025-12-22 10:15:33  INFO  CacheWarmingService :   - Application is now production-ready
```

### ❌ Cache Warming Disabled

```
2025-12-22 10:15:30  INFO  Application started in 1.234 seconds
(No cache warming logs - caches will be populated on first use)
```

---

## Runtime Behavior

### Lazy Loading Flow

```
Request arrives
    ↓
Cache miss (first time)
    ↓
Load from disk (15-50ms) ← DELAY
    ↓
Parse YAML/PDF
    ↓
Cache result
    ↓
Generate PDF
    ↓
Return response (65ms total)

Next request for same config:
    ↓
Cache hit (<1ms) ← FAST
```

### Cache Warming Flow

```
Application startup
    ↓
Preload all configs (parallel) ← UPFRONT COST
    ↓
Preload all templates (parallel)
    ↓
Application ready (4-6s total)

Every request:
    ↓
Cache hit (<1ms) ← ALWAYS FAST
```

---

## Environment-Specific Recommendations

| Environment | Cache Warming | Config Patterns | Parallel Threads |
|-------------|--------------|-----------------|------------------|
| **Development** | ❌ Disabled | N/A | N/A |
| **Testing** | ❌ Disabled | N/A | N/A |
| **Staging** | ✅ Enabled | Critical configs only | 2-4 |
| **Production** | ✅ Enabled | All active configs | 4-8 |
| **High-Volume Prod** | ✅ Enabled | All configs | 8-12 |

---

## Monitoring

### Check Cache Status

```bash
curl http://localhost:8080/api/admin/cache/stats
```

**With cache warming (good):**
```json
{
  "pdfConfigs": {
    "hitRate": "100.00%",  ← Perfect hit rate
    "missCount": 0,         ← No cold starts
    "size": 8               ← All configs cached
  }
}
```

**Without cache warming (expected):**
```json
{
  "pdfConfigs": {
    "hitRate": "87.50%",   ← Some cache misses
    "missCount": 2,         ← 2 cold starts
    "size": 6               ← Gradually populated
  }
}
```

---

## When to Reconsider

### Switch FROM lazy loading TO cache warming:

- Response time SLA violations
- First-request complaints from users
- Kubernetes pods failing readiness checks
- Inconsistent performance metrics

### Switch FROM cache warming TO lazy loading:

- Startup time exceeding acceptable limits (>10s)
- Out of memory errors at startup
- Too many config changes (cache invalidation overhead)
- Development environment (prefer fast restart)

---

## Advanced: Profile-Based Configuration

**application-dev.yml** (Development):
```yaml
app:
  cache-warming:
    enabled: false
```

**application-prod.yml** (Production):
```yaml
app:
  cache-warming:
    enabled: true
    config-patterns: >
      dental-individual-*.yml,
      dental-small-group-*.yml,
      medical-individual-*.yml,
      vision-*.yml
    parallel-threads: 8
```

**Run with profile:**
```bash
# Development (lazy loading)
java -jar app.jar --spring.profiles.active=dev

# Production (cache warming)
java -jar app.jar --spring.profiles.active=prod
```

---

## Summary

**Cache Warming = Better User Experience at Cost of Startup Time**

| Aspect | Winner |
|--------|--------|
| User-facing response time | Cache Warming ⚡ |
| Application startup speed | Lazy Loading ⚡ |
| Production readiness | Cache Warming ⚡ |
| Development experience | Lazy Loading ⚡ |
| Memory efficiency | Lazy Loading ⚡ |
| Consistency | Cache Warming ⚡ |

**Recommendation:** Use cache warming in production, disable in development.

---

## See Also

- [CACHE-WARMING-GUIDE.md](CACHE-WARMING-GUIDE.md) - Full documentation
- [CONFIG-CACHING-GUIDE.md](CONFIG-CACHING-GUIDE.md) - How caching works
- [TEMPLATE-CACHING-SUMMARY.md](TEMPLATE-CACHING-SUMMARY.md) - What's cached
