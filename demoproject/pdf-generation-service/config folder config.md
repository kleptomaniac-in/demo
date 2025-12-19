Searched for regex `config.repo|config-repo|configRepoPath` (`**/*.{yml,yaml,properties,java}`), 12 results

Searched for files matching `**/application*.yml`, 10 matches

Searched for files matching `**/application*.properties`, 1 match

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/application.yml)

Read [](file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/PdfMergeConfigService.java#20-20), lines 20 to 80

Perfect! I found it. The local folder is configured using the **`@Value`** annotation with a default value:

## Configuration Location

**In [PdfMergeConfigService.java](cci:1://file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/PdfMergeConfigService.java:28:0-28:0):**

```java
@Value("${config.repo.path:../config-repo}")
private String configRepoPath;
```

**And in [ExcelMergeConfigService.java](cci:1://file:///workspaces/demo/demoproject/pdf-generation-service/src/main/java/com/example/service/ExcelMergeConfigService.java:24:0-24:0):**

```java
@Value("${config.repo.path:../config-repo}")
private String configRepoPath;
```

## How It Works

1. **Property Name:** `config.repo.path`
2. **Default Value:** `../config-repo` (relative to where the service is running)
3. **Override via:** You can set it in application.yml or environment variables

## Where to Configure It

You can set the local folder path in **application.yml**:

```yaml
# Add this to application.yml
config:
  repo:
    path: ../config-repo  # or any other path
```

Or via **environment variable**:
```bash
export CONFIG_REPO_PATH=/workspaces/demo/demoproject/config-repo
```

Or via **command line**:
```bash
java -jar app.jar --config.repo.path=/path/to/config-repo
```

## Current Behavior (from [application.yml](cci:1://file:///workspaces/demo/demoproject/pdf-generation-service/src/main/resources/application.yml:10:0-21:0))

1. **Config Server**: `http://localhost:8888` (line 11)
2. **Fail-Fast**: `false` (line 21) - Won't fail if config server unavailable
3. **Enabled**: `false` by default (line 20)
4. **Fallback**: Uses `${config.repo.path:../config-repo}` when config server fails

The `../config-repo` path is relative to the working directory where the service runs, which is typically pdf-generation-service, so it resolves to config-repo.