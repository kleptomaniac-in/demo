<#-- Dynamic layout invoice template -->
<#import "/templates/macros.ftl" as m>

<#-- Select layout dynamically from model key 'layoutName' (defaults to 'compact') -->
<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<#-- Get column configuration from payload (default to 2 columns) -->
<#assign columnCount = (payload.columns?default("2"))?number>
<#assign layoutMode = payload.layoutMode?default("grid")> <#-- "grid" or "table" -->
<#assign gridGap = payload.gap?default("10px")>

<@layout.layout title="Invoice ${invoice.number}">
  <h2>Invoice ${invoice.number}</h2>
  <p>Customer: ${customer.name}</p>

  <#-- Table-based layout that works with OpenHTMLToPDF -->
  <table style="width:100%; border-collapse:separate; border-spacing:${gridGap};">
    <tbody>
      <#list invoice.items?chunk(columnCount) as row>
        <tr>
          <#list row as item>
            <td style="border:1px solid #ddd; padding:8px; vertical-align:top; width:${100/columnCount}%;">
              <div><strong>${item.description}</strong></div>
              <div>${item.price}</div>
              <#if item.quantity??>
                <div style="font-size:0.9em; color:#666;">Qty: ${item.quantity}</div>
              </#if>
            </td>
          </#list>
          <#-- Pad empty cells when row is shorter than columnCount -->
          <#if (row?size < columnCount)>
            <#list 1..(columnCount - row?size) as _>
              <td style="border:1px solid #ddd; padding:8px; width:${100/columnCount}%;">&nbsp;</td>
            </#list>
          </#if>
        </tr>
      </#list>
    </tbody>
  </table>

  <div class="total" style="margin-top:20px; font-weight:bold;">
    Total: ${invoice.total}
  </div>
</@layout.layout>
