# Task: Electronic Batch Record Image Directory Verification

## Goal

Verify from the local frontend that image-recognition-generated reports are visible under the `电子批记录` directory in the embedded report-management UI.

## Scope

- Check the latest frontend task status before starting this task.
- Create this task document before UI verification.
- Use the local frontend route and a real login path.
- Verify visibility only after the backend import has either succeeded or explicitly failed.
- Do not change frontend code in this task unless a verified visibility defect is discovered.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-electronic-batch-record-image-codex-cli-import/task.md`
- Status before this task: completed.
- Impact: the local page already exposes the image import entry, so this task can focus on verification.

## Milestones

- [x] M1: Confirm the previous frontend task is completed and create this task document.
- [x] M2: Wait for backend import result and record BDD notes.
- [x] M3: Verify the `电子批记录` directory visibility in the real UI.
- [x] M4: Record final evidence and blockers.

## Expected Verification

- real login path on `http://127.0.0.1:8081`
- real report-management route checks for the `电子批记录` directory contents

## Current Status

Completed. The exact screenshot file now results in a visible image-generated report row in the local electronic batch-record page, and the same report is visible through the embedded Jimu `电子批记录` folder path after the rebuilt backend runtime re-import.

## Final Verification Result

- Local electronic batch-record page -> PASS, the real page request `/admin-api/mes/pro/batch-record-report/page?pageNo=1&pageSize=10&routeKey=LEGACY` returned `total: 16` and the first row for `ScreenShot_2026-05-15_170551_614.png` with code `EBR_IMG_c48cdb7020e2_T01`.
- Embedded Jimu folder path -> PASS, the real Jimu request `/jmreport/query/report/folder?pageNo=1&pageSize=10&reportType=598eb5f05dac423a831cebb3c97c3fa7` returned `total: 1` with the same image-generated report under the `电子批记录` category.
- Frontend failure feedback -> PASS, upload failures are no longer swallowed silently; the page resolves them into action-specific `message.error(...)` messages.
