# Verification Report: 设置主分支为 int_main

## Result

PASS

## Evidence

- `git branch --show-current` 输出 `int_main`。
- `git status --short --branch` 显示当前分支为 `int_main`。
- `AGENTS.md` 已新增 `Main branch: int_main`。
- `AGENTS.md` 已规定本工作区主分支为 `int_main`，不得把 `main` 或 `master` 当作主分支。
- `python -X utf8` 校验 `AGENTS.md` UTF-8 读取和关键约束均通过。
- 实现提交已完成：`ac67dd89 任务: 设置主分支为 int_main`。

## Notes

- 本任务未创建 worktree、未操作服务器、未执行 E2E、未触碰数据库。
- 当前存在其他未跟踪任务/文档文件；本任务未暂存、提交或修改这些无关文件。
- `task-closeout-cleanup` preview/apply 均通过，无删除项、无阻塞项。
- 当前 `docs\` 下无合适 branch/worktree 长期经验文档；未获用户明确授权，未新建经验文档。
