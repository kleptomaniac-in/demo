<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        @page {
            size: 8.5in 11in;  /* US Letter explicit dimensions */
            margin: 0;
        }
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }
        .cover-page {
            text-align: center;
            padding-top: 150px;
        }
        h1 {
            font-size: 36px;
            color: #003366;
            margin-bottom: 20px;
        }
        h2 {
            font-size: 24px;
            color: #666;
            margin-bottom: 40px;
        }
        .application-info {
            margin-top: 100px;
            font-size: 18px;
            line-height: 2;
        }
        .label {
            font-weight: bold;
            color: #003366;
        }
        .footer {
            position: fixed;
            bottom: 40px;
            left: 0;
            right: 0;
            text-align: center;
            font-size: 12px;
            color: #999;
        }
    </style>
</head>
<body>
    <div class="cover-page">
        <h1>Health Insurance Enrollment Application</h1>
        <h2>${enrollmentContext.marketDisplay!""} Market - ${enrollmentContext.stateFullName!""}</h2>
        
        <div class="application-info">
            <p><span class="label">Application Number:</span> ${applicationNumber!""}</p>
            <p><span class="label">Submitted Date:</span> ${enrollmentContext.submittedDate!""}</p>
            <p><span class="label">Effective Date:</span> ${enrollmentContext.effectiveDate!""}</p>
            <#if applicants?? && (applicants?size > 0)>
                <#-- Handle both nested demographic and flat structure -->
                <#if applicants[0].demographic??>
                    <p><span class="label">Primary Applicant:</span> ${applicants[0].demographic.firstName!""} ${applicants[0].demographic.lastName!""}</p>
                <#else>
                    <p><span class="label">Primary Applicant:</span> ${applicants[0].firstName!""} ${applicants[0].lastName!""}</p>
                </#if>
            </#if>
            <#if enrollmentContext.productsDisplay??>
                <p><span class="label">Products:</span> ${enrollmentContext.productsDisplay}</p>
            </#if>
        </div>
    </div>
    
    <div class="footer">
        <#if companyInfo??>
            <p>${companyInfo.name!""} | ${companyInfo.phone!""} | ${companyInfo.email!""}</p>
        </#if>
    </div>
</body>
</html>
