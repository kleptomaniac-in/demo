# Config File Caching Guide

## 1. Add Spring Cache Dependency

**pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## 2. Enable Caching in Application

**CacheConfig.java:**
```java
package com.example.pdf.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("pdfConfigs");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)                    // Cache up to 500 configs
            .expireAfterWrite(1, TimeUnit.HOURS) // Expire after 1 hour
            .recordStats()                       // Enable cache statistics
        );
        return cacheManager;
    }
}
```

## 3. Add @Cacheable to Service

**PdfMergeConfigService.java:**
```java
package com.example.pdf.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class PdfMergeConfigService {

    /**
     * Load config with caching
     * Same config name = returns cached result, no file I/O
     */
    @Cacheable(value = "pdfConfigs", key = "#configName")
    public PdfMergeConfig loadConfig(String configName) {
        log.debug("Loading config from disk (cache miss): {}", configName);
        
        Map<String, Object> data = loadYamlFile(configName);
        
        if (data.containsKey("composition")) {
            Map<String, Object> composition = (Map<String, Object>) data.get("composition");
            return loadComposedConfig(composition, data);
        }
        
        return parsePdfMergeConfig(data);
    }
    
    /**
     * Evict specific config from cache (e.g., after hot-reload)
     */
    @CacheEvict(value = "pdfConfigs", key = "#configName")
    public void evictConfig(String configName) {
        log.info("Evicted config from cache: {}", configName);
    }
    
    /**
     * Clear entire cache
     */
    @CacheEvict(value = "pdfConfigs", allEntries = true)
    public void clearCache() {
        log.info("Cleared all configs from cache");
    }
}
```

## 4. Pre-Load Common Configs at Startup (Optional)

**ConfigPreloader.java:**
```java
package com.example.pdf.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigPreloader {

    private final PdfMergeConfigService configService;
    
    private static final List<String> COMMON_CONFIGS = List.of(
        "medical-individual-ca.yml",
        "medical-individual-tx.yml",
        "medical-individual-ny.yml",
        "dental-medical-individual-ca.yml",
        "dental-medical-individual-tx.yml",
        "medical-medicare-ca.yml",
        "medical-medicare-tx.yml",
        "medical-medicare-fl.yml"
    );

    public ConfigPreloader(PdfMergeConfigService configService) {
        this.configService = configService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preloadConfigs() {
        log.info("Pre-loading {} common configs into cache...", COMMON_CONFIGS.size());
        
        int loaded = 0;
        for (String configName : COMMON_CONFIGS) {
            try {
                configService.loadConfig(configName);
                loaded++;
            } catch (Exception e) {
                log.warn("Failed to pre-load config: {}", configName, e);
            }
        }
        
        log.info("Pre-loaded {}/{} configs into cache", loaded, COMMON_CONFIGS.size());
    }
}
```

## 5. Cache Configuration Options

**application.yml:**
```yaml
spring:
  cache:
    cache-names: pdfConfigs
    caffeine:
      spec: maximumSize=500,expireAfterWrite=1h,recordStats

# Or for development (no caching):
# spring:
#   cache:
#     type: none
```

## 6. Monitor Cache Performance

**CacheMetricsController.java:**
```java
package com.example.pdf.controller;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
public class CacheMetricsController {

    private final CacheManager cacheManager;

    public CacheMetricsController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("pdfConfigs");
        if (cache == null) {
            return Map.of("error", "Cache not found");
        }
        
        CacheStats stats = cache.getNativeCache().stats();
        
        Map<String, Object> result = new HashMap<>();
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("hitRate", stats.hitRate());
        result.put("evictionCount", stats.evictionCount());
        result.put("estimatedSize", cache.getNativeCache().estimatedSize());
        
        return result;
    }
    
    @PostMapping("/clear")
    public Map<String, String> clearCache() {
        cacheManager.getCache("pdfConfigs").clear();
        return Map.of("message", "Cache cleared successfully");
    }
}
```

## 7. Testing Cache Behavior

