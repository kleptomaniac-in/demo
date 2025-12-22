package com.example.service.enrichers;

import com.example.service.PayloadEnricher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Enriches enrollment payload with product, market, and state-specific context.
 * 
 * Extracts:
 * - Product selections (medical, dental, vision)
 * - Market category context (individual, small-group, large-group)
 * - State-specific requirements (CA mandates, NY regulations, etc.)
 * - Multi-product summaries and pricing
 */
@Component
public class EnrollmentContextEnricher implements PayloadEnricher {

    @Override
    public String getName() {
        return "enrollmentContext";
    }

    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Check if enrollmentContext already exists in payload (pre-populated)
        Map<String, Object> existingContext = (Map<String, Object>) payload.get("enrollmentContext");
        if (existingContext != null) {
            // Context already exists - keep it as-is and also spread fields to root for convenience
            enriched.put("enrollmentContext", existingContext);
            // Also add common fields to root level for backward compatibility
            if (existingContext.get("state") != null) {
                enriched.put("state", existingContext.get("state"));
            }
            if (existingContext.get("marketCategory") != null) {
                enriched.put("marketCategory", existingContext.get("marketCategory"));
            }
            return enriched;
        }
        
        // Extract enrollment metadata if present
        Map<String, Object> enrollment = (Map<String, Object>) payload.get("enrollment");
        if (enrollment != null) {
            enriched.putAll(enrichEnrollmentContext(enrollment, payload));
        } else {
            // Fallback: Look for products, market, state at root level
            enriched.putAll(enrichFromRootLevel(payload));
        }
        
