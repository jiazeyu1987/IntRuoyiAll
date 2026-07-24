# Execution Log: Report Management Six-Route Recognition Frontend

BDD: report management exposes a second tab for six-route recognition -> Given an operator opens the global report-management JimuReport page, When the page loads, Then the existing JimuReport iframe remains available and a second tab shows six route buttons for the fixed pilot `.doc` recognition flow.

BDD: generated reports remain isolated by route -> Given the operator triggers different recognition routes from the new tab, When generated reports are listed, Then the UI should show or filter route-specific report sets without overwriting other routes' results.

RED: `node scripts/report-management-six-route-page.test.mjs` -> FAIL, the current frontend API module has no `recognizeFixedRoute` binding and `src/views/report/jmreport/index.vue` is still a single-iframe page with no tab shell or six route buttons.

GREEN: `node scripts/report-management-six-route-page.test.mjs` -> PASS, the report-management page now exposes the second recognition tab, six route buttons, and the fixed-route API binding.

GREEN: `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS, the legacy electronic batch-record page still renders its upload/list/designer flow after the `routeKey=LEGACY` isolation change.

GREEN: `pnpm exec eslint src/views/report/jmreport/index.vue src/api/mes/pro/batchrecordreport/index.ts src/views/mes/pro/batchrecordtemplate/index.vue` -> PASS.

NOTE: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check` still fails because of pre-existing unrelated TypeScript errors in legacy files outside this task's ownership; the changed frontend files for this task were not named in the reported errors.

GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, the local frontend entry `http://127.0.0.1:8081` and backend docs `http://127.0.0.1:48081/v3/api-docs` both returned HTTP `200` after restart.

RED: initial real-page smoke through `playwright-cli run-code --filename ...\\verify-report-management-six-route-smoke.mjs` -> FAIL, the browser hit `ERR_CONNECTION_REFUSED` first because the local backend process had already exited, and after backend restart the page still failed with `Unknown column 'route_key' in 'field list'` because the local MySQL runtime schema had not yet applied the new column.

GREEN: local runtime repair -> PASS, after rebuilding `yudao-server.jar`, restarting local frontend/backend, and applying the local MySQL `route_key` column/index upgrade for `mes_pro_batch_record_report`, the real report-management page reached `六路识别`, route `A` returned HTTP `200`, and the visible table rendered route-A report rows.

GREEN: authenticated browser-context runtime checks -> PASS for routes B, C, and F, each returning HTTP 200 with importedCount=15; measured durations were about 40s for B, 3.5s for C, and under 1s for F.

NOTE: route D is still blocked in the live runtime even though its focused unit tests pass; the real-page click returned business code 500 with oute_d_pdf_reflow_converter_failed ... 'NoneType' object has no attribute 'SaveAs2'.

NOTE: route E is still blocked in the live runtime; backend logs show MesProBatchRecordCodexCliImageParser timing out on the first rendered template image and the request ends with Codex CLI ͼƬʶ��ʱ.
