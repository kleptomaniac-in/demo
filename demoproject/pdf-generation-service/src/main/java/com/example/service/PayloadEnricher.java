package com.example.service;

import java.util.Map;

/**
 * Interface for payload enrichers/transformers.
 * 
 * Use this to extract logic/data from existing PDFBox code and make it available
 * to FreeMarker templates without generating PDF directly with PDFBox.
 * 
 * Example use case:
 * - You have complex calculation logic in PDFBox generator
 * - You want to reuse that logic but render with FreeMarker instead
 * - Create a PayloadEnricher that calls your calculation logic
 * - Add calculated data to payload
 * - Use enriched payload in FreeMarker template
 */
public interface PayloadEnricher {
    
    /**
     * Enrich/transform the payload by adding calculated fields, formatted data, etc.
     * 
     * @param payload Original payload
     * @return Enriched payload with additional fields
     */
    Map<String, Object> enrich(Map<String, Object> payload);
    
    /**
     * Unique name for this enricher (used in YAML config)
     */
    String getName();
}
