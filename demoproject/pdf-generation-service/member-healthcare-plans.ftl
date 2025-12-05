<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Member Healthcare Plans</title>
    <style>
        @page {
            size: A4 portrait;
            margin: 10mm;
        }
        
        body {
            font-family: Arial, sans-serif;
            margin: 10px;
            font-size: 9px;
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
            min-width: 40px;
            max-width: 60px;
            padding: 4px 2px;
            vertical-align: middle;
            word-wrap: break-word;
            white-space: normal;
            line-height: 1.2;
        }
        
        .premium-header {
            background-color: #F5F5F5;
            font-size: 7px;
            font-weight: bold;
            min-width: 25px;
        }
        
        .member-name {
            background-color: #f2f2f2;
            font-weight: bold;
            text-align: left;
            width: 100px;
            font-size: 9px;
        }
        
        .premium-value {
            font-size: 8px;
            min-width: 25px;
        }
        
        .no-plan {
            color: #999;
            font-style: italic;
            font-size: 8px;
        }
    </style>
</head>
<body>
    <h1>Healthcare Member Plans Summary</h1>
    
    <#-- Maximum plans to show per product in main table -->
    <#assign maxPlansPerProduct = 2>
    
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
    
    <#-- Collect all unique plan names for each product type (limited to first 3) -->
    <#assign medicalPlanNames = []>
    <#assign dentalPlanNames = []>
    <#assign visionPlanNames = []>
    
    <#-- Build lists of plan names across all members (first 3 only) -->
    <#list payload.members as member>
        <#if member.medicalPlans??>
            <#list member.medicalPlans as plan>
                <#if !medicalPlanNames?seq_contains(plan.planName) && (medicalPlanNames?size < maxPlansPerProduct)>
                    <#assign medicalPlanNames = medicalPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
        <#if member.dentalPlans??>
            <#list member.dentalPlans as plan>
                <#if !dentalPlanNames?seq_contains(plan.planName) && (dentalPlanNames?size < maxPlansPerProduct)>
                    <#assign dentalPlanNames = dentalPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
        <#if member.visionPlans??>
            <#list member.visionPlans as plan>
                <#if !visionPlanNames?seq_contains(plan.planName) && (visionPlanNames?size < maxPlansPerProduct)>
                    <#assign visionPlanNames = visionPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
    </#list>
    
    <#-- Collect ALL plan names for each product type -->
    <#assign allMedicalPlanNames = []>
    <#assign allDentalPlanNames = []>
    <#assign allVisionPlanNames = []>
    
    <#list payload.members as member>
        <#if member.medicalPlans??>
            <#list member.medicalPlans as plan>
                <#if !allMedicalPlanNames?seq_contains(plan.planName)>
                    <#assign allMedicalPlanNames = allMedicalPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
        <#if member.dentalPlans??>
            <#list member.dentalPlans as plan>
                <#if !allDentalPlanNames?seq_contains(plan.planName)>
                    <#assign allDentalPlanNames = allDentalPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
        <#if member.visionPlans??>
            <#list member.visionPlans as plan>
                <#if !allVisionPlanNames?seq_contains(plan.planName)>
                    <#assign allVisionPlanNames = allVisionPlanNames + [plan.planName]>
                </#if>
            </#list>
        </#if>
    </#list>
    
    <#-- Function to split plans into groups of maxPlansPerProduct -->
    <#function splitIntoGroups planList maxPerGroup>
        <#local groups = []>
        <#local currentGroup = []>
        <#list planList as plan>
            <#if (currentGroup?size < maxPerGroup)>
                <#local currentGroup = currentGroup + [plan]>
            <#else>
                <#local groups = groups + [currentGroup]>
                <#local currentGroup = [plan]>
            </#if>
        </#list>
        <#if (currentGroup?size > 0)>
            <#local groups = groups + [currentGroup]>
        </#if>
        <#return groups>
    </#function>
    
    <#-- Split plans into groups -->
    <#assign medicalGroups = splitIntoGroups(allMedicalPlanNames, maxPlansPerProduct)>
    <#assign dentalGroups = splitIntoGroups(allDentalPlanNames, maxPlansPerProduct)>
    <#assign visionGroups = splitIntoGroups(allVisionPlanNames, maxPlansPerProduct)>
    
    <#-- Calculate total number of tables needed -->
    <#assign totalTables = [medicalGroups?size, dentalGroups?size, visionGroups?size]?max>
    
    <#-- Render tables for each group -->
    <#list 0..<totalTables as tableIndex>
    
    <#-- Get the plan groups for this table -->
    <#assign currentMedicalPlans = (tableIndex < medicalGroups?size)?then(medicalGroups[tableIndex], [])>
    <#assign currentDentalPlans = (tableIndex < dentalGroups?size)?then(dentalGroups[tableIndex], [])>
    <#assign currentVisionPlans = (tableIndex < visionGroups?size)?then(visionGroups[tableIndex], [])>
    
    <#-- Show header for additional tables -->
    <#if (tableIndex > 0)>
    <h2 style="color: #333; font-size: 12px; margin-top: 20px; margin-bottom: 10px; border-bottom: 2px solid #FF9800; padding-bottom: 5px;">Additional Plans - Set ${tableIndex + 1}</h2>
    </#if>
    
    <table class="member-table">
        <thead>
            <!-- Row 1: Product Type Headers -->
            <tr>
                <th rowspan="3">Member Name</th>
                <#if (currentMedicalPlans?size > 0)>
                    <th colspan="${currentMedicalPlans?size * 2}" class="product-header">Medical</th>
                </#if>
                <#if (currentDentalPlans?size > 0)>
                    <th colspan="${currentDentalPlans?size * 2}" class="product-header">Dental</th>
                </#if>
                <#if (currentVisionPlans?size > 0)>
                    <th colspan="${currentVisionPlans?size * 2}" class="product-header">Vision</th>
                </#if>
            </tr>
            
            <!-- Row 2: Plan Name Headers -->
            <tr>
                <#if (currentMedicalPlans?size > 0)>
                    <#list currentMedicalPlans as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
                <#if (currentDentalPlans?size > 0)>
                    <#list currentDentalPlans as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
                <#if (currentVisionPlans?size > 0)>
                    <#list currentVisionPlans as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
            </tr>
            
            <!-- Row 3: Premium Type Headers -->
            <tr>
                <#if (currentMedicalPlans?size > 0)>
                    <#list currentMedicalPlans as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
                <#if (currentDentalPlans?size > 0)>
                    <#list currentDentalPlans as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
                <#if (currentVisionPlans?size > 0)>
                    <#list currentVisionPlans as planName>
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
                <#if (currentMedicalPlans?size > 0)>
                    <#list currentMedicalPlans as expectedPlanName>
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
                <#if (currentDentalPlans?size > 0)>
                    <#list currentDentalPlans as expectedPlanName>
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
                <#if (currentVisionPlans?size > 0)>
                    <#list currentVisionPlans as expectedPlanName>
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
    
    </#list>
    
    <#-- Remove old overflow table code -->
    <#if false>
    
    <!-- Additional Plans Table -->
    <h2 style="color: #333; font-size: 12px; margin-top: 20px; margin-bottom: 10px; border-bottom: 2px solid #FF9800; padding-bottom: 5px;">Additional Plans (Beyond First 3 per Product)</h2>
    
    <table class="member-table">
        <thead>
            <!-- Row 1: Product Type Headers -->
            <tr>
                <th rowspan="3">Member Name</th>
                <#if (medicalOverflowPlanNames?size > 0)>
                    <th colspan="${medicalOverflowPlanNames?size * 2}" class="product-header">Medical</th>
                </#if>
                <#if (dentalOverflowPlanNames?size > 0)>
                    <th colspan="${dentalOverflowPlanNames?size * 2}" class="product-header">Dental</th>
                </#if>
                <#if (visionOverflowPlanNames?size > 0)>
                    <th colspan="${visionOverflowPlanNames?size * 2}" class="product-header">Vision</th>
                </#if>
            </tr>
            
            <!-- Row 2: Plan Name Headers -->
            <tr>
                <#if (medicalOverflowPlanNames?size > 0)>
                    <#list medicalOverflowPlanNames as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
                <#if (dentalOverflowPlanNames?size > 0)>
                    <#list dentalOverflowPlanNames as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
                <#if (visionOverflowPlanNames?size > 0)>
                    <#list visionOverflowPlanNames as planName>
                        <th colspan="2" class="plan-header">${planName}</th>
                    </#list>
                </#if>
            </tr>
            
            <!-- Row 3: Premium Type Headers -->
            <tr>
                <#if (medicalOverflowPlanNames?size > 0)>
                    <#list medicalOverflowPlanNames as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
                <#if (dentalOverflowPlanNames?size > 0)>
                    <#list dentalOverflowPlanNames as planName>
                        <th class="premium-header">Base</th>
                        <th class="premium-header">Bundled</th>
                    </#list>
                </#if>
                <#if (visionOverflowPlanNames?size > 0)>
                    <#list visionOverflowPlanNames as planName>
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
                
                <!-- Medical Overflow Plans -->
                <#if (medicalOverflowPlanNames?size > 0)>
                    <#list medicalOverflowPlanNames as expectedPlanName>
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
                
                <!-- Dental Overflow Plans -->
                <#if (dentalOverflowPlanNames?size > 0)>
                    <#list dentalOverflowPlanNames as expectedPlanName>
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
                
                <!-- Vision Overflow Plans -->
                <#if (visionOverflowPlanNames?size > 0)>
                    <#list visionOverflowPlanNames as expectedPlanName>
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
    
    </#if><#-- End of old overflow code to remove -->
    
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
