# Verification Report

## Summary

- Result: PASS.
- Scope: `E:\IntRuoyi` `int_main` local frontend/backend runtime restart.
- Frontend: `http://127.0.0.1:8081/` returned HTTP `200`.
- Backend: `http://127.0.0.1:48081/actuator/health` returned `UP`.

## Evidence

- Standard restart command: `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
- Maven package: reactor `BUILD SUCCESS`, finished at `2026-08-08T21:47:37+08:00`.
- Startup dispatch: `Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`.
- Frontend listener: PID `40240`, `node.exe`, command path under `E:\IntRuoyi\IntRuoyiFronted`.
- Backend listener: PID `22900`, `java.exe`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260808-214737.jar`.
- Runtime Jar immutable check: PASS; Jar last write `2026-08-08 21:47:35` <= process start `2026-08-08 21:47:50`.

## Notes

- The first standard restart attempt failed fast because `int-ruoyi-mysql` was stopped.
- Explicitly started task-relevant local dependency containers: `int-ruoyi-mysql`, `int-ruoyi-redis`, `docker-minio-1`.
- No fallback, mock service, alternate port, or API-only replacement was used.
- Cleanup preview/apply completed with no deletions or blockers.
- Reusable lesson merged into `docs\local-runtime.md` and indexed in `docs\experience-index.md`.
