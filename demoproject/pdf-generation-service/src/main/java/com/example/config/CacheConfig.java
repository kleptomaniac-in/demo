package com.example.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.caching.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "pdfConfigs",           // PDF merge configurations
            "yamlComponents",       // Individual YAML component files (shared across configs)
            "acroformTemplates",    // AcroForm PDF templates (as bytes)
            "configFile",           // Config server file cache
            "appSource"             // Config server app source cache
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)                    // Cache up to 500 items per cache
            .expireAfterWrite(1, TimeUnit.HOURS) // Expire after 1 hour
            .recordStats()                       // Enable cache statistics
        );
        return cacheManager;
    }
}
