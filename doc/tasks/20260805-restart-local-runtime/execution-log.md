# Execution Log

## 2026-08-05

- User intent: restart local frontend and backend.
- Bootstrap: read `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/branch-runtime-ports.md`, `docs/worktree-restrictions.md`, `docs/powershell-memory.md`, and `docs/powershell-encoding.md`.
- Experience gate: `docs/experience-index.md` exists; matching restart/runtime gates route to `docs/local-runtime.md`, already read before process changes.
- BDD: local runtime restart -> Given `int_main` local runtime should use frontend `8081` and backend `48081`, When confirmed old local runtime processes are restarted, Then both endpoints are reachable on the fixed ports with no unknown process killed and no port fallback.
- Port ownership: frontend `8081` is PID `17816`, `node.exe`, command line rooted at `E:\IntRuoyi\IntRuoyiFronted`.
- Port ownership: backend `48081` is PID `43376`, `java.exe`, command line includes `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260805-upload-approval-quick-action-hotpatch.jar` and `--yudao.runtime-control.repo-root=D:\IntRuoyiWorktree\20260804-upload-approval-quick-action\IntRuoyiBackend`.
- BLOCKER: `48081` is occupied by a `D:\IntRuoyiWorktree` runtime. `docs/worktree-restrictions.md` and `docs/local-runtime.md` require fail-fast; no process was stopped and no port was changed.
