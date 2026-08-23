# Execution Log

## User Intent

- 用户要求分类 `int_main` 工作树的大量 dirty/untracked 文件，判断哪些需要提交、哪些应忽略，并在不损坏其它任务成果的前提下让主线尽可能干净。

## Baseline

- 当前分支：`int_main`。
- 当前 HEAD：`bee7811cb`。
- 初始状态：49 项 tracked modified、2 项 deleted、12523 项 untracked，共 12574 项状态记录。

## Constraints

- 不删除、reset、checkout、stash 或整体提交无法确认归属的并行改动。
- 任务文档、运行产物、迁移包、用户资料和代码必须分开判断。
- 生成物只有在确认可再生且不属于正式交付物时才进入 ignore。

## Milestone 1

状态：completed。已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/worktree-memory.md`、`.gitignore`，并冻结上述 baseline。

## Milestone 2

状态：in_progress。正在核对 `doc/tasks` 大型目录、tracked 文档修改、资源/迁移包、临时运行目录和未知根目录文件。
