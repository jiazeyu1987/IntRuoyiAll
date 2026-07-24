# 排程明细任务表产品列拆分

## 任务目标

按照截图要求调整生产排程日历任务类明细弹框右侧任务表：隐藏红框中的 `待检`、`执行状态` 两列；将绿框中的合并产品列拆成 `产品编码` 和 `产品名称` 两列。仅修改前端展示，不修改接口、后端、真实数据结构或数据来源。

## 里程碑

1. [x] 建立任务文档、经验门禁和 BDD/TDD 基线。
2. [x] 补充 RED 静态契约，锁定隐藏列与产品拆列。
3. [x] 实现右侧任务明细表列调整。
4. [x] 运行目标验证并记录证据。
5. [x] 完成任务文档收尾并提交本次改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js`
- `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish`
- `pnpm.cmd run ts:check:schedule`

## BDD 场景

BDD: 隐藏红框任务列 -> Given 用户打开生产排程日历任务类明细弹框 / When 查看右侧所选工单任务表 / Then 不再展示 `待检` 和 `执行状态` 列。

BDD: 产品拆分为编码和名称 -> Given 用户打开生产排程日历任务类明细弹框 / When 查看右侧所选工单任务表 / Then 产品信息拆分显示为 `产品编码` 与 `产品名称` 两列，不再合并为 `产品` 一列。

BDD: 工单分组保持不变 -> Given 用户切换左侧工单 / When 右侧任务表刷新 / Then 仍只展示当前工单对应的工序级任务行，工单详情和异常详情不受影响。

## 经验门禁

- PowerShell：已读取 `docs/powershell-memory.md`，命令显式设置 UTF-8，不使用 `&&`，复杂中文写入使用 `apply_patch`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次只调整排程明细表列，不做无关视觉重构。
- BDD/TDD：先新增失败静态契约，再最小实现页面列调整，最后回归验证。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本次仅调整已有真实字段展示，不新增默认成功、mock 或错误吞噬。
- 是否从根因和长期维护角度解决：是。直接在任务类明细表列定义中隐藏无用状态列，并按真实字段拆分产品编码和名称。
- 是否存在临时补丁或绕过：否。

## 当前状态

completed

## Current Status

completed

## 完成结果

- 生产排程日历任务类明细弹框右侧任务表已隐藏 `待检` 和 `执行状态` 两列。
- 原合并 `产品` 列已拆分为 `产品编码` 和 `产品名称` 两列，分别读取真实 `itemCode` 与 `itemName`。
- 工单分组弹框、工单详情分支和异常详情分支保持不变。

## 最终验证

- `node tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> GREEN PASS。
- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> GREEN PASS。
- `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish` -> GREEN PASS。
- `pnpm.cmd run ts:check:schedule` -> GREEN PASS。
