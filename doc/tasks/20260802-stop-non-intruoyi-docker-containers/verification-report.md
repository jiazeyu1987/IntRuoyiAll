# Verification Report

## Scope

Local Docker runtime cleanup on `E:\IntRuoyi` host only.

## Interpretation

The user asked to close/stop Docker items unrelated to IntRuoyi. Docker images cannot be stopped, so this task stopped currently running non-IntRuoyi containers and did not delete images.

## Stopped Containers

- `yudao-redis` / `redis:8-alpine` / Compose project `yudao-system`
- `docker-redis-1` / `valkey/valkey:8` / Compose project `docker`
- `docker-minio-1` / `quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z` / Compose project `docker`

## Preserved Running Containers

- `int-ruoyi-mysql` / `mysql:8.0.39`
- `int-ruoyi-redis` / `redis:8-alpine`
- `onlyoffice` / `onlyoffice/documentserver:latest`

`onlyoffice` was preserved as a documented local OnlyOffice dependency.

## Final Docker State

- `int-ruoyi-mysql` / running / restart policy `no`
- `int-ruoyi-redis` / running / restart policy `no`
- `onlyoffice` / running / restart policy `unless-stopped`
- `yudao-redis` / exited / restart policy `no`
- `docker-redis-1` / exited / restart policy `no`
- `docker-minio-1` / exited / restart policy `no`
- `docker-es01-1` / exited / restart policy `no`

Final verification result: PASS. `RUNNING_NON_INTRUOYI_CONTAINERS=0`.

## Safety Notes

No image, container, volume, or network was deleted. No IntRuoyi-related running container was stopped.
