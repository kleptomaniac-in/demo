package com.example.service;

import java.util.List;
import java.util.Map;

/**
 * Model class representing enrollment submission data
 */
public class EnrollmentSubmission {
    private List<String> products;          // ["medical", "dental"]
    private String marketCategory;          // "individual", "small-group", etc.
    private String state;                   // "CA", "TX", "NY"
    private Integer groupSize;              // For group plans
    private Map<String, List<String>> plansByProduct;  // Product -> List of plan IDs
    
    // Getters and setters
    public List<String> getProducts() { return products; }
    public void setProducts(List<String> products) { this.products = products; }
    
    public String getMarketCategory() { return marketCategory; }
    public void setMarketCategory(String marketCategory) { this.marketCategory = marketCategory; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }
    
    public Map<String, List<String>> getPlansByProduct() { return plansByProduct; }
    public void setPlansByProduct(Map<String, List<String>> plansByProduct) { 
        this.plansByProduct = plansByProduct; 
    }
}
