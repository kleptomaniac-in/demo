<#-- Example: Using Functions -->
<#import "/templates/components.ftl" as c>
<#import "/templates/functions.ftl" as f>

<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Functions Demo - Invoice ${invoice.number}">
  
  <h2>Invoice ${invoice.number}</h2>

  <#-- Using calculation functions -->
  <#assign subtotal = f.calculateSubtotal(invoice.items) />
  <#assign taxAmount = f.calculateTax(subtotal, 0.08) />
  <#assign discountAmount = 25.00 />
  <#assign finalTotal = f.calculateTotal(subtotal, taxAmount, discountAmount) />

  <#-- Invoice Header with functions -->
  <@c.card title="Invoice Information" borderColor="#007bff">
    <@c.verticalTable 
      data={
        "Invoice Number": invoice.number,
        "Customer": customer.name,
        "Date": f.formatDate(invoice.date),
        "Status": f.getStatusIcon("paid") + " " + "Paid",
        "Items Count": f.countItems(invoice.items)?string
      }
      headerWidth="35%"
    />
  </@c.card>

  <#-- Items with formatting functions -->
  <@c.section title="Invoice Items">
    <table style="width:100%; border-collapse:collapse;">
      <thead>
        <tr style="background:#f5f5f5;">
          <th style="border:1px solid #ddd; padding:8px; text-align:left;">Description</th>
          <th style="border:1px solid #ddd; padding:8px; text-align:center; width:10%;">Qty</th>
          <th style="border:1px solid #ddd; padding:8px; text-align:right; width:15%;">Price</th>
          <th style="border:1px solid #ddd; padding:8px; text-align:right; width:15%;">Total</th>
        </tr>
      </thead>
      <tbody>
        <#list invoice.items as item>
          <#assign qty = item.quantity!1 />
          <#assign price = item.price?replace("$", "")?replace(",", "")?number />
          <#assign itemTotal = price * qty />
          <tr>
            <td style="border:1px solid #ddd; padding:8px;">
              ${f.truncateText(item.description, 40)}
            </td>
            <td style="border:1px solid #ddd; padding:8px; text-align:center;">
              ${qty}
            </td>
            <td style="border:1px solid #ddd; padding:8px; text-align:right;">
              ${f.formatCurrency(price)}
            </td>
            <td style="border:1px solid #ddd; padding:8px; text-align:right; font-weight:bold;">
              ${f.formatCurrency(itemTotal)}
            </td>
          </tr>
        </#list>
      </tbody>
    </table>
  </@c.section>

  <#-- Financial Summary using functions -->
  <div style="margin-top:20px;">
    <table style="width:100%; border-collapse:separate; border-spacing:15px;">
      <tr>
        <td style="width:60%; vertical-align:top;">
          <#-- Payment info with validation functions -->
          <@c.card title="Payment Information">
            <#assign email = "billing@acme.com" />
            <#assign isValidEmail = f.isValidEmail(email) />
            
            <p><strong>Email:</strong> ${email} 
              <#if isValidEmail>
                <span style="color:#28a745;">✓ Valid</span>
              <#else>
                <span style="color:#dc3545;">✗ Invalid</span>
              </#if>
            </p>
            <p><strong>Payment Method:</strong> Credit Card</p>
            <p><strong>Status:</strong> 
              <span style="color:${f.getStatusColor('paid')};">
                ${f.getStatusIcon('paid')} PAID
              </span>
            </p>
          </@c.card>
        </td>
        <td style="width:40%; vertical-align:top;">
          <#-- Totals calculation -->
          <@c.card bgColor="#f8f9fa" borderColor="#007bff">
            <table style="width:100%;">
              <tr>
                <td style="padding:5px;"><strong>Subtotal:</strong></td>
                <td style="text-align:right; padding:5px;">${f.formatCurrency(subtotal)}</td>
              </tr>
              <tr>
                <td style="padding:5px;"><strong>Tax (8%):</strong></td>
                <td style="text-align:right; padding:5px;">${f.formatCurrency(taxAmount)}</td>
              </tr>
              <tr>
                <td style="padding:5px;"><strong>Discount:</strong></td>
                <td style="text-align:right; padding:5px; color:#dc3545;">-${f.formatCurrency(discountAmount)}</td>
              </tr>
              <tr style="border-top:2px solid #007bff;">
                <td style="padding:8px; font-size:1.2em;"><strong>TOTAL:</strong></td>
                <td style="text-align:right; padding:8px; font-size:1.3em; color:#007bff; font-weight:bold;">
                  ${f.formatCurrency(finalTotal)}
                </td>
              </tr>
            </table>
          </@c.card>
        </td>
      </tr>
    </table>
  </div>

  <#-- Using utility functions -->
  <@c.section title="Additional Details">
    <#assign projectName = f.defaultValue(invoice.projectName!"", "N/A") />
    <#assign notes = f.coalesce(invoice.notes!"", invoice.comments!"", "No additional notes") />
    
    <@c.verticalTable 
      data={
        "Project": projectName,
        "Priority": f.getPriorityLabel("high"),
        "Payment Terms": "Net 30",
        "Notes": notes
      }
      headerWidth="25%"
    />
  </@c.section>

  <#-- Math functions demo -->
  <@c.section title="Analysis">
    <#assign itemCount = f.countItems(invoice.items) />
    <#assign avgItemPrice = f.round(subtotal / itemCount, 2) />
    <#assign savingsPercent = f.percentage(discountAmount, subtotal) />
    
    <div style="margin:10px 0;">
      <@c.autoGrid columns=3 gap="10px" items=[
        {"label": "Average Item Price", "value": f.formatCurrency(avgItemPrice)},
        {"label": "Savings", "value": f.formatPercentage(savingsPercent, 1)},
        {"label": "Tax Rate", "value": "8.0%"}
      ] ; stat>
        <@c.statCard label=stat.label value=stat.value />
      </@c.autoGrid>
    </div>
  </@c.section>

  <#-- Conditional rendering with ternary function -->
  <#assign isPaidOnTime = true />
  <@c.alert 
    message=f.ternary(isPaidOnTime, "Thank you for your prompt payment!", "Payment is overdue. Please remit immediately.")
    type=f.ternary(isPaidOnTime, "success", "warning")
    icon=f.ternary(isPaidOnTime, "✓", "⚠")
  />

</@layout.layout>
