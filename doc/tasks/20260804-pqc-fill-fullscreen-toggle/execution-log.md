# Execution Log

## User Intent

用户使用 PQC 账号登录系统进入 `PQC填写` 页面时，页面顶部按钮默认不显示“主页”，而是显示“最大化”。点击“最大化”后页面全屏最大化，按钮切换成“主页”；点击“主页”后退出全屏恢复。最大化后的视觉布局需与用户截图一致。

## Preconditions And Rule Reads

- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`.
- Read `docs\task-closeout-rules.md`.
- Read `docs\frontend-development.md`.
- Read `docs\e2e-rules.md`.
- Read `docs\powershell-encoding.md`.
- Read `docs\engineering\technology-stack-routing.md`.
- Read `docs\experience-index.md`.
- Read matching PQC experience gate in `docs\backend-development.md`.
- Git status before implementation showed many unrelated dirty files and branch already ahead of `origin/int_main`; this task must keep edits scoped to PQC fullscreen files and avoid mixing unrelated existing changes.

## BDD

- BDD: PQC填写默认最大化入口 -> Given PQC账号进入 PQC填写页面, When 页面首次渲染顶部操作区, Then 操作按钮显示“最大化”且不显示“主页”作为默认入口。
- BDD: PQC填写进入全屏 -> Given PQC填写页面显示“最大化”, When 点击最大化按钮, Then 页面请求浏览器全屏并应用最大化样式，按钮文案切换为“主页”。
- BDD: PQC填写退出全屏 -> Given PQC填写页面处于最大化状态且按钮显示“主页”, When 点击主页按钮, Then 页面退出浏览器全屏并恢复普通布局，按钮文案切回“最大化”。

## RED / GREEN / REGRESSION

- RED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> FAIL, expected reason: `fullscreen state must be applied to the operator panel that also owns PQC pickers and dialogs`，当前 PQC 模板仍硬编码 `@click="handleHome">主页</button>` 且没有 PQC fullscreen 状态。
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS.
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS.
- REGRESSION BLOCKED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> FAIL before this task's PQC assertions, expected unrelated reason: `eDHR batch tabs must include 历史批记录`; current `EdhrBatchRecordTabs.vue` snapshot in workspace does not render that tab.
- GREEN: `workdir=E:\IntRuoyi; git diff --check -- <task-owned files>` -> PASS with only CRLF normalization warnings for existing frontend files.
- GREEN: `workdir=E:\IntRuoyi; python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-fill-fullscreen-toggle/frontend-feature-evidence.md` -> PASS.
- E2E PREFLIGHT: `workdir=IntRuoyiFronted; npx --version` -> PASS, `11.6.2`.
- E2E PREFLIGHT: `workdir=E:\IntRuoyi; Invoke-WebRequest http://127.0.0.1:8081/` -> PASS, `FRONTEND_HTTP=200`.
- E2E PREFLIGHT: `workdir=E:\IntRuoyi; Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS, `BACKEND_HEALTH=UP`.
- E2E PREFLIGHT: `workdir=E:\IntRuoyi; node --check doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs` -> PASS.
- E2E PREFLIGHT: `workdir=E:\IntRuoyi; Get-ChildItem Env: | Where-Object Name -match 'PQC|RRM|EDHR_FRONTLINE'` -> `NO_PQC_E2E_ENV_VARS`, no confirmed PQC E2E account is configured in the current shell.
- REAL E2E BLOCKED: `workdir=E:\IntRuoyi; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs` -> FAIL after reaching the real PQC填写 page and producing screenshots, expected current-environment reason: page error `当前没有活跃订单，PQC 不能选择订单`. This is not recorded as PASS because the requested PQC-account path requires an active PQC order/task data precondition.
- DB PREFLIGHT: local Docker MySQL `int-ruoyi-mysql` -> PASS, MySQL `8.0.39`, database `ruoyi-vue-pro`.
- DB PREFLIGHT: tenant `1` had removed active order `12` for work order `980008` and route `922119`; all existing tasks for that order were `SUBMITTED`. The fixture therefore created a separate task-owned work order instead of resetting historical submitted tasks.
- DB RED: first fixture transaction -> FAIL with MySQL `ERROR 1267 Illegal mix of collations`; transaction was not committed.
- DB GREEN: rerun with explicit `_utf8mb4 ... COLLATE utf8mb4_unicode_ci` -> PASS, created user `914524`, work order `980019`, and active order `30`.
- LOGIN RED: username `pqc_e2e_fullscreen` -> FAIL, expected backend validation reason: `账号格式为数字以及字母`.
- LOGIN GREEN: task-owned username updated to `pqce2efullscreen` -> PASS, login reached `/index`.
- DATA RED: first PQC-account E2E after creating one task -> FAIL at route process `928610`, expected reason: the backend requires a pending task for every published route process in the selected route.
- DATA GREEN: created one PENDING task for each of 14 published route processes and equipment options for all equipment-required PATROL items -> `missing_task_count=0`.
- SCRIPT RED: fullscreen E2E initially rejected the formal initialization request `POST .../pqc/switch-employee` as a generic MES write.
- SCRIPT GREEN: scoped write assertions now require exactly one task-owned `pqc/switch-employee`, forbid `pqc/submit`, and forbid all other MES writes.
- NETWORK RED: navigation away from `/index` produced aborted homepage requests that were incorrectly classified as PQC target failures.
- NETWORK GREEN: target classification now hard-fails only `/admin-api/mes/pro/feedback/frontline/device-account/pqc/` failures and records aborted homepage requests separately.
- REAL E2E GREEN: `PQC_FULLSCREEN_E2E_USERNAME=pqce2efullscreen; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs` -> PASS, identity `芋道源码/pqce2efullscreen`.

## Milestone Updates

- Located target entry from router and tests: `BatchPqcFillPage.vue` renders `FrontlineFixedTemplatePanel mode="pqc"`.
- Located screenshot UI block in `FrontlineFixedTemplatePanel.vue` under `data-frontline-pqc-operator`.
- Added `edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` to lock default “最大化”, fullscreen “主页”, browser fullscreen lifecycle, and screenshot-like full-width proportions.
- Updated PQC block in `FrontlineFixedTemplatePanel.vue` to render `pqcFullscreenActionText`, request fullscreen on the outer panel, and keep picker/dialog overlays inside the fullscreen subtree.
- Updated adjacent fill-tabs static contract so PQC no longer requires the old hard-coded `主页` button before fullscreen.
- Ran `project-experience-consolidation` check; existing `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁` already covers the reusable fullscreen subtree rule, so no long-term experience document was created or modified.
- Added and ran a real Playwright E2E script for `/mes/pro/feedback/edhr-batch-pqc-fill`; the script reached the page, exercised the 最大化/主页/恢复 sequence, and refreshed screenshots under `output\playwright\20260804-pqc-fill-fullscreen-toggle\`, but strict E2E result remains BLOCKED due the active-order business precondition page error.
- Created the user-requested local PQC E2E preconditions: user `pqce2efullscreen`, work order code `PQC-E2E-FS-20260804`, active order `30`, 14 PENDING route-process tasks, 32 PATROL equipment options, and a PQC personnel scope.
- Final Playwright result records fullscreen panel `1525x841`, grid rows `114px 585px 102px`, picker retained inside the fullscreen subtree, no PQC submit requests, no page/console errors, and no PQC target failures.
- Removed the task-owned login debug screenshot; retained only the three acceptance screenshots and `result.json`.
- Ran `project-experience-consolidation`; MySQL collation and Playwright non-target request rules were already covered by `docs/database-rules.md` and `docs/e2e-rules.md`. Added the missing reusable E2E username-format gate to `docs/login-access.md`.

## Blockers

- Formal closeout remains blocked by unrelated dirty workspace and branch state: initial `git status --short --branch` showed many pre-existing dirty files and `int_main...origin/int_main [ahead 14]`; final status still shows many unrelated dirty files and `int_main...origin/int_main [ahead 16]`.
- Adjacent fill-tabs regression command remains blocked by pre-existing `EdhrBatchRecordTabs.vue` content missing “历史批记录”; this was not changed by this task.
- The PQC-account/data blocker is resolved. The configured local fixture is intentionally retained per the user's request and has an explicit rollback scope in `database-schema-evidence.md`.
