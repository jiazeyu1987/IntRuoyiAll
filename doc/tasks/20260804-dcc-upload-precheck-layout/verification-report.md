# Verification Report

## Summary

- Implementation: Moved `dcc-upload-preflight-panel` into the new left-side `dcc-upload-left-column` after `dcc-upload-section-file`.
- Right column: Kept `dcc-upload-section-approval` and `dcc-upload-section-attachment` in `dcc-upload-right-column` so long previews no longer push the preflight cards down.
- Behavior: Upload preview, PDF upload, submit, permission preflight, and validation functions were not changed.

## Commands

- RED: `pnpm e2e:dcc:upload-layout:static` -> FAIL, old layout had no `dcc-upload-left-column`.
- GREEN: `pnpm e2e:dcc:upload-layout:static` -> PASS.
- REGRESSION: `pnpm ts:check` -> FAIL due unrelated existing type mismatches outside `upload/index.vue`.
- CHECK: `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue IntRuoyiFronted/tests/e2e/dcc-upload-layout-static.spec.js doc/tasks/20260804-dcc-upload-precheck-layout` -> PASS with LF/CRLF warnings only.
- CHECK: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-dcc-upload-precheck-layout/frontend-feature-evidence.md` -> PASS.
- E2E PREFLIGHT: Login preflight to `/dcc/controlled-file/upload` -> PASS on `http://127.0.0.1:8081`, identity label `芋道源码/admin`, password redacted.
- E2E GREEN: Real Playwright layout validation -> PASS; evidence JSON at `output/playwright/20260804-dcc-upload-precheck-layout/dcc-upload-precheck-layout-real-e2e.json`; screenshot at `output/playwright/20260804-dcc-upload-precheck-layout/dcc-upload-precheck-layout-real-e2e.png`.
- E2E ASSERTIONS: Preflight panel is inside left column after file info; right column contains approval and attachment only; no DCC write requests, no DCC bad responses, no console errors, no page errors.

## Non-Task TypeScript Blocker

`pnpm ts:check` fails in existing DCC/Workbench files unrelated to this layout change:

- `src/views/dcc/controlled-file/browser/index.vue`: `publishedTime` number vs string summary source mismatch.
- `src/views/dcc/controlled-file/categories/components/UploadSizePolicyDialog.vue`: upload policy date fields number vs string save request mismatch.
- `src/views/dcc/controlled-file/training/mine/index.vue`: `acknowledgedAt` number vs string summary source mismatch.
- `src/views/dcc/controlled-file/workbench/presentation.ts`: `string | number` assigned to string.
- `src/views/Profile/components/ProfileWorkbench.vue`: number assigned to string.

## Closeout Blocker

The workspace was already dirty and `int_main` was ahead of `origin/int_main` before/after this task. No task commit or push was performed to avoid mixing unrelated existing work with this layout change.
