package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Returns a default value if the input is null or empty.
 * 
 * Usage: #{default(value, 'N/A')} -> value or 'N/A' if empty
 */
public class DefaultFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 2) {
            return "";
        }
        
        String value = args[0] != null ? args[0].toString() : "";
        String defaultValue = args[1] != null ? args[1].toString() : "";
        
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
    
    @Override
    public String getName() {
        return "default";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 2;
    }
    
    @Override
    public String getDescription() {
        return "Returns default if empty: default(value, 'N/A') -> value or 'N/A'";
    }
}
