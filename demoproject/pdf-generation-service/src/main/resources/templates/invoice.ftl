<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Invoice (Freemarker)</title>
  <style>
    body { font-family: Arial, Helvetica, sans-serif; padding: 20px; }
    .header { font-size: 20px; margin-bottom: 10px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { border: 1px solid #ddd; padding: 8px; }
    .total { font-weight: bold; font-size: 18px; margin-top: 20px; }
  </style>
</head>
<body>
  <div class="header">Invoice for ${customer.name}</div>
  <div>Invoice #: ${invoice.number}</div>
  <div>Date: ${invoice.date}</div>
  <hr />

  <div>Items:</div>
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
</body>
</html>
