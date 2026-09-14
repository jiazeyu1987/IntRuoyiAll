# Execution Log

## 2026-07-27

- USER: 指出上一轮修复有问题：设置不用走审批就不应该走审批，设置要走审批就必须走审批。
- PRECHECK: 读取 bug-regression-fix-loop、backend-api-delivery、task-closeout、backend、database、PowerShell 编码和 Git 门禁。
- BDD: Direct upgrade publishes immediately -> Given 表单模板升版策略为 DIRECT When 导入已有模板生成 DRAFT 新版本 Then 直接发布该版本，不启动 BPM。
- BDD: BPM upgrade starts approval -> Given 表单模板升版策略为 BPM_REQUIRED When 导入已有模板生成 DRAFT 新版本 Then 启动 `form-template-upgrade-v1` 并把版本置为 `PENDING_APPROVAL`。
- BDD: Direct obsolete obsoletes immediately -> Given 表单模板作废策略为 DIRECT When 提交作废 Then 直接置为 `OBSOLETE`，不启动 BPM。
- BDD: BPM obsolete starts approval -> Given 表单模板作废策略为 BPM_REQUIRED When 提交作废 Then 启动作废 BPM 并置为 `PENDING_APPROVAL`。
- PRECHECK: dirty-worktree baseline -> PASS, commit `0d7ab593`, files: `IntRuoyiBackend/script/tests/test_dcc_onlyoffice_local_runtime_config.py`, `IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml`, `IntRuoyiBackend/yudao-server/src/test/java/cn/iocoder/yudao/server/DccOnlyOfficeLocalConfigTest.java`, `doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-artifacts/special-node-filler-yudao-real.json`。
- RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> FAIL, expected reason: old policy admin forced `FORM_TEMPLATE_UPGRADE` to `BPM_REQUIRED`, direct upgrade/obsolete executor threw requires BPM approval.
- RED: `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> FAIL, expected reason: old seed still forced `SET policy_mode = 'BPM_REQUIRED'`.
- IMPLEMENTATION: removed hard-coded mandatory BPM executor guard from policy administration; implemented direct upgrade publish and direct obsolete status update; changed upgrade/obsolete seed contracts so published DIRECT policy is not overwritten or treated as conflict.
- EXPERIENCE: corrected `docs/backend-development.md` gate from "强制 BPM" to "按配置执行"; updated `docs/experience-index.md` route.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> PASS, 32 tests passed.
- GREEN: `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py` -> PASS, 7 tests passed.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS, 4 tests passed.
- GREEN: hard-coded BPM residue scan -> PASS, old production/test error strings and `BPM_REQUIRED_EFFECT_EXECUTOR_CODES` removed from code paths.
- STATUS: implementation and required verification complete; task set to `ready_for_closeout` pending evidence validation, cleanup, commit, and push.
- GREEN: bug evidence validation -> PASS, `Bug regression evidence is valid.`
- GREEN: backend API evidence validation -> PASS, `Backend API evidence is valid.`
- GREEN: cleanup preview -> PASS, keep `backend-api-evidence.md`, `bug-regression-evidence.md`, `execution-log.md`, `task.md`, `verification-report.md`; delete none; blocked none; warnings none.
- GREEN: cleanup apply -> PASS, deleted none.
- EXPERIENCE: project-experience-consolidation applied to existing `docs/backend-development.md` and `docs/experience-index.md`; no new long-term document needed.
- STATUS: cleanup complete; task marked `completed` pending implementation/closeout commits and push.
- COMMIT: implementation -> `938ba1e0 fix: respect form template approval policy mode`, files: business approval policy administration, form template upgrade/obsolete executors, targeted tests, upgrade/obsolete seed contracts, backend gate and experience-index route.
