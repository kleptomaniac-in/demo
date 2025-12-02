<#-- Compact layout: tighter spacing -->
<#macro layout title>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${title}</title>
  <style>
    body { font-family: Arial, Helvetica, sans-serif; padding: 10px; font-size: 12px }
    table { width: 100%; border-collapse: collapse; font-size: 12px }
    th, td { border: 1px solid #ddd; padding: 6px; }
    .total { font-weight: bold; font-size: 14px; margin-top: 10px; }
  </style>
</head>
<body>
  <#include "/templates/header.ftl" />
  <main>
    <#nested />
  </main>
  <#include "/templates/footer.ftl" />
</body>
</html>
</#macro>
