# Execution Log: Electronic Batch Record DOC Report Frontend

BDD: electronic batch-record page exposes the DOC import management flow -> Given an operator opens `/mes/pro/batch-record-template`, When the page loads successfully, Then it should show the import button, refresh action, search input, and generated-report table instead of blocker-only content.

BDD: generated reports can be opened in the designer wrapper -> Given generated reports exist in the list, When the operator clicks `修改` on a row, Then the app should route to the internal wrapper page and load the JimuReport designer for that report.

BDD: generated reports can be removed from the management table -> Given generated reports exist in the list, When the operator confirms `删除` on a row, Then the page should call the backend delete API and refresh the table without leaving stale rows behind.

RED: `node --test scripts/electronic-batch-record-report-page.test.mjs` -> FAIL, before implementation the page still rendered blocker-only content, the new API module did not exist, and there was no designer-wrapper mode.

GREEN: `node --test scripts/electronic-batch-record-report-page.test.mjs` -> PASS.

GREEN: `pnpm exec eslint scripts/electronic-batch-record-report-page.test.mjs src/api/mes/pro/batchrecordreport/index.ts src/views/mes/pro/batchrecordtemplate/index.vue src/views/mes/pro/batchrecordtemplate/DesignerWrapper.vue` -> PASS.

GREEN: `pnpm build:local` -> PASS.
