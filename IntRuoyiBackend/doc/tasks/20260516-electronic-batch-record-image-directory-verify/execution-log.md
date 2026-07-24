BDD: recognized image reports belong to the electronic batch record category -> Given the backend successfully recognizes the target batch-record image, When the generated reports are saved into Jimu report metadata, Then they should use the category created by `ensureElectronicBatchRecordCategoryId()` so they appear under `电子批记录`.

BDD: failed imports do not pretend success -> Given the target image import still times out or otherwise fails, When verification runs, Then the task should stop with explicit failure evidence and should not claim the reports exist in the directory.

BDD: image recognition must survive the Jimu save step -> Given the real UI import uses the exact screenshot file, When Codex CLI returns structured recognition output, Then the backend must still fit the generated report metadata into the current Jimu schema so the report can be inserted and listed.

RED: `npx --yes --package @playwright/cli playwright-cli -s=ebrimport open http://127.0.0.1:8081/login?redirect=%2Fmes%2Fpro%2Fbatch-record-template --headed` + login + `click e519` + `upload C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` -> FAIL, backend log `output/runtime/backend-20260516-125222.out.log` shows `Batch record image recognition process finished ... exitCode=0` followed by `PreparedStatementCallback ... Data too long for column 'name' at row 1`, so the recognized report never reaches the `电子批记录` directory.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 10 tests passed, including the new regression that captures the generated Jimu save request and verifies image report names are trimmed to the live `jimu_report.name` limit before persistence.

GREEN: backend runtime restart on `output/runtime/backend-20260516-134436.jar` -> PASS, after rebuilding `yudao-server.jar` and restarting the local 48081 backend, `GET http://127.0.0.1:48081/admin-api/system/auth/get-permission-info` returned HTTP 200.

BDD: Jimu folder visibility requires persisted template zero -> Given the live Jimu folder query filters report rows by `jimu_report.template = 0`, When the MES gateway inserts or updates an image-generated Jimu report, Then it must persist `template = 0` explicitly so the report stays visible through the Jimu folder path.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the new gateway regression expected persisted `template = 0`, but the gateway did not explicitly set `JimuReport.template` on save/update.

GREEN: live review-loop evidence for `jimu_report.template = 0` -> PASS, the confirmed runtime evidence for this round shows that setting `template = 0` makes the generated report visible in the Jimu folder query.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 4 tests passed, including the owned regressions for new and existing Jimu report saves persisting `template = 0`.

GREEN: rebuilt backend runtime `output/runtime/backend-20260516-142707.jar` + exact screenshot re-import -> PASS, backend log `output/runtime/backend-20260516-142707.out.log` shows `/admin-api/mes/pro/batch-record-report/import-image` completed at `2026-05-16 14:33:02` after the `template = 0` gateway fix, updating report `EBR_IMG_c48cdb7020e2_T01` under category `598eb5f05dac423a831cebb3c97c3fa7`.

GREEN: live category visibility after code-driven recovery -> PASS, after resetting the existing Jimu row back to `template = NULL`, the rebuilt backend runtime re-imported the same screenshot and restored dual visibility: `/admin-api/mes/pro/batch-record-report/page?pageNo=1&pageSize=10&routeKey=LEGACY` returned the image-generated row, and `/jmreport/query/report/folder?pageNo=1&pageSize=10&reportType=598eb5f05dac423a831cebb3c97c3fa7` returned `total: 1` with the same report code `EBR_IMG_c48cdb7020e2_T01`.
