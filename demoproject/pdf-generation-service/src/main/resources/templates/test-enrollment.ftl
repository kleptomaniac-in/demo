<#import "styles.ftl" as styles>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <@styles.completeEnrollmentStyles colorPrimary="#003366" />
</head>
<body>
    <h1>Enrollment Document Title</h1>
    
    <div class="summary-box">
        <div class="summary-item">
            <span class="summary-label">Application Number:</span>
            <span class="summary-value">${applicationNumber!""}</span>
        </div>
        <div class="summary-item">
            <span class="summary-label">Effective Date:</span>
            <span class="summary-value">${effectiveDate!""}</span>
        </div>
    </div>
    
    <div class="section">
        <h2>Section Title</h2>
        <p>Your content here...</p>
    </div>
    
    <#if members??>
    <table>
        <thead>
            <tr>
                <th>Member</th>
                <th>Relationship</th>
                <th>Date of Birth</th>
            </tr>
        </thead>
        <tbody>
            <#list members as member>
            <tr>
                <td>${member.name!""}</td>
                <td>${member.relationship!""}</td>
                <td>${member.dateOfBirth!""}</td>
            </tr>
            </#list>
        </tbody>
    </table>
    </#if>
    
    <div class="info-section">
        <strong>Note:</strong> Additional information or disclaimers.
    </div>
</body>
</html>
