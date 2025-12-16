package com.example.service.enrichers;

import com.example.service.PayloadEnricher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Example enricher that adds formatted dates and age calculations.
 * 
 * Use case: You have date formatting logic in PDFBox code that you want
 * to reuse with FreeMarker templates.
 */
@Component
public class DateFormattingEnricher implements PayloadEnricher {
    
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    @Override
    public Map<String, Object> enrich(Map<String, Object> payload) {
        Map<String, Object> enriched = new HashMap<>(payload);
        
        // Add formatted dates
        Map<String, Object> formattedDates = new HashMap<>();
        
        // Format common date fields
        if (payload.containsKey("submittedDate")) {
            String date = payload.get("submittedDate").toString();
            formattedDates.put("submittedDateLong", formatDateLong(date));
            formattedDates.put("submittedDateShort", formatDateShort(date));
        }
        
        if (payload.containsKey("effectiveDate")) {
            String date = payload.get("effectiveDate").toString();
            formattedDates.put("effectiveDateLong", formatDateLong(date));
            formattedDates.put("effectiveDateShort", formatDateShort(date));
        }
        
        enriched.put("formattedDates", formattedDates);
        
        // Calculate ages for applicants
        calculateAges(enriched);
        
        return enriched;
    }
    
    @Override
    public String getName() {
        return "dateFormatting";
    }
    
    /**
     * Format date in long format: "December 15, 2025"
     */
    private String formatDateLong(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate, INPUT_FORMAT);
            return date.format(DISPLAY_FORMAT);
        } catch (Exception e) {
            return isoDate;
        }
    }
    
    /**
     * Format date in short format: "12/15/2025"
     */
    private String formatDateShort(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate, INPUT_FORMAT);
            return date.format(SHORT_FORMAT);
        } catch (Exception e) {
            return isoDate;
        }
    }
    
    /**
     * Calculate ages for primary, spouse, and dependents
     */
    @SuppressWarnings("unchecked")
    private void calculateAges(Map<String, Object> payload) {
        LocalDate today = LocalDate.now();
        
        // Calculate primary age
        if (payload.containsKey("primary") && payload.get("primary") instanceof Map) {
            Map<String, Object> primary = (Map<String, Object>) payload.get("primary");
            calculateAge(primary, today);
        }
        
        // Calculate spouse age
        if (payload.containsKey("spouse") && payload.get("spouse") instanceof Map) {
            Map<String, Object> spouse = (Map<String, Object>) payload.get("spouse");
            calculateAge(spouse, today);
        }
        
        // Calculate dependent ages (dependent1, dependent2, dependent3)
        for (int i = 1; i <= 10; i++) {
            String key = "dependent" + i;
            if (payload.containsKey(key) && payload.get(key) instanceof Map) {
                Map<String, Object> dependent = (Map<String, Object>) payload.get(key);
                calculateAge(dependent, today);
            }
        }
        
        // Calculate ages for allDependents array
        if (payload.containsKey("allDependents") && payload.get("allDependents") instanceof List) {
            List<Map<String, Object>> dependents = (List<Map<String, Object>>) payload.get("allDependents");
            for (Map<String, Object> dependent : dependents) {
                calculateAge(dependent, today);
            }
        }
    }
    
    /**
     * Calculate age from dateOfBirth and add to applicant map
     */
    private void calculateAge(Map<String, Object> applicant, LocalDate today) {
        if (applicant.containsKey("dateOfBirth")) {
            try {
                String dobStr = applicant.get("dateOfBirth").toString();
                LocalDate dob = LocalDate.parse(dobStr, INPUT_FORMAT);
                int age = Period.between(dob, today).getYears();
                
                // Add calculated age
                applicant.put("calculatedAge", age);
                
                // Add age category
                applicant.put("ageCategory", getAgeCategory(age));
                
            } catch (Exception e) {
                // If age already exists or date parsing fails, skip
            }
        }
    }
    
    /**
     * Get age category for business logic
     */
    private String getAgeCategory(int age) {
        if (age < 18) return "MINOR";
        if (age < 26) return "YOUNG_ADULT";
        if (age < 65) return "ADULT";
        return "SENIOR";
    }
}
