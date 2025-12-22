package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cache-warming")
public class CacheWarmingProperties {

    private boolean enabled = true;
    private String configPatterns = "dental-*.yml,medical-*.yml,vision-*.yml,enrollment-*.yml";
    private String acroformPatterns = "*.pdf";
    private String freemarkerPatterns = "*.ftl";
    private int parallelThreads = 4;
    private boolean preloadComponents = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigPatterns() {
        return configPatterns;
    }

    public void setConfigPatterns(String configPatterns) {
        this.configPatterns = configPatterns;
    }

    public String getAcroformPatterns() {
        return acroformPatterns;
    }

    public void setAcroformPatterns(String acroformPatterns) {
        this.acroformPatterns = acroformPatterns;
    }

    public String getFreemarkerPatterns() {
        return freemarkerPatterns;
    }

    public void setFreemarkerPatterns(String freemarkerPatterns) {
        this.freemarkerPatterns = freemarkerPatterns;
    }

    public int getParallelThreads() {
        return parallelThreads;
    }

    public void setParallelThreads(int parallelThreads) {
        this.parallelThreads = parallelThreads;
    }

    public boolean isPreloadComponents() {
        return preloadComponents;
    }

    public void setPreloadComponents(boolean preloadComponents) {
        this.preloadComponents = preloadComponents;
    }
}
