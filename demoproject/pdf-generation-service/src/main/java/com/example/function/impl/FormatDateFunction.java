package com.example.function.impl;

import com.example.function.FieldTransformationFunction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Formats a date string to a specified format.
 * 
 * Usage: #{formatDate(date, 'yyyy-MM-dd', 'MMM dd, yyyy')} -> "Jan 15, 2026"
 * Usage: #{formatDate(date, 'MMM dd, yyyy')} -> formats assuming ISO date input
 */
public class FormatDateFunction implements FieldTransformationFunction {
    
    @Override
    public String apply(Object[] args, Map<String, Object> payload) {
        if (args == null || args.length < 2 || args[0] == null) {
            return "";
        }
        
        try {
            String dateStr = args[0].toString();
            String outputFormat = args[1].toString();
            
            // Try to parse the date
            Date date;
            if (args.length > 2 && args[2] != null) {
                // Custom input format provided
                String inputFormat = args[2].toString();
                SimpleDateFormat inputSdf = new SimpleDateFormat(inputFormat);
                date = inputSdf.parse(dateStr);
            } else {
                // Try common formats
                date = parseCommonFormats(dateStr);
            }
            
            SimpleDateFormat outputSdf = new SimpleDateFormat(outputFormat);
            return outputSdf.format(date);
            
        } catch (Exception e) {
            return args[0].toString(); // Return original if parsing fails
        }
    }
    
    @Override
    public String getName() {
        return "formatDate";
    }
    
    @Override
    public String getDescription() {
        return "Formats date: formatDate(date, 'MMM dd, yyyy') -> 'Jan 15, 2026'";
    }
    
    private Date parseCommonFormats(String dateStr) throws Exception {
        String[] formats = {
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy-MM-dd'T'HH:mm:ss",
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
