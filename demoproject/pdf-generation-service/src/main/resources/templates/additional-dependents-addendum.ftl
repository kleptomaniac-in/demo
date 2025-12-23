<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1, h2 { color: #0066cc; }
        h1 { text-align: center; color: #333; border-bottom: 3px solid #0066cc; padding-bottom: 10px; }
        h2 { margin-top: 30px; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th { background: #0066cc; color: white; padding: 12px; text-align: left; font-weight: bold; }
        td { border: 1px solid #ddd; padding: 10px; }
        tr:nth-child(even) { background: #f9f9f9; }
        .info-section { margin: 20px 0; padding: 15px; background: #f0f8ff; border-left: 4px solid #0066cc; }
        .page-break { page-break-after: always; }
    </style>
</head>
<body>
    <#-- Filter to get only DEPENDENT applicants -->
    <#assign allDependents = application.applicants?filter(a -> a.relationship == "DEPENDENT")>
    <#-- Skip first 3 (already on main form) -->
    <#assign additionalDependents = allDependents[3..]>
    
    <#if (additionalDependents?size > 0)>
    <h1>Additional Dependents Addendum</h1>
    
    <div class="info-section">
        <p><strong>Application ID:</strong> ${application.applicationId}</p>
        <p><strong>Primary Applicant:</strong> 
            <#assign primary = application.applicants?filter(a -> a.relationship == "PRIMARY")?first>
            ${primary.demographic.firstName} ${primary.demographic.lastName}
        </p>
        <p><strong>Total Dependents:</strong> ${allDependents?size}</p>
        <p><strong>Dependents on Main Form:</strong> 3</p>
        <p><strong>Additional Dependents:</strong> ${additionalDependents?size}</p>
    </div>
    
    <h2>Additional Dependent Information</h2>
    <p>The following dependents could not fit on the main enrollment application form and are listed below:</p>
    
    <table>
        <thead>
            <tr>
                <th>Dependent #</th>
                <th>First Name</th>
                <th>Middle Name</th>
                <th>Last Name</th>
                <th>Date of Birth</th>
                <th>SSN</th>
                <th>Gender</th>
            </tr>
        </thead>
        <tbody>
            <#list additionalDependents as dependent>
            <tr>
                <td>${dependent?index + 4}</td>
                <td>${dependent.demographic.firstName}</td>
                <td>${dependent.demographic.middleName!""}</td>
                <td>${dependent.demographic.lastName}</td>
                <td>${dependent.demographic.dateOfBirth}</td>
                <td>${dependent.demographic.ssn}</td>
                <td>${dependent.demographic.gender}</td>
            </tr>
            </#list>
        </tbody>
    </table>
    
    <#-- Check which products cover these dependents -->
    <h2>Coverage Details for Additional Dependents</h2>
    <#list additionalDependents as dependent>
        <h3>Dependent #${dependent?index + 4}: ${dependent.demographic.firstName} ${dependent.demographic.lastName}</h3>
        
        <table>
            <thead>
                <tr>
                    <th>Product Type</th>
                    <th>Plan Name</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <#list application.proposedProducts as product>
                    <#if product.coveredApplicants?seq_contains(dependent.applicantId)>
                    <tr>
                        <td>${product.productType}</td>
                        <td>${product.planName}</td>
                        <td style="color: green; font-weight: bold;">✓ Covered</td>
                    </tr>
                    <#else>
                    <tr>
                        <td>${product.productType}</td>
                        <td>${product.planName}</td>
                        <td style="color: #999;">Not Covered</td>
                    </tr>
                    </#if>
                </#list>
            </tbody>
        </table>
    </#list>
    
    <div class="info-section" style="margin-top: 40px;">
        <p><strong>Note:</strong> This addendum is an integral part of the enrollment application. 
        All information provided herein is subject to the same terms and conditions as the main application.</p>
        <p><strong>Effective Date:</strong> ${application.effectiveDate}</p>
    </div>
    
    <#else>
    <!-- No additional dependents beyond the 3 on main form -->
    </#if>
</body>
</html>
