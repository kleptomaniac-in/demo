package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Concatenates multiple strings together.
 * 
 * Usage: #{concat(firstName, ' ', lastName)}
 * Example: #{concat(firstName, ' ', middleName, ' ', lastName)} -> "John Michael Doe"
 */
public class ConcatFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                result.append(arg.toString());
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String getName() {
        return "concat";
    }
    
    @Override
    public String getDescription() {
        return "Concatenates multiple strings: concat(str1, str2, ...) -> 'str1str2...'";
    }
}
