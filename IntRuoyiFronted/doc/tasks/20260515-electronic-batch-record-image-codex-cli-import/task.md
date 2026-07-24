# Task: Electronic Batch Record Image Import With Codex CLI Correction

## Goal

Implement the frontend portion of image-based electronic batch-record report import so operators can start the image import from the local MES page and see a dedicated `导入图片报表` entry without entering Jimu's embedded toolbar.

## Scope

- Check the latest frontend task status before starting this task.
- Create the task document and execution log before editing production code.
- After backend prerequisite verification passes, implement the minimal local-page image import entry and API wiring.
- Verify the visible page entry with a real Playwright browser path.
- Record any follow-up live-test blocker that prevents a full operator upload from succeeding.
- Do not redesign the page or inject controls into Jimu's internal iframe page.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-workorder-erp-billno-code/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this implementation task.

## Milestones

- [x] M1: Confirm the previous frontend task is completed and create this task document.
- [x] M2: Wait for backend prerequisite verification and record frontend BDD notes.
- [x] M3: Record RED evidence for the local-page image import entry.
- [x] M4: Implement the minimal frontend image-import and correction-result flow.
- [x] M5: Complete GREEN verification and create the scoped frontend commit.
- [x] M6: Record the follow-up live test result for the exact screenshot file.

## Expected Verification

- `pnpm exec eslint src/api/mes/pro/batchrecordreport/index.ts src/views/mes/pro/batchrecordtemplate/index.vue`
- Playwright real-page verification on `http://127.0.0.1:8081/mes/pro/batch-record-template`

## Current Status

Frontend entry is implemented and verified. The local MES page shows both `导入 Word 报表` and `导入图片报表`, and a real Playwright browser path verified the visible entry controls on `http://127.0.0.1:8081/mes/pro/batch-record-template`. A follow-up live backend test with the exact screenshot file still timed out, so this task remains functionally wired on the page but not yet end-to-end successful for that specific image.

## Blocker And Impact

- Blocker: after rebuilding and restarting the backend, the exact screenshot image test still exceeds the current structured-recognition time budget on the backend side.
- Impact: the frontend entry and API wiring are correct, but a real operator upload of this exact image cannot yet be called successful until the backend recognition runtime is tuned.

## Final Verification Result

- `git show HEAD:src/views/mes/pro/batchrecordtemplate/index.vue | Select-String -Pattern '导入图片报表'` -> RED baseline, no output, confirming the previously committed page source had no image-import entry.
- `git show HEAD:src/api/mes/pro/batchrecordreport/index.ts | Select-String -Pattern 'importImage'` -> RED baseline, no output, confirming the previously committed API file had no image-import method.
- `pnpm exec eslint src/api/mes/pro/batchrecordreport/index.ts src/views/mes/pro/batchrecordtemplate/index.vue` -> PASS.
- Playwright real browser path:
  - `playwright-cli open http://127.0.0.1:8081/login?redirect=%2Fmes%2Fpro%2Fbatch-record-template` -> PASS.
  - real login with tenant `芋道源码`, username `admin`, password `admin123` reached `http://127.0.0.1:8081/mes/pro/batch-record-template`.
  - page snapshot after login showed visible buttons `导入 Word 报表`, `导入图片报表`, and `刷新`, plus the search input `按报表名称或编码搜索`.
- Follow-up system test:
  - exact screenshot file used: `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png`
  - backend was rebuilt and restarted before the retest
  - result: the backend-side image recognition request still timed out, so no successful live page upload result can be claimed for this specific image yet.
