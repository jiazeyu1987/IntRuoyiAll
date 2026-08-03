# Bug Regression Evidence: Controlled Preview Requests Unrendered Auxiliary Data

## Bug Summary

- User-visible symptom: clicking preview for controlled file `2054545668044052098` shows `请求地址不存在:admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records`.
- Expected behavior: viewer preview mode loads the protected preview and basic file detail only; controlled print records are loaded only on non-viewer traceability/detail pages where the records section is rendered.
- Similar risk found during follow-up: viewer preview initialization also requested paper distribution records and the active process-print template even though those actions or dialogs are not rendered in viewer mode.

## Reproduction

- Path: DCC controlled file list -> click preview -> detail route with `viewer=1`.
- Static reproduction command: `node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js`.

## Root Cause

- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` initialized controlled print records whenever the current user had `dcc:controlled-file:print`, even in `viewer=1` preview mode where the records section is not rendered.
- `loadControlledPrintRecords()` also rethrew auxiliary records-load failures, causing `reloadAll()` to handle the error as a whole-page detail/preview load failure instead of keeping the failure visible in the print records section for non-viewer pages.
- The same `loadData()` initialization chain requested `getPaperDistributionRecords(controlledFileId.value)` and `getActiveApprovalPrintTemplate()` without a viewer-mode gate, so preview mode could fail on auxiliary data for UI controls that are not rendered.

## Regression Test

- RED: `node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> FAIL, expected failure because `shouldLoadControlledPrintRecords` did not contain `!viewerMode.value`.
- GREEN: `node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> PASS, static contract proves viewer preview skips controlled print records and auxiliary record errors are not rethrown to whole-page load.
- RED: `node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> FAIL, expected failure because viewer preview mode still called `getPaperDistributionRecords(controlledFileId.value)` and `getActiveApprovalPrintTemplate()`.
- GREEN: `node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> PASS, static contract proves viewer preview skips paper distribution records and process-print template data while non-viewer pages keep real requests.
- GREEN: `node doc\tasks\20260803-dcc-controlled-print-ux-optimization\dcc-controlled-print-ux-static.spec.cjs` -> PASS.
- REGRESSION: `node IntRuoyiFronted\tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> PASS.
- REGRESSION: `pnpm ts:check` in `IntRuoyiFronted` -> PASS.

## Verification

- The fix adds `!viewerMode.value` to the controlled print records load gate.
- The fix keeps non-viewer records API failures visible through `controlledPrintRecordsError` without converting them into a whole-page preview/detail load failure.
- The similar fix uses viewer-mode gated promises for paper distribution records and process-print template data, so preview mode resolves to empty local values without requesting unrendered auxiliary endpoints.
- No backend endpoint, permission, route, or data contract was changed.

## Risk And Scope

- Scope is limited to `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` and controlled print static contract.
- No backend endpoint contract change and no fallback behavior added.
- Source and static contract changes for the similar risk were included in concurrent baseline commit `03646727b`; no history rewrite was performed.

## Blockers

- None.
