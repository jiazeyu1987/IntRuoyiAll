# Bug Regression Evidence

## Bug Summary

The DCC controlled-file upload page renders the DHF/DMR product-code helper as red error text whenever the selected file category requires a DCC project code, even after a DCC project has been selected and its project code has been automatically populated into the read-only product-code field.

## Expected Behavior

- Missing project code: show a red blocking prompt.
- Bound project code: show a non-error confirmation such as `已自动绑定 DCC 项目代码：IDI`.

## Reproduction Command Or Path

- Static source contract: `node tests/e2e/dcc-upload-project-code-hint-static.spec.js`

## Root Cause

- Pending.

## Regression Test

- Pending.

## RED

- Pending.

## GREEN

- Pending.

## Risk And Regression Scope

- DCC controlled-file upload page only. Submit payload and backend binding are out of scope and must remain unchanged.

## Blockers And Follow-Up

- None currently.

