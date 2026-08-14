# Verification Report

## Result

PASS

## Evidence

- Initial restart attempt failed fast because required local dependency container `int-ruoyi-mysql` was stopped.
- Confirmed and started existing IntRuoyi local dependency containers: `int-ruoyi-mysql`, `int-ruoyi-redis`, and `docker-minio-1`.
- Dependency readiness: MySQL route `127.0.0.2:23306=True`, Redis route `127.0.0.2:26379=True`, MinIO route `127.0.0.2:9000=True`, MinIO ready HTTP `200`.
- Restart command passed: `restart-int-ruoyi-local.ps1 -Component full`.
- Backend listener: PID `6424`, port `48081`, repo root `E:\IntRuoyi\IntRuoyiBackend`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260805-135250.jar`.
- Runtime Jar immutability check: Jar modified `2026-08-05 13:52:48`, backend process started `2026-08-05 13:52:58`.
- Frontend listener: PID `10888`, port `8081`, frontend root `E:\IntRuoyi\IntRuoyiFronted`, Vite mode `env.local`.
- Backend health: `http://127.0.0.1:48081/actuator/health` returned `status=UP`.
- Frontend entry: `http://127.0.0.1:8081/` returned HTTP `200` with content length `3458`.
- Cleanup: preview/apply passed with no delete candidates and no blocked paths.

## Rerun 2026-08-05 17:10

- Initial full restart command dispatched, but backend exited because local Docker MySQL dependency at `127.0.0.2:23306` was not yet reachable.
- Docker Desktop and existing local dependency containers recovered: `int-ruoyi-mysql`, `int-ruoyi-redis`, and `docker-minio-1`; MinIO became healthy.
- Backend restart command passed on rerun: `restart-int-ruoyi-local.ps1 -Component backend`.
- Backend listener: PID `45576`, port `48081`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260805-172627.jar`.
- Runtime Jar immutability check: Jar modified `2026-08-05 17:26:21`, backend process started `2026-08-05 17:26:36`.
- Frontend listener: PID `43956`, port `8081`, frontend root `E:\IntRuoyi\IntRuoyiFronted`, Vite mode `env.local`.
- Backend health: `http://127.0.0.1:48081/actuator/health` returned `status=UP`.
- Frontend entry: `http://127.0.0.1:8081/` returned HTTP `200` with content length `3474`.
- Cleanup rerun: preview/apply passed with no delete candidates and no blocked paths.

## Notes

- One frontend probe with a 30-second timeout elapsed while Vite was still handling first-load work; repeated verification with a 120-second timeout returned HTTP `200`.
- Backend process command line includes local datasource password parameters; they are not copied into task evidence beyond this redacted note.
- Git closeout is not performed in this runtime-only task because the shared branch already has unrelated ahead commits and many parallel dirty changes.
- Rerun encountered a transient `docker inspect` crash while Docker Desktop was warming up; existing project experience already covers Docker inspect crash and dependency-readiness handling, so no new long-term experience document was created.
