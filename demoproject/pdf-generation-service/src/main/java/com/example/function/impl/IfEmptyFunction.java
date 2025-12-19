package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Returns a replacement value if the input is empty.
 * 
 * Usage: #{ifEmpty(value, 'Not Provided')} -> value or 'Not Provided'
 */
public class IfEmptyFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 2) {
            return "";
        }
        
        String value = args[0] != null ? args[0].toString().trim() : "";
        String replacement = args[1] != null ? args[1].toString() : "";
        
        return value.isEmpty() ? replacement : value;
    }
    
    @Override
    public String getName() {
        return "ifEmpty";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 2;
    }
    
    @Override
    public String getDescription() {
        return "Returns replacement if empty: ifEmpty('', 'Not Provided') -> 'Not Provided'";
    }
}
