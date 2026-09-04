# Verification Report

## Summary

- Standard restart command: `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full`
- Result: PASS
- Backend package result: `BUILD SUCCESS`, finished `2026-09-04T08:17:21+08:00`.
- Compile errors encountered: none.

## Runtime Evidence

- Backend: `http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`.
- Frontend: `http://127.0.0.1:8081/` returned HTTP `200`; final `curl.exe` probe returned `200` in `26.112092s`.
- Backend listener: PID `41292`, process `java`, start `2026-09-04T08:17:27`.
- Backend command ownership: runs `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260904-081721.jar` with `--server.port=48081`, `--spring.profiles.active=local`, repo root `E:\IntRuoyi\IntRuoyiBackend`, logs under `E:\IntRuoyi\output\runtime\int_main\logs`. Database and other secret-bearing arguments were observed but are intentionally not recorded.
- Frontend listener: PID `59308`, process `node`, start `2026-09-04T08:17:30`.
- Frontend command ownership: runs `E:\IntRuoyi\IntRuoyiFronted\node_modules\.bin\..\vite\bin\vite.js --mode env.local --strictPort`.

## Notes

- Some frontend HTTP probes were slow while Vite warmed/transformed modules; subsequent probes returned HTTP `200`.
- The workspace had many pre-existing dirty and untracked files before this restart task. No source compile fix was required by this task.
- Cleanup preview/apply completed with no delete or blocked paths.
- Git commit/push was not performed because the current request did not explicitly authorize Git commit or remote operations.
