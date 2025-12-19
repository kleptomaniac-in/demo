package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Parses a date string and returns it in ISO format.
 * 
 * Usage: #{parseDate(date, 'MM/dd/yyyy')} -> "2026-01-15"
 */
public class ParseDateFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 1 || args[0] == null) {
            return "";
        }
        
        try {
            String dateStr = args[0].toString();
            String inputFormat = args.length > 1 ? args[1].toString() : null;
            
            Date date;
            if (inputFormat != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(inputFormat);
                date = sdf.parse(dateStr);
            } else {
                // Try common formats
                date = parseCommonFormats(dateStr);
            }
            
            SimpleDateFormat outputSdf = new SimpleDateFormat("yyyy-MM-dd");
            return outputSdf.format(date);
            
        } catch (Exception e) {
            return args[0].toString();
        }
    }
    
    @Override
    public String getName() {
        return "parseDate";
    }
    
    @Override
    public String getDescription() {
        return "Parses date to ISO format: parseDate('01/15/2026', 'MM/dd/yyyy') -> '2026-01-15'";
    }
    
    private Date parseCommonFormats(String dateStr) throws Exception {
        String[] formats = {
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "MMM dd, yyyy"
        };
        
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(dateStr);
            } catch (Exception e) {
                // Try next format
            }
        }
        
        throw new Exception("Unable to parse date: " + dateStr);
    }
}
