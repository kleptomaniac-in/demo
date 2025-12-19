<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Enrollment with Functions Demo</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        .section {
            margin: 20px 0;
            padding: 15px;
            border: 1px solid #ddd;
            border-radius: 5px;
        }
        .section-title {
            font-weight: bold;
            font-size: 14pt;
            margin-bottom: 10px;
            color: #333;
        }
        .function-example {
            background-color: #f5f5f5;
            padding: 10px;
            margin: 5px 0;
            border-left: 3px solid #007bff;
        }
        .function-name {
            font-family: monospace;
            color: #007bff;
            font-weight: bold;
        }
        .result {
            color: #28a745;
            font-weight: bold;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 10px 0;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #f8f9fa;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>Field Transformation Functions Demo</h1>
        <h2>${enrollmentContext.marketDisplay} Enrollment</h2>
        <p>State: ${enrollmentContext.stateFullName}</p>
    </div>
    
    <div class="section">
        <div class="section-title">1. String Operations</div>
        
        <div class="function-example">
            <span class="function-name">concat()</span> - Concatenate fields<br>
            Result: <span class="result">${applicants[0].firstName} ${applicants[0].lastName}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">uppercase()</span> - Convert to uppercase<br>
            Email: <span class="result">${applicants[0].email?upper_case}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">lowercase()</span> - Convert to lowercase<br>
            Email: <span class="result">${applicants[0].email?lower_case}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">capitalize()</span> - Capitalize words<br>
            Name: <span class="result">${applicants[0].firstName?capitalize} ${applicants[0].lastName?capitalize}</span>
        </div>
    </div>
    
    <div class="section">
        <div class="section-title">2. Masking Functions (Security)</div>
        
        <div class="function-example">
            <span class="function-name">mask()</span> - Mask sensitive data<br>
            SSN: <span class="result">XXX-XX-${applicants[0].ssn[7..]!''}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">maskEmail()</span> - Mask email<br>
            Email: <span class="result">${applicants[0].email[0]}***@${applicants[0].email?split("@")[1]!''}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">maskPhone()</span> - Mask phone<br>
            Phone: <span class="result">XXX-XXX-${applicants[0].phone?replace("[^0-9]", "", "r")[6..]!''}</span>
        </div>
    </div>
    
    <div class="section">
        <div class="section-title">3. Date Formatting</div>
        
        <div class="function-example">
            <span class="function-name">formatDate()</span> - Format dates<br>
            Date of Birth: <span class="result">${applicants[0].dateOfBirth}</span><br>
            Formatted: <span class="result">${coverageSummary.formattedEffectiveDate!enrollment.effectiveDate}</span>
        </div>
    </div>
    
    <div class="section">
        <div class="section-title">4. Number & Currency Formatting</div>
        
        <div class="function-example">
            <span class="function-name">formatCurrency()</span> - Format as currency<br>
            Total Premium: <span class="result">$${productSummary.grandTotalPremium!"0.00"}</span>
        </div>
        
        <table>
            <tr>
                <th>Product</th>
                <th>Premium</th>
            </tr>
            <#if productSummary.medicalPremiumTotal??>
            <tr>
                <td>Medical</td>
                <td class="result">$${productSummary.medicalPremiumTotal}</td>
            </tr>
            </#if>
            <#if productSummary.dentalPremiumTotal??>
            <tr>
                <td>Dental</td>
                <td class="result">$${productSummary.dentalPremiumTotal}</td>
            </tr>
            </#if>
            <tr>
                <td><strong>Grand Total</strong></td>
                <td class="result"><strong>$${productSummary.grandTotalPremium!"0.00"}</strong></td>
            </tr>
        </table>
    </div>
    
    <div class="section">
        <div class="section-title">5. Conditional & Default Values</div>
        
        <div class="function-example">
            <span class="function-name">default()</span> - Provide default if empty<br>
            Middle Name: <span class="result">${applicants[0].middleName!"N/A"}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">ifEmpty()</span> - Use fallback value<br>
            Nickname: <span class="result">${applicants[0].nickname!applicants[0].firstName}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">coalesce()</span> - First non-empty value<br>
            Contact: <span class="result">${applicants[0].workEmail!applicants[0].personalEmail!applicants[0].email!'No email'}</span>
        </div>
    </div>
    
    <div class="section">
        <div class="section-title">6. Enriched Data with Functions</div>
        
        <table>
            <tr>
                <th>Field</th>
                <th>Value</th>
                <th>Source</th>
            </tr>
            <tr>
                <td>Calculated Age</td>
                <td class="result">${coverageSummary.enrichedApplicants[0].calculatedAge!''} years</td>
                <td>CoverageSummaryEnricher</td>
            </tr>
            <tr>
                <td>Display Name</td>
                <td class="result">${coverageSummary.enrichedApplicants[0].displayName!''}</td>
                <td>CoverageSummaryEnricher</td>
            </tr>
            <tr>
                <td>Market Display</td>
                <td class="result">${enrollmentContext.marketDisplay!''}</td>
                <td>EnrollmentContextEnricher</td>
            </tr>
            <tr>
                <td>Product Flags</td>
                <td class="result">Medical: ${enrollmentContext.hasMedical?c}, Dental: ${enrollmentContext.hasDental?c}</td>
                <td>EnrollmentContextEnricher</td>
            </tr>
            <tr>
                <td>State Info</td>
                <td class="result">${enrollmentContext.stateFullName!''} (${enrollmentContext.state!''})</td>
                <td>EnrollmentContextEnricher</td>
            </tr>
        </table>
    </div>
    
    <div class="section">
        <div class="section-title">7. Combined Functions (Nested)</div>
        
        <div class="function-example">
            <span class="function-name">uppercase(concat(...))</span> - Nested functions<br>
            Result: <span class="result">${(applicants[0].firstName + ' ' + applicants[0].lastName)?upper_case}</span>
        </div>
        
        <div class="function-example">
            <span class="function-name">capitalize(concat(...))</span> - Complex transformation<br>
            Result: <span class="result">${(applicants[0].firstName + ' ' + (applicants[0].middleName!'') + ' ' + applicants[0].lastName)?capitalize}</span>
        </div>
    </div>
</body>
</html>
