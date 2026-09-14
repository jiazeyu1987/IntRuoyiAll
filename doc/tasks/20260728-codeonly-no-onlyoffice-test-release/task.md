# 测试服程序-only 不含 OnlyOffice 发布

## Task Goal

将当前发布分支构建并发布到测试服务器 `172.30.30.58`，本轮只发布 IntRuoyi 后端/前端程序，不携带数据库 dump、MinIO snapshot、runtime-data，也不包含或重启 OnlyOffice；不执行正式服、备用服、`mark-tested`、`promote-prod` 或 `promote-backup`。

## Milestones

- [x] 创建任务记录，读取测试服发布、服务器、worktree、PowerShell、数据库、CI/CD 与经验门禁。
- [x] 构建新的 code-only 且 `onlyOfficeIncluded=false` release package，并完成本地/NAS manifest 与 artifact 校验。
- [x] 发布到测试服务器，只重启 backend/frontend，不包含 OnlyOffice 发布动作。
- [x] 验证测试服运行态、发布锁、镜像 tag、健康检查和无数据/无 OnlyOffice 包边界。
- [ ] 更新任务证据，提交并推送当前分支。

## Expected Verification

- 目标服务器：`172.30.30.58`。
- 发布范围：`test` only，组件范围 `intruoyi`。
- ReleaseTag：`release-20260728-codeonly-noonlyoffice-test-r2`。`r1` 已完成程序-only/no-OnlyOffice 发布，但前端发布信息文件缺失，不能作为最终验收版本。
- `manifest.json`：`publishScope=code-only`、`component=intruoyi`、`changeSet.includeOnlyOffice=false`。
- `release-manifest.json`：`onlyOfficeIncluded=false`。
- 发布包不包含 database dump、MinIO snapshot、runtime-data，镜像 tar 不包含 `onlyoffice/documentserver`。
- 发布命令不传 `-IncludeOnlyOffice`，部署日志中启动服务列表只包含 backend/frontend，并使用 `--no-deps`。
- 测试服 `.env IMAGE_TAG`、backend 镜像、frontend 镜像均为本轮 releaseTag。
- 后端 health 返回 `UP`，前端 HTTP 200，发布锁 `APPLIED`，无 `RUNNING` 发布锁。
- 前端 `/release-info.json` 返回本轮 releaseTag，而不是 SPA fallback 的 `index.html`。

## Current Status

ready_for_closeout

## Verification Summary

- Final releaseTag: `release-20260728-codeonly-noonlyoffice-test-r2`.
- Build source commit: `2763b45b88974debb680903de3812969a80968a4`; sourceRepos dirty flags are `false`.
- Local and NAS package verification: `publishScope=code-only`, `component=intruoyi`, `includeOnlyOffice=false`, `onlyOfficeIncluded=false`, no database dump, no MinIO snapshot, no runtime-data, image tar contains no `onlyoffice/documentserver`.
- Deploy verification: test server `.env`, backend image and frontend image all use r2; backend health is `UP`; frontend HTTP is `200`; `/release-info.json` returns r2 and `code-only`; release lock is `APPLIED` with no `RUNNING` lock.
- Scope verification: deploy log starts only `backend frontend` with `--no-deps`; OnlyOffice container image remains `onlyoffice/documentserver:latest` and `StartedAt=2026-07-27T16:04:31.094101012Z`, unchanged from pre-r2 verification.

## Closeout Blocker

- `task-closeout-cleanup --mode preview` is blocked because branch `codex/20260727-onlyoffice-test-release` cannot be fast-forward merged into local `int_main`; current branch is behind `int_main` by 71 commits and ahead by 15 commits.
- Cleanup apply, worktree merge/removal and final `completed` status are intentionally not performed until integration is explicitly handled.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；无数据、无 OnlyOffice 是本轮明确发布范围，不通过脚本降级或手工跳过错误。
- `是否从根因和长期维护角度解决`：是；以发布包 manifest、artifact 和远端运行态共同证明范围，而不是用口头说明替代。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 测试服发布门禁：只允许 `172.30.30.58`，不得执行正式服、备用服、`mark-tested`、`promote-prod` 或 `promote-backup`。
- Code-only 门禁：必须传 `-SkipDatabaseSync -SkipMinioSync`，且验证 manifest 无数据目录；`type=data` required SQL 及其依赖不得进入远端 MySQL APPLY 队列。
- No-OnlyOffice 门禁：构建和部署均不得传 `-IncludeOnlyOffice`；manifest 必须为 `onlyOfficeIncluded=false`，运行日志不得出现 OnlyOffice 服务启动或容器内可达性校验。
- Manifest 门禁：以 `manifest.json` 为 sourceRepos、publishScope、component 和 artifact 权威；legacy `release-manifest.json` 只做兼容校验。
- 发布日志脱敏门禁：保留或提交日志前必须扫描并脱敏 `mysql -p...`、NAS、SSH、token、私钥和连接串。
- 并发发布门禁：发布前后检查本地/远端发布进程、`infra_release_operation_lock`、远端 `.env IMAGE_TAG` 和实际镜像，避免与其他任务重叠。

## Cleanup Keep

- doc/tasks/20260728-codeonly-no-onlyoffice-test-release/ci-cd-evidence.md
