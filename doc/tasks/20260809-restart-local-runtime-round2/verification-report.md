# Verification Report

## Summary

- Result: PASS.
- Scope: `E:\IntRuoyi` `int_main` local frontend/backend runtime restart.
- Frontend: `http://127.0.0.1:8081/` returned HTTP `200`.
- Backend: `http://127.0.0.1:48081/actuator/health` returned `UP`.

## Evidence

- Standard restart command: `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
- Maven package: reactor 30/30 `BUILD SUCCESS`, finished at `2026-08-09T14:04:30+08:00`.
- Startup dispatch: `Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`.
- Frontend listener: PID `51912`, `node.exe`, command path under `E:\IntRuoyi\IntRuoyiFronted`; Vite ready in `209697 ms`.
- Backend listener: PID `52880`, `java.exe`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-140430.jar`.
- Runtime Jar immutable check: PASS; Jar last write `2026-08-09T14:04:30.1327415+08:00` <= process start `2026-08-09T14:04:47.8788087+08:00`.
- Final frontend probe: HTTP `200` in `8602 ms`.
- Final backend probe: health `UP` in `191 ms`.

## Notes

- Local MySQL, Redis, and MinIO dependency containers were already running; no container restart was required.
- No fallback, mock service, alternate port, configuration rewrite, or API-only replacement was used.
- Cleanup preview/apply passed with no deletions, blockers, or warnings; core task records were preserved.
