# Verification Report

## Runtime

- Branch profile: `int_shedule`.
- Backend URL: `http://127.0.0.1:48021/actuator/health`.
- Frontend URL: `http://127.0.0.1:8021/`.
- Docker MySQL: `127.0.0.1:23306/ruoyi-vue-pro`.
- Docker Redis: `127.0.0.1:26379`.

## Evidence

- Backend package: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS.
- Backend health: HTTP `200`.
- Frontend entry: HTTP `200`.
- Backend listener: port `48021`, process `46016`.
- Frontend listener: port `8021`, process `44120`.

## Result

Runtime verification passed. Closeout remains pending because services are intentionally left running for user use.
## 2026-07-25 Rerun Evidence

- Backend health: `GET http://127.0.0.1:48021/actuator/health` -> HTTP `200`.
- Frontend entry: `GET http://127.0.0.1:8021/` -> HTTP `200`.
- Backend listener: port `48021`, process `31412`, command `java -jar ...yudao-server-exec.jar --server.port=48021 --spring.profiles.active=local`.
- Frontend listener: port `8021`, process `39436`, command `vite --mode branch-shedule --host 127.0.0.1 --port 8021 --strictPort`.
- Restart note: first Vite process `30612` listened on `8021` but returned no HTTP bytes within 90 seconds; it was task-owned and stopped before restart.
- Runtime logs: `backend-runtime-20260725-080817.stdout.log`, `backend-runtime-20260725-080817.stderr.log`, `frontend-runtime-20260725-082126.stdout.log`, `frontend-runtime-20260725-082126.stderr.log`.

## Commit Verification

- Static contract: `node tests\e2e\dcc-controlled-file-logs-static.spec.js` -> PASS.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-run-shedule-local-runtime --mode preview` -> PASS, no blocked or warnings.
- Cleanup apply: BLOCKED by active backend log handle on `backend-runtime-20260725-080817.stderr.log`; no production code or verification failure.
