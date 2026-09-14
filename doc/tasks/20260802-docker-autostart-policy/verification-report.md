# Verification Report

## Scope

Local Docker restart policy enforcement on `E:\IntRuoyi` host only.

## Classification Used

- IntRuoyi-related: container or Docker Compose project clearly tied to `intruoyi`, `int-ruoyi`, `yudao`, or documented local IntRuoyi dependencies such as OnlyOffice.
- Non-IntRuoyi: unrelated generic Docker Compose stacks, including the `docker_ragflow` stack.

## Before

`docker inspect` showed these non-IntRuoyi containers were allowed to autostart:

- `docker-redis-1` / `valkey/valkey:8` / `unless-stopped`
- `docker-minio-1` / `quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z` / `unless-stopped`
- `docker-es01-1` / `elasticsearch:8.11.3` / `unless-stopped`

## Change Applied

```powershell
docker update --restart no docker-redis-1
docker update --restart no docker-minio-1
docker update --restart no docker-es01-1
```

## After

`docker inspect` showed:

- `docker-redis-1` / `valkey/valkey:8` / `no` / running
- `docker-minio-1` / `quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z` / `no` / running
- `docker-es01-1` / `elasticsearch:8.11.3` / `no` / exited

Final verification result: PASS. Non-IntRuoyi autostart violation count is `0`.

## Notes

No container was stopped, started, deleted, or recreated.
