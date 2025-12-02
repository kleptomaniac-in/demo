<#-- Custom Reusable Components Library -->

<#-- Component 1: Card with optional header and footer -->
<#macro card title="" footer="" bgColor="#ffffff" borderColor="#ddd">
  <div style="border:1px solid ${borderColor}; background:${bgColor}; margin-bottom:10px;">
    <#if title?has_content>
      <div style="background:#f5f5f5; padding:8px; font-weight:bold; border-bottom:1px solid ${borderColor};">
        ${title}
      </div>
    </#if>
    <div style="padding:10px;">
      <#nested> <#-- This is where nested content goes -->
    </div>
    <#if footer?has_content>
      <div style="background:#f5f5f5; padding:6px; font-size:0.9em; border-top:1px solid ${borderColor};">
        ${footer}
      </div>
    </#if>
  </div>
</#macro>

<#-- Component 2: Badge/Tag -->
<#macro badge text type="default">
  <#switch type>
    <#case "success">
      <#assign bg="#28a745" color="#fff" />
      <#break>
    <#case "warning">
      <#assign bg="#ffc107" color="#000" />
      <#break>
    <#case "danger">
      <#assign bg="#dc3545" color="#fff" />
      <#break>
    <#case "info">
      <#assign bg="#17a2b8" color="#fff" />
      <#break>
    <#default>
      <#assign bg="#6c757d" color="#fff" />
  </#switch>
  <span style="display:inline-block; padding:3px 8px; background:${bg}; color:${color}; border-radius:3px; font-size:0.85em;">
    ${text}
  </span>
</#macro>

<#-- Component 3: Dynamic Grid Container -->
<#macro gridContainer columns=2 gap="10px">
  <table style="width:100%; border-collapse:separate; border-spacing:${gap};">
    <tbody>
      <tr>
        <#nested> <#-- Nested content handles the td elements -->
      </tr>
    </tbody>
  </table>
</#macro>

<#-- Component 3b: Grid Cell (to use inside gridContainer) -->
<#macro gridCell width="">
  <td style="vertical-align:top; <#if width?has_content>width:${width};</#if>">
    <#nested>
  </td>
</#macro>

<#-- Component 3c: Auto Grid (simpler version that handles items automatically) -->
<#macro autoGrid columns=2 gap="10px" items=[]>
  <table style="width:100%; border-collapse:separate; border-spacing:${gap};">
    <tbody>
      <#list items?chunk(columns) as row>
        <tr>
          <#list row as item>
            <td style="vertical-align:top; width:${100/columns}%;">
              <#nested item>
            </td>
          </#list>
          <#-- Pad empty cells -->
          <#if (row?size < columns)>
            <#list 1..(columns - row?size) as _>
              <td style="width:${100/columns}%;">&nbsp;</td>
            </#list>
          </#if>
        </tr>
      </#list>
    </tbody>
  </table>
</#macro>

<#-- Component 4: Key-Value Pair Display -->
<#macro keyValue label value labelWidth="30%">
  <table style="width:100%; margin-bottom:5px;">
    <tr>
      <td style="width:${labelWidth}; font-weight:bold; color:#666;">${label}:</td>
      <td>${value}</td>
    </tr>
  </table>
</#macro>

<#-- Component 5: Alert Box -->
<#macro alert message type="info" icon="">
  <#switch type>
    <#case "success">
      <#assign bg="#d4edda" border="#c3e6cb" color="#155724" />
      <#break>
    <#case "warning">
      <#assign bg="#fff3cd" border="#ffeeba" color="#856404" />
      <#break>
    <#case "danger">
      <#assign bg="#f8d7da" border="#f5c6cb" color="#721c24" />
      <#break>
    <#default>
      <#assign bg="#d1ecf1" border="#bee5eb" color="#0c5460" />
  </#switch>
  <div style="padding:10px; background:${bg}; border:1px solid ${border}; border-left:4px solid ${border}; color:${color}; margin:10px 0;">
    <#if icon?has_content><strong>${icon}</strong> </#if>${message}
  </div>
</#macro>

