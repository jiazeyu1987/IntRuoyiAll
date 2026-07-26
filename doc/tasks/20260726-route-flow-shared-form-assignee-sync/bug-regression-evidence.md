# Bug Regression Evidence

## Bug Summary

共享动态表单（未开启“工序独立”）的填写人配置只更新当前选中工序草稿，没有同步到同路线同表单的其他工序草稿。

## Expected Behavior

批次共享动态表单以同一路线同一 `formTemplateId` 为共享配置单元；更换填写人来源、填写人或恢复默认时，所有仍处于 `BATCH_SHARED` 的同表单工序草稿必须同步。`PROCESS` 工序独立绑定不参与同步。

## Reproduction

RED 静态合同将检查候选来源、候选 ID 和恢复默认处理器是否调用共享填写人同步 helper，并检查 helper 只同步同 `formTemplateId` 且 `BATCH_SHARED` 的绑定。

## Root Cause

填写人来源、填写人 ID 与恢复默认处理器只更新 `selectedRecordBindings` 当前工序草稿；已有路线内同步只覆盖“工序独立”实例范围，未覆盖批次共享表单的填写人 override。

## Regression Test

`IntRuoyiFronted/tests/e2e/mes-route-flow-shared-form-filler-sync-static.spec.js`

## RED

`node tests/e2e/mes-route-flow-shared-form-filler-sync-static.spec.js` -> FAIL, expected reason: `共享表单填写人必须具备路线内联动 helper: applyRecordBindingFillerOverride`。

RED: `node tests/e2e/mes-route-flow-shared-form-filler-sync-static.spec.js` -> FAIL, expected reason: `共享表单填写人必须具备路线内联动 helper: applyRecordBindingFillerOverride`。

## GREEN

`node tests/e2e/mes-route-flow-shared-form-filler-sync-static.spec.js` -> PASS。

`node tests/e2e/mes-route-flow-form-process-independent-switch-static.spec.js` -> PASS。

`node tests/e2e/mes-route-flow-shared-form-simplify-static.spec.js` -> PASS。

`pnpm ts:check` -> PASS。

GREEN: targeted static contracts and `pnpm ts:check` -> PASS。

## Verification

- `node tests/e2e/mes-route-flow-shared-form-filler-sync-static.spec.js` -> PASS
- `node tests/e2e/mes-route-flow-form-process-independent-switch-static.spec.js` -> PASS
- `node tests/e2e/mes-route-flow-shared-form-simplify-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Blockers

当前工作区存在大量任务外未提交改动，且目标组件文件已有其他任务改动；提交/推送需先处理脏工作区基线与同文件选择性暂存。

## Risk And Scope

范围限定在工艺路线流程图前端动态表单绑定草稿同步，不修改后端接口契约、不引入 fallback。
