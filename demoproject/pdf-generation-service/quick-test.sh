#!/bin/bash

# Quick test script for PDF generation with addendums
# Usage: ./quick-test.sh [test-number]
# Example: ./quick-test.sh 1

BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTPUT_DIR="curl-test-output"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== PDF Generation Test Suite ===${NC}"
echo ""

# Function to run a test
run_test() {
    local test_num=$1
    local test_name=$2
    local output_file=$3
    local json_payload=$4
    
    echo -e "${YELLOW}Test $test_num: $test_name${NC}"
    
    if curl -s -X POST "$BASE_URL/api/enrollment/generate" \
        -H "Content-Type: application/json" \
        -o "$OUTPUT_DIR/$output_file" \
        -d "$json_payload"; then
        
        local file_size=$(stat -f%z "$OUTPUT_DIR/$output_file" 2>/dev/null || stat -c%s "$OUTPUT_DIR/$output_file" 2>/dev/null)
        echo -e "${GREEN}✓ Success${NC} - Saved to: $OUTPUT_DIR/$output_file ($file_size bytes)"
    else
        echo -e "❌ Failed - Check if service is running on $BASE_URL"
        return 1
    fi
    echo ""
}

# Test 1: 6 Dependents (overflow)
test1() {
    run_test 1 \
        "Dependent Overflow (6 dependents)" \
        "test1-6-dependents.pdf" \
        '{
          "enrollment": {
            "state": "CA",
            "marketCategory": "INDIVIDUAL",
            "products": ["DENTAL"]
          },
          "payload": {
            "enrollmentData": {
              "groupNumber": "GRP-12345",
              "effectiveDate": "2024-01-01"
            },
            "applicants": [
              {"applicantId": "A001", "firstName": "John", "lastName": "Smith", "demographic": {"relationshipType": "PRIMARY", "dateOfBirth": "1980-05-15", "gender": "M", "ssn": "123-45-6789"}, "coverages": [{"productType": "DENTAL", "premium": 50.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A002", "firstName": "Mary", "lastName": "Smith", "demographic": {"relationshipType": "SPOUSE", "dateOfBirth": "1982-08-20", "gender": "F", "ssn": "987-65-4321"}, "coverages": [{"productType": "DENTAL", "premium": 45.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A003", "firstName": "Child1", "lastName": "Smith", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2010-03-10", "gender": "M", "ssn": "111-11-1111"}, "coverages": [{"productType": "DENTAL", "premium": 30.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A004", "firstName": "Child2", "lastName": "Smith", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2012-07-22", "gender": "F", "ssn": "222-22-2222"}, "coverages": [{"productType": "DENTAL", "premium": 30.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A005", "firstName": "Child3", "lastName": "Smith", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2014-11-05", "gender": "M", "ssn": "333-33-3333"}, "coverages": [{"productType": "DENTAL", "premium": 30.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A006", "firstName": "Child4", "lastName": "Smith", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2016-02-14", "gender": "F", "ssn": "444-44-4444"}, "coverages": [{"productType": "DENTAL", "premium": 30.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A007", "firstName": "Child5", "lastName": "Smith", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2018-09-30", "gender": "M", "ssn": "555-55-5555"}, "coverages": [{"productType": "DENTAL", "premium": 30.00, "carrier": "Delta Dental"}]},
              {"applicantId": "A008", "firstName": "Child6", "lastName": "Smith", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2020-12-25", "gender": "F", "ssn": "666-66-6666"}, "coverages": [{"productType": "DENTAL", "premium": 30.00, "carrier": "Delta Dental"}]}
            ]
          }
        }'
}

# Test 2: Multiple Coverages (overflow)
test2() {
    run_test 2 \
        "Coverage Overflow (3 coverages per person)" \
        "test2-multiple-coverages.pdf" \
        '{
          "enrollment": {
            "state": "CA",
            "marketCategory": "INDIVIDUAL",
            "products": ["MEDICAL", "DENTAL", "VISION"]
          },
          "payload": {
            "enrollmentData": {
              "groupNumber": "GRP-99999",
              "effectiveDate": "2024-03-01"
            },
            "applicants": [
              {"applicantId": "A001", "firstName": "John", "lastName": "Doe", "demographic": {"relationshipType": "PRIMARY", "dateOfBirth": "1975-04-12", "gender": "M", "ssn": "555-12-3456"}, "coverages": [{"productType": "MEDICAL", "premium": 500.00, "carrier": "Blue Cross"}, {"productType": "DENTAL", "premium": 50.00, "carrier": "Delta Dental"}, {"productType": "VISION", "premium": 25.00, "carrier": "VSP"}]},
              {"applicantId": "A002", "firstName": "Jane", "lastName": "Doe", "demographic": {"relationshipType": "SPOUSE", "dateOfBirth": "1977-09-22", "gender": "F", "ssn": "555-98-7654"}, "coverages": [{"productType": "MEDICAL", "premium": 450.00, "carrier": "Blue Cross"}, {"productType": "DENTAL", "premium": 45.00, "carrier": "Delta Dental"}]}
            ]
          }
        }'
}

# Test 3: Both Overflows
test3() {
    run_test 3 \
        "Both Overflows (5 dependents + multiple coverages)" \
        "test3-both-overflows.pdf" \
        '{
          "enrollment": {
            "state": "TX",
            "marketCategory": "SMALL_GROUP",
            "products": ["MEDICAL", "DENTAL"]
          },
          "payload": {
            "enrollmentData": {
              "groupNumber": "GRP-BOTH",
              "effectiveDate": "2024-01-15"
            },
            "applicants": [
              {"applicantId": "A001", "firstName": "Robert", "lastName": "Johnson", "demographic": {"relationshipType": "PRIMARY", "dateOfBirth": "1978-01-10", "gender": "M", "ssn": "444-55-6666"}, "coverages": [{"productType": "MEDICAL", "premium": 550.00, "carrier": "Aetna"}, {"productType": "DENTAL", "premium": 60.00, "carrier": "Cigna"}]},
              {"applicantId": "A002", "firstName": "Linda", "lastName": "Johnson", "demographic": {"relationshipType": "SPOUSE", "dateOfBirth": "1980-05-22", "gender": "F", "ssn": "444-77-8888"}, "coverages": [{"productType": "MEDICAL", "premium": 500.00, "carrier": "Aetna"}, {"productType": "DENTAL", "premium": 55.00, "carrier": "Cigna"}]},
              {"applicantId": "A003", "firstName": "Child1", "lastName": "Johnson", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2008-03-15", "gender": "M", "ssn": "444-11-2222"}, "coverages": [{"productType": "MEDICAL", "premium": 180.00, "carrier": "Aetna"}]},
              {"applicantId": "A004", "firstName": "Child2", "lastName": "Johnson", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2010-07-20", "gender": "F", "ssn": "444-33-4444"}, "coverages": [{"productType": "MEDICAL", "premium": 180.00, "carrier": "Aetna"}]},
              {"applicantId": "A005", "firstName": "Child3", "lastName": "Johnson", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2012-11-08", "gender": "M", "ssn": "444-55-6677"}, "coverages": [{"productType": "MEDICAL", "premium": 180.00, "carrier": "Aetna"}]},
              {"applicantId": "A006", "firstName": "Child4", "lastName": "Johnson", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2014-02-14", "gender": "F", "ssn": "444-88-9900"}, "coverages": [{"productType": "MEDICAL", "premium": 180.00, "carrier": "Aetna"}]},
              {"applicantId": "A007", "firstName": "Child5", "lastName": "Johnson", "demographic": {"relationshipType": "DEPENDENT", "dateOfBirth": "2016-09-30", "gender": "M", "ssn": "444-11-3344"}, "coverages": [{"productType": "MEDICAL", "premium": 180.00, "carrier": "Aetna"}]}
            ]
          }
        }'
}

# Main execution
if [ -z "$1" ]; then
    echo "Running all tests..."
    echo ""
    test1
    test2
    test3
    
    echo -e "${BLUE}=== Summary ===${NC}"
    echo "PDFs saved to: $OUTPUT_DIR/"
    ls -lh "$OUTPUT_DIR"/*.pdf 2>/dev/null || echo "No PDFs generated"
else
    case $1 in
        1) test1 ;;
        2) test2 ;;
        3) test3 ;;
        *) echo "Invalid test number. Use 1, 2, or 3" ;;
    esac
fi
