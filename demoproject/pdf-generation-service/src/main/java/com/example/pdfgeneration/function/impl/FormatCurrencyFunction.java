package com.example.pdfgeneration.function.impl;

import com.example.pdfgeneration.function.FieldTransformationFunction;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Formats a number as currency with $ symbol.
 * 
 * Usage: #{formatCurrency(amount)} -> "$1,234.50"
 */
public class FormatCurrencyFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "$0.00";
        }
        
        try {
            double value = parseDouble(args[0]);
            DecimalFormat df = new DecimalFormat("$#,##0.00");
            return df.format(value);
            
        } catch (Exception e) {
            return args[0].toString();
        }
    }
    
    @Override
    public String getName() {
        return "formatCurrency";
    }
    
    @Override
    public int getExpectedArgCount() {
        return 1;
    }
    
    @Override
    public String getDescription() {
        return "Formats as currency: formatCurrency(1234.5) -> '$1,234.50'";
    }
    
    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString().replace(",", "").replace("$", ""));
    }
}
