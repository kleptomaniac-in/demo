package com.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for PDFBox-based page generators.
 * Implement this for each legacy PDFBox generator you want to integrate.
 */
public interface PdfBoxGenerator {
    PDDocument generate(Map<String, Object> payload) throws IOException;
    String getName();
}
