# eDHR 批次执行工单有效性后端校验执行日志

## 2026-06-09

- BDD: 后端拒绝未确认工单 -> Given 生产工单存在但不是已确认状态 When 调用 eDHR 批次执行 openOrCreate Then 后端拒绝创建并返回明确错误。
- BDD: 后端拒绝冻结工单 -> Given 生产工单已确认但被临时冻结 When 调用 eDHR 批次执行 openOrCreate Then 后端拒绝创建并返回明确错误。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` -> FAIL, expected reason: 只应用新增 service 测试且未应用后端实现时，测试编译阶段缺少 `PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID` 错误码。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` -> PASS，13 个 eDHR 批次执行 service 场景通过，新增覆盖未确认工单和临时冻结工单拒绝创建。
