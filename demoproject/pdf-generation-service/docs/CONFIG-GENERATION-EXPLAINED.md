# Config Generation: Pre-Generated vs Dynamic - Explained

## The Two Approaches

---

## Approach 1: Pre-Generated Config Files (Current Implementation)

### What Files Exist on Disk

```
config-repo/
├── templates/                          # 22 COMPONENT FILES
│   ├── base-payer.yml                 # 1 base
│   ├── products/
│   │   ├── medical.yml                # 3 products
│   │   ├── dental.yml
│   │   └── vision.yml
│   ├── markets/
│   │   ├── individual.yml             # 3 markets
│   │   ├── medicare.yml
│   │   └── small-group.yml
│   └── states/
│       ├── california.yml             # 15 states
│       ├── texas.yml
│       └── ... (13 more states)
│
├── medical-individual-ca.yml          # TOP-LEVEL CONFIG FILES
├── dental-medical-individual-ca.yml   # (up to 315 files)
├── medical-medicare-tx.yml
└── ... (more pre-generated configs)
```

### Top-Level Config File Content (Minimal)

**File:** `dental-medical-individual-ca.yml`

```yaml
# This is a TINY file - just references components
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california.yml
```

**That's it! Just 6 lines referencing other files.**

### How It Works at Runtime

```
1. API Request:
   POST /api/enrollment/generate
   {
     "enrollment": {
       "products": ["medical", "dental"],
       "marketCategory": "individual",
       "state": "CA"
     }
   }

2. ConfigSelectionService:
   → Builds config name: "dental-medical-individual-ca.yml"

3. PdfMergeConfigService.loadConfig("dental-medical-individual-ca.yml"):
   → Reads file from disk: config-repo/dental-medical-individual-ca.yml
   → Sees "composition" key
   → Loads base: templates/base-payer.yml
   → Loads component: templates/products/dental.yml
   → Loads component: templates/products/medical.yml
   → Loads component: templates/markets/individual.yml
   → Loads component: templates/states/california.yml
   → Merges all sections together
   → Returns final merged config

4. FlexiblePdfMergeService:
   → Uses merged config to generate PDF
```

### Advantages
✅ Explicit - you can see exactly which configs are supported
✅ Can add custom overrides in top-level files
✅ Easy to understand for new developers

