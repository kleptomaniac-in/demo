package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Masks an email address showing only first char and domain.
 * 
 * Usage: #{maskEmail(email)} -> "j***@example.com"
 */
public class MaskEmailFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        String email = args[0].toString();
        if (!email.contains("@")) {
            return email; // Not a valid email, return as-is
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 1) {
            return email; // Too short to mask
        }
        
        String maskedLocal = localPart.charAt(0) + "***";
        return maskedLocal + "@" + domain;
    }
    
    @Override
    public String getName() {
        return "maskEmail";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Masks email address: maskEmail('john@example.com') -> 'j***@example.com'";
    }
}
