<#-- Example: Using Custom Components -->
<#import "/templates/components.ftl" as c>

<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Component Demo - Invoice ${invoice.number}">
  
  <#-- Using Card Component -->
  <@c.card title="Invoice Information" borderColor="#007bff">
    <@c.keyValue label="Invoice Number" value=invoice.number />
    <@c.keyValue label="Customer" value=customer.name />
    <@c.keyValue label="Date" value=invoice.date />
    <@c.keyValue label="Status" value="Paid" />
  </@c.card>

  <#-- Using Badge Component -->
  <div style="margin:15px 0;">
    Payment Status: <@c.badge text="PAID" type="success" />
    Priority: <@c.badge text="HIGH" type="warning" />
  </div>

  <#-- Using Alert Component -->
  <@c.alert 
    message="Payment received successfully. Thank you for your business!" 
    type="success" 
    icon="✓" 
  />

  <#-- Using Section Component -->
  <@c.section title="Invoice Items">
    <#assign columnCount = (payload.columns?default("2"))?number>
    
    <#-- Using Auto Grid Component with nested content -->
    <@c.autoGrid columns=columnCount gap="15px" items=invoice.items ; item>
      <@c.card footer="Qty: ${item.quantity!1}" bgColor="#f9f9f9">
        <div style="font-weight:bold; font-size:1.1em; margin-bottom:5px;">
          ${item.description}
        </div>
        <div style="color:#007bff; font-size:1.3em; font-weight:bold;">
          ${item.price}
        </div>
      </@c.card>
    </@c.autoGrid>
  </@c.section>

  <#-- Using Data Table Component -->
  <@c.section title="Itemized Breakdown">
    <@c.dataTable 
      headers=["Description", "Quantity", "Unit Price", "Total"]
      rows=[
        ["Web Development", "1", "$200.00", "$200.00"],
        ["API Integration", "1", "$150.00", "$150.00"],
        ["Testing", "2", "$25.00", "$50.00"],
        ["Documentation", "1", "$50.00", "$50.00"]
      ]
      striped=true
    />
  </@c.section>

  <#-- Using Stat Cards in Grid -->
  <div style="margin-top:20px;">
    <#assign stats = [
      {"label": "Total Items", "value": invoice.items?size?string, "icon": "📦"},
      {"label": "Subtotal", "value": "$400.00", "icon": "💰"},
      {"label": "Discount", "value": "10%", "icon": "🎉"}
    ] />
    <@c.autoGrid columns=3 gap="10px" items=stats ; stat>
      <@c.statCard 
        label=stat.label 
        value=stat.value 
        icon=stat.icon 
      />
    </@c.autoGrid>
  </div>

  <#-- Using Progress Bar -->
  <div style="margin-top:20px;">
    <h4>Project Completion</h4>
    <@c.progressBar percentage="75" label="75% Complete" height="25px" />
  </div>

  <#-- Using Icon Text -->
  <div style="margin-top:20px;">
    <@c.iconText icon="📧" text="billing@company.com" />
    <span style="margin:0 10px;">|</span>
    <@c.iconText icon="📞" text="+1 (555) 123-4567" />
    <span style="margin:0 10px;">|</span>
    <@c.iconText icon="🌐" text="www.company.com" />
  </div>

  <#-- Total with Card -->
  <div style="margin-top:20px;">
    <@c.card bgColor="#e7f3ff" borderColor="#007bff">
      <table style="width:100%;">
        <tr>
          <td style="font-size:1.5em; font-weight:bold;">TOTAL:</td>
          <td style="text-align:right; font-size:1.8em; font-weight:bold; color:#007bff;">
            ${invoice.total}
          </td>
        </tr>
      </table>
    </@c.card>
  </div>

</@layout.layout>
