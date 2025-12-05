# Generalized Pagination Design - Complete Explanation

## Problem Statement

You needed to limit the number of plans displayed per product in each table to `maxPlansPerProduct`, with overflow plans appearing in additional tables below, all using the same layout.

## Design Approach

### 1. **Data Collection Phase**

Instead of collecting just the first N plans, we collect ALL unique plans across all members:

```freemarker
<#assign allMedicalPlanNames = []>
<#assign allDentalPlanNames = []>
<#assign allVisionPlanNames = []>

<#list payload.members as member>
    <#-- Collect every unique plan name without limiting -->
</#list>
```

**Result:** Complete lists of all unique plans for each product type.

### 2. **Pagination Function - `splitIntoGroups()`**

This is the core innovation. It takes a list and splits it into chunks:

```freemarker
<#function splitIntoGroups planList maxPerGroup>
    <#local groups = []>              // Array to hold groups
    <#local currentGroup = []>        // Current group being built
    
    <#list planList as plan>
        <#if (currentGroup?size < maxPerGroup)>
            // Add to current group if not full
            <#local currentGroup = currentGroup + [plan]>
        <#else>
            // Current group is full, save it and start new one
            <#local groups = groups + [currentGroup]>
            <#local currentGroup = [plan]>
        </#if>
    </#list>
    
    // Don't forget the last group
    <#if (currentGroup?size > 0)>
        <#local groups = groups + [currentGroup]>
    </#if>
    
    <#return groups>
</#function>
```

**Example:**
```
Input: ["Plan A", "Plan B", "Plan C", "Plan D", "Plan E"], maxPerGroup=2
Output: [
  ["Plan A", "Plan B"],
  ["Plan C", "Plan D"],
  ["Plan E"]
]
```

### 3. **Apply Pagination to All Products**

```freemarker
<#assign medicalGroups = splitIntoGroups(allMedicalPlanNames, maxPlansPerProduct)>
<#assign dentalGroups = splitIntoGroups(allDentalPlanNames, maxPlansPerProduct)>
<#assign visionGroups = splitIntoGroups(allVisionPlanNames, maxPlansPerProduct)>
```

**Result:** Each product type has an array of groups.

**Example with maxPlansPerProduct=2:**
- Medical: 5 plans → `[[Plan1, Plan2], [Plan3, Plan4], [Plan5]]` → 3 groups
- Dental: 3 plans → `[[PlanA, PlanB], [PlanC]]` → 2 groups
- Vision: 1 plan → `[[PlanX]]` → 1 group

### 4. **Calculate Number of Tables Needed**

```freemarker
<#assign totalTables = [medicalGroups?size, dentalGroups?size, visionGroups?size]?max>
```

Takes the maximum number of groups across all product types.

**Example:** max(3, 2, 1) = 3 tables needed

### 5. **Render Tables in a Loop**

```freemarker
<#list 0..<totalTables as tableIndex>
    
    // Get the appropriate group for this table (or empty array if none)
    <#assign currentMedicalPlans = (tableIndex < medicalGroups?size)?then(medicalGroups[tableIndex], [])>
    <#assign currentDentalPlans = (tableIndex < dentalGroups?size)?then(dentalGroups[tableIndex], [])>
    <#assign currentVisionPlans = (tableIndex < visionGroups?size)?then(visionGroups[tableIndex], [])>
    
    // Render table with these plans
</#list>
```

**How it works:**

| Table Index | Medical Plans | Dental Plans | Vision Plans |
|-------------|---------------|--------------|--------------|
| 0 | Group[0]: Plan1, Plan2 | Group[0]: PlanA, PlanB | Group[0]: PlanX |
| 1 | Group[1]: Plan3, Plan4 | Group[1]: PlanC | [] (empty) |
| 2 | Group[2]: Plan5 | [] (empty) | [] (empty) |

### 6. **Ternary Operator for Safe Access**

```freemarker
(tableIndex < medicalGroups?size)?then(medicalGroups[tableIndex], [])
```

**Breakdown:**
- `tableIndex < medicalGroups?size` → Condition: Does this group exist?
- `?then(A, B)` → If true, return A; if false, return B
- Returns the group if it exists, otherwise empty array `[]`

**Why needed?** Table 2 needs dental plans, but vision only has 1 group (shown in table 0), so vision returns `[]` for table 2.

