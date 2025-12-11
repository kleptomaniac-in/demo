#!/bin/bash
curl -s -X POST http://localhost:8080/api/pdf/merge \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "pdf-merge-config.yml",
    "outputFileName": "test.pdf",
    "payload": {
      "companyName": "Test Company",
      "members": [
        {
          "name": "John Doe",
          "medicalPlans": [{"planName": "Gold PPO", "basePremium": 450, "bundledPremium": 425}],
          "dentalPlans": [{"planName": "Basic", "basePremium": 45, "bundledPremium": 40}],
          "visionPlans": [{"planName": "Standard", "basePremium": 15, "bundledPremium": 12}]
        }
      ]
    }
  }' --output /tmp/test-output.pdf

ls -lh /tmp/test-output.pdf
strings /tmp/test-output.pdf | grep -E "Test Company|John Doe|Gold PPO" || echo "Text not found in simple grep"
