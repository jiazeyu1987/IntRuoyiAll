# IntRuoyi Internal Backend Base Image Packaging Design

## Purpose and Scope

This document defines the target packaging design for IntRuoyi backend release images. The goal is to stop daily `build-release` runs from rebuilding the backend runtime base layer and from accessing Docker Hub or Ubuntu apt sources on every package build.

In scope:

- Backend release image base-layer design.
- Internal base image build, verification, versioning, and distribution.
- Daily release package behavior after the base image exists.
- Fail-fast behavior when the internal base image prerequisite is missing.
- Release manifest and operational evidence requirements.

Out of scope for this design-only task:

- Changing `script/deploy/int-ruoyi-test/Dockerfile.backend`.
- Changing `script/deploy/publish-int-ruoyi.ps1`.
- Selecting the final internal registry, credential owner, or retention policy.
- Deploying or promoting any server runtime.

## Evidence Reviewed

- User-provided release failure logs on 2026-06-04:
  - Floating `eclipse-temurin:21-jre` resolved to Ubuntu `resolute`.
  - `apt-get update` failed on `resolute/universe` with connection failures and 404.
  - A later verification build using `noble` passed package index fetch but still failed downloading `docker.io` from Ubuntu package nodes due container-network connection failure.
- Current `script/deploy/int-ruoyi-test/Dockerfile.backend`:
  - `FROM eclipse-temurin:21-jre`
  - `apt-get install python3 docker.io docker-compose-v2`
- Current `script/deploy/publish-int-ruoyi.ps1`:
  - Builds `intruoyi-backend:<packageDirectoryName>`.
  - Saves backend/frontend images into `intruoyi-images_<packageDirectoryName>.tar`.
  - Deploy flow loads the tar on the target server with `docker load`.
- Prior task evidence:
  - `doc/tasks/20260603-backend-dockerfile-lts-base`
  - `doc/tasks/20260603-restore-release-code-before-dockerhub-preflight`
  - `doc/tasks/20260604-backend-dockerfile-lts-base-repin`

## Configuration

Introduce explicit release build configuration when the design is implemented:

- `INTRUOYI_BACKEND_RUNTIME_BASE_IMAGE`
  - Required.
  - Example: `registry.internal/intruoyi/intruoyi-backend-runtime-base:2026.06.04-jre21-noble-docker29`.
- `INTRUOYI_BACKEND_RUNTIME_BASE_DIGEST`
  - Required for production-grade release builds.
  - Example: `sha256:<digest>`.
- `INTRUOYI_BACKEND_RUNTIME_BASE_VERSION`
  - Required.
  - Human-readable version recorded in release manifest.
- `INTRUOYI_BACKEND_RUNTIME_BASE_MODE`
  - Allowed values: `registry` or `offline-tar`.
  - This is a selected distribution mode, not an automatic fallback. Missing artifacts in the selected mode must fail.
- `INTRUOYI_BACKEND_RUNTIME_BASE_TAR`
  - Required only when mode is `offline-tar`.
  - Points to a pre-approved internal base image tar, not a generated-on-demand cache.

Missing or malformed values must fail before Maven, Vite, Docker build context transfer, or NAS upload.

## Secrets

No new secret may be committed to the repository.

Expected secret/runtime inputs:

- Internal registry read credentials for daily release builds, if `registry` mode is selected.
- Internal registry write credentials for the controlled base image build pipeline.
- NAS path access if `offline-tar` mode is selected.

Secret ownership must be assigned before implementation. The release script must report missing credentials clearly and stop; it must not switch to Docker Hub or a public mirror.

## Permissions

Two operational roles are required:

- Base image maintainer:
  - Builds and updates `intruoyi-backend-runtime-base`.
  - Approves JRE, OS, Python, Docker CLI, and Compose upgrades.
  - Pushes to the internal registry or publishes the approved offline tar.
