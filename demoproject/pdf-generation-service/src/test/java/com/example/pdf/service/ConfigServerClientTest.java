package com.example.pdf.service;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

class ConfigServerClientTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws Exception {
        if (server != null) server.shutdown();
    }

    @Test
    void getFile_parsesJsonConfigServerResponse_withWebClient() throws Exception {
        String path = "mappings/base-application.yml";
        String body = "{\"propertySources\":[{\"name\":\"base\",\"source\":{\"mapping.pdf.field.issuedDate\":\"invoiceDate\"}}],\"version\":\"v1\"}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        WebClient wc = WebClient.builder().baseUrl(server.url("").toString()).build();
        ConfigServerClient client = new ConfigServerClient(wc, server.url("").toString());

        ConfigServerClient.ConfigServerResponse resp = client.getFile("default", "main", path);

        Assertions.assertNotNull(resp);
        Assertions.assertNotNull(resp.propertySources);
        Assertions.assertEquals("base", resp.propertySources.get(0).name);
        Map<String, Object> src = resp.propertySources.get(0).source;
        Assertions.assertTrue(src.containsKey("mapping.pdf.field.issuedDate"));
    }

    @Test
    void getFile_parsesYamlFileContent_withWebClient() throws Exception {
        String path = "mappings/templates/invoice-v2.yml";
        String yaml = "mapping:\n  pdf:\n    field:\n      invoiceNumber: payload.order.id\n";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(yaml)
                .addHeader("Content-Type", "application/x-yaml"));

        WebClient wc = WebClient.builder().baseUrl(server.url("").toString()).build();
        ConfigServerClient client = new ConfigServerClient(wc, server.url("").toString());
        ConfigServerClient.ConfigServerResponse resp = client.getFile("default", "main", path);

        Assertions.assertNotNull(resp);
        Assertions.assertNotNull(resp.propertySources);
        Map<String, Object> src = resp.propertySources.get(0).source;
        Assertions.assertTrue(src.containsKey("mapping"));
        Object mappingNode = src.get("mapping");
        Assertions.assertTrue(mappingNode instanceof Map);
    }

    @Test
    void getFile_returnsNullOnNotFound_withWebClient() throws Exception {
        String path = "mappings/states/NOPE.yml";

        server.enqueue(new MockResponse().setResponseCode(404));

        WebClient wc = WebClient.builder().baseUrl(server.url("").toString()).build();
        ConfigServerClient client = new ConfigServerClient(wc, server.url("").toString());
        ConfigServerClient.ConfigServerResponse resp = client.getFile("default", "main", path);

        Assertions.assertNull(resp);
    }

    @Test
    void getFile_handlesMalformedJsonAndYamlGracefully_withWebClient() throws Exception {
        String path = "mappings/bad.yml";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("this is not json nor yaml")
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        WebClient wc = WebClient.builder().baseUrl(server.url("").toString()).build();
        ConfigServerClient client = new ConfigServerClient(wc, server.url("").toString());
        ConfigServerClient.ConfigServerResponse resp = client.getFile("default", "main", path);

        Assertions.assertNull(resp);
    }

    @Test
    void getFile_handlesPlainTextNonYamlAsNull_withWebClient() throws Exception {
        String path = "mappings/plain.txt";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("just some plain text that is not yaml")
                .addHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE));

        WebClient wc = WebClient.builder().baseUrl(server.url("").toString()).build();
        ConfigServerClient client = new ConfigServerClient(wc, server.url("").toString());
        ConfigServerClient.ConfigServerResponse resp = client.getFile("default", "main", path);

        Assertions.assertNull(resp);
    }
}
