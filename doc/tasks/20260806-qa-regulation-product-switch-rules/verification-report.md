# Verification Report

## Summary

QA 规程检验规则产品切换回归已修复。未配置规则的其它产品不再继承按压式球囊扩充压力泵的检验类型、规则标签或 5% 巡检预览；保存门禁会阻止没有检验规则的产品提交 QA 规程。

## Commands

- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS。
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/bug-regression-evidence.md` -> PASS（cleanup 前已执行）。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/frontend-feature-evidence.md` -> PASS（cleanup 前已执行）。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs doc/tasks/20260806-qa-regulation-product-switch-rules/task.md doc/tasks/20260806-qa-regulation-product-switch-rules/execution-log.md` -> PASS，只有 Git CRLF 工作区提示。
- 合并最新 `origin/int_main` 后复跑 `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`、`node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`、`node tests/e2e/qa-regulation-final-applicability-static.spec.cjs`、`node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs`、`pnpm ts:check` -> PASS。

## Scope Checked

- 产品级草稿 key：仍使用正式 `productMasterId`。
- 空产品规则：`createEmptyQaInspectionTypeRules()` 返回空数组。
- 压力泵规则：只由 `createPressurePumpQaInspectionTypeRules()` 显式构建。
- 页面展示：检验规则标签和巡检预览均来自当前产品规则数组。
- 保存门禁：没有检验规则时阻止保存并提示。

## Blockers

Worktree/slot 删除收尾阻塞：主工作区 `E:\IntRuoyi` 存在无关脏改动，`task-closeout-cleanup` auto 模式不能安全执行 ff-only 合并和 worktree 删除。本次已完成实现、验证、临时 evidence 清理、实现提交与分支同步；worktree `D:\IntRuoyiWorktree\qa-regulation-product-switch-fix` 和 slot 3 暂保留。
