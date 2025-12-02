<#-- Layout macro providing a page skeleton; pages use <@layout.title> with nested content -->
<#macro layout title>
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${title}</title>
  <style>
    body { font-family: Arial, Helvetica, sans-serif; padding: 20px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { border: 1px solid #ddd; padding: 8px; }
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
