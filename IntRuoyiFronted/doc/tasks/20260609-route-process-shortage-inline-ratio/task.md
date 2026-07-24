# 工艺路线组成工序短缺比值展示

## 任务目标

在工艺路线弹框“组成工序”表格中删除 `今日可用`、`今日班次产能` 两列后，将今日资源短缺信息压缩展示到现有列：

- 当今日可用资源小于标准资源时，`标准资源` 以红色显示 `今日/标准`，例如 `4/5`。
- 当今日班次产能小于标准班次产能时，`标准班次产能` 以红色显示 `今日/标准`，例如 `400/500`。
- 当今日不低于标准时，仍按原样显示标准资源和标准班次产能。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-process-remove-today-columns/task.md`。
- 检查结果：该任务已标记 `completed`，本任务在其删除今日列的结果上继续优化短缺展示。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只使用后端已有标准/今日字段进行展示，不新增兜底数据源。
- `是否从根因和长期维护角度解决`：是。今日资源短缺通过标准列内联比值表达，保留列表简洁度，同时不丢失异常信号。
- `是否存在临时补丁或绕过`：否。不修改后端接口、不改产能算法、不隐藏资源状态。

## BDD 场景

- BDD: 资源短缺时标准资源显示红色比值 -> Given 工序标准资源为 5 且今日可用为 4 / When 用户查看组成工序表格 / Then `标准资源` 显示红色 `4/5`。
- BDD: 产能短缺时标准班次产能显示红色比值 -> Given 工序标准班次产能为 500 且今日班次产能为 400 / When 用户查看组成工序表格 / Then `标准班次产能` 显示红色 `400/500`。
- BDD: 无短缺时保持标准值展示 -> Given 今日可用资源和今日班次产能不小于标准值 / When 用户查看组成工序表格 / Then `标准资源` 和 `标准班次产能` 仍显示标准值。

## 里程碑

- [x] M1：添加静态契约 RED 测试，锁定短缺比值和红色样式。
- [x] M2：实现 `标准资源`、`标准班次产能` 的短缺比值展示。
- [x] M3：更新受影响 E2E 断言并运行静态、类型、真实 UI 验证。

## 预期验证

- `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js`
- `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js`

## 当前状态

completed

## 完成记录

- `标准资源` 在今日资源短缺时显示红色 `今日/标准`，例如 `4/5`；无短缺时仍显示 `5台` 或 `5人`。
- `标准班次产能` 在今日产能短缺时显示红色 `今日/标准`，例如 `400/500`；无短缺时仍显示标准产能。
- 保留 `标准资源` 点击入口，短缺比值显示不影响打开设备/人工资源详情弹框。
- 未修改后端接口、今日可用计算、维修判断或产能算法。

## 最终验证

- RED: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> FAIL，标准资源列缺少今日资源短缺红色样式判断。
- GREEN: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-machinery-column.spec.js` -> PASS。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin` 打开路线 `900026` 验证。

## Cleanup Keep

- `doc/tasks/20260609-route-process-shortage-inline-ratio/frontend-feature-evidence.md`
