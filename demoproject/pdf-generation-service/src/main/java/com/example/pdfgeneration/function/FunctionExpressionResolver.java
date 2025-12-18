package com.example.pdfgeneration.function;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves function expressions in field mappings.
 * 
 * Supports syntax like:
 * - #{concat(firstName, ' ', lastName)}
 * - #{mask(ssn, 'XXX-XX-', 5)}
 * - #{uppercase(email)}
 * 
 * Also supports nested payload references:
 * - #{concat(applicant.firstName, ' ', applicant.lastName)}
 */
@Component
public class FunctionExpressionResolver {
    
    private final FunctionRegistry functionRegistry;
    
    // Pattern to match function expressions: #{functionName(arg1, arg2, ...)}
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "#\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*)\\)\\s*\\}"
    );
    
    public FunctionExpressionResolver(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }
    
    /**
     * Check if a string contains a function expression.
     * 
     * @param expression The expression to check
     * @return true if it contains a function expression
     */
    public boolean isFunction(String expression) {
        if (expression == null) return false;
        return FUNCTION_PATTERN.matcher(expression.trim()).matches();
    }
    
    /**
     * Resolve a function expression against a payload.
     * 
     * @param expression The function expression (e.g., "#{concat(firstName, ' ', lastName)}")
     * @param payload The payload context for resolving field references
     * @return The resolved value
     * @throws IllegalArgumentException if the function is not found or arguments are invalid
     */
    public String resolve(String expression, Map<String, Object> payload) {
        if (expression == null || expression.trim().isEmpty()) {
            return expression;
        }
        
        String trimmed = expression.trim();
        Matcher matcher = FUNCTION_PATTERN.matcher(trimmed);
        
        if (!matcher.matches()) {
            // Not a function expression, return as-is or resolve as simple field reference
            return resolveFieldReference(trimmed, payload);
        }
        
        String functionName = matcher.group(1);
        String argsString = matcher.group(2);
        
        FieldTransformationFunction function = functionRegistry.get(functionName);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function: " + functionName);
        }
        
        Object[] args = parseArguments(argsString, payload);
        
        return function.apply(args, payload);
    }
    
    /**
     * Resolve multiple function expressions in a string.
     * Supports mixed content like: "Name: #{concat(firstName, lastName)}, Email: #{lowercase(email)}"
     * 
     * @param template The template string with function expressions
     * @param payload The payload context
     * @return The resolved string
     */
    public String resolveAll(String template, Map<String, Object> payload) {
        if (template == null || template.trim().isEmpty()) {
            return template;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = FUNCTION_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String resolved = resolve(fullMatch, payload);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Parse function arguments from the argument string.
     * 
     * @param argsString The arguments string (e.g., "firstName, ' ', lastName")
     * @param payload The payload for resolving field references
     * @return Array of resolved argument values
     */
    private Object[] parseArguments(String argsString, Map<String, Object> payload) {
        if (argsString == null || argsString.trim().isEmpty()) {
            return new Object[0];
        }
        
        List<Object> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\0';
        int parenthesesDepth = 0;
        
        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);
            
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
                continue;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                continue;
            } else if (!inQuotes && c == '(') {
                parenthesesDepth++;
            } else if (!inQuotes && c == ')') {
                parenthesesDepth--;
            } else if (!inQuotes && c == ',' && parenthesesDepth == 0) {
                args.add(resolveArgument(currentArg.toString().trim(), payload));
                currentArg = new StringBuilder();
                continue;
            }
            
            currentArg.append(c);
        }
        
        // Add the last argument
        if (currentArg.length() > 0) {
            args.add(resolveArgument(currentArg.toString().trim(), payload));
        }
        
        return args.toArray();
    }
    
    /**
     * Resolve a single argument (could be a literal, field reference, or nested function).
     * 
     * @param arg The argument string
     * @param payload The payload context
     * @return The resolved value
     */
    private Object resolveArgument(String arg, Map<String, Object> payload) {
        if (arg == null || arg.isEmpty()) {
            return "";
        }
        
        // Check if it's a nested function
        if (isFunction(arg)) {
            return resolve(arg, payload);
        }
        
        // Check if it's a string literal (already handled in parsing, but keep for safety)
        if ((arg.startsWith("\"") && arg.endsWith("\"")) || 
            (arg.startsWith("'") && arg.endsWith("'"))) {
            return arg.substring(1, arg.length() - 1);
        }
        
        // Check if it's a number
        try {
            if (arg.contains(".")) {
                return Double.parseDouble(arg);
            } else {
                return Integer.parseInt(arg);
            }
        } catch (NumberFormatException e) {
            // Not a number, continue
        }
        
        // Check if it's a boolean
        if ("true".equalsIgnoreCase(arg) || "false".equalsIgnoreCase(arg)) {
            return Boolean.parseBoolean(arg);
        }
        
        // Otherwise, treat as a field reference
        return resolveFieldReference(arg, payload);
    }
    
    /**
     * Resolve a field reference from the payload (supports dot notation).
     * 
     * @param fieldPath The field path (e.g., "applicant.firstName")
     * @param payload The payload context
     * @return The field value, or empty string if not found
     */
    private String resolveFieldReference(String fieldPath, Map<String, Object> payload) {
        if (fieldPath == null || fieldPath.isEmpty() || payload == null) {
            return "";
        }
        
        String[] parts = fieldPath.split("\\.");
        Object current = payload;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return "";
            }
            
            if (current == null) {
                return "";
            }
        }
        
        return current.toString();
    }
}
