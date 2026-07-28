# Execution Log

## 2026-07-27

- User intent: 发布到测试服务器。
- Target scope: only test server `172.30.30.58`; forbidden actions are prod, backup, `mark-tested`, `promote-prod`, and `promote-backup`.
- Rules loaded: `ci-cd-environment-delivery`, `docs/server-access.md`, `docs/release-backup-restore.md`, `docs/task-closeout-rules.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/powershell-memory.md`, `docs/powershell-encoding.md`, `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md`, `docs/experience-index.md`.
- BDD: 测试服发布 OnlyOffice 修复 -> Given `origin/int_main` 已包含 DCC OnlyOffice 下载地址修复, When 发布 `origin/int_main` 到测试服务器, Then 测试服后端和前端运行在同一 releaseTag，后端 health 为 UP，前端 HTTP 200，且 release-info/manifest 指向本轮冻结提交。
- Baseline: `origin/int_main` commit `9562dca4982007f36c302aaa99847a59d6a4c28e` contains `application-local.yaml` `public-file-base-url` default `http://host.docker.internal:${server.port}`.
- Current workspace: local `HEAD` is ahead of `origin/int_main` and has unrelated dirty/untracked concurrent task files; this release will not use dirty main workspace as build input.
- GREEN: experience-preflight -> PASS；matched test-only release, server access, worktree, PowerShell, and local OnlyOffice Docker URL gates.