<#-- Component 6: Progress Bar -->
<#macro progressBar percentage label="" height="20px">
  <div style="position:relative; width:100%; height:${height}; background:#e9ecef; border:1px solid #ddd; margin:5px 0;">
    <div style="width:${percentage}%; height:100%; background:#007bff;"></div>
    <#if label?has_content>
      <div style="position:absolute; top:0; left:0; right:0; text-align:center; line-height:${height}; font-size:0.85em;">
        ${label}
      </div>
    </#if>
  </div>
</#macro>

<#-- Component 7: Data Table with Headers (Horizontal) -->
<#macro dataTable headers=[] rows=[] striped=true>
  <table style="width:100%; border-collapse:collapse;">
    <#if headers?has_content>
      <thead>
        <tr style="background:#f5f5f5;">
          <#list headers as header>
            <th style="border:1px solid #ddd; padding:8px; text-align:left; font-weight:bold;">
              ${header}
            </th>
          </#list>
        </tr>
      </thead>
    </#if>
    <tbody>
      <#list rows as row>
        <tr <#if striped && (row?index % 2 == 0)>style="background:#f9f9f9;"</#if>>
          <#list row as cell>
            <td style="border:1px solid #ddd; padding:8px;">
              ${cell}
            </td>
          </#list>
        </tr>
      </#list>
    </tbody>
  </table>
</#macro>

<#-- Component 7b: Vertical Table (headers in first column) -->
<#macro verticalTable data={} headerWidth="30%" striped=false headerBg="#f5f5f5">
  <table style="width:100%; border-collapse:collapse;">
    <tbody>
      <#list data as key, value>
        <tr <#if striped && (key?index % 2 == 0)>style="background:#f9f9f9;"</#if>>
          <td style="border:1px solid #ddd; padding:8px; font-weight:bold; background:${headerBg}; width:${headerWidth};">
            ${key}
          </td>
          <td style="border:1px solid #ddd; padding:8px;">
            ${value}
          </td>
        </tr>
      </#list>
    </tbody>
  </table>
</#macro>

<#-- Component 7c: Key-Value Table (alternative vertical display) -->
<#macro recordTable headers=[] values=[] headerWidth="30%" headerBg="#f5f5f5">
  <table style="width:100%; border-collapse:collapse;">
    <tbody>
      <#list headers as header>
        <tr>
          <td style="border:1px solid #ddd; padding:8px; font-weight:bold; background:${headerBg}; width:${headerWidth};">
            ${header}
          </td>
          <td style="border:1px solid #ddd; padding:8px;">
            ${values[header?index]}
          </td>
        </tr>
      </#list>
    </tbody>
  </table>
</#macro>

<#-- Component 8: Section with Collapsible Content (visual only for PDF) -->
<#macro section title expanded=true>
  <div style="border:1px solid #ddd; margin:10px 0;">
    <div style="background:#007bff; color:#fff; padding:10px; font-weight:bold;">
      ${title}
    </div>
    <#if expanded>
      <div style="padding:15px; background:#fff;">
        <#nested>
      </div>
    </#if>
  </div>
</#macro>

<#-- Component 9: Icon Text (using Unicode symbols) -->
<#macro iconText icon text>
  <span style="display:inline-block;">
    <span style="font-size:1.2em; margin-right:5px;">${icon}</span>
    <span>${text}</span>
  </span>
</#macro>

<#-- Component 10: Stat Card -->
<#macro statCard label value icon="" trend="" trendUp=true>
  <div style="border:1px solid #ddd; padding:15px; background:#f8f9fa; text-align:center;">
    <#if icon?has_content>
      <div style="font-size:2em; margin-bottom:10px;">${icon}</div>
    </#if>
    <div style="font-size:2em; font-weight:bold; color:#007bff;">${value}</div>
    <div style="font-size:0.9em; color:#666; margin-top:5px;">${label}</div>
    <#if trend?has_content>
      <div style="font-size:0.85em; margin-top:5px; color:<#if trendUp>#28a745<#else>#dc3545</#if>;">
        <#if trendUp>▲<#else>▼</#if> ${trend}
      </div>
    </#if>
  </div>
</#macro>
