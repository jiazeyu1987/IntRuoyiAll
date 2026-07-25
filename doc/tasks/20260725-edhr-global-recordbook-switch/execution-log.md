# Execution Log

## User Intent

用户要求实现 eDHR 金手指全局记录本开关。开关位于个人中心配置页签，仅金手指可见可改；关闭后所有批次所有工序隐藏批记录/记录本切换按钮，并强制走批记录流程。

## BDD

- BDD: 金手指关闭全局记录本 -> Given 金手指用户在个人中心配置页签 When 关闭记录本开关 Then 所有批次详情不显示“批记录/记录本”切换按钮 And 打开任务进入批记录流程。
- BDD: 非金手指不可配置 -> Given 普通用户进入个人中心 When 查看页签 Then 不显示配置页签 And 直接请求开关接口返回权限失败。
- BDD: 关闭后禁止直连记录本 -> Given 全局记录本已关闭 When 用户用旧链接携带 fillCarrier=RECORDBOOK 打开或提交 Then 前端提示关闭状态 And 后端拒绝记录本写入。
- BDD: 重新开启恢复现有逻辑 -> Given 金手指用户重新开启记录本 When 进入原本任务配置允许记录本的批次工序 Then 可再次看到切换按钮并选择记录本。

## Command Intent And Evidence

- Read project rules and skill guidance for backend, frontend, database, BDD, task closeout, PowerShell encoding, Git orchestration, E2E, login and local runtime.
- Git preflight: branch `int_main`, origin `https://github.com/jiazeyu1987/IntRuoyiAll.git`, worktree already dirty and ahead before this task. Baseline commit required before implementation edits.
- GREEN: experience-preflight -> PASS, `docs/experience-index.md` read after task creation; applicable backend eDHR runtime gates, static E2E gate, real E2E fixture gate, and Git dirty baseline gate copied into `task.md`.
- GREEN: dirty-worktree-baseline -> PASS, created baseline commit `c44492e4 chore: baseline pre-existing workspace changes` before implementation. Baseline files: `IntRuoyiFronted/tests/e2e/edhr-batch-execution-golden-finger-bulk-void-static.spec.js`, `IntRuoyiFronted/tests/e2e/edhr-preview-header-layout-static.spec.js`, `IntRuoyiFronted/tests/e2e/system-codex-test-management-real.e2e.js`, `doc/tasks/20260725-fix-batch-execution-sfc-syntax/verification-report.md`, `doc/tasks/20260725-full-e2e-admin-validation/execution-log.md`, `doc/tasks/20260725-test-management-manual-replan-881mo/test-management-manual-replan-full.e2e.cjs`.
- Implemented SQL seed `sql/mysql/20260725_mes_edhr_recordbook_global_setting.sql` with config key `mes.edhr.recordbook.global.enabled` default `true`.
- Implemented backend controller `/mes/pro/edhr-recordbook-setting/global`, VO, global config service, strict boolean parsing, operation audit, runtime effective recordbook value and recordbook unrestricted write block.
- Implemented frontend API wrapper, Profile 配置页签/组件, 批次详情隐藏填写载体切换和强制 FORM, 填写页直连记录本提示与保存门禁。

## RED/GREEN/REGRESSION

- RED: `python IntRuoyiBackend/script/tests/test_mes_edhr_recordbook_global_setting_sql.py` -> expected FAIL before SQL migration exists.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrRecordbookGlobalSettingContractTest test` -> expected FAIL before backend controller/service/VO/runtime gates exist.
- RED: `node IntRuoyiFronted/tests/e2e/edhr-recordbook-global-setting-static.spec.js` -> expected FAIL before frontend API/profile/batch/detail execution changes exist.
- GREEN: `python IntRuoyiBackend/script/tests/test_mes_edhr_recordbook_global_setting_sql.py` -> PASS, SQL contract passed after migration added and backtick metadata fixed.
- GREEN: `node IntRuoyiFronted/tests/e2e/edhr-recordbook-global-setting-static.spec.js` -> PASS, frontend static contract passed after Profile, batch detail and execution page changes.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordbookGlobalSettingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0.
- REGRESSION: initial `mvn -pl yudao-module-mes -am -Dtest=MesProEdhrRecordbookGlobalSettingContractTest test` failed because sibling modules had no matching specified test; reran with quoted `-Dsurefire.failIfNoSpecifiedTests=false` per reactor guidance.
- REGRESSION: `pnpm ts:check` first timed out at 124s, then failed in `src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` with missing `selectedGoldenFingerBulkVoidIds`, `selectCurrentPageGoldenFingerBulkVoidRows`, `handleBatchExecutionSelectionChange`, and related non-task symbols; later stale blocker复验 passed after parallel workspace state changed.
- GREEN: `pnpm ts:check` -> PASS, `vue-tsc --noEmit -p tsconfig.relaxed.json` completed successfully.
- REGRESSION: `node IntRuoyiFronted\tests\e2e\edhr-recordbook-global-setting-static.spec.js` from frontend root -> FAIL, incorrect duplicated working-directory path resolved to `IntRuoyiFronted\IntRuoyiFronted\...`; no product failure.
- GREEN: `node tests\e2e\edhr-recordbook-global-setting-static.spec.js` from `IntRuoyiFronted` -> PASS.
- GREEN: real E2E preflight -> PASS, Playwright logged into local `http://127.0.0.1:8081` as `芋道源码/admin`, verified permission info contains `mes:pro-batch-record-execution:golden-finger`, and found candidate batch `900000000819`, task `5989`, execution `1321`.
- REGRESSION: real E2E close/direct-link script -> PASS for product assertions but FAIL during automatic UI restore hook; global setting was temporarily left `false`, so cleanup immediately restored through authenticated API.
- GREEN: real E2E restore cleanup -> PASS_RESTORED, authenticated API restored `mes.edhr.recordbook.global.enabled` from `false` to `true`.
- GREEN: real E2E reopen verification -> PASS_REOPEN_VERIFIED, setting `true`, batch `900000000819` task `5989` effective `recordbookEnabled=true`, detail page shows fill-carrier control and “记录本” button again.
- GREEN: SQL contract rerun -> PASS, `python IntRuoyiBackend\script\tests\test_mes_edhr_recordbook_global_setting_sql.py`.
- GREEN: backend contract rerun -> PASS, `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordbookGlobalSettingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- GREEN: project-experience-consolidation -> PASS, merged reusable global-switch E2E restore lesson into `docs/e2e-rules.md#全局开关类 E2E 恢复门禁` and indexed it in `docs/experience-index.md`.

## Blockers

- BLOCKED: Commit/push/closeout still pending precise staging because current worktree contains many unrelated parallel dirty files and several shared files contain mixed task and non-task hunks.
