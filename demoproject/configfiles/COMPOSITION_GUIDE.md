# PDF Template Composition System

## Overview

The composition system allows you to build complex PDF templates by combining reusable components. This avoids the explosion of 600+ individual files by using a hierarchical component-based approach.

## Architecture

### Component Hierarchy

```
Base Template (common to all)
    ↓
Product Components (Medical, Dental, Vision)
    ↓
Market Components (Individual, Small Group, Large Group, Medicare)
    ↓
State Components (CA, TX, NY, etc.)
    ↓
Final Composed Template (with optional overrides)
```

### File Structure

```
config-repo/
├── templates/
│   ├── base-payer.yml              # Foundation for all templates
│   ├── products/
│   │   ├── medical.yml             # Medical-specific sections
│   │   ├── dental.yml              # Dental-specific sections
│   │   └── vision.yml              # Vision-specific sections
│   ├── markets/
│   │   ├── individual.yml          # Individual market sections
│   │   ├── small-group.yml         # Small group sections
│   │   ├── large-group.yml         # Large group sections
│   │   └── medicare.yml            # Medicare sections
│   └── states/
│       ├── california.yml          # CA-specific requirements
│       ├── texas.yml               # TX-specific requirements
│       └── newyork.yml             # NY-specific requirements
└── [final-templates]/
    ├── medical-individual-ca.yml   # Composed template
    ├── dental-small-group-tx.yml   # Composed template
    └── ...
```

## How Composition Works

### 1. Define Component Files

Each component file contains sections specific to that dimension:

**Product Component** (medical.yml):
```yaml
pdfMerge:
  sections:
    - name: medical-coverage
      type: freemarker
      template: products/medical-coverage-details.ftl
      enabled: true
```

**State Component** (california.yml):
```yaml
pdfMerge:
  sections:
    - name: ca-dmhc-disclosure
      type: freemarker
      template: states/california-dmhc-disclosure.ftl
      enabled: true
  
  # Override header
  header:
    content:
      left:
        text: "CA License #12345 | {companyName}"
```

### 2. Create Composed Templates

Reference components in order of precedence:

```yaml
# medical-individual-ca.yml
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/medical.yml
    - templates/markets/individual.yml
    - templates/states/california.yml

# Optional: Add template-specific overrides
pdfMerge:
  sections:
    - name: custom-section
      type: freemarker
      template: specialized/custom.ftl
```

### 3. Deep Merge Semantics

The system merges configurations using these rules:

1. **Field-level Override**: Later components override earlier ones
   ```yaml
   # Base: fontSize: 10
   # State: fontSize: 8
   # Result: fontSize: 8
   ```

2. **Nested Map Merge**: Maps are recursively merged
   ```yaml
   # Base: header.content.left.text: "{companyName}"
   # State: header.content.left.text: "CA License | {companyName}"
   # Result: CA license text, other header fields preserved
   ```

3. **Section List Merge**: Sections with same `name` are merged; new sections appended
   ```yaml
   # Base: section "member-summary" with template A
   # Component: section "member-summary" with template B
   # Result: "member-summary" uses template B
   ```

4. **Other Lists**: Appended (bookmarks, conditionals)

## Usage Examples

### Example 1: Basic Composition

**Request:**
```json
{
  "configName": "medical-individual-ca.yml",
  "payload": {
    "companyName": "HealthCorp",
    "members": [...]
  }
}
```

**Effective Configuration:**
- Base sections (cover page, member summary)
- Medical sections (coverage, provider network, prescriptions)
- Individual market sections (mandate, cost-sharing)
- California sections (DMHC disclosure, benefit mandates)
- Headers/footers with CA license
- All bookmarks combined

### Example 2: Overriding Sections

```yaml
composition:
  base: templates/base-payer.yml
  components:
    - templates/products/dental.yml
    - templates/markets/small-group.yml

pdfMerge:
  sections:
    # Override the base member-summary with group-specific version
    - name: member-summary
      template: group-member-summary.ftl
```

### Example 3: State-Specific Premium Disclosure

California requires different premium disclosure for Individual market:

```yaml
# california.yml
pdfMerge:
  conditionalSections:
    - condition: "payload.marketCategory"
      sections:
        - name: ca-individual-premium-disclosure
          type: freemarker
          template: states/california-individual-premium.ftl
          insertAfter: cost-sharing
```

This section only appears when:
1. Template includes california.yml component
2. Payload contains marketCategory field
3. Cost-sharing section exists (for insertAfter)

## Benefits

### 1. Maintainability
- Change base header → affects all 600 templates
- Update CA requirements → affects all CA templates
- Fix medical section → affects all medical templates

### 2. Scalability
- Add new state: 1 file (not 60 files)
- Add new product: 1 file (not 200 files)
- Total files: ~70 instead of 600+

### 3. Flexibility
- Mix and match any combination
- Override specific sections per template
- Add conditional sections based on payload

### 4. Testing
- Test components in isolation
- Verify composition logic once
- Mock specific component combinations

## Advanced Features

### Conditional Overrides

Apply overrides based on payload data:

```yaml
# states/california.yml
conditionalSections:
  - condition: "payload.marketCategory == 'Individual'"
    sections:
      - name: special-individual-disclosure
        template: ca-individual-special.ftl
```

### Insert Positioning

Control where new sections appear:

```yaml
sections:
  - name: my-custom-section
    insertAfter: medical-coverage  # Appears after this section
```

### Partial Overrides

Override only specific fields:

```yaml
# Base has: header.content.left, center, right
# Component overrides only left:
header:
  content:
    left:
      text: "New text"
      # fontSize inherited from base
    # center and right inherited from base
```

## Migration Strategy

### From Individual Files to Composition

**Before** (600 files):
```
medical-individual-ca.yml (full config)
medical-individual-tx.yml (full config, 90% duplicate)
medical-individual-ny.yml (full config, 90% duplicate)
... (597 more files)
```

**After** (70 files):
```
templates/base-payer.yml (1 file)
templates/products/*.yml (3 files)
templates/markets/*.yml (4 files)
templates/states/*.yml (50 files)
composed-templates/*.yml (12 common combinations)
```

### Incremental Adoption

1. **Phase 1**: Create base and most common components
2. **Phase 2**: Convert top 20 templates to composition
3. **Phase 3**: Generate remaining composed templates programmatically
4. **Phase 4**: Remove legacy individual files

## Best Practices

1. **Component Independence**: Each component should be self-contained
2. **Naming Consistency**: Use consistent section names across components
3. **Override Sparingly**: Most customization via components, not overrides
4. **Document Conditions**: Clearly document conditional sections
5. **Test Combinations**: Test common product × market × state combinations

## Troubleshooting

### Issue: Section Not Appearing

**Check:**
1. Is section enabled in component?
2. Is component included in composition list?
3. Is there a conflicting section with same name?

### Issue: Wrong Template Used

**Remember:** Later components override earlier ones
- Check component order in composition list
- Check if there's an override in the final template

### Issue: Header/Footer Not Correct

**Check:**
1. Which component defines header/footer?
2. Are you doing partial override (specific field) or full override?
3. Look at console logs for component loading order

## Performance

- Components are loaded once and cached
- Merge happens at template load time, not per-request
- Typical composition: 4 files merged in <10ms
- Consider caching composed configurations in production
