package com.example.pdf.controller;

import com.example.pdf.service.MappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/generate")
public class GenerateController {

    private final MappingService mappingService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final com.example.pdf.service.HtmlPdfService htmlPdfService;
    private final com.example.pdf.service.FreemarkerService freemarkerService;

    public GenerateController(MappingService mappingService,
                              com.example.pdf.service.HtmlPdfService htmlPdfService,
                              com.example.pdf.service.FreemarkerService freemarkerService) {
        this.mappingService = mappingService;
        this.htmlPdfService = htmlPdfService;
        this.freemarkerService = freemarkerService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ByteArrayResource> generate(@Valid @RequestBody GenerateRequest req) throws Exception {
        System.out.println("Received generate request: " + mapper.writeValueAsString(req));
        try {
            // Resolve mapping document: either override or fetch from config server
            com.example.pdf.model.MappingDocument doc = mappingService.composeMappingDocument(req);

            System.out.println("Resolved mapping document: " + mapper.writeValueAsString(doc));

            // Extract the pdf field->payloadPath map
            Map<String, String> fieldMap = mappingService.extractFieldMap(doc);
            System.out.println("Extracted field map: " + mapper.writeValueAsString(fieldMap));

            // Apply mapping: for each mapping entry, resolve payload path
            Map<String, Object> resolved = new LinkedHashMap<>();
            Map<String, Object> payload = req.getPayload() == null ? Map.of() : req.getPayload();
            System.out.println("Using payload: " + mapper.writeValueAsString(payload));
            for (Map.Entry<String, String> e : fieldMap.entrySet()) {
                String pdfField = e.getKey();
                String payloadPath = e.getValue();
                System.out.println("Mapping PDF field '" + pdfField + "' to payload path '" + payloadPath + "'");
                Object value = mappingService.resolvePath(payload, payloadPath);
                System.out.println("  Resolved value: " + (value == null ? "null" : mapper.writeValueAsString(value)));
                resolved.put(pdfField, value == null ? "" : value);
            }
            System.out.println("Final resolved PDF data: " + mapper.writeValueAsString(resolved));

            byte[] pdf;

        // If mapping document indicates an HTML template, render HTML -> PDF
        if (doc.getTemplate() != null) {
            String ttype = doc.getTemplate().getType();
            String templateUrl = doc.getTemplate().getUrl();
            try {
                if (ttype != null && ("freemarker".equalsIgnoreCase(ttype) || "ftl".equalsIgnoreCase(ttype))) {
                    // Build a model that contains both the resolved mapping values and the original payload
                    Map<String, Object> model = new LinkedHashMap<>(resolved);
                    model.put("payload", payload);
                    String rendered = freemarkerService.processTemplateFromLocation(templateUrl, model);
                    pdf = htmlPdfService.renderHtmlToPdf(rendered);
                } else if (ttype != null && "html".equalsIgnoreCase(ttype)) {
                    // Provide the payload to the simple replacer too (it expects a map)
                    Map<String, Object> model = new LinkedHashMap<>(resolved);
                    model.put("payload", payload);
                    String html = htmlPdfService.fetchTemplateContent(templateUrl);
                    String rendered = htmlPdfService.applySimpleReplacements(html, model);
                    pdf = htmlPdfService.renderHtmlToPdf(rendered);
                } else {
                    pdf = createPdfFromMap(resolved);
                }
            } catch (Exception ex) {
                // Return the exception message as JSON to aid debugging (temporary)
                String msg = "{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
                ByteArrayResource err = new ByteArrayResource(msg.getBytes());
                HttpHeaders errHeaders = new HttpHeaders();
                errHeaders.setContentType(MediaType.APPLICATION_JSON);
                errHeaders.setContentLength(msg.length());
                return ResponseEntity.status(500).headers(errHeaders).body(err);
            }
        } else {
            // Create a tiny PDF with resolved key-values
            pdf = createPdfFromMap(resolved);
        }

            ByteArrayResource resource = new ByteArrayResource(pdf);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdf.length);
            headers.setContentDispositionFormData("attachment", req.getTemplateName() + ".pdf");

            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception ex) {
            // Catch anything and return JSON error body to help debugging
            String msg = "{\"error\":\"" + ex.toString().replace("\"", "'") + "\"}";
            ByteArrayResource err = new ByteArrayResource(msg.getBytes());
            HttpHeaders errHeaders = new HttpHeaders();
            errHeaders.setContentType(MediaType.APPLICATION_JSON);
            errHeaders.setContentLength(msg.length());
            System.out.println("Template/request processing failed: " + ex.toString());
            ex.printStackTrace(System.out);
            return ResponseEntity.status(500).headers(errHeaders).body(err);
        }
    }

    private byte[] createPdfFromMap(Map<String, Object> data) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 700);
                for (Map.Entry<String, Object> e : data.entrySet()) {
                    String line = String.format("%s: %s", e.getKey(), String.valueOf(e.getValue()));
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }

}
