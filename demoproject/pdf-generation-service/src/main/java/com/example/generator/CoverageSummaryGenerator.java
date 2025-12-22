package com.example.generator;

import com.example.service.PdfBoxGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component("CoverageSummaryGenerator")
public class CoverageSummaryGenerator implements PdfBoxGenerator {

    @Override
    public String getName() {
        return "CoverageSummaryGenerator";
    }

    @Override
    public PDDocument generate(Map<String, Object> payload) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;
            float lineHeight = 15;

            // Title
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Coverage Summary");
            contentStream.endText();
            yPosition -= 30;

            // Use enriched data from CoverageSummaryEnricher
            Map<String, Object> coverageSummary = (Map<String, Object>) payload.get("coverageSummary");
            
            if (coverageSummary != null) {
                contentStream.setFont(PDType1Font.HELVETICA, 11);
                
                // Application Info (pre-formatted by enricher)
                String applicationNumber = getString(coverageSummary, "applicationNumber");
                if (applicationNumber != null) {
                    drawText(contentStream, "Application Number: " + applicationNumber, margin, yPosition);
                    yPosition -= lineHeight;
                }

                String formattedDate = getString(coverageSummary, "formattedEffectiveDate");
                if (formattedDate != null) {
                    drawText(contentStream, "Effective Date: " + formattedDate, margin, yPosition);
                    yPosition -= lineHeight;
                }

                String totalPremium = getString(coverageSummary, "totalPremium");
                if (totalPremium != null) {
                    drawText(contentStream, "Total Monthly Premium: $" + totalPremium, margin, yPosition);
                    yPosition -= lineHeight * 2;
                }

                // Enriched Applicants (with calculated ages)
                List<Map<String, Object>> enrichedApplicants = 
                    (List<Map<String, Object>>) coverageSummary.get("enrichedApplicants");
                    
                if (enrichedApplicants != null && !enrichedApplicants.isEmpty()) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    drawText(contentStream, "Covered Individuals (" + coverageSummary.get("applicantCount") + "):", margin, yPosition);
                    yPosition -= lineHeight * 1.5f;
                    
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    
                    for (Map<String, Object> applicant : enrichedApplicants) {
                        // Use pre-calculated display fields from enricher
                        String displayName = getString(applicant, "displayName");
                        String relationship = getString(applicant, "displayRelationship");
                        Integer age = (Integer) applicant.get("calculatedAge");
                        
                        if (displayName != null) {
                            String ageStr = age != null ? " (Age " + age + ")" : "";
                            drawText(contentStream, "• " + displayName + " - " + relationship + ageStr, margin + 20, yPosition);
                            yPosition -= lineHeight;
                        }
                        
                        // Products
                        List<Map<String, Object>> products = (List<Map<String, Object>>) applicant.get("products");
                        if (products != null && !products.isEmpty()) {
                            for (Map<String, Object> product : products) {
                                String planName = (String) product.get("planName");
                                Object premiumObj = product.get("premium");
                                String premium = premiumObj != null ? String.valueOf(premiumObj) : "0.00";
                                
                                if (planName != null) {
                                    drawText(contentStream, "  Coverage: " + planName + " ($" + premium + "/mo)", margin + 20, yPosition);
                                    yPosition -= lineHeight;
                                }
                            }
                        }
                        
                        yPosition -= lineHeight * 0.5f; // Space between applicants
                    }
                }

                // Coverage Details (enriched with benefit counts)
                yPosition -= lineHeight;
                List<Map<String, Object>> enrichedCoverages = 
                    (List<Map<String, Object>>) coverageSummary.get("enrichedCoverages");
                    
                if (enrichedCoverages != null && !enrichedCoverages.isEmpty()) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    Integer totalBenefits = (Integer) coverageSummary.get("totalBenefits");
                    drawText(contentStream, "Coverage Details (" + totalBenefits + " total benefits):", margin, yPosition);
                    yPosition -= lineHeight * 1.5f;
                    
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    
                    for (Map<String, Object> coverage : enrichedCoverages) {
                        String planName = (String) coverage.get("planName");
                        String carrierName = (String) coverage.get("carrierName");
                        Integer benefitCount = (Integer) coverage.get("benefitCount");
                        
                        if (planName != null) {
                            String benefitInfo = benefitCount != null ? " (" + benefitCount + " benefits)" : "";
                            drawText(contentStream, "• " + planName + benefitInfo, margin + 20, yPosition);
                            yPosition -= lineHeight;
                        }
                        
                        if (carrierName != null) {
                            drawText(contentStream, "  Carrier: " + carrierName, margin + 20, yPosition);
                            yPosition -= lineHeight;
                        }
                        
                        yPosition -= lineHeight * 0.5f;
                    }
                }
            } else {
                // Fallback: use original payload if enricher not configured
                drawText(contentStream, "Note: Configure 'coverageSummary' enricher for enhanced data", margin, yPosition);
            }
            
            // Footer
            yPosition = margin + 20;
            contentStream.setFont(PDType1Font.HELVETICA, 8);
            drawText(contentStream, "This is a summary of your coverage. Please review all policy documents for complete details.", margin, yPosition);
        }

        return document;
    }

    private void drawText(PDPageContentStream contentStream, String text, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
