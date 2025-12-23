<#-- 
  Optimized CSS Styles Library for FreeMarker Templates
  
  These macros provide pre-optimized CSS that follows best practices:
  - Consolidated selectors to reduce redundancy
  - CSS shorthand properties for smaller payload
  - No expensive properties (position:fixed, complex transforms)
  - Optimized for OpenHTMLToPDF performance
  
  Usage in templates:
  <#import "styles.ftl" as styles>
  <@styles.basePageStyles />
  <@styles.enrollmentStyles />
  <@styles.tableStyles />
-->

<#-- Base page setup with US Letter dimensions -->
<#macro basePageStyles>
@page { size: 8.5in 11in; margin: 0; }
body { font-family: Arial, sans-serif; margin: 40px; font-size: 11px; }
</#macro>

<#-- Common heading styles (optimized with grouped selectors) -->
<#macro headingStyles colorPrimary="#003366">
h1, h2, h3 { color: ${colorPrimary}; }
h1 { font-size: 20px; border-bottom: 2px solid ${colorPrimary}; padding-bottom: 10px; margin-bottom: 20px; }
h2 { font-size: 16px; margin: 25px 0 15px; }
h3 { font-size: 14px; margin: 20px 0 10px; }
</#macro>

<#-- Enrollment-specific styles -->
<#macro enrollmentStyles colorPrimary="#003366" colorSecondary="#0066cc">
.cover-page { text-align: center; }
.cover-page h1 { font-size: 28px; margin: 150px 0 20px; }
.cover-page h2 { font-size: 18px; color: #666; margin: 10px 0; }
.cover-page .footer { margin-top: 200px; font-size: 10px; color: #999; }
.section { margin-bottom: 20px; }
.important { font-weight: bold; color: #cc0000; }
</#macro>

<#-- Table styles (optimized) -->
<#macro tableStyles colorHeader="#003366">
table { width: 100%; border-collapse: collapse; margin: 10px 0; }
th { background: ${colorHeader}; color: white; padding: 10px; text-align: left; font-weight: bold; }
td { padding: 8px 10px; border-bottom: 1px solid #ddd; }
tr:nth-child(even) { background: #f9f9f9; }
tr:hover { background: #f5f5f5; }
</#macro>

<#-- Summary box styles -->
<#macro summaryBoxStyles>
.summary-box { background: #f5f5f5; border: 1px solid #ddd; border-radius: 5px; padding: 20px; margin: 20px 0; }
.summary-item { margin: 10px 0; display: flex; justify-content: space-between; }
.summary-label { font-weight: bold; color: #333; }
.summary-value { color: #666; }
</#macro>

<#-- Info section styles -->
<#macro infoSectionStyles colorAccent="#0066cc">
.info-section { margin: 20px 0; padding: 15px; background: #f0f8ff; border-left: 4px solid ${colorAccent}; }
.note-section { margin: 20px 0; padding: 15px; background: #fffbcc; border-left: 4px solid #ffcc00; }
.warning-section { margin: 20px 0; padding: 15px; background: #ffe6e6; border-left: 4px solid #cc0000; }
</#macro>

<#-- List styles -->
<#macro listStyles>
ul { margin: 10px 0; padding-left: 20px; }
li { margin-bottom: 8px; }
p { line-height: 1.4; }
</#macro>

<#-- Product section styles -->
<#macro productSectionStyles colorPrimary="#003366">
.product-section { margin: 25px 0; padding: 15px; border-left: 4px solid ${colorPrimary}; background: #fafafa; }
.product-title { font-size: 14px; font-weight: bold; color: ${colorPrimary}; margin-bottom: 10px; text-transform: capitalize; }
.price-total { font: bold 18px Arial; color: ${colorPrimary}; text-align: right; margin-top: 20px; padding-top: 15px; border-top: 2px solid ${colorPrimary}; }
</#macro>

<#-- Complete enrollment document styles (all-in-one) -->
<#macro completeEnrollmentStyles colorPrimary="#003366" colorSecondary="#0066cc">
<style>
<@basePageStyles />
<@headingStyles colorPrimary />
<@enrollmentStyles colorPrimary colorSecondary />
<@tableStyles colorPrimary />
<@summaryBoxStyles />
<@infoSectionStyles colorSecondary />
<@listStyles />
<@productSectionStyles colorPrimary />
</style>
</#macro>

<#-- Minimal document styles (for simple pages) -->
<#macro minimalStyles colorPrimary="#003366">
<style>
<@basePageStyles />
<@headingStyles colorPrimary />
<@tableStyles colorPrimary />
<@listStyles />
</style>
</#macro>

<#-- Terms and conditions specific styles -->
<#macro termsStyles>
<style>
<@basePageStyles />
body { font-size: 10px; }
h1, h2 { color: #003366; }
h1 { font-size: 16px; border-bottom: 2px solid #003366; padding-bottom: 8px; }
h2 { font-size: 12px; margin-top: 15px; }
p { line-height: 1.4; text-align: justify; }
.section { margin-bottom: 20px; }
.important { font-weight: bold; color: #cc0000; }
<@listStyles />
</style>
</#macro>
