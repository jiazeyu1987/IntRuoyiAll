# 工艺路线组成工序隐藏等待时间和颜色列

## 任务目标

在工艺路线弹框“组成工序”主表格中不再显示 `等待时间` 和 `甘特图颜色` 两列，进一步降低列表密度。编辑工序弹框中的 `等待时间` 和 `甘特图颜色` 字段继续保留，避免影响既有工艺路线配置。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-process-shortage-inline-ratio/task.md`。
- 检查结果：该任务已标记 `completed`，本任务继续在组成工序表格上做展示简化。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只调整主表格展示列，不新增兜底逻辑。
- `是否从根因和长期维护角度解决`：是。将排产主视图中低频辅助字段移出主表，保留编辑入口，降低表格拥挤。
- `是否存在临时补丁或绕过`：否。不删除后端字段、不删除编辑表单字段、不改变保存逻辑。

## BDD 场景

- BDD: 组成工序主表不显示等待时间和颜色列 -> Given 用户打开工艺路线详情或编辑弹框 / When 查看组成工序表格 / Then 表头不包含 `等待时间` 和 `甘特图颜色`。
- BDD: 编辑工序仍可维护等待时间和颜色 -> Given 用户点击组成工序行的编辑 / When 编辑工序弹框打开 / Then 弹框仍保留 `等待时间` 和 `甘特图颜色` 字段。

## 里程碑

- [x] M1：添加静态契约 RED 测试，确认主表隐藏两列且编辑表单保留字段。
- [x] M2：调整 `RouteProcessList.vue` 删除主表格两列。
- [x] M3：运行静态测试、类型检查和真实 UI E2E。

## 预期验证

- `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js`
- `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js`

## 当前状态

completed

## 完成记录

- 已删除组成工序主表格中的 `等待时间`、`甘特图颜色` 两列。
- 编辑工序弹框中的 `等待时间`、`甘特图颜色` 字段仍保留，可继续维护。
- 未修改后端接口、数据保存或排产资源计算。

## 最终验证

- RED: `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js` -> FAIL，主表仍显示 `等待时间` 列。
- GREEN: `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> PASS。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS。

## Cleanup Keep

- `doc/tasks/20260609-route-process-hide-wait-color-columns/frontend-feature-evidence.md`
