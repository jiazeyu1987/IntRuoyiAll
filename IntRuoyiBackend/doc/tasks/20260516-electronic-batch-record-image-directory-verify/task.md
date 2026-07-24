# Task: Electronic Batch Record Image Directory Verification

## Goal

Use the existing image-recognition import path to generate report records from the target batch-record image and verify that the generated reports are stored under the Jimu report category named `鐢靛瓙鎵硅褰昤.

## Scope

- Check the latest backend task status before starting this task.
- Create this task document before running import or verification steps.
- Verify the backend runtime is reachable with the latest image-import behavior.
- Run the real image import with the exact screenshot file.
- Verify category placement and visible report results.
- If the real import still fails, stop and record the exact blocker and impact instead of faking success.

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-electronic-batch-record-image-performance-optimization/task.md`
- Status before this task: completed.
- Impact: the import path and performance tuning are available for live verification.

## Milestones

- [x] M1: Confirm the previous backend task is completed and create this task document.
- [x] M2: Verify runtime prerequisites and record BDD notes.
- [x] M3: Execute the real image import against the target file.
- [x] M4: Verify the generated reports are placed under the `鐢靛瓙鎵硅褰昤 category.
- [x] M5: Record final evidence and blockers.

## Expected Verification

- authenticated `POST /admin-api/mes/pro/batch-record-report/import-image`
- authenticated report listing checks
- category visibility check against the local report-management UI or backend evidence

## Current Status

Completed. The exact screenshot file was re-imported against the rebuilt backend runtime on `output/runtime/backend-20260516-142707.jar`, and the generated report became visible in both the local electronic batch-record list and the Jimu `电子批记录` folder query.

## Final Verification Result

- Live UI/API evidence -> PASS, after resetting the existing Jimu row back to `template = NULL`, the rebuilt backend runtime re-imported the exact screenshot and restored visibility with `template = 0`; `GET /admin-api/mes/pro/batch-record-report/page?pageNo=1&pageSize=10&routeKey=LEGACY` returned the image report row, and `GET /jmreport/query/report/folder?pageNo=1&pageSize=10&reportType=598eb5f05dac423a831cebb3c97c3fa7` returned the same report in the `电子批记录` category.
- Targeted backend regressions -> PASS, `MesProBatchRecordReportServiceImplDbTest` and `MesProBatchRecordJimuReportGatewayImplTest` both passed after the name-length and `template = 0` persistence fixes.
- Runtime verification -> PASS, `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP 200 after restarting the backend on `output/runtime/backend-20260516-142707.jar`.
