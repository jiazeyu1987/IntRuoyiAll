# 任务：删除运行控制台三个运维卡片

## 任务目标

按用户要求从运行控制台前端删除 `站内信告警`、`责任人矩阵`、`备份演练` 三个卡片。删除范围限定为前端卡片显示、组件文件、卡片专用状态和卡片专用测试；保留责任人矩阵数据加载，因为高风险操作弹窗仍依赖该数据进行责任人门禁提示。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-rollback-target-ui/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台前端卡片删除、前端测试和任务证据。

## BDD 场景

- BDD: 三个运维卡片不再渲染 -> Given 操作员进入运行控制台 / When 页面加载完成 / Then 页面不显示 `站内信告警`、`责任人矩阵`、`备份演练` 三个卡片。
- BDD: 删除卡片不破坏责任人门禁 -> Given 操作员打开高风险操作弹窗 / When 前端需要展示责任人 / Then `ownerMatrix` 数据仍可加载并用于弹窗责任人提示。
- BDD: 删除卡片不隐藏错误 -> Given 运行控制台其它接口失败 / When 页面加载或操作 / Then 仍通过现有错误机制暴露失败，不新增 fallback 或静默成功。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 前端静态回归测试，锁定三个卡片删除契约。
- [x] M3：删除三个卡片、组件文件和卡片专用前端状态。
- [x] M4：运行目标测试、相关回归、类型检查和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-remove-ops-cards-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。删除卡片不改变现有接口错误暴露方式。
- `是否从根因和长期维护角度解决`：是。删除卡片入口、组件文件和卡片专用状态，保留仍被其它流程依赖的数据。
- `是否存在临时补丁或绕过`：否。不新增隐藏入口或测试专用内容。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260604-runtime-control-rollback-target-ui/task.md` 状态为 `completed`。
- RED：`node tests/e2e/runtime-control-remove-ops-cards-static.spec.js` -> FAIL，原因：页面仍 import `OpsAlertInboxCard`。
- GREEN：`node tests/e2e/runtime-control-remove-ops-cards-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-foolproof-timeout-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：frontend feature evidence validator -> PASS。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-remove-ops-cards --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-remove-ops-cards/frontend-feature-evidence.md`
