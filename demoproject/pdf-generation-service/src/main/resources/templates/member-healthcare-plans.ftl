<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; color: #333; }
        h1 { color: #2c5aa0; border-bottom: 3px solid #2c5aa0; padding-bottom: 10px; }
        .info { background-color: #f5f5f5; padding: 20px; margin: 20px 0; }
        .label { font-weight: bold; }
    </style>
</head>
<body>
    <h1>Member Healthcare Plans</h1>
    <div class="info">
        <#if (payload.memberName)??><p><span class="label">Member Name:</span> ${payload.memberName}</p></#if>
        <#if (payload.memberId)??><p><span class="label">Member ID:</span> ${payload.memberId}</p></#if>
        <#if (payload.planName)??><p><span class="label">Plan Name:</span> ${payload.planName}</p></#if>
    </div>
    <p>This document contains your healthcare coverage information.</p>
</body>
</html>
