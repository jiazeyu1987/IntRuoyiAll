# 任务：删除运行控制台决策巡检健康卡片

## 任务目标

按用户要求从运行控制台前端删除 `决策向导`、`巡检报告`、`业务健康` 三个卡片。删除范围限定为前端卡片显示、组件文件、卡片专用状态、卡片专用交互和相关前端测试期望；保留后端/API 能力，因为探针、巡检接口、业务健康接口仍可能被其它运维流程或外部验证使用。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-remove-ops-cards/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台前端卡片删除、前端测试和任务证据。

## BDD 场景

- BDD: 三个诊断卡片不再渲染 -> Given 操作员进入运行控制台 / When 页面加载完成 / Then 页面不显示 `决策向导`、`巡检报告`、`业务健康` 三个卡片。
- BDD: 删除诊断卡片不破坏剩余运维功能 -> Given 操作员进入运行控制台 / When 页面加载 foolproof 数据 / Then 发布候选、探针状态、日志磁盘风险、事故闭环等剩余功能仍按原入口工作。
- BDD: 删除诊断卡片不隐藏错误 -> Given 运行控制台其它接口失败 / When 页面加载或操作 / Then 仍通过现有错误机制暴露失败，不新增 fallback 或静默成功。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 前端静态回归测试，锁定三个卡片删除契约。
- [x] M3：删除三个卡片、组件文件、卡片专用状态和过期测试期望。
- [x] M4：运行目标测试、相关回归、类型检查和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-remove-health-guide-cards-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-remove-ops-cards-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-foolproof-timeout-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。删除卡片不改变现有接口错误暴露方式。
- `是否从根因和长期维护角度解决`：是。删除卡片入口、组件文件、卡片专用状态和过期测试期望，保留仍被其它流程依赖的 API 能力。
- `是否存在临时补丁或绕过`：否。不新增隐藏入口或测试专用内容。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260604-runtime-control-remove-ops-cards/task.md` 状态为 `completed`。
- RED：`node tests/e2e/runtime-control-remove-health-guide-cards-static.spec.js` -> FAIL，原因：页面仍 import `OpsDecisionWizard`。
- GREEN：`node tests/e2e/runtime-control-remove-health-guide-cards-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-remove-ops-cards-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-foolproof-timeout-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/runtime-control-all-buttons-real.e2e.js`、`node --check tests/e2e/runtime-control-real-data-all-features.e2e.js`、`node --check tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS。
- REGRESSION：删除真实 E2E 中对已移除卡片 API 的页面等待后，`node tests/e2e/runtime-control-remove-health-guide-cards-static.spec.js`、`node tests/e2e/runtime-control-foolproof-static.spec.js`、上述三项 `node --check` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：frontend feature evidence validator -> PASS。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-remove-health-guide-cards --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-remove-health-guide-cards/frontend-feature-evidence.md`
