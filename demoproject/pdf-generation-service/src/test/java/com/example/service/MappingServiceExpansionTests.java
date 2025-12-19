package com.example.service;

import com.example.pdf.controller.GenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MappingServiceExpansionTests {

    @Test
    void expandsMarketListFromPayloadIntoCandidatesInOrder() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of("mappings/base-application", "mappings/markets/{market}"));
        svc.setMappingProperties(props);

        GenerateRequest req = new GenerateRequest();
        req.setPayload(Map.of("markets", List.of("M1", "M2")));

        List<String> candidates = svc.buildCandidates(req);

        assertTrue(candidates.size() >= 3);
        assertEquals("mappings/base-application", candidates.get(0));
        assertEquals("mappings/markets/M1", candidates.get(1));
        assertEquals("mappings/markets/M2", candidates.get(2));
    }

    @Test
    void expandsProductListAndTemplateCombinedIntoOrderedCandidates() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        props.setCandidateOrder(List.of(
                "mappings/base-application",
                "mappings/templates/{template}",
                "mappings/products/{product}",
                "mappings/templates/{product}/{template}"
        ));
        svc.setMappingProperties(props);

        GenerateRequest req = new GenerateRequest();
        req.setTemplateName("inv");
        req.setPayload(Map.of("products", List.of("X", "Y")));

        List<String> candidates = svc.buildCandidates(req);

        assertTrue(candidates.size() >= 6);
        assertEquals("mappings/base-application", candidates.get(0));
        assertEquals("mappings/templates/inv", candidates.get(1));
        assertEquals("mappings/products/X", candidates.get(2));
        assertEquals("mappings/products/Y", candidates.get(3));
        assertEquals("mappings/templates/X/inv", candidates.get(4));
        assertEquals("mappings/templates/Y/inv", candidates.get(5));
    }
}
