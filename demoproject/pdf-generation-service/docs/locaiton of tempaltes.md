Searched for regex `freemarker.*template|template.*location|classpath.*templates|file.*templates` (`**/*.yml,*.yaml,*.properties,*Service.java,*Config.java`), no results

Searched for regex `template|freemarker|acroform` (`**/demoproject/pdf-generation-service/**/*.yml`), 20 results

Searched codebase for "FreemarkerService template location base path classpath resources templates directory configuration", 21 results

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/PdfMergeConfigService.java#1-1), lines 1 to 50

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/application.yml#1-1), lines 1 to 50

Perfect! I found the configuration. Here's the answer:

## Default Template Folders Configuration

### Local File System Path

When **not** fetched from config-server, templates are loaded from a local folder configured in application.yml:

```yaml
config:
  repo:
    path: ../config-repo  # Default value
```

This is injected into PdfMergeConfigService.java:

```java
@Value("${config.repo.path:../config-repo}")
private String configRepoPath;
```

### Template Resolution Paths

**FreeMarker Templates** (FreemarkerService.java):
1. **Classpath**: `src/main/resources/` (packaged in JAR)
2. **File system**: Relative to working directory (`.`)
3. **HTTP/HTTPS**: Remote URLs

**AcroForm PDF Templates**:
- Located in: `{configRepoPath}/acroforms/`
- Example: `../config-repo/acroforms/enrollment-form.pdf`

### Directory Structure

```
config-repo/                          # ← configRepoPath
├── acroforms/                        # AcroForm PDF templates
│   ├── enrollment-form.pdf
│   └── ca-dmhc-disclosure-2025.pdf
├── templates/                        # FreeMarker templates (local)
│   ├── member-healthcare-plans.ftl
│   ├── enrollment-cover.ftl
│   └── products/
│       ├── medical.ftl
│       └── dental.ftl
└── dental-individual-ca.yml          # Config files

src/main/resources/templates/          # FreeMarker (classpath)
├── footer.ftl
├── header.ftl
└── components.ftl
```

### Changing the Default

You can override in **application.yml**:
```yaml
config:
  repo:
    path: /absolute/path/to/config-repo
```

Or via **environment variable**:
```bash
CONFIG_REPO_PATH=/path/to/configs java -jar app.jar
```