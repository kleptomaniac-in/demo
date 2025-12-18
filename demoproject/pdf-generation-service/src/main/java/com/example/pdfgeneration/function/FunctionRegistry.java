package com.example.pdfgeneration.function;

import com.example.pdfgeneration.function.impl.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for field transformation functions.
 * 
 * Manages registration and lookup of functions that can be used in
 * YAML configurations and template field mappings.
 */
@Component
public class FunctionRegistry {
    
    private final Map<String, FieldTransformationFunction> functions = new ConcurrentHashMap<>();
    
    public FunctionRegistry() {
        // Register built-in functions
        registerDefaultFunctions();
    }
    
    /**
     * Register a function by name.
     * 
     * @param name The function name
     * @param function The function implementation
     */
    public void register(String name, FieldTransformationFunction function) {
        functions.put(name.toLowerCase(), function);
    }
    
    /**
     * Register a function using its default name.
     * 
     * @param function The function implementation
     */
    public void register(FieldTransformationFunction function) {
        register(function.getName(), function);
    }
    
    /**
     * Get a function by name.
     * 
     * @param name The function name
     * @return The function, or null if not found
     */
    public FieldTransformationFunction get(String name) {
        return functions.get(name.toLowerCase());
    }
    
    /**
     * Check if a function is registered.
     * 
     * @param name The function name
     * @return true if the function exists
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name.toLowerCase());
    }
    
    /**
     * Get all registered function names.
     * 
     * @return Set of function names
     */
    public Set<String> getFunctionNames() {
        return new HashSet<>(functions.keySet());
    }
    
    /**
     * Get all registered functions with their descriptions.
     * 
     * @return Map of function name to description
     */
    public Map<String, String> getFunctionDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        functions.forEach((name, func) -> descriptions.put(name, func.getDescription()));
        return descriptions;
    }
    
    private void registerDefaultFunctions() {
        // String operations
        register(new ConcatFunction());
        register(new UppercaseFunction());
        register(new LowercaseFunction());
        register(new CapitalizeFunction());
        register(new SubstringFunction());
        register(new ReplaceFunction());
        register(new TrimFunction());
        
        // Masking and security
        register(new MaskFunction());
        register(new MaskEmailFunction());
        register(new MaskPhoneFunction());
        
        // Date formatting
        register(new FormatDateFunction());
        register(new ParseDateFunction());
        
        // Numeric operations
        register(new FormatNumberFunction());
        register(new FormatCurrencyFunction());
        
        // Conditional operations
        register(new CoalesceFunction());
        register(new DefaultFunction());
        register(new IfEmptyFunction());
    }
}
