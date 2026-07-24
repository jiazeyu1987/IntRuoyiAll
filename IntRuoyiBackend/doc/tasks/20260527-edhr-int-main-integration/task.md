# 20260527-edhr-int-main-integration

## 目标

将 eDHR 审批追踪关闭闭环与字段级不可篡改审计链融合到 `int_main` 后端集成分支，确保审批、追踪、归档、字段审计在同一代码线上自洽运行。

## 里程碑

- [x] M1 后端提交按审定顺序 cherry-pick 到 integration worktree。
- [x] M2 解决字段审计提交与审批闭环提交在执行服务和服务测试中的冲突。
- [x] M3 验证 SQL 契约、审批归档闸门、字段审计链、签名绑定和上下文复用目标测试。
- [ ] M4 与前端集成、主分支快进合并和合并后验证一起完成。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_edhr_approval_archive_schema_contract_sql.py script/tests/test_edhr_field_audit_sql.py -q`
- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionFieldAuditHashTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionFieldAuditControllerTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

后端 integration worktree 已完成融合与目标测试验证；等待前端融合、快进合并和合并结果复验。
