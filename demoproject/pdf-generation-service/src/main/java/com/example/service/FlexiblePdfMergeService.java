package com.example.service;

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

    public byte[] generateMergedPdf(String configName, Map<String, Object> payload) throws IOException {
        // Load merge configuration
        PdfMergeConfig config = configService.loadConfig(configName);
        
        // Resolve sections (including conditionals)
        List<SectionConfig> resolvedSections = resolveSections(config, payload);
        
        // Generate individual PDFs for each section
        Map<String, PDDocument> sectionDocs = new HashMap<>();
        Map<String, Integer> sectionStartPages = new HashMap<>();
        int currentPage = 0;
        
        for (SectionConfig section : resolvedSections) {
            if (!section.isEnabled()) {
                continue;
            }
            
            PDDocument doc = generateSectionPdf(section, payload);
            sectionDocs.put(section.getName(), doc);
            sectionStartPages.put(section.getName(), currentPage);
            currentPage += doc.getNumberOfPages();
        }
        
        // Merge all documents
        PDDocument mergedDoc = mergeDocs(sectionDocs, resolvedSections);
        
        // Add page numbers if configured
        if (config.getPageNumberingConfig() != null) {
            addPageNumbers(mergedDoc, config.getPageNumberingConfig());
        }
        
        // Add common header if configured
        if (config.getHeader() != null && config.getHeader().isEnabled()) {
            addHeaderFooter(mergedDoc, config.getHeader(), payload, true);
        }
        
        // Add common footer if configured
        if (config.getFooter() != null && config.getFooter().isEnabled()) {
            addHeaderFooter(mergedDoc, config.getFooter(), payload, false);
        }
        
        // Add bookmarks if configured
        if (config.isAddBookmarks() && config.getBookmarks() != null) {
            addBookmarks(mergedDoc, config.getBookmarks(), sectionStartPages);
        }
        
        // Convert to byte array
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mergedDoc.save(output);
        mergedDoc.close();
        
        // Close section documents
        for (PDDocument doc : sectionDocs.values()) {
            doc.close();
        }
        
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
        if ("freemarker".equals(section.getType())) {
            // Generate HTML via FreeMarker
            String html = freemarkerService.processTemplateFromLocation(section.getTemplate(), payload);
            byte[] pdfBytes = htmlPdfService.renderHtmlToPdf(html);
            return PDDocument.load(new ByteArrayInputStream(pdfBytes));
            
        } else if ("pdfbox".equals(section.getType())) {
            // Generate via PDFBox generator
            PdfBoxGenerator generator = pdfBoxRegistry.getGenerator(section.getTemplate());
            return generator.generate(payload);
            
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
            float yPosition = isHeader ? pageSize.getHeight() - 20 : 20;
            
            ContentConfig content = config.getContent();
            
            // Draw left content
            if (content.getLeft() != null) {
                drawText(contentStream, content.getLeft(), 20, yPosition, i + 1, totalPages, payload);
            }
            
            // Draw center content
            if (content.getCenter() != null) {
                String text = replaceVariables(content.getCenter().getText(), i + 1, totalPages, payload);
                PDType1Font font = getFont(content.getCenter().getFont());
                float textWidth = font.getStringWidth(text) / 1000 * content.getCenter().getFontSize();
                float centerX = (pageSize.getWidth() - textWidth) / 2;
                drawText(contentStream, content.getCenter(), centerX, yPosition, i + 1, totalPages, payload);
            }
            
            // Draw right content
            if (content.getRight() != null) {
                String text = replaceVariables(content.getRight().getText(), i + 1, totalPages, payload);
                PDType1Font font = getFont(content.getRight().getFont());
                float textWidth = font.getStringWidth(text) / 1000 * content.getRight().getFontSize();
                float rightX = pageSize.getWidth() - textWidth - 20;
                drawText(contentStream, content.getRight(), rightX, yPosition, i + 1, totalPages, payload);
            }
            
            // Draw border if configured
            if (config.getBorder() != null && config.getBorder().isEnabled()) {
                float borderY = isHeader ? pageSize.getHeight() - config.getHeight() : config.getHeight();
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
        
        // Replace payload variables
        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                if (result.contains(placeholder) && entry.getValue() != null) {
                    result = result.replace(placeholder, entry.getValue().toString());
                }
            }
        }
        
        return result;
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
