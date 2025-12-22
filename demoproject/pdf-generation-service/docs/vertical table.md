The current `dataTable` macro displays horizontal tables (headers on top). Let me create a vertical table macro and update the existing one to support both orientations: 

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/components.ftl#110-110), lines 110 to 135

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/components.ftl#135-135), lines 135 to 155

Using "Replace String in File"


```

```
Now let me create an example showing how to use these vertical table components: 

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/templates/invoice_vertical_table_demo.ftl)

Created [](file:///workspaces/demo/demoproject/pdf-generation-service/request_vertical_table.json)

Perfect! Now you have three ways to create vertical tables:

## **1. Using `verticalTable` (Best for Maps/Hashes)**

```freemarker
<@c.verticalTable 
  data={
    "Invoice Number": invoice.number,
    "Customer Name": customer.name,
    "Total Amount": invoice.total
  }
  headerWidth="35%"
  striped=true
/>
```

**Output:**
```
┌─────────────────────┬──────────────────┐
│ Invoice Number      │ INV-600          │
├─────────────────────┼──────────────────┤
│ Customer Name       │ Acme Corp        │
├─────────────────────┼──────────────────┤
│ Total Amount        │ $450.00          │
└─────────────────────┴──────────────────┘
```

## **2. Using `recordTable` (Best for Arrays)**

```freemarker
<@c.recordTable 
  headers=["Company", "Email", "Phone"]
  values=["Acme Corp", "info@acme.com", "555-1234"]
  headerWidth="30%"
/>
```

## **3. Using Original `keyValue` (Simpler, No Borders)**

```freemarker
<@c.keyValue label="Invoice Number" value=invoice.number />
<@c.keyValue label="Customer" value=customer.name />
```

## **Test It:**

```bash
curl -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request_vertical_table.json \
  --output invoice_vertical.pdf
```

The demo shows:
- Single vertical tables for records
- Side-by-side vertical tables (billing/shipping addresses)
- Mixed layouts (vertical + horizontal)
- Striped rows
- Custom header colors and widths

Made changes.