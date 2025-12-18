package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Extracts a substring from a string.
 * 
 * Usage: #{substring(text, start, end)} -> substring
 * Usage: #{substring(text, start)} -> substring from start to end
 */
public class SubstringFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 2 || args[0] == null) {
            return "";
        }
        
        String value = args[0].toString();
        int start = parseIntOrDefault(args[1], 0);
        
        if (start >= value.length()) {
            return "";
        }
        
        if (args.length > 2) {
            int end = parseIntOrDefault(args[2], value.length());
            end = Math.min(end, value.length());
            return value.substring(start, end);
        } else {
            return value.substring(start);
        }
    }
    
    @Override
    public String getName() {
        return "substring";
    }
    
    @Override
    public String getDescription() {
        return "Extracts substring: substring(text, start, end) or substring(text, start)";
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
