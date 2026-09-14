# Bug Regression Evidence

## Bug Summary

“切换填写人”弹窗中详情接口已返回多个 `fillableUsers`，但非当前登录用户的候选项被按钮 `disabled`，导致截图中的另外 2 个填写人无法选择。

## Expected Behavior

具备金手指/代填权限的账号打开“切换填写人”弹窗时，当前工序其他可填写候选人不应被前端 `currentAssistUserId` 硬禁用；点击仍必须进入正式 `openTask` 流程，由后端继续 fail-fast 校验任务可见性和可打开状态。

## Reproduction Path

- UI path: eDHR 填写页 -> 填写辅助模式 -> 切换填写人。
- Static reproduction: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js`。

## Root Cause

`ExecutionPage.vue` 的 `isAssistFillerSwitchItemSelectable` 原实现为 `currentAssistUserId() === item.userId && isAssistBatchTaskOpenable(item.task)`，将候选项可选态绑定到当前登录用户本人，金手指/代填权限无法进入后端正式 `openTask` 校验。

## Regression Test

- Added `IntRuoyiFronted/tests/e2e/edhr-switch-filler-selectability-static.spec.js`。
- The contract asserts the selectable predicate keeps `isAssistBatchTaskOpenable(item.task)`, includes `hasGoldenFingerPermission.value`, and no longer uses the old current-user-only predicate.

## RED

- RED: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> FAIL, missing `hasGoldenFingerPermission.value` in the selectable predicate.

## GREEN

- GREEN: `node --check tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- REGRESSION: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。

## Verification

- Verification: `node --check tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- Verification: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- Verification: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。

## Risk And Regression Scope

- Scope is limited to eDHR auxiliary fill mode filler switching in `ExecutionPage.vue` and one focused static contract.
- The fix does not bypass backend authorization; `isAssistBatchTaskOpenable(item.task)` and `openEdhrBatchTask` remain the official gate.
- Existing wide assist-fill static contract still fails before this task's scope on an unrelated redbox cleanup assertion.

## Blockers And Follow-up

- No blocker for the focused fix.
- Existing `edhr-assist-fill-mode-static.spec.js` should be reconciled separately with the current redbox cleanup behavior.
