BDD: the report-management UI exposes the electronic batch record directory -> Given the operator opens report management, When the category tree loads, Then the `电子批记录` directory should be present as a selectable node.

BDD: generated reports are visible only after successful backend import -> Given backend image recognition successfully saves generated reports, When the operator opens the `电子批记录` directory, Then the generated report cards should be visible there.

BDD: the local upload entry must reach the real backend path -> Given the operator opens `http://127.0.0.1:8081/mes/pro/batch-record-template`, When the operator uses the `导入图片报表` button with the exact screenshot file, Then the real `/admin-api/mes/pro/batch-record-report/import-image` request should be issued before any directory visibility claim is made.

RED: `npx --yes --package @playwright/cli playwright-cli -s=ebrimport open http://127.0.0.1:8081/login?redirect=%2Fmes%2Fpro%2Fbatch-record-template --headed` + login + `click e519` + `upload C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` -> FAIL, Playwright request log shows `/admin-api/mes/pro/batch-record-report/import-image` was issued from the real page, but the backend save failed before any new report card could appear under `电子批记录`.

GREEN: image import failure feedback update -> PASS, the page now resolves upload errors into an action-specific `message.error(...)` branch for both the document import and image import handlers instead of silently swallowing failed requests.

GREEN: backend runtime on `http://127.0.0.1:48081` after rebuild -> PASS, the page backend was restarted on `output/runtime/backend-20260516-134436.jar` and responded with HTTP 200 before the interrupted Playwright rerun.

GREEN: exact screenshot visible in the local electronic batch-record page -> PASS, the real page request `/admin-api/mes/pro/batch-record-report/page?pageNo=1&pageSize=10&routeKey=LEGACY` returned `total: 16`, with the first row showing `ScreenShot_2026-05-15_170551_614.png` and report code `EBR_IMG_c48cdb7020e2_T01`.

GREEN: exact screenshot visible in the embedded Jimu `电子批记录` folder -> PASS, after the rebuilt backend runtime re-imported the same screenshot, the real Jimu request `/jmreport/query/report/folder?pageNo=1&pageSize=10&reportType=598eb5f05dac423a831cebb3c97c3fa7` returned `total: 1` and the record `EBR_IMG_c48cdb7020e2_T01`.
