# Execution Log

## 2026-08-05

- User intent: 重启本地前后端。
- Scope: `E:\IntRuoyi` 主工作区 `int_main`，前端 `8081`，后端 `48081`。
- Ownership: 仅本任务文档和本次重启产生的任务自有运行证据；不修改并行任务文件。
- Rule reads: `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/powershell-memory.md`, `docs/experience-index.md`.
- Git baseline: 当前 `int_main` 落后 `origin/int_main` 6 个提交，且存在多个并行任务的已修改、已暂存和未跟踪文件；本运行任务不提交、回滚或清理这些改动。
- BDD: Restart local frontend and backend -> Given the `int_main` workspace uses fixed ports `8081/48081`, When the standard local restart script replaces confirmed same-profile processes, Then backend health is `UP` and the frontend entry returns HTTP `200`.