```java
@SpringBootTest
public class ConfigCachingTest {

    @Autowired
    private PdfMergeConfigService configService;
    
    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testConfigCaching() {
        String configName = "medical-individual-ca.yml";
        
        // First call - loads from disk (cache miss)
        long start1 = System.currentTimeMillis();
        PdfMergeConfig config1 = configService.loadConfig(configName);
        long time1 = System.currentTimeMillis() - start1;
        
        // Second call - loads from cache (cache hit)
        long start2 = System.currentTimeMillis();
        PdfMergeConfig config2 = configService.loadConfig(configName);
        long time2 = System.currentTimeMillis() - start2;
        
        // Verify same instance (cached)
        assertSame(config1, config2);
        
        // Cache should be much faster
        assertTrue(time2 < time1 / 10); // At least 10x faster
        
        System.out.println("Disk load: " + time1 + "ms");
        System.out.println("Cache load: " + time2 + "ms");
    }
    
    @Test
    public void testCacheEviction() {
        String configName = "medical-individual-ca.yml";
        
        // Load config (cache)
        PdfMergeConfig config1 = configService.loadConfig(configName);
        
        // Evict from cache
        configService.evictConfig(configName);
        
        // Load again (should reload from disk)
        PdfMergeConfig config2 = configService.loadConfig(configName);
        
        // Different instances (new load)
        assertNotSame(config1, config2);
    }
}
```

## Performance Comparison

### Without Caching
```
Request 1: Load medical-individual-ca.yml → 15ms (disk I/O + YAML parse + merge)
Request 2: Load medical-individual-ca.yml → 15ms (disk I/O + YAML parse + merge)
Request 3: Load medical-individual-ca.yml → 15ms (disk I/O + YAML parse + merge)

Total: 45ms for 3 requests
```

### With Caching
```
Request 1: Load medical-individual-ca.yml → 15ms (disk I/O + YAML parse + merge + cache store)
Request 2: Load medical-individual-ca.yml → 0.1ms (cache hit)
Request 3: Load medical-individual-ca.yml → 0.1ms (cache hit)

Total: 15.2ms for 3 requests (3x faster)
```

## Cache Key Strategy

**Simple key:**
```java
@Cacheable(value = "pdfConfigs", key = "#configName")
public PdfMergeConfig loadConfig(String configName)
```
- Cache key: `"medical-individual-ca.yml"`

**Complex key (if you have multi-tenancy):**
```java
@Cacheable(value = "pdfConfigs", key = "#tenantId + ':' + #configName")
public PdfMergeConfig loadConfig(String tenantId, String configName)
```
- Cache key: `"acme:medical-individual-ca.yml"`

## Cache Eviction Strategies

### Time-Based (Default)
```java
Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)  // Evict after 1 hour
```

### Size-Based
```java
Caffeine.newBuilder()
    .maximumSize(500)  // Keep only 500 most recent configs
```

### Manual Eviction (Hot Reload)
```java
@PostMapping("/api/admin/config/reload/{configName}")
public void reloadConfig(@PathVariable String configName) {
    configService.evictConfig(configName);  // Remove from cache
    configService.loadConfig(configName);   // Reload fresh
}
```

### Scheduled Eviction (Nightly Refresh)
```java
@Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
public void refreshCache() {
    log.info("Nightly cache refresh");
    configService.clearCache();
    preloadConfigs();
}
```

## Production Recommendations

1. **Cache Size**: Set based on expected config count
   - 315 possible combinations → `maximumSize=500`
   - Pre-load top 20 → Other 295 load on-demand

2. **Expiration**: Balance freshness vs performance
   - Production: `expireAfterWrite(1, TimeUnit.HOURS)` or longer
   - Development: `expireAfterWrite(5, TimeUnit.MINUTES)` for faster iteration

3. **Pre-loading**: Load common configs at startup
   - Reduces first-request latency
   - Warms up the cache

4. **Monitoring**: Track cache hit rate
   - Target: >90% hit rate for common configs
   - Low hit rate → increase cache size or pre-load more configs

5. **Invalidation**: Implement manual eviction endpoint
   - Use when configs change on disk
   - Trigger via CI/CD after config updates

## Summary

**Setup Steps:**
1. Add dependencies: `spring-boot-starter-cache` + `caffeine`
2. Enable caching: `@EnableCaching` + `CacheConfig`
3. Annotate method: `@Cacheable` on `loadConfig()`
4. Optional: Pre-load common configs at startup
5. Optional: Add cache metrics endpoint

**Result:**
- First request: 15ms (disk load)
- Subsequent requests: <1ms (memory cache)
- 10-100x faster for repeated config loads
- Especially useful for high-traffic APIs using same configs
