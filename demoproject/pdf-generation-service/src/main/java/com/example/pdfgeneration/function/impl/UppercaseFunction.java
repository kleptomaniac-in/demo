package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Converts a string to uppercase.
 * 
 * Usage: #{uppercase(email)} -> "JOHN@EXAMPLE.COM"
 */
public class UppercaseFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        return args[0].toString().toUpperCase();
    }
    
    @Override
    public String getName() {
        return "uppercase";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Converts string to uppercase: uppercase(text) -> 'TEXT'";
    }
}
