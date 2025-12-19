package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for merging multiple PDF documents into a single PDF.
 * Used to combine the main enrollment form with dependent addendum pages.
 */
@Service
public class PdfMergerService {
    
    /**
     * Merge multiple PDF byte arrays into a single PDF.
     * 
     * @param pdfDocuments List of PDF documents as byte arrays
     * @return Merged PDF as byte array
     * @throws IOException if merging fails
     */
    public byte[] mergePdfs(List<byte[]> pdfDocuments) throws IOException {
        if (pdfDocuments == null || pdfDocuments.isEmpty()) {
            return new byte[0];
        }
        
        // If only one document, return it as-is
        if (pdfDocuments.size() == 1) {
            return pdfDocuments.get(0);
        }
        
        // Load all source documents first (need to keep them open during merge)
        List<PDDocument> sourceDocuments = new ArrayList<>();
        try {
            // Load all documents
            for (byte[] pdfBytes : pdfDocuments) {
                if (pdfBytes != null && pdfBytes.length > 0) {
                    sourceDocuments.add(PDDocument.load(new ByteArrayInputStream(pdfBytes)));
                }
            }
            
            // Merge all documents
            try (PDDocument mergedDocument = new PDDocument()) {
                for (PDDocument sourceDoc : sourceDocuments) {
                    // Add all pages from source document to merged document
                    for (int i = 0; i < sourceDoc.getNumberOfPages(); i++) {
                        mergedDocument.importPage(sourceDoc.getPage(i));
                    }
                }
                
                // Convert to byte array
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                mergedDocument.save(outputStream);
                return outputStream.toByteArray();
            }
        } finally {
            // Close all source documents
            for (PDDocument doc : sourceDocuments) {
                if (doc != null) {
                    doc.close();
                }
            }
        }
    }
    
    /**
     * Merge main enrollment form with dependent addendum.
     * 
     * @param mainForm Main enrollment AcroForm PDF
     * @param addendum Dependent addendum PDF (can be empty)
     * @return Merged PDF with main form + addendum
     * @throws IOException if merging fails
     */
    public byte[] mergeEnrollmentWithAddendum(byte[] mainForm, byte[] addendum) throws IOException {
        List<byte[]> documents = new ArrayList<>();
        documents.add(mainForm);
        
        if (addendum != null && addendum.length > 0) {
            documents.add(addendum);
        }
        
        return mergePdfs(documents);
    }
}
