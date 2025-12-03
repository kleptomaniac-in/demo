<#-- FreeMarker Functions Library -->

<#-- ============================================
     CALCULATION FUNCTIONS
     ============================================ -->

<#function calculateSubtotal items>
  <#assign total = 0 />
  <#list items as item>
    <#assign qty = item.quantity!1 />
    <#assign price = item.price?replace("$", "")?replace(",", "")?number />
    <#assign total = total + (price * qty) />
  </#list>
  <#return total>
</#function>

<#function calculateTax amount rate=0.10>
  <#return amount * rate>
</#function>

<#function applyDiscount amount discountPercent>
  <#return amount * (1 - discountPercent/100)>
</#function>

<#function calculateTotal subtotal tax discount=0>
  <#return subtotal + tax - discount>
</#function>

<#-- ============================================
     FORMATTING FUNCTIONS
     ============================================ -->

<#function formatCurrency value>
  <#return "$" + value?string("0.00")>
</#function>

<#function formatPercentage value decimals=1>
  <#-- Build format string manually since ?repeat doesn't exist -->
  <#if decimals == 0>
    <#return value?string("0") + "%">
  <#elseif decimals == 1>
    <#return value?string("0.0") + "%">
  <#elseif decimals == 2>
    <#return value?string("0.00") + "%">
  <#else>
    <#return value?string("0.00") + "%">
  </#if>
</#function>

<#function formatDate dateString format="MM/dd/yyyy">
  <#-- Simple format for demo - in real use, parse properly -->
  <#return dateString>
</#function>

<#function truncateText text maxLength suffix="...">
  <#if text?length > maxLength>
    <#return text?substring(0, maxLength)?trim + suffix>
  <#else>
    <#return text>
  </#if>
</#function>

<#function padLeft text width padChar=" ">
  <#if text?length >= width>
    <#return text>
  <#else>
    <#-- Build padding manually -->
    <#assign padding = "" />
    <#assign needed = width - text?length />
    <#list 1..needed as i>
      <#assign padding = padding + padChar />
    </#list>
    <#return padding + text>
  </#if>
</#function>

<#-- ============================================
     VALIDATION FUNCTIONS
     ============================================ -->

<#function isValidEmail email>
  <#return email?? && email?matches(r"^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")>
</#function>

<#function isNotEmpty value>
  <#return value?? && value?has_content>
</#function>

<#function isPositive number>
  <#return number?? && number?number > 0>
</#function>

<#function hasMinLength text minLength>
  <#return text?? && text?length >= minLength>
</#function>

<#-- ============================================
     CONDITIONAL FUNCTIONS
     ============================================ -->

<#function getStatusColor status>
  <#assign statusLower = status?lower_case?trim />
  <#switch statusLower>
    <#case "paid">
    <#case "completed">
    <#case "success">
      <#return "#28a745">
    <#case "pending">
    <#case "processing">
    <#case "warning">
      <#return "#ffc107">
    <#case "overdue">
    <#case "failed">
    <#case "error">
    <#case "danger">
      <#return "#dc3545">
    <#case "draft">
    <#case "inactive">
      <#return "#6c757d">
    <#default>
      <#return "#007bff">
  </#switch>
</#function>

<#function getStatusIcon status>
  <#assign statusLower = status?lower_case?trim />
  <#switch statusLower>
    <#case "paid">
    <#case "completed">
      <#return "✓">
    <#case "pending">
      <#return "⏱">
    <#case "overdue">
    <#case "failed">
      <#return "✗">
    <#case "draft">
      <#return "📝">
    <#default>
      <#return "•">
  </#switch>
</#function>

<#function getPriorityLabel priority>
  <#switch priority?lower_case>
    <#case "high">
    <#case "urgent">
      <#return "🔴 HIGH">
    <#case "medium">
      <#return "🟡 MEDIUM">
    <#case "low">
      <#return "🟢 LOW">
    <#default>
      <#return "⚪ NORMAL">
  </#switch>
</#function>

<#-- ============================================
     ARRAY/LIST FUNCTIONS
     ============================================ -->

<#function countItems items>
  <#if items??>
    <#return items?size>
  <#else>
    <#return 0>
  </#if>
</#function>

<#function getFirst list default="">
  <#if list?? && list?has_content>
    <#return list?first>
  <#else>
    <#return default>
  </#if>
</#function>

<#function getLast list default="">
  <#if list?? && list?has_content>
    <#return list?last>
  <#else>
    <#return default>
  </#if>
</#function>

<#function contains list item>
  <#if !list?? || !list?has_content>
    <#return false>
  </#if>
  <#list list as element>
    <#if element == item>
      <#return true>
    </#if>
  </#list>
  <#return false>
</#function>

<#-- ============================================
     UTILITY FUNCTIONS
     ============================================ -->

<#function defaultValue value defaultVal>
  <#if value?? && value?has_content>
    <#return value>
  <#else>
    <#return defaultVal>
  </#if>
</#function>

<#function coalesce values...>
  <#list values as value>
    <#if value?? && value?has_content>
      <#return value>
    </#if>
  </#list>
  <#return "">
</#function>

<#function ternary condition trueValue falseValue>
  <#if condition>
    <#return trueValue>
  <#else>
    <#return falseValue>
  </#if>
</#function>

<#function randomId prefix="id">
  <#return prefix + "-" + .now?long?string>
</#function>

<#-- ============================================
     MATH FUNCTIONS
     ============================================ -->

<#function pow base exponent>
  <#-- Manual power calculation since ?pow doesn't exist -->
  <#if exponent == 0>
    <#return 1>
  </#if>
  <#assign result = 1 />
  <#list 1..exponent as i>
    <#assign result = result * base />
  </#list>
  <#return result>
</#function>

<#function round value decimals=2>
  <#assign multiplier = pow(10, decimals) />
  <#return (value * multiplier)?round / multiplier>
</#function>

<#function min a b>
  <#if a < b>
    <#return a>
  <#else>
    <#return b>
  </#if>
</#function>

<#function max a b>
  <#if a > b>
    <#return a>
  <#else>
    <#return b>
  </#if>
</#function>

<#function abs value>
  <#if value < 0>
    <#return -value>
  <#else>
    <#return value>
  </#if>
</#function>

<#function percentage part total>
  <#if total == 0>
    <#return 0>
  <#else>
    <#return (part / total) * 100>
  </#if>
</#function>
