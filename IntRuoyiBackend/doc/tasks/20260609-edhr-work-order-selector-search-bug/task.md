# eDHR 工单下拉搜索缺陷后端修复任务

- Task ID: `20260609-edhr-work-order-selector-search-bug`
- Status: `completed`
- Branch: `int_main`

## 任务目标

修复 eDHR 批次执行后端工单有效性校验过严的问题：`openOrCreate` 应接受未临时冻结且未取消的有效生产工单，不应强制要求工单状态必须为已确认，避免前端下拉可选工单提交时被后端再次拦截。

## 里程碑

1. RED：新增 service 测试，验证草稿但未冻结工单可以创建 eDHR 批次执行。
2. GREEN：后端校验改为拒绝临时冻结或已取消工单。
3. REGRESSION：运行 eDHR 批次执行 service 测试。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；无效工单直接报错。
- `是否从根因和长期维护角度解决`：是；后端有效性口径与前端选择口径保持一致。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 用户证据：输入 `881` 时下拉显示“无数据”，实际业务期望是可选择未冻结工单，不应额外限定已确认。
- 根因：后端 `openOrCreate` 仍把有效工单限定为已确认工单，与“未取消且未冻结”的选择口径不一致。
- 修复：后端拒绝已取消或临时冻结工单，允许未取消且未冻结工单创建或打开 eDHR 批次执行。
- 验证：`mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` 通过。
