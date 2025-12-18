package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Formats a number with specified decimal places.
 * 
 * Usage: #{formatNumber(value, 2)} -> "123.45"
 */
public class FormatNumberFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        
        try {
            double value = parseDouble(args[0]);
            int decimalPlaces = args.length > 1 ? parseIntOrDefault(args[1], 2) : 2;
            
            StringBuilder pattern = new StringBuilder("#,##0");
            if (decimalPlaces > 0) {
                pattern.append(".");
                for (int i = 0; i < decimalPlaces; i++) {
                    pattern.append("0");
                }
            }
            
            DecimalFormat df = new DecimalFormat(pattern.toString());
            return df.format(value);
            
        } catch (Exception e) {
            return args[0].toString();
        }
    }
    
    @Override
    public String getName() {
        return "formatNumber";
    }
    
    @Override
    public String getDescription() {
        return "Formats number: formatNumber(1234.5, 2) -> '1,234.50'";
    }
    
    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString().replace(",", ""));
    }
    
    private int parseIntOrDefault(Object value, int defaultValue) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