### 7. **Dynamic Table Headers**

Each table's headers are based on the current group:

```freemarker
<#if (currentMedicalPlans?size > 0)>
    <th colspan="${currentMedicalPlans?size * 2}" class="product-header">Medical</th>
</#if>
```

- If current group is empty → header doesn't render
- Colspan dynamically adjusts to number of plans in current group
- Each plan gets 2 columns (Base + Bundled)

### 8. **Data Row Matching**

For each member, match their plans to the current table's columns:

```freemarker
<#if (currentMedicalPlans?size > 0)>
    <#list currentMedicalPlans as expectedPlanName>
        <#assign foundPlan = false>
        <#if member.medicalPlans??>
            <#list member.medicalPlans as plan>
                <#if plan.planName == expectedPlanName>
                    <td>$${plan.basePremium}</td>
                    <td>$${plan.bundledPremium}</td>
                    <#assign foundPlan = true>
                    <#break>
                </#if>
            </#list>
        </#if>
        <#if !foundPlan>
            <td class="no-plan">-</td>
            <td class="no-plan">-</td>
        </#if>
    </#list>
</#if>
```

**Logic:**
1. For each expected plan in current group
2. Search member's plans for a match
3. If found → display premiums
4. If not found → display "-"

## Flow Diagram

```
Input Data (Members with Plans)
    ↓
Collect ALL unique plans per product
    ↓
Split into groups of maxPlansPerProduct
    Medical: [[G0], [G1], [G2]]
    Dental:  [[G0], [G1]]
    Vision:  [[G0]]
    ↓
Calculate totalTables = max(3, 2, 1) = 3
    ↓
Loop: tableIndex = 0, 1, 2
    ↓
    For each table:
        Get current group for each product
        ↓
        Render table headers (3 rows)
        ↓
        For each member:
            Match member's plans to current group
            Display premiums or "-"
    ↓
Result: 3 tables with consistent structure
```

## Example Walkthrough

**Given:**
- `maxPlansPerProduct = 2`
- Medical: 5 plans (A, B, C, D, E)
- Dental: 3 plans (X, Y, Z)
- Vision: 1 plan (V)

**Step 1: Split into groups**
```
medicalGroups = [[A, B], [C, D], [E]]      // 3 groups
dentalGroups = [[X, Y], [Z]]               // 2 groups
visionGroups = [[V]]                       // 1 group
totalTables = 3
```

**Step 2: Table 0 (tableIndex=0)**
```
currentMedicalPlans = [A, B]
currentDentalPlans = [X, Y]
currentVisionPlans = [V]

Header: | Member | Medical(2 plans) | Dental(2 plans) | Vision(1 plan) |
        |        | A    | B    |     | X    | Y    |     | V    |      |
        |        | Base | Bund |     | Base | Bund |     | Base | Bund |
```

**Step 3: Table 1 (tableIndex=1)**
```
currentMedicalPlans = [C, D]
currentDentalPlans = [Z]
currentVisionPlans = []                     // No group[1] for vision

Header: | Member | Medical(2 plans) | Dental(1 plan) |
        |        | C    | D    |     | Z    |      |
        |        | Base | Bund |     | Base | Bund |
```

**Step 4: Table 2 (tableIndex=2)**
```
currentMedicalPlans = [E]
currentDentalPlans = []                     // No group[2] for dental
currentVisionPlans = []                     // No group[2] for vision

Header: | Member | Medical(1 plan) |
        |        | E    |      |
        |        | Base | Bund |
```

## Key Benefits

✅ **Fully Configurable:** Change `maxPlansPerProduct` once, everything adjusts
✅ **Balanced Tables:** Each table respects the limit for ALL products
✅ **No Waste:** Empty columns don't appear (conditional rendering)
✅ **Consistent Layout:** All tables use identical structure
✅ **Scalable:** Works with any number of plans and products
✅ **Member Alignment:** Same members in same order across all tables

## Configuration Impact

| maxPlansPerProduct | 5 Medical, 3 Dental, 1 Vision | Result |
|-------------------|-------------------------------|---------|
| 1 | Each plan in separate table | 5 tables |
| 2 | Up to 2 plans per product | 3 tables |
| 3 | Up to 3 plans per product | 2 tables |
| 5+ | All plans in one table | 1 table |

The design is **product-agnostic** and **plan-agnostic** - it works regardless of how many products or plans you have!