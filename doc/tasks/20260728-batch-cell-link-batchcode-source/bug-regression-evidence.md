# Bug Regression Evidence

## Bug Summary

创建 eDHR 批次执行并打开/创建传统批记录执行记录时，已配置的批记录单元格链接 `PRODUCTION_WORK_ORDER.batchCode -> 4:1` 自动落库报错：`批记录单元格链接自动落库缺少来源值：executionId=32，ruleId=16，sourceField=batchCode，targetCell=4:1`。

## Expected Behavior

当批次执行创建入参已经提供批号，且批记录执行记录已保存该解析后的 `batchCode` 时，`sourceField=batchCode` 应读取当前批记录执行上下文的正式批号并继续通过字段审计链自动落库；如果执行上下文批号真实缺失，则继续 fail-fast，不写空值。

## Reproduction Command

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`MesProBatchRecordCellLinkServiceImpl.resolveProductionWorkOrderFieldValue` 对所有 `PRODUCTION_WORK_ORDER` 来源字段都读取 `MesProWorkOrderDO`。其中 `batchCode` 在批记录执行创建链路中已经由 `MesProBatchRecordExecutionServiceImpl.resolveBatchCode` 从创建入参或工单批号解析后写入 `MesProBatchRecordExecutionDO.batchCode`，但单元格链接预填没有读取该执行上下文；当生产工单主表 `batchCode` 为空时，预填服务返回 `SOURCE_VALUE_MISSING`，自动落库服务按设计 fail-fast 抛出缺少来源值。

## Regression Test

新增 `MesProBatchRecordCellLinkServiceImplTest#getPrefill_resolvesProductionBatchCodeFromExecutionContextWhenWorkOrderBatchCodeEmpty`，覆盖目标执行记录 `batchCode` 有值、生产工单 `batchCode` 为空、规则 `sourceField=batchCode` 且目标单元格为 `4:1` 时必须产生一个可落库预填值。

## RED: Failing Regression

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增用例期望 `prefills.size() == 1`，实际为 `0`。

## GREEN: Fixed Regression

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests。

## Verification

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10 tests。

## Risk

低风险。变更只影响 `PRODUCTION_WORK_ORDER.batchCode` 的值解析，使其使用批记录执行创建时已经解析并持久化的正式执行上下文批号；其他生产工单字段仍按原有 `MesProWorkOrderDO` 来源读取，目标已有人工值不覆盖、来源真实缺失 fail-fast、字段审计链和幂等键逻辑保持不变。

## Blockers And Follow-Up

无当前阻塞。发现一个非本任务 `.docx` 删除留在工作区，未纳入本次修复提交范围。
