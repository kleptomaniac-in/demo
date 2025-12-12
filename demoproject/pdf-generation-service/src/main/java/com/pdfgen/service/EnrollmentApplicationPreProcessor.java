package com.pdfgen.service;

import com.pdfgen.model.Application;
import com.pdfgen.model.Applicant;
import com.pdfgen.model.Address;
import com.pdfgen.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-processes complex enrollment applications for PDF field mapping.
 * Transforms nested, array-based structures into flat, direct-access structures.
 * 
 * This simplifies AcroForm field mappings from:
 *   "Primary_FirstName": "application.applicants[relationship=PRIMARY].demographic.firstName"
 * To:
 *   "Primary_FirstName": "primary.demographic.firstName"
 */
@Service
public class EnrollmentApplicationPreProcessor {
    
    /**
     * Flattens complex application structure for simplified PDF field mapping.
     * 
     * @param application Complex application with nested arrays
     * @return Flattened map with direct access to role-based entities
     */
    public Map<String, Object> prepareForPdfMapping(Map<String, Object> applicationData) {
        Map<String, Object> flattened = new HashMap<>();
        
        // Handle if root is wrapped in "application" key or not
        Map<String, Object> app = applicationData.containsKey("application") 
            ? (Map<String, Object>) applicationData.get("application")
            : applicationData;
        
        // Application-level fields
        flattened.put("applicationId", app.get("applicationId"));
        flattened.put("submittedDate", app.get("submittedDate"));
        flattened.put("effectiveDate", app.get("effectiveDate"));
        
        // Process applicants array
        if (app.containsKey("applicants") && app.get("applicants") instanceof List) {
            List<Map<String, Object>> applicants = (List<Map<String, Object>>) app.get("applicants");
            
            // Separate by relationship
            applicants.stream()
                .filter(a -> "PRIMARY".equals(a.get("relationship")))
                .findFirst()
                .ifPresent(primary -> flattened.put("primary", primary));
            
            applicants.stream()
                .filter(a -> "SPOUSE".equals(a.get("relationship")))
                .findFirst()
                .ifPresent(spouse -> flattened.put("spouse", spouse));
            
            // Get all dependents
            List<Map<String, Object>> dependents = applicants.stream()
                .filter(a -> "DEPENDENT".equals(a.get("relationship")))
                .collect(Collectors.toList());
            
            // Map first 3 dependents to fixed slots
            if (dependents.size() > 0) {
                flattened.put("dependent1", dependents.get(0));
            }
            if (dependents.size() > 1) {
                flattened.put("dependent2", dependents.get(1));
            }
            if (dependents.size() > 2) {
                flattened.put("dependent3", dependents.get(2));
            }
            
            // Overflow dependents (4+) for addendum
            if (dependents.size() > 3) {
                flattened.put("additionalDependents", 
                    dependents.subList(3, dependents.size()));
            }
            
            // Counts
            flattened.put("hasSpouse", applicants.stream()
                .anyMatch(a -> "SPOUSE".equals(a.get("relationship"))));
            flattened.put("dependentCount", dependents.size());
            flattened.put("primaryDependentCount", Math.min(3, dependents.size()));
            flattened.put("additionalDependentCount", Math.max(0, dependents.size() - 3));
        }
        
        // Process addresses array
        if (app.containsKey("addresses") && app.get("addresses") instanceof List) {
            List<Map<String, Object>> addresses = (List<Map<String, Object>>) app.get("addresses");
            
            addresses.stream()
                .filter(a -> "BILLING".equals(a.get("type")))
                .findFirst()
                .ifPresent(billing -> flattened.put("billing", billing));
            
            addresses.stream()
                .filter(a -> "MAILING".equals(a.get("type")))
                .findFirst()
                .ifPresent(mailing -> flattened.put("mailing", mailing));
        }
        
        // Process proposed products array
        if (app.containsKey("proposedProducts") && app.get("proposedProducts") instanceof List) {
            List<Map<String, Object>> products = (List<Map<String, Object>>) app.get("proposedProducts");
            
            products.stream()
                .filter(p -> "MEDICAL".equals(p.get("productType")))
                .findFirst()
                .ifPresent(medical -> flattened.put("medical", medical));
            
            products.stream()
                .filter(p -> "DENTAL".equals(p.get("productType")))
                .findFirst()
                .ifPresent(dental -> flattened.put("dental", dental));
            
            products.stream()
                .filter(p -> "VISION".equals(p.get("productType")))
                .findFirst()
                .ifPresent(vision -> flattened.put("vision", vision));
            
            // Has product flags
            flattened.put("hasMedical", products.stream()
                .anyMatch(p -> "MEDICAL".equals(p.get("productType"))));
            flattened.put("hasDental", products.stream()
                .anyMatch(p -> "DENTAL".equals(p.get("productType"))));
            flattened.put("hasVision", products.stream()
                .anyMatch(p -> "VISION".equals(p.get("productType"))));
        }
        
        // Process current coverages array (if needed for AcroForm)
        if (app.containsKey("currentCoverages") && app.get("currentCoverages") instanceof List) {
            List<Map<String, Object>> coverages = (List<Map<String, Object>>) app.get("currentCoverages");
            Map<String, Map<String, Object>> coverageMap = new HashMap<>();
            
            // Group by applicantId + productType
            for (Map<String, Object> coverage : coverages) {
                String applicantId = (String) coverage.get("applicantId");
                String productType = (String) coverage.get("productType");
                String key = applicantId + "_" + productType;
                coverageMap.put(key, coverage);
            }
            
            flattened.put("currentCoverageMap", coverageMap);
            
            // Convenience accessors for primary applicant (assuming A001)
            coverageMap.entrySet().stream()
                .filter(e -> e.getKey().startsWith("A001_MEDICAL"))
                .findFirst()
                .ifPresent(e -> flattened.put("primaryPriorMedical", e.getValue()));
            
            coverageMap.entrySet().stream()
                .filter(e -> e.getKey().startsWith("A001_DENTAL"))
                .findFirst()
                .ifPresent(e -> flattened.put("primaryPriorDental", e.getValue()));
            
            coverageMap.entrySet().stream()
                .filter(e -> e.getKey().startsWith("A001_VISION"))
                .findFirst()
                .ifPresent(e -> flattened.put("primaryPriorVision", e.getValue()));
        }
        
        // Copy calculated values if present
        if (app.containsKey("calculatedValues")) {
            Map<String, Object> calculated = (Map<String, Object>) app.get("calculatedValues");
            flattened.putAll(calculated);
        }
        
        return flattened;
    }
    
    /**
     * Alternative: Keep original structure but add flattened accessors.
     * Use this if you need both filtered and unfiltered access.
     */
    public Map<String, Object> enrichWithFlattenedAccessors(Map<String, Object> applicationData) {
        Map<String, Object> enriched = new HashMap<>(applicationData);
        Map<String, Object> flattened = prepareForPdfMapping(applicationData);
        enriched.put("flat", flattened);
        return enriched;
    }
}
