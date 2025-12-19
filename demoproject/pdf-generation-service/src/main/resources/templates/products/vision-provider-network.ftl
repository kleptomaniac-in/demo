<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; color: #333; }
        h1 { color: #2c5aa0; border-bottom: 3px solid #2c5aa0; padding-bottom: 10px; }
        h2 { color: #4a7cb8; margin-top: 30px; }
        .provider-info { background-color: #f5f5f5; padding: 20px; margin: 20px 0; }
        .label { font-weight: bold; }
        ul { list-style-type: disc; margin-left: 20px; }
        li { margin: 10px 0; }
    </style>
</head>
<body>
    <h1>Vision Provider Network</h1>
    
    <div class="provider-info">
        <h2>In-Network Benefits</h2>
        <p>Your vision plan provides access to a comprehensive network of eye care professionals.</p>
        
        <ul>
            <li><span class="label">Eye Exams:</span> Annual comprehensive eye exam with $10 copay</li>
            <li><span class="label">Frames:</span> $130 allowance every 12 months</li>
            <li><span class="label">Lenses:</span> Standard single, bifocal, or trifocal lenses covered</li>
            <li><span class="label">Contact Lenses:</span> $130 allowance in lieu of glasses</li>
        </ul>
    </div>
    
    <div class="provider-info">
        <h2>Find a Provider</h2>
        <p>Visit our website or call customer service to locate participating vision providers in your area:</p>
        <ul>
            <li>National retail chains</li>
            <li>Independent optometrists</li>
            <li>Ophthalmologists</li>
            <li>Online vision retailers</li>
        </ul>
    </div>
    
    <#if (payload.memberName)??><p><span class="label">Member:</span> ${payload.memberName}</p></#if>
</body>
</html>
