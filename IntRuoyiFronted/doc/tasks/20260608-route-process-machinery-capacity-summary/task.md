# 工序设备列表产能汇总展示

## 任务目标

调整 MES 工艺路线详情中“组成工序”的设备列表弹窗：行内展示单台产能/h 与单台产能/班次；工序总产能/h 和总产能/班次放在弹窗底部汇总；1 班次固定按 10.5 小时计算并在底部显示。

## 前置任务状态

- 已检查最近同主题前端任务 `20260608-route-process-machinery-capacity-list`：状态为已完成。
- 当前前端工作区开始时为干净状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少产能仍显示 `未配置`，不使用其他产能来源兜底。
- `是否从根因和长期维护角度解决`：是。复用后端已返回的设备+工序单台小时产能，在前端按班次小时与数量汇总展示。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 行内显示单台班次产能 -> Given 设备列表中某设备有单台小时产能 / When 用户打开设备列表弹窗 / Then 行内显示 `单台产能/h` 和 `单台产能/班次`。
- BDD: 底部显示工序总产能 -> Given 工序设备列表包含多台设备 / When 用户打开设备列表弹窗 / Then 底部显示所有设备按数量汇总后的 `总产能/h` 和 `总产能/班次`。
- BDD: 班次小时说明可见 -> Given 班次产能按固定时长计算 / When 用户查看设备列表弹窗 / Then 底部显示 `1班次=10.5小时`。

## 里程碑

- [x] M1：创建任务文档，记录 BDD 与设计约束。
- [x] M2：新增 RED 静态契约测试。
- [x] M3：调整设备列表弹窗列、底部汇总与班次计算。
- [x] M4：运行静态测试、类型检查和真实页面只读验证。
- [x] M5：更新执行证据，运行 task-closeout-cleanup 预览并提交。

## 预期验证

- `node tests/e2e/mes-pro-route-process-machinery-capacity-summary.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 登录本机 `芋道源码/admin`，打开 `/mes/pro/route?openId=900026`，验证 `B010` 设备列表弹窗展示单台班次产能、底部总产能和班次说明。

## 当前状态

已完成：设备列表弹窗展示已调整，静态契约、类型检查、真实页面只读验证、证据校验和 task-closeout-cleanup 预览均通过，等待提交。

## 验证结果

- RED：`node tests\e2e\mes-pro-route-process-machinery-capacity-summary.spec.js` -> FAIL，缺少班次小时常量、单台班次产能列和底部汇总。
- GREEN：`node tests\e2e\mes-pro-route-process-machinery-capacity-summary.spec.js` -> PASS。
- GREEN：`node --check tests\e2e\mes-pro-route-process-machinery-capacity-summary-real-flow.e2e.js` -> PASS。
- GREEN：`node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN：`node tests\e2e\mes-pro-route-process-machinery-capacity-summary-real-flow.e2e.js` -> PASS，本机 `芋道源码/admin` 只读路径下 `B010` 设备列表显示 `单台产能/班次`，底部显示 `1班次=10.5小时`、`总产能/h：47.61905`、`总产能/班次：500`。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260608-route-process-machinery-capacity-summary\frontend-feature-evidence.md` -> PASS。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-machinery-capacity-summary --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --worktree-closeout off --json` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- `doc/tasks/20260608-route-process-machinery-capacity-summary/frontend-feature-evidence.md`
