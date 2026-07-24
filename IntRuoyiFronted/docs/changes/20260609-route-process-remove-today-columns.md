# 变更请求：删除组成工序今日列

## Request Summary And Source

- Source: 用户在当前任务中确认“这两列可以删除”。
- Summary: 将工艺路线弹框“组成工序”表格中的 `今日可用`、`今日班次产能` 两列从列表展示中删除。

## Current Baseline Reviewed

- `src/views/mes/pro/route/RouteProcessList.vue` 当前展示 `今日可用`、`今日班次产能` 两列。
- `今日可用` 列当前提供 `openProcessCapacityDetail(scope.row)` 点击入口。
- 后端 `/mes/pro/route-process/list-by-route` 仍返回今日产能相关字段，资源详情弹框仍使用这些字段展示设备/人工产能详情。
- 前序任务 `doc/tasks/20260609-route-form-full-width-dialog/task.md` 已完成，工艺路线弹框已接近满屏宽度。

## Classification

Requirement change / UI simplification.

## Impact

- Product: 组成工序表格减少两列，更简洁；今日能力信息仍可从资源详情弹框查看。
- Design: 列密度下降，工作站、资源状态等列更容易查看。
- Data: 不改变任何数据写入、读取或计算。
- API: 不删除后端字段，因为资源详情弹框和状态判断仍需要今日产能数据。
- Test: 更新静态契约测试，删除旧的表格列必需断言，新增“不显示今日列”的断言。
- Release: 前端小范围展示调整，风险低。
- Operations: 排产员需要通过 `标准资源` 或资源详情入口查看今日资源明细，不再在列表直接看这两列。

## Decision

Accepted.

## Required Approvals

- 用户已在当前对话中明确确认“这两列可以删除”。

## Downstream Skill Reruns

- `frontend-feature-delivery`: 更新任务文档、静态契约测试、前端组件并运行验证。

## Blockers And Next Action

- Blockers: 无。
- Next action: 创建删除今日列任务，按 RED -> GREEN 更新 `RouteProcessList.vue` 与受影响测试。
