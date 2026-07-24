# 任务：排产工单主表长编码换行完整显示

- Task ID: `20260701-schedule-order-main-table-wrap-display`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修复 `排产工单` 主表与 `生成工单/待同步差异` 列表中长文本在列宽不足时被截断不完整的问题，使目标列允许换行并显示全量内容，同时保持表格整体密度和现有交互不变。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-zhaojie-feedback-attribution-success-permission-toast-fix\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成，不阻塞本次主表显示修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 仅调整主表局部列的显示策略，保持运维台式表格密度、色彩和整体结构不变。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。按列粒度开放换行，不靠 tooltip 兜底，不扩大到整表。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产工单主表长编码列在列宽不足时换行完整显示 -> Given 排产编码、工单编码或产品编号很长 / When 主表渲染 / Then 三列都允许换行并显示全量内容。`
- `BDD: 生成工单列表长文本单元在列宽不足时换行完整显示 -> Given 待同步差异列表中的工单编码、产品编号、产品名称、规格型号或不可排原因很长 / When 列表渲染 / Then 这些单元都允许换行并显示全量文本。`
- `BDD: 其它列表列保持原有密度 -> Given 仅修复目标文本列 / When 页面渲染 / Then 未命中的列仍保持既有 tooltip/单行策略。`

## Milestones

1. M1：建立任务台账并锁定主表与生成工单列表目标列。`completed`
2. M2：补 RED 静态回归。`completed`
3. M3：实现最小样式修复并跑到 GREEN。`completed`
4. M4：回填 evidence 与结果。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-main-table-wrap-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-admission-wrap-static.spec.js`

## Current Blockers

- 暂无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-main-table-wrap-static.spec.js` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-admission-wrap-static.spec.js` -> `PASS`
- 结论：
  - 主表 `排产编码 / 工单编码 / 产品编号` 已按列粒度开放换行并完整显示。
  - `生成工单/待同步差异` 列表中的 `工单编码 / 产品编号 / 产品名称 / 规格型号 / 不可排原因` 已统一开放换行显示。
  - 其他未命中列仍保持原有单行密度与 tooltip 策略。
