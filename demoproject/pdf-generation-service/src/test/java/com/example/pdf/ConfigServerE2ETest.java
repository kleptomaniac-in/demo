package com.example.pdf;

import com.example.pdf.controller.GenerateRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConfigServerE2ETest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Skip the test if the local Config Server is not available.
    private static boolean isConfigServerAvailable() {
        try {
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> r = rt.getForEntity("http://localhost:8888/acme-invoice-vw/default/main", String.class);
            return r.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void endToEnd_generate_usesConfigServerMappings() throws Exception {
        Assumptions.assumeTrue(isConfigServerAvailable(), "Local Config Server not available on http://localhost:8888 — skipping E2E test");

        GenerateRequest req = new GenerateRequest();
        req.setTemplateName("invoice-vw");
        req.setClientService("acme");
        req.setLabel("main");
        req.setProductType("medicare");
        req.setMarketCategory("group");
        req.setState("CA");

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("date", "2025-12-01");
        payload.put("invoice", invoice);
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", "E2E-INV-999");
        payload.put("order", order);
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("name", "E2E Alice");
        payload.put("customer", customer);
        req.setPayload(payload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerateRequest> ent = new HttpEntity<>(req, headers);

        String url = "http://localhost:" + port + "/generate";
        ResponseEntity<byte[]> resp = restTemplate.postForEntity(url, ent, byte[].class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        byte[] pdf = resp.getBody();
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);

        // Inspect PDF text to ensure mapped values from the real Config Server are present
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            // These values are provided in the request payload above
            assertThat(text).contains("E2E-INV-999");
            assertThat(text).contains("E2E Alice");
        }
    }
}
