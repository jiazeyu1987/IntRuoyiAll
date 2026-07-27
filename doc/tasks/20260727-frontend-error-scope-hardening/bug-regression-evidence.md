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

GREEN: `node tests/e2e/frontend-error-scope-hardening-static.spec.js` -> PASS; primary, auxiliary, row, panel, and action errors stay in their own scope.

Adjacent regression verification:

- `node tests/e2e/dcc-directory-lazy-loading-static.spec.js` -> PASS
- `node tests/e2e/edhr-delivery-static.spec.js` -> PASS
- `node tests/e2e/edhr-validation-package-static.spec.js` -> PASS
- `node tests/e2e/edhr-field-audit-toolbar-advanced-static.spec.js` -> PASS
- `node tests/e2e/edhr-field-audit-detail-evidence-collapse-static.spec.js` -> PASS
- `node tests/e2e/edhr-domain-trace-toolbar-advanced-static.spec.js` -> PASS
- `node tests/e2e/edhr-domain-trace-detail-evidence-collapse-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-detail-preview-scroll-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Verification

The focused contract and all listed adjacent contracts passed after the fix. `pnpm ts:check` also passed with exit code 0. The two batch-detail contract failures listed below are unrelated existing assertions.

## Risk And Regression Scope

- Preserve existing API calls, loading states, data clearing behavior, and real error text.
- Change only error ownership and presentation.
- Preserve concurrent changes outside this task.

## Blockers And Follow-up

- No blocker belongs to this task. The adjacent `edhr-batch-detail-review-fusion-static.spec.js` and `edhr-batch-detail-admin-takeover-static.spec.js` contracts still fail on their existing unrelated assertions and are not changed by this fix.
