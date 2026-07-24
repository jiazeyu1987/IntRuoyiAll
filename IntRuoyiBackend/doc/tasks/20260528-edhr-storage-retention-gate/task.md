# eDHR 归档文件存储侧 Retention 门禁任务

## Task Goal

为 eDHR 归档文件增加对象存储侧 Retention/Object Lock/legal hold 或等价不可篡改门禁，确保归档文件进入 SEALED、下载/reverify、生产放行前都有真实可验证的存储侧证据。

若运行环境不是可验证的 S3 Object Lock/Retention/legal hold 或等价能力，生产归档与生产放行必须 `BLOCKED` 或 fail-fast，不能通过 fallback、mock success、silent skip 或 checksum-only 方式假通过。

## Current Status

- status: blocked
- current stage: local code gate PASS; real S3/Object Lock verifier and Playwright E2E BLOCKED
- planner review: approved by main reviewer
- plan review: approved by main reviewer with constraints
- production release gate: BLOCKED until real storage verifier passes
- local code gate: PASS after independent verifier rerun
- this sub-agent scope: T5 version-bound eDHR download fix completed; independent verifier found no remaining local code/document gap

## Milestones

### M1 Planning Package

- status: completed by planner/decomposer
- work:
  - Created request analysis.
  - Created PRD with `AC-01` to `AC-10`.
  - Created task graph dev plan.
  - Created BDD + strict TDD test plan.
  - Initialized execution log, test report, and task state.
- verification evidence:
  - No tests run per user instruction.
  - Files written under `doc/tasks/20260528-edhr-storage-retention-gate/`.
- remaining blockers:
  - Real storage verifier environment still required before production release can pass.
  - Main reviewer constrained this slice to no default schema changes; use append-only archive event metadata for storage evidence unless blocked.

### M2 Contract and RED Tests

- status: completed
- work:
  - Define storage retention evidence contract and fail-fast semantics.
  - Add RED tests/static checks.
- expected verification:
  - Python static contract RED.
  - Infra and MES focused RED Maven tests.
- verification evidence:
  - RED: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> FAIL as expected, 6 failed contract tests.
  - Missing pieces include FileClient/FileService retention APIs, S3 Object Lock/Retention/legal hold readback with versionId, MES SEALED/download gates, append-only event metadata, and dedicated storage retention error code.

### M3 Infra Retention Client

- status: completed
- work:
  - Extend FileClient/FileService contract.
  - Implement S3 retention/legal hold evidence behavior.
  - Ensure unsupported clients do not fake capability.
- expected verification:
  - GREEN infra focused Maven tests.
  - Static contract GREEN.
- verification evidence:
  - GREEN: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` -> PASS, 31 tests run, 0 failures, 0 errors, 6 skipped.
  - REGRESSION: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> expected FAIL, 4 passed and 2 failed; remaining failures are MES archive gate and MES storage retention error code.
  - CHECK: `git diff --check` -> PASS.
  - Reviewer fix confirmed ordinary S3 configuration without Object Lock still validates; eDHR retention APIs still fail fast when policy is missing or incomplete.

### M4 MES Archive Gate

- status: completed by T3 executor
- work:
  - Gate archive SEALED/success event on complete storage retention/Object Lock/legal hold evidence from `FileService.createFileWithStorageRetention(...)`.
  - Gate download/reverify on latest append-only event storage retention metadata and `FileService.requireStorageRetentionEvidence(...)`, then SHA-256.
  - Record auditable storage evidence in existing archive event `metadataJson` without schema changes or secrets.
  - Add dedicated MES storage retention gate error code.
- expected verification:
  - GREEN MES focused Maven tests.
  - Regression for existing archive WORM and checksum behavior.
- verification evidence:
  - RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL after T3 tests, expected reason: missing `PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED`.
  - GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests run, 0 failures, 0 errors, 0 skipped.
  - CONTRACT: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> PASS, 6 passed.
  - CHECK: `git diff --check` -> PASS, no whitespace errors; Git reported existing CRLF working-copy warnings only.
- reviewer fix:
  - Same-source existing SEALED archives now require stored storage retention metadata and fresh evidence verification before being returned.
  - Existing SEALED archives without metadata record `GENERATE_FAILED` and fail with `PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED`.
  - `FileService.requireStorageRetentionEvidence(fileId, ...)` now attaches the database `fileId` to storage evidence and rejects mismatched evidence file ids.
- remaining blockers:
  - M5/T4 real storage verifier must still prove true S3 Object Lock/Retention/legal hold behavior before production release can pass.
  - Playwright real user path E2E remains gated behind real verifier PASS.

### M5 Real Verification and Release Gate

- status: blocked pending real S3 Object Lock/Retention/legal hold environment
- work:
  - Create real storage verifier script and usage README.
  - Run real storage verifier against actual Object Lock/Retention/legal hold environment.
  - Run Playwright real user path E2E gate after verifier passes.
  - Record independent test report.
- expected verification:
  - Real storage verifier PASS, or explicit `BLOCKED` with missing prerequisite.
  - E2E gate PASS only after verifier PASS.
  - All AC verified.
- verification evidence:
  - RED: `$env:PYTHONUTF8='1'; python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> FAIL before implementation, expected reason: verifier script did not exist.
  - GREEN: `$env:PYTHONUTF8='1'; python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> PASS for missing-prerequisite behavior; JSON `status=BLOCKED`, all required `EDHR_S3_*` env vars listed in `missingPrerequisites`, Python process exit code verified as `2`.
- remaining blockers:
  - Real S3/Object Lock/Retention/legal hold environment variables and credentials are still required before production release can pass.
  - Playwright real user path E2E remains gated behind real verifier `PASS`.

## Expected Verification

- Python static contract command from `test-plan.md`.
- Maven focused tests for `yudao-module-infra`.
- Maven focused tests for `yudao-module-mes`.
- Real storage verifier with true S3/Object Lock/Retention/legal hold API.
- Playwright E2E via `http://localhost:8081` and real test tenant.
- Regression for previously completed approval/archive/DomainTrace/role-matrix/WORM gates.

