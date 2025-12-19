Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/pdf/controller/EnrollmentPdfController.java#40-40), lines 40 to 75

Searched for files matching `**/ConfigSelectionService.java`, 1 match

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/ConfigSelectionService.java)

Now let me check what configs exist in the config-repo: 

Ran terminal command: cd /workspaces/demo/demoproject/config-repo && ls -1 dental*.yml medical*.yml 2>/dev/null | head -20

Perfect! Now let me check one of these config files to show the user how they work: 

Read [](file:///workspaces/demo/demoproject/config-repo/dental-medical-individual-ca.yml)

Now let me create a comprehensive explanation document:

Yes! **The config YAML is determined dynamically** based on `marketCategory`, `state`, and `products`. Here's how it works:

## Dynamic Config Selection

### **Convention-Based Selection** (Default)

The `ConfigSelectionService.selectConfigByConvention()` builds the filename from your enrollment parameters:

**Formula:**
```
{sorted-products}-{market}-{state}.yml
```

**Examples from your curl tests:**

| Test Input | Generated Config Name |
|------------|----------------------|
| `products: ["DENTAL"]`<br>`marketCategory: "INDIVIDUAL"`<br>`state: "CA"` | **`dental-individual-ca.yml`** |
| `products: ["DENTAL", "MEDICAL", "VISION"]`<br>`marketCategory: "INDIVIDUAL"`<br>`state: "CA"` | **`dental-medical-vision-individual-ca.yml`** |
| `products: ["DENTAL", "MEDICAL"]`<br>`marketCategory: "SMALL_GROUP"`<br>`state: "TX"` | **`dental-medical-small-group-tx.yml`** |
| `products: ["MEDICAL"]`<br>`marketCategory: "INDIVIDUAL"`<br>`state: "CA"` | **`medical-individual-ca.yml`** |

### **Available Config Files**

Your `config-repo` already has these configs:
```
dental-individual-ca.yml
dental-medical-individual-ca.yml
dental-medical-vision-individual-ca.yml
dental-medical-vision-small-group-tx.yml
dental-small-group-tx.yml
medical-individual-ca.yml
medical-individual-family-ca.yml
...and more
```

### **How It Works - Step by Step**

1. **Request comes in** with enrollment metadata:
   ```json
   {
     "enrollment": {
       "state": "CA",
       "marketCategory": "INDIVIDUAL", 
       "products": ["DENTAL", "MEDICAL"]
     }
   }
   ```

2. **ConfigSelectionService processes it:**
   ```java
   // Sort products alphabetically
   products = ["dental", "medical"] // sorted
   
   // Build config name
   configName = "dental-medical-individual-ca.yml"
   ```

3. **Config is loaded** from dental-medical-individual-ca.yml

4. **Config defines composition:**
   ```yaml
   composition:
     base: templates/base-payer.yml
     components:
       - templates/products/dental.yml
       - templates/products/medical.yml
       - templates/markets/individual.yml
       - templates/states/california.yml
   ```

5. **PDF is generated** using the composed configuration

### **Alternative Selection Strategies**

The service supports 4 strategies:

**Strategy 1: Convention-Based** (Currently Used)
- Automatic based on products + market + state
- Most common approach

**Strategy 2: Dynamic Composition**
- Builds composition on-the-fly
- No predefined YAML needed

**Strategy 3: Fallback Chain**
- Tries specific config first
- Falls back to more general configs if not found

**Strategy 4: Rule-Based**
- Uses business logic for complex scenarios
- Example: Medicare-specific templates, Large group customizations

### **To Use a Different Config**

You have 3 options:

**Option 1: Create matching config file**
```bash
# If test uses: CA, INDIVIDUAL, ["MEDICAL", "VISION"]
# Create: config-repo/medical-vision-individual-ca.yml
```

**Option 2: Use explicit config endpoint** (if implemented)
```bash
curl -X POST http://localhost:8080/api/enrollment/generate-with-config \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "dental-individual-ca.yml",
    "payload": {...}
  }'
```

**Option 3: Modify enrollment metadata to match existing config**
```json
{
  "enrollment": {
    "state": "CA",
    "marketCategory": "INDIVIDUAL",
    "products": ["DENTAL"]  // Matches dental-individual-ca.yml
  }
}
```

### **What Addendums Need**

The addendum generation (DependentAddendumService, CoverageAddendumService) happens **automatically** regardless of which config is selected. The addendums are generated when:

- **Dependent Addendum:** More than 3 dependents in payload
- **Coverage Addendum:** Any applicant has more than 1 coverage

These are merged with the main form PDF automatically by the `EnrollmentPdfService`.

**Summary:** Yes, it's fully dynamic! Just set `state`, `marketCategory`, and `products` in your curl request, and the appropriate config YAML will be selected automatically.