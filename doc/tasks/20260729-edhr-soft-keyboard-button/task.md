# 20260729 eDHR 填写页软键盘按钮

## Task Goal

在 eDHR 批记录填写页左侧红框位置新增软键盘图标按钮，点击后在页面内弹出软键盘，不改动后端接口、不引入降级或 mock。

## Milestones

- [x] 建立任务目录并读取前端、E2E、PowerShell、收尾和经验门禁
- [x] 保存任务前脏工作区基线，避免本次改动混入既有变更
- [x] 新增聚焦静态合同，先 RED 锁定软键盘入口、弹层和按键行为
- [x] 实现 eDHR 填写页左侧软键盘按钮和页面内软键盘弹层
- [x] 运行目标静态合同、相邻合同和 TypeScript 检查
- [x] 更新任务文档、执行 cleanup preview/apply 并完成状态收尾

## Expected Verification

- `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-soft-keyboard-button/frontend-feature-evidence.md`
- `git diff --check`

## Current Status

completed

## Verification Evidence

- `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`：PASS。
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-soft-keyboard-button/frontend-feature-evidence.md`：PASS。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue IntRuoyiFronted/tests/e2e/edhr-soft-keyboard-button-static.spec.js doc/tasks/20260729-edhr-soft-keyboard-button`：PASS。
- `task_closeout.py --task-id 20260729-edhr-soft-keyboard-button --mode preview`：PASS，keep 4 个正式任务文件，delete/blocked/warnings 均为 none。
- `task_closeout.py --task-id 20260729-edhr-soft-keyboard-button --mode apply`：PASS，deleted_paths 为 none。
- 真实浏览器 E2E 未运行：本次为单组件左侧工具按钮和弹层交互，已用聚焦静态合同锁定 DOM、点击处理、焦点输入、事件派发和不传送出全屏容器；未启动或修改本地运行态。

## Cleanup Keep

- doc/tasks/20260729-edhr-soft-keyboard-button/frontend-feature-evidence.md

## Applicable Gates

- 前端功能必须按 BDD/TDD 记录 RED/GREEN，不允许通过 mock、默认成功或隐藏错误代替真实页面行为。
- eDHR 填写页相关改动需要保留原有填写模式、原表模式、保存草稿、提交执行和全屏按钮链路。
- 当前任务命中同文件并行改动和提交后残余改动门禁：本次提交必须只包含软键盘按钮/弹层/合同/任务文档。
- 当前工作区存在并发任务改动；同文件新增的 `sameRouteQueryId(task.routeProcessId, routeProcessId)` hunk 不属于本任务，提交时必须保持未暂存。
- 本次只在页面内增加软键盘辅助输入 UI，不修改 `assistRows`、批记录表单、表单槽位、工序开始或后端数据来源。
- 经验沉淀检查：`docs/powershell-memory.md` 已覆盖同文件并行改动选择性暂存，`docs/e2e-rules.md` 已覆盖 popover `:teleported="false"` 归属门禁；本次无新增长期经验文档。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接在 eDHR 填写页左侧工具栏提供正式软键盘入口和可测交互。
- `是否存在临时补丁或绕过`：否。
