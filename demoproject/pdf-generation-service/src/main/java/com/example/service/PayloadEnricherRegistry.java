package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for payload enrichers.
 * Auto-discovers all PayloadEnricher implementations.
 */
@Component
public class PayloadEnricherRegistry {
    
    private final Map<String, PayloadEnricher> enrichers = new HashMap<>();
    
    @Autowired(required = false)
    public PayloadEnricherRegistry(List<PayloadEnricher> enricherList) {
        if (enricherList != null) {
            for (PayloadEnricher enricher : enricherList) {
                enrichers.put(enricher.getName(), enricher);
            }
        }
    }
    
    /**
     * Get enricher by name
     */
    public PayloadEnricher getEnricher(String name) {
        PayloadEnricher enricher = enrichers.get(name);
        if (enricher == null) {
            throw new IllegalArgumentException("No payload enricher found with name: " + name);
        }
        return enricher;
    }
    
    /**
     * Check if enricher exists
     */
    public boolean hasEnricher(String name) {
        return enrichers.containsKey(name);
    }
    
    /**
     * Apply multiple enrichers in sequence
     */
    public Map<String, Object> applyEnrichers(List<String> enricherNames, Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        if (enricherNames != null) {
            for (String enricherName : enricherNames) {
                PayloadEnricher enricher = getEnricher(enricherName);
                enriched = enricher.enrich(enriched);
            }
        }
        
        return enriched;
    }
}
