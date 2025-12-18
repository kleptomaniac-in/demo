package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Capitalizes the first letter of each word.
 * 
 * Usage: #{capitalize(name)} -> "John Doe"
 */
public class CapitalizeFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        String value = args[0].toString();
        if (value.isEmpty()) {
            return "";
        }
        
        String[] words = value.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String getName() {
        return "capitalize";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Capitalizes first letter of each word: capitalize('john doe') -> 'John Doe'";
    }
}
