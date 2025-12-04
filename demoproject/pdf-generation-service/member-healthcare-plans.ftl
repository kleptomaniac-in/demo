<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Member Healthcare Plans</title>
    <style>
        @page {
            size: A4 landscape;
            margin: 10mm;
        }
        
        body {
            font-family: Arial, sans-serif;
            margin: 10px;
            font-size: 8px;
        }
        
        h1 {
            color: #333;
            border-bottom: 2px solid #4CAF50;
            padding-bottom: 8px;
            font-size: 14px;
            margin-bottom: 10px;
        }
        
        .member-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
            table-layout: fixed;
        }
        
        .member-table th,
        .member-table td {
            border: 1px solid #333;
            padding: 4px 2px;
            text-align: center;
            vertical-align: middle;
            word-wrap: break-word;
            overflow: hidden;
        }
        
        .member-table th {
            background-color: #4CAF50;
            color: white;
            font-weight: bold;
            font-size: 7px;
        }
        
        .product-header {
            background-color: #2196F3;
            color: white;
            font-weight: bold;
            font-size: 8px;
        }
        
        .plan-header {
            background-color: #E3F2FD;
            font-weight: bold;
            font-size: 7px;
            color: #333;
            max-width: 60px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        
        .premium-header {
            background-color: #F5F5F5;
            font-size: 6px;
            font-weight: bold;
        }
        
        .member-name {
            background-color: #f2f2f2;
            font-weight: bold;
            text-align: left;
            width: 80px;
            font-size: 8px;
        }
        
        .premium-value {
            font-size: 7px;
        }
        
        .no-plan {
            color: #999;
            font-style: italic;
            font-size: 7px;
        }
    </style>
</head>
<body>
    <h1>Healthcare Member Plans Summary</h1>
    
    <#-- Calculate maximum number of plans for each product type across all members -->
    <#assign maxMedical = 0>
    <#assign maxDental = 0>
    <#assign maxVision = 0>
    
    <#list payload.members as member>
        <#if member.medicalPlans?? && (member.medicalPlans?size > maxMedical)>
            <#assign maxMedical = member.medicalPlans?size>
        </#if>
        <#if member.dentalPlans?? && (member.dentalPlans?size > maxDental)>
            <#assign maxDental = member.dentalPlans?size>
        </#if>
        <#if member.visionPlans?? && (member.visionPlans?size > maxVision)>
            <#assign maxVision = member.visionPlans?size>
        </#if>
    </#list>
    
    <#-- Collect all unique plan names for each product type -->
    <#assign medicalPlanNames = []>
    <#assign dentalPlanNames = []>
    <#assign visionPlanNames = []>
    
    <#-- Build lists of plan names across all members -->
    <#list payload.members as member>
        <#if member.medicalPlans??>
            <#list member.medicalPlans as plan>
                <#if !medicalPlanNames?seq_contains(plan.planName)>
                    <#assign medicalPlanNames = medicalPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
        <#if member.dentalPlans??>
            <#list member.dentalPlans as plan>
                <#if !dentalPlanNames?seq_contains(plan.planName)>
                    <#assign dentalPlanNames = dentalPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
        <#if member.visionPlans??>
            <#list member.visionPlans as plan>
                <#if !visionPlanNames?seq_contains(plan.planName)>
                    <#assign visionPlanNames = visionPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
    </#list>
    
    <table class="member-table">
        <thead>
            <!-- Row 1: Product Type Headers -->
            <tr>
                <th rowspan="3">Member Name</th>
                <#if (medicalPlanNames?size > 0)>
                    <th colspan="${medicalPlanNames?size * 2}" class="product-header">Medical</th>
                </#if>
                <#if (dentalPlanNames?size > 0)>
                    <th colspan="${dentalPlanNames?size * 2}" class="product-header">Dental</th>
                </#if>
                <#if (visionPlanNames?size > 0)>
                    <th colspan="${visionPlanNames?size * 2}" class="product-header">Vision</th>
                </#if>
            </tr>
            
            <!-- Row 2: Plan Name Headers -->
            <tr>
                <#if (medicalPlanNames?size > 0)>
                    <#list medicalPlanNames as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
                <#if (dentalPlanNames?size > 0)>
                    <#list dentalPlanNames as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
                <#if (visionPlanNames?size > 0)>
                    <#list visionPlanNames as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
            </tr>
            
            <!-- Row 3: Premium Type Headers -->
            <tr>
                <#if (medicalPlanNames?size > 0)>
                    <#list medicalPlanNames as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
                <#if (dentalPlanNames?size > 0)>
                    <#list dentalPlanNames as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
                <#if (visionPlanNames?size > 0)>
                    <#list visionPlanNames as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
            </tr>
        </thead>
        <tbody>
            <#list payload.members as member>
            <tr>
                <td class="member-name">${member.name}</td>
                
                <!-- Medical Plans -->
                <#if (medicalPlanNames?size > 0)>
                    <#list medicalPlanNames as expectedPlanName>
                        <#assign foundPlan = false>
                        <#if member.medicalPlans??>
                            <#list member.medicalPlans as plan>
                                <#if plan.planName == expectedPlanName>
                                    <td class="premium-value">$${plan.basePremium?string["0.00"]}</td>
                                    <td class="premium-value">$${plan.bundledPremium?string["0.00"]}</td>
                                    <#assign foundPlan = true>
                                    <#break>
                                </#if>
                            </#list>
                        </#if>
                        <#if !foundPlan>
                            <td class="no-plan">-</td>
                            <td class="no-plan">-</td>
                        </#if>
                    </#list>
                </#if>
                
                <!-- Dental Plans -->
                <#if (dentalPlanNames?size > 0)>
                    <#list dentalPlanNames as expectedPlanName>
                        <#assign foundPlan = false>
                        <#if member.dentalPlans??>
                            <#list member.dentalPlans as plan>
                                <#if plan.planName == expectedPlanName>
                                    <td class="premium-value">$${plan.basePremium?string["0.00"]}</td>
                                    <td class="premium-value">$${plan.bundledPremium?string["0.00"]}</td>
                                    <#assign foundPlan = true>
                                    <#break>
                                </#if>
                            </#list>
                        </#if>
                        <#if !foundPlan>
                            <td class="no-plan">-</td>
                            <td class="no-plan">-</td>
                        </#if>
                    </#list>
                </#if>
                
                <!-- Vision Plans -->
                <#if (visionPlanNames?size > 0)>
                    <#list visionPlanNames as expectedPlanName>
                        <#assign foundPlan = false>
                        <#if member.visionPlans??>
                            <#list member.visionPlans as plan>
                                <#if plan.planName == expectedPlanName>
                                    <td class="premium-value">$${plan.basePremium?string["0.00"]}</td>
                                    <td class="premium-value">$${plan.bundledPremium?string["0.00"]}</td>
                                    <#assign foundPlan = true>
                                    <#break>
                                </#if>
                            </#list>
                        </#if>
                        <#if !foundPlan>
                            <td class="no-plan">-</td>
                            <td class="no-plan">-</td>
                        </#if>
                    </#list>
                </#if>
            </tr>
            </#list>
        </tbody>
    </table>
    
    <!-- Legend for Plan Names -->
    <div style="margin-top: 15px; font-size: 8px; page-break-before: avoid;">
        <h3 style="font-size: 10px; margin-bottom: 8px;">Plan Details:</h3>
        <#list payload.members as member>
            <div style="margin-bottom: 15px;">
                <strong>${member.name}:</strong>
                <ul style="margin: 5px 0; padding-left: 20px;">
                    <#if member.medicalPlans?? && (member.medicalPlans?size > 0)>
                        <li><strong>Medical:</strong>
                            <#list member.medicalPlans as plan>
                                ${plan.planName}<#if plan_has_next>, </#if>
                            </#list>
                        </li>
                    </#if>
                    <#if member.dentalPlans?? && (member.dentalPlans?size > 0)>
                        <li><strong>Dental:</strong>
                            <#list member.dentalPlans as plan>
                                ${plan.planName}<#if plan_has_next>, </#if>
                            </#list>
                        </li>
                    </#if>
                    <#if member.visionPlans?? && (member.visionPlans?size > 0)>
                        <li><strong>Vision:</strong>
                            <#list member.visionPlans as plan>
                                ${plan.planName}<#if plan_has_next>, </#if>
                            </#list>
                        </li>
                    </#if>
                </ul>
            </div>
        </#list>
    </div>
</body>
</html>
