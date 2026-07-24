BDD: image import starts from the local MES page -> Given the operator opens the electronic batch-record page, When image import is supported by the backend, Then the page should expose a local `导入图片报表` action instead of requiring changes inside Jimu's embedded toolbar.

BDD: visible page entry is distinct from the Word import -> Given the page already supports Word report import, When the new feature is added, Then the toolbar should render a separate image-import button and keep the existing refresh and search controls intact.

BDD: blocking backend errors are surfaced directly -> Given backend prerequisite, recognition, or validation errors occur, When the frontend handles the failed request, Then the page should show the blocking message and should not silently switch to another import path.

RED: `git show HEAD:src/views/mes/pro/batchrecordtemplate/index.vue | Select-String -Pattern '导入图片报表'` -> no output, confirming the previously committed page source had no image-import entry.
RED: `git show HEAD:src/api/mes/pro/batchrecordreport/index.ts | Select-String -Pattern 'importImage'` -> no output, confirming the previously committed API file had no image-import method.
GREEN: `pnpm exec eslint src/api/mes/pro/batchrecordreport/index.ts src/views/mes/pro/batchrecordtemplate/index.vue` -> PASS.
GREEN: Playwright real-page verification on `http://127.0.0.1:8081/mes/pro/batch-record-template` -> PASS, after a real login the page visibly rendered `导入 Word 报表`, `导入图片报表`, `刷新`, and the search input `按报表名称或编码搜索`.
RED: follow-up backend live test with the exact screenshot file `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` after rebuilding and restarting the backend -> FAIL, the backend recognition request still timed out, so frontend end-to-end success cannot yet be claimed for this specific image.
