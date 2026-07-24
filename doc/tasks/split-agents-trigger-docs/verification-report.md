# Verification Report: 拆分 AGENTS 触发式专项规则

## Result

PASS

## Evidence

- 已新增 `docs/local-runtime.md`。
- 已新增 `docs/e2e-rules.md`。
- 已新增 `docs/database-rules.md`。
- 已新增 `docs/powershell-encoding.md`。
- 已新增 `docs/task-closeout-rules.md`。
- 已新增 `docs/release-backup-restore.md`。
- `AGENTS.md` 已新增 `Trigger-Read Rule Files` 索引。
- `AGENTS.md` 已引用 worktree、本机运行、服务器、登录、E2E、数据库、PowerShell、任务收尾、发布备份恢复规则文件。
- `python -X utf8` 校验所有触发式规则文件和 `AGENTS.md` 均可 UTF-8 读取。

## Notes

- 本任务未修改现有未跟踪的 `docs/server-access.md` 和 `docs/login-access.md` 内容。
- 本任务未启动服务、未创建 worktree、未操作服务器、未执行 E2E、未触碰数据库。
- 当前工作区存在大量其他任务的未提交/未跟踪文件；本任务不暂存、提交或修改这些无关文件。
- `task-closeout-cleanup` preview/apply 均通过，无删除项、无阻塞项。
- 实现提交已完成：`cec604a5 任务: 拆分触发式规则文档`。
