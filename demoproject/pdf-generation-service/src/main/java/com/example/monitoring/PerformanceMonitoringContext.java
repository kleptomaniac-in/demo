package com.example.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local context for tracking performance metrics during PDF generation.
 * Allows nested operations to contribute timing data to a single metrics object.
 */
@Component
public class PerformanceMonitoringContext {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringContext.class);
    
    private final ThreadLocal<PerformanceMetrics> currentMetrics = new ThreadLocal<>();
    private final ThreadLocal<Map<String, Long>> phaseStartTimes = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    /**
     * Start a new monitoring context
     */
    public PerformanceMetrics start(String operation, String configName) {
        PerformanceMetrics metrics = new PerformanceMetrics(operation, configName);
        currentMetrics.set(metrics);
        phaseStartTimes.get().clear();
        log.debug("Started performance monitoring for: {} (config: {})", operation, configName);
        return metrics;
    }
    
    /**
     * Start timing a phase
     */
    public void startPhase(String phaseName) {
        phaseStartTimes.get().put(phaseName, System.currentTimeMillis());
    }
    
    /**
     * End timing a phase and record it
     */
    public void endPhase(String phaseName) {
        Long startTime = phaseStartTimes.get().remove(phaseName);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            PerformanceMetrics metrics = currentMetrics.get();
            if (metrics != null) {
                metrics.addPhase(phaseName, duration);
            }
        }
    }
    
    /**
     * End timing a phase with additional details
     */
    public void endPhase(String phaseName, Map<String, Object> details) {
        Long startTime = phaseStartTimes.get().remove(phaseName);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            PerformanceMetrics metrics = currentMetrics.get();
            if (metrics != null) {
                metrics.addPhase(phaseName, duration, details);
            }
        }
    }
    
    /**
     * Add a warning to current metrics
     */
    public void addWarning(String warning) {
        PerformanceMetrics metrics = currentMetrics.get();
        if (metrics != null) {
            metrics.addWarning(warning);
        }
    }
    
    /**
     * Get current metrics (or null if not started)
     */
    public PerformanceMetrics getCurrentMetrics() {
        return currentMetrics.get();
    }
    
    /**
     * Complete monitoring and return final metrics
     */
    public PerformanceMetrics complete() {
        PerformanceMetrics metrics = currentMetrics.get();
        if (metrics != null) {
            metrics.complete();
            log.info(metrics.getSummary());
        }
        return metrics;
    }
    
    /**
     * Clear the context (important for thread pool reuse)
     */
    public void clear() {
        currentMetrics.remove();
        phaseStartTimes.remove();
    }
}
