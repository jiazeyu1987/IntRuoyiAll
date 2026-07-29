# Verification Report

## Scope

- 页面：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- 行为：原表模式顶部规则类型图例不显示。
- 保留：批记录表格、单元格规则类型角标、字段填写插槽和其他页面默认图例。
- 未引入 fallback、降级、吞异常或默认成功路径。

## Verification

- `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js` -> PASS
- `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- `git diff --check` -> PASS

## Concurrency Boundary

- `ExecutionPage.vue` 存在其他任务的并发改动。
- 本任务实现仅为原表模式组件调用增加 `:show-rule-legend="false"`。
- 提交必须使用选择性暂存，不得混入辅助卡片或网格布局改动。
