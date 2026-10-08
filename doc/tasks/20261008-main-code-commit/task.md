# 主干代码本地提交

## Goal

按用户“提交主干代码”的指令，将当前 int_main 的源码、测试和正式项目文档快照提交到本地主干。

## Milestones

- M1 completed：核对起始状态，冻结提交路径和 SHA-256。
- M2 completed：完成 Git 完整性、端口守卫及安全脚本定向验证，提交代码快照。
- M3 completed：收尾清理、任务记录提交与最终状态核验。

## Expected Verification

- 分支为 int_main，主工作区，无冲突或既有暂存内容。
- 冻结的路径、工作区 SHA-256、索引 blob 和 HEAD 一致。
- git diff --cached --check、UTF-8、冲突标记和端口守卫通过。
- restart-backend-safety.test.ps1 在替身环境通过，不操作实际服务或数据库。
- cleanup preview/apply 只处理本任务资产；提交后记录剩余改动。
- 本轮为 Git 快照任务，不运行构建、Java 业务回归或 E2E，不把 Git 核验当作业务验收。

## Current Status

completed

代码已本地提交，cleanup preview/apply 通过；三份收尾记录随最终本地提交保留。

## 设计约束检查

- 已读取 AGENTS.md、task-closeout-rules.md、worktree 与分支端口规则、PowerShell 编码规则及相关经验。
- 当前用户明确授权本地提交；按根 AGENTS.md 的当轮授权要求，不将下级文档的默认推送条款扩大为本轮推送授权。
- 当前分支为 int_main，起始 HEAD 为 476ec5322；索引为空。
- 精确纳入既有源码、配套测试及正式文档，不重写业务文件。
- 其他任务目录的四份变更不属于本任务；保留原处，不提交、不清理。
- 冻结后新出现的改动保留原处，不覆盖、不回滚。
- 使用 task-closeout-cleanup 和 project-experience-consolidation；已有主干快照经验覆盖本轮流程，无须新增经验文档。

## Cleanup Keep

- doc/tasks/20261008-main-code-commit/task.md
- doc/tasks/20261008-main-code-commit/execution-log.md
- doc/tasks/20261008-main-code-commit/verification-report.md
