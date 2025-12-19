package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/**
 * Quick utility to verify PDF content
 */
public class PdfContentVerifier {
    
    public static void main(String[] args) throws IOException {
        String[] files = {
            "output/test-pdfs/dependent-addendum-6deps.pdf",
            "output/test-pdfs/coverage-addendum-5overflow.pdf",
            "output/test-pdfs/complete-enrollment-with-both-addendums.pdf"
        };
        
        for (String filePath : files) {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("❌ File not found: " + filePath);
                continue;
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("File: " + filePath);
            System.out.println("=".repeat(80));
            
            try (PDDocument document = PDDocument.load(file)) {
                System.out.println("Pages: " + document.getNumberOfPages());
                System.out.println("File size: " + file.length() + " bytes");
                
                // Extract text from first page
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(1);
                String text = stripper.getText(document);
                
                System.out.println("\nFirst page content:");
                System.out.println("-".repeat(80));
                if (text.trim().isEmpty()) {
                    System.out.println("⚠️  WARNING: NO TEXT FOUND ON FIRST PAGE!");
                } else {
                    System.out.println(text.substring(0, Math.min(500, text.length())));
                    if (text.length() > 500) {
                        System.out.println("... (truncated)");
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error reading PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
