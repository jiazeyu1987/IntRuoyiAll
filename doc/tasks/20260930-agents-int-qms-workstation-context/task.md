# 本机文控分支约定

## Task Goal

将用户明确的两台电脑分工和本机 int_qms 默认提交目标写入根 AGENTS.md，使本机其他 Codex 线程读取后能正确理解当前文控工作区。

## Milestones

- [x] 读取现有 AGENTS.md、任务收尾和 PowerShell UTF-8 规则。
- [x] 新增本机分工、提交目标和跨分支操作边界。
- [x] 完成文档结构、UTF-8、差异和端口合同验证。
- [x] ready_for_closeout 后完成当前任务 cleanup。

## Expected Verification

- AGENTS.md 明确本机在 int_qms 开发、修复、验证和提交文控模块；另一台电脑在 int_main 开发主程序。
- 本机“提交代码”“提交主干代码”默认指 int_qms；不自动切换、合并或推送 int_main。
- 每次以 git branch --show-current 核对分支，不从目录名推断分支；不扩大提交推送授权。
- AGENTS.md 的既有通用规则、端口矩阵标记和 E2E 环境说明保留。
- UTF-8、文档结构、git diff --check 和 branch-runtime-port-guard.ps1 通过。
- cleanup 仅处理本任务三份记录。

## Current Status

blocked — 文档更新、验证和 cleanup 均完成；按 task-closeout-rules.md，提交推送前不能标记 completed。Git 闭环由既有提交线程处理，本任务未并发操作索引。

## 设计约束检查

- 用户要求修改根 AGENTS.md；只修改该文件和本任务记录。
- 仓库集成主分支仍为 int_main，本机文控开发与验证分支为 int_qms；不能将仓库约定等同于本机当前任务目标。
- 当前为文档变更，做结构验证，不修改生产行为，不执行业务 TDD 或 E2E。
- 不启动子 Agent，不操作数据库、远程服务器或服务，不清理其他任务资产。
- 既有提交任务已有独立线程执行；本任务不争用其 Git 索引或更改其任务记录。

## Cleanup Keep

- doc/tasks/20260930-agents-int-qms-workstation-context/task.md
- doc/tasks/20260930-agents-int-qms-workstation-context/execution-log.md
- doc/tasks/20260930-agents-int-qms-workstation-context/verification-report.md
