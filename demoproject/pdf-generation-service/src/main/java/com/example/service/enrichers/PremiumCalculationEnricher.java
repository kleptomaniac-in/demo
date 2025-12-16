package com.example.service.enrichers;

import com.example.service.PayloadEnricher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Example enricher that extracts premium calculation logic.
 * 
 * Use case: You have complex premium calculations in existing PDFBox code
 * and want to reuse that logic with FreeMarker templates.
 */
@Component
public class PremiumCalculationEnricher implements PayloadEnricher {
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Extract products from nested structure
        List<Map<String, Object>> products = extractProducts(payload);
        
        if (products.isEmpty()) {
            return enriched;
        }
        
        // Calculate totals
        BigDecimal monthlyTotal = BigDecimal.ZERO;
        BigDecimal annualTotal = BigDecimal.ZERO;
        
        for (Map<String, Object> product : products) {
            Object premiumObj = product.get("monthlyPremium");
            if (premiumObj instanceof Number) {
                BigDecimal premium = new BigDecimal(premiumObj.toString());
                monthlyTotal = monthlyTotal.add(premium);
                annualTotal = annualTotal.add(premium.multiply(new BigDecimal("12")));
            }
        }
        
        // Calculate discounts (example business logic)
        BigDecimal discount = calculateDiscount(products, monthlyTotal);
        BigDecimal finalMonthly = monthlyTotal.subtract(discount);
        BigDecimal finalAnnual = annualTotal.subtract(discount.multiply(new BigDecimal("12")));
        
        // Add calculated fields to payload
        Map<String, Object> premiumCalculations = new HashMap<>();
        premiumCalculations.put("monthlyTotal", monthlyTotal.setScale(2, RoundingMode.HALF_UP).doubleValue());
        premiumCalculations.put("annualTotal", annualTotal.setScale(2, RoundingMode.HALF_UP).doubleValue());
        premiumCalculations.put("discount", discount.setScale(2, RoundingMode.HALF_UP).doubleValue());
        premiumCalculations.put("finalMonthly", finalMonthly.setScale(2, RoundingMode.HALF_UP).doubleValue());
        premiumCalculations.put("finalAnnual", finalAnnual.setScale(2, RoundingMode.HALF_UP).doubleValue());
        premiumCalculations.put("savingsPercent", calculateSavingsPercent(monthlyTotal, discount));
        
        enriched.put("premiumCalculations", premiumCalculations);
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "premiumCalculation";
    }
    
    /**
     * Extract products from nested payload structure
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractProducts(Map<String, Object> payload) {
        // Try different payload structures
        if (payload.containsKey("proposedProducts")) {
            Object products = payload.get("proposedProducts");
            if (products instanceof List) {
                return (List<Map<String, Object>>) products;
            }
        }
        
        // Try medical/dental/vision flat structure
        List<Map<String, Object>> products = new ArrayList<>();
        if (payload.containsKey("medical") && payload.get("medical") instanceof Map) {
            products.add((Map<String, Object>) payload.get("medical"));
        }
        if (payload.containsKey("dental") && payload.get("dental") instanceof Map) {
            products.add((Map<String, Object>) payload.get("dental"));
        }
        if (payload.containsKey("vision") && payload.get("vision") instanceof Map) {
            products.add((Map<String, Object>) payload.get("vision"));
        }
        
        return products;
    }
    
    /**
     * Calculate discount based on business rules
     */
    private BigDecimal calculateDiscount(List<Map<String, Object>> products, BigDecimal total) {
        // Example: 10% discount if purchasing 3+ products
        if (products.size() >= 3) {
            return total.multiply(new BigDecimal("0.10"));
        }
        // Example: 5% discount if total > $500
        else if (total.compareTo(new BigDecimal("500")) > 0) {
            return total.multiply(new BigDecimal("0.05"));
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate savings percentage
     */
    private double calculateSavingsPercent(BigDecimal total, BigDecimal discount) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        BigDecimal percent = discount.divide(total, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        
        return percent.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
