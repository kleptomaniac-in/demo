package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Masks a string by showing only the last N characters.
 * 
 * Usage: #{mask(ssn, 'XXX-XX-', 4)} -> "XXX-XX-1234"
 * Usage: #{mask(creditCard, '****-****-****-', 4)} -> "****-****-****-5678"
 */
public class MaskFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 1) {
            return "";
        }
        
        String value = args[0] != null ? args[0].toString() : "";
        if (value.isEmpty()) {
            return "";
        }
        
        String maskPattern = args.length > 1 && args[1] != null ? args[1].toString() : "***";
        int visibleChars = args.length > 2 ? parseIntOrDefault(args[2], 4) : 4;
        
        if (value.length() <= visibleChars) {
            return value; // Don't mask if value is too short
        }
        
        String visiblePart = value.substring(value.length() - visibleChars);
        return maskPattern + visiblePart;
    }
    
    @Override
    public String getName() {
        return "mask";
    }
    
    @Override
    public String getDescription() {
        return "Masks a string showing only last N characters: mask(value, maskPattern, visibleCount) -> 'XXX-XX-1234'";
    }
    
    private int parseIntOrDefault(Object value, int defaultValue) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
