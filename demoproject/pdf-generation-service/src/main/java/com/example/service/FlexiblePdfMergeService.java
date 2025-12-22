package com.example.service;

import com.example.monitoring.PerformanceMonitoringContext;
import com.example.service.FreemarkerService;
import com.example.service.HtmlPdfService;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class FlexiblePdfMergeService {

    @Autowired
    private PdfMergeConfigService configService;
    
    @Autowired
    private FreemarkerService freemarkerService;
    
    @Autowired
    private HtmlPdfService htmlPdfService;
    
    @Autowired
    private PdfBoxGeneratorRegistry pdfBoxRegistry;
    
    @Autowired
    private AcroFormFillService acroFormFillService;
    
    @Autowired(required = false)
    private PayloadEnricherRegistry payloadEnricherRegistry;
    
    @Autowired
    private PerformanceMonitoringContext performanceMonitor;

    public byte[] generateMergedPdf(String configName, Map<String, Object> payload) throws IOException {
        // Start performance monitoring
        performanceMonitor.start("PDF Generation", configName);
        
        try {
            return generateMergedPdfInternal(configName, payload);
        } finally {
            performanceMonitor.complete();
            performanceMonitor.clear();
        }
    }
    
    private byte[] generateMergedPdfInternal(String configName, Map<String, Object> payload) throws IOException {
        // Load merge configuration
        performanceMonitor.startPhase("Load Config");
        PdfMergeConfig config = configService.loadConfig(configName);
        performanceMonitor.endPhase("Load Config");
        performanceMonitor.endPhase("Load Config");
        
        // Apply global enrichers from config if specified
        performanceMonitor.startPhase("Apply Global Enrichers");
        Map<String, Object> enrichedPayload = payload;
        if (config.getPayloadEnrichers() != null && !config.getPayloadEnrichers().isEmpty()) {
            if (payloadEnricherRegistry != null) {
                System.out.println("Applying global enrichers: " + config.getPayloadEnrichers());
                enrichedPayload = payloadEnricherRegistry.applyEnrichers(
                    config.getPayloadEnrichers(), 
                    payload
                );
                System.out.println("Enriched payload keys: " + enrichedPayload.keySet());
            } else {
                System.err.println("PayloadEnricherRegistry not available, skipping global enrichers");
            }
        }
        performanceMonitor.endPhase("Apply Global Enrichers");
        
        // Resolve sections (including conditionals)
        performanceMonitor.startPhase("Resolve Sections");
        List<SectionConfig> resolvedSections = resolveSections(config, enrichedPayload);
        performanceMonitor.endPhase("Resolve Sections");
        
        // Generate individual PDFs for each section
        performanceMonitor.startPhase("Generate Section PDFs");
        Map<String, PDDocument> sectionDocs = new HashMap<>();
        Map<String, Integer> sectionStartPages = new HashMap<>();
        int currentPage = 0;
        
        for (SectionConfig section : resolvedSections) {
            if (!section.isEnabled()) {
                continue;
            }
            
            performanceMonitor.startPhase("Section: " + section.getName());
            PDDocument doc = generateSectionPdf(section, enrichedPayload);
            performanceMonitor.endPhase("Section: " + section.getName(), Map.of(
                "type", section.getType(),
                "pages", doc.getNumberOfPages()
            ));
            
            sectionDocs.put(section.getName(), doc);
            sectionStartPages.put(section.getName(), currentPage);
            currentPage += doc.getNumberOfPages();
        }
        performanceMonitor.endPhase("Generate Section PDFs");
        
        // Merge all documents
        performanceMonitor.startPhase("Merge Documents");
        PDDocument mergedDoc = mergeDocs(sectionDocs, resolvedSections);
        performanceMonitor.endPhase("Merge Documents", Map.of(
            "totalPages", mergedDoc.getNumberOfPages()
        ));
        
        // Add page numbers if configured
        if (config.getPageNumberingConfig() != null) {
            performanceMonitor.startPhase("Add Page Numbers");
            addPageNumbers(mergedDoc, config.getPageNumberingConfig());
            performanceMonitor.endPhase("Add Page Numbers");
        }
        
        // Add common header if configured
        if (config.getHeader() != null && config.getHeader().isEnabled()) {
            performanceMonitor.startPhase("Add Header");
            addHeaderFooter(mergedDoc, config.getHeader(), enrichedPayload, true);
            performanceMonitor.endPhase("Add Header");
        }
        
        // Add common footer if configured
        if (config.getFooter() != null && config.getFooter().isEnabled()) {
            performanceMonitor.startPhase("Add Footer");
            addHeaderFooter(mergedDoc, config.getFooter(), enrichedPayload, false);
            performanceMonitor.endPhase("Add Footer");
        }
        
        // Add bookmarks if configured
        if (config.isAddBookmarks() && config.getBookmarks() != null) {
            performanceMonitor.startPhase("Add Bookmarks");
            addBookmarks(mergedDoc, config.getBookmarks(), sectionStartPages);
            performanceMonitor.endPhase("Add Bookmarks");
        }
        
        // Convert to byte array
        performanceMonitor.startPhase("Serialize to Bytes");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mergedDoc.save(output);
        mergedDoc.close();
        
        // Close section documents
        for (PDDocument doc : sectionDocs.values()) {
            doc.close();
        }
        performanceMonitor.endPhase("Serialize to Bytes", Map.of(
            "sizeBytes", output.size()
        ));
        
        return output.toByteArray();
    }
    
    private List<SectionConfig> resolveSections(PdfMergeConfig config, Map<String, Object> payload) {
        List<SectionConfig> resolved = new ArrayList<>(config.getSections());
        
        // Evaluate conditional sections
        if (config.getConditionalSections() != null) {
            for (ConditionalSection conditional : config.getConditionalSections()) {
                if (evaluateCondition(conditional.getCondition(), payload)) {
                    // Insert conditional sections at appropriate positions
                    for (SectionConfig section : conditional.getSections()) {
                        if (section.getInsertAfter() != null) {
                            int insertIndex = findSectionIndex(resolved, section.getInsertAfter());
                            if (insertIndex >= 0) {
                                resolved.add(insertIndex + 1, section);
                            }
                        } else {
                            resolved.add(section);
                        }
                    }
                }
            }
        }
        
        return resolved;
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> payload) {
        // Simple condition evaluation (can be enhanced with SpEL or similar)
        // Example: "payload.includeDetailedBreakdown"
        String[] parts = condition.replace("payload.", "").split("\\.");
        Object current = payload;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return false;
            }
        }
        
        return current instanceof Boolean ? (Boolean) current : current != null;
    }
    
    private int findSectionIndex(List<SectionConfig> sections, String sectionName) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).getName().equals(sectionName)) {
                return i;
            }
        }
        return -1;
    }
    
    private PDDocument generateSectionPdf(SectionConfig section, Map<String, Object> payload) throws IOException {
        // Apply payload enrichers if specified
        Map<String, Object> enrichedPayload = payload;
        if (section.getPayloadEnrichers() != null && !section.getPayloadEnrichers().isEmpty()) {
            if (payloadEnricherRegistry != null) {
                System.out.println("Applying enrichers: " + section.getPayloadEnrichers());
                enrichedPayload = payloadEnricherRegistry.applyEnrichers(
                    section.getPayloadEnrichers(), 
                    payload
                );
            } else {
                System.err.println("PayloadEnricherRegistry not available, skipping enrichers");
            }
        }
        
        if ("freemarker".equals(section.getType())) {
            // Generate HTML via FreeMarker
            // Pass enriched payload directly as the model
            // Templates can access fields directly: ${enrollmentContext.marketDisplay}
            String html = freemarkerService.processTemplateFromLocation(section.getTemplate(), enrichedPayload);
            byte[] pdfBytes = htmlPdfService.renderHtmlToPdf(html);
            return PDDocument.load(new ByteArrayInputStream(pdfBytes));
            
        } else if ("pdfbox".equals(section.getType())) {
            // Generate via PDFBox generator
            PdfBoxGenerator generator = pdfBoxRegistry.getGenerator(section.getTemplate());
            return generator.generate(enrichedPayload);
            
        } else if ("acroform".equals(section.getType())) {
            // Fill AcroForm PDF using field mappings
            
            // Start with base field mappings
            java.util.Map<String, String> allFieldMappings = new java.util.HashMap<>();
            
            // Expand patterns first (if any)
            if (section.getPatterns() != null && !section.getPatterns().isEmpty()) {
                java.util.Map<String, String> expandedMappings = acroFormFillService.expandPatterns(section.getPatterns());
                allFieldMappings.putAll(expandedMappings);
            }
            
            // Add explicit field mappings (can override pattern-generated ones)
            if (section.getFieldMapping() != null) {
                allFieldMappings.putAll(section.getFieldMapping());
            }
            
            if (allFieldMappings.isEmpty()) {
                throw new IllegalArgumentException("AcroForm section must have fieldMapping or patterns: " + section.getName());
            }
            
            byte[] filledPdf = acroFormFillService.fillAcroForm(
                section.getTemplate(), 
                allFieldMappings, 
                enrichedPayload
            );
            return PDDocument.load(new ByteArrayInputStream(filledPdf));
            
        } else {
            throw new IllegalArgumentException("Unknown section type: " + section.getType());
        }
    }
    
    private PDDocument mergeDocs(Map<String, PDDocument> sectionDocs, List<SectionConfig> sections) throws IOException {
        PDDocument mergedDoc = new PDDocument();
        
        for (SectionConfig section : sections) {
            if (!section.isEnabled()) {
                continue;
            }
            
            PDDocument doc = sectionDocs.get(section.getName());
            if (doc != null) {
                for (PDPage page : doc.getPages()) {
                    mergedDoc.addPage(page);
                }
            }
        }
        
        return mergedDoc;
    }
    
    private void addPageNumbers(PDDocument doc, PageNumberingConfig config) throws IOException {
        int totalPages = doc.getNumberOfPages();
        int startPage = config.getStartPage() - 1; // Convert to 0-based index
        
        for (int i = startPage; i < totalPages; i++) {
            PDPage page = doc.getPage(i);
            PDPageContentStream contentStream = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true
            );
            
            String pageText = config.getFormat()
                .replace("{current}", String.valueOf(i + 1))
                .replace("{total}", String.valueOf(totalPages));
            
            float fontSize = config.getFontSize();
            PDType1Font font = PDType1Font.HELVETICA;
            float textWidth = font.getStringWidth(pageText) / 1000 * fontSize;
            
            PDRectangle pageSize = page.getMediaBox();
            float x = calculateXPosition(config.getPosition(), pageSize.getWidth(), textWidth);
            float y = calculateYPosition(config.getPosition(), pageSize.getHeight());
            
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(pageText);
            contentStream.endText();
            contentStream.close();
        }
    }
    
    private float calculateXPosition(String position, float pageWidth, float textWidth) {
        if (position.contains("center")) {
            return (pageWidth - textWidth) / 2;
        } else if (position.contains("right")) {
            return pageWidth - textWidth - 20;
        } else {
            return 20;
        }
    }
    
    private float calculateYPosition(String position, float pageHeight) {
        if (position.contains("top")) {
            return pageHeight - 30;
        } else {
            return 20;
        }
    }
    
    private void addBookmarks(PDDocument doc, List<BookmarkConfig> bookmarks, Map<String, Integer> sectionStartPages) {
        PDDocumentOutline outline = new PDDocumentOutline();
        doc.getDocumentCatalog().setDocumentOutline(outline);
        
        Map<Integer, PDOutlineItem> levelParents = new HashMap<>();
        
        for (BookmarkConfig bookmark : bookmarks) {
            Integer pageIndex = sectionStartPages.get(bookmark.getSection());
            if (pageIndex == null || pageIndex >= doc.getNumberOfPages()) {
                continue;
            }
            
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(bookmark.getTitle());
            
            PDPageDestination dest = new PDPageFitDestination();
            dest.setPage(doc.getPage(pageIndex));
            item.setDestination(dest);
            
            if (bookmark.getLevel() == 1) {
                outline.addLast(item);
                levelParents.put(1, item);
            } else {
                PDOutlineItem parent = levelParents.get(bookmark.getLevel() - 1);
                if (parent != null) {
                    parent.addLast(item);
                    levelParents.put(bookmark.getLevel(), item);
                }
            }
        }
        
        outline.openNode();
    }
    
    private void addHeaderFooter(PDDocument doc, HeaderFooterConfig config, Map<String, Object> payload, boolean isHeader) throws IOException {
        int totalPages = doc.getNumberOfPages();
        int startPage = config.getStartPage() - 1; // Convert to 0-based index
        
        for (int i = startPage; i < totalPages; i++) {
            PDPage page = doc.getPage(i);
            PDPageContentStream contentStream = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true
            );
            
            PDRectangle pageSize = page.getMediaBox();
            // Position text centered within the header/footer area
            float textYPosition;
            if (isHeader) {
                // Center in header area: pageSize.getHeight() - (config.getHeight() / 2)
                // Adjust slightly for baseline (approx 3-4 points)
                textYPosition = pageSize.getHeight() - (config.getHeight() / 2) - 4;
            } else {
                // Center in footer area: config.getHeight() / 2
                textYPosition = (config.getHeight() / 2) - 4;
            }
            
            ContentConfig content = config.getContent();
            
            // Draw left content
            if (content.getLeft() != null) {
                drawText(contentStream, content.getLeft(), 20, textYPosition, i + 1, totalPages, payload);
            }
            
            // Draw center content
            if (content.getCenter() != null) {
                String text = replaceVariables(content.getCenter().getText(), i + 1, totalPages, payload);
                PDType1Font font = getFont(content.getCenter().getFont());
                float textWidth = font.getStringWidth(text) / 1000 * content.getCenter().getFontSize();
                float centerX = (pageSize.getWidth() - textWidth) / 2;
                drawText(contentStream, content.getCenter(), centerX, textYPosition, i + 1, totalPages, payload);
            }
            
            // Draw right content
            if (content.getRight() != null) {
                String text = replaceVariables(content.getRight().getText(), i + 1, totalPages, payload);
                PDType1Font font = getFont(content.getRight().getFont());
                float textWidth = font.getStringWidth(text) / 1000 * content.getRight().getFontSize();
                float rightX = pageSize.getWidth() - textWidth - 20;
                drawText(contentStream, content.getRight(), rightX, textYPosition, i + 1, totalPages, payload);
            }
            
            // Draw border if configured (at the bottom edge of header / top edge of footer)
            if (config.getBorder() != null && config.getBorder().isEnabled()) {
                float borderY = isHeader 
                    ? pageSize.getHeight() - config.getHeight()  // Bottom of header area
                    : config.getHeight();  // Top of footer area
                contentStream.setLineWidth(config.getBorder().getThickness());
                contentStream.setStrokingColor(parseColor(config.getBorder().getColor()));
                contentStream.moveTo(20, borderY);
                contentStream.lineTo(pageSize.getWidth() - 20, borderY);
                contentStream.stroke();
            }
            
            contentStream.close();
        }
    }
    
    private void drawText(PDPageContentStream contentStream, TextConfig textConfig, float x, float y, int currentPage, int totalPages, Map<String, Object> payload) throws IOException {
        String text = replaceVariables(textConfig.getText(), currentPage, totalPages, payload);
        PDType1Font font = getFont(textConfig.getFont());
        
        contentStream.beginText();
        contentStream.setFont(font, textConfig.getFontSize());
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
    
    private String replaceVariables(String text, int currentPage, int totalPages, Map<String, Object> payload) {
        if (text == null) return "";
        
        String result = text
            .replace("{current}", String.valueOf(currentPage))
            .replace("{total}", String.valueOf(totalPages))
            .replace("{date}", java.time.LocalDate.now().toString());
        
        // Replace payload variables (including nested paths with dot notation)
        if (payload != null) {
            // Find all placeholders in the text
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(result);
            
            while (matcher.find()) {
                String placeholder = matcher.group(0); // e.g., "{enrollment.effectiveDate}"
                String path = matcher.group(1);        // e.g., "enrollment.effectiveDate"
                
                Object value = resolveNestedPath(payload, path);
                if (value != null) {
                    result = result.replace(placeholder, value.toString());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Resolves nested paths in payload using dot notation.
     * Example: "enrollment.effectiveDate" -> payload.get("enrollment").get("effectiveDate")
     */
    private Object resolveNestedPath(Map<String, Object> payload, String path) {
        if (path == null || path.isEmpty()) return null;
        
        String[] parts = path.split("\\.");
        Object current = payload;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    private PDType1Font getFont(String fontName) {
        switch (fontName) {
            case "Helvetica-Bold":
                return PDType1Font.HELVETICA_BOLD;
            case "Helvetica-Oblique":
                return PDType1Font.HELVETICA_OBLIQUE;
            case "Times-Roman":
                return PDType1Font.TIMES_ROMAN;
            case "Times-Bold":
                return PDType1Font.TIMES_BOLD;
            case "Courier":
                return PDType1Font.COURIER;
            default:
                return PDType1Font.HELVETICA;
        }
    }
    
    private java.awt.Color parseColor(String colorHex) {
        if (colorHex.startsWith("#")) {
            colorHex = colorHex.substring(1);
        }
        return new java.awt.Color(
            Integer.parseInt(colorHex.substring(0, 2), 16),
            Integer.parseInt(colorHex.substring(2, 4), 16),
            Integer.parseInt(colorHex.substring(4, 6), 16)
        );
    }
}