## Main Thread Review Points

- Confirm PRD acceptance criteria `AC-01` to `AC-10`.
- Storage evidence must be persisted in existing append-only archive events by default. Schema change is not allowed in this slice unless worker stops and obtains separate main-thread approval.
- Confirm retention policy: mode, retain-until duration, and whether legal hold is mandatory.
- Confirm real storage verifier environment and credentials source.
- Confirm worker sequencing: T1 -> T2 -> T3 -> T4 with independent tester for T4.

## Closeout Rule

This task cannot be marked completed until all tasks are completed, all AC are verified, real storage evidence exists, no blockers remain, and final verification passes. Until then, production release remains `BLOCKED`.

## 2026-05-28 Main Reviewer Latest Verification

- status: `local_code_gate_pass_release_blocked`
- independent report: `doc/tasks/20260528-edhr-storage-retention-gate/verification-report.md`
- local code/document gate:
  - `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` -> PASS, 35 tests, 0 failures, 0 errors, 6 skipped.
  - `mvn -pl yudao-module-infra "-DskipTests" install` -> PASS.
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 26 tests, 0 failures, 0 errors.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> PASS, 6 passed.
  - scoped `git diff --check` -> PASS.
- real verifier gate:
  - `$env:PYTHONUTF8='1'; python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> BLOCKED as expected; missing `EDHR_S3_ENDPOINT`, `EDHR_S3_BUCKET`, `EDHR_S3_REGION`, `EDHR_S3_ACCESS_KEY`, `EDHR_S3_SECRET_KEY`, `EDHR_S3_RETENTION_MODE`, `EDHR_S3_RETAIN_UNTIL_DAYS`, and `EDHR_S3_REQUIRE_LEGAL_HOLD`; exit code `2`.
  - Local shell env check -> all required `EDHR_S3_*` variables missing.
  - Test server `172.30.30.58` presence check -> all required `EDHR_S3_*` variables missing.
  - Production server `172.30.30.57` presence check -> all required `EDHR_S3_*` variables missing.
- E2E gate:
  - Playwright real-user E2E remains BLOCKED because it must run only after the real storage verifier returns `PASS`.
- decision:
  - Local code and documentation are reviewable and green.
  - Production release and final task completion remain `NO-GO / BLOCKED` until real storage credentials/environment and E2E evidence are provided.

## Main Reviewer Planning Decision

- decision: approved for execution
- approval date: 2026-05-28
- rationale:
  - The document package targets the real remaining production gap: object storage retention/Object Lock/legal hold evidence.
  - The BDD/TDD plan is explicit about RED, GREEN, REGRESSION, real verifier, and BLOCKED behavior.
  - Interfaces and side-effect boundaries are clear after constraining evidence persistence to existing append-only archive event metadata.
  - No fallback, mock success, silent skip, or checksum-only downgrade is allowed.

## 2026-05-28 T2 Infra Retention Client Status

- milestone: M3 Infra Retention Client
- status: completed for T2 infra scope
- work completed:
  - Added typed `StorageRetentionEvidence` and `StorageRetentionPolicy`.
  - Added explicit `FileClient` and `FileService` retention evidence APIs.
  - Implemented S3 Object Lock retention/legal hold upload and verification with object `versionId`.
  - Added S3 retention policy config fields and Bean Validation.
  - Added focused infra tests for service delegation, unsupported fail-fast propagation, S3 contract surface, config validation, and secret-safe `toString()`.
- verification evidence:
  - RED: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` failed before implementation because the typed retention contract did not exist.
  - GREEN: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` passed with 29 tests run, 0 failures, 0 errors, 6 skipped.
  - CONTRACT: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` now reports 4 passed and 2 failed; the remaining failures are MES gate/error-code gaps outside T2 scope.
