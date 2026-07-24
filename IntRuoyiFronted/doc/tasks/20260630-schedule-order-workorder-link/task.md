# 任务：排产工单工单编码支持跳转生产工单

- Task ID: `20260630-schedule-order-workorder-link`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

将排产工单列表中的“工单编码”改为可点击链接；点击后跳转到生产工单页，并自动按工单编码筛选且打开对应生产工单详情。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成，可继续本次排产工单跳转增强。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Vue/TS/Markdown 统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持列表主键使用链接式主键风格，不引入额外大按钮或卡片化改造。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。复用生产工单页既有 `code + openId` 路由承接能力，不新增旁路页面。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产工单工单编码可跳到生产工单详情 -> Given 用户在排产工单列表看到一条已关联生产工单的排产记录 / When 点击该行工单编码 / Then 页面跳转到生产工单页，并自动按该工单编码筛选且打开目标工单详情。`
- `BDD: 缺少工单编码时不渲染假链接 -> Given 某排产工单行没有有效工单编码 / When 页面渲染该列 / Then 页面只显示占位文本，不渲染可点击链接。`

## Milestones

1. M1：建立任务文档并锁定现有跳转承接方式。`completed`
2. M2：补 RED 静态回归测试。`completed`
3. M3：实现排产工单工单编码链接跳转。`completed`
4. M4：回归验证并补证据。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-workorder-link-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-schedule-order-workorder-link\frontend-feature-evidence.md`

## Final Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-workorder-link-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-schedule-order-workorder-link\frontend-feature-evidence.md` -> PASS

## Completion

- `completed`

## Current Blockers

- 无。
