# 20260802 DCC 升版修订发布真实 E2E Execution Log

## User Intent

用户要求在 `E:\IntRuoyi` 对 DCC 文控“升版/修订发布”进行真实 Playwright E2E 验证，必须使用非 admin 账号、密码通过环境变量注入，通过真实前端页面完成 V1.0 到 V2.0 升版、审批、签名、生效发布、受控浏览和版本历史验证；API/DB 仅用于最终只读核验；不得直接 SQL/API 修改状态、master 指针或审批状态。

## Rule Reads

- Read `AGENTS.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/login-access.md`.
- Read `docs/frontend-development.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/local-runtime.md`.
- Read `docs/worktree-restrictions.md`.
- Read `docs/database-rules.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/powershell-memory.md`.
- Read `docs/experience-index.md`; matching gates copied into `task.md`.
- Read Playwright skill `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.

## BDD

BDD: DCC controlled file revision publish full chain -> Given a non-admin upload/revision initiator and non-admin approver/signature users, and an ACTIVE task-owned V1.0 controlled file, When the initiator creates revision V2.0 through the real DCC page, uploads a new local file, fills revision reason and change description, submits approval, and all approval/signature users complete the real page tasks, Then V2.0 becomes ACTIVE, V1.0 becomes SUPERSEDED, master points to V2.0, controlled browsing defaults to V2.0, version history shows both V1.0/V2.0 with change reason, and read-only API/DB evidence matches the page result.

## Command Intent Log

- Created task directory `doc/tasks/20260802-dcc-revision-publish-real-e2e`.
- Created baseline `task.md` and `execution-log.md`.
- Confirmed `npx` exists at `D:\Programs\npx.ps1`.
- Confirmed `int_main` frontend port `8081` is served by `E:\IntRuoyi\IntRuoyiFronted\node_modules\.bin\..\vite\bin\vite.js`.
- Confirmed `int_main` backend port `48081` is served by Java from `E:\IntRuoyi\output\...`.
- Confirmed backend health `http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`.
- Confirmed frontend `http://127.0.0.1:8081/` returned HTTP `200`.
- Confirmed local Chrome executable exists at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Confirmed upload fixtures exist: `E:\IntRuoyi\resource\批记录节点-解析样本.docx` and `E:\IntRuoyi\resource\过程检验记录.docx`.
- Created task-owned Playwright orchestrator `doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs`.
- Copied task-owned stamped PDF fixture to `doc/tasks/20260802-dcc-revision-publish-real-e2e/stamped-approval-sample.pdf`.

## RED / GREEN

- GREEN: `node --check doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs` -> PASS.
- BLOCKED: `DCC_E2E_PASSWORD` precheck -> FAIL, environment variable is not set in the current shell; impact is that non-admin real-page login cannot be performed without violating the user's password-injection/no-admin/no-API-only constraints.
- GREEN: MinIO precheck -> PASS, `docker-minio-1` running/healthy, ready HTTP `200`, `/data/yudao` bucket exists.
- GREEN: real Playwright revision publish chain for `CODX-DCC-REV-FULL-20260802-20260802091213` -> PASS, V1 `2054545668044070271` created/approved/effective, V2 `2054545668044070272` uploaded/approved/published/effective, publish instance `437` effective.
- RED: first wrapper browser verification -> FAIL, response matcher for `/admin-api/dcc/controlled-files/{id}` also matched same-ID child endpoints and produced `Browser default detail id=undefined`; fixed by exact pathname matching.
- RED: full rerun retry -> FAIL, Playwright response body protocol error after creating partial task-owned V1 `2054545668044070274`; no SQL/API cleanup performed because direct state/approval mutation is forbidden.
- RED: first supplemental browser/history verification -> FAIL, read-only DB precheck hit MySQL collation mismatch on `bpm_form_action_instance.object_id`; fixed by explicit `COLLATE utf8mb4_unicode_ci`.
- RED: second supplemental browser/history verification -> FAIL, viewer detail page does not render the non-viewer `版本历史` content block; fixed assertion to follow the real viewer UI by clicking the `版本` button and verifying the version information dialog, while checking change reason in visible `提交备注`.
- GREEN: supplemental Playwright browser/history verification with `DCC_E2E_USE_EXISTING_CHAIN=1` -> PASS, controlled browser current row is V2 only, browser detail opens V2 viewer, version dialog shows V1/V2, submit remark shows `升版 E2E 20260802091213`.
- GREEN: final read-only DB verification -> PASS, V1 `SUPERSEDED`, V2 `ACTIVE`, master current active pointer `2054545668044070272`, V2 DCC signatures valid, published/stamped file IDs present.
- GREEN: sensitive scan of `doc/tasks/20260802-dcc-revision-publish-real-e2e` -> PASS, no bearer/access/refresh token or plaintext password pattern found.
- GREEN: UTF-8 readback of `task.md`, `execution-log.md`, `verification-report.md`, and `e2e-result.json` with `python -X utf8` -> PASS.
- GREEN: final `node --check doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs` -> PASS.

