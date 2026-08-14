# 20260802 DCC 受控打印 E2E Execution Log

## User Intent

- 用户要求在 `E:\IntRuoyi` 对 DCC 文控“受控打印”做真实 Playwright E2E 验证。
- 用户补充限定：只验证本场景；如发现缺页面入口、权限、测试数据或运行态问题，先记录 BLOCKED 和影响；不得用 API-only、SQL 改状态或 admin 账号绕过。

## Preflight Rules Read

- `E:\IntRuoyi\AGENTS.md`
- `E:\IntRuoyi\docs\e2e-rules.md`
- `E:\IntRuoyi\docs\login-access.md`
- `E:\IntRuoyi\docs\frontend-development.md`
- `E:\IntRuoyi\docs\task-closeout-rules.md`
- `E:\IntRuoyi\docs\powershell-encoding.md`
- `E:\IntRuoyi\docs\local-runtime.md`
- `E:\IntRuoyi\docs\database-rules.md`
- `E:\IntRuoyi\docs\worktree-restrictions.md`
- `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`

## BDD

- BDD: 有权限用户受控打印当前有效文件 -> Given 租户 1 存在有打印权限的非 admin 用户和任务自有 `ACTIVE` 受控文件，When 用户从受控浏览或详情页点击受控打印入口并填写用途、份数、接收部门/使用位置等必填信息，Then 系统按设计完成审批或直接打印，并展示打印编号、文件编号、版本、打印人、打印时间、水印等受控信息。
- BDD: 打印动作可追溯 -> Given 有权限用户完成一次真实受控打印，When 打开打印记录、分发记录或操作日志，Then 可以看到本次打印记录，且只读 API/DB 核验打印记录 ID、文件版本、打印人、份数、审批状态与页面证据一致。
- BDD: 无权限用户被阻断 -> Given 同一 `ACTIVE` 受控文件和无打印权限的非 admin 用户，When 用户进入同一受控浏览或详情页，Then 打印按钮不可见/不可用，或点击后出现明确权限拒绝，且不会生成打印记录。
- BDD: 缺入口时 fail fast -> Given 当前系统没有完整受控打印页面入口、表单、审批或记录链路，When Playwright 从真实页面验证目标路径，Then 记录 `E2E BLOCKED` 及缺失项，不使用 API-only、SQL 改状态或 admin 账号冒充通过。

## RED / GREEN Log

- RED: source preflight for DCC controlled print -> FAIL, initial code search found DCC detail page `流程打印` and `approval-print` template APIs, but no confirmed controlled print application form with purpose/copies/department/location fields or print record entity yet. Impact: must use real page verification to determine whether target scenario is BLOCKED.
- RED: login credential preflight -> FAIL, `DCC_E2E_PASSWORD` environment variable is missing. Expected reason: project rules require non admin login with password injected through environment variable, and the user explicitly forbids admin/default/API-only/SQL-state bypass.
- GREEN: credential injection preflight -> PASS, user supplied the password source and this task used only the PowerShell expression `$env:DCC_E2E_PASSWORD = -join (1..6 | ForEach-Object { [char]49 })`; no password literal was written to task docs.
- GREEN: node --check doc\tasks\20260802-dcc-controlled-print-e2e\controlled-print-real-e2e.mjs -> PASS.
- RED: node doc\tasks\20260802-dcc-controlled-print-e2e\controlled-print-real-e2e.mjs -> FAIL/E2E_BLOCKED, expected reason: real page path lacks DCC controlled print entry/form/record chain; must not use API-only or SQL to create a print record.

## Command / Evidence Log

