# ADR-0001: Use an Internal Fixed Backend Runtime Base Image for Release Packages

## Status

Proposed. The user approved the design direction on 2026-06-04; implementation still requires selecting the internal distribution target, owners, credentials, and retention policy.

## Context

IntRuoyi `build-release` currently builds the backend runtime image during each release package build. The backend Dockerfile starts from a public `eclipse-temurin` image and installs runtime tools with Ubuntu apt:

- `python3`
- `docker.io`
- `docker-compose-v2`

This makes normal release packaging depend on external Docker Hub metadata and Ubuntu package availability. On 2026-06-04, release packaging failed after the floating `eclipse-temurin:21-jre` tag resolved to Ubuntu `resolute`; apt failed to fetch `resolute/universe` package metadata. A later verification attempt with an LTS base avoided `resolute` but still failed downloading a Ubuntu package due Docker/BuildKit network instability.

## Decision Drivers or Forces

- Daily release packaging must be stable and repeatable.
- Missing prerequisites must fail fast and clearly.
- No fallback to public mirrors, floating tags, stale cache, or mock success is allowed by project policy.
- Backend runtime-control Linux operations currently require Python, Docker CLI, and Docker Compose inside the backend image.
- External dependency access should happen in a controlled maintenance workflow, not every business release.
- Release artifacts must remain deployable through the current image tar + `docker load` deployment path.

## Options Considered

### Option A: Keep Current Daily Dockerfile Build

Continue using public `eclipse-temurin` and apt install during every `build-release`.

### Option B: Pin Public LTS Base Image in Daily Build

Change daily Dockerfile to `eclipse-temurin:21-jre-noble` and keep apt install in every release build.

### Option C: Use Internal Fixed Backend Runtime Base Image

Build a company-controlled `intruoyi-backend-runtime-base:<version>` only when runtime dependencies change. Daily release builds use that internal base image and only copy the current jar.

### Option D: Remove Docker CLI/Python/Compose from Backend Image

Make backend image smaller and move runtime-control operations elsewhere.

## Decision

Choose Option C as the target architecture.

Daily release package builds must use an approved internal backend runtime base image by explicit version and digest. The base image build becomes a separate controlled pipeline with its own verification evidence. Daily `build-release` must fail fast if the approved internal base image is unavailable; it must not silently fall back to Docker Hub, public apt sources, or local cache.

## Consequences

Positive:

- Normal release builds no longer fetch JRE images or apt packages from external sources.
- Runtime dependency changes become deliberate, reviewable, and traceable.
- Release manifests can record the exact base image digest used by each package.
- Docker Hub and Ubuntu apt outages stop blocking ordinary business code packaging once the internal base image is available.

Negative:

- A new base image maintenance workflow is required.
- Internal registry or offline image-tar storage must be approved.
- Base image vulnerabilities and upgrades need ownership and cadence.
- Implementation requires changes to Dockerfile, publish script, tests, and release evidence.

## Rejected Options

- Reject Option A because it repeats the current failure mode on every release.
- Reject Option B as the final design because it still requires Ubuntu apt during every daily build, even though it reduces floating tag drift.
- Reject Option D for now because runtime-control Linux operations currently depend on Python, Docker CLI, and Docker Compose inside the backend container; moving those operations is a larger architecture change.
- Reject local Docker cache as a release mechanism because it hides missing prerequisites and is not reproducible across machines.

## Verification and Operational Impact

Implementation must add tests and release checks that prove:

- daily backend Dockerfile does not run `apt-get`;
- daily build reads an internal base image config and digest;
- missing base image config blocks before Maven/Vite/Docker build work starts;
- release manifest records base image name, version, digest, and distribution mode;
- base image build evidence records Java, Python, Docker CLI, Compose, and OS versions;
- deploy-release still works with the existing image tar and `docker load` path.

Operational impact:

- Base image builds become a controlled maintenance task.
- Daily release package operators need read access to the internal base image source.
- External source access is isolated to the base image maintenance environment.

## Revisit or Rollback Conditions

Revisit this decision if:

- runtime-control Linux operations are moved out of the backend container;
- the organization approves a managed internal registry with vulnerability scanning that changes digest/retention requirements;
- Docker CLI and Compose are no longer required in backend runtime;
- release packaging moves from image tar transfer to a registry-pull deployment model.

Rollback condition:

- If internal base image distribution cannot be approved, implementation must stop. Do not reintroduce a silent public-source fallback; continue to fail fast and report the missing approved base image prerequisite.

## References and Owners

- Design task: `doc/tasks/20260604-internal-backend-base-image-packaging-design`
- Superseded direct Dockerfile task: `doc/tasks/20260604-backend-dockerfile-lts-base-repin`
- Prior LTS pin attempt: `doc/tasks/20260603-backend-dockerfile-lts-base`
- Prior explicit rollback: `doc/tasks/20260603-restore-release-code-before-dockerhub-preflight`
- Proposed owner: runtime/release operations owner, pending assignment.
