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
        
        // Get effective date from enrollmentContext or root
        Object effectiveDate = null;
        Map<String, Object> enrollmentContext = (Map<String, Object>) payload.get("enrollmentContext");
        if (enrollmentContext != null) {
            effectiveDate = enrollmentContext.get("effectiveDate");
        }
        if (effectiveDate == null) {
            effectiveDate = payload.get("effectiveDate");
        }
        coverageSummary.put("effectiveDate", effectiveDate);
        
        // Process members (which may have products with premiums)
        List<Map<String, Object>> members = (List<Map<String, Object>>) payload.get("members");
        List<Map<String, Object>> applicants = (List<Map<String, Object>>) payload.get("applicants");
        
        // Calculate total premium from members
        double totalPremium = 0.0;
        if (members != null) {
            for (Map<String, Object> member : members) {
                List<Map<String, Object>> products = (List<Map<String, Object>>) member.get("products");
                if (products != null) {
                    for (Map<String, Object> product : products) {
                        Object premiumObj = product.get("premium");
                        if (premiumObj != null) {
                            try {
                                if (premiumObj instanceof Number) {
                                    totalPremium += ((Number) premiumObj).doubleValue();
                                } else {
                                    totalPremium += Double.parseDouble(premiumObj.toString());
                                }
                            } catch (NumberFormatException e) {
                                // Skip invalid premium
                            }
                        }
                    }
                }
            }
        }
        coverageSummary.put("totalPremium", String.format("%.2f", totalPremium));
        
        // Process applicants/members and calculate ages
        List<Map<String, Object>> enrichedApplicants = new ArrayList<>();
        
        // Try members first (if they exist and have more detail)
        if (members != null && !members.isEmpty()) {
            for (Map<String, Object> member : members) {
                Map<String, Object> enrichedMember = new HashMap<>(member);
                
                // Extract name (flat structure)
                String name = (String) member.get("name");
                String relationship = (String) member.get("relationship");
                String dob = (String) member.get("dateOfBirth");
                
                if (name != null) {
                    enrichedMember.put("displayName", name);
                }
                if (relationship != null) {
                    enrichedMember.put("displayRelationship", relationship);
                }
                
                // Calculate age from DOB
                if (dob != null) {
                    int age = calculateAge(dob);
                    enrichedMember.put("calculatedAge", age);
                }
                
                // Keep products array as-is for rendering
                enrichedApplicants.add(enrichedMember);
            }
        } 
        // Fallback to applicants array
        else if (applicants != null && !applicants.isEmpty()) {
            for (Map<String, Object> applicant : applicants) {
                Map<String, Object> enrichedApplicant = new HashMap<>(applicant);
                
                // Handle both nested demographic and flat structure
                Map<String, Object> demographic = (Map<String, Object>) applicant.get("demographic");
                String firstName, lastName, dob;
                
                if (demographic != null) {
                    // Nested structure
                    firstName = (String) demographic.get("firstName");
                    lastName = (String) demographic.get("lastName");
                    dob = (String) demographic.get("dateOfBirth");
                } else {
                    // Flat structure
                    firstName = (String) applicant.get("firstName");
                    lastName = (String) applicant.get("lastName");
                    dob = (String) applicant.get("dateOfBirth");
                }
                
                if (firstName != null && lastName != null) {
                    enrichedApplicant.put("displayName", firstName + " " + lastName);
                }
                
                String relationship = (String) applicant.get("relationship");
                enrichedApplicant.put("displayRelationship", relationship != null ? relationship : "Primary");
                
                // Calculate age
                if (dob != null) {
                    int age = calculateAge(dob);
                    enrichedApplicant.put("calculatedAge", age);
                }
                
                enrichedApplicants.add(enrichedApplicant);
            }
        }
        
        coverageSummary.put("enrichedApplicants", enrichedApplicants);
        coverageSummary.put("applicantCount", enrichedApplicants.size());
        
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
        String effectiveDateStr = (String) coverageSummary.get("effectiveDate");
        if (effectiveDateStr != null) {
            coverageSummary.put("formattedEffectiveDate", formatDate(effectiveDateStr));
            coverageSummary.put("daysUntilEffective", calculateDaysUntilEffective(effectiveDateStr));
        }
        
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
