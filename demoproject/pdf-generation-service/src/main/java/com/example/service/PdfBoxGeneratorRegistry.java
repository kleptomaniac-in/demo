package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry to manage PDFBox generators.
 * Auto-discovers all PdfBoxGenerator implementations.
 */
@Component
public class PdfBoxGeneratorRegistry {
    
    private final Map<String, PdfBoxGenerator> generators = new HashMap<>();
    
    @Autowired(required = false)
    public PdfBoxGeneratorRegistry(List<PdfBoxGenerator> generatorList) {
        if (generatorList != null) {
            for (PdfBoxGenerator generator : generatorList) {
                generators.put(generator.getName(), generator);
            }
        }
    }
    
    public PdfBoxGenerator getGenerator(String name) {
        PdfBoxGenerator generator = generators.get(name);
        if (generator == null) {
            throw new IllegalArgumentException("No PDFBox generator found with name: " + name);
        }
        return generator;
    }
    
    public boolean hasGenerator(String name) {
        return generators.containsKey(name);
    }
}
