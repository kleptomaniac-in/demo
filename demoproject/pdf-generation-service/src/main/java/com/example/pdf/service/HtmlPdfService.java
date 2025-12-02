package com.example.pdf.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class HtmlPdfService {

    private static final Logger log = LoggerFactory.getLogger(HtmlPdfService.class);

    public byte[] renderHtmlToPdf(String html) throws IOException {
        if (html == null) html = "";
        // Remove BOM if present
        if (html.length() > 0 && html.charAt(0) == '\uFEFF') {
            html = html.substring(1);
        }
        // Strip any characters that appear before the first root element '<'
        int firstLt = html.indexOf('<');
        if (firstLt > 0) {
            html = html.substring(firstLt);
        }

        // Use Jsoup to tidy and convert arbitrary HTML into well-formed XHTML
        try {
            Document doc = Jsoup.parse(html);
            Document.OutputSettings out = new Document.OutputSettings();
            out.syntax(Document.OutputSettings.Syntax.xml);
            out.charset(StandardCharsets.UTF_8);
            out.escapeMode(Entities.EscapeMode.xhtml);
            doc.outputSettings(out);
            // Ensure the html element has the XHTML namespace
            if (!doc.select("html").attr("xmlns").equals("http://www.w3.org/1999/xhtml")) {
                doc.select("html").attr("xmlns", "http://www.w3.org/1999/xhtml");
            }
            html = doc.outerHtml();
            // Prepend XML prolog if missing
            if (!html.trim().startsWith("<?xml")) {
                html = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + html;
            }
        } catch (Exception e) {
            log.debug("Jsoup conversion to XHTML failed, falling back to simple coercion", e);
            try {
                if (html.toLowerCase().contains("<!doctype html>")) {
                    html = html.replaceAll("(?i)<!doctype html>", "<!DOCTYPE html>");
                }
                if (!html.toLowerCase().contains("xmlns=\"http://www.w3.org/1999/xhtml\"")) {
                    html = html.replaceFirst("(?i)<html(?![^>]*xmlns)", "<html xmlns=\"http://www.w3.org/1999/xhtml\"");
                }
                if (!html.trim().startsWith("<?xml")) {
                    html = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + html;
                }
            } catch (Exception ex) {
                log.debug("Fallback coercion also failed", ex);
            }
        }

        // Log a short snippet (codepoints) to help diagnose malformed-leading-content errors
        if (log.isDebugEnabled()) {
            String snippet = html.length() <= 200 ? html : html.substring(0, 200);
            StringBuilder cps = new StringBuilder();
            for (int i = 0; i < Math.min(40, snippet.length()); i++) {
                cps.append((int) snippet.charAt(i)).append(' ');
            }
            log.debug("Rendering HTML snippet (first 200 chars): \n{}", snippet);
            log.debug("Snippet char codes: {}", cps.toString());
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            // Use current directory as base URI so relative resources resolve during development
            String baseUri = null;
            builder.withHtmlContent(html, baseUri);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            String snippet = html.length() <= 200 ? html : html.substring(0, 200);
            String msg = "Failed to render HTML to PDF. HTML snippet: '" + snippet.replaceAll("\n", "\\n") + "'";
            log.error(msg, e);
            throw new IOException(msg, e);
        }
    }

    public String fetchTemplateContent(String urlOrPath) throws IOException {
        if (urlOrPath == null) return "";
        String trimmed = urlOrPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try (InputStream in = new URL(trimmed).openStream()) {
                return readStream(in);
            }
        }

        // try classpath resource
        InputStream res = this.getClass().getResourceAsStream(trimmed.startsWith("/") ? trimmed : ("/" + trimmed));
        if (res != null) {
            try (InputStream in = res) {
                return readStream(in);
            }
        }

        // try local file
        File f = new File(trimmed);
        if (f.exists() && f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                return readStream(in);
            }
        }

        throw new FileNotFoundException("Template not found: " + urlOrPath);
    }

    private String readStream(InputStream in) throws IOException {
        try (Reader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    public String applySimpleReplacements(String templateHtml, Map<String, Object> values) {
        if (templateHtml == null) return "";
        String out = templateHtml;
        if (values == null || values.isEmpty()) return out;
        for (Map.Entry<String, Object> e : values.entrySet()) {
            String key = e.getKey();
            String val = e.getValue() == null ? "" : String.valueOf(e.getValue());
            // replace {{key}} and ${key} forms
            out = out.replace("{{" + key + "}}", escapeHtml(val));
            out = out.replace("${" + key + "}", escapeHtml(val));
        }
        return out;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
