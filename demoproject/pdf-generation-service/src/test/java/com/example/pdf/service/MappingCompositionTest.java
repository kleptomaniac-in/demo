package com.example.pdf.service;

import com.example.pdf.controller.GenerateRequest;
import com.example.pdf.model.MappingDocument;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingCompositionTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        if (server != null) server.shutdown();
    }

    @Test
    void composeAndMergeCandidates() throws Exception {
        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String p = request.getPath();
                if (p.equals("/application/default/main/mappings/base-application.yml")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"propertySources\":[{\"name\":\"base\",\"source\":{\"mapping.pdf.field.issuedDate\":\"invoiceDate\"}}],\"version\":\"v1\"}")
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                }
                if (p.equals("/application/default/main/mappings/templates/invoice-v2.yml")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"propertySources\":[{\"name\":\"template\",\"source\":{\"mapping.pdf.field.invoiceNumber\":\"invoiceId\",\"mapping.pdf.field.customerName\":\"customer.name\"}}],\"version\":\"v1\"}")
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                }
                if (p.equals("/application/default/main/mappings/products/medicare.yml")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"propertySources\":[{\"name\":\"product\",\"source\":{\"mapping.pdf.field.planName\":\"product.planName\"}}],\"version\":\"v1\"}")
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                }
                if (p.equals("/application/default/main/mappings/markets/group.yml") || p.equals("/application/default/main/mappings/states/CA.yml")) {
                    return new MockResponse().setResponseCode(200).setBody("{}").addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                }
                if (p.equals("/application/default/main/mappings/templates/medicare/invoice-v2.yml")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"propertySources\":[{\"name\":\"product-template\",\"source\":{\"mapping.pdf.field.medicareSpecificField\":\"product.medicareField\"}}],\"version\":\"v1\"}")
                            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);

        WebClient wc = WebClient.builder().baseUrl(server.url("").toString()).build();
        MappingService svc = new MappingService(new ConfigServerClient(wc, server.url("").toString()));

        GenerateRequest req = new GenerateRequest();
        req.setTemplateName("invoice-v2");
        req.setProductType("medicare");
        req.setMarketCategory("group");
        req.setState("CA");
        req.setLabel("main");

        MappingDocument doc = svc.composeMappingDocument(req);

        assertNotNull(doc.getMapping());
        assertNotNull(doc.getMapping().getPdf());
        Map<String, String> fields = doc.getMapping().getPdf().getField();
        assertTrue(fields.containsKey("issuedDate"));
        assertTrue(fields.containsKey("invoiceNumber"));
        assertTrue(fields.containsKey("planName"));
    }
}
