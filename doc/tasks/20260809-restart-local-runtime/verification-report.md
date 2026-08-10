# Verification Report

## Summary

- Result: PASS.
- Scope: `E:\IntRuoyi` `int_main` local frontend/backend runtime restart.
- Frontend: `http://127.0.0.1:8081/` returned HTTP `200`.
- Backend: `http://127.0.0.1:48081/actuator/health` returned `UP`.

## Evidence

- Standard restart command: `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
- Maven package: reactor `BUILD SUCCESS`, finished at `2026-08-09T00:34:55+08:00`.
- Startup dispatch: `Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`.
- Frontend listener: PID `25476`, `node.exe`, command path under `E:\IntRuoyi\IntRuoyiFronted`.
- Backend listener: PID `26280`, `java.exe`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-003455.jar`.
- Runtime Jar immutable check: PASS; Jar last write `2026-08-09 00:34:54` <= process start `2026-08-09 00:35:03`.

## Notes

- Vite reported ready after `241015 ms`; the first cold HTTP probe timed out before initialization completed, and the subsequent verification returned HTTP `200` in `4262 ms`.
- Backend health verification returned `UP` in `144 ms`.
- No fallback, mock service, alternate port, or API-only replacement was used.
- Cleanup preview/apply completed with no deletions, blockers, or warnings.
