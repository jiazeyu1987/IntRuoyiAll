BDD: frontend batch-record preview chain -> Given the backend worktree already exposes `/jmreport/view/...`, When the frontend worktree opens an electronic batch-record report, Then the iframe should use the preview path and the surrounding page copy should reflect preview mode rather than designer mode.

RED: `npm run ts:check` -> FAIL, the frontend repo still has a broad pre-existing baseline of missing auto-import globals (`ref`, `computed`, `onMounted`, etc.) unrelated to the batch-record preview-chain changes.

GREEN: `node tests/e2e/batch-record-preview-chain.spec.js` -> PASS
GREEN: `npm exec eslint src/views/mes/pro/batchrecordtemplate/DesignerWrapper.vue src/views/report/jmreport/index.vue tests/e2e/batch-record-preview-chain.spec.js` -> PASS
GREEN: frontend worktree dev server `npm run dev:batch-record-preview` -> PASS on `http://127.0.0.1:8082`
GREEN: real page verification on `http://127.0.0.1:8082/mes/pro/batch-record-template?mode=designer&reportId=85468f144bf54c198df8ae6cf8027b41` -> PASS, iframe `src` was `http://127.0.0.1:48082/jmreport/view/85468f144bf54c198df8ae6cf8027b41?tenantId=1&token=...`

Summary:
- Added a dedicated frontend worktree mode that points to backend `48082`.
- Kept the existing API contract, but made the page copy preview-aware when the backend returns `/jmreport/view/...`.
- Verified the real page chain is now `frontend worktree 8082 -> backend worktree 48082 -> jmreport/view/...`.