- Preflight: `git -C E:\IntRuoyi status --short --branch` showed branch `int_main...origin/int_main` with many existing tracked/untracked changes outside this task. This task will not commit or alter unrelated work.
- Preflight: `where.exe npx` returned `D:\Programs\npx` and `D:\Programs\npx.cmd`, satisfying the Playwright CLI prerequisite.
- Experience gate: `docs\experience-index.md` contains matching DCC, Playwright, E2E, login, runtime and artifact cleanup gates; applicable gate summary copied into `task.md`.
- Runtime preflight: backend health `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Runtime preflight: frontend `http://127.0.0.1:8081/` returned HTTP `200`.
- Browser preflight: local Chrome executable exists at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Database container preflight: Docker container `int-ruoyi-mysql` is running. At initial preflight, no SQL state change was performed; later account permission preparation was limited to the user-authorized non-admin browser/preview grants and did not create print records or change file status.
- Source preflight: DCC browser page exposes row actions `预览` and `下载`; no browser-row `受控打印` button was found in `IntRuoyiFronted\src\views\dcc\controlled-file\browser\index.vue`.
- Source preflight: DCC detail page exposes `预览受控文件`, `下载受控文件`, and More menu item `流程打印`; no detail-page form for print purpose, copies, receiving department, or use location was found in `IntRuoyiFronted\src\views\dcc\controlled-file\detail\index.vue`.
- Source preflight: backend DCC controller exposes `/admin-api/dcc/controlled-files/{id}/approval-print/print-html` and `/export-word` for approval-process printing, plus preview/download endpoints; no DCC controlled-print request/approval/record endpoint was confirmed in `DccControlledFileController`.
- Schema preflight: DCC schema contains `dcc_controlled_file_access_log`, `dcc_controlled_file_access_event`, `dcc_controlled_file_watermark_trace`, `dcc_controlled_file_download_record`, `dcc_approval_print_template`, and distribution tables; no dedicated DCC controlled-print application/print-record table with copies/department/location fields was confirmed.
- Permission setup: user authorized choosing any non-admin account and granting needed permissions. Kept `wangsiyu` as the positive non-admin account and `zhangkeying` as the negative no-print account. Added only browse/preview capability for `zhangkeying` on the task-owned file directory/category and did not add download or print-specific permission.
- Permission setup verification: `zhangkeying` has role `910232` and task object rules `dcc_directory_access_rule(can_query=1, can_preview=1, can_download=0)` plus `dcc_file_category_permission_rule(action_type=VIEW)`. `DOWNLOAD` category rule count for `zhangkeying` remains `0`.
- Cache hygiene: deleted Redis keys `user_role_ids:152` and `user_role_ids:910250` after permission preparation.
- Runtime preflight on 2026-08-02 18:49 CST: frontend `http://127.0.0.1:8081/` returned `200`, backend `http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`, MinIO ready endpoint `http://127.0.0.1:9000/minio/health/ready` returned `200`.
- Playwright command: ran `node E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-print-e2e\controlled-print-real-e2e.mjs` with `DCC_E2E_PASSWORD` injected by expression, `DCC_E2E_FRONTEND_URL=http://127.0.0.1:8081`, `DCC_E2E_BACKEND_URL=http://127.0.0.1:48081`, and local Chrome executable path.
- Playwright result: `doc\tasks\20260802-dcc-controlled-print-e2e\controlled-print-real-e2e-result.json` status `E2E_BLOCKED`.
- Positive browser evidence: `wangsiyu` login succeeded; `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-ORIG-20260802101521&pageNo=1&pageSize=20` returned `browser-page` code `0`, total `1`; row text includes `CODX-DCC-ORIG-20260802101521`; row actions were `预览` and `下载`; `hasPrintLikeRowAction=false`.
- Positive detail evidence: Playwright clicked the visible row file-number button `data-testid="dcc-browser-file-number-detail-link"` and landed at `/dcc/controlled-file/detail/2054545668044070287?traceability=1&from=browser...`; detail API returned HTTP `200`, code `0`, status `ACTIVE`, version `V1.0`, master current version `V1.0`, published/stamped file ID `9198354916366`, but the real page body did not show the file number and exposed only `返回`; no `受控打印` / `打印申请` / `流程打印` action appeared.
- Required form evidence: the real page did not show `打印用途`、`份数`、`接收部门`、`使用位置`.
- Negative browser evidence: `zhangkeying` login succeeded; the same browser path returned code `0`, total `1`; row was visible with actions `预览` only; no print-like entry appeared.
- Read-only DB evidence: `dcc_controlled_file.id=2054545668044070287`, file number `CODX-DCC-ORIG-20260802101521`, status `ACTIVE`, version `V1.0`, master current active ID `2054545668044070287`, tenant `1`, published/stamped file ID `9198354916366`.
- Read-only DB schema evidence: `information_schema.tables LIKE 'dcc%print%'` returned only `dcc_approval_print_template`; no DCC controlled-print request/application/record table with copies, receiving department, use location, print person, print status or approval state was found.
- Blocker conclusion: no print record ID, copies, print person, approval status, or controlled print output can be reported because no real page controlled-print action exists to trigger them.
- Experience consolidation: reviewed `project-experience-consolidation`; no new long-term experience document update was made because the reusable lessons are already covered by `docs/login-access.md` password-env gate and `docs/e2e-rules.md` no API-only/real-page gate.
- Sensitive scan: `rg -n "111111|[Pp]assword\s*=\s*['\"][^'\"]+|Bearer\s+[A-Za-z0-9._-]+|AKIA[0-9A-Z]{16}" doc\tasks\20260802-dcc-controlled-print-e2e` returned no matches; no literal password or bearer-style secret was written to the task directory.

## Current Status

blocked
