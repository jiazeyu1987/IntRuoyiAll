# 工艺路线组成工序删除今日列

## 任务目标

将工艺路线弹框中“组成工序”表格的 `今日可用`、`今日班次产能` 两列从列表中删除，减少排产资源列对工作站、状态和操作列的挤占。今日产能数据仍保留在后端接口与资源详情弹框中，不改变产能计算。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-form-full-width-dialog/task.md`。
- 检查结果：该任务已标记 `completed`；本任务在其满屏弹框基础上继续简化组成工序表格列。
- 本任务变更请求记录：`docs/changes/20260609-route-process-remove-today-columns.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只删除表格展示列，不新增兜底逻辑。
- `是否从根因和长期维护角度解决`：是。列表只保留标准资源、标准班次产能和资源状态，今日详细能力通过资源详情弹框查看，降低列表密度。
- `是否存在临时补丁或绕过`：否。不隐藏错误、不改数据来源、不删除仍被详情弹框使用的后端字段。

## BDD 场景

- BDD: 组成工序表格不再显示今日列 -> Given 用户打开工艺路线详情或编辑弹框 / When 查看组成工序表格 / Then 表头不再包含 `今日可用` 和 `今日班次产能`。
- BDD: 今日产能详情仍可查看 -> Given 工序存在设备或人工资源 / When 用户点击 `标准资源` / Then 仍打开资源产能详情弹框，并显示今日产能相关详情。

## 里程碑

- [x] M1：添加静态契约 RED 测试，确认组成工序表格不再显示两列。
- [x] M2：调整 `RouteProcessList.vue` 删除 `今日可用`、`今日班次产能` 两列表格展示。
- [x] M3：更新受影响静态契约测试并运行类型检查。

## 预期验证

- `node tests\e2e\mes-route-process-remove-today-columns.spec.js`
- `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`

## 当前状态

completed

## 完成记录

- 已删除 `RouteProcessList.vue` 主表格中的 `今日可用`、`今日班次产能` 两列。
- 已保留 `标准资源` 点击入口，用户仍可进入设备/人工资源详情查看今日产能信息。
- 未修改后端接口字段和产能计算，避免影响资源详情弹框与排产状态判断。

## 最终验证

- `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> PASS。
- `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- `node tests\e2e\mes-pro-route-process-machinery-column.spec.js` -> PASS。
- `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin` 打开路线 `900026` 验证。

## Cleanup Keep

- `doc/tasks/20260609-route-process-remove-today-columns/frontend-feature-evidence.md`
