# Execution Log

## 2026-07-28

- Created task record before starting local runtime services.
- BDD: local int_main runtime startup -> Given the `E:\IntRuoyi` baseline workspace and fixed local ports, When starting the backend and frontend, Then backend health must be `UP` on `48081` and frontend must return HTTP `200` on `8081`.
- Rule evidence: read `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/powershell-memory.md`, and `docs/powershell-encoding.md`.
- Experience gate evidence: read `docs/experience-index.md`; matching gates are routed to `docs/local-runtime.md`, `docs/worktree-restrictions.md`, and `docs/powershell-memory.md`.
- Git state evidence: `git status --short --branch` showed many pre-existing modified and untracked files from unrelated task directories and source/test files. Current startup task will not stage, commit, revert, or clean those artifacts.
