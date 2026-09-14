# Bug Regression Evidence

## Bug Summary

工艺路线流转关系图选中“表单槽位”时，右侧当前已有 1 个动态表单；用户点击“新增表单”后，右侧出现第二个动态表单行，但节点右上角数量仍显示 `1`。

## Expected Behavior

点击“新增表单”产生第二个非 `MAIN` 动态槽位行后，节点数量徽标应立即显示 `2`，不必等待模板选择完成。

## Reproduction

- RED: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> FAIL, expected reason: 数量 helper 仍使用 `isRecordBindingConfigured(binding)` 排除未选模板的新行。

## Root Cause

新增空绑定已经默认使用非 `MAIN` 动态槽位，但 `getRouteNodeAdditionalFormCount()` 仍要求 `formTemplateId > 0`。因此点击“新增表单”只新增了本地动态槽位行，未选择模板前不会被数量徽标统计。

## Regression Test

- 更新 `IntRuoyiFronted/tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js`，明确数量 helper 不得再依赖 `isRecordBindingConfigured` 或 `formTemplateId`。

## Fix

- 修改 `getRouteNodeAdditionalFormCount()`，按非 `MAIN` 动态槽位行计数，保持批记录 `MAIN` 排除口径不变。

## Verification

- GREEN: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> PASS。
- GREEN: `pnpm e2e:mes:route-flow-node-text-center:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Risk

低。修复只影响节点徽标与表单槽位绑定状态的本地计数口径；保存 payload 仍由既有 `buildFormBindingSaveRows()` 过滤未选择模板的空行。

## Blockers And Follow-Up

- 当前分支已有非本任务 ahead 提交；若需要推送，必须避免把并行任务状态误当成本任务成果。

