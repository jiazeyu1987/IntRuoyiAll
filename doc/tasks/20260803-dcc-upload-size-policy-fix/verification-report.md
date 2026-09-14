# Verification Report

## Summary

- Status: ready_for_closeout.
- Fix: added an idempotent DCC upload-size-policy default seed migration.
- Scope: backend SQL seed and SQL contract test only; no runtime fallback, exception swallowing, or frontend masking was introduced.

## Verification

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py` -> FAIL because `20260803_dcc_upload_size_policy_default_seed.sql` was missing.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py` -> PASS, 3 tests.
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260803-dcc-upload-size-policy-fix\migration-policy-gate.json` -> PASS, `migrationCount=420`.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccUploadSizePolicyServiceTest,DccControlledFileUploadApiTest#uploadPreviewFile_missingSizePolicy_throwsBeforeStorageOrTicket,DccControlledFileUploadApiTest#uploadPreviewFile_sourceDocx_successCreatesTicketAndDoesNotExposeFileId" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests, `BUILD SUCCESS`.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-dcc-upload-size-policy-fix\bug-regression-evidence.md` -> PASS before cleanup removed the temporary evidence file.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `codex/dcc-upload-size-policy-fix/int_main`, frontend `8095`, backend `48095`.
- GREEN: `git -c http.https://github.com.proxy= -c http.proxy= -c https.proxy= push origin codex/dcc-upload-size-policy-fix` -> PASS, branch pushed to `origin`.
- GREEN: local main recheck `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py` -> PASS, 3 tests.
- GREEN: local main recheck `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccUploadSizePolicyServiceTest,DccControlledFileUploadApiTest#uploadPreviewFile_missingSizePolicy_throwsBeforeStorageOrTicket,DccControlledFileUploadApiTest#uploadPreviewFile_sourceDocx_successCreatesTicketAndDoesNotExposeFileId" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests.
- GREEN: local `int_main` `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py` -> PASS, 3 tests.
- GREEN: local Docker MySQL runtime coverage check -> tenants `0`, `1`, and `122` have effective `PURPOSE` policies for all supported DCC upload purposes; missing-effective-policy query returned no rows.
- GREEN: real frontend login plus running backend effective-policy check for screenshot category `907180 / 专利检索与分析报告`, `purpose=SOURCE`, `fileSize=1000` -> HTTP `200`, business `code=0`, `policyId=22`, `policyCode=DCC_UPLOAD_DEFAULT_SOURCE_V1`, `maxBytes=10485760`.
- GREEN: real-page-context upload-preview for `芋道源码/admin`, `categoryId=907180`, `purpose=SOURCE`, `codex-upload-policy-empty.docx` -> upload HTTP `200`, business `code=0`, upload ticket present, cleanup HTTP `200`, cleanup business `code=0`, `cleanedCount=1`.
- GREEN: Playwright real UI E2E for `/dcc/controlled-file/upload` -> selected `技术文档 / 设计和开发输入阶段 / 专利检索与分析报告（如适用）` through the visible Cascader, uploaded `codex-e2e-upload-policy-empty.docx` through the real file input, received upload-preview HTTP `200` and business `code=0`, saw the preview panel, and did not see `DCC upload size policy is missing or invalid`.
- GREEN: E2E hygiene check -> previous leftover preview session cleanup `cleanedCount=1`, current E2E preview cleanup `cleanedCount=1`, and DB post-check `active_codex_upload_policy_temp_files=0`.
- BLOCKED: local `int_main` release migration policy gate fails before judging this migration because unrelated `20260730_mes_process_pool_team_leader.sql` is missing release-migration metadata.

## Root Cause

- DCC upload-preview correctly validates upload size before storage and fails fast when no effective policy exists.
- Existing release SQL created the `dcc_controlled_file_upload_policy` table but did not provide formal default effective policies for supported upload purposes.
- Real upload evidence from `doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/execution-log.md` showed `SOURCE` policy missing for upload categories with valid approval routes.

## Risk Notes

- The seed creates purpose-level defaults below category-specific rules, so stricter category or category-purpose policies still win.
- The seed only inserts when a tenant lacks an effective global or same-purpose purpose policy.
- Existing policies are not updated or deleted; if the exact seed policy code already exists but is invalid, the migration fails fast instead of silently overriding.
- Runtime note: the affected environment must apply `20260803_dcc_upload_size_policy_default_seed.sql` before users stop seeing the missing-policy message.
- Local runtime note: the current `E:\IntRuoyi` local runtime has the seed data applied and no longer reproduces the missing-policy toast for `907180 / SOURCE`; if a browser still shows the old toast, refresh the page and retry the file selection so the upload-preview request is reissued.
- Closeout blocker: linked worktree cleanup/merge is not complete because `E:\IntRuoyi` is dirty and cannot receive the ff-only merge.
- Git network note: persistent scoped GitHub proxy config points to closed `127.0.0.1:7890`; push succeeded only with one-off proxy clearing after direct `github.com:443` connectivity was verified.
- Continued closeout blocker: main worktree closeout could not proceed while another `git commit` and multiple Git status/diff processes were active in `E:\IntRuoyi`; no locks were deleted and no unrelated processes were killed.
- Third closeout attempt blocker: main worktree had active staged/unstaged concurrent changes and an active `git commit -m "docs: close DCC download entry task"` process, so ff-only merge and worktree removal remain blocked by shared-index ownership.
- Local `int_main` note: the SQL seed and SQL contract test have been applied to `E:\IntRuoyi` and require local verification/commit before this main workspace can be considered fixed.
- Local release-gate note: latest full migration policy gate in `E:\IntRuoyi` is blocked by an existing unrelated migration missing release metadata: `IntRuoyiBackend/sql/mysql/20260730_mes_process_pool_team_leader.sql`. This does not change the targeted SQL contract result for `20260803_dcc_upload_size_policy_default_seed.sql`.
- Local `int_main` risk note: DCC upload-size-policy SQL contract is green, but repo-wide release migration gate remains blocked by unrelated migration metadata and must be fixed before full release readiness can be claimed.
- Local commit note: applied on `int_main` as `4add2d288`; push to `origin/int_main` is blocked by current GitHub HTTPS connectivity failure.
