# 提交前后端代码

## Task Goal

核对并安全提交当前 `E:\IntRuoyi` 统一仓库中的前后端代码，完成 `int_main` 与 `origin/int_main` 同步；如果前后端源码没有待提交差异，不制造空代码提交，并记录准确的同步结论。

## Milestones

- [x] M1：确认仓库所有权、当前分支、远端、工作区脏状态和提交边界。
- [x] M2：将开始前已存在的脏改动作为独立基线保全，避免与本任务记录混合。
- [x] M3：验证前后端源码差异、分支运行端口守卫、空白检查和远端同步。
- [x] M4：完成任务记录、cleanup preview/apply、收尾提交、推送和最终同步确认。

## Expected Verification

- `git status --short --branch --untracked-files=all`
- `git diff --name-status HEAD -- IntRuoyiBackend IntRuoyiFronted`
- `git diff --check`
- `git diff --cached --check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 待推送提交的大文件检查
- `git push origin int_main`
- `git rev-list --left-right --count origin/int_main...HEAD` 返回 `0 0`

## Applicable Experience Gates

- `docs\powershell-memory.md`：提交前检查分支、远端、脏状态和 staged 清单；既有脏改动独立基线；提交后复扫残余改动；推送后确认不再领先。
- `docs\branch-runtime-ports.md`：提交或推送前运行分支运行端口守卫。
- `docs\task-closeout-rules.md`：先标记 `ready_for_closeout`，再执行 cleanup preview/apply，最后标记 `completed`。
- `docs\experience-index.md`：复用提交、脏工作区基线、提交后残余复扫和 GitHub 推送门禁；不新建一次性长期经验文档。

## Current Status

completed

前后端源码相对 `HEAD` 和 `origin/int_main` 均无待提交差异，未制造空代码提交。既有任务记录已由独立基线提交保全，cleanup preview/apply、收尾提交和远端推送均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；无源码差异时直接记录同步结果，不制造空提交。
- `是否从根因和长期维护角度解决`：是；按统一仓库、明确暂存边界和远端同步门禁处理。
- `是否存在临时补丁或绕过`：否。

## Verification Evidence

- 基线提交：`842ead6abe6f4fb54a92c9ef1082dfd2db07384a`，仅包含开始前已存在的 7 份任务记录。
- `git fetch origin int_main`：PASS。
- `git diff --name-status HEAD -- IntRuoyiBackend IntRuoyiFronted`：无输出。
- `git diff --check` 和 `git diff --cached --check`：PASS。
- `scripts\preflight\branch-runtime-port-guard.ps1`：PASS，`int_main/int_main` 前端 `8081`、后端 `48081`。
- `origin/int_main...HEAD`：`0 2`，包括本任务基线提交 `842ead6ab` 和并发任务独立收尾提交 `66b0aff29`；二者均未包含待提交的前后端源码差异。
- `project-experience-consolidation`：现有 `docs\powershell-memory.md` 已覆盖本次脏工作区基线、残余复扫、推送和陈旧锁恢复门禁，无需新增长期经验文档。
- `task-closeout-cleanup` preview/apply：PASS，保留本任务 3 份记录，删除 0 项，blocked 0 项，warnings 0 项。
- `git push origin int_main`：PASS，`a373af073..12c014d5a`。
- 最终同步：`HEAD` 与 `origin/int_main` 均为 `12c014d5ad548b98a6c7c6f1c53e23f9b04258bf`，`origin/int_main...HEAD` 为 `0 0`。

## Cleanup Keep

- doc/tasks/20260807-submit-frontend-backend-code/task.md
- doc/tasks/20260807-submit-frontend-backend-code/execution-log.md
- doc/tasks/20260807-submit-frontend-backend-code/verification-report.md
