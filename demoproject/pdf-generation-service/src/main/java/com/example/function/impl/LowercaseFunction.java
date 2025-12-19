package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Converts a string to lowercase.
 * 
 * Usage: #{lowercase(email)} -> "john@example.com"
 */
public class LowercaseFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        return args[0].toString().toLowerCase();
    }
    
    @Override
    public String getName() {
        return "lowercase";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Converts string to lowercase: lowercase(TEXT) -> 'text'";
    }
}
