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
            font-size: 11px;
        }
        h1 {
            font-size: 18px;
            color: #003366;
            border-bottom: 2px solid #003366;
            padding-bottom: 10px;
        }
        h2 {
            font-size: 14px;
            color: #003366;
            margin-top: 20px;
        }
        p {
            line-height: 1.6;
            text-align: justify;
        }
        .section {
            margin-bottom: 30px;
        }
        .important {
            font-weight: bold;
            color: #cc0000;
        }
        ul {
            margin: 10px 0;
            padding-left: 20px;
        }
        li {
            margin-bottom: 8px;
        }
    </style>
</head>
<body>
    <h1>Terms and Conditions</h1>
    
    <div class="section">
        <h2>1. Enrollment Terms</h2>
        <p>
            By submitting this enrollment application, you agree to the following terms and conditions. 
            Your enrollment is subject to approval<#if companyInfo??> by ${companyInfo.name}</#if> and compliance with all 
            applicable regulations.
        </p>
    </div>
    
    <div class="section">
        <h2>2. Effective Date</h2>
        <p>
            Coverage will become effective on ${effectiveDate!""}, provided that:
        </p>
        <ul>
            <li>Your enrollment application is approved</li>
            <li>All required premiums have been paid</li>
            <li>You meet all eligibility requirements</li>
            <li>No material changes occur before the effective date</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>3. Premium Payment</h2>
        <p>
            The total monthly premium for your selected coverage is <strong>$${totalPremium!"0.00"}</strong>. 
            Premiums are due on the first day of each month. Failure to pay premiums may result in 
            termination of coverage.
        </p>
    </div>
    
    <div class="section">
        <h2>4. Covered Individuals</h2>
        <p>
            This policy covers the following individuals:
        </p>
        <ul>
            <#if applicants??>
                <#list applicants as applicant>
                <li>${applicant.demographic.firstName!""} ${applicant.demographic.lastName!""} - ${applicant.relationship!""}</li>
                </#list>
            </#if>
        </ul>
    </div>
    
    <div class="section">
        <h2>5. Cancellation Policy</h2>
        <p>
            You may cancel this policy at any time by providing written notice<#if companyInfo??> to ${companyInfo.name}</#if>. 
            Cancellation will be effective as of the end of the premium period for which payment has been made.
        </p>
    </div>
    
    <div class="section">
        <h2>6. Certification</h2>
        <p class="important">
            I certify that the information provided in this application is true, accurate, and complete 
            to the best of my knowledge. I understand that any false statements or material misrepresentations 
            may result in denial of coverage or cancellation of my policy.
        </p>
    </div>
    
    <div class="section">
        <h2>7. Privacy Notice</h2>
        <p>
            Your personal information is protected in accordance with HIPAA regulations and our privacy policy. 
            We will not share your information with third parties without your consent, except as required by law.
        </p>
    </div>
    
    <div class="section">
        <h2>8. Contact Information</h2>
        <p>
            For questions or assistance, please contact us:
        </p>
        <#if companyInfo??>
        <p>
            <strong>${companyInfo.name}</strong><br>
            ${companyInfo.address}<br>
            ${companyInfo.city}, ${companyInfo.state} ${companyInfo.zipCode}<br>
            Phone: ${companyInfo.phone}<br>
            Email: ${companyInfo.email}<br>
            Website: ${companyInfo.website}
        </p>
        </#if>
        <#if agentInfo??>
        <p>
            <strong>Your Agent:</strong> ${agentInfo.name}<br>
            License: ${agentInfo.licenseNumber}<br>
            Phone: ${agentInfo.phone}<br>
            Email: ${agentInfo.email}
        </p>
        </#if>
    </div>
    
    <div class="section" style="margin-top: 50px; text-align: center; font-size: 10px; color: #999;">
        <p>© 2025<#if companyInfo??> ${companyInfo.name}</#if>. All rights reserved.</p>
        <p>Application Number: ${applicationNumber!""} | Generated: ${applicationDate!""}</p>
    </div>
</body>
</html>
