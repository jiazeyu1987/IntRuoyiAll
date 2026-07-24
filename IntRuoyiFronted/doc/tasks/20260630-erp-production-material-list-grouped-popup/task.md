# 任务：ERP 生产用料清单单据汇总与明细弹窗（前端）

- Task ID: `20260630-erp-production-material-list-grouped-popup`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

把 ERP 生产用料清单页面从“明细行分页”改为“按单据汇总分页”，主表一张单据一行，点击单据号后弹出该单据完整子项明细表。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-scheduler-material-analysis-trace\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成；本次进入新的前端页面交付任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Vue/TS/Markdown 与执行日志统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 页面保持 IntPP 生产订单列表式紧凑运营风格。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实只读 E2E 前先跑官方登录预检。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；前端直接切到正式分组接口与明细接口，不在页面端拼装假分组。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 主表按单据汇总展示 -> Given 进入 ERP 生产用料清单页面 / When 查询分组列表 / Then 主表每个 sourceBillNo 只显示一行。`
- `BDD: 点击单据号查看整单子项 -> Given 主表存在某个单据号 / When 点击单据号 / Then 弹窗展示该单据全部子项明细。`

## Milestones

1. M1：建立前端任务文档并锁定页面边界。`completed`
2. M2：补 RED 静态合同。`completed`
3. M3：实现主表分组视图与明细弹窗。`completed`
4. M4：完成静态与只读验证并回填证据。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-readonly.e2e.js`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-readonly.e2e.js` -> PASS

## Current Blockers

- `pnpm ts:check` 仍受仓库既有 eDHR 类型错误阻塞，与本任务无关。
