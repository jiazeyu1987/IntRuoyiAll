# Execution Log

## BDD

BDD: 后端融合后审批与字段审计共存 -> Given 一个草稿 eDHR 已通过字段审计链记录单元格值，When 提交审批并进入 BPM 审批，Then 提交签名必须同时绑定 BPM 流程实例和字段审计证据。

BDD: 后端融合后上下文 key 使用当前租户 -> Given 单元测试默认租户为 1，When 获取入口上下文、详情或分页过滤 activeContextKey，Then activeContextKey 必须与生产代码使用同一租户前缀，避免错误地用 0 租户过滤为空。

BDD: 后端融合后归档必须等待审批关闭 -> Given eDHR 处于提交审批但未关闭状态，When 请求归档，Then 服务必须拒绝归档；只有审批通过并关闭后才允许归档并保留审批与字段审计证据。

## TDD Evidence

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionFieldAuditHashTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionFieldAuditControllerTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `MesProBatchRecordExecutionServiceImplTest` 中两个 activeContextKey 断言仍硬编码 `0:`，与 `BaseDbUnitTest` 默认租户 `1` 和生产 `buildActiveContextKey` 不一致。

GREEN: `python -X utf8 -m pytest script/tests/test_edhr_approval_archive_schema_contract_sql.py script/tests/test_edhr_field_audit_sql.py -q` -> PASS, 21 passed.

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 34 tests passed.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionFieldAuditHashTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionFieldAuditControllerTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 87 tests passed.
