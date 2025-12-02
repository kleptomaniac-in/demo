<#-- Example: Vertical Tables Demo -->
<#import "/templates/components.ftl" as c>

<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Vertical Table Demo - Invoice ${invoice.number}">
  
  <h2>Invoice ${invoice.number}</h2>

  <#-- Method 1: Using verticalTable with a map/hash -->
  <@c.section title="Invoice Details (Using verticalTable with Map)">
    <@c.verticalTable 
      data={
        "Invoice Number": invoice.number,
        "Customer Name": customer.name,
        "Invoice Date": invoice.date,
        "Total Amount": invoice.total,
        "Payment Status": "Paid",
        "Payment Method": "Credit Card",
        "Due Date": "2025-12-15"
      }
      headerWidth="35%"
      striped=true
    />
  </@c.section>

  <#-- Method 2: Using recordTable with separate headers and values -->
  <@c.section title="Customer Information (Using recordTable)">
    <@c.recordTable 
      headers=["Company Name", "Contact Person", "Email", "Phone", "Address", "City"]
      values=[customer.name, "John Doe", "john@acme.com", "+1 (555) 123-4567", "123 Main St", "New York"]
      headerWidth="30%"
      headerBg="#e7f3ff"
    />
  </@c.section>

  <#-- Method 3: Building a vertical table from dynamic data -->
  <@c.section title="Invoice Summary (Dynamic Vertical Table)">
    <#assign summaryData = {
      "Subtotal": "$400.00",
      "Tax (10%)": "$40.00",
      "Discount": "-$10.00",
      "Shipping": "$20.00",
      "Total": invoice.total
    } />
    
    <@c.verticalTable 
      data=summaryData
      headerWidth="40%"
      headerBg="#fff9e6"
    />
  </@c.section>

  <#-- Method 4: Multiple vertical tables side by side -->
  <div style="margin-top:20px;">
    <table style="width:100%; border-collapse:separate; border-spacing:15px;">
      <tr>
        <td style="width:50%; vertical-align:top;">
          <@c.card title="Billing Address" borderColor="#007bff">
            <@c.verticalTable 
              data={
                "Street": "123 Main St",
                "City": "New York",
                "State": "NY",
                "ZIP": "10001",
                "Country": "USA"
              }
              headerWidth="35%"
            />
          </@c.card>
        </td>
        <td style="width:50%; vertical-align:top;">
          <@c.card title="Shipping Address" borderColor="#007bff">
            <@c.verticalTable 
              data={
                "Street": "456 Oak Ave",
                "City": "Boston",
                "State": "MA",
                "ZIP": "02101",
                "Country": "USA"
              }
              headerWidth="35%"
            />
          </@c.card>
        </td>
      </tr>
    </table>
  </div>

  <#-- Method 5: Vertical table with conditional values -->
  <@c.section title="Project Details">
    <#assign projectInfo = {} />
    <#assign projectInfo = projectInfo + {"Project Name": "Website Redesign"} />
    <#assign projectInfo = projectInfo + {"Start Date": "2025-11-01"} />
    <#assign projectInfo = projectInfo + {"End Date": "2025-12-15"} />
    <#assign projectInfo = projectInfo + {"Status": "In Progress"} />
    <#assign projectInfo = projectInfo + {"Completion": "75%"} />
    
    <@c.verticalTable 
      data=projectInfo
      headerWidth="30%"
      striped=true
      headerBg="#d4edda"
    />
  </@c.section>

  <#-- Regular horizontal table for comparison -->
  <@c.section title="Invoice Items (Horizontal Table for Comparison)">
    <@c.dataTable 
      headers=["Description", "Quantity", "Unit Price", "Total"]
      rows=[
        ["Web Development", "1", "$200.00", "$200.00"],
        ["API Integration", "1", "$150.00", "$150.00"],
        ["Testing", "2", "$25.00", "$50.00"],
        ["Documentation", "1", "$50.00", "$50.00"]
      ]
      striped=true
    />
  </@c.section>

  <#-- Mixed layout: Vertical table for record, horizontal for list -->
  <div style="margin-top:20px;">
    <table style="width:100%; border-collapse:separate; border-spacing:15px;">
      <tr>
        <td style="width:40%; vertical-align:top;">
          <h3>Order Details</h3>
          <@c.verticalTable 
            data={
              "Order ID": "ORD-12345",
              "Date": invoice.date,
              "Items": invoice.items?size?string,
              "Total": invoice.total
            }
            headerWidth="45%"
            headerBg="#f0f0f0"
          />
        </td>
        <td style="width:60%; vertical-align:top;">
          <h3>Items List</h3>
          <@c.dataTable 
            headers=["Item", "Price"]
            rows=[
              [invoice.items[0].description, invoice.items[0].price],
              [invoice.items[1].description, invoice.items[1].price],
              [invoice.items[2].description, invoice.items[2].price],
              [invoice.items[3].description, invoice.items[3].price]
            ]
          />
        </td>
      </tr>
    </table>
  </div>

</@layout.layout>
