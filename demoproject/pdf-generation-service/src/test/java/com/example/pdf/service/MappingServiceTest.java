package com.example.pdf.service;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MappingServiceTest {

    @Test
    void configuredCandidateOrderExpandsPlaceholders() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of(
                "mappings/base-application",
                "mappings/templates/{template}",
                "mappings/products/{product}",
                "mappings/markets/{market}",
                "mappings/states/{state}",
                "mappings/templates/{product}/{template}"
        ));
        svc.setMappingProperties(props);

        com.example.pdf.controller.GenerateRequest req = new com.example.pdf.controller.GenerateRequest();
        req.setTemplateName("invoice-v2");
        req.setProductType("medicare");
        req.setMarketCategory("group");
        req.setState("CA");
        List<String> candidates = svc.buildCandidates(req);

        assertEquals(6, candidates.size());
        assertEquals("mappings/base-application", candidates.get(0));
        assertEquals("mappings/templates/invoice-v2", candidates.get(1));
        assertEquals("mappings/products/medicare", candidates.get(2));
        assertEquals("mappings/markets/group", candidates.get(3));
        assertEquals("mappings/states/CA", candidates.get(4));
        assertEquals("mappings/templates/medicare/invoice-v2", candidates.get(5));
    }

    @Test
    void configuredCandidateOrderSkipsMissingPlaceholders() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of(
                "mappings/base-application",
                "mappings/templates/{template}",
                "mappings/products/{product}",
                "mappings/templates/{product}/{template}"
        ));
        svc.setMappingProperties(props);

        // template missing -> template entries should be skipped
        com.example.pdf.controller.GenerateRequest req2 = new com.example.pdf.controller.GenerateRequest();
        req2.setProductType("medicare");
        List<String> candidates = svc.buildCandidates(req2);
        assertTrue(candidates.contains("mappings/base-application"));
        assertTrue(candidates.contains("mappings/products/medicare"));
        // template-based entries should not be present
        assertFalse(candidates.stream().anyMatch(s -> s.contains("templates/" ) && s.contains("null")));
        // combined product/template should be skipped because template is missing
        assertFalse(candidates.stream().anyMatch(s -> s.equals("mappings/templates/medicare/null")));
    }

    @Test
    void fallbackOrderingWhenNoProperties() {
        MappingService svc = new MappingService();
        // do not set mappingProperties -> should use fallback
        com.example.pdf.controller.GenerateRequest req3 = new com.example.pdf.controller.GenerateRequest();
        req3.setTemplateName("t1");
        req3.setMarketCategory("marketA");
        List<String> candidates = svc.buildCandidates(req3);
        // fallback always includes base and present segments only
        assertTrue(candidates.contains("mappings/base-application"));
        assertTrue(candidates.contains("mappings/templates/t1"));
        assertTrue(candidates.contains("mappings/markets/marketA"));
        // products/state missing -> should not contain these
        assertFalse(candidates.stream().anyMatch(s -> s.startsWith("mappings/products/")));
        assertFalse(candidates.stream().anyMatch(s -> s.startsWith("mappings/states/")));
    }

    @Test
    void trimmedPatternIsTrimmedAndExpanded() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        // pattern has surrounding whitespace that should be trimmed
        props.setCandidateOrder(List.of("  mappings/templates/{template}  "));
        svc.setMappingProperties(props);

        com.example.pdf.controller.GenerateRequest req4 = new com.example.pdf.controller.GenerateRequest();
        req4.setTemplateName("invoice-v2");
        List<String> candidates = svc.buildCandidates(req4);
        assertEquals(1, candidates.size());
        assertEquals("mappings/templates/invoice-v2", candidates.get(0));
    }

    @Test
    void emptyConfiguredListFallsBackToDefaultOrdering() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of());
        svc.setMappingProperties(props);

        com.example.pdf.controller.GenerateRequest req5 = new com.example.pdf.controller.GenerateRequest();
        req5.setTemplateName("t1");
        req5.setProductType("p1");
        req5.setMarketCategory("m1");
        req5.setState("s1");
        List<String> candidates = svc.buildCandidates(req5);
        assertTrue(candidates.contains("mappings/base-application"));
        assertTrue(candidates.contains("mappings/templates/t1"));
        assertTrue(candidates.contains("mappings/products/p1"));
        assertTrue(candidates.contains("mappings/markets/m1"));
        assertTrue(candidates.contains("mappings/states/s1"));
        assertTrue(candidates.contains("mappings/templates/p1/t1"));
    }

    @Test
    void rejectsLiteralNullStringValues() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of("mappings/base-application", "mappings/templates/{template}"));
        svc.setMappingProperties(props);

        // template literally "null" should be treated as invalid and skipped by expandPattern
        com.example.pdf.controller.GenerateRequest req6 = new com.example.pdf.controller.GenerateRequest();
        req6.setTemplateName("null");
        req6.setProductType("p1");
        List<String> candidates = svc.buildCandidates(req6);
        assertTrue(candidates.contains("mappings/base-application"));
        assertFalse(candidates.stream().anyMatch(s -> s.contains("templates/")));
    }

    @Test
    void keepsUnknownPlaceholderWhenNotRecognized() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of("mappings/custom/{unknown}"));
        svc.setMappingProperties(props);

        com.example.pdf.controller.GenerateRequest req7 = new com.example.pdf.controller.GenerateRequest();
        req7.setTemplateName("t");
        req7.setProductType("p");
        req7.setMarketCategory("m");
        req7.setState("s");
        List<String> candidates = svc.buildCandidates(req7);
        // unknown placeholder remains unexpanded but the pattern is non-empty and should be returned
        assertTrue(candidates.size() == 1);
        assertTrue(candidates.get(0).contains("{unknown}"));
    }
}

