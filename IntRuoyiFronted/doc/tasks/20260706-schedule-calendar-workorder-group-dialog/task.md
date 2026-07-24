# 排程明细弹框按工单合并

## 任务目标

将生产排程日历里的 `任务详情 / 白班详情 / 夜班详情 / 锁定详情` 从按工序平铺的任务表，改为左侧按工单合并、右侧展示所选工单对应任务明细的弹框布局。仅调整前端展示，不修改接口、后端、真实数据结构或数据来源。

## 里程碑

1. [x] 建立任务文档、经验门禁和 BDD/TDD 基线。
2. [x] 补充 RED 静态契约，锁定按工单合并弹框结构。
3. [x] 实现日汇总任务类弹框的工单分组视图。
4. [x] 运行目标验证并记录证据。
5. [x] 完成任务文档收尾并提交本次改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js`
- `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js --format stylish`
- `pnpm.cmd run ts:check:schedule`

## BDD 场景

BDD: 任务类弹框按工单合并 -> Given 用户打开生产排程日历某天的任务详情、白班详情、夜班详情或锁定详情 / When 弹框渲染任务明细 / Then 左侧同一工单只显示一次，右侧展示当前选中工单对应的工序级任务行。

BDD: 点击工单切换明细 -> Given 弹框左侧存在多个工单 / When 用户点击其中一个工单 / Then 右侧表格只展示该工单对应的工序、产品、数量、已报工、待检、执行状态、锁定、排产冻结和产线。

BDD: 非任务类弹框保持原状 -> Given 用户打开工单详情或异常详情 / When 弹框渲染 / Then 原有工单聚合表和异常表保持独立，不被任务类工单分组视图替换。

## 经验门禁

- PowerShell：已读取 `docs/powershell-memory.md`，命令显式设置 UTF-8，不使用 `&&`，复杂中文写入优先使用 `apply_patch`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次使用白底、浅边框、紧凑表格、蓝色选中态，不引入无关视觉重构。
- BDD/TDD：先新增失败静态契约，再最小实现页面展示，最后回归验证。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本次只对已有真实数据做前端分组展示，不新增兜底成功或异常吞噬。
- 是否从根因和长期维护角度解决：是。将任务类明细弹框明确拆分为工单导航与任务明细两个职责区，避免同一工单在弹框内重复平铺。
- 是否存在临时补丁或绕过：否。

## 当前状态

completed

## Current Status

completed

## 完成结果

- 生产排程日历 `任务详情 / 白班详情 / 夜班详情 / 锁定详情` 已改为左侧按工单合并、右侧展示当前工单工序级任务明细。
- 左侧同一工单只展示一次，并显示任务数、总数量、白班、夜班、锁定摘要。
- 右侧保留原任务列：工序、产品、数量、已报工、待检、执行状态、锁定、排产冻结、产线。
- 工单产线分析入口已迁移到左侧工单卡片的工单编码链接；工单详情和异常详情保持原分支。

## 最终验证

- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> GREEN PASS。
- `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish` -> GREEN PASS。
- `pnpm.cmd run ts:check:schedule` -> GREEN PASS。

## Cleanup Preview

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-calendar-workorder-group-dialog --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，删除本次额外 `frontend-feature-evidence.md`。

## Cleanup Apply

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-calendar-workorder-group-dialog --mode apply` -> PASS，已删除本次额外 `frontend-feature-evidence.md`，保留任务核心记录。
