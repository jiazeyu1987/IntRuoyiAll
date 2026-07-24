# Verification Report: 创建 worktree 限制文件

## Result

PASS

## Evidence

- 已创建 `docs/worktree-restrictions.md`。
- `docs/worktree-restrictions.md` 包含 worktree 根目录 `D:\IntRuoyiWorktree\`。
- `docs/worktree-restrictions.md` 包含端口登记表 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`。
- `docs/worktree-restrictions.md` 固化 `int_main` 专属端口：前端 `8081`、后端 `48081`。
- `docs/worktree-restrictions.md` 固化非 `int_main` 端口槽位公式：前端 `8081 + slot`、后端 `48081 + slot`。
- `docs/worktree-restrictions.md` 明确端口冲突和未知占用必须 fail fast。
- `AGENTS.md` 已明确创建、启动、停止、重启、合并或清理任何 IntRuoyi worktree 前必须先读取 `docs\worktree-restrictions.md`。
- `python -X utf8` 校验 `AGENTS.md` 和 `docs/worktree-restrictions.md` UTF-8 读取和关键约束均通过。

## Notes

- 本任务未创建 worktree、未启动/停止端口、未操作服务器、未执行 E2E、未触碰数据库。
- 当前工作区存在大量其他任务的未提交/未跟踪文件；本任务未暂存、提交或修改这些无关文件。
- `task-closeout-cleanup` preview/apply 均通过，无删除项、无阻塞项。
- 实现提交已完成：`eb8f78bc 任务: 新增 worktree 限制文件`。
