# Healthcare Member Plans Template - Design & Implementation

## Requirement Analysis

You needed a table to display:
1. **Members** in the first column
2. **Products** (Medical, Dental, Vision) as separate column headers
3. **Plan names** under each product header
4. **Premium data** (Base and Bundled) as actual data rows for each member

## Design Approach

### 1. **Dynamic Column Structure**
The template uses a **3-row header** design:

```
Row 1: [Member Name] | [    Medical    ] | [    Dental    ] | [   Vision   ]
Row 2:                | [Plan A] [Plan B] | [Plan C]         | [Plan D]
Row 3:                | [B] [Bd] [B] [Bd] | [B] [Bd]         | [B] [Bd]
Data:  John Smith     | $450 $425 $380... | $45 $40          | $25 $22
```

Where:
- **Row 1**: Product type headers (colspan to cover all plans)
- **Row 2**: Individual plan names (colspan=2 for Base+Bundled)
- **Row 3**: Premium type labels (Base/Bundled)
- **Data rows**: Actual premium amounts

### 2. **Data Collection Phase**

The template first scans ALL members to collect unique plan names:

```freemarker
<#assign medicalPlanNames = []>
<#assign dentalPlanNames = []>
<#assign visionPlanNames = []>

<#list payload.members as member>
    <#-- Collect all unique plan names across all members -->
    <#if member.medicalPlans??>
        <#list member.medicalPlans as plan>
            <#if !medicalPlanNames?seq_contains(plan.planName)>
                <#assign medicalPlanNames = medicalPlanNames + [plan.planName]>
            </#if>
        </#list>
    </#if>
    <#-- Same for dental and vision -->
</#list>
```

**Why?** This ensures the table has consistent columns even if different members have different plans.

### 3. **Header Generation**

#### Row 1 - Product Headers
```freemarker
<th colspan="${medicalPlanNames?size * 2}" class="product-header">Medical</th>
```
- Colspan = (number of unique medical plans) × 2 (Base + Bundled columns per plan)
- Example: 3 medical plans → colspan=6

#### Row 2 - Plan Names
```freemarker
<#list medicalPlanNames as planName>
    <th colspan="2" class="plan-header">${planName}</th>
</#list>
```
- Each plan name spans 2 columns (Base + Bundled)

#### Row 3 - Premium Types
```freemarker
<#list medicalPlanNames as planName>
    <th class="premium-header">Base</th>
    <th class="premium-header">Bundled</th>
</#list>
```
- One header for each premium type under each plan

### 4. **Data Row Generation**

For each member, the template matches their plans to the column structure:

```freemarker
<#list medicalPlanNames as expectedPlanName>
    <#assign foundPlan = false>
    <#if member.medicalPlans??>
        <#list member.medicalPlans as plan>
            <#if plan.planName == expectedPlanName>
                <td>$${plan.basePremium?string["0.00"]}</td>
                <td>$${plan.bundledPremium?string["0.00"]}</td>
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
```

**Logic:**
- For each expected plan (column), search if the member has that plan
- If found → display premiums
- If not found → display "-"
- This ensures alignment with the header columns

### 5. **Page Layout Optimization**

To fit many columns:

```css
@page {
    size: A4 landscape;  /* Wider page */
    margin: 10mm;
}

body {
    font-size: 8px;      /* Smaller text */
}

.member-table {
    table-layout: fixed; /* Consistent column widths */
}
```

### 6. **Key Design Decisions**

| Decision | Rationale |
|----------|-----------|
| **Collect unique plan names** | Different members may have different plans; need consistent columns |
| **Match by plan name** | Ensures correct premium aligns with correct plan header |
| **3-row headers** | Clear hierarchy: Product → Plan → Premium Type |
| **Colspan calculations** | Uses `planNames?size * 2` to ensure headers span correct number of columns |
| **Landscape orientation** | Accommodates wide tables with many plans |
| **Show "-" for missing plans** | Members without certain plans still align correctly |

## Data Flow

```
Input JSON
    ↓
Scan all members → Collect unique plan names
    ↓
Generate headers (Product → Plans → Base/Bundled)
    ↓
For each member:
    For each expected plan:
        Search member's plans
        Display premium or "-"
    ↓
Output: Aligned table with all members × all plans
```

## Example Output Structure

```
Member      | Medical                    | Dental        | Vision
           | PPO Gold | HSA Plus        | Dental Basic  | Vision Ess
           | B   | Bd  | B   | Bd       | B   | Bd      | B   | Bd
-----------|---------|---------|---------|---------------|-------------
John       | $450|$425| $380|$360       | $45 | $40     | $25 | $22
Sarah      | -   | -   | -   | -        | $30 | $28     | -   | -
```

This design ensures that:
✅ All members are visible in rows
✅ Products are grouped under clear headers
✅ Plan names are shown and aligned
✅ Premiums match their respective plans
✅ Missing plans don't break alignment