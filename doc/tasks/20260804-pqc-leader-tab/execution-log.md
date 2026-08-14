# Execution Log

## User Intent

PQC 组长内容需要从组长工作台中拆出。最终纠正口径：`生产组长` 和 `PQC组长` 都必须是类似 `批次执行` 的独立主导航页签/菜单入口，位于 eDHR 父菜单下、`QA` 下面；不是 eDHR 批次页面内部 Tab。

- 追加反馈：当前 `admin` 登录后看不到新增的生产组长/PQC组长入口，要求明确保证 `admin` 可见。

## Preconditions And Rule Reads

- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`.
- Read `C:\Users\BJB110\.codex\skills\database-schema-delivery\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\database-schema-delivery\references\database-contract.md`.
- Read `C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\references\qa-contract.md`.
- Read `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.
- Read `docs/e2e-rules.md`, `docs/login-access.md`, `docs/local-runtime.md`, and `docs/worktree-restrictions.md` before real E2E/runtime checks.
- Prior handoff confirmed reads for `docs/frontend-development.md`, `docs/database-rules.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, and `docs/experience-index.md`.
- Git status before closeout had existing unrelated dirty/ahead state; task-owned edits must not be mixed with unrelated concurrent changes.

## BDD

- BDD: 两类组长独立主导航页签 -> Given 用户展开 eDHR 主导航, When 查看 QA 与批次执行之间的菜单, Then 依次显示 `生产组长` 和 `PQC组长` 两个独立入口。
- BDD: 生产组长独立页面 -> Given 用户进入 `生产组长`, When 页面加载, Then 使用 `leaderType=PRODUCTION` 且不显示内部类型切换。
- BDD: PQC组长独立页面 -> Given 用户进入 `PQC组长`, When 页面加载, Then 使用 `leaderType=PQC` 且不显示内部类型切换。
- BDD: eDHR 内部页签清理 -> Given 用户打开 eDHR 批次内部页签, When 查看顶部 tabs, Then 不出现 `组长工作台`、`生产组长` 或 `PQC组长`。
- BDD: admin 菜单可见 -> Given 启用的 `admin` 用户在目标租户拥有正式管理角色, When 菜单迁移执行并补齐新菜单记录, Then `admin` 对 QA、生产组长和 PQC组长入口均有有效可见绑定。

## RED / GREEN / REGRESSION

- RED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL, expected reason: corrected contract required standalone production/PQC leader pages while existing eDHR tabs/route graph still carried leader entries.
- RED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> FAIL, expected reason: eDHR page graph still referenced old eDHR leader route semantics.
- RED: `workdir=IntRuoyiFronted; node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> FAIL, expected reason: SQL/menu contract did not include `生产组长` menu id `900436` and corrected ordering.
- RED: `workdir=E:\IntRuoyi; python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> FAIL, expected reason: SQL migration did not yet declare the seven-entry visible order with production/PQC leader menus.
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> PASS, eDHR internal tabs no longer expose leader content and old wrapper routes remain negative assertions only.
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS, page graph now points production/PQC leader nodes to process-pool main-navigation routes.
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS, QA/production leader/PQC leader dynamic menu contract is aligned.
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS, wrapper pages lock leader type without internal type tabs.
- GREEN: `workdir=E:\IntRuoyi; python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> PASS, 3 SQL contract tests passed.
- REGRESSION: `workdir=E:\IntRuoyi\IntRuoyiFronted; pnpm ts:check` -> PASS.
- REGRESSION: `workdir=E:\IntRuoyi; git diff --check -- <task-owned paths>` -> PASS; Git emitted CRLF normalization warnings only.
- GREEN: `workdir=E:\IntRuoyi; python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-leader-tab/frontend-feature-evidence.md` -> PASS.
- GREEN: `workdir=E:\IntRuoyi; python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260804-pqc-leader-tab/database-schema-evidence.md` -> PASS.
- RED: `workdir=E:\IntRuoyi; python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> FAIL, expected reason: admin 可见性回归合同要求迁移覆盖 admin 可见路径前，SQL 合同缺少对应证据。
- GREEN: `workdir=E:\IntRuoyi; python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> PASS，3 SQL contract tests passed after confirming the migration declares production/PQC leader menu rows, package inclusion, and admin-role bindings.
- DATA: local MySQL `int-ruoyi-mysql` applied `20260804_mes_edhr_qa_menu.sql` successfully; read-only verification showed tenant 1 admin and tenant 122 admin each have effective bindings for menu ids `900434`, `900435`, and `900436`.
- DATA: a first local attempt briefly over-bound admin's non-admin roles; 42 task-created rows were immediately soft-deleted with updater `edhr-qa-menu-scope-fix`, and follow-up read-only verification showed no non-admin role without existing eDHR anchors retained the new menu bindings.
- REGRESSION: `workdir=IntRuoyiFronted; node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS after admin visibility fix.
- REGRESSION: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> PASS after admin visibility fix.
- REGRESSION: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS after admin visibility fix.
- REGRESSION: `workdir=IntRuoyiFronted; node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS after admin visibility fix.
- REGRESSION: `workdir=IntRuoyiFronted; pnpm ts:check` -> PASS after admin visibility fix.
- GREEN: `workdir=E:\IntRuoyi; node --check doc\tasks\20260804-pqc-leader-tab\e2e-admin-edhr-menu-readonly.cjs` -> PASS.
- GREEN: `workdir=E:\IntRuoyi; node doc\tasks\20260804-pqc-leader-tab\e2e-admin-edhr-menu-readonly.cjs` -> PASS, real Playwright login as `芋道源码/admin` verified eDHR admin menu visibility, `/mes/pro/process-pool/production-leader`, `/mes/pro/process-pool/pqc-leader`, hidden leader-type tabs, eDHR internal tabs cleanup, target page APIs HTTP 200/business code 0, and MES write request count `0`.
- DATA: E2E result artifact `doc/tasks/20260804-pqc-leader-tab/e2e-output/admin-edhr-menu-readonly-result.json` -> `status=PASS`, `targetFailureCount=0`, `pageErrorCount=0`, `mesWriteRequestCount=0`; non-target navigation-aborted GETs were recorded separately and did not affect target controls.
- DATA: read-only local DB check of `system_menu` under parent `900220` returned exactly seven visible type-2 children in order: `批记录表单(0)`, `QA(1)`, `生产组长(2)`, `PQC组长(3)`, `批次执行(4)`, `表单追溯(5)`, `表单日志(6)`.

## Milestone Updates

- Removed `生产组长`、`PQC组长`、`组长工作台` from `EdhrBatchRecordTabs.vue`.
- Updated `BatchPageGraphPage.vue` so production/PQC leader review nodes use `/mes/pro/process-pool/production-leader` and `/mes/pro/process-pool/pqc-leader`.
- Updated `20260804_mes_edhr_qa_menu.sql` to declare seven visible entries under eDHR: `批记录表单`, `QA`, `生产组长`, `PQC组长`, `批次执行`, `表单追溯`, `表单日志`.
- Added/kept SQL package and role binding coverage for QA plus both leader menus.
- Updated static contracts to reflect final main-navigation page口径 and to keep QA DCC project-code selector without allowing DCC document taxonomy page semantics.
- Project experience consolidation: updated `docs/frontend-development.md#前端角色内容页签拆分口径门禁` and `docs/experience-index.md` so future tasks distinguish main-navigation “页签” from page-internal `el-tabs`.
- Applied the local MySQL menu migration so current `admin` can see the new entries without waiting for a later release migration run.
- Added and ran a task-owned read-only Playwright E2E script that proves current local `admin` can see and open the standalone `生产组长` and `PQC组长` eDHR entries.

## Blockers

- Closeout commit/push is blocked by existing unrelated dirty files and current branch ahead state; committing now would risk mixing non-task changes.
- Real Playwright E2E is now complete and passing; remaining blocker is only final commit/push ownership in the shared dirty/ahead workspace.
