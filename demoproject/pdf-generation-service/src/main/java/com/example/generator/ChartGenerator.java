package com.example.generator;


import com.example.service.PdfBoxGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Example PDFBox generator for cover pages.
 * This shows how to wrap your existing PDFBox code.
 */
@Component
public class ChartGenerator implements PdfBoxGenerator {
    
    @Override
    public String getName() {
        return "premium-chart-generator";
    }
    
    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        // Extract data from payload
        String title = (String) payload.getOrDefault("title", "Healthcare Plan Report");
        String companyName = (String) payload.getOrDefault("companyName", "Company Name");
        
        // Draw title
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
        contentStream.newLineAtOffset(100, 700);
        contentStream.showText("chart:" + title);
        contentStream.endText();
        
        // Draw company name
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 16);
        contentStream.newLineAtOffset(100, 650);
        contentStream.showText(companyName);
        contentStream.endText();
        
        // Draw date
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.newLineAtOffset(100, 100);
        contentStream.showText("Generated: " + java.time.LocalDate.now());
        contentStream.endText();
        
        contentStream.close();
        
        return document;
    }
}
