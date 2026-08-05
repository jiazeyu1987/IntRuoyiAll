# Docker Ruoyi Unused Object Cleanup

## Task Goal

Delete Docker images, volumes, and containers not needed by the current IntRuoyi local runtime, while preserving running Ruoyi dependencies and their data volumes.

## Milestones

- [x] Record current Docker containers, images, volumes, and disk usage.
- [x] Delete stopped containers not used by current Ruoyi runtime.
- [x] Delete volumes with no container links after stopped-container cleanup.
- [x] Delete images no longer referenced by any container.
- [x] Verify current Ruoyi containers remain running and Docker unused objects are gone.

## Expected Verification

- Current running containers remain: `int-ruoyi-mysql`, `int-ruoyi-redis`, `onlyoffice`, `docker-minio-1`.
- Preserve active data volumes: MySQL volume, `docker_minio_data`, and `intkb_onlyoffice_*` volumes.
- `docker system df` before and after cleanup.
- No `docker system prune --volumes` and no manual VHDX deletion.

## Current Status

completed

## Applicable Experience Gate

- `docs/local-runtime.md#2026-08-05-本机-docker-未使用镜像清理门禁` applies because the user requested Docker cleanup for D drive pressure and Docker Desktop WSL disk growth.
- This task had explicit user authorization for image, volume, and container cleanup, so the cleanup scope was widened from image-only to stopped non-current containers, dangling volumes, and images no longer referenced by containers.
- Guardrail applied: preserve running Ruoyi containers and all linked data volumes; do not delete `docker_data.vhdx`; do not remove current MySQL, MinIO, or OnlyOffice volumes.

## Closeout Evidence

- `task-closeout-cleanup` preview found no blocked paths or warnings.
- `task-closeout-cleanup` apply deleted only this task's intermediate Docker evidence files and kept `task.md`, `execution-log.md`, and `verification-report.md`.
- Final Docker verification confirmed no additional unused image or volume space reclaimable through Docker prune commands.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按当前容器挂载关系删除未引用资源，保留当前 Ruoyi 数据链路。
- `是否存在临时补丁或绕过`：否。
