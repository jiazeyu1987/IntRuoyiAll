# Execution Log

## 2026-08-05

- User intent: restart local frontend and backend.
- Bootstrap: read `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/branch-runtime-ports.md`, `docs/worktree-restrictions.md`, `docs/powershell-memory.md`, and `docs/powershell-encoding.md`.
- Experience gate: `docs/experience-index.md` exists; matching restart/runtime gates route to `docs/local-runtime.md`, already read before process changes.
- BDD: local runtime restart -> Given `int_main` local runtime should use frontend `8081` and backend `48081`, When confirmed old local runtime processes are restarted, Then both endpoints are reachable on the fixed ports with no unknown process killed and no port fallback.
- Port ownership: frontend `8081` is PID `17816`, `node.exe`, command line rooted at `E:\IntRuoyi\IntRuoyiFronted`.
- Port ownership: backend `48081` is PID `43376`, `java.exe`, command line includes `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260805-upload-approval-quick-action-hotpatch.jar` and `--yudao.runtime-control.repo-root=D:\IntRuoyiWorktree\20260804-upload-approval-quick-action\IntRuoyiBackend`.
- BLOCKER: `48081` is occupied by a `D:\IntRuoyiWorktree` runtime. `docs/worktree-restrictions.md` and `docs/local-runtime.md` require fail-fast; no process was stopped and no port was changed.
- User authorization: user replied `授权`, approving stop of PID `43376` and restart back to `E:\IntRuoyi`.
- Stop verification: rechecked PID `43376` command line contained `D:\IntRuoyiWorktree\20260804-upload-approval-quick-action\IntRuoyiBackend`; rechecked PID `17816` command line contained `E:\IntRuoyi\IntRuoyiFronted`.
- STOP: stopped PID `43376` on backend port `48081` and PID `17816` on frontend port `8081`; post-stop port scan showed both ports free.
- GREEN: `E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> PASS, exit code `0`.
- Verification: backend `48081` final PID `56580`, command line rooted at `E:\IntRuoyi\IntRuoyiBackend` with runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260805-090357.jar`; sensitive datasource password parameters were present in process command line and intentionally not copied here.
- Verification: frontend `8081` final PID `39080`, command line rooted at `E:\IntRuoyi\IntRuoyiFronted` with `vite --mode env.local --strictPort`.
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS, `status=UP`.
- GREEN: `Invoke-WebRequest http://127.0.0.1:8081/` -> PASS, HTTP `200`, content length `3458`.
- Project experience consolidation: checked long-term memory routing. No new durable lesson was added because the encountered port/worktree conflict is already covered by `docs/local-runtime.md` and `docs/worktree-restrictions.md`.
- Cleanup preview: `task_closeout.py --task-id 20260805-restart-local-runtime --mode preview` -> PASS; kept `task.md`, `execution-log.md`, `verification-report.md`; delete `<none>`; blocked `<none>`.
- Cleanup apply: `task_closeout.py --task-id 20260805-restart-local-runtime --mode apply` -> PASS; deleted `<none>`.
- Final status: completed.
