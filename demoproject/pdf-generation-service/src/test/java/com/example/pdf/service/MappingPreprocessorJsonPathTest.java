package com.example.pdf.service;

import com.example.pdf.controller.GenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MappingPreprocessorJsonPathTest {

    @Test
    public void selectorsFindPrimaryAndChildren() {
        Map<String, Object> primary = Map.of(
                "type", "Primary",
                "demographic", Map.of("firstName", "Alice")
        );
        Map<String, Object> child = Map.of(
                "type", "Child",
                "demographic", Map.of("firstName", "Charlie")
        );

        Map<String, Object> app = Map.of("applicants", List.of(primary, child));
        GenerateRequest req = new GenerateRequest();
        req.setPayload(Map.of("app", app));

        PreprocessorConfig cfg = new PreprocessorConfig();
        cfg.selectors = Map.of(
                "primary", "$.app.applicants[?(@.type=='Primary')]",
                "children", "$.app.applicants[?(@.type=='Child')]"
        );
        cfg.primaryOutputKey = "primaryApplicant";
        cfg.childSlotPrefix = "child";
        cfg.childSlotCount = 3;
        cfg.childTypeValue = "Child";

        Map<String, Object> normalized = MappingPreprocessor.normalize(req, cfg);
        System.out.println("NORMALIZED: " + normalized);
        assertNotNull(normalized);
        assertTrue(normalized.containsKey("primaryApplicant"), "primaryApplicant missing: " + normalized);
        assertTrue(normalized.containsKey("child1"), "child1 missing: " + normalized);
    }
}
