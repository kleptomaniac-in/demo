# Performance Monitoring Coverage Map

## Complete Coverage Overview

### 🟢 Fully Monitored Endpoints

#### 1. /api/document/generate
**Controller:** DocumentController  
**Service:** FlexiblePdfMergeService  
**Status:** ✅ Has dedicated metrics endpoint

**Phases Tracked:**
- Load Config
- Apply Enrichers  
- Resolve Sections
- Generate Sections (with sub-phases per section)
- Merge PDFs
- Apply Headers/Footers
- Serialize to Bytes

**Metrics Endpoint:** `/api/document/generate-with-metrics`

---

#### 2. /api/enrollment/generate
**Controller:** EnrollmentPdfController  
**Service:** EnrollmentPdfService OR FlexiblePdfMergeService  
**Status:** ✅ Newly extended (see ENROLLMENT_MONITORING_EXTENSION_SUMMARY.md)

**Phases Tracked:**

**High-Level (Controller):**
- Config Selection
- Payload Processing
- PDF Generation

**Addendum Sub-Phases (Service):**
- Fill AcroForm
- Generate Dependent Addendum (if 4+ dependents)
- Generate Coverage Addendum (if 2+ coverages)
- Merge PDFs

**Metrics Endpoint:** None (logs only)  
**Note:** Routes to FlexiblePdfMergeService if no addendums configured

---

#### 3. /api/enrollment/generate-with-rules
**Controller:** EnrollmentPdfController  
**Service:** EnrollmentPdfService OR FlexiblePdfMergeService  
**Status:** ✅ Same monitoring as /api/enrollment/generate

**Phases Tracked:** Same as above  
**Difference:** Uses rule-based config selection instead of convention

---

## Phase Tracking Matrix

| Phase | DocumentController | EnrollmentPdfController | FlexiblePdfMergeService | EnrollmentPdfService |
|-------|-------------------|------------------------|------------------------|---------------------|
| Config Selection | - | ✅ | - | - |
| Payload Processing | - | ✅ | - | - |
| Load Config | - | - | ✅ | - |
| Apply Enrichers | - | - | ✅ | - |
| Resolve Sections | - | - | ✅ | - |
| Generate Sections | - | - | ✅ | - |
| Fill AcroForm | - | - | - | ✅ |
| Generate Dependent Addendum | - | - | - | ✅ |
| Generate Coverage Addendum | - | - | - | ✅ |
| Merge PDFs | - | - | ✅ | ✅ |
| Apply Headers/Footers | - | - | ✅ | - |
| Serialize to Bytes | - | - | ✅ | - |

## Service Selection Flow

```
EnrollmentPdfController
  │
  ├─ Config has addendums?
  │   │
  │   ├─ YES → EnrollmentPdfService (AcroForm + Addendums)
  │   │         ├─ Fill AcroForm
  │   │         ├─ Generate Dependent Addendum (if needed)
  │   │         ├─ Generate Coverage Addendum (if needed)
  │   │         └─ Merge PDFs
  │   │
  │   └─ NO → FlexiblePdfMergeService (FreeMarker sections)
  │             ├─ Load Config
  │             ├─ Apply Enrichers
  │             ├─ Resolve Sections
  │             ├─ Generate Sections
  │             ├─ Merge PDFs
  │             ├─ Apply Headers/Footers
  │             └─ Serialize to Bytes

DocumentController
  │
  └─ FlexiblePdfMergeService (always)
      ├─ Load Config
      ├─ Apply Enrichers
      ├─ Resolve Sections
      ├─ Generate Sections
      ├─ Merge PDFs
      ├─ Apply Headers/Footers
      └─ Serialize to Bytes
```

## Performance Log Examples

### DocumentController (/api/document/generate)
```
Performance Summary for Document Generation:
  Total Time: 2500ms
  Phases:
    Load Config: 100ms
    Apply Enrichers: 200ms
    Resolve Sections: 150ms
    Generate Sections: 1800ms
      Section: coverPage: 400ms
      Section: applicantInfo: 600ms
      Section: dependentInfo: 500ms
      Section: coverageInfo: 300ms
    Merge PDFs: 150ms
    Apply Headers/Footers: 50ms
    Serialize to Bytes: 50ms
```

### EnrollmentPdfController - No Addendums
```
Performance Summary for Enrollment PDF Generation:
  Total Time: 2300ms
  Phases:
    Config Selection: 150ms
    Payload Processing: 50ms
    PDF Generation: 2100ms
      Load Config: 100ms (FlexiblePdfMergeService)
      Apply Enrichers: 200ms
      Resolve Sections: 150ms
      Generate Sections: 1400ms
      Merge PDFs: 150ms
      Apply Headers/Footers: 50ms
      Serialize to Bytes: 50ms
```

### EnrollmentPdfController - With Addendums
```
Performance Summary for Enrollment PDF Generation:
  Total Time: 1250ms
  Phases:
    Config Selection: 150ms
    Payload Processing: 50ms
    PDF Generation: 1000ms
      Fill AcroForm: 300ms (EnrollmentPdfService)
      Generate Dependent Addendum: 450ms
      Generate Coverage Addendum: 150ms
      Merge PDFs: 100ms
```

## Monitoring Infrastructure

### Core Components
- **PerformanceMonitoringContext** - ThreadLocal context manager
- **PerformanceMetrics** - Metrics DTO with phase timing
- **Logger** - SLF4J logging with INFO/DEBUG levels

### Integration
- All monitoring uses the same ThreadLocal context
- Phases can be nested (service phases appear under controller phases)
- Context is cleared on exception to prevent memory leaks
- Thread-safe for concurrent requests

### Configuration
```yaml
# application.yml
logging:
  level:
    com.example.monitoring.PerformanceMonitoringContext: INFO  # Summary logs
    # com.example.monitoring.PerformanceMonitoringContext: DEBUG  # Detailed phase tracking
```

## Coverage Summary

✅ **100% Coverage** of PDF generation endpoints:
- `/api/document/generate` - Full monitoring with metrics endpoint
- `/api/enrollment/generate` - Full monitoring (logs only)
- `/api/enrollment/generate-with-rules` - Full monitoring (logs only)

🔧 **Extensible Design:**
- Easy to add new phases with `startPhase()/endPhase()`
- Can add metadata to phases with `endPhase(name, Map<>)`
- Ready for metrics export (Prometheus/Micrometer)
- ThreadLocal context prevents cross-request contamination

📊 **Metrics Available:**
- Phase durations (ms)
- Total request time
- Config name
- Number of pages/sections
- Addendum generation indicators
- Warnings/errors

## Related Files

- [PerformanceMonitoringContext.java](../pdf-generation-service/src/main/java/com/example/monitoring/PerformanceMonitoringContext.java)
- [PerformanceMetrics.java](../pdf-generation-service/src/main/java/com/example/monitoring/PerformanceMetrics.java)
- [DocumentController.java](../pdf-generation-service/src/main/java/com/example/pdf/controller/DocumentController.java)
- [EnrollmentPdfController.java](../pdf-generation-service/src/main/java/com/example/pdf/controller/EnrollmentPdfController.java)
- [FlexiblePdfMergeService.java](../pdf-generation-service/src/main/java/com/example/service/FlexiblePdfMergeService.java)
- [EnrollmentPdfService.java](../pdf-generation-service/src/main/java/com/example/service/EnrollmentPdfService.java)
