package com.example.controller;

import com.example.service.AcroFormFillService;
import com.example.service.PdfMergeConfigService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.*;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoints for cache management and monitoring
 */
@RestController
@RequestMapping("/api/admin/cache")
public class CacheAdminController {

    private final CacheManager cacheManager;
    private final PdfMergeConfigService configService;
    private final AcroFormFillService acroFormService;

    public CacheAdminController(
        CacheManager cacheManager,
        PdfMergeConfigService configService,
        AcroFormFillService acroFormService
    ) {
        this.cacheManager = cacheManager;
        this.configService = configService;
        this.acroFormService = acroFormService;
    }

    /**
     * Get statistics for all caches
     * GET /api/admin/cache/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getAllCacheStats() {
        Map<String, Object> allStats = new HashMap<>();
        
        allStats.put("pdfConfigs", getCacheStats("pdfConfigs"));
        allStats.put("acroformTemplates", getCacheStats("acroformTemplates"));
        allStats.put("configFile", getCacheStats("configFile"));
        allStats.put("appSource", getCacheStats("appSource"));
        
        return allStats;
    }
    
    /**
     * Get statistics for a specific cache
     * GET /api/admin/cache/stats/{cacheName}
     */
    @GetMapping("/stats/{cacheName}")
    public Map<String, Object> getCacheStats(@PathVariable String cacheName) {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
        
        if (cache == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cache not found: " + cacheName);
            return error;
        }
        
        CacheStats stats = cache.getNativeCache().stats();
        
        Map<String, Object> result = new HashMap<>();
        result.put("cacheName", cacheName);
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
        result.put("evictionCount", stats.evictionCount());
        result.put("estimatedSize", cache.getNativeCache().estimatedSize());
        result.put("loadSuccessCount", stats.loadSuccessCount());
        result.put("loadFailureCount", stats.loadFailureCount());
        result.put("totalLoadTime", stats.totalLoadTime());
        result.put("averageLoadPenalty", stats.averageLoadPenalty());
        
        return result;
    }
    
    /**
     * Clear specific cache
     * POST /api/admin/cache/clear/{cacheName}
     */
    @PostMapping("/clear/{cacheName}")
    public Map<String, String> clearCache(@PathVariable String cacheName) {
        switch (cacheName) {
            case "pdfConfigs":
                configService.clearCache();
                break;
            case "acroformTemplates":
                acroFormService.clearTemplateCache();
                break;
            case "configFile":
            case "appSource":
                cacheManager.getCache(cacheName).clear();
                break;
            default:
                return Map.of("error", "Unknown cache: " + cacheName);
        }
        
        return Map.of(
            "message", "Cache cleared successfully",
            "cacheName", cacheName
        );
    }
    
    /**
     * Clear all caches
     * POST /api/admin/cache/clear-all
     */
    @PostMapping("/clear-all")
    public Map<String, String> clearAllCaches() {
        configService.clearCache();
        acroFormService.clearTemplateCache();
        cacheManager.getCache("configFile").clear();
        cacheManager.getCache("appSource").clear();
        
        return Map.of("message", "All caches cleared successfully");
    }
    
    /**
     * Evict specific config from cache
     * POST /api/admin/cache/evict/config/{configName}
     */
    @PostMapping("/evict/config/{configName}")
    public Map<String, String> evictConfig(@PathVariable String configName) {
        configService.evictConfig(configName);
        
        return Map.of(
            "message", "Config evicted successfully",
            "configName", configName
        );
    }
    
    /**
     * Evict specific AcroForm template from cache
     * POST /api/admin/cache/evict/acroform/{templatePath}
     */
    @PostMapping("/evict/acroform/**")
    public Map<String, String> evictAcroFormTemplate(@RequestParam String templatePath) {
        acroFormService.evictTemplate(templatePath);
        
        return Map.of(
            "message", "AcroForm template evicted successfully",
            "templatePath", templatePath
        );
    }
    
    /**
     * Get cache health summary
     * GET /api/admin/cache/health
     */
    @GetMapping("/health")
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();
        
        Map<String, Object> pdfConfigStats = getCacheStats("pdfConfigs");
        Map<String, Object> acroformStats = getCacheStats("acroformTemplates");
        Map<String, Object> configFileStats = getCacheStats("configFile");
        Map<String, Object> appSourceStats = getCacheStats("appSource");
        
        // Calculate overall health
        double avgHitRate = (
            parseHitRate(pdfConfigStats) + 
            parseHitRate(acroformStats) +
            parseHitRate(configFileStats) +
            parseHitRate(appSourceStats)
        ) / 4.0;
        
        long totalSize = 
            (long) pdfConfigStats.getOrDefault("estimatedSize", 0L) +
            (long) acroformStats.getOrDefault("estimatedSize", 0L) +
            (long) configFileStats.getOrDefault("estimatedSize", 0L) +
            (long) appSourceStats.getOrDefault("estimatedSize", 0L);
        
        health.put("status", avgHitRate > 50 ? "HEALTHY" : "DEGRADED");
        health.put("averageHitRate", String.format("%.2f%%", avgHitRate));
        health.put("totalCachedItems", totalSize);
        health.put("note", "FreeMarker templates cached natively by FreeMarker engine");
        health.put("caches", Map.of(
            "pdfConfigs", pdfConfigStats,
            "acroformTemplates", acroformStats,
            "configFile", configFileStats,
            "appSource", appSourceStats
        ));
        return health;
    }
    
    private double parseHitRate(Map<String, Object> stats) {
        String hitRate = (String) stats.getOrDefault("hitRate", "0.00%");
        return Double.parseDouble(hitRate.replace("%", ""));
    }
}