### Disadvantages
❌ Still need to create 315 tiny files (though they're minimal)
❌ Adding new combination = create new file

---

## Approach 2: Fully Dynamic (No Top-Level Files)

### What Files Exist on Disk

```
config-repo/
└── templates/                          # ONLY 22 COMPONENT FILES
    ├── base-payer.yml                 # 1 base
    ├── products/
    │   ├── medical.yml                # 3 products
    │   ├── dental.yml
    │   └── vision.yml
    ├── markets/
    │   ├── individual.yml             # 3 markets
    │   ├── medicare.yml
    │   └── small-group.yml
    └── states/
        ├── california.yml             # 15 states
        ├── texas.yml
        └── ... (13 more states)

# NO top-level config files!
```

### Modified Service Implementation

**New Method in PdfMergeConfigService:**

```java
/**
 * Load config dynamically without requiring a physical file
 */
public PdfMergeConfig loadConfigDynamic(EnrollmentSubmission enrollment) {
    try {
        // Build composition structure in memory
        Map<String, Object> composition = new HashMap<>();
        composition.put("base", "templates/base-payer.yml");
        
        List<String> components = new ArrayList<>();
        
        // Add product components
        for (String product : enrollment.getProducts()) {
            components.add("templates/products/" + product.toLowerCase() + ".yml");
        }
        
        // Add market component
        components.add("templates/markets/" + enrollment.getMarketCategory().toLowerCase() + ".yml");
        
        // Add state component
        components.add("templates/states/" + enrollment.getState().toLowerCase() + ".yml");
        
        composition.put("components", components);
        
        // Load composed config (same logic as before, but composition built in memory)
        return loadComposedConfig(composition, new HashMap<>());
        
    } catch (Exception e) {
        throw new RuntimeException("Failed to load dynamic config", e);
    }
}
```

**Modified Controller:**

```java
@PostMapping("/generate")
public ResponseEntity<byte[]> generateDynamic(@RequestBody EnrollmentPdfRequest request) {
    try {
        // Option 1: Generate config name and try to load file
        String configName = configSelectionService.selectConfigByConvention(request.getEnrollment());
        
        PdfMergeConfig config;
        try {
            // Try to load pre-generated file first
            config = pdfMergeConfigService.loadConfig(configName);
        } catch (Exception e) {
            // File doesn't exist - generate dynamically
            config = pdfMergeConfigService.loadConfigDynamic(request.getEnrollment());
        }
        
        byte[] pdf = pdfMergeService.generateMergedPdf(config, request.getPayload());
        
        // ... return PDF
        
    } catch (Exception e) {
        // ... error handling
    }
}
```

### How It Works at Runtime

```
1. API Request:
   POST /api/enrollment/generate
   {
     "enrollment": {
       "products": ["medical", "dental"],
       "marketCategory": "individual",
       "state": "CA"
     }
   }

2. ConfigSelectionService:
   → Builds config name: "dental-medical-individual-ca.yml"

3. Controller tries loadConfig("dental-medical-individual-ca.yml"):
   → File doesn't exist on disk
   → Catches exception
   → Calls loadConfigDynamic(enrollment)

4. loadConfigDynamic(enrollment):
   → Builds composition in memory:
     {
       base: "templates/base-payer.yml",
       components: [
         "templates/products/dental.yml",
         "templates/products/medical.yml",
         "templates/markets/individual.yml",
         "templates/states/california.yml"
       ]
     }
   → Loads base: templates/base-payer.yml
   → Loads component: templates/products/dental.yml
   → Loads component: templates/products/medical.yml
   → Loads component: templates/markets/individual.yml
   → Loads component: templates/states/california.yml
   → Merges all sections together
   → Returns final merged config

5. FlexiblePdfMergeService:
   → Uses merged config to generate PDF
```

### Advantages
✅ Zero top-level config files needed
✅ All 315 combinations automatically supported
✅ Add new state = just create state component file
✅ Add new product = just create product component file

### Disadvantages
❌ Less explicit about supported combinations
❌ Cannot easily add custom overrides per combination

---

## Hybrid Approach (Best of Both Worlds)

### What Files Exist on Disk

```
config-repo/
├── templates/                          # 22 COMPONENT FILES (always)
│   ├── base-payer.yml
│   ├── products/
│   ├── markets/
│   └── states/
│
├── medical-individual-ca.yml          # PRE-GENERATED for common combos
├── dental-medical-individual-ca.yml   # (maybe 20-30 files)
└── medical-medicare-tx.yml

# Uncommon combinations not pre-generated
# (e.g., vision-small-group-wyoming.yml doesn't exist)
```

### How It Works

```java
@PostMapping("/generate")
public ResponseEntity<byte[]> generateHybrid(@RequestBody EnrollmentPdfRequest request) {
    String configName = configSelectionService.selectConfigByConvention(request.getEnrollment());
    
    PdfMergeConfig config;
    try {
        // Try pre-generated file first (for common combinations)
        config = pdfMergeConfigService.loadConfig(configName);
        log.info("Using pre-generated config: {}", configName);
    } catch (FileNotFoundException e) {
        // Not pre-generated - generate dynamically
        config = pdfMergeConfigService.loadConfigDynamic(request.getEnrollment());
        log.info("Using dynamic config for: {}", configName);
    }
    
    byte[] pdf = pdfMergeService.generateMergedPdf(config, request.getPayload());
    return ResponseEntity.ok(pdf);
}
```

**Result:**
- Common combos (90% of requests): Use fast pre-generated file
- Rare combos (10% of requests): Generate dynamically on-the-fly
- Only maintain 20-30 pre-generated files instead of 315

---

## Visual Comparison

### Traditional Approach (315 Files)

```
dental-medical-individual-ca.yml
────────────────────────────────
sections:
  - name: cover
    type: pdfbox
    template: cover-generator
  - name: dental-coverage
    type: freemarker
    template: dental-coverage.ftl
  - name: medical-coverage
    type: freemarker
    template: medical-coverage.ftl
  - name: individual-mandate
    type: freemarker
    template: individual-mandate.ftl
  - name: ca-dmhc-form
    type: acroform
    template: ca-dmhc.pdf
    fieldMapping:
      "MemberName": "member.name"
      ...

(50-100 lines of duplicated config)
```

**Every combination is a complete standalone file.**

---

### Pre-Generated Composition (22 Components + 315 Tiny Files)

```
dental-medical-individual-ca.yml        ← 6 lines!
────────────────────────────────
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california.yml
```

**Top-level file just lists components. Actual config lives in 22 component files.**

---

### Fully Dynamic (22 Components Only)

```
No file exists for dental-medical-individual-ca.yml!

At runtime:
1. Request comes in with products=["medical","dental"], market="individual", state="CA"
2. Code builds composition in memory
3. Loads components and merges them
4. Generates PDF
```

**No top-level files at all. Everything happens in memory at runtime.**

---

## Real Example: What Happens Step by Step

### Scenario: Request for Medical + Dental, Individual, California

#### With Pre-Generated File

```bash
# Step 1: API Request
curl -X POST http://localhost:8080/api/enrollment/generate \
  -d '{"enrollment": {"products": ["medical","dental"], "marketCategory": "individual", "state": "CA"}}'

# Step 2: ConfigSelectionService
configName = "dental-medical-individual-ca.yml"

# Step 3: Check if file exists
File exists? → YES
Read file: config-repo/dental-medical-individual-ca.yml

Content:
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california.yml

# Step 4: Load and merge components
Load: templates/base-payer.yml          → sections: [cover, member-plans]
Load: templates/products/dental.yml     → sections: [dental-coverage, dental-ack-form]
Load: templates/products/medical.yml    → sections: [medical-coverage, rx-coverage]
Load: templates/markets/individual.yml  → sections: [individual-mandate]
Load: templates/states/california.yml   → sections: [ca-dmhc-form]

# Step 5: Merged result (in memory)
Final config has sections:
  1. cover (from base)
  2. member-plans (from base)
  3. dental-coverage (from dental.yml)
  4. dental-ack-form (from dental.yml)
  5. medical-coverage (from medical.yml)
  6. rx-coverage (from medical.yml)
  7. individual-mandate (from individual.yml)
  8. ca-dmhc-form (from california.yml)

# Step 6: Generate PDF using merged config
```

#### With Fully Dynamic (No File)

```bash
# Step 1: API Request
curl -X POST http://localhost:8080/api/enrollment/generate \
  -d '{"enrollment": {"products": ["medical","dental"], "marketCategory": "individual", "state": "CA"}}'

# Step 2: ConfigSelectionService
configName = "dental-medical-individual-ca.yml"

# Step 3: Check if file exists
File exists? → NO

# Step 4: Build composition in memory
composition = {
  base: "templates/base-payer.yml",
  components: [
    "templates/products/dental.yml",
    "templates/products/medical.yml",
    "templates/markets/individual.yml",
    "templates/states/california.yml"
  ]
}

# Step 5: Load and merge components (same as above)
Load: templates/base-payer.yml          → sections: [cover, member-plans]
Load: templates/products/dental.yml     → sections: [dental-coverage, dental-ack-form]
Load: templates/products/medical.yml    → sections: [medical-coverage, rx-coverage]
Load: templates/markets/individual.yml  → sections: [individual-mandate]
Load: templates/states/california.yml   → sections: [ca-dmhc-form]

# Step 6: Same merged result, generate PDF
```

**End result is IDENTICAL! Only difference is whether composition structure comes from file or memory.**

---

## Recommendation

### Start with Hybrid Approach

**Create pre-generated files for common combinations:**

```bash
# Top 20 combinations (covers 90% of requests)
medical-individual-ca.yml
medical-individual-tx.yml
medical-individual-ny.yml
dental-medical-individual-ca.yml
dental-medical-individual-tx.yml
medical-medicare-ca.yml
medical-medicare-tx.yml
medical-medicare-fl.yml
dental-medical-vision-individual-ca.yml
... (11 more)
```

**For uncommon combinations:**
- They generate dynamically on first request
- Optional: Cache the generated config in memory for future requests
- Optional: Auto-create physical file on first use (write-through cache)

### File Count Summary

| Approach | Component Files | Top-Level Files | Total |
|----------|----------------|-----------------|-------|
| **Traditional** | 0 | 315 (full configs) | 315 |
| **Pre-Generated Composition** | 22 | 315 (tiny refs) | 337 |
| **Hybrid** | 22 | 20-30 (common) | 42-52 |
| **Fully Dynamic** | 22 | 0 | 22 |

**Best choice: Hybrid with 22 components + 20-30 pre-generated = ~50 total files**

---

## Summary

**Pre-Generated:**
- Physical file `dental-medical-individual-ca.yml` exists on disk
- Contains only composition structure (6 lines)
- At runtime: Read file → Load components → Merge → Generate PDF

**Fully Dynamic:**
- No physical file for `dental-medical-individual-ca.yml`
- At runtime: Build composition in memory → Load components → Merge → Generate PDF

**Hybrid (Recommended):**
- Pre-generate common combinations (fast path)
- Generate rare combinations dynamically (fallback)
- Best of both worlds

**In all cases: Actual section definitions live in 22 component files. The only question is whether the composition structure comes from a file or is built in memory.**
