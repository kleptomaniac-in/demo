#!/bin/bash
#
# FreeMarker Template Generator
# Creates new optimized FreeMarker templates with best practices baked in
#
# Usage: ./generate-template.sh <template-name> <type>
#   template-name: Name of the template (e.g., "enrollment-summary")
#   type: one of: enrollment, terms, simple, custom
#

set -e

TEMPLATE_NAME="$1"
TEMPLATE_TYPE="${2:-simple}"
TEMPLATE_DIR="src/main/resources/templates"

# Validate inputs
if [ -z "$TEMPLATE_NAME" ]; then
    echo "❌ Error: Template name is required"
    echo "Usage: $0 <template-name> <type>"
    echo "  type: enrollment | terms | simple | custom"
    exit 1
fi

# Ensure .ftl extension
if [[ ! "$TEMPLATE_NAME" =~ \.ftl$ ]]; then
    TEMPLATE_NAME="${TEMPLATE_NAME}.ftl"
fi

TARGET_FILE="${TEMPLATE_DIR}/${TEMPLATE_NAME}"

# Check if file already exists
if [ -f "$TARGET_FILE" ]; then
    echo "⚠️  Warning: $TARGET_FILE already exists"
    read -p "Overwrite? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
fi

# Generate template based on type
case "$TEMPLATE_TYPE" in
    enrollment)
        cat > "$TARGET_FILE" << 'EOF'
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
EOF
        echo "✅ Created enrollment template: $TARGET_FILE"
        ;;
        
    terms)
        cat > "$TARGET_FILE" << 'EOF'
<#import "styles.ftl" as styles>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <@styles.termsStyles />
</head>
<body>
    <h1>Terms and Conditions</h1>
    
    <div class="section">
        <h2>1. Section Title</h2>
        <p>
            Terms content with proper formatting. Use text-align: justify 
            for professional legal document appearance.
        </p>
        <ul>
            <li>List item one</li>
            <li>List item two</li>
            <li>List item three</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>2. Another Section</h2>
        <p class="important">
            Important terms can be highlighted using the .important class.
        </p>
    </div>
    
    <div class="section">
        <h2>3. Contact Information</h2>
        <p>
            <#if companyInfo??>
            <strong>${companyInfo.name!""}</strong><br>
            ${companyInfo.address!""}<br>
            Phone: ${companyInfo.phone!""}<br>
            Email: ${companyInfo.email!""}
            </#if>
        </p>
    </div>
</body>
</html>
EOF
        echo "✅ Created terms template: $TARGET_FILE"
        ;;
        
    simple)
        cat > "$TARGET_FILE" << 'EOF'
<#import "styles.ftl" as styles>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <@styles.minimalStyles />
</head>
<body>
    <h1>Document Title</h1>
    
    <p>Simple document content goes here.</p>
    
    <#if items??>
    <table>
        <thead>
            <tr>
                <th>Column 1</th>
                <th>Column 2</th>
                <th>Column 3</th>
            </tr>
        </thead>
        <tbody>
            <#list items as item>
            <tr>
                <td>${item.field1!""}</td>
                <td>${item.field2!""}</td>
                <td>${item.field3!""}</td>
            </tr>
            </#list>
        </tbody>
    </table>
    </#if>
</body>
</html>
EOF
        echo "✅ Created simple template: $TARGET_FILE"
        ;;
        
    custom)
        cat > "$TARGET_FILE" << 'EOF'
<#import "styles.ftl" as styles>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        <@styles.basePageStyles />
        <@styles.headingStyles colorPrimary="#003366" />
        <@styles.tableStyles colorHeader="#003366" />
        
        /* Add your custom optimized CSS here */
        /* Remember: use consolidated selectors and shorthand properties */
        /* NEVER use position: fixed */
    </style>
</head>
<body>
    <h1>Custom Document</h1>
    
    <p>Your custom content here.</p>
    
    <!-- Build your structure using the base styles as foundation -->
</body>
</html>
EOF
        echo "✅ Created custom template: $TARGET_FILE"
        ;;
        
    *)
        echo "❌ Error: Unknown template type '$TEMPLATE_TYPE'"
        echo "Valid types: enrollment, terms, simple, custom"
        exit 1
        ;;
esac

echo ""
echo "📚 Template created with optimized CSS!"
echo "📝 Review the style guide: docs/FREEMARKER_STYLE_GUIDE.md"
echo "🚀 Expected performance: < 50ms per page (warm cache)"
echo ""
echo "Next steps:"
echo "  1. Edit $TARGET_FILE"
echo "  2. Add your content/logic"
echo "  3. Test with: mvn test -Dtest=YourTest"
echo "  4. Verify performance in logs"