## Evidence

- `doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs`
- `doc/tasks/20260802-dcc-revision-publish-real-e2e/stamped-approval-sample.pdf`
- `doc/tasks/20260802-dcc-revision-publish-real-e2e/e2e-result.json`
- `doc/tasks/20260802-dcc-revision-publish-real-e2e/browser-current-v2.png`
- `doc/tasks/20260802-dcc-revision-publish-real-e2e/detail-version-history.png`
- `doc/tasks/20260802-dcc-revision-publish-real-e2e/verification-report.md`

## Blockers

- Resolved: missing required environment variable `DCC_E2E_PASSWORD`; user provided approved injection expression and the value was not logged.
- Residual non-blocking artifact: failed retry data `CODX-DCC-REV-FULL-20260802-20260802091853`, controlled file `2054545668044070274`, status `PENDING_MATRIX_REVIEW`; retained because direct SQL/API cleanup of DCC state or approval state is forbidden.

## Final Result

- PASS evidence: `e2e-result.json` status `PASS`, phases `existing-revision-publish-chain`, `browser-and-history-verification`, and `final-readonly-db-verification` all `PASS`.
- Verified final file number: `CODX-DCC-REV-FULL-20260802-20260802091213`.
- Verified V1/V2: V1 `2054545668044070271` -> `SUPERSEDED`, V2 `2054545668044070272` -> `ACTIVE`.
- Verified master: `2054545668044062882` current active controlled file -> V2 `2054545668044070272`.
- Verified signers: V2 DCC password signatures by `zhaohaichen`, `zhaojie`, `zhaomingyu`, `wangsiyu`, all `VALID`.
- Verified controlled browse: active browser row and default viewer detail both point to V2; V1 is absent from current active browser row results.

## Closeout

- Project experience consolidation: updated existing `docs/e2e-rules.md` with DCC viewer-mode version information dialog and exact detail endpoint matching guidance.
- Project experience consolidation: updated existing `docs/task-closeout-rules.md` with cleanup keep bullet path format required by `task-closeout-cleanup`.
- Cleanup preview: `task-closeout-cleanup --mode preview` -> ready; keep final task records, script, `e2e-result.json`, screenshots, stamped fixture; delete only failed retry `chain-result.json`; blocked `<none>`.
- Cleanup apply: `task-closeout-cleanup --mode apply` -> applied; deleted only `chain-result.json`.
- Git closeout: `git status --short --branch` reported `int_main...origin/int_main [ahead 1]` plus many unrelated modified/untracked files outside this task. No staging, commit, revert, or push was performed to avoid mixing unrelated work.
- Final task status: `ready_for_closeout`; verification is PASS, Git commit/push closeout is blocked by unrelated workspace state.
