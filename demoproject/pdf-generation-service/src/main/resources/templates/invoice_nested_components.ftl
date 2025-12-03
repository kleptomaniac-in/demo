<#-- Nested Tables Using Components - Cleaner Approach -->
<#import "/templates/components.ftl" as c>
<#import "/templates/functions.ftl" as f>

<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Component-Based Nested Demo - Invoice ${invoice.number}">
  
  <h2>Invoice ${invoice.number}</h2>

  <#-- Example 1: Using card component with nested vertical tables -->
  <@c.section title="Invoice Details with Nested Information">
    <#list invoice.items as item>
      <@c.card 
        title="${item.description}" 
        footer="Total: ${item.price}"
        bgColor="#f9f9f9"
        borderColor="#007bff">
        
        <#-- Nested vertical table inside card -->
        <@c.verticalTable 
          data={
            "Quantity": item.quantity!1?string,
            "Unit Price": item.price,
            "Status": "Completed",
            "Delivery": "Immediate"
          }
          headerWidth="40%"
          headerBg="#e7f3ff"
        />
      </@c.card>
      <div style="margin-bottom:10px;"></div>
    </#list>
  </@c.section>

  <#-- Example 2: Grouped data using sections with nested dataTable -->
  <@c.section title="Services by Category">
    <#assign categories = [
      {
        "name": "Development",
        "icon": "💻",
        "items": [
          ["Web Development", "1", "$200.00", "$200.00"],
          ["API Integration", "1", "$150.00", "$150.00"]
        ],
        "total": "$350.00"
      },
      {
        "name": "Quality Assurance",
        "icon": "🧪",
        "items": [
          ["Testing", "2", "$25.00", "$50.00"],
          ["Documentation", "1", "$50.00", "$50.00"]
        ],
        "total": "$100.00"
      }
    ] />
    
    <#list categories as category>
      <@c.card 
        title="${category.icon} ${category.name}"
        footer="Category Total: ${category.total}"
        borderColor="#28a745">
        
        <#-- Nested horizontal table -->
        <@c.dataTable 
          headers=["Item", "Qty", "Unit Price", "Total"]
          rows=category.items
          striped=true
        />
      </@c.card>
      <#if !category?is_last>
        <div style="margin-bottom:15px;"></div>
      </#if>
    </#list>
  </@c.section>

  <#-- Example 3: Side-by-side nested components -->
  <@c.section title="Customer & Payment Information">
    <table style="width:100%; border-collapse:separate; border-spacing:15px;">
      <tr>
        <td style="width:50%; vertical-align:top;">
          <@c.card title="Customer Details" borderColor="#007bff">
            <@c.verticalTable 
              data={
                "Company": customer.name,
                "Contact": "John Doe",
                "Email": "john@company.com",
                "Phone": "+1 (555) 123-4567"
              }
              headerWidth="35%"
            />
          </@c.card>
        </td>
        <td style="width:50%; vertical-align:top;">
          <@c.card title="Payment Summary" borderColor="#28a745">
            <@c.verticalTable 
              data={
                "Subtotal": "$400.00",
                "Tax (8%)": "$32.00",
                "Discount": "-$25.00",
                "Total": invoice.total
              }
              headerWidth="40%"
              headerBg="#d4edda"
            />
          </@c.card>
        </td>
      </tr>
    </table>
  </@c.section>

  <#-- Example 4: Multi-level nesting with autoGrid -->
  <@c.section title="Project Breakdown">
    <#assign projects = [
      {
        "name": "Website Redesign",
        "status": "In Progress",
        "details": {
          "Start Date": "2025-11-01",
          "End Date": "2025-12-15",
          "Team Size": "5",
          "Budget": "$10,000"
        }
      },
      {
        "name": "Mobile App",
        "status": "Planning",
        "details": {
          "Start Date": "2026-01-01",
          "End Date": "2026-03-31",
          "Team Size": "3",
          "Budget": "$15,000"
        }
      }
    ] />
    
    <@c.autoGrid columns=2 gap="15px" items=projects ; project>
      <@c.card 
        title="${project.name}"
        footer="Status: ${project.status}"
        borderColor="#6c757d">
        
        <#-- Nested vertical table for project details -->
        <@c.verticalTable 
          data=project.details
          headerWidth="45%"
          striped=true
        />
      </@c.card>
    </@c.autoGrid>
  </@c.section>

  <#-- Example 5: Expandable sections with nested content -->
  <@c.section title="Detailed Item Breakdown">
    <#list invoice.items as item>
      <@c.alert 
        message="${item.description} - ${item.price}"
        type="info">
      </@c.alert>
      
      <div style="margin-left:20px; margin-bottom:15px;">
        <@c.card bgColor="#f8f9fa" borderColor="#dee2e6">
          <table style="width:100%; border-collapse:separate; border-spacing:10px;">
            <tr>
              <td style="width:50%; vertical-align:top;">
                <strong>Item Details:</strong>
                <@c.verticalTable 
                  data={
                    "Quantity": item.quantity!1?string,
                    "Unit Price": item.price,
                    "Tax Included": "Yes"
                  }
                  headerWidth="50%"
                />
              </td>
              <td style="width:50%; vertical-align:top;">
                <strong>Delivery Info:</strong>
                <@c.verticalTable 
                  data={
                    "Method": "Digital",
                    "Timeline": "Immediate",
                    "Status": "Completed"
                  }
                  headerWidth="50%"
                />
              </td>
            </tr>
          </table>
        </@c.card>
      </div>
    </#list>
  </@c.section>

  <#-- Example 6: Nested tables with stat cards -->
  <@c.section title="Performance Metrics">
    <#assign metrics = [
      {
        "category": "Development",
        "stats": [
          {"label": "Tasks", "value": "12", "icon": "📋"},
          {"label": "Completed", "value": "10", "icon": "✓"},
          {"label": "Progress", "value": "83%", "icon": "📊"}
        ]
      },
      {
        "category": "Quality",
        "stats": [
          {"label": "Tests", "value": "45", "icon": "🧪"},
          {"label": "Passed", "value": "42", "icon": "✓"},
          {"label": "Success Rate", "value": "93%", "icon": "📈"}
        ]
      }
    ] />
    
    <#list metrics as metric>
      <h4>${metric.category} Metrics</h4>
      <@c.autoGrid columns=3 gap="10px" items=metric.stats ; stat>
        <@c.statCard 
          label=stat.label
          value=stat.value
          icon=stat.icon
        />
      </@c.autoGrid>
      <#if !metric?is_last>
        <div style="margin-bottom:20px;"></div>
      </#if>
    </#list>
  </@c.section>

  <#-- Example 7: Complex nested hierarchy -->
  <@c.section title="Complete Order Structure">
    <@c.card title="Order #${invoice.number}" borderColor="#007bff">
      
      <#-- Level 1: Order info -->
      <@c.verticalTable 
        data={
          "Date": invoice.date,
          "Customer": customer.name,
          "Items": invoice.items?size?string
        }
        headerWidth="30%"
      />
      
      <div style="margin-top:15px;">
        <strong>Line Items:</strong>
        
        <#-- Level 2: Items table -->
        <div style="margin-top:10px;">
          <@c.dataTable 
            headers=["#", "Description", "Quantity", "Price"]
            rows=[
              ["1", invoice.items[0].description, invoice.items[0].quantity!1?string, invoice.items[0].price],
              ["2", invoice.items[1].description, invoice.items[1].quantity!1?string, invoice.items[1].price],
              ["3", invoice.items[2].description, invoice.items[2].quantity!1?string, invoice.items[2].price],
              ["4", invoice.items[3].description, invoice.items[3].quantity!1?string, invoice.items[3].price]
            ]
            striped=true
          />
        </div>
      </div>
      
      <div style="margin-top:15px;">
        <strong>Financial Summary:</strong>
        
        <#-- Level 3: Nested summary -->
        <div style="margin-top:10px;">
          <@c.card bgColor="#e7f3ff" borderColor="#007bff">
            <table style="width:100%;">
              <tr>
                <td style="font-size:1.2em; font-weight:bold;">TOTAL:</td>
                <td style="text-align:right; font-size:1.3em; font-weight:bold; color:#007bff;">
                  ${invoice.total}
                </td>
              </tr>
            </table>
          </@c.card>
        </div>
      </div>
      
    </@c.card>
  </@c.section>

</@layout.layout>
