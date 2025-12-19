package com.example.service;

import com.example.pdf.controller.GenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MappingPreprocessorTest {

    @Test
    public void normalizeTypicalAppPayload() {
        Map<String, Object> primary = Map.of(
                "type", "Primary",
                "demographic", Map.of("firstName", "Alice", "lastName", "Smith"),
                "addresses", List.of(
                        Map.of("addressType", "HOME", "addressLine1", "123 Main"),
                        Map.of("addressType", "BILLING", "addressLine1", "PO Box 1")
                ),
                "priorCoverages", List.of(
                        Map.of("coverageType", "MEDICAL", "insurer", "Acme Med"),
                        Map.of("coverageType", "VISION", "insurer", "Sight Inc")
                ),
                "products", List.of(
                        Map.of("type", "MEDICAL", "plans", List.of(Map.of("planName", "Gold", "planCode", "G1")))
                )
        );

        Map<String, Object> spouse = Map.of(
                "type", "Spouse",
                "demographic", Map.of("firstName", "Bob")
        );

        Map<String, Object> child1 = Map.of(
                "type", "Child",
                "demographic", Map.of("firstName", "Charlie")
        );

        Map<String, Object> app = Map.of("applicants", List.of(primary, spouse, child1));
        GenerateRequest req = new GenerateRequest();
        // also verify behavior when payload root is the app object
        req.setPayload(Map.of("app", app));

        PreprocessorConfig cfg = new PreprocessorConfig();
        // configure selectors and output keys explicitly so the preprocessor
        // remains generic and free of embedded domain strings.
        cfg.primaryTypeValue = "Primary";
        cfg.spouseTypeValue = "Spouse";
        cfg.childTypeValue = "Child";
        cfg.primaryOutputKey = "primaryApplicant";
        cfg.spouseOutputKey = "spouseApplicant";
        cfg.childSlotPrefix = "child";
        cfg.childSlotCount = 3;
        cfg.homeAddressOutputKey = "homeAddress";
        cfg.billingAddressOutputKey = "billingAddress";
        cfg.coverageOutputMap = Map.of("MEDICAL", "priorMedical", "VISION", "priorVision");

        Map<String, Object> normalized = MappingPreprocessor.normalize(req, cfg);

        assertNotNull(normalized);
        assertTrue(normalized.containsKey("primaryApplicant"));
        assertTrue(normalized.containsKey("spouseApplicant"));
        assertTrue(normalized.containsKey("child1"));
        assertNull(normalized.get("child2"));

        Map<?,?> p = (Map<?,?>) normalized.get("primaryApplicant");
        assertNotNull(p.get("homeAddress"));
        assertNotNull(p.get("billingAddress"));

        Map<?,?> priorMed = (Map<?,?>) p.get("priorMedical");
        assertNotNull(priorMed);
        assertEquals("Acme Med", priorMed.get("insurer"));

        Object products = p.get("products");
        assertTrue(products instanceof Map);
        Map<?,?> prodMap = (Map<?,?>) products;
        assertTrue(prodMap.containsKey("MEDICAL"));
                Map<?,?> med = (Map<?,?>) prodMap.get("MEDICAL");
                assertTrue(med.containsKey("plans"));
    }
}
