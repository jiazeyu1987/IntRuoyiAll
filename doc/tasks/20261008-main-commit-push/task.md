# 主干提交与推送

## Goal

按用户“提交推送主干代码”的授权，将当前 int_main 上可归属的源码、测试及正式项目文档快照提交并推送到 origin/int_main。

## Milestones

- M1 completed：核对分支、远端、起始改动与提交清单，冻结路径和 SHA-256。
- M2 completed：端口守卫、差异完整性与暂存内容核验，提交代码快照。
- M3 completed：推送、收尾清理与最终远端一致性核验。

## Expected Verification

- 当前分支为 int_main，origin 可用，无未解决冲突。
- branch-runtime-port-guard.ps1 通过。
- 路径/SHA-256 快照、HEAD 与暂存内容一致；git diff --cached --check 通过。
- 清理 preview/apply 仅涉及本任务目录。
- 推送后本地 HEAD 与远端 int_main 一致，记录剩余未提交文件。
- 本任务为 Git 快照提交，不把 Git 检查等同于构建或业务回归；未获当轮 E2E 授权，不运行 E2E。

## Current Status

completed

代码快照已推送且 cleanup preview/apply 通过；收尾记录随本任务最终提交推送。

## 设计约束检查

- 已读取 AGENTS.md、docs/task-closeout-rules.md、端口矩阵、worktree 与 PowerShell 相关规则。
- 本轮明确授权提交与推送；不改业务逻辑、数据库、服务或其他任务工作区。
- 精确暂存可归属路径；不使用整体 git add -A，不收录敏感数据、临时输出或未知业务输入。
- 冻结后新出现的并行修改保留原处，不覆盖或回滚。
- 收尾先置 ready_for_closeout，执行技能 preview/apply 后记录最终结果。

## Cleanup Keep

- doc/tasks/20261008-main-commit-push/task.md
- doc/tasks/20261008-main-commit-push/execution-log.md
- doc/tasks/20261008-main-commit-push/verification-report.md
