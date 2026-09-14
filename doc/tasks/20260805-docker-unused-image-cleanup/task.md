# Docker Unused Image Cleanup

## Task Goal

Clean Docker images that are not referenced by any container, without deleting containers, volumes, or application data.

## Milestones

- [x] Inspect current repository and task rules before environment changes.
- [x] Capture Docker disk usage before cleanup.
- [x] Run Docker unused image cleanup.
- [x] Capture Docker disk usage after cleanup and record the result.

## Expected Verification

- `docker system df` before and after cleanup.
- `docker image prune -a -f` completes successfully.
- No Docker volumes are pruned by this task.

## Current Status

completed

## Experience Gate Summary

- `docs/experience-index.md` exists and was read.
- No exact Docker image-prune-specific experience gate was identified.
- Related local-runtime Docker dependency guidance means volumes and running container data must not be pruned unless explicitly requested.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本次只清理 Docker 未使用镜像，避免手动删除 Docker Desktop VHDX。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260805-docker-unused-image-cleanup/task.md
- doc/tasks/20260805-docker-unused-image-cleanup/execution-log.md
- doc/tasks/20260805-docker-unused-image-cleanup/verification-report.md
