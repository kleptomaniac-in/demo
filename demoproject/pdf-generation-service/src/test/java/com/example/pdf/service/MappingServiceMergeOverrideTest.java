package com.example.pdf.service;

import com.example.pdf.controller.GenerateRequest;
import com.example.pdf.model.MappingDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

class MappingServiceMergeOverrideTest {

    static class TestClient extends ConfigServerClient {
        @Override
        public Optional<Map<String, Object>> getFileSource(String profile, String label, String pathWithExtension) {
            if ("mappings/base-application.yml".equals(pathWithExtension)) {
                return Optional.of(Map.of(
                        "mapping.pdf.field.invoiceNumber", "payload.order.id",
                        "mapping.pdf.field.customerName", "payload.customer.name",
                        "mapping.pdf.field.planName", "payload.product.planName"
                ));
            }
            return Optional.empty();
        }
    }

    @Test
    void overrideIsMergedIntoComposedMapping() throws Exception {
        MappingService svc = new MappingService(new TestClient());

        GenerateRequest req = new GenerateRequest();
        req.setClientService("acme");
        req.setTemplateName("invoice-v2");
        req.setLabel("main");

        // Inline override replaces invoiceNumber and adds issuedDate
        String overrideYaml = "mapping:\n  pdf:\n    field:\n      invoiceNumber: payload.order.customId\n      issuedDate: payload.invoice.date\n";
        req.setMappingOverride(overrideYaml);

        MappingDocument doc = svc.composeMappingDocument(req);

        Assertions.assertNotNull(doc.getMapping());
        Assertions.assertNotNull(doc.getMapping().getPdf());
        Map<String, String> fields = doc.getMapping().getPdf().getField();

        // invoiceNumber should come from override
        Assertions.assertEquals("payload.order.customId", fields.get("invoiceNumber"));
        // customerName should remain from baseline
        Assertions.assertEquals("payload.customer.name", fields.get("customerName"));
        // issuedDate should be present from override
        Assertions.assertEquals("payload.invoice.date", fields.get("issuedDate"));
        // planName should remain from baseline
        Assertions.assertEquals("payload.product.planName", fields.get("planName"));
    }
}
