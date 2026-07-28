# Verification Report

## Result

PASS

## Evidence

- Initial restart failed fast because `int-ruoyi-mysql` was stopped and Docker Desktop could not bind `E:\IntRuoyi\IntRuoyiBackend\sql\mysql\ruoyi-vue-pro.sql`.
- Windows confirmed the SQL file exists; Docker/WSL initially saw E drive as missing or empty.
- E drive was mounted into Ubuntu WSL and Docker Desktop runtime namespace; Docker bind verification changed from `BIND_MISSING` to `BIND_OK`.
- `int-ruoyi-mysql` started successfully.
- Standard full restart script succeeded: `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
- Backend health: `http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`.
- Frontend entry: `http://127.0.0.1:8081/` returned HTTP `200`.
- Backend listener: PID `42652`, stable runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-084231.jar`.
- Frontend listener: PID `43232`, Vite on fixed port `8081`.
- Runner token gate: token file is ignored by `.gitignore`, script regression tests passed, register/heartbeat probes returned business code `0`, DB readback showed `ONLINE`, `current_running_count=0`, heartbeat age `1s`.
- Experience gate: reusable Docker Desktop / WSL E drive bind-mount lesson merged into `docs/local-runtime.md` and indexed in `docs/experience-index.md`.
- Document verification: UTF-8 read check passed; `git diff --check` passed with only CRLF normalization warnings.

## Follow-Up

- The E drive mount repair is runtime-state based. If Docker Desktop or WSL is fully restarted and E drive disappears again, remount E into WSL/Docker Desktop or configure Docker Desktop/WSL to persistently expose the E drive before starting `int-ruoyi-mysql`.