- Release package operator:
  - Runs daily `build-release`.
  - Has read-only access to the approved internal base image.
  - Cannot silently rebuild the base image as part of daily release packaging.

Runtime-control UI permissions do not need to change for the design phase, but the implementation must expose base image status and failures in operation logs.

## Security Controls

- Pin daily release builds to an internal base image digest.
- Record base image name, version, and digest in `release-manifest.json`.
- The base image build must produce a manifest containing:
  - upstream source image and digest,
  - OS release,
  - installed package versions,
  - Java version,
  - Python version,
  - Docker CLI version,
  - Docker Compose version,
  - build time,
  - builder identity,
  - verification commands and results.
- Do not use public Docker Hub or Ubuntu apt in daily `build-release`.
- Do not remove `python3`, Docker CLI, or Compose from the backend runtime image unless the Linux runtime-control operations are redesigned first.
- Do not use local cache as proof of availability.

## Deployment

### Target State

Split the current single build path into two explicit pipelines.

### Pipeline A: Controlled Base Image Build

Runs only when the base runtime layer changes.

1. Pull approved upstream JRE LTS image, such as `eclipse-temurin:21-jre-noble`, from an explicitly approved environment.
2. Install runtime tools:
   - `python3`
   - `docker.io`
   - `docker-compose-v2`
3. Run self-checks:
   - `java -version`
   - `python3 --version`
   - `docker --version`
   - `docker compose version`
   - OS release readback.
4. Build `intruoyi-backend-runtime-base:<version>`.
5. Push to internal registry or export a signed/hashed offline tar.
6. Record base manifest and digest.

### Pipeline B: Daily Release Package Build

Runs for normal `build-release`.

1. Read required base image config and digest.
2. Verify selected internal base image exists.
3. Build backend jar with Maven.
4. Build frontend and Website assets.
5. Build backend app image from internal base image:
   - no `apt-get update`;
   - no package install;
   - copy only `yudao-server.jar`;
   - set runtime env and command.
6. Build frontend image as current design already does.
7. `docker save` backend/frontend images into the release package tar.
8. Write release manifest with app package tag and base image metadata.
9. Upload package to NAS.

### Deploy Release

Remote deploy remains conceptually unchanged:

1. Copy release image tar to the target server.
2. `docker load -i <imageTar>`.
3. Start services with the package image tag.
4. Verify HTTP readiness and runtime smoke checks.

## Observability

Operation logs must include:

- selected base image name,
- selected base image digest,
- base image version,
- distribution mode,
- internal registry or offline tar path,
- whether daily build used external Docker Hub or apt sources; expected value is `false`.

Release manifest must include:

- `backendRuntimeBaseImage`
- `backendRuntimeBaseDigest`
- `backendRuntimeBaseVersion`
- `backendRuntimeBaseMode`

Base image build evidence must be retained with the base image version.

## Open Questions

- Which internal distribution target is approved: private registry, NAS offline tar, or both as separately selected modes?
- Who owns base image maintenance and approval?
- What naming convention should be used for base image versions?
- What retention policy applies to old base images?
- Should base image manifest evidence live in NAS, Git, registry labels, or all three?
- Should the release script refuse non-digest tags in all environments or only production-grade targets?

## Design Blockers

- Internal registry or offline tar storage location is not approved.
- Credentials and owners for base image publish/read access are not assigned.
- Base image versioning and retention policy are not approved.
- Implementation cannot begin safely until the selected distribution mode is confirmed.

## Implementation Notes for the Next Task

The implementation task should be split into milestones:

1. Add contract tests for the new base image config and manifest fields.
2. Add `Dockerfile.backend-base` or equivalent controlled base image definition.
3. Add a base-image build script that produces manifest evidence.
4. Change daily `Dockerfile.backend` to use the internal base image and only copy jar.
5. Change `publish-int-ruoyi.ps1` to fail fast when the selected base image/digest is missing.
6. Run local contract tests and one real base-image build in the approved environment.
