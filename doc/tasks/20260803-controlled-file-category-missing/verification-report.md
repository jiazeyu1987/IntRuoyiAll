# Verification Report

## Summary

受控文件提交页已按用户口径收敛为：文件类别自动显示所选文件分类叶子节点，只读不可填写；前端仍提交正式 DCC `categoryId`，该 ID 由当前 taxonomy 叶子节点唯一可上传类别自动解析。若该正式类别未绑定提交目录，后端查询目录树和提交链路都自动使用正式 `UNCLASSIFIED / 未分类` 目录，并通过 `defaultUnclassified` 提示前端展示“自动提交到未分类目录”；缺少或重复启用未分类目录时 fail-fast。

## Verification

- PASS: `node tests/e2e/dcc-upload-category-permission-static.spec.js`
- PASS: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`
- PASS: `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js`
- PASS: `node tests/e2e/dcc-upload-name-version-autofill-static.spec.js`
- PASS: `node tests/e2e/dcc-upload-product-autofill-static.spec.js`
- PASS: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q`
- PASS: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260803_dcc_unclassified_upload_directory_seed.sql --output doc\tasks\20260803-controlled-file-category-missing\migration-policy-gate-unclassified.json`
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingReturnsUnclassifiedDirectory,DccControlledFileWorkflowServiceImplTest#submitControlledFile_categoryWithoutDirectoryBindingUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: heartbeat Maven `DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingAndUnclassifiedMissingFailsFast`
- PASS: heartbeat Maven `DccControlledFileWorkflowServiceImplTest#submitControlledFile_bindingMissingAndUnclassifiedDirectoryMissing_throwsNotExists`
- PASS: real E2E prerequisite `npx --version` -> `11.6.2`; local frontend `http://127.0.0.1:8081/` -> HTTP 200; local backend `http://127.0.0.1:48081/actuator/health` -> `UP`; missing Playwright Chromium was installed with `npx playwright install chromium`.
- PASS: `node --check tests\e2e\dcc-upload-category-leaf-real.e2e.js`.
- PASS: isolated backend package `mvn.cmd -pl yudao-server -am "-DskipTests" package`; Jar SHA256 `4f3def41fe02d7b0d565e272821fc26fb00d58fdbd1d5cdbb6342e8f4bd5ca04`.
- PASS: nested Jar inspection confirmed `BOOT-INF/lib/yudao-module-dcc-2026.04-SNAPSHOT.jar` contains `cn/iocoder/yudao/module/dcc/service/file/DccUploadDirectoryResolver.class`.
- PASS: isolated runtime health `http://127.0.0.1:48099/actuator/health` -> `UP`; isolated frontend `http://127.0.0.1:8099/` -> HTTP `200`.
- PASS: local DB prerequisite seed `20260803_dcc_unclassified_upload_directory_seed.sql` inserted unique active `UNCLASSIFIED / 未分类` rows for tenants `0/1/122`; `HEX(name)=E69CAAE58886E7B1BB`.
- PASS: real Playwright E2E `DCC_UPLOAD_CATEGORY_LEAF_E2E_BASE_URL=http://127.0.0.1:8099 node tests\e2e\dcc-upload-category-leaf-real.e2e.js`; evidence JSON shows `status=PASS`, selected real candidate `技术文档 / 设计和开发策划阶段 / 技术调研报告`, `bindingDirectoryPath=未分类`, `defaultUnclassified=true`, and no DCC write requests, target network failures, console errors, or page errors.

## Blocked Checks

- Shared-runtime E2E remains blocked: running `48081` process `42064` uses `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-int-main-6f5f52814.jar`; read-only nested-Jar inspection shows `BOOT-INF/lib/yudao-module-dcc-2026.04-SNAPSHOT.jar` does **not** contain `cn/iocoder/yudao/module/dcc/service/file/DccUploadDirectoryResolver.class`. The shared runtime has not loaded the backend fix.
- Shared-runtime restart remains blocked: the current workspace contains many unrelated dirty backend/frontend changes and branch-ahead state, so project runtime rules do not allow rebuilding/restarting the shared `int_main` backend from this mixed workspace just to make E2E pass. The real E2E acceptance was completed on isolated slot 18 instead.
- BLOCKED: `pnpm ts:check` failed in unrelated `src/views/dcc/controlled-file/detail/index.vue` missing `pagedRouteSnapshotRows`, `distributionStatusRows`, and `pagedDistributionStatusRows`; no new upload-page type error was reported before that failure.
- BLOCKED: full migration policy gate over all SQL failed on unrelated `IntRuoyiBackend\sql\mysql\20260730_mes_process_pool_team_leader.sql` missing release metadata; the DCC base + unclassified seed migration chain passed.

## Closeout Status

Implementation, targeted verification, and real Playwright E2E are complete on the isolated `8099/48099` runtime. The task-owned isolated Vite/Java processes were stopped after evidence capture, and ports `8099/48099` are free. Task status remains `ready_for_closeout` rather than `completed` because the shared `48081` runtime has not loaded the backend fix, the shared worktree has unrelated dirty changes, the branch is currently behind `origin/int_main`, and full-suite gates remain blocked by unrelated historical issues.
