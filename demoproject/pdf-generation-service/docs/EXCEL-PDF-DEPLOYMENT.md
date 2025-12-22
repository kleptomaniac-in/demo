# Excel-to-PDF Deployment Options

## 📦 Dependency Options

The Excel-to-PDF feature can work in multiple configurations:

### Option 1: **With LibreOffice** (Full Features, Larger Image)

**Installation:**
```bash
# Ubuntu/Debian
apt-get install libreoffice-calc libreoffice-writer

# Alpine (Docker)
apk add libreoffice
```

**Dockerfile:**
```dockerfile
FROM openjdk:17-slim
RUN apt-get update && apt-get install -y \
    libreoffice-calc \
    libreoffice-writer \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

**Image Size:** ~800MB  
**Conversion Quality:** ⭐⭐⭐⭐⭐ Excellent  
**Endpoints:** All work (including `/generate-as-pdf`)

---

### Option 2: **Without LibreOffice** (Smaller Image, Excel-only)

**Dockerfile:**
```dockerfile
FROM openjdk:17-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

**Image Size:** ~300MB  
**Behavior:**
- ✅ `/api/excel/generate` - Works (returns Excel)
- ❌ `/api/excel/generate-as-pdf` - Returns 503 with helpful error
- ✅ `/api/excel/conversion-info` - Returns `{"available": false}`

**Client Workflow:**
```bash
# Get Excel file
curl -X POST .../api/excel/generate -o output.xlsx

# Convert locally
libreoffice --headless --convert-to pdf output.xlsx
```

---

### Option 3: **Commercial Library** (No LibreOffice, Paid)

**Add Aspose.Cells:**
```xml
<dependency>
    <groupId>com.aspose</groupId>
    <artifactId>aspose-cells</artifactId>
    <version>23.12</version>
</dependency>
```

**Update ExcelToPdfConverter:**
```java
// In convertToPdf method, before LibreOffice check:
if (isAsposeAvailable()) {
    return convertUsingAspose(excelData);
}

private byte[] convertUsingAspose(byte[] excelData) {
    Workbook workbook = new Workbook(new ByteArrayInputStream(excelData));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.save(out, SaveFormat.PDF);
    return out.toByteArray();
}
```

**Image Size:** ~350MB  
**Cost:** ~$1,500/year per developer  
**Conversion Quality:** ⭐⭐⭐⭐⭐ Excellent

---

### Option 4: **Cloud API** (No Dependencies, Pay-per-Use)

**Update ExcelToPdfConverter:**
```java
private byte[] convertUsingCloudApi(byte[] excelData) {
    RestTemplate rest = new RestTemplate();
    
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new ByteArrayResource(excelData));
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + apiKey);
    
    ResponseEntity<byte[]> response = rest.exchange(
        "https://api.convertapi.com/convert/xlsx/to/pdf",
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        byte[].class
    );
    
    return response.getBody();
}
```

**Image Size:** ~300MB  
**Cost:** $0.01-0.10 per conversion  
**Requirements:** Internet access, API key

---

## 🎯 Recommendation by Use Case

| Use Case | Recommended Option | Reason |
|----------|-------------------|--------|
| **Internal tools** | Option 1 (LibreOffice) | Free, high quality, self-contained |
| **Cloud deployment** | Option 1 or 4 | LibreOffice is standard in cloud VMs |
| **Serverless/Lambda** | Option 4 (Cloud API) | Keep function small |
| **High volume** | Option 3 (Aspose) | Best performance, no external calls |
| **Budget-conscious** | Option 1 (LibreOffice) | Free and reliable |
| **Air-gapped/Offline** | Option 1 or 3 | No internet required |

---

## 🐳 Docker Deployment Examples

### Minimal (No PDF Conversion)
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
**Size:** ~280MB

### With LibreOffice
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

# Install LibreOffice
RUN apt-get update && \
    apt-get install -y libreoffice-calc libreoffice-writer && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
**Size:** ~850MB

