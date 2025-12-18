package com.example.service.enrichers;

import com.example.service.PayloadEnricher;
import org.springframework.stereotype.Component;

import java.util.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Component
public class CoverageSummaryEnricher implements PayloadEnricher {

    @Override
    public String getName() {
        return "coverageSummary";
    }

    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Create coverage summary data structure
        Map<String, Object> coverageSummary = new HashMap<>();
        
        // Extract and format application info
        coverageSummary.put("applicationNumber", payload.get("applicationNumber"));
        coverageSummary.put("effectiveDate", payload.get("effectiveDate"));
        coverageSummary.put("totalPremium", payload.get("totalPremium"));
        
        // Process applicants and calculate ages
        List<Map<String, Object>> applicants = (List<Map<String, Object>>) payload.get("applicants");
        if (applicants != null && !applicants.isEmpty()) {
            List<Map<String, Object>> enrichedApplicants = new ArrayList<>();
            
            for (Map<String, Object> applicant : applicants) {
                Map<String, Object> enrichedApplicant = new HashMap<>(applicant);
                
                // Calculate age from DOB
                Map<String, Object> demographic = (Map<String, Object>) applicant.get("demographic");
                if (demographic != null) {
                    String dob = (String) demographic.get("dateOfBirth");
                    if (dob != null) {
                        int age = calculateAge(dob);
                        enrichedApplicant.put("calculatedAge", age);
                        
                        // Create display name
                        String firstName = (String) demographic.get("firstName");
                        String lastName = (String) demographic.get("lastName");
                        String relationship = (String) applicant.get("relationship");
                        enrichedApplicant.put("displayName", firstName + " " + lastName);
                        enrichedApplicant.put("displayRelationship", relationship != null ? relationship : "Primary");
                    }
                }
                
                // Calculate total premium for this applicant
                List<Map<String, Object>> products = (List<Map<String, Object>>) applicant.get("products");
                if (products != null && !products.isEmpty()) {
                    double applicantTotalPremium = 0.0;
                    for (Map<String, Object> product : products) {
                        String premiumStr = (String) product.get("premium");
                        if (premiumStr != null) {
                            try {
                                applicantTotalPremium += Double.parseDouble(premiumStr);
                            } catch (NumberFormatException e) {
                                // Skip invalid premium
                            }
                        }
                    }
                    enrichedApplicant.put("totalApplicantPremium", String.format("%.2f", applicantTotalPremium));
                }
                
                enrichedApplicants.add(enrichedApplicant);
            }
            
            coverageSummary.put("enrichedApplicants", enrichedApplicants);
            coverageSummary.put("applicantCount", enrichedApplicants.size());
        }
        
        // Process coverages and create summary
        List<Map<String, Object>> coverages = (List<Map<String, Object>>) payload.get("coverages");
        if (coverages != null && !coverages.isEmpty()) {
            List<Map<String, Object>> enrichedCoverages = new ArrayList<>();
            Set<String> carriers = new HashSet<>();
            int totalBenefits = 0;
            
            for (Map<String, Object> coverage : coverages) {
                Map<String, Object> enrichedCoverage = new HashMap<>(coverage);
                
                String carrierName = (String) coverage.get("carrierName");
                if (carrierName != null) {
                    carriers.add(carrierName);
                }
                
                List<String> benefits = (List<String>) coverage.get("benefits");
                if (benefits != null) {
                    totalBenefits += benefits.size();
                    enrichedCoverage.put("benefitCount", benefits.size());
                }
                
                enrichedCoverages.add(enrichedCoverage);
            }
            
            coverageSummary.put("enrichedCoverages", enrichedCoverages);
            coverageSummary.put("totalCarriers", carriers.size());
            coverageSummary.put("carrierNames", new ArrayList<>(carriers));
            coverageSummary.put("totalBenefits", totalBenefits);
        }
        
        // Add formatted dates
        String effectiveDate = (String) payload.get("effectiveDate");
        if (effectiveDate != null) {
            coverageSummary.put("formattedEffectiveDate", formatDate(effectiveDate));
        }
        
        // Calculate coverage start date info
        coverageSummary.put("daysUntilEffective", calculateDaysUntilEffective(effectiveDate));
        
        // Add enriched summary to payload
        enriched.put("coverageSummary", coverageSummary);
        
        return enriched;
    }
    
    private int calculateAge(String dateOfBirth) {
        try {
            LocalDate dob = LocalDate.parse(dateOfBirth);
            LocalDate now = LocalDate.now();
            return Period.between(dob, now).getYears();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String formatDate(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            return localDate.format(formatter);
        } catch (Exception e) {
            return date;
        }
    }
    
    private long calculateDaysUntilEffective(String effectiveDate) {
        try {
            LocalDate effective = LocalDate.parse(effectiveDate);
            LocalDate now = LocalDate.now();
            return Period.between(now, effective).getDays();
        } catch (Exception e) {
            return 0;
        }
    }
}
