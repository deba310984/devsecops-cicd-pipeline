# Implementation Notes

## What this project is
An **advanced CI/CD pipeline with security built in** (DevSecOps). A small Java
HTTP service is the payload; the value is the pipeline that builds, tests, scans
across five dimensions, containerises, and ships it — with security checks woven
through rather than bolted on at the end.

## The pipeline (`.github/workflows/devsecops.yml`)

| Stage | Tool | Checks for | Posture |
|-------|------|------------|---------|
| Build & Test | Maven + JUnit | compilation, unit tests | **gate** — fails the build |
| Secret Scan | gitleaks | committed credentials/keys | **gate** — a leak always stops the pipeline |
| SAST | Semgrep | insecure code patterns | report-only |
| Dependencies + FS | Trivy (`fs`) | vulnerable dependencies, misconfig, secrets | report-only |
| IaC | Checkov | insecure Terraform | soft-fail (report) |
| Image Scan | Trivy (`image`) | OS/library CVEs in the built image | report-only |
| SBOM | Syft | software bill of materials (SPDX) | artifact |

### Why these postures
- **Gates** are things that should *never* ship: broken code and leaked secrets.
- **Report-only** scans (SAST, dependency, IaC, image CVEs) publish findings as
  build artifacts so they are visible and triaged, without blocking every build
  on, say, an unpatched transitive CVE. In a real org these thresholds tighten
  over time (e.g. fail on new CRITICALs) — the structure is already here.

## Application & container security choices
- **Output encoding** — the `/echo` endpoint HTML-escapes untrusted input
  (`HtmlEscaper`), a baseline defence against reflected XSS, and it is unit-tested.
- **Multi-stage image** — the JDK/Maven build tooling never ships to production;
  the final image is a slim JRE plus the JAR.
- **Non-root container** — the app runs as `appuser` (uid 1001), so a container
  compromise does not start as root. (Verified: `id` inside the container.)
- **Health check** — a dependency-free TCP check lets Docker/orchestrators detect
  an unhealthy container.

## Infrastructure as Code
`infra/main.tf` provisions an S3 artifact bucket written to pass security scans:
encryption at rest, versioning, and a full public-access block. It is **scanned,
not applied**, in CI.

## Verification performed
- `mvn clean verify` — JAR built, 4 unit tests pass.
- Endpoints verified locally: `/`, `/health`, and `/echo` (confirmed it escapes
  `<script>` payloads).
- The image was run from the built JAR and confirmed to serve traffic **as a
  non-root user**.
- The full scanning pipeline runs in GitHub Actions on every push.

## Running it
- **Local app:** `docker compose`-style — `docker build -t app . && docker run -p 8080:8080 app` (in CI; locally the in-image Maven build needs internet).
- **Pipeline:** pushes trigger `.github/workflows/devsecops.yml`; the Jenkins
  equivalent is in `Jenkinsfile`.
