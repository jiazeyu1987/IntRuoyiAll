# Verification Report

## Current Result

PASS for current submit gate before Git commit/push. 用户已授权提交/推送；本报告记录截至提交前的验证结论。尚需处理 `int_main` 相对 `origin/int_main` 的 `ahead 2 / behind 2` 分歧并完成推送。

## Evidence

- `python -X utf8 -m pytest ...test_dcc_controlled_file_submit_idempotency_sql.py ...test_dcc_retire_form_center_upload_entry_sql.py ...test_system_signature_release_migration_metadata_sql.py ...test_mes_process_pool_device_selection_mode_sql.py ...test_gxp_audit_core_contract.py ...test_edhr_multi_signature_approval_sql.py -q` -> PASS，17 passed。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260911-commit-frontend-backend\migration-policy-gate-final.json` -> PASS，migrationCount 618。
- `mvn -q -pl yudao-module-dcc -am clean -Dtest=DccControlledFileQueryServiceTest#getControlledFile_requesterOutsideAssignmentHardScopeIsDenied,DccPublicationFollowupQueryServiceTest#getFileFollowup_linkRevisionTimelineUsesAuditLinkedVersionAfterTaskReopened -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn -q -pl yudao-module-infra -am -Dtest=RuntimeControlServiceImplTest#executeBackupNowShouldAllowTestEnvironmentWithoutProdConfirmAndPersistTargetEnvironment+executeBackupNowShouldAllowProdReadonlyBackupWhenProductionAccessIsDisabled -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn -q -pl yudao-module-system -am -Dtest=AuthConvertTest,TemporaryRoleGrantServiceImplTest,InvoiceVoucherPrintAssistantErpConfigBridgeContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn -q -pl yudao-module-bpm -am -Dtest=FormCenterTemplateImportRuntimeTest,FormCenterTemplateVersionQueryTest,FormTemplateFillRuleAutoDetectServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn -q -pl yudao-module-mdm -am -Dtest=MdmDependencyDirectionContractTest,MdmCompanyScopeQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `pnpm ts:check` in `IntRuoyiFronted` -> PASS。
- `git diff --check -- IntRuoyiBackend IntRuoyiFronted` -> PASS，仅 LF/CRLF warning。
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main` 端口矩阵为前端 8081、后端 48081。

## Warnings / Follow-up

- 扩展执行的全量 `mvn -q -pl yudao-module-dcc -am -Dsurefire.failIfNoSpecifiedTests=false test` 暴露 DCC 全仓级历史/并发问题，未列入本提交任务 Expected Verification；本任务门禁使用已清理 target 后的 DCC 定向回归。
- `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` 读取外部仓库 `E:\ProjectPackage\erp-invoice-voucher-print-assistant` 的当前工作区状态；该外部仓库有脏改动，未纳入本主仓库提交。
