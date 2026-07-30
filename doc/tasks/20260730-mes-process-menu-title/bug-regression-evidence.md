# Bug Regression Evidence

## Bug Summary

- The production menu entry was displayed as `标准模板列表`, while the expected product term is `MES工序`.

## Expected Behavior

- Dynamic menu, search result and visible page navigation title display `MES工序`.
- The old title is absent.

## Reproduction

- Login as `芋道源码/admin`.
- Expand `MES 系统 > 生产管理`.
- Observe `/mes/pro/mes-process`, or search `mes工序`.

## Root Cause

- The page title and formal `system_menu.name` migration had previously been renamed to `标准模板列表`.
- The local runtime database still stored the old UTF-8 values for menu IDs 5718 and 5719.

## Regression Test

- Updated `IntRuoyiFronted/tests/e2e/mes-pro-mes-process-readonly-static.spec.js` to require `MES工序` and reject the old title/HEX.
- Added a task-owned real Playwright path covering visible menu, search, page load and read-only network behavior.

## RED

RED:

- Focused static contract failed at `页面标题必须显示为 MES工序`.

## GREEN

GREEN:

- Focused static contract passed after the page and SQL were updated.
- Real E2E passed with resource API code 0 and zero MES writes.

## Verification

- Static contract, adjacent tab contract, migration policy gate and real browser E2E all passed.

## Risk And Regression Scope

- Low risk: title and menu data only.
- Existing route, component, permission code and list API remain unchanged.

## Blockers

- None for the requested behavior.
