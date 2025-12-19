package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Returns the first non-null, non-empty value.
 * 
 * Usage: #{coalesce(value1, value2, 'default')} -> first non-empty value
 */
public class CoalesceFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        for (Object arg : args) {
            if (arg != null) {
                String value = arg.toString();
                if (!value.trim().isEmpty()) {
                    return value;
                }
            }
        }
        
        return "";
    }
    
    @Override
    public String getName() {
        return "coalesce";
    }
    
    @Override
    public String getDescription() {
        return "Returns first non-empty value: coalesce(null, '', 'default') -> 'default'";
    }
}
