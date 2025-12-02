<#-- Reusable helper macros -->
<#macro currency amount>
  <#-- Simple currency formatter; expects a numeric string or number -->
  <#if amount?has_content>
    ${amount}
  <#else>
    0.00
  </#if>
</#macro>

<#macro badge text style="info">
  <span class="badge ${style}">${text}</span>
</#macro>

<#--
  columnGrid macro
  - Renders a responsive grid using CSS `grid`. Works well for 1/2/3+ columns.
  - Parameters:
    - items: the sequence to render
    - columns: number of columns (default 2)
    - gap: gap between grid items (default "8px")
    - mode: "grid" (default) or "table" to render as an HTML table
  - Usage: provide a nested block which can reference `item` (the current element).
    The nested block will be inserted for each item; set a local `item` variable
    in the loop so the nested block can reference it.
-->
<#macro columnGrid items columns=2 gap="8px" mode="grid">
  <#if (mode == "table")>
    <table class="grid-table" style="width:100%; border-collapse:collapse;">
      <thead>
        <tr>
          <#-- optional header can be rendered by caller around the macro call -->
        </tr>
      </thead>
      <tbody>
        <#list items?chunk(columns) as row>
          <tr>
            <#list row as it>
              <td style="border:1px solid #ddd; padding:6px; vertical-align:top;">
                <#nested item=it />
              </td>
            </#list>
            <#-- pad empty cells when row shorter than columns -->
            <#if (row?size &lt; columns)>
              <#list 1..(columns - row?size) as _>
                <td style="border:1px solid #ddd; padding:6px;">&nbsp;</td>
              </#list>
            </#if>
          </tr>
        </#list>
      </tbody>
    </table>
  <#else>
    <div class="grid" style="display:grid; grid-template-columns:repeat(${columns}, 1fr); gap:${gap};">
      <#list items as it>
        <div class="grid-item" style="break-inside:avoid;">
          <#nested item=it />
        </div>
      </#list>
    </div>
  </#if>
</#macro>
