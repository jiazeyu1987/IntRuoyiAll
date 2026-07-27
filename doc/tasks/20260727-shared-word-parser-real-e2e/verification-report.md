# Verification Report

## Summary

- Overall status: BLOCKED after real E2E execution.
- Scope: real Word import validation for Form Center and MES batch record using files under `resource`.
- Environment: local frontend `http://localhost:8081`, local backend `http://127.0.0.1:48081`, tenant/user `测试租户/aoteman`.
- No fallback: no mock, no API-only import, no direct SQL write, no admin-baseline write path.

## Form Center

- Result: PASS.
- Page path: `/mdm/form-center/template`.
- Word file: `resource/过程检验记录.docx`.
- Upload endpoint: `/admin-api/form-center/templates/import-doc`.
- Response evidence: `templateId=30`, `versionNo=V1.0`, `importAction=CREATE`, `recognizedFields=56`, `warnings=0`.
- Screenshot: `doc/tasks/20260727-shared-word-parser-real-e2e/artifacts/form-center-20260727-shared-word-parser-real-e2e.png`.

## MES Batch Record

- Result: BLOCKED by real business state.
- Page path: `/mes/pro/batch-record-form-list`.
- Word file: `resource/批记录压力泵.doc`.
- Preflight endpoint: `/admin-api/mes/pro/batch-record-report/recognize-uploaded/preflight`.
- Preflight evidence: `allowedActions=[]`, `confirmDisabled=true`, `latestBatchRecordVersionNo=V3.0`, `latestBatchRecordVersionStatus=PENDING_APPROVAL`, `currentBatchRecordVersionNo=V1.0`, `currentBatchRecordHasMainReports=true`, `nextVersionNo=V4.0`.
- Interpretation: the real page and real preflight were reached, but current batch-record version governance prevents saving the import until the pending version is resolved.
- Screenshot: `doc/tasks/20260727-shared-word-parser-real-e2e/artifacts/mes-preflight-20260727-shared-word-parser-real-e2e.png`.

## Evidence

- Command: `node doc/tasks/20260727-shared-word-parser-real-e2e/shared-word-parser-real-e2e.js`.
- Structured evidence: `doc/tasks/20260727-shared-word-parser-real-e2e/real-e2e-evidence.json`.
- Script syntax check: `node --check doc/tasks/20260727-shared-word-parser-real-e2e/shared-word-parser-real-e2e.js` -> PASS.
- Durable E2E lesson: `docs/e2e-rules.md` now requires visible Element Plus upload-list/request-trigger assertions before waiting for upload responses.
