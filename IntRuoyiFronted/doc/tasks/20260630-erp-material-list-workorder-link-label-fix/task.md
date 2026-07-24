# 任务：生产用料清单主表生产工单列名与跳转修正（前端）

- Task ID: `20260630-erp-material-list-workorder-link-label-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

将生产用料清单主表中的“对应生产订单”列改为“生产工单”，并让汇总行中唯一对应工单可点击跳转到生产工单页对应条目。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成双向展示与明细跳转，本次在其基础上补主表列名和主表点击入口。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Vue/TS/Markdown 统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 主表保持链接式关键标识展示，沿用现有紧凑表格样式。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。仅在现有后端汇总字段与现有工单页面路由能力内完成点击跳转。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 生产用料清单主表显示生产工单链接 -> Given 某生产用料清单汇总行只对应一个生产工单 / When 打开生产用料清单主表 / Then 该列标题显示为生产工单，行内工单号可点击并跳到生产工单页对应条目。`
- `BDD: 多工单或无工单时不伪造精确跳转 -> Given 某汇总行对应多个工单或没有工单 / When 打开生产用料清单主表 / Then 页面显示摘要或无，但不伪造指向错误条目的 openId 跳转。`

## Milestones

1. M1：建立前端任务文档并锁定改动边界。`completed`
2. M2：补 RED 静态合同。`completed`
3. M3：实现主表列名与点击跳转。`completed`
4. M4：完成定向验证并回填证据。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-material-list-workorder-link-label-fix\frontend-feature-evidence.md`

## Completed Work

- 将生产用料清单主表“对应生产订单”列改为“生产工单”。
- 新增 `handleOpenGroupWorkOrder`，让唯一对应工单的汇总行可直接跳到生产工单页。
- 保持明细弹窗既有 `handleOpenWorkOrder` 不变；多工单摘要和无关联场景不渲染误导性主表链接。

## Final Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-material-list-workorder-link-label-fix\frontend-feature-evidence.md` -> PASS

## Current Blockers

- 无。
