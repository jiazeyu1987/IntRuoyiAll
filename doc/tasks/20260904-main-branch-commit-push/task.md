# 20260904 main branch commit push

## Task Goal

将当前 `int_main` 主干本地已提交但未推送的代码与任务记录推送到 `origin/int_main`，并按项目规则补齐本次提交/推送台账。

## Milestones

- [x] 读取项目 Git、任务收尾与命令记录规则
- [x] 确认当前分支与本地/远端差异
- [x] 验证并提交提交前出现的当前脏改基线
- [x] 推送本地领先提交到 `origin/int_main`
- [x] 运行收尾清理 preview/apply
- [x] 标记任务完成并提交/推送本次收尾记录

## Expected Verification

- `git status --short --branch` 在最终推送后显示 `int_main` 不再 ahead。
- `git status --porcelain=v1` 最终为空。
- DCC Mapper 定向 Maven 测试通过。
- DCC source governance SQL 依赖闭包 release migration policy gate 通过。
- Stage1 静态合同与前端 `pnpm ts:check` 通过。
- `task-closeout-cleanup` preview/apply 均通过，且只处理本任务目录。

## Current Status

completed - 当前快照提交 `ed5201d258d6f4b23589e21ef7e42b8b9567311e` 已推送到 `origin/int_main`；收尾清理 preview/apply 已通过，最终任务记录待作为收尾提交推送。

## Design Constraints Check

- 不引入 fallback、降级、模拟成功或兼容补丁。
- 不运行 E2E；用户本轮只授权 Git 提交/推送主干代码。
- 不停止/重启 `int_main` 服务，不操作远程服务器或数据库。
- 仅提交/推送当前分支与本任务台账相关内容。
- 当前脏改包含迁移 SQL、后端 Mapper/测试、前端 Stage1 行为与长期前端规则沉淀；已按对应规则补读并做定向验证。

## Cleanup Candidates

- doc/tasks/20260904-main-branch-commit-push/bug-regression-evidence.md

## Cleanup Keep

- doc/tasks/20260904-main-branch-commit-push/task.md
- doc/tasks/20260904-main-branch-commit-push/execution-log.md
- doc/tasks/20260904-main-branch-commit-push/verification-report.md
