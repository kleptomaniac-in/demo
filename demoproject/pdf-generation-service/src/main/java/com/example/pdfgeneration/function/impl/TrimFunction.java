package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Trims whitespace from both ends of a string.
 * 
 * Usage: #{trim(text)} -> trimmed text
 */
public class TrimFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        return args[0].toString().trim();
    }
    
    @Override
    public String getName() {
        return "trim";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Trims whitespace: trim('  text  ') -> 'text'";
    }
}
