# Execution Log

## User Intent

- User asked to delete Docker images, volumes, and containers not used by the current Ruoyi system environment.

## Scope

- Preserve running current Ruoyi runtime containers and linked volumes.
- Delete stopped non-current containers, dangling volumes, and images with no container references.

## Milestone Evidence

- BDD: Cleanup unused Docker objects -> Given the current Ruoyi runtime uses MySQL, Redis, OnlyOffice, and MinIO containers, When unused Docker objects are removed, Then those containers and their linked data volumes remain available.
- RED: `docker system df` and `docker container ls -a` -> FAIL, found stopped non-current containers (`yudao-redis`, `docker-redis-1`, `docker-es01-1`), dangling volumes, and images no longer needed by the current Ruoyi runtime.
- GREEN: `docker rm yudao-redis docker-redis-1 docker-es01-1` -> PASS, removed only stopped containers outside the current running Ruoyi dependency set.
- GREEN: removed dangling/unlinked volumes -> PASS, deleted obsolete Redis, Elasticsearch, sub2api cache, only-office orphan, and anonymous volumes after confirming current data volumes remained linked.
- GREEN: removed unused images -> PASS, deleted `valkey/valkey:8` and `elasticsearch:8.11.3`; final image and volume prune checks reclaimed `0B`.
- REGRESSION: `docker container ls -a` -> PASS, only `int-ruoyi-mysql`, `int-ruoyi-redis`, `onlyoffice`, and `docker-minio-1` remain and all are running.
- REGRESSION: volume reference scan -> PASS, every remaining Docker volume is linked to a current running container.
- Verification: `docker system df` -> PASS, final state is Images 4 / Containers 4 / Local Volumes 9 / Build Cache 0B.
- Verification: D drive check -> PASS, `D:\Docker\DockerDesktopWSL\disk\docker_data.vhdx` remains 426.87 GiB and Windows-visible free space is about 70.29 GiB; VHDX compaction still requires elevated/admin execution as a separate step.
- Cleanup: `task-closeout-cleanup --mode preview` -> PASS, kept `task.md`, `execution-log.md`, `verification-report.md`, and listed only task-local intermediate evidence files for deletion.
- Cleanup: `task-closeout-cleanup --mode apply` -> PASS, deleted only the previewed task-local intermediate files.