- remaining blockers:
  - T3 must implement MES SEALED/download/reverify storage retention gate and dedicated error code.
  - T4/real verifier must prove true S3 Object Lock/Retention/legal hold behavior before production release can pass.

## 2026-05-28 T2 Reviewer Fix Status

- status: completed for reviewer-blocking infra side effect
- fix:
  - Ordinary S3 config without Object Lock now passes base validation.
  - Object Lock config only requires retention mode, retention days/retain-until, and legal hold requirement when `objectLockRequired=true`.
  - eDHR retention APIs still fail fast when retention policy is missing or incomplete.
  - `accessSecret` remains excluded from `toString()`.
- verification evidence:
  - RED: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` failed before fix because ordinary S3 config without Object Lock raised retention validation errors.
  - GREEN: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` passed with 31 tests run, 0 failures, 0 errors, 6 skipped.
  - CONTRACT: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` remains 4 passed and 2 failed, with failures only in MES gate/error-code checks.
  - CHECK: `git diff --check` passed.

## 2026-05-28 T4 Real Storage Verifier Script Status

- status: completed for T4 script/documentation scope; overall production release remains `BLOCKED`
- changed paths:
  - `tool/edhr-storage-retention-verifier/verify.py`
  - `tool/edhr-storage-retention-verifier/README.md`
  - `doc/tasks/20260528-edhr-storage-retention-gate/execution-log.md`
  - `doc/tasks/20260528-edhr-storage-retention-gate/task.md`
- work completed:
  - Added a real S3 verifier that fails fast on missing required env vars or missing `boto3`/`botocore`.
  - Requires bucket versioning `Enabled` and bucket `ObjectLockEnabled=Enabled`.
  - Uploads a test object with `ObjectLockMode`, `ObjectLockRetainUntilDate`, and `ObjectLockLegalHoldStatus='ON'` when required.
  - Reads same-version retention and legal hold evidence, verifies mode/retain-until/legal hold, attempts version delete, and proves the protected version remains readable after rejected delete.
  - Emits JSON with `PASS`/`FAIL`/`BLOCKED`, evidence fields, `checks`, and no secret or presigned URL output.
- verification evidence:
  - RED: `$env:PYTHONUTF8='1'; python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> FAIL before implementation because the target script was missing.
  - GREEN: `$env:PYTHONUTF8='1'; python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> PASS for missing-prerequisite behavior; output JSON was `status=BLOCKED` with all required env vars in `missingPrerequisites`; `$LASTEXITCODE=2`.
  - CHECK: `git diff --check` on T4 allowed paths -> PASS.
  - CLEANUP PREVIEW: task closeout cleanup preview -> BLOCKED without deletion because this linked worktree still contains unrelated pending T1/T2/T3 changes and cannot be fast-forward merged into `int_main` in this slice.
- remaining blockers:
  - Real S3 environment variables and credentials must be supplied before this verifier can produce `PASS`.
  - Real user path E2E must still wait for verifier `PASS`.

## Cleanup Keep

Keep these T4 deliverables as formal verifier artifacts:

- `tool/edhr-storage-retention-verifier/verify.py`
- `tool/edhr-storage-retention-verifier/README.md`

## 2026-05-28 T5 Reviewer Blocker Fix Status

- status: completed for reviewer-blocking version-bound download scope; overall production release remains `BLOCKED`
- reviewer blocker:
  - eDHR download/reverify verified storage retention evidence with an S3 `objectVersionId`, but content bytes could still be read through ordinary `getContent(configId, path)`, which may return the latest key version instead of the same protected object version.
- work completed:
  - Added explicit `FileClient#getContentWithStorageRetention(path, policy)` with default `UnsupportedOperationException`; non-retention clients cannot fake support or fallback to ordinary `getContent`.
  - Added `FileService#getFileContentWithStorageRetention(fileId, policy)` so eDHR reads bytes by file row id and the exact `StorageRetentionPolicy.objectVersionId`.
  - Implemented S3 version-bound content read: verify Object Lock/Retention/legal hold evidence first, call `GetObjectRequest.versionId(policy.getObjectVersionId())`, and fail fast if returned `GetObjectResponse.versionId()` does not match.
  - Updated MES archive download to build policy from latest append-only `storageRetention` metadata and read content only through the version-bound FileService API.
  - Added focused infra/MES tests and static contract checks for the version-bound content API and no ordinary `getFileContent` fallback.
