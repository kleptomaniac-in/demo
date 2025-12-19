package com.example.pdf.controller;

import com.example.pdf.model.MappingDocument;
import com.example.service.MappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc
public class InvoiceControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MappingService mappingService;
    
    @MockBean
    private com.example.service.HtmlPdfService htmlPdfService;
    
    @MockBean
    private com.example.service.FreemarkerService freemarkerService;
    
    @MockBean
    private com.example.function.FunctionExpressionResolver functionExpressionResolver;
    
    @MockBean
    private com.example.preprocessing.service.ConfigurablePayloadPreProcessor configurablePayloadPreProcessor;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void generateReturnsPdfBytes() throws Exception {
        // Prepare a simple request JSON with nested payload
        String reqJson = "{" +
                "\"templateName\": \"invoice-test\"," +
                "\"clientService\": \"acme\"," +
                "\"label\": \"main\"," +
                "\"productType\": \"medicare\"," +
                "\"marketCategory\": \"group\"," +
                "\"state\": \"CA\"," +
                "\"payload\": {\"invoice\": {\"date\": \"2025-12-01\"},\"order\": {\"id\": \"INV-123\"},\"customer\": {\"name\": \"Alice\"}}" +
                "}";

        // Build a MappingDocument describing pdf field mappings
        MappingDocument doc = new MappingDocument();
        MappingDocument.Mapping m = new MappingDocument.Mapping();
        MappingDocument.Mapping.Pdf p = new MappingDocument.Mapping.Pdf();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("issuedDate", "payload.invoice.date");
        fields.put("invoiceNumber", "payload.order.id");
        fields.put("customerName", "payload.customer.name");
        p.setField(fields);
        m.setPdf(p);
        doc.setMapping(m);

        when(mappingService.composeMappingDocument(any())).thenReturn(doc);
        // extractFieldMap should return the same flattened mapping (controller calls it)
        when(mappingService.extractFieldMap(any())).thenReturn(fields);

        // Make resolvePath behave like the real method: traverse maps by '.'
        Answer<Object> resolver = invocation -> {
            Map<String, Object> payload = invocation.getArgument(0);
            String path = invocation.getArgument(1);
            if (path == null) return null;
            String pth = path;
            if (pth.startsWith("payload.")) pth = pth.substring("payload.".length());
            String[] parts = pth.split("\\.");
            Object cur = payload;
            for (String part : parts) {
                if (!(cur instanceof Map)) return null;
                cur = ((Map) cur).get(part);
            }
            return cur;
        };

        doAnswer(resolver).when(mappingService).resolvePath(anyMap(), anyString());

        // Perform POST and assert response
        byte[] resp = mvc.perform(post("/api/invoice/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(resp).isNotNull();
        assertThat(resp.length).isGreaterThan(0);
    }
}
