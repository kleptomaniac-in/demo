package com.example.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to select the appropriate PDF template configuration
 * based on enrollment submission parameters.
 */
@Service
public class ConfigSelectionService {
    
    /**
     * Strategy 1: Convention-based config selection
     * Builds config filename from enrollment parameters.
     * 
     * Example: medical + dental, individual market, CA
     *   -> "dental-medical-individual-ca.yml"
     */
    public String selectConfigByConvention(EnrollmentSubmission enrollment) {
        // Sort products alphabetically for consistent naming
        List<String> products = enrollment.getProducts().stream()
            .map(String::toLowerCase)
            .sorted()
            .collect(Collectors.toList());
        
        String productKey = String.join("-", products);
        String market = enrollment.getMarketCategory().toLowerCase();
        String state = enrollment.getState().toLowerCase();
        
        return String.format("%s-%s-%s.yml", productKey, market, state);
    }
    
    /**
     * Strategy 2: Dynamic composition
     * Returns a map that can be used to build composition at runtime.
     */
    public Map<String, Object> buildDynamicComposition(EnrollmentSubmission enrollment) {
        Map<String, Object> composition = new HashMap<>();
        composition.put("base", "templates/base-payer.yml");
        
        List<String> components = new ArrayList<>();
        
        // Add product components
        for (String product : enrollment.getProducts()) {
            components.add("templates/products/" + product.toLowerCase() + ".yml");
        }
        
        // Add market component
        components.add("templates/markets/" + enrollment.getMarketCategory().toLowerCase() + ".yml");
        
        // Add state component
        components.add("templates/states/" + enrollment.getState().toLowerCase() + ".yml");
        
        composition.put("components", components);
        
        Map<String, Object> config = new HashMap<>();
        config.put("composition", composition);
        
        return config;
    }
    
    /**
     * Strategy 3: Fallback chain
     * Try specific config first, fall back to more general configs.
     */
    public String selectConfigWithFallback(EnrollmentSubmission enrollment) {
        // Try most specific first
        String specific = selectConfigByConvention(enrollment);
        
        // Could add logic to check if file exists, then try fallbacks:
        // 1. products-market-state.yml  (most specific)
        // 2. products-market.yml        (state-agnostic)
        // 3. products-state.yml         (market-agnostic)
        // 4. products.yml               (generic)
        
        return specific;
    }
    
    /**
     * Strategy 4: Rule-based selection
     * Use business rules for complex selection logic.
     */
    public String selectConfigByRules(EnrollmentSubmission enrollment) {
        // Medicare members always use medicare-specific templates
        if ("medicare".equalsIgnoreCase(enrollment.getMarketCategory())) {
            return selectMedicareConfig(enrollment);
        }
        
        // Large groups may have custom templates
        if ("large-group".equalsIgnoreCase(enrollment.getMarketCategory()) 
            && enrollment.getGroupSize() > 1000) {
            return "large-group-enterprise-" + enrollment.getState().toLowerCase() + ".yml";
        }
        
        // Default to convention
        return selectConfigByConvention(enrollment);
    }
    
    private String selectMedicareConfig(EnrollmentSubmission enrollment) {
        // Medicare-specific logic
        boolean isMedicareAdvantage = enrollment.getProducts().contains("medical");
        boolean hasPrescriptions = enrollment.getProducts().contains("prescription");
        
        if (isMedicareAdvantage && hasPrescriptions) {
            return "medicare-advantage-partd-" + enrollment.getState().toLowerCase() + ".yml";
        } else if (isMedicareAdvantage) {
            return "medicare-advantage-" + enrollment.getState().toLowerCase() + ".yml";
        } else {
            return "medicare-supplement-" + enrollment.getState().toLowerCase() + ".yml";
        }
    }
}
