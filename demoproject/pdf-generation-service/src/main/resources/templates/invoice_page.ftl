<#-- Invoice page that uses layout and macros -->
<#import "/templates/macros.ftl" as m>

<#-- select layout dynamically from model key 'layoutName' (defaults to 'compact') -->
<#-- Use ?default to provide a fallback string when layoutName is missing/null -->
<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Invoice ${invoice.number}">
  <h2>Invoice ${invoice.number}</h2>
  <p>Customer: ${customer.name}</p>

  <table>
    <thead>
      <tr><th>Description</th><th>Price</th></tr>
    </thead>
    <tbody>
      <#list invoice.items as it>
        <tr><td>${it.description}</td><td>${it.price}</td></tr>
      </#list>
    </tbody>
  </table>

  <div class="total">Total: ${invoice.total}</div>
</@layout.layout>
