package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for EnrollmentPdfService with configurable addendum generation.
 * Validates that AddendumConfig settings are properly applied.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentPdfServiceConfigTest {
    
    @Mock
    private AcroFormFillService acroFormService;
    
    @Mock
    private DependentAddendumService dependentAddendumService;
    
    @Mock
    private CoverageAddendumService coverageAddendumService;
    
    @Mock
    private PdfMergerService pdfMergerService;
    
    @InjectMocks
    private EnrollmentPdfService service;
    
    @BeforeEach
    void setUp() throws Exception {
        // Use lenient() for stubs that may not be called in all tests
        lenient().when(acroFormService.fillAcroForm(anyString(), anyMap(), anyMap()))
            .thenReturn("main-form-pdf".getBytes());
        lenient().when(dependentAddendumService.generateDependentAddendum(anyList(), anyMap(), anyInt()))
            .thenReturn("dependent-addendum-pdf".getBytes());
        lenient().when(coverageAddendumService.generateCoverageAddendum(anyList(), anyMap(), anyInt()))
            .thenReturn("coverage-addendum-pdf".getBytes());
        lenient().when(pdfMergerService.mergePdfs(anyList()))
            .thenReturn("merged-pdf".getBytes());
    }
    
    @Test
    @DisplayName("Null config uses default values (max=3 dependents, max=1 coverage)")
    void testNullConfigUsesDefaults() throws Exception {
        Map<String, Object> payload = createPayloadWithOverflow(6, 2);
        
        when(dependentAddendumService.isAddendumNeeded(anyList(), eq(3))).thenReturn(true);
        when(coverageAddendumService.isAddendumNeeded(anyList(), eq(1))).thenReturn(true);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, null);
        
        // Verify default max values used
        verify(dependentAddendumService).isAddendumNeeded(anyList(), eq(3));
        verify(coverageAddendumService).isAddendumNeeded(anyList(), eq(1));
        
        System.out.println("✓ Null config uses defaults: maxDependents=3, maxCoverages=1");
    }
    
    @Test
    @DisplayName("Custom dependent config applied correctly")
    void testCustomDependentConfig() throws Exception {
        AddendumConfig config = new AddendumConfig();
        DependentAddendumConfig depConfig = new DependentAddendumConfig();
        depConfig.setEnabled(true);
        depConfig.setMaxInMainForm(5);
        config.setDependents(depConfig);
        
        Map<String, Object> payload = createPayloadWithOverflow(6, 1);
        
        when(dependentAddendumService.isAddendumNeeded(anyList(), eq(5))).thenReturn(true);
        when(coverageAddendumService.isAddendumNeeded(anyList(), eq(1))).thenReturn(false);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        // Verify custom max value used
        verify(dependentAddendumService).isAddendumNeeded(anyList(), eq(5));
        verify(dependentAddendumService).generateDependentAddendum(anyList(), anyMap(), eq(5));
        
        System.out.println("✓ Custom dependent config: maxInMainForm=5 applied correctly");
    }
    
    @Test
    @DisplayName("Custom coverage config applied correctly")
    void testCustomCoverageConfig() throws Exception {
        AddendumConfig config = new AddendumConfig();
        CoverageAddendumConfig covConfig = new CoverageAddendumConfig();
        covConfig.setEnabled(true);
        covConfig.setMaxPerApplicant(2);
        config.setCoverages(covConfig);
        
        Map<String, Object> payload = createPayloadWithOverflow(2, 3);
        
        when(dependentAddendumService.isAddendumNeeded(anyList(), eq(3))).thenReturn(false);
        when(coverageAddendumService.isAddendumNeeded(anyList(), eq(2))).thenReturn(true);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        // Verify custom max value used
        verify(coverageAddendumService).isAddendumNeeded(anyList(), eq(2));
        verify(coverageAddendumService).generateCoverageAddendum(anyList(), anyMap(), eq(2));
        
        System.out.println("✓ Custom coverage config: maxPerApplicant=2 applied correctly");
    }
    
    @Test
    @DisplayName("Disabled dependent addendum not generated")
    void testDependentAddendumDisabled() throws Exception {
        AddendumConfig config = new AddendumConfig();
        DependentAddendumConfig depConfig = new DependentAddendumConfig();
        depConfig.setEnabled(false);  // Disabled
        config.setDependents(depConfig);
        
        Map<String, Object> payload = createPayloadWithOverflow(10, 1);
        
        when(coverageAddendumService.isAddendumNeeded(anyList(), eq(1))).thenReturn(false);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        // Verify dependent addendum NOT checked or generated
        verify(dependentAddendumService, never()).isAddendumNeeded(anyList(), anyInt());
        verify(dependentAddendumService, never()).generateDependentAddendum(anyList(), anyMap(), anyInt());
        
        System.out.println("✓ Dependent addendum disabled: not generated even with 10 dependents");
    }
    
    @Test
    @DisplayName("Disabled coverage addendum not generated")
    void testCoverageAddendumDisabled() throws Exception {
        AddendumConfig config = new AddendumConfig();
        CoverageAddendumConfig covConfig = new CoverageAddendumConfig();
        covConfig.setEnabled(false);  // Disabled
        config.setCoverages(covConfig);
        
        Map<String, Object> payload = createPayloadWithOverflow(2, 5);
        
        when(dependentAddendumService.isAddendumNeeded(anyList(), eq(3))).thenReturn(false);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        // Verify coverage addendum NOT checked or generated
        verify(coverageAddendumService, never()).isAddendumNeeded(anyList(), anyInt());
        verify(coverageAddendumService, never()).generateCoverageAddendum(anyList(), anyMap(), anyInt());
        
        System.out.println("✓ Coverage addendum disabled: not generated even with 5 coverages per applicant");
    }
    
    @Test
    @DisplayName("Both addendums disabled")
    void testBothAddendumsDisabled() throws Exception {
        AddendumConfig config = new AddendumConfig();
        
        DependentAddendumConfig depConfig = new DependentAddendumConfig();
        depConfig.setEnabled(false);
        config.setDependents(depConfig);
        
        CoverageAddendumConfig covConfig = new CoverageAddendumConfig();
        covConfig.setEnabled(false);
        config.setCoverages(covConfig);
        
        Map<String, Object> payload = createPayloadWithOverflow(10, 5);
        
        byte[] result = service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        // Should return main form only
        assertArrayEquals("main-form-pdf".getBytes(), result);
        verify(pdfMergerService, never()).mergePdfs(anyList());
        
        System.out.println("✓ Both addendums disabled: returns main form only");
    }
    
    @Test
    @DisplayName("Partial config: only dependents specified")
    void testPartialConfigDependentsOnly() throws Exception {
        AddendumConfig config = new AddendumConfig();
        DependentAddendumConfig depConfig = new DependentAddendumConfig();
        depConfig.setMaxInMainForm(4);
        config.setDependents(depConfig);
        // coverages config is null, should use defaults
        
        Map<String, Object> payload = createPayloadWithOverflow(5, 2);
        
        when(dependentAddendumService.isAddendumNeeded(anyList(), eq(4))).thenReturn(true);
        when(coverageAddendumService.isAddendumNeeded(anyList(), eq(1))).thenReturn(true);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        // Verify custom dependent max, default coverage max
        verify(dependentAddendumService).isAddendumNeeded(anyList(), eq(4));
        verify(coverageAddendumService).isAddendumNeeded(anyList(), eq(1));
        
        System.out.println("✓ Partial config: dependents=4 (custom), coverages=1 (default)");
    }
    
    @Test
    @DisplayName("Both addendums with custom values")
    void testBothAddendumsCustom() throws Exception {
        AddendumConfig config = new AddendumConfig();
        
        DependentAddendumConfig depConfig = new DependentAddendumConfig();
        depConfig.setEnabled(true);
        depConfig.setMaxInMainForm(5);
        config.setDependents(depConfig);
        
        CoverageAddendumConfig covConfig = new CoverageAddendumConfig();
        covConfig.setEnabled(true);
        covConfig.setMaxPerApplicant(2);
        config.setCoverages(covConfig);
        
        Map<String, Object> payload = createPayloadWithOverflow(7, 3);
        
        when(dependentAddendumService.isAddendumNeeded(anyList(), eq(5))).thenReturn(true);
        when(coverageAddendumService.isAddendumNeeded(anyList(), eq(2))).thenReturn(true);
        
        service.generateEnrollmentPdf("template.pdf", Map.of(), payload, config);
        
        verify(dependentAddendumService).isAddendumNeeded(anyList(), eq(5));
        verify(dependentAddendumService).generateDependentAddendum(anyList(), anyMap(), eq(5));
        verify(coverageAddendumService).isAddendumNeeded(anyList(), eq(2));
        verify(coverageAddendumService).generateCoverageAddendum(anyList(), anyMap(), eq(2));
        
        System.out.println("✓ Both addendums with custom values: dependents=5, coverages=2");
    }
    
    // Helper methods
    
    private Map<String, Object> createPayloadWithOverflow(int dependentCount, int coveragesPerApplicant) {
        Map<String, Object> payload = new HashMap<>();
        
        List<Map<String, Object>> applicants = new ArrayList<>();
        
        // PRIMARY
        applicants.add(createApplicant("PRIMARY", coveragesPerApplicant));
        
        // DEPENDENTS
        for (int i = 0; i < dependentCount; i++) {
            applicants.add(createApplicant("DEPENDENT", 0));
        }
        
        payload.put("applicants", applicants);
        payload.put("enrollment", Map.of("groupNumber", "TEST-123"));
        
        return payload;
    }
    
    private Map<String, Object> createApplicant(String relationshipType, int coverageCount) {
        Map<String, Object> applicant = new HashMap<>();
        Map<String, Object> demographic = new HashMap<>();
        demographic.put("relationshipType", relationshipType);
        demographic.put("firstName", "Test");
        demographic.put("lastName", "Person");
        applicant.put("demographic", demographic);
        
        List<Map<String, Object>> coverages = new ArrayList<>();
        for (int i = 0; i < coverageCount; i++) {
            Map<String, Object> coverage = new HashMap<>();
            coverage.put("productType", "DENTAL");
            coverage.put("premium", 100.0);
            coverages.add(coverage);
        }
        applicant.put("coverages", coverages);
        
        return applicant;
    }
}
