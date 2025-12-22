# Enrollment PDF Performance Monitoring Guide

## Overview
Performance monitoring has been extended to cover the enrollment PDF generation endpoints (`/api/enrollment/generate` and `/api/enrollment/generate-with-rules`).

## Monitored Endpoints

### 1. POST /api/enrollment/generate
Generates enrollment PDFs with automatic config selection based on products/market/state.

**Monitored Phases:**
- **Config Selection**: Time to select and load configuration
- **Payload Processing**: Time to pre-process and enrich payload data  
- **PDF Generation**: Total time for PDF generation (includes sub-phases when addendums are used)

### 2. POST /api/enrollment/generate-with-rules
Generates enrollment PDFs using rule-based config selection.

**Monitored Phases:** Same as above

## Addendum Sub-Phases

When a configuration has addendums enabled (e.g., for dependent/coverage overflow), the following sub-phases are tracked within the "PDF Generation" phase:

1. **Fill AcroForm**: Time to fill the main PDF template
2. **Generate Dependent Addendum**: Time to generate overflow dependent addendum (if needed)
3. **Generate Coverage Addendum**: Time to generate overflow coverage addendum (if needed)
4. **Merge PDFs**: Time to merge main form + addendums

## Example Performance Log Output

```
2024-12-22 18:00:00 INFO  PerformanceMonitoringContext - Started performance monitoring for: Enrollment PDF Generation (config: unknown)
2024-12-22 18:00:01 INFO  PerformanceMonitoringContext - Performance Summary for Enrollment PDF Generation:
  Total Time: 1250ms
  Phases:
    Config Selection: 150ms
    Payload Processing: 50ms
    PDF Generation: 1000ms
      Fill AcroForm: 300ms
      Generate Dependent Addendum: 450ms
      Generate Coverage Addendum: 150ms
      Merge PDFs: 100ms
```

## Configuration Examples

### No Addendums (Standard Flow)
```yaml
# dental-small-group-tx.yml
name: dental-small-group-tx
# No addendums section - uses FlexiblePdfMergeService
```

**Phases:**
- Config Selection
- Payload Processing
- PDF Generation (single call to FlexiblePdfMergeService)

### With Addendums (Overflow Handling)
```yaml
# dental-individual-ca.yml
name: dental-individual-ca-acroform
addendums:
  dependents:
    enabled: true
    maxInMainForm: 3
    template: "templates/dental-ca-dependent-addendum.ftl"
  coverages:
    enabled: true
    maxPerApplicant: 1
    template: "templates/dental-ca-coverage-addendum.ftl"
```

**Phases:**
- Config Selection
- Payload Processing
- PDF Generation
  - Fill AcroForm
  - Generate Dependent Addendum (if 4+ dependents)
  - Generate Coverage Addendum (if 2+ coverages)
  - Merge PDFs

## Implementation Details

### EnrollmentPdfController
- Injects `PerformanceMonitoringContext`
- Starts monitoring at method entry
- Tracks high-level phases (Config Selection, Payload Processing, PDF Generation)
- Completes monitoring before response
- Clears context on exception

### EnrollmentPdfService
- Injects `PerformanceMonitoringContext`
- Tracks detailed addendum generation phases
- Only tracks phases when addendums are actually generated
- Provides visibility into overflow handling performance

### FlexiblePdfMergeService
- Already has performance monitoring
- Used when config has no addendums
- Tracks: Load Config, Apply Enrichers, Resolve Sections, Generate Sections, Merge, etc.

## Performance Optimization Tips

1. **Config Selection**: 
   - Use convention-based naming to avoid dynamic composition
   - Pre-generate config files for common scenarios

2. **AcroForm Filling**:
   - Ensure templates are cached
   - Minimize field count for better performance

3. **Addendum Generation**:
   - Only enable addendums when overflow is expected
   - Use maxInMainForm/maxPerApplicant values that match your template capacity

4. **PDF Merging**:
   - Minimize number of addendums needed
   - Consider pagination strategies for large datasets

## Monitoring Best Practices

1. **Enable INFO logging** for `com.example.monitoring.PerformanceMonitoringContext` to see summary logs
2. **Enable DEBUG logging** to see phase start/end events
3. **Track trends** over time to identify performance degradation
4. **Compare** standard flow vs addendum flow performance
5. **Use metrics** to guide template design (e.g., if coverage addendum is slow, optimize template)

## Related Files

- [EnrollmentPdfController.java](../pdf-generation-service/src/main/java/com/example/pdf/controller/EnrollmentPdfController.java)
- [EnrollmentPdfService.java](../pdf-generation-service/src/main/java/com/example/service/EnrollmentPdfService.java)
- [PerformanceMonitoringContext.java](../pdf-generation-service/src/main/java/com/example/monitoring/PerformanceMonitoringContext.java)
- [PERFORMANCE-MONITORING-GUIDE.md](PERFORMANCE-MONITORING-GUIDE.md) - General monitoring guide
- [ADDENDUM_FIX_SUMMARY.md](ADDENDUM_FIX_SUMMARY.md) - Addendum implementation details

## Testing Performance

Use the test files to verify performance tracking:

```bash
# Test with dependent overflow (4+ dependents)
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d @backup/pdf-generation-service/test-overflow.json

# Test with coverage overflow (2+ coverages)  
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d @backup/pdf-generation-service/test-coverage-overflow.json

# Test with both overflows
curl -X POST http://localhost:8080/api/enrollment/generate \
  -H "Content-Type: application/json" \
  -d @backup/pdf-generation-service/test-both-overflows.json
```

Check logs for performance summaries after each request.
