<#-- Invoice page that uses layout and macros -->
<#import "/templates/macros.ftl" as m>

<#-- select layout dynamically from model key 'layoutName' (defaults to 'compact') -->
<#-- Use ?default to provide a fallback string when layoutName is missing/null -->
<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Invoice ${invoice.number}">
  <h2>Invoice ${invoice.number}</h2>
  <p>Customer: ${customer.name}</p>

 <div class="grid" style="display:grid; grid-template-columns:repeat(${(payload.columns?default("2"))?number}, 1fr); gap:10px;">
  <#list invoice.items as it>
    <div style="border:1px solid #ddd; padding:8px;">
      <div><strong>${it.description}</strong></div>
      <div>${it.price}</div>
    </div>
  </#list>
</div>

  <div class="total">Total: ${invoice.total}</div>
</@layout.layout>
