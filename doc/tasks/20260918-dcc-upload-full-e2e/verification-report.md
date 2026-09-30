# DCC Upload Full E2E Verification Report

## Result

`BLOCKED`

The real Playwright flow completed login, opened the DCC upload page, and selected a project, but it could not reach stage/template selection, PDF upload preview, or submission on the latest backend runtime.

## Scope And Identity

- Frontend: `http://localhost:8081`
- Backend: `http://127.0.0.1:48081`
- Tenant: `芋道源码`
- User label: `admin`
- Target page: `/dcc/controlled-file/upload`
- Task source file: `doc/tasks/20260918-dcc-upload-full-e2e/upload-source.pdf`
- Acceptance actions were performed through a real Playwright browser page. API/DB access was not used to perform login, selection, upload, preview, or submission.
- The user-authorized local repair included applying the formal DCC template schema migration; this changed schema only and inserted no business rows.

## Evidence Summary

### Initial Real-Page Runs

1. `artifacts/run-20260918-231416-chrome/result.json`
   - Real login form was filled, but the `127.0.0.1` login attempt timed out because tenant/captcha/login processing exceeded the frontend request timeout.
2. `artifacts/run-20260918-2328-localhost/result.json`
   - Real login succeeded with HTTP 200 and business code 0.
   - The upload page opened and project `按压式球囊扩充压力泵 · IDI · 1` was selected.
3. `artifacts/run-20260918-2334-upload/result.json`
   - Corrected real-page flow logged in successfully and selected the same project.
   - The page showed `项目文件模板加载失败：系统异常`.
   - The page also showed `请求地址不存在: /admin-api/dcc/controlled-files/project-product`.
   - No `upload-preview` or `submit` request was triggered.

Every run has a Playwright `trace.zip`; the completed page stages have screenshots and the final blocked run has `failure.png`.

### Authorized Schema Repair

- The initial read-only probe found `dcc_project_file_template_item` missing while `dcc_file_type_taxonomy` and project code `129` existed.
- The targeted migration policy gate for `20260513_dcc_base_schema -> 20260719_dcc_file_type_taxonomy -> 20260910_dcc_project_file_template` passed.
- `IntRuoyiBackend/sql/mysql/20260910_dcc_project_file_template.sql` executed successfully and remained idempotent on repeat execution.
- Post-migration read-only verification found the table, generated `active_unique_flag`, active unique key, and expected project/taxonomy indexes. No business rows were inserted.

### Runtime Boundary

- The current target runtime Jar contains the new DCC project-product route, upload-preview, temporary-session cleanup, and submit mappings. Task copy SHA-256: `6E90D2EE327E279E348DD5018031A72E7A8B25A2F80C1B7B8362D73C42387F84`.
- Starting that target Jar on `48081` failed before application startup because `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM` was missing; the corresponding signing-secret setting is also required. No authorized source for either value exists in process/user/machine environment or repository-visible local configuration, so no value was guessed or generated.
- The prior runtime was restored only to return the local environment to a healthy state: PID `24248`, health HTTP 200 / `UP`, Jar `backend-runtime-control-20260914-135918.jar`, SHA-256 `7DA91DED88C93B34B8E6D38C8DBD5279C2BCC00EA942C9B1ABC4505A6496F918`.
- The restored old Jar does not contain `/project-product`, so it cannot be used to claim the latest upload implementation passed.

## Acceptance Matrix

| Step | Result | Evidence |
| --- | --- | --- |
| Real login | PASS on prior real-page run | `run-20260918-2328-localhost/result.json` and `run-20260918-2334-upload/result.json`, HTTP 200/business code 0 |
| Open DCC upload page | PASS | `02-upload-page.png` |
| Select DCC project | PASS | `projectOption` and `03-project-selected.png` |
| Apply DCC template schema | PASS | formal migration and `migration-policy-gate-dcc-project-template.json` |
| Start latest backend runtime | BLOCKED | missing formal MES independent-receipt runtime configuration |
| Select stage | NOT REACHED after repair | latest runtime unavailable; old runtime lacks the new route |
| Select file type/name | NOT REACHED | depends on latest runtime/template path |
| Fill metadata | NOT REACHED | depends on template selection |
| Upload task-owned PDF | NOT REACHED | no `upload-preview` request |
| Preview PDF in page | NOT REACHED | no preview session created |
| Submit controlled file | NOT REACHED | no `submit` request |
| Temporary upload cleanup | NOT REQUIRED | no upload preview/ticket was created |

## Conclusion

This is an environment/runtime configuration blocker, not an E2E pass and not a confirmed product-flow failure. The database schema prerequisite is now present, but the latest backend cannot be started without the project’s formal MES independent-receipt settings. The local backend was restored to the previous healthy Jar, and no business upload data was created. The task remains `blocked`.
