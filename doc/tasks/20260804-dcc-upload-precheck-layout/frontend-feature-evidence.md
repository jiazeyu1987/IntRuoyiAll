# Frontend Feature Evidence

## Feature Goal

Move the DCC upload pre-submit validation panel into the left-side form information column so long attachment previews in the right upload column do not push validation content far below the fold.

## Non-Goals

- Do not change upload API contracts.
- Do not change file preview behavior.
- Do not change validation rules, submit payload, permissions, or backend error handling.

## Requirements And Acceptance

- Acceptance: The pre-submit validation DOM belongs to `dcc-upload-left-column` after `dcc-upload-section-file`.
- Acceptance: The right/upload column keeps approval requirements, accepted file upload, PDF upload controls, and preview behavior.
- Acceptance: Submit action remains in the existing bottom action area.

## UI Entry Points

- Route/component: `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` (`DccControlledFileUpload`).
- Static contract: `IntRuoyiFronted/tests/e2e/dcc-upload-layout-static.spec.js`.

## API Contracts And Data States

- No API contract changes.
- Existing upload preview, drawing PDF, upload directory, category permission, current version, and submit functions are retained.
- Layout contract now asserts `handleProjectCodeChange`, `handleFileChange`, `handleDrawingPdfChange`, `uploadControlledFilePreview`, and `submitControlledFile` remain present.

## BDD Scenarios

- BDD: DCC upload preflight near form info -> Given the upload page can render a long attachment preview / When the workbench renders form, upload, and preflight sections / Then preflight appears in the left column after file info, not after the right preview stream.
- BDD: Submit capability preserved -> Given upload form prerequisites are complete / When the layout moves the preflight panel / Then submit, preflight cards, upload preview, and existing errors remain wired to current data and functions.

## RED

- RED: `pnpm e2e:dcc:upload-layout:static` -> FAIL before implementation because the page lacked `dcc-upload-left-column` and preflight was after the attachment section.

## GREEN

- GREEN: `pnpm e2e:dcc:upload-layout:static` -> PASS.

## Responsive Accessibility Loading Empty Error Permission Checks

- Responsive: CSS uses independent left/right column stacks and collapses to one column at `max-width: 1280px`; preflight cards collapse to one column at `max-width: 720px`.
- Accessibility: No control labels or button text changed.
- Loading/empty/error: Upload preview loading, preview error, category permission alerts, and current version lookup states are unchanged.
- Permission: Category upload permission preflight logic is unchanged.

## E2E Or Component Verification Path

- Static component contract passed.
- `pnpm ts:check` was attempted and is blocked by unrelated existing type mismatches outside the upload page.
- Real E2E evidence: `output/playwright/20260804-dcc-upload-precheck-layout/dcc-upload-precheck-layout-real-e2e.json`; screenshot: `output/playwright/20260804-dcc-upload-precheck-layout/dcc-upload-precheck-layout-real-e2e.png`.
- Real Playwright page validation was run against `http://127.0.0.1:8081/dcc/controlled-file/upload`; it passed with `dccWriteRequests=[]`, `dccBadResponses=[]`, `consoleErrors=[]`, and `pageErrors=[]`.

## Blockers And Follow-Up Skills

- Closeout/commit/push blocked by pre-existing dirty worktree and branch ahead state.
- Full TypeScript regression blocked by unrelated existing DCC/Workbench time-field typing errors.
