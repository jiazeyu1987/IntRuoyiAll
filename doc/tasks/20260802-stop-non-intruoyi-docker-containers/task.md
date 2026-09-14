# 20260802 Stop Non-IntRuoyi Docker Containers

## Task Goal

Stop currently running local Docker containers that are not related to IntRuoyi. Do not delete images, containers, volumes, or networks.

## Milestones

- [x] Record current running Docker containers and ownership classification.
- [x] Stop only running non-IntRuoyi containers with clear ownership.
- [x] Verify non-IntRuoyi containers are no longer running.
- [x] Preserve IntRuoyi-related containers and local dependencies.

## Expected Verification

- Running non-IntRuoyi container count is `0`.
- IntRuoyi-related containers remain in their previous running state unless they were already stopped.
- No image, volume, network, or container is deleted.

## Current Status

completed

## Experience Gate

Read `docs/experience-index.md`; existing `docs/local-runtime.md` Docker gate covers this task, so no new long-term experience document was created.

## Cleanup Keep

- doc/tasks/20260802-stop-non-intruoyi-docker-containers/task.md
- doc/tasks/20260802-stop-non-intruoyi-docker-containers/execution-log.md
- doc/tasks/20260802-stop-non-intruoyi-docker-containers/verification-report.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过停止无关运行容器减少本机运行噪音，不删除任何资产。
- `是否存在临时补丁或绕过`：否。
