# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: hide the screenshot-marked taxonomy path helper and orange permission preflight alert from the readonly file-category area on the DCC controlled-file upload page.
- Non-goal: do not change category selection, `canUpload` projection, form validation, upload preview, submit behavior, APIs, or backend permission enforcement.

## Requirements And Acceptance

- A1: the readonly file-category value remains visible after a taxonomy leaf is selected.
- A2: the readonly file-category block does not render `自动取文件分类最后一级`.
- A3: the readonly file-category block does not render `categoryPermissionPreflightMessage` as an alert.
- A4: categories with `canUpload=false` remain filtered and stale selections remain blocked by form validation.

## UI Entry Points And Owned Files

- Route: `/dcc/controlled-file/upload`.
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`.
- Contracts: `IntRuoyiFronted/tests/e2e/dcc-upload-category-permission-static.spec.js` and `IntRuoyiFronted/tests/e2e/dcc-upload-category-leaf-real.e2e.js`.

## API Contracts And Data States

- No API contract changes.
- The category list continues to provide `canUpload`; the frontend continues filtering unavailable categories.
- Permission, missing taxonomy, invalid depth, duplicate binding, and missing directory states remain in existing validation/preflight computations; this task only removes two presentation nodes from the readonly category block.

## BDD Scenarios

- BDD: Given a taxonomy leaf is selected, when the readonly file category renders, then its value is visible without the taxonomy path helper or orange permission alert.
- BDD: Given a bound category has no upload permission, when candidates and validation are evaluated, then the category is still excluded and stale selection validation still fails.

## RED / GREEN

- RED: `node tests/e2e/dcc-upload-category-permission-static.spec.js` failed on the new negative assertion because the readonly category block still rendered `自动取文件分类最后一级`.
- GREEN: the same command passed after removing the path helper and alert from only the readonly category block.
- Regression: taxonomy binding static contract, project taxonomy revision static contract, real E2E syntax check, and `pnpm ts:check` all passed.

## Responsive Accessibility Loading Empty Error Permission Checks

- Responsive: removing the two block-level helpers must not change the stable readonly field width.
- Accessibility: the visible `文件类别` label and readonly value remain.
- Loading/empty/error: no request or state-handling changes.
- Permission: `canUpload` filtering, validation, and backend enforcement remain unchanged.

## E2E Or Component Verification Path

- Focused and adjacent static contracts passed, followed by `pnpm ts:check` with exit code 0.
- Real Playwright path passed at `http://127.0.0.1:8081/dcc/controlled-file/upload` using the configured local tenant/account and a read-only candidate category.
- The readonly category value `技术调研报告` remained visible; the target helper and category alert were absent.
- Evidence: `output/playwright/20260807-dcc-upload-hide-category-permission-hint/dcc-upload-category-leaf-real-evidence.json` and `.png` screenshot.
- Read-only boundary: `writeRequests=[]`, `targetNetworkFailures=[]`, `consoleErrors=[]`, and `pageErrors=[]`.

## Blockers And Follow-Up Skills

- None.