        return enriched;
    }
    
    private Map<String, Object> enrichEnrollmentContext(Map<String, Object> enrollment, Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>();
        Map<String, Object> enrollmentContext = new HashMap<>();
        
        // Product selections
        List<String> products = (List<String>) enrollment.get("products");
        if (products != null) {
            enrollmentContext.put("selectedProducts", products);
            enrollmentContext.put("hasMultipleProducts", products.size() > 1);
            enrollmentContext.put("productCount", products.size());
            
            // Product type flags for easy conditional rendering
            enrollmentContext.put("hasMedical", products.contains("medical"));
            enrollmentContext.put("hasDental", products.contains("dental"));
            enrollmentContext.put("hasVision", products.contains("vision"));
            enrollmentContext.put("hasLife", products.contains("life"));
            
            // Create display string
            enrollmentContext.put("productsDisplay", String.join(", ", products));
        }
        
        // Market category
        String marketCategory = (String) enrollment.get("marketCategory");
        if (marketCategory != null) {
            enrollmentContext.put("marketCategory", marketCategory);
            enrollmentContext.put("marketDisplay", formatMarketCategory(marketCategory));
            
            // Market type flags
            enrollmentContext.put("isIndividual", "individual".equalsIgnoreCase(marketCategory));
            enrollmentContext.put("isSmallGroup", "small-group".equalsIgnoreCase(marketCategory) || 
                                                   "small_group".equalsIgnoreCase(marketCategory));
            enrollmentContext.put("isLargeGroup", "large-group".equalsIgnoreCase(marketCategory) || 
                                                   "large_group".equalsIgnoreCase(marketCategory));
        }
        
        // State
        String state = (String) enrollment.get("state");
        if (state != null) {
            enrollmentContext.put("state", state);
            enrollmentContext.put("stateFullName", getStateName(state));
            
            // State-specific flags for compliance requirements
            enrollmentContext.put("requiresCADisclosures", "CA".equalsIgnoreCase(state));
            enrollmentContext.put("requiresNYRegulations", "NY".equalsIgnoreCase(state));
            enrollmentContext.put("requiresTXNotices", "TX".equalsIgnoreCase(state));
        }
        
        // Plans by product
        Map<String, List<String>> plansByProduct = (Map<String, List<String>>) enrollment.get("plansByProduct");
        if (plansByProduct != null) {
            enrollmentContext.put("plansByProduct", plansByProduct);
            
            // Calculate total plans across all products
            int totalPlans = plansByProduct.values().stream()
                .mapToInt(List::size)
                .sum();
            enrollmentContext.put("totalPlansSelected", totalPlans);
        }
        
        // Aggregate product pricing from members
        enriched.put("productSummary", aggregateProductPricing(payload));
        
        enriched.put("enrollmentContext", enrollmentContext);
        return enriched;
    }
    
    private Map<String, Object> enrichFromRootLevel(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>();
        Map<String, Object> enrollmentContext = new HashMap<>();
        
        // Try to extract products from members data
        List<Map<String, Object>> members = (List<Map<String, Object>>) payload.get("members");
        if (members != null && !members.isEmpty()) {
            Set<String> productsFound = new HashSet<>();
            for (Map<String, Object> member : members) {
                if (member.containsKey("medical")) productsFound.add("medical");
                if (member.containsKey("dental")) productsFound.add("dental");
                if (member.containsKey("vision")) productsFound.add("vision");
                if (member.containsKey("life")) productsFound.add("life");
            }
            enrollmentContext.put("selectedProducts", new ArrayList<>(productsFound));
            enrollmentContext.put("productCount", productsFound.size());
        }
        
        enriched.put("enrollmentContext", enrollmentContext);
        return enriched;
    }
    
    private Map<String, Object> aggregateProductPricing(Map<String, Object> payload) {
        Map<String, Object> productSummary = new HashMap<>();
        
        List<Map<String, Object>> members = (List<Map<String, Object>>) payload.get("members");
        if (members == null || members.isEmpty()) {
            return productSummary;
        }
        
        double totalMedicalPremium = 0.0;
        double totalDentalPremium = 0.0;
        double totalVisionPremium = 0.0;
        int medicalCount = 0;
        int dentalCount = 0;
        int visionCount = 0;
        
        for (Map<String, Object> member : members) {
            // Medical
            Map<String, Object> medical = (Map<String, Object>) member.get("medical");
            if (medical != null) {
                Object premium = medical.get("premium");
                if (premium != null) {
                    totalMedicalPremium += ((Number) premium).doubleValue();
                    medicalCount++;
                }
            }
            
            // Dental
            Map<String, Object> dental = (Map<String, Object>) member.get("dental");
            if (dental != null) {
                Object premium = dental.get("premium");
                if (premium != null) {
                    totalDentalPremium += ((Number) premium).doubleValue();
                    dentalCount++;
                }
            }
            
            // Vision
            Map<String, Object> vision = (Map<String, Object>) member.get("vision");
            if (vision != null) {
                Object premium = vision.get("premium");
                if (premium != null) {
                    totalVisionPremium += ((Number) premium).doubleValue();
                    visionCount++;
                }
            }
        }
        
        if (medicalCount > 0) {
            productSummary.put("medicalPremiumTotal", String.format("%.2f", totalMedicalPremium));
            productSummary.put("medicalMemberCount", medicalCount);
        }
        
        if (dentalCount > 0) {
            productSummary.put("dentalPremiumTotal", String.format("%.2f", totalDentalPremium));
            productSummary.put("dentalMemberCount", dentalCount);
        }
        
        if (visionCount > 0) {
            productSummary.put("visionPremiumTotal", String.format("%.2f", totalVisionPremium));
            productSummary.put("visionMemberCount", visionCount);
        }
        
        double grandTotal = totalMedicalPremium + totalDentalPremium + totalVisionPremium;
        productSummary.put("grandTotalPremium", String.format("%.2f", grandTotal));
        
        return productSummary;
    }
    
    private String formatMarketCategory(String marketCategory) {
        if (marketCategory == null) return "";
        
        switch (marketCategory.toLowerCase().replace("-", "_")) {
            case "individual": return "Individual & Family";
            case "small_group":
            case "small group": return "Small Group (2-50)";
            case "large_group":
            case "large group": return "Large Group (51+)";
            default: return marketCategory;
        }
    }
    
    private String getStateName(String stateCode) {
        if (stateCode == null) return "";
        
        Map<String, String> stateNames = Map.ofEntries(
            Map.entry("CA", "California"),
            Map.entry("NY", "New York"),
            Map.entry("TX", "Texas"),
            Map.entry("FL", "Florida"),
            Map.entry("IL", "Illinois"),
            Map.entry("PA", "Pennsylvania"),
            Map.entry("OH", "Ohio"),
            Map.entry("GA", "Georgia"),
            Map.entry("NC", "North Carolina"),
            Map.entry("MI", "Michigan")
        );
        
        return stateNames.getOrDefault(stateCode.toUpperCase(), stateCode);
    }
}
