# 20260802 Docker Autostart Policy

## Task Goal

Restrict local Docker boot autostart so only IntRuoyi-related containers are allowed to start automatically. Non-IntRuoyi containers must have Docker restart policy set to `no`.

## Milestones

- [x] Capture current local Docker restart policies.
- [x] Identify IntRuoyi-related versus non-IntRuoyi containers.
- [x] Disable autostart for non-IntRuoyi containers only.
- [x] Verify resulting Docker restart policies.

## Expected Verification

- `docker inspect` shows non-IntRuoyi containers have `HostConfig.RestartPolicy.Name = no`.
- IntRuoyi-related containers retain their existing restart policy.
- No container is deleted or stopped as part of this task.

## Current Status

completed

## Experience Gate

Read `docs/experience-index.md`; matching local runtime policy belongs in `docs/local-runtime.md`.

## Cleanup Keep

- doc/tasks/20260802-docker-autostart-policy/task.md
- doc/tasks/20260802-docker-autostart-policy/execution-log.md
- doc/tasks/20260802-docker-autostart-policy/verification-report.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过 Docker restart policy 控制开机自启。
- `是否存在临时补丁或绕过`：否。
