# 任务：修复运行控制台运维卡片不显示

## 任务目标

修复运行控制台中 `站内信告警`、`责任人矩阵`、`备份演练` 三个卡片不显示的问题，确保页面初始视口内能看到这三个卡片，并保留其真实接口刷新、确认、重发与备份点展示能力。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-restore-target-ui/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台卡片可见性、前端真实页面回归测试和任务证据。

## BDD 场景

- BDD: 运维辅助卡片必须显示 -> Given 操作员进入运行控制台 / When 状态矩阵加载后查看运维区域 / Then 页面必须显示 `站内信告警`、`责任人矩阵`、`备份演练` 三个卡片。
- BDD: 运维辅助卡片不依赖空数据隐藏 -> Given 告警、责任人或备份点接口返回空列表 / When 页面渲染运维区域 / Then 三个卡片仍显示空状态和刷新入口，不得整卡消失。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：复现卡片缺失并新增 RED 真实页面回归测试。
- [x] M3：定位根因并最小修复运行控制台卡片显示。
- [x] M4：运行目标测试、相关回归、类型检查和 bug evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED：`node tests/e2e/runtime-control-ops-cards-visible.e2e.js` 先失败，指出三个卡片标题位于初始视口之外。
- GREEN：同一命令通过。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` 通过。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` 通过。
- GREEN：`pnpm ts:check` 通过。
- GREEN：bug regression evidence validator 通过。
- GREEN：task-closeout-cleanup 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口失败仍按现有错误机制暴露，不把失败伪装成成功。
- `是否从根因和长期维护角度解决`：是。通过真实页面可见性契约和回归测试防止卡片再次从初始视口消失。
- `是否存在临时补丁或绕过`：否。不新增测试专用控件，不绕过真实接口。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260604-runtime-control-restore-target-ui/task.md` 状态为 `completed`。
- RED：`node tests/e2e/runtime-control-ops-cards-visible.e2e.js` -> FAIL，原因：`站内信告警` 标题位于初始 1366x900 视口之外，`y=906.5`。
- GREEN：`node tests/e2e/runtime-control-ops-cards-visible.e2e.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：bug regression evidence validator -> PASS。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-ops-cards-visible --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-ops-cards-visible/bug-regression-evidence.md`
