# Bug Regression Evidence

## Bug Summary

Several frontend pages reuse one page-level load error for primary queries, deferred auxiliary queries, row-level lazy loading, secondary panels, and user actions. This can show a full-page error while valid primary content remains visible.

## Expected Behavior

Primary list or detail requests own page-level load errors. Auxiliary, row, panel, and action requests must expose their real errors in their own visible scope without clearing or overwriting the primary error.

## Reproduction

`node tests/e2e/frontend-error-scope-hardening-static.spec.js`

## Root Cause

Multiple asynchronous operations write to the same page-level reactive error variable despite owning different UI regions and failure semantics.

## Regression Test

Added `IntRuoyiFronted/tests/e2e/frontend-error-scope-hardening-static.spec.js`, covering primary, deferred auxiliary, row-level lazy load, secondary panel, verification, export, and create-operation error ownership.

## RED

RED: `node tests/e2e/frontend-error-scope-hardening-static.spec.js` -> FAIL because `loadBatchDetailSecondaryData` still assigned the page-level `loadError`.

## GREEN

Pending.

## Risk And Regression Scope

- Preserve existing API calls, loading states, data clearing behavior, and real error text.
- Change only error ownership and presentation.
- Preserve concurrent changes outside this task.

## Blockers And Follow-up

- None at task start.
