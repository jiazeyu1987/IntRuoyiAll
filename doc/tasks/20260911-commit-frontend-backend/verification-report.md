# Verification Report

## Current Result

PASS. 用户已授权提交/推送；提交阻断点已修复，验证门禁已通过，前后端代码已推送到 `origin/int_main`。

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
- `git diff --check origin/int_main..HEAD -- IntRuoyiBackend IntRuoyiFronted docs doc` -> PASS。
- `git rev-list --objects origin/int_main..HEAD | rg "resource/"` -> no matches before final push；本轮远端提交不包含 `resource/` 大文件。
- `git -c http.version=HTTP/1.1 -c http.postBuffer=524288000 push origin int_main` -> PASS，`5e556f31d..860496e33  int_main -> int_main`。

## Warnings / Follow-up

- 扩展执行的全量 `mvn -q -pl yudao-module-dcc -am -Dsurefire.failIfNoSpecifiedTests=false test` 暴露 DCC 全仓级历史/并发问题，未列入本提交任务 Expected Verification；本任务门禁使用已清理 target 后的 DCC 定向回归。
- `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` 读取外部仓库 `E:\ProjectPackage\erp-invoice-voucher-print-assistant` 的当前工作区状态；该外部仓库有脏改动，未纳入本主仓库提交。
- 首次 HTTPS push 因 `resource/通用检验规程` 下两个约 33MB PPTX 大对象返回 HTTP 408；按本轮“提交前后端代码”范围，已将这些新增 Office 资源保留为本地未跟踪文件并从待推提交中移除。
