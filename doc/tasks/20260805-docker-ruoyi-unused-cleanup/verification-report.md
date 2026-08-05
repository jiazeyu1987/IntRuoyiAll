# Verification Report

## Scope

- Deleted Docker images, volumes, and containers not used by the current local Ruoyi runtime.
- Preserved running Ruoyi dependencies and their linked data volumes.
- Did not delete `D:\Docker\DockerDesktopWSL\disk\docker_data.vhdx`; VHDX compaction remains a separate administrator step.

## Deleted Objects

- Stopped containers removed: `yudao-redis`, `docker-redis-1`, `docker-es01-1`.
- Images removed: `valkey/valkey:8`, `elasticsearch:8.11.3`.
- Obsolete volumes removed: `docker_esdata01`, `docker_redis_data`, `yudao-system_redis`, `sub2api-go-build-cache`, `sub2api-go-mod-cache`, old `only-office_*` volumes, and anonymous dangling volumes.

## Preserved Runtime

- `int-ruoyi-mysql` using `mysql:8.0.39`, running.
- `int-ruoyi-redis` using `redis:8-alpine`, running.
- `onlyoffice` using `onlyoffice/documentserver:latest`, running.
- `docker-minio-1` using `quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z`, running and healthy.

## Preserved Volumes

- `9760f222f4235bdf4153a111e80f5f05a5324e202faccbdcf35c0175aa85526a` -> `int-ruoyi-mysql` `/var/lib/mysql`, about 44.18 GB.
- `docker_minio_data` -> `docker-minio-1` `/data`, about 24.64 GB.
- `intkb_onlyoffice_data`, `intkb_onlyoffice_fonts`, `intkb_onlyoffice_lib`, `intkb_onlyoffice_log`, `intkb_onlyoffice_postgresql`, `intkb_onlyoffice_rabbitmq`, `intkb_onlyoffice_redis` -> `onlyoffice`.

## Final Verification

- `docker image prune -a -f` -> `Total reclaimed space: 0B`.
- `docker volume prune -f` -> `Total reclaimed space: 0B`.
- `docker container ls -a` -> only 4 current runtime containers remain, all running.
- Volume reference scan -> all 9 remaining volumes are linked to current containers.
- `docker system df` -> Images 4, Containers 4, Local Volumes 9, Build Cache 0B.
- `D:\Docker\DockerDesktopWSL\disk\docker_data.vhdx` -> 458,346,201,088 bytes, about 426.87 GiB.
- `Get-PSDrive -Name D` -> latest free space 99,037,368,320 bytes, about 92.24 GiB.
