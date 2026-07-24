# Task: Publish Current IntRuoyi Program To Test Server

## Goal

Publish the current local IntRuoyi backend and Vue3 frontend state to the test server by following the same overall release pattern used by `D:\ProjectPackage\RagflowAuth\运维工具.bat`: explicit precheck, local build, artifact transfer, remote deployment, and post-release verification.

## Scope

- Confirm the latest same-repository task is explicitly completed or blocked before starting this deployment task.
- Inspect the reference RagflowAuth release flow and identify the reusable local-to-test release pattern.
- Inspect the target test server and fail fast if the current IntRuoyi runtime, deployment directory, ports, or credentials are missing.
- Record BDD deployment scenarios and release evidence before changing any deployment assets.
- Prepare only the minimal deployment assets required for the current IntRuoyi backend and Vue3 frontend.
- Publish the current local program state to the test server only after the remote target is explicit.
- Verify the remote release result and record closeout evidence.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-workorder-row-freeze-toggle-action/task.md`
- Status before this task: completed.
- Impact: the latest same-repository backend task is already closed, so it does not block this deployment task.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this deployment task package.
- [x] M2: Inspect the reference release flow and the test-server runtime prerequisites.
- [x] M3: Prepare the minimal IntRuoyi release assets for the current local backend and frontend state.
- [x] M4: Publish to the test server and verify the remote runtime.
- [x] M5: Record final evidence and run closeout preview.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 build:test`
- Remote release health checks against the final backend and frontend URLs on the test server
- `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs/environments/ci-cd-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-publish-int-ruoyi-to-test-server --mode preview`

## Current Status

Completed on 2026-05-18. The current local backend and Vue3 frontend state has been published into a new isolated runtime on the test server, and remote verification plus evidence validation are complete.

## Final Verification Result

- PASS: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package`
- PASS: `pnpm exec vite build --mode test` with runtime overrides `VITE_BASE_URL=http://172.30.30.58:48081`, `VITE_BASE_PATH=/`, and `VITE_OUT_DIR=dist-intruoyi-test`
- PASS: `docker compose -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\int-ruoyi-test\docker-compose.yml --env-file <temp-env> config`
- PASS: remote `curl http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`
- PASS: remote `curl -I http://127.0.0.1:8081/` -> `HTTP/1.1 200 OK`
- PASS: local `Invoke-WebRequest http://172.30.30.58:48081/actuator/health`
- PASS: local `Invoke-WebRequest http://172.30.30.58:8081/` -> `200`
- PASS: local `Invoke-WebRequest -Method Head http://172.30.30.58:9000/yudao/dcc/original/20260513/dcc-sample.pdf` -> `200`
- PASS: remote MySQL verification `SELECT COUNT(*) FROM infra_file` and `SELECT COUNT(*) FROM infra_file WHERE url LIKE 'http://172.30.30.58:9000/%'` -> `48 / 48`
- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\docs\environments\ci-cd-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-publish-int-ruoyi-to-test-server --mode preview`

## Blocker And Impact

- Blocker: none active.
- Impact: none.
