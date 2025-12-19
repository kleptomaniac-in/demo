package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestration service for generating enrollment PDFs with multiple addendum support.
 * 
 * Workflow:
 * 1. Fill main AcroForm template
 * 2. Check for overflow dependents (4+ dependents) → Generate dependent addendum
 * 3. Check for overflow coverages (2+ coverages per applicant) → Generate coverage addendum
 * 4. Merge main form + all addendums into final PDF
 */
@Service
public class EnrollmentPdfService {
    
    @Autowired
    private AcroFormFillService acroFormService;
    
    @Autowired
    private DependentAddendumService dependentAddendumService;
    
    @Autowired
    private CoverageAddendumService coverageAddendumService;
    
    @Autowired
    private PdfMergerService pdfMergerService;
    
    /**
     * Generate complete enrollment PDF with automatic addendums for overflow data.
     * 
     * Automatically generates addendums for:
     * - Overflow dependents (4+ dependents)
     * - Overflow coverages (2+ coverages per applicant)
     * 
     * @param templatePath Path to AcroForm template
     * @param fieldMappings Field mappings for the form
     * @param payload Enrollment data including all applicants, dependents, and coverages
     * @return Complete PDF (main form + all addendums)
     * @throws IOException if PDF generation fails
     */
    public byte[] generateEnrollmentPdf(String templatePath, 
                                       Map<String, String> fieldMappings, 
                                       Map<String, Object> payload) throws IOException {
        // Step 1: Fill main AcroForm template
        byte[] mainForm = acroFormService.fillAcroForm(templatePath, fieldMappings, payload);
        
        // Step 2: Collect all addendums needed
        List<byte[]> pdfDocuments = new ArrayList<>();
        pdfDocuments.add(mainForm);
        
        List<Map<String, Object>> applicants = (List<Map<String, Object>>) payload.get("applicants");
        Map<String, Object> enrollmentData = (Map<String, Object>) payload.getOrDefault("enrollment", Map.of());
        
        // Step 3: Generate dependent addendum if needed
        if (applicants != null && dependentAddendumService.isAddendumNeeded(applicants)) {
            byte[] dependentAddendum = dependentAddendumService.generateDependentAddendum(applicants, enrollmentData);
            if (dependentAddendum.length > 0) {
                pdfDocuments.add(dependentAddendum);
            }
        }
        
        // Step 4: Generate coverage addendum if needed
        if (applicants != null && coverageAddendumService.isAddendumNeeded(applicants)) {
            byte[] coverageAddendum = coverageAddendumService.generateCoverageAddendum(applicants, enrollmentData);
            if (coverageAddendum.length > 0) {
                pdfDocuments.add(coverageAddendum);
            }
        }
        
        // Step 5: Merge all documents
        if (pdfDocuments.size() == 1) {
            return mainForm; // No addendums needed
        }
        
        return pdfMergerService.mergePdfs(pdfDocuments);
    }
    
    /**
     * Generate enrollment PDF with explicit control over addendum generation.
     * 
     * @param templatePath Path to AcroForm template
     * @param fieldMappings Field mappings for the form
     * @param payload Enrollment data
     * @param generateAddendums Control which addendums to generate
     * @return Complete PDF
     * @throws IOException if PDF generation fails
     */
    public byte[] generateEnrollmentPdf(String templatePath,
                                       Map<String, String> fieldMappings,
                                       Map<String, Object> payload,
                                       AddendumOptions generateAddendums) throws IOException {
        if (generateAddendums == AddendumOptions.NONE) {
            // Just fill the form without any addendums
            return acroFormService.fillAcroForm(templatePath, fieldMappings, payload);
        }
        
        // Fill main form
        byte[] mainForm = acroFormService.fillAcroForm(templatePath, fieldMappings, payload);
        List<byte[]> pdfDocuments = new ArrayList<>();
        pdfDocuments.add(mainForm);
        
        List<Map<String, Object>> applicants = (List<Map<String, Object>>) payload.get("applicants");
        Map<String, Object> enrollmentData = (Map<String, Object>) payload.getOrDefault("enrollment", Map.of());
        
        // Generate dependent addendum if requested
        if ((generateAddendums == AddendumOptions.DEPENDENTS_ONLY || generateAddendums == AddendumOptions.ALL) 
            && applicants != null && dependentAddendumService.isAddendumNeeded(applicants)) {
            byte[] dependentAddendum = dependentAddendumService.generateDependentAddendum(applicants, enrollmentData);
            if (dependentAddendum.length > 0) {
                pdfDocuments.add(dependentAddendum);
            }
        }
        
        // Generate coverage addendum if requested
        if ((generateAddendums == AddendumOptions.COVERAGES_ONLY || generateAddendums == AddendumOptions.ALL)
            && applicants != null && coverageAddendumService.isAddendumNeeded(applicants)) {
            byte[] coverageAddendum = coverageAddendumService.generateCoverageAddendum(applicants, enrollmentData);
            if (coverageAddendum.length > 0) {
                pdfDocuments.add(coverageAddendum);
            }
        }
        
        // Merge if we have multiple documents
        if (pdfDocuments.size() == 1) {
            return mainForm;
        }
        
        return pdfMergerService.mergePdfs(pdfDocuments);
    }
    
    /**
     * Options for controlling addendum generation
     */
    public enum AddendumOptions {
        NONE,              // No addendums
        DEPENDENTS_ONLY,   // Only dependent addendum
        COVERAGES_ONLY,    // Only coverage addendum
        ALL                // All addendums (default)
    }
}
