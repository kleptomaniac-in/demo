package com.example.function;

import java.util.Map;

/**
 * Interface for field transformation functions that can be used in YAML configurations
 * and template field mappings.
 * 
 * Functions provide reusable operations like concatenation, masking, case conversion, etc.
 * that can be applied across different templates and configurations.
 * 
 * Example usage in YAML:
 * <pre>
 * fieldMappings:
 *   FullName: "#{concat(firstName, ' ', lastName)}"
 *   SSN: "#{mask(ssn, 'XXX-XX-', 5)}"
 *   Email: "#{lowercase(email)}"
 * </pre>
 */
@FunctionalInterface
public interface FieldTransformationFunction {
    
    /**
     * Apply the transformation function.
     * 
     * @param args Array of arguments passed to the function
     * @param payload The complete payload context for accessing nested fields
     * @return The transformed value as a String
     * @throws IllegalArgumentException if the arguments are invalid
     */
    String apply(Object[] args, Map<String, Object> payload);
    
    /**
     * Get the name of this function (used for registration).
     * 
     * @return The function name (e.g., "concat", "mask", "uppercase")
     */
    default String getName() {
        return this.getClass().getSimpleName().replace("Function", "").toLowerCase();
    }
    
    /**
     * Get the expected number of arguments (or -1 for variable args).
     * 
     * @return Expected argument count, or -1 for variable arguments
     */
    default int getExpectedArgCount() {
        return -1; // Variable by default
    }
    
    /**
     * Get a description of this function.
     * 
     * @return Function description
     */
    default String getDescription() {
        return "Transformation function: " + getName();
    }
}