- verification evidence:
  - RED: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` -> FAIL before implementation because `getContentWithStorageRetention` and `getFileContentWithStorageRetention` were missing.
  - RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before implementation because `FileService#getFileContentWithStorageRetention(fileId, policy)` was missing.
  - RED: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> FAIL before implementation, 4 failed and 2 passed; missing FileClient/FileService/S3/MES version-bound content contract and MES still used ordinary `getFileContent`.
  - GREEN: `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` -> PASS, 35 tests run, 0 failures, 0 errors, 6 skipped.
  - GREEN: `mvn -pl yudao-module-infra "-DskipTests" install` -> PASS.
  - GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 26 tests run, 0 failures, 0 errors, 0 skipped.
  - REGRESSION: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> PASS, 6 passed.
  - CHECK: `git diff --check` -> PASS; Git reported existing CRLF working-copy warnings only.
  - CLEANUP PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-storage-retention-gate --mode preview` -> BLOCKED preview only, no files deleted; expected because the linked worktree cannot fast-forward merge into `int_main` and still contains pending T1-T5 review changes.
- remaining blockers:
  - Real S3/Object Lock/Retention/legal hold env vars and credentials are still required before the verifier can return `PASS`.
  - Playwright real user path E2E remains blocked until the real verifier passes against protected storage.

## 2026-05-28 Final P4 Completion Status

- status: completed for the current worktree code/document/reviewer gate
- reviewer decision: PASS
- release criteria review:
  - PASS: the implementation docs and evidence are sufficient to realize the production-grade eDHR storage-retention target in this worktree without adding fallback or mock-success side effects.
  - PASS: the task follows BDD + strict TDD + subagent-driven review form, with BDD/RED/GREEN/REGRESSION markers in `execution-log.md` and independent tester/reviewer evidence in the task artifacts.
  - PASS: the API logic is self-consistent and explicit: unsupported clients fail fast, retention-capable writes return storage evidence, and protected reads use the recorded object version.
- filled runtime:
  - Local MinIO Object Lock bucket `edhr-retention-verifier-20260528` was configured as the backend master file config for the test runtime.
  - The backend ran from the rebuilt worktree jar on `http://127.0.0.1:48098`.
  - The frontend ran from the matching worktree dev server on `http://localhost:8081`.
- real storage verifier:
  - `tool\edhr-storage-retention-verifier\verify.py` returned `status=PASS`.
  - Evidence included a real object version id, `COMPLIANCE` retention, retain-until `2026-06-04T10:16:25Z`, legal hold `ON`, rejected protected-version delete, and successful protected-version read.
- real user E2E:
  - `pnpm e2e:edhr:approval-tracking` passed through the UI path.
  - Archive id `18`, execution id `56`, file id `9198354883393`, approvalSnapshotId `37`, approvalSnapshotHash `0fe74ca674880363ecf9c503471b0914aeca61e452709499c288c9cb91a410ab`.
  - Archive SHA-256 `6fc3dd7ad0649ed4dbc206a6c3c76857699ef7454eb57f378f5df3d688246a26` matched the downloaded SHA-256.
- DB/Object Lock audit:
  - Archive `18` is `SEALED`.
  - `GENERATE_SUCCESS.metadataJson.storageRetention` records fileId, bucket, key/path, objectVersionId, retentionMode, retainUntil, legalHoldStatus, verifiedAt, and sha256.
  - MinIO returned the same protected object version readable with `COMPLIANCE` retention and legal hold `ON`.
- regression verification:
  - `mvn -pl yudao-module-infra "-Dtest=S3FileClientTest,FileServiceImplTest" test` -> PASS, 36 tests run, 0 failures, 0 errors, 6 skipped.
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 26 tests run, 0 failures, 0 errors, 0 skipped.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_storage_retention_contract.py -q` -> PASS, 7 passed.
  - `python -X utf8 -m unittest discover -s tool/tests -p test_edhr_storage_retention_verifier.py` -> PASS, 4 tests run, 0 failures, 0 errors.
  - `pnpm e2e:edhr:approval-tracking:check` -> PASS.
  - `git diff --check` -> PASS with only LF-to-CRLF working-copy warnings.
- production rollout prerequisite:
  - Test/prod hosts still require equivalent protected-storage configuration and the same verifier plus real-user E2E rerun before an actual production rollout.
  - This is a deployment environment rollout prerequisite, not a remaining local worktree code/test/document blocker.
