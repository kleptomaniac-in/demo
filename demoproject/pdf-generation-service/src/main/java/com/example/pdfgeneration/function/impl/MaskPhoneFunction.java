package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.util.Map;

/**
 * Masks a phone number showing only last 4 digits.
 * 
 * Usage: #{maskPhone(phone)} -> "XXX-XXX-1234"
 */
public class MaskPhoneFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        String phone = args[0].toString();
        // Extract digits only
        String digits = phone.replaceAll("[^0-9]", "");
        
        if (digits.length() < 4) {
            return phone; // Too short to mask
        }
        
        String lastFour = digits.substring(digits.length() - 4);
        return "XXX-XXX-" + lastFour;
    }
    
    @Override
    public String getName() {
        return "maskPhone";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Masks phone number: maskPhone('555-123-4567') -> 'XXX-XXX-4567'";
    }
}
