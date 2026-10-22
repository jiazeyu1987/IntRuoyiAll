# Execution Log

## 2026-09-18

- Task created before runtime checks and browser execution.
- User requested a complete upload-flow E2E.
- After the runtime dependency blocker was reported, the user replied `授权继续`; this authorizes continuing with local runtime dependency repair and real-page re-verification, without Git commit/push or remote server operations.
- Required evidence sources: repository rules, local runtime health, real Playwright page actions, browser network observations, screenshots, trace, and cleanup response.
- No production-code RED/GREEN applies because this task is verification-only and makes no code change.

## Runtime Preflight

- `http://127.0.0.1:48081/actuator/health` -> HTTP 200 / `UP`.
- `http://127.0.0.1:8081/` -> HTTP 200.
- `http://127.0.0.1:9000/minio/health/ready` -> HTTP 200.
- Existing `int_main` backend PID `30292` and frontend PID `31460` were verified; neither service was restarted or stopped.
- Task-local source file created at `doc/tasks/20260918-dcc-upload-full-e2e/upload-source.pdf`.

## Authorized Runtime Dependency Repair

- Runtime datasource preflight: PID `30292` command line points to `127.0.0.2:23306/ruoyi-vue-pro`; this is the database used for the schema repair.
- RED schema probe: `dcc_project_file_template_item` was absent; dependency table `dcc_file_type_taxonomy` and project code `129` were present.
- Targeted release migration gate for `20260513_dcc_base_schema -> 20260719_dcc_file_type_taxonomy -> 20260910_dcc_project_file_template` -> PASS; evidence: `migration-policy-gate-dcc-project-template.json`.
- Formal migration `IntRuoyiBackend/sql/mysql/20260910_dcc_project_file_template.sql` -> PASS on first execution and PASS on repeated execution. Post-migration schema probe -> PASS: table count `1`, generated `active_unique_flag`, active unique key and project/taxonomy indexes match the formal script; no business rows were inserted.
- The old runtime jar `backend-runtime-control-20260914-135918.jar` was inspected read-only and did not contain `previewProjectProduct` or `/project-product` in the embedded DCC controller. The current `IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar` was inspected read-only and contains `/project-product`, `upload-preview`, temporary-session cleanup and `/submit`; its embedded DCC jar is stored with compression type `0` and the Spring Boot executable manifest is present.
- The current target jar was already present before the runtime switch and was structurally validated; no new Maven build was started in this continuation because the shell lacks `JAVA_HOME`. No source code was changed by the runtime repair.

## Playwright Run

- Initial run `run-20260918-231416-chrome`: managed Playwright Chromium was unavailable, so the run was retried with the installed system Chrome executable. No business action was performed.
- Run `run-20260918-231416-chrome` on `http://127.0.0.1:8081`: real login form was filled with tenant `芋道源码` and user label `admin`, but the page timed out. Backend logs show tenant lookup took about 93 seconds, captcha about 60 seconds, and login about 29 seconds; the frontend request timeout is 30 seconds. Evidence: `artifacts/run-20260918-231416-chrome/result.json` and `failure.png`.
- Run `run-20260918-2328-localhost` on `http://localhost:8081`: real login succeeded with HTTP 200/business code 0; DCC upload page opened and project `按压式球囊扩充压力泵 · IDI · 1` was selected. The first script version incorrectly clicked a still-disabled stage control and was corrected.
- Run `run-20260918-2334-upload` on `http://localhost:8081`: corrected script login succeeded with HTTP 200/business code 0 and the same project was selected. The project template load failed in the real page with `项目文件模板加载失败：系统异常`; backend log records `Table 'ruoyi-vue-pro.dcc_project_file_template_item' doesn't exist` while servicing `/admin-api/dcc/project-codes/129/file-template`. The same page also displayed `请求地址不存在: /admin-api/dcc/controlled-files/project-product`.
- No `upload-preview` or `submit` request was triggered in any run. No file preview, formal controlled-file submission, or temporary upload ticket was created.
- No production source was changed. The only task-local code edit was the Playwright verification script’s wait/selector correction.

## 2026-09-19 Authorized Continuation And Runtime Recovery

- The formal migration was rechecked after the user authorization: `dcc_project_file_template_item` exists with the expected generated unique flag, active unique key, project/taxonomy indexes, and no business rows were inserted.
- The current target `yudao-server-exec.jar` was copied to the task runtime as `backend-runtime-control-20260919-dcc-upload.jar`; SHA-256 `6E90D2EE327E279E348DD5018031A72E7A8B25A2F80C1B7B8362D73C42387F84`. Read-only inspection confirmed the new DCC project-product route, upload-preview, temporary-session cleanup, and submit mappings.
- Starting that target runtime on the fixed `int_main` backend port failed during Spring context initialization because `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM` could not be resolved. The matching signing-secret setting is also required by `application-local.yaml`. Process, user, machine, repository-visible `.env`, and local runtime records were checked without finding an authorized source for either setting. No default, guessed, or generated value was used.
- The old runtime initially failed under the temporary wrapper because its DCC download-encryption settings were not propagated to the Java child process. The wrapper was corrected to require the existing user-scoped settings explicitly; the existing Base64 key passed a metadata-only decode check (decoded length 32) without exposing its value.
- Old runtime restore then succeeded: PID `24248`, fixed port `48081`, health HTTP 200 / `UP`, Jar `backend-runtime-control-20260914-135918.jar`, SHA-256 `7DA91DED88C93B34B8E6D38C8DBD5279C2BCC00EA942C9B1ABC4505A6496F918`. This restored runtime is not the latest target and does not contain the new `/project-product` route, so it is not evidence for a latest-code E2E pass.
- No `upload-preview` or `submit` request was performed after the schema repair. No task-owned business file, formal controlled-file record, or temporary upload ticket was created by this continuation.

## 2026-09-19 Temporary EDHR Runtime Start

- User explicitly authorized starting the latest backend with process-scoped temporary EDHR values and a randomly generated temporary signing secret. No EDHR value content was written to task files or output.
- Latest runtime start -> PASS: PID `38696`, fixed port `48081`, health HTTP 200 / `UP`, Jar `backend-runtime-control-20260919-dcc-upload.jar`, SHA-256 `6E90D2EE327E279E348DD5018031A72E7A8B25A2F80C1B7B8362D73C42387F84`.
- EDHR metadata-only evidence: issuer length `36`, signing secret length `44`; process command line contains neither temporary issuer nor temporary signing secret.
- The helper script used for the process-scoped temporary start was removed immediately after startup. This start only restores the latest runtime for local E2E continuation; it does not replace formal runtime configuration.

## Cleanup

- No business-data cleanup was required because the flow never reached file selection or upload preview.
- Task status is `in_progress`: the latest backend is now running with user-authorized process-scoped temporary EDHR values; upload E2E still needs to be rerun before any pass/fail conclusion changes.
- The temporary `start-verified-backend.ps1` helper was removed after runtime recovery. Generated Playwright traces, screenshots, and structured result files remain under the task evidence directory.
- Cleanup preview/apply was not run because the configured `task-closeout-cleanup` skill/script is not present under the current `C:\Users\D01020\.codex` skill tree; no other manual cleanup was performed, so the trace and screenshot evidence remains available.
