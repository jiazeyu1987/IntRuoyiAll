# 工艺路线工序班次产能前端展示

## 任务目标

调整 MES 工艺路线详情“组成工序”表格：将 `准备时间` 列替换为 `班次产能` 列；设备列统一显示可点击的 `N 台`，无设备显示 `0 台`；点击 `0 台` 时展示该工序 5 人人工总班次产能，不展示设备明细表。

## 前置任务状态

- 已检查同主题前端任务 `20260608-route-process-machinery-capacity-summary`：状态为已完成。
- 当前前端工作区开始时为干净状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。前端只展示后端正式字段，不自行切换产能来源。
- `是否从根因和长期维护角度解决`：是。工序级产能由后端聚合，前端只负责显示和弹窗交互。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 表格显示工序班次产能 -> Given 工艺路线工序接口返回 `processShiftCapacityTotal` / When 用户查看组成工序表格 / Then 原 `准备时间` 列位置显示 `班次产能`。
- BDD: 无设备工序显示可点击 0 台 -> Given 工序没有设备 / When 用户查看设备列 / Then 显示可点击 `0 台` 而不是 `-`。
- BDD: 0 台弹窗显示人工总产能 -> Given 无设备工序有人工人数和班次产能 / When 用户点击 `0 台` / Then 弹窗显示 `人工人数：5人`、`总产能/班次` 和 `1班次=10.5小时`，不显示设备明细表。

## 里程碑

- [x] M1：创建任务文档，记录 BDD 与设计约束。
- [x] M2：新增前端 RED 静态契约测试。
- [x] M3：更新 API 类型、表格列和人工产能弹窗。
- [x] M4：运行静态测试、类型检查和真实页面只读验证。
- [x] M5：更新执行证据，运行 task-closeout-cleanup 预览并提交。

## 预期验证

- `node tests/e2e/mes-pro-route-process-shift-capacity-display.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `node tests/e2e/mes-pro-route-process-shift-capacity-display-real-flow.e2e.js`

## 当前状态

已完成：前端展示已实现，静态契约、类型检查、真实页面只读验证、证据校验和 task-closeout-cleanup 预览均通过，等待提交。

## 验证结果

- RED：`node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> FAIL，旧表格仍显示 `准备时间`。
- GREEN：`node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- GREEN：`node --check tests\e2e\mes-pro-route-process-shift-capacity-display-real-flow.e2e.js` -> PASS。
- GREEN：`node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN：`node tests\e2e\mes-pro-route-process-shift-capacity-display-real-flow.e2e.js` -> PASS；当前真实 `0 台` 人工工序为 `B080`，不是截图示例里的 `B020`。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260608-route-process-shift-capacity-display\frontend-feature-evidence.md` -> PASS。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-shift-capacity-display --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --worktree-closeout off --json` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- `doc/tasks/20260608-route-process-shift-capacity-display/frontend-feature-evidence.md`