### Optimized LibreOffice (Debian Slim)
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install only required LibreOffice components
RUN apt-get update && \
    apt-get install -y \
        libreoffice-calc \
        libreoffice-core-nogui \
        --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
**Size:** ~650MB (25% smaller)

---

## 🔧 Configuration

### Make LibreOffice Optional via Configuration

**application.yml:**
```yaml
excel:
  conversion:
    enabled: ${EXCEL_PDF_ENABLED:true}
    method: ${EXCEL_PDF_METHOD:libreoffice} # libreoffice, aspose, cloudapi
    timeout: 30000
    cloudapi:
      url: ${CONVERTAPI_URL:}
      key: ${CONVERTAPI_KEY:}
```

**Updated ExcelToPdfConverter:**
```java
@Value("${excel.conversion.enabled:true}")
private boolean conversionEnabled;

@Value("${excel.conversion.method:libreoffice}")
private String conversionMethod;

public byte[] convertToPdf(byte[] excelData) throws IOException {
    if (!conversionEnabled) {
        throw new UnsupportedOperationException("Excel-to-PDF conversion is disabled");
    }
    
    switch (conversionMethod.toLowerCase()) {
        case "libreoffice":
            return convertUsingLibreOffice(excelData);
        case "aspose":
            return convertUsingAspose(excelData);
        case "cloudapi":
            return convertUsingCloudApi(excelData);
        default:
            throw new IllegalArgumentException("Unknown conversion method: " + conversionMethod);
    }
}
```

**Environment Variables:**
```bash
# Disable conversion (smaller Docker image)
EXCEL_PDF_ENABLED=false

# Use cloud API
EXCEL_PDF_METHOD=cloudapi
CONVERTAPI_KEY=your-api-key-here
```

---

## 📊 Comparison Matrix

| Feature | LibreOffice | Aspose | Cloud API | POI Manual |
|---------|-------------|--------|-----------|------------|
| **Cost** | Free | ~$1500/yr | ~$0.05/conv | Free |
| **Image Size** | +500MB | +50MB | 0 | 0 |
| **Quality** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **Speed** | 2-3s | 0.5-1s | 1-2s | 5-10s |
| **Offline** | ✅ | ✅ | ❌ | ✅ |
| **Maintenance** | Low | None | None | High |
| **Complexity** | Low | Very Low | Low | Very High |

---

## 💡 Best Practice: Feature Toggle

Keep the feature optional and document clearly:

```java
@GetMapping("/conversion-info")
public ConversionInfo getInfo() {
    return new ConversionInfo(
        isAvailable(),
        getMethod(),
        isAvailable() ? 
            "Excel-to-PDF conversion available" : 
            "Feature disabled. Set EXCEL_PDF_ENABLED=true and install LibreOffice"
    );
}
```

**API Response:**
```json
{
  "available": true,
  "method": "LibreOffice 24.2.7.2",
  "message": "Ready to convert",
  "endpoints": [
    "/api/excel/generate-as-pdf",
    "/api/excel/generate-from-config-as-pdf"
  ]
}
```

Or when disabled:
```json
{
  "available": false,
  "method": "None",
  "message": "Excel-to-PDF disabled. Use /generate for Excel output.",
  "alternatives": [
    "Install LibreOffice: apt-get install libreoffice-calc",
    "Enable cloud API: Set EXCEL_PDF_METHOD=cloudapi",
    "Download Excel and convert locally"
  ]
}
```

---

## 🚀 Quick Start Without LibreOffice

If you want to avoid the LibreOffice dependency entirely:

1. **Comment out** the conversion endpoints in `ExcelGenerationController`
2. **Remove** `ExcelToPdfConverter` injection
3. **Use** only `/api/excel/generate` endpoints (return Excel)
4. **Convert** client-side or via external tools

The service will work perfectly for Excel generation. PDF conversion is an **optional add-on feature**.

---

**Recommendation:** Start with **Option 1 (LibreOffice)** for simplicity. Move to paid solutions only if you need better performance or smaller images become critical.
