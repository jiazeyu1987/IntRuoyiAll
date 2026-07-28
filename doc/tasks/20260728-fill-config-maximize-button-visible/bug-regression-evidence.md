# Bug Regression Evidence

## Bug Summary

截图中的批记录“填写配置”弹窗右上角只有关闭按钮，红框处缺少最大化/恢复按钮。

## Expected Behavior

“填写配置”弹窗默认最大化打开，并在右上角显示最大化/恢复按钮，可切换最大化和恢复。

## Reproduction

- Visual evidence: `C:\Users\BJB110\AppData\Local\Temp\codex-clipboard-e3e059af-388c-43c7-887d-d21b9082e0dc.png`
- Static reproduction command recorded in `execution-log.md` RED section.

## Root Cause

`BatchRecordCellRulesConfirmDialog.vue` explicitly passed `:fullscreen="false"` to the shared `Dialog` component. The shared `Dialog` only renders the maximize/restore icon when `fullscreen` is truthy, so the batch-record “填写配置” dialog could not display the button. The same dialog also lacked `:default-fullscreen="true"`, so it did not satisfy the default maximized requirement.

## Regression Test

Updated `tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js` to assert the batch-record fill-config dialog uses `:fullscreen="true"`, `:default-fullscreen="true"`, the current `title="填写配置"`, and the existing `保存填写配置` footer action. Updated `tests/e2e/edhr-visual-fill-config-static.spec.js` to keep the adjacent eDHR visual fill config contract aligned with the new maximize/default-fullscreen behavior.

## RED:

- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js` -> FAIL, expected reason: `BatchRecordCellRulesConfirmDialog.vue` used `:fullscreen="false"` and lacked `:default-fullscreen="true"`.

## GREEN:

- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js` -> PASS.
- `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS.
- `node tests/e2e/form-template-fill-config-static.spec.js` -> PASS.
- `node tests/e2e/form-center-static.spec.js` -> PASS.
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS.
- `node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS.
- `pnpm exec eslint --ext .vue src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue` -> PASS.
- `pnpm ts:check` -> PASS.

## Risk And Scope

Scope is limited to the batch-record fill-config dialog and static contracts. No backend/API/data behavior changes.

## Verification

- Evidence validator target: `doc/tasks/20260728-fill-config-maximize-button-visible/bug-regression-evidence.md`.
- Verification commands and PASS results are listed in `GREEN:`.

## Blockers And Follow-Up

- No blocker for the code fix or static/type verification.
- Real browser recheck was not executed in this pass; no API-only substitute was used.
