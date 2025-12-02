package com.example.pdf.service;

import com.example.pdf.controller.GenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MappingServiceProductListTest {

    @Test
    void expandsProductListFromPayloadIntoCandidatesInOrder() {
        MappingService svc = new MappingService();
        MappingProperties props = new MappingProperties();
        // candidate order contains base then product placeholder
        props.setCandidateOrder(List.of("mappings/base-application", "mappings/products/{product}", "mappings/templates/{template}"));
        svc.setMappingProperties(props);

        GenerateRequest req = new GenerateRequest();
        // payload contains products array which should be expanded
        req.setPayload(Map.of("products", List.of("A","B","C")));

        List<String> candidates = svc.buildCandidates(req);

        // Expect base first then three product entries
        assertTrue(candidates.size() >= 4);
        assertEquals("mappings/base-application", candidates.get(0));
        assertEquals("mappings/products/A", candidates.get(1));
        assertEquals("mappings/products/B", candidates.get(2));
        assertEquals("mappings/products/C", candidates.get(3));
    }
}
