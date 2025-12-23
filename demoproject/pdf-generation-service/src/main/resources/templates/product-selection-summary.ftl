<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        @page { size: 8.5in 11in; margin: 0; }
        body { font-family: Arial, sans-serif; margin: 40px; font-size: 11px; }
        h1, h2 { color: #003366; }
        h1 { font-size: 20px; border-bottom: 2px solid #003366; padding-bottom: 10px; margin-bottom: 20px; }
        h2 { font-size: 16px; margin: 25px 0 15px; }
        .summary-box { background: #f5f5f5; border: 1px solid #ddd; border-radius: 5px; padding: 20px; margin: 20px 0; }
        .summary-item { margin: 10px 0; display: flex; justify-content: space-between; }
        .summary-label { font-weight: bold; color: #333; }
        .summary-value { color: #666; }
        .product-section { margin: 25px 0; padding: 15px; border-left: 4px solid #003366; background: #fafafa; }
        .product-title { font-size: 14px; font-weight: bold; color: #003366; margin-bottom: 10px; text-transform: capitalize; }
        .price-total { font: bold 18px Arial; color: #003366; text-align: right; margin-top: 20px; padding-top: 15px; border-top: 2px solid #003366; }
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th { background: #003366; color: white; padding: 10px; text-align: left; font-weight: bold; }
        td { padding: 8px 10px; border-bottom: 1px solid #ddd; }
        tr:hover { background: #f9f9f9; }
    </style>
</head>
<body>
    <h1>Product Selection Summary</h1>
    
    <div class="summary-box">
        <div class="summary-item">
            <span class="summary-label">Market Category:</span>
            <span class="summary-value">${enrollmentContext.marketDisplay!""}</span>
        </div>
        <div class="summary-item">
            <span class="summary-label">State:</span>
            <span class="summary-value">${enrollmentContext.stateFullName!""} (${enrollmentContext.state!""})</span>
        </div>
        <div class="summary-item">
            <span class="summary-label">Products Selected:</span>
            <span class="summary-value">${enrollmentContext.productCount!0}</span>
        </div>
        <div class="summary-item">
            <span class="summary-label">Effective Date:</span>
            <span class="summary-value">${effectiveDate!""}</span>
        </div>
    </div>
    
    <h2>Selected Products & Pricing</h2>
    
    <#if enrollmentContext.hasMedical!false>
    <div class="product-section">
        <div class="product-title">🏥 Medical Coverage</div>
        <table>
            <tr>
                <th>Covered Member</th>
                <th>Plan</th>
                <th>Monthly Premium</th>
            </tr>
            <#if members??>
                <#list members as member>
                    <#if member.medical??>
                    <tr>
                        <td>${member.name!""}</td>
                        <td>${member.medical.planName!""}</td>
                        <td>$${member.medical.premium?string["0.00"]}</td>
                    </tr>
                    </#if>
                </#list>
            </#if>
        </table>
        <#if productSummary.medicalPremiumTotal??>
        <div style="text-align: right; margin-top: 10px; font-weight: bold;">
            Subtotal: $${productSummary.medicalPremiumTotal}
        </div>
        </#if>
    </div>
    </#if>
    
    <#if enrollmentContext.hasDental!false>
    <div class="product-section">
        <div class="product-title">🦷 Dental Coverage</div>
        <table>
            <tr>
                <th>Covered Member</th>
                <th>Plan</th>
                <th>Monthly Premium</th>
            </tr>
            <#if members??>
                <#list members as member>
                    <#if member.dental??>
                    <tr>
                        <td>${member.name!""}</td>
                        <td>${member.dental.planName!""}</td>
                        <td>$${member.dental.premium?string["0.00"]}</td>
                    </tr>
                    </#if>
                </#list>
            </#if>
        </table>
        <#if productSummary.dentalPremiumTotal??>
        <div style="text-align: right; margin-top: 10px; font-weight: bold;">
            Subtotal: $${productSummary.dentalPremiumTotal}
        </div>
        </#if>
    </div>
    </#if>
    
    <#if enrollmentContext.hasVision!false>
    <div class="product-section">
        <div class="product-title">👓 Vision Coverage</div>
        <table>
            <tr>
                <th>Covered Member</th>
                <th>Plan</th>
                <th>Monthly Premium</th>
            </tr>
            <#if members??>
                <#list members as member>
                    <#if member.vision??>
                    <tr>
                        <td>${member.name!""}</td>
                        <td>${member.vision.planName!""}</td>
                        <td>$${member.vision.premium?string["0.00"]}</td>
                    </tr>
                    </#if>
                </#list>
            </#if>
        </table>
        <#if productSummary.visionPremiumTotal??>
        <div style="text-align: right; margin-top: 10px; font-weight: bold;">
            Subtotal: $${productSummary.visionPremiumTotal}
        </div>
        </#if>
    </div>
    </#if>
    
    <div class="price-total">
        Total Monthly Premium: $${productSummary.grandTotalPremium!"0.00"}
    </div>
    
    <div style="margin-top: 30px; padding: 15px; background-color: #fffbcc; border-left: 4px solid #ffcc00;">
        <strong>Note:</strong> This summary shows your selected products and pricing. 
        <#if enrollmentContext.requiresCADisclosures!false>
        California state-specific disclosures are included in this enrollment packet.
        </#if>
        Complete coverage details and benefit information can be found in the following pages.
    </div>
</body>
</html>
