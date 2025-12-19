Searched for regex `startsWith\(|toUpperCase|toLowerCase|concat|mask` (`**/AcroFormFillService.java`), 2 results

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/AcroFormFillService.java#250-250), lines 250 to 350

Searched for regex `applyFunction|function:|fn:` (`**/AcroFormFillService.java`), no results

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/AcroFormFillService.java#100-100), lines 100 to 150

Searched codebase for "function toUpperCase toLowerCase concat mask concatenate case conversion functions field mapping", 23 results

Great question! Let me check how functions are processed and where caching happens: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/AcroFormFillService.java#150-150), lines 150 to 200

Searched for regex `processFunctionExpression|FunctionEvaluator|#\{` (`**/AcroFormFillService.java`), no results

Searched for regex `FunctionEvaluator|applyFunctions|processFunction` (`**/FlexiblePdfMergeService.java`), no results

Searched codebase for "function evaluator transformation functions apply execute process fieldMapping", 13 results

Perfect! Now let me check how functions integrate with the field value resolution and caching: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/FlexiblePdfMergeService.java#45-45), lines 45 to 100

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/function/FunctionExpressionResolver.java#1-1), lines 1 to 100

Now let me check where functions are called in relation to the caching layer: 

Searched for regex `FunctionExpressionResolver|isFunction|resolve\(` (`**/AcroFormFillService.java`), 5 results

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/AcroFormFillService.java#70-70), lines 70 to 100

Perfect! Now I can give you the complete answer:

## Function Expressions and Caching

**Answer:** Functions themselves **bypass path caching**, but the **arguments inside functions still benefit from caching**.

Here's how it works:

### 1. **Function Detection Happens First**
```java
// Line 85-88 in AcroFormFillService
if (functionResolver.isFunction(payloadPath)) {
    // Functions are resolved directly
    String resolvedValue = functionResolver.resolve(payloadPath, payload);
    value = resolvedValue;
} else {
    // Plain paths use cached resolution
    value = resolveValue(payload, payloadPath);
}
```

### 2. **Path Caching vs Function Resolution** 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/function/FunctionExpressionResolver.java#100-100), lines 100 to 150

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/function/FunctionExpressionResolver.java#150-150), lines 150 to 200

## The Complete Picture: Functions + Caching

### **Caching Behavior:**

| Mapping Type | Example | Path Cache | Filter Cache |
|--------------|---------|------------|--------------|
| **Plain path** | `"applicants[0].firstName"` | ✅ **YES** | ✅ **YES** |
| **Function with literals** | `"#{uppercase('hello')}"` | ❌ No | ❌ No |
| **Function with field refs** | `"#{uppercase(applicants[0].firstName)}"` | ❌ Function itself not cached | ✅ **YES** (for the field argument) |
| **Function with filter** | `"#{mask(applicants[relationship=PRIMARY].ssn, '***-**-', 4)}"` | ❌ Function itself not cached | ✅ **YES** (for filtered array) |

### **Key Insights:**

#### 1. **Function Results Are NOT Cached**
```yaml
fieldMappings:
  "Field1": "#{uppercase(applicants[0].firstName)}"  # Executes uppercase() every time
  "Field2": "#{uppercase(applicants[0].firstName)}"  # Executes uppercase() again
```

The `uppercase()` function runs twice, but the underlying field resolution (`applicants[0].firstName`) **IS cached**.

#### 2. **Field Arguments ARE Cached**
```yaml
fieldMappings:
  "Field1": "#{concat(applicants[relationship=PRIMARY].firstName, ' ', applicants[relationship=PRIMARY].lastName)}"
  "Field2": "#{uppercase(applicants[relationship=PRIMARY].firstName)}"
```

- `applicants[relationship=PRIMARY]` filter: **Cached once** (88.6% reduction)
- `.firstName` access: **Cached**
- `.lastName` access: **Cached**
- But `concat()` and `uppercase()` **execute on each field**

#### 3. **Best Practice for Repeated Function Results**
```yaml
# ❌ Inefficient - function runs 3 times
"Field1": "#{uppercase(#{concat(firstName, ' ', lastName)})}"
"Field2": "#{uppercase(#{concat(firstName, ' ', lastName)})}"
"Field3": "#{uppercase(#{concat(firstName, ' ', lastName)})}"

# ✅ Better - Use payload enricher
enrichers:
  - fullNameEnricher  # Calculates once: enrichedData.fullNameUpper

fieldMappings:
  "Field1": "enrichedData.fullNameUpper"  # Cached path resolution
  "Field2": "enrichedData.fullNameUpper"  # Retrieved from cache
  "Field3": "enrichedData.fullNameUpper"  # Retrieved from cache
```

### **Summary:**

- **Function expressions** bypass path caching (they execute every time)
- **Arguments inside functions** benefit from both path and filter caching
- For repeated transformations, use **payload enrichers** to pre-calculate and cache results
- Simple formatting functions (uppercase, mask) execute quickly even without caching
- Complex functions or nested functions → Consider enrichers for performance

The caching optimizations still provide **significant value** even with functions because:
1. Field resolution inside function arguments is cached
2. Filter operations are cached (88.6% reduction)
3. Enrichers can pre-compute expensive function results