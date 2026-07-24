# eDHR 工单下拉搜索缺陷后端执行日志

## 2026-06-09

- BDD: 后端允许未取消且未冻结工单 -> Given 生产工单存在、未取消且未临时冻结 When 调用 eDHR 批次执行 openOrCreate Then 后端创建或打开批次执行，不因状态不是已确认而拒绝。
- BDD: 后端拒绝取消或冻结工单 -> Given 生产工单已取消或被临时冻结 When 调用 eDHR 批次执行 openOrCreate Then 后端 fail fast，不创建 eDHR 批次执行。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` -> FAIL, expected reason: `openOrCreate_allowsPreparedUnfrozenWorkOrder` 被旧校验拒绝，错误为 `eDHR 批次执行只能选择已确认且未冻结的生产工单`。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` -> PASS，14 个 eDHR 批次执行 service 场景通过，新增覆盖草稿未冻结工单允许创建、取消工单拒绝创建。
