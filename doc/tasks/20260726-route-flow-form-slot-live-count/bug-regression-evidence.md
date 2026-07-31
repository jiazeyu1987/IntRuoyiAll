# Bug Regression Evidence

## Bug Summary

工艺路线流转关系图选中“表单槽位”时，右侧已有 1 个动态表单，用户新增并选择第二个表单后，节点右上角数量徽标仍显示 `1`，应实时显示 `2`。

## Expected Behavior

右侧“新增表单”创建的是动态表单槽位，不应继续使用 `MAIN` 批记录槽位；选择模板后，`getRouteNodeAdditionalFormCount` 应立即统计到新增项。

## Reproduction

- RED: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> FAIL, expected reason: 新增动态表单没有非 `MAIN` 槽位默认值。

## Root Cause

`createEmptyRecordBinding()` 将新增本地绑定的 `formSlotType` 固定为 `MAIN`。节点徽标按设计只统计非 `MAIN` 的有效动态表单，因此新增并选择模板后仍被排除，数量保持旧值。

## Fix

新增 `ADDITIONAL_RECORD_BINDING_SLOT_TYPES` 和 `resolveNextAdditionalRecordBindingSlotType()`，新增动态表单默认选择下一个未使用的非 `MAIN` 槽位；已有槽位保持不变，避免覆盖已保存配置。

## Verification

- GREEN: `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> PASS。
- GREEN: `pnpm e2e:mes:route-flow-node-text-center:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Risk

低。修复只影响右侧“新增表单”创建本地空绑定的默认槽位，不改变已保存表单绑定、不改变后端接口，也不引入 fallback。

## Blockers And Follow-Up

- 无本任务阻塞。
