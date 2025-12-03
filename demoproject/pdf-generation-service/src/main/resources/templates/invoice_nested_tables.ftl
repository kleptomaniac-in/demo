<#-- Nested Tables Examples -->
<#import "/templates/components.ftl" as c>
<#import "/templates/functions.ftl" as f>

<#assign chosenLayout = layoutName?default("compact")>
<#import "/templates/layouts/${chosenLayout}/layout.ftl" as layout>

<@layout.layout title="Nested Tables Demo - Invoice ${invoice.number}">
  
  <h2>Invoice ${invoice.number} - Nested Tables Demo</h2>

  <#-- Example 1: Simple nested table - Invoice items with details -->
  <@c.section title="Example 1: Items with Sub-Items">
    <table style="width:100%; border-collapse:collapse;">
      <thead>
        <tr style="background:#f5f5f5;">
          <th style="border:1px solid #ddd; padding:8px; text-align:left;">Item</th>
          <th style="border:1px solid #ddd; padding:8px; text-align:right; width:15%;">Total</th>
        </tr>
      </thead>
      <tbody>
        <#list invoice.items as item>
          <tr>
            <td style="border:1px solid #ddd; padding:8px;">
              <div style="font-weight:bold; margin-bottom:5px;">${item.description}</div>
              
              <#-- NESTED TABLE for sub-items/details -->
              <table style="width:100%; border-collapse:collapse; margin-top:5px; background:#f9f9f9;">
                <tr>
                  <td style="border:1px solid #e0e0e0; padding:4px; width:30%;">Quantity:</td>
                  <td style="border:1px solid #e0e0e0; padding:4px;">${item.quantity!1}</td>
                </tr>
                <tr>
                  <td style="border:1px solid #e0e0e0; padding:4px;">Unit Price:</td>
                  <td style="border:1px solid #e0e0e0; padding:4px;">${item.price}</td>
                </tr>
                <tr>
                  <td style="border:1px solid #e0e0e0; padding:4px;">Status:</td>
                  <td style="border:1px solid #e0e0e0; padding:4px;">
                    <span style="color:#28a745;">✓ Completed</span>
                  </td>
                </tr>
              </table>
            </td>
            <td style="border:1px solid #ddd; padding:8px; text-align:right; vertical-align:top; font-weight:bold;">
              ${item.price}
            </td>
          </tr>
        </#list>
      </tbody>
    </table>
  </@c.section>

  <#-- Example 2: Multi-level nested tables -->
  <@c.section title="Example 2: Three-Level Nested Structure">
    <table style="width:100%; border-collapse:collapse;">
      <tr>
        <td style="border:2px solid #007bff; padding:10px;">
          <strong>Level 1: Customer Information</strong>
          
          <#-- LEVEL 2 NESTED TABLE -->
          <table style="width:100%; border-collapse:collapse; margin-top:10px;">
            <tr>
              <td style="border:1px solid #ddd; padding:8px; width:50%; vertical-align:top;">
                <strong>Billing Address</strong>
                
                <#-- LEVEL 3 NESTED TABLE -->
                <table style="width:100%; border-collapse:collapse; margin-top:5px; background:#f9f9f9;">
                  <tr>
                    <td style="padding:4px; border-bottom:1px solid #e0e0e0;">123 Main St</td>
                  </tr>
                  <tr>
                    <td style="padding:4px; border-bottom:1px solid #e0e0e0;">New York, NY 10001</td>
                  </tr>
                  <tr>
                    <td style="padding:4px;">USA</td>
                  </tr>
                </table>
              </td>
              <td style="border:1px solid #ddd; padding:8px; width:50%; vertical-align:top;">
                <strong>Shipping Address</strong>
                
                <#-- LEVEL 3 NESTED TABLE -->
                <table style="width:100%; border-collapse:collapse; margin-top:5px; background:#f9f9f9;">
                  <tr>
                    <td style="padding:4px; border-bottom:1px solid #e0e0e0;">456 Oak Ave</td>
                  </tr>
                  <tr>
                    <td style="padding:4px; border-bottom:1px solid #e0e0e0;">Boston, MA 02101</td>
                  </tr>
                  <tr>
                    <td style="padding:4px;">USA</td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </@c.section>

  <#-- Example 3: Nested table with grouped items -->
  <@c.section title="Example 3: Grouped Items with Category Breakdown">
    <table style="width:100%; border-collapse:collapse;">
      <thead>
        <tr style="background:#007bff; color:#fff;">
          <th style="border:1px solid #0056b3; padding:8px; text-align:left;">Category</th>
          <th style="border:1px solid #0056b3; padding:8px; text-align:right; width:15%;">Subtotal</th>
        </tr>
      </thead>
      <tbody>
        <#-- Category 1: Development -->
        <tr>
          <td style="border:1px solid #ddd; padding:10px;" colspan="2">
            <div style="font-weight:bold; font-size:1.1em; margin-bottom:8px; color:#007bff;">
              💻 Development Services
            </div>
            
            <#-- NESTED TABLE for category items -->
            <table style="width:100%; border-collapse:collapse; background:#f8f9fa;">
              <thead>
                <tr style="background:#e9ecef;">
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:left;">Item</th>
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:center; width:10%;">Qty</th>
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:right; width:15%;">Price</th>
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:right; width:15%;">Total</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="border:1px solid #dee2e6; padding:6px;">Web Development</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:center;">1</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right;">$200.00</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold;">$200.00</td>
                </tr>
                <tr>
                  <td style="border:1px solid #dee2e6; padding:6px;">API Integration</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:center;">1</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right;">$150.00</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold;">$150.00</td>
                </tr>
                <tr style="background:#fff;">
                  <td colspan="3" style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold;">
                    Category Subtotal:
                  </td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold; color:#007bff;">
                    $350.00
                  </td>
                </tr>
              </tbody>
            </table>
          </td>
        </tr>
        
        <#-- Category 2: Testing -->
        <tr>
          <td style="border:1px solid #ddd; padding:10px;" colspan="2">
            <div style="font-weight:bold; font-size:1.1em; margin-bottom:8px; color:#28a745;">
              🧪 Quality Assurance
            </div>
            
            <#-- NESTED TABLE for category items -->
            <table style="width:100%; border-collapse:collapse; background:#f8f9fa;">
              <thead>
                <tr style="background:#e9ecef;">
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:left;">Item</th>
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:center; width:10%;">Qty</th>
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:right; width:15%;">Price</th>
                  <th style="border:1px solid #dee2e6; padding:6px; text-align:right; width:15%;">Total</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style="border:1px solid #dee2e6; padding:6px;">Testing</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:center;">2</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right;">$25.00</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold;">$50.00</td>
                </tr>
                <tr>
                  <td style="border:1px solid #dee2e6; padding:6px;">Documentation</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:center;">1</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right;">$50.00</td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold;">$50.00</td>
                </tr>
                <tr style="background:#fff;">
                  <td colspan="3" style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold;">
                    Category Subtotal:
                  </td>
                  <td style="border:1px solid #dee2e6; padding:6px; text-align:right; font-weight:bold; color:#28a745;">
                    $100.00
                  </td>
                </tr>
              </tbody>
            </table>
          </td>
        </tr>
        
        <#-- Grand Total Row -->
        <tr style="background:#f5f5f5;">
          <td style="border:2px solid #007bff; padding:10px; text-align:right; font-size:1.2em; font-weight:bold;">
            GRAND TOTAL:
          </td>
          <td style="border:2px solid #007bff; padding:10px; text-align:right; font-size:1.3em; font-weight:bold; color:#007bff;">
            ${invoice.total}
          </td>
        </tr>
      </tbody>
    </table>
  </@c.section>

  <#-- Example 4: Nested table with mixed layouts -->
  <@c.section title="Example 4: Complex Mixed Layout">
    <table style="width:100%; border-collapse:collapse;">
      <tr>
        <td style="border:1px solid #ddd; padding:10px; width:60%;">
          <strong style="color:#007bff;">Order Summary</strong>
          
          <#-- Vertical nested table -->
          <table style="width:100%; border-collapse:collapse; margin-top:8px;">
            <tr>
              <td style="border:1px solid #e0e0e0; padding:6px; width:40%; background:#f9f9f9; font-weight:bold;">
                Order ID:
              </td>
              <td style="border:1px solid #e0e0e0; padding:6px;">${invoice.number}</td>
            </tr>
            <tr>
              <td style="border:1px solid #e0e0e0; padding:6px; background:#f9f9f9; font-weight:bold;">
                Customer:
              </td>
              <td style="border:1px solid #e0e0e0; padding:6px;">${customer.name}</td>
            </tr>
            <tr>
              <td style="border:1px solid #e0e0e0; padding:6px; background:#f9f9f9; font-weight:bold;">
                Date:
              </td>
              <td style="border:1px solid #e0e0e0; padding:6px;">${invoice.date}</td>
            </tr>
          </table>
        </td>
        <td style="border:1px solid #ddd; padding:10px; width:40%; vertical-align:top;">
          <strong style="color:#28a745;">Payment Details</strong>
          
          <#-- Nested table with calculations -->
          <table style="width:100%; border-collapse:collapse; margin-top:8px;">
            <tr>
              <td style="padding:4px;">Subtotal:</td>
              <td style="padding:4px; text-align:right;">$400.00</td>
            </tr>
            <tr>
              <td style="padding:4px;">Tax (8%):</td>
              <td style="padding:4px; text-align:right;">$32.00</td>
            </tr>
            <tr>
              <td style="padding:4px;">Discount:</td>
              <td style="padding:4px; text-align:right; color:#dc3545;">-$25.00</td>
            </tr>
            <tr style="border-top:2px solid #28a745;">
              <td style="padding:6px; font-weight:bold; font-size:1.1em;">Total:</td>
              <td style="padding:6px; text-align:right; font-weight:bold; font-size:1.2em; color:#28a745;">
                ${invoice.total}
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </@c.section>

  <#-- Example 5: Dynamic nested tables with loops -->
  <@c.section title="Example 5: Dynamic Nested Items (Loop-based)">
    <table style="width:100%; border-collapse:collapse;">
      <thead>
        <tr style="background:#6c757d; color:#fff;">
          <th style="border:1px solid #5a6268; padding:8px; text-align:left;">Service Package</th>
          <th style="border:1px solid #5a6268; padding:8px; text-align:right; width:15%;">Package Total</th>
        </tr>
      </thead>
      <tbody>
        <#assign packages = [
          {
            "name": "Basic Package",
            "icon": "📦",
            "items": [
              {"name": "Setup", "price": "$100"},
              {"name": "Configuration", "price": "$50"}
            ],
            "total": "$150"
          },
          {
            "name": "Premium Package",
            "icon": "⭐",
            "items": [
              {"name": "Advanced Setup", "price": "$200"},
              {"name": "Custom Config", "price": "$100"},
              {"name": "Priority Support", "price": "$150"}
            ],
            "total": "$450"
          }
        ] />
        
        <#list packages as package>
          <tr>
            <td style="border:1px solid #ddd; padding:10px;" colspan="2">
              <div style="font-weight:bold; font-size:1.1em; margin-bottom:5px;">
                ${package.icon} ${package.name}
              </div>
              
              <#-- NESTED TABLE for package items (dynamic) -->
              <table style="width:100%; border-collapse:collapse; background:#f8f9fa;">
                <tbody>
                  <#list package.items as item>
                    <tr>
                      <td style="border:1px solid #e0e0e0; padding:6px; width:70%;">
                        → ${item.name}
                      </td>
                      <td style="border:1px solid #e0e0e0; padding:6px; text-align:right; width:30%;">
                        ${item.price}
                      </td>
                    </tr>
                  </#list>
                  <tr style="background:#fff;">
                    <td style="border:1px solid #6c757d; padding:6px; font-weight:bold; text-align:right;">
                      Package Total:
                    </td>
                    <td style="border:1px solid #6c757d; padding:6px; text-align:right; font-weight:bold; color:#6c757d;">
                      ${package.total}
                    </td>
                  </tr>
                </tbody>
              </table>
            </td>
          </tr>
        </#list>
      </tbody>
    </table>
  </@c.section>

</@layout.layout>
