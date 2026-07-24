# eDHR 批次执行工单有效性后端校验任务

- Task ID: `20260609-edhr-work-order-select`
- Status: `completed`
- Branch: `int_main`

## 任务目标

为 eDHR 批次执行 `openOrCreate` 增加工单有效性门禁：生产工单必须存在、状态为已确认且未临时冻结，避免绕过前端直接创建无效批次执行。

## 里程碑

1. RED：新增 service 测试，验证草稿或临时冻结工单不能创建 eDHR 批次执行。
2. GREEN：在后端 service 中 fail fast 校验工单状态和冻结状态。
3. REGRESSION：运行 eDHR 批次执行 service 测试。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；无效工单直接报错。
- `是否从根因和长期维护角度解决`：是；前端选择和后端校验同时收口。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 已完成：`openOrCreate` 增加已确认且未冻结工单校验；新增 service 测试覆盖未确认和临时冻结工单。
- 验证：`mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` 通过。
