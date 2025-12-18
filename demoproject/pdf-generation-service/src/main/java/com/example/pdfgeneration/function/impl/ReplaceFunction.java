package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Replaces all occurrences of a substring.
 * 
 * Usage: #{replace(text, 'old', 'new')} -> text with replacements
 */
public class ReplaceFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 3 || args[0] == null) {
            return args != null && args.length > 0 && args[0] != null ? args[0].toString() : "";
        }
        
        String value = args[0].toString();
        String oldStr = args[1] != null ? args[1].toString() : "";
        String newStr = args[2] != null ? args[2].toString() : "";
        
        return value.replace(oldStr, newStr);
    }
    
    @Override
    public String getName() {
        return "replace";
    }
    
    @Override
    public String getDescription() {
        return "Replaces substring: replace(text, 'old', 'new') -> text with 'old' replaced by 'new'";
    }
}
