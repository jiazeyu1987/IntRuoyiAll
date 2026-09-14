# Docker Build Cache And VHDX Compaction

## Task Goal

Free D drive space by cleaning Docker build cache and compacting Docker Desktop's WSL VHDX, without pruning Docker volumes or deleting containers.

## Milestones

- [x] Record pre-cleanup Docker, D drive, WSL, container, and VHDX state.
- [x] Run Docker build cache cleanup.
- [x] Stop Docker/WSL safely and attempt `docker_data.vhdx` compaction.
- [x] Record post-cleanup verification and remaining blockers.

## Expected Verification

- `docker system df` before and after build cache cleanup.
- D drive free space before and after VHDX compaction attempt.
- `docker image prune -a -f` and `docker builder prune -a -f` final idempotency where available.
- Confirm no `docker volume prune`, `docker system prune --volumes`, or container deletion was run.

## Current Status

blocked

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先释放 Docker 内部 build cache，再通过正式 VHDX 压缩把空间返还给 Windows。
- `是否存在临时补丁或绕过`：否。
## Blocker

Windows-visible D drive space is still blocked by VHDX compaction requirements. Docker build cache is clean, but `D:\Docker\DockerDesktopWSL\disk\docker_data.vhdx` remains 426.87GB because `diskpart compact vdisk` requires elevation and WSL sparse management rejected the available argument forms.