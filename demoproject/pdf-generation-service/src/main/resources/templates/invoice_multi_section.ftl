<#-- Advanced dynamic layout template with multiple sections -->
<#import "/templates/macros.ftl" as m>

<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Invoice ${invoice.number}">
  <h2>Invoice ${invoice.number}</h2>
  <p>Customer: ${customer.name}</p>

  <#-- Section 1: Invoice Items with dynamic columns -->
  <#if invoice.items?has_content>
    <h3>Items</h3>
    <#assign itemColumns = (payload.itemsColumns?default("2"))?number>
    <table style="width:100%; border-collapse:separate; border-spacing:10px;">
      <tbody>
        <#list invoice.items?chunk(itemColumns) as row>
          <tr>
            <#list row as item>
              <td style="border:1px solid #ddd; padding:8px; background:#f9f9f9; vertical-align:top; width:${100/itemColumns}%;">
                <div><strong>${item.description}</strong></div>
                <div>${item.price}</div>
              </td>
            </#list>
            <#if (row?size < itemColumns)>
              <#list 1..(itemColumns - row?size) as _>
                <td style="border:1px solid #ddd; padding:8px; background:#f9f9f9; width:${100/itemColumns}%;">&nbsp;</td>
              </#list>
            </#if>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>

  <#-- Section 2: Additional details with different column layout -->
  <#if invoice.additionalDetails?has_content>
    <h3 style="margin-top:20px;">Additional Details</h3>
    <#assign detailColumns = (payload.detailsColumns?default("3"))?number>
    <table style="width:100%; border-collapse:separate; border-spacing:8px;">
      <tbody>
        <#list invoice.additionalDetails?chunk(detailColumns) as row>
          <tr>
            <#list row as detail>
              <td style="padding:6px; border-left:3px solid #007bff; vertical-align:top; width:${100/detailColumns}%;">
                <div style="font-size:0.85em; color:#666;">${detail.label}</div>
                <div>${detail.value}</div>
              </td>
            </#list>
            <#if (row?size < detailColumns)>
              <#list 1..(detailColumns - row?size) as _>
                <td style="padding:6px; width:${100/detailColumns}%;">&nbsp;</td>
              </#list>
            </#if>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>

  <#-- Section 3: Notes in single column if present -->
  <#if invoice.notes?has_content>
    <div style="margin-top:20px; padding:10px; background:#fff9e6; border-left:4px solid #ffc107;">
      <strong>Notes:</strong> ${invoice.notes}
    </div>
  </#if>

  <div class="total" style="margin-top:20px; font-weight:bold; font-size:1.2em;">
    Total: ${invoice.total}
  </div>
</@layout.layout>
