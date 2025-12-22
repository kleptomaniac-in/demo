package com.example.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds detailed performance metrics for PDF generation operations.
 * Tracks timing for each phase of the process.
 */
public class PerformanceMetrics {
    
    private String operation;
    private String configName;
    private Instant startTime;
    private Instant endTime;
    private long totalDurationMs;
    
    private Map<String, PhaseMetric> phases = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();
    
    public PerformanceMetrics(String operation, String configName) {
        this.operation = operation;
        this.configName = configName;
        this.startTime = Instant.now();
    }
    
    public void complete() {
        this.endTime = Instant.now();
        this.totalDurationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
    }
    
    public void addPhase(String phaseName, long durationMs) {
        phases.put(phaseName, new PhaseMetric(phaseName, durationMs));
    }
    
    public void addPhase(String phaseName, long durationMs, Map<String, Object> details) {
        phases.put(phaseName, new PhaseMetric(phaseName, durationMs, details));
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getConfigName() {
        return configName;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public long getTotalDurationMs() {
        return totalDurationMs;
    }
    
    public Map<String, PhaseMetric> getPhases() {
        return phases;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    /**
     * Get formatted summary for logging
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Performance Metrics: ").append(operation).append(" ===\n");
        sb.append("Config: ").append(configName).append("\n");
        sb.append("Total Duration: ").append(totalDurationMs).append("ms\n");
        sb.append("\nPhase Breakdown:\n");
        
        for (PhaseMetric phase : phases.values()) {
            double percentage = (phase.getDurationMs() * 100.0) / totalDurationMs;
            sb.append(String.format("  %-30s: %6dms (%5.1f%%)", 
                phase.getName(), phase.getDurationMs(), percentage));
            
            if (phase.getDetails() != null && !phase.getDetails().isEmpty()) {
                sb.append(" - ").append(phase.getDetails());
            }
            sb.append("\n");
        }
        
        if (!warnings.isEmpty()) {
            sb.append("\nWarnings:\n");
            for (String warning : warnings) {
                sb.append("  - ").append(warning).append("\n");
            }
        }
        
        sb.append("=====================================\n");
        return sb.toString();
    }
    
    /**
     * Get JSON-friendly map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("operation", operation);
        map.put("configName", configName);
        map.put("startTime", startTime.toString());
        map.put("endTime", endTime != null ? endTime.toString() : null);
        map.put("totalDurationMs", totalDurationMs);
        
        Map<String, Object> phasesMap = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseMetric> entry : phases.entrySet()) {
            Map<String, Object> phaseMap = new LinkedHashMap<>();
            phaseMap.put("durationMs", entry.getValue().getDurationMs());
            phaseMap.put("percentage", (entry.getValue().getDurationMs() * 100.0) / totalDurationMs);
            if (entry.getValue().getDetails() != null) {
                phaseMap.put("details", entry.getValue().getDetails());
            }
            phasesMap.put(entry.getKey(), phaseMap);
        }
        map.put("phases", phasesMap);
        
        if (!warnings.isEmpty()) {
            map.put("warnings", warnings);
        }
        
        return map;
    }
    
    public static class PhaseMetric {
        private String name;
        private long durationMs;
        private Map<String, Object> details;
        
        public PhaseMetric(String name, long durationMs) {
            this.name = name;
            this.durationMs = durationMs;
        }
        
        public PhaseMetric(String name, long durationMs, Map<String, Object> details) {
            this.name = name;
            this.durationMs = durationMs;
            this.details = details;
        }
        
        public String getName() {
            return name;
        }
        
        public long getDurationMs() {
            return durationMs;
        }
        
        public Map<String, Object> getDetails() {
            return details;
        }
    }
}
