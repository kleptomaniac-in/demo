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
     * Uses default configuration (3 dependents max, 1 coverage per applicant max).
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
        return generateEnrollmentPdf(templatePath, fieldMappings, payload, null);
    }
    
    /**
     * Generate complete enrollment PDF with configurable addendum generation.
     * 
     * @param templatePath Path to AcroForm template
     * @param fieldMappings Field mappings for the form
     * @param payload Enrollment data
     * @param addendumConfig Configuration for addendum generation (null = use defaults)
     * @return Complete PDF (main form + all addendums)
     * @throws IOException if PDF generation fails
     */
    public byte[] generateEnrollmentPdf(String templatePath, 
                                       Map<String, String> fieldMappings, 
                                       Map<String, Object> payload,
                                       AddendumConfig addendumConfig) throws IOException {
        // Apply defaults if config not provided
        DependentAddendumConfig depConfig = null;
        CoverageAddendumConfig covConfig = null;
        
        if (addendumConfig != null) {
            depConfig = addendumConfig.getDependents();
            covConfig = addendumConfig.getCoverages();
        }
        
        // Use defaults if specific configs not provided
        boolean dependentsEnabled = (depConfig == null) || depConfig.isEnabled();
        int maxDependents = (depConfig == null) ? 3 : depConfig.getMaxInMainForm();
        
        boolean coveragesEnabled = (covConfig == null) || covConfig.isEnabled();
        int maxCoverages = (covConfig == null) ? 1 : covConfig.getMaxPerApplicant();
        
        // Step 1: Fill main AcroForm template
        byte[] mainForm = acroFormService.fillAcroForm(templatePath, fieldMappings, payload);
        
        // Step 2: Collect all addendums needed
        List<byte[]> pdfDocuments = new ArrayList<>();
        pdfDocuments.add(mainForm);
        
        List<Map<String, Object>> applicants = (List<Map<String, Object>>) payload.get("applicants");
        Map<String, Object> enrollmentData = (Map<String, Object>) payload.getOrDefault("enrollment", Map.of());
        
        // Step 3: Generate dependent addendum if enabled and needed
        if (dependentsEnabled && applicants != null && dependentAddendumService.isAddendumNeeded(applicants, maxDependents)) {
            System.out.println("EnrollmentPdfService: Dependent addendum IS needed - generating...");
            byte[] dependentAddendum = dependentAddendumService.generateDependentAddendum(applicants, enrollmentData, maxDependents);
            if (dependentAddendum.length > 0) {
                System.out.println("EnrollmentPdfService: Added dependent addendum (" + dependentAddendum.length + " bytes)");
                pdfDocuments.add(dependentAddendum);
            }
        } else if (applicants != null) {
            System.out.println("EnrollmentPdfService: Dependent addendum NOT needed (enabled=" + dependentsEnabled + ")");
        }
        
        // Step 4: Generate coverage addendum if enabled and needed
        if (coveragesEnabled && applicants != null && coverageAddendumService.isAddendumNeeded(applicants, maxCoverages)) {
            System.out.println("EnrollmentPdfService: Coverage addendum IS needed - generating...");
            byte[] coverageAddendum = coverageAddendumService.generateCoverageAddendum(applicants, enrollmentData, maxCoverages);
            if (coverageAddendum.length > 0) {
                System.out.println("EnrollmentPdfService: Added coverage addendum (" + coverageAddendum.length + " bytes)");
                pdfDocuments.add(coverageAddendum);
            }
        } else {
            System.out.println("EnrollmentPdfService: Coverage addendum NOT needed (enabled=" + coveragesEnabled + ")");
        }
        
        // Step 5: Merge all documents
        if (pdfDocuments.size() == 1) {
            return mainForm; // No addendums needed
        }
        
        return pdfMergerService.mergePdfs(pdfDocuments);
    }
}
