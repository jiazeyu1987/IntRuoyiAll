# Verification Report

## Scope

- 修复生产组长“活跃订单分配”下拉候选重叠。
- 保留活跃订单分配提交身份字段 `activeOrderId`，不使用内部 ID 作为可见订单编号。
- 解释“未返回订单编号”来源为正式 `workOrderCode` 缺失暴露。

## Results

- PASS: `node tests/e2e/team-leader-active-order-option-label-static.spec.js`
- PASS: `node tests/e2e/team-leader-report-allocation-static.spec.cjs`
- PASS: `node tests/e2e/team-leader-workbench-static.spec.cjs`
- PASS: `pnpm ts:check`
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-active-order-option-label-static.spec.js doc/tasks/20260808-active-order-allocation-select-overlap`，只有 CRLF 工作区提示。
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-active-order-allocation-select-overlap\bug-regression-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260808-active-order-allocation-select-overlap\frontend-feature-evidence.md`

## Findings

- 下拉重叠根因是多行自定义候选内容仍使用 Element Plus 单行 `el-option` 高度。
- “未返回订单编号”不是新订单号，也不是前端随机文案；它表示活跃订单列表响应中的正式 `workOrderCode` 为空。按无 fallback 规则，前端不能用 `workOrderId` 或活跃订单 `id` 冒充订单编号。

## Current Status

completed：验证通过，cleanup preview/apply 已完成；未执行 Git commit/push。
