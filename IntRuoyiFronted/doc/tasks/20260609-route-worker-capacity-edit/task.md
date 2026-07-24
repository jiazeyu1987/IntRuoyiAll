# 工艺路线人工产能直接编辑

## 任务目标

在 MES 工艺路线“组成工序”里，点击人工工序的 `5人` 或行内 `编辑` 时直接显示“人工产能”编辑区，可以改人数、单人产能/h、班次小时，并自动显示班次总产能。保存时调用现有资源保存接口，底层仍写工作站与工作站人力资源表。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-process-hide-wait-color-columns/task.md`。
- 检查结果：该任务已标记 `completed`；本任务继续在同一组成工序表格上增加人工产能编辑能力。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少工作站时阻止保存并提示真实原因；保存失败交由接口错误暴露。
- `是否从根因和长期维护角度解决`：是。前端只提供入口和编辑表单，数据仍由后端资源接口写入工作站/人力资源表。
- `是否存在临时补丁或绕过`：否。不新增 mock、不新增前端本地持久化、不新增排产专用资源表。

## BDD 场景

- BDD: 点击人工资源打开编辑区 -> Given 用户在编辑工艺路线弹框查看人工工序 / When 点击 `5人` / Then 打开“人工产能”编辑区，显示人数、单人产能/h、班次小时和自动班次总产能。
- BDD: 点击人工工序编辑优先维护人工产能 -> Given 用户在组成工序人工行点击 `编辑` / When 行资源类型是人工 / Then 打开“人工产能”编辑区而不是普通工序表单。
- BDD: 保存人工产能调用资源接口 -> Given 用户修改人数、单人产能/h 和班次小时 / When 点击保存 / Then 前端调用 `/mes/pro/route-resource/save` 并刷新组成工序列表。

## 里程碑

- [x] M1：添加前端 RED 静态契约测试。
- [x] M2：实现人工产能编辑区、保存状态、自动计算和错误提示。
- [x] M3：运行静态测试、类型检查和真实页面只读验证。
- [x] M4：与后端任务完成联调记录。

## 预期验证

- `node tests\e2e\mes-route-process-worker-capacity-edit.spec.js`
- `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- 真实页面只读验证：登录 `芋道源码/admin`，打开 `/mes/pro/route?openId=900026`，确认人工工序点击入口和编辑区可见；不保存 admin 租户数据。

## 当前状态

completed

## 完成记录

- 人工工序的 `标准资源` 点击入口改为打开“人工产能”编辑区；设备工序仍打开设备列表。
- 人工工序行内 `编辑` 改为直接打开“人工产能”编辑区，非人工工序仍打开原工序编辑表单。
- 编辑区支持人数、单人产能/h、班次小时，并自动计算班次总产能。
- 保存调用 `/mes/pro/route-resource/save`，不在前端新增本地持久化数据源。
- 工作站编辑表单同步展示 `班次小时` 字段，避免字段只能从工艺路线入口维护。

## 最终验证

- RED: `node tests\e2e\mes-route-process-worker-capacity-edit.spec.js` -> FAIL，缺少人工产能编辑入口。
- GREEN: `node tests\e2e\mes-route-process-worker-capacity-edit.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js` -> PASS。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，只读验证 `芋道源码/admin` 路线 `900026`。
- GREEN: `MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-pro-route-process-shift-capacity-display-real-flow.e2e.js` -> PASS，只读验证 `芋道源码/admin` 路线 `900026`。

## Cleanup Keep

- `doc/tasks/20260609-route-worker-capacity-edit/frontend-feature-evidence.md`
