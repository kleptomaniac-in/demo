<#-- Detailed layout: larger spacing, header emphasis -->
<#macro layout title>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${title}</title>
  <style>
    body { font-family: Georgia, serif; padding: 30px; font-size: 14px }
    header { margin-bottom: 20px }
    table { width: 100%; border-collapse: collapse; font-size: 14px }
    th, td { border: 1px solid #ccc; padding: 10px; }
    .total { font-weight: bold; font-size: 18px; margin-top: 20px; }
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
