# PDF Generation Service — HTML Template Example

This tiny README shows how to run the `pdf-generation-service` locally and exercise the HTML-to-PDF path using the provided sample template and mapping.

Files added for example:

- `src/main/resources/templates/invoice.html` — sample invoice HTML template
- `src/main/resources/mappings/sample-invoice-mapping.yml` — YAML mapping pointing at the template
- `src/main/resources/mappings/sample-invoice-mapping.json` — same mapping as JSON
- `request.json` — sample POST body you can use to generate a PDF (uses `mappingOverride`)

Run the service (from repository root):

```bash
cd demoproject/pdf-generation-service
mvn -f pom.xml spring-boot:run
```

Or build the jar and run:

```bash
mvn -f pom.xml clean package -DskipTests
java -jar target/pdf-generation-service-0.0.1-SNAPSHOT.jar
```

Generate a PDF using the sample request (saves as `invoice.pdf`):

```bash
curl -s -X POST http://localhost:8080/generate \
  -H 'Content-Type: application/json' \
  --data-binary @request.json \
  --output invoice.pdf

# open invoice.pdf with a PDF viewer
```

Notes and next steps:

- The example uses a simple placeholder replacer; the template expects a pre-rendered `itemsHtml` fragment for table rows. If you need loops/conditionals, consider integrating a proper template engine (Thymeleaf or Mustache).
- If you want the template to be fetched from an HTTP URL or a filesystem path, update the `mappingOverride.template.url` accordingly.
# PDF Generation Service

Overview
- **Purpose:** Produces PDF documents by composing mapping fragments (YAML) fetched from a central Spring Cloud Config repository and rendering templates.
- **Key features:** Externalized mapping order, mapping composition (deep merge + unflatten), Config Server client integration, actuator diagnostics.

Architecture & config
- **Config Server:** The service reads runtime configuration from a Spring Cloud Config Server (default `http://localhost:8888`). The import is enabled in `application.yml` with `spring.config.import: optional:configserver:${config-server.url}`.
- **Application name:** The service identifies itself using `spring.application.name=pdf-generation-service` so the Config Server serves `pdf-generation-service.yml`.
- **mapping.candidate-order:** The order of mapping candidates is externalized into the Config Repo (see `config-repo/pdf-generation-service.yml`) under `mapping.candidate-order`. Candidates may be:
  - file paths (repo-relative), e.g. `file:mappings/base-application.yml` or `mappings/base-application.yml` (the code canonicalizes and will append `.yml` if missing)
  - application names (published under an application property source), e.g. `app:order-service-invoice-v2` or simply `order-service-invoice-v2` (no slash → treated as app unless `file:` prefix used)
- **Explicit prefixes:** Use `file:` or `app:` to remove ambiguity. Example canonical entries used in the repo:

```
mapping:
  candidate-order:
    - file:mappings/base-application.yml
    - file:mappings/templates/{template}.yml
    - file:mappings/products/{product}.yml
```

Mapping composition
- `MappingComposer` accepts an ordered list of candidate patterns, fetches each fragment via a mapping source (file or application), unflattens dotted keys, wraps `pdf` into `mapping.pdf` if needed, and deep-merges fragments in order.
- The composer uses a per-compose in-memory cache so identical candidate keys (e.g. `mappings/foo` and `mappings/foo.yml`) are fetched only once.

Endpoints & diagnostics
- `/internal/mapping-order` — returns the effective `mapping.candidate-order` list used by the service (handy for debugging remote config binding).
- Actuator: `/actuator/configprops` shows `mappingProperties` and the `inputs` origins (which file in the config repo supplied the values).

Local development: build & run
- Build the service jar:
```bash
mvn -f demoproject/pdf-generation-service -DskipTests package
```
- Run the Config Server (if not running) so the service can fetch its config:
```bash
mvn -f demoproject/config-server -DskipTests package
nohup java -jar demoproject/config-server/target/config-server-0.0.1-SNAPSHOT.jar > demoproject/config-server/server.log 2>&1 &
```
- Start the PDF service (ensure `spring.application.name` is set):
```bash
nohup java -Dspring.application.name=pdf-generation-service -jar demoproject/pdf-generation-service/target/pdf-generation-service-0.0.1-SNAPSHOT.jar > demoproject/pdf-generation-service/service.log 2>&1 &
echo $! > /tmp/pdf_generation_service.pid
```

Quick checks
- View mapping order:
```bash
curl http://localhost:8080/internal/mapping-order
```
- Inspect bound config props:
```bash
curl http://localhost:8080/actuator/configprops | jq '.contexts["pdf-generation-service"].beans.mappingProperties'
```

Testing
- Unit tests: `mvn -f demoproject/pdf-generation-service test` (the project includes tests for `MappingComposer` and `ConfigServerClient`).

Operational tips
- If the Config Server is unavailable when the service starts, the client will treat the config import as `optional` and the bean may initialize with defaults or empty lists — restart the service after the Config Server is available, or enable runtime refresh (`/actuator/refresh`) if you add Spring Cloud Refresh.
- The config repo in this workspace is a Git repository at `demoproject/config-repo`. It is local by default; you can add a remote and push it when desired.

Where to look in the code
- `MappingComposer` — composition and dispatch rules.
- `ConfigServerClient` — fetch + YAML/JSON parsing and per-segment encoding for file fetches.
- `MappingProperties` — bound `mapping.candidate-order` via `@ConfigurationProperties`.

If you want any of these sections expanded (examples of mapping fragments, a sample integration script, or instructions to enable auto-refresh), tell me which and I'll add it.
