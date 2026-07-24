# eDHR 归档业务健康采集任务

## Goal

为 eDHR 归档新增 runtime-control business health collector，至少暴露健康项 `edhr-archive-integrity`，只读 DB 检查 SEALED 归档、归档事件和 `storageRetention` metadata 完整性，并通过现有 `GET /infra/runtime-control/business-health` 与前端 `/infra/monitors/runtime-control` 真实用户路径可见。

## Current Status

Status: `completed`

Current stage: `completed; cleanup apply blocked by unrelated worktree state`

Planner/decomposer 文档已由 reviewer 复核通过。T1 RED contract/test 已新增失败测试和静态契约，并由主 reviewer 复跑确认失败原因正确。T2 后端 collector 已实现并通过后端 GREEN。T3 已新增真实用户路径 E2E；在 reviewer 安装 MES 模块并重启本地 current-code `48098` 后，真实 E2E 已通过。T4 独立验证曾因 AC-03、AC-08、AC-11 不放行；repair worker 已按 RED/GREEN 修复，主 reviewer 已复验通过，独立 re-verification 已 PASS。本任务功能和验证已 completed；task-closeout cleanup apply / ff-only merge / worktree removal 因 unrelated worktree state 阻塞，未执行删除或合并。

## Scope

In scope:

- `edhr-archive-integrity` 健康项 contract。
- 只读 DB collector 设计。
- SEALED archive 与 `ARCHIVE_SEAL` event 存在性检查，`ARCHIVE_SEAL` 只证明 seal evidence。
- 最新或对应 append-only 归档事件中的 `metadata_json.storageRetention` 完整性和一致性检查；当前已知来源是 `GENERATE_SUCCESS.metadataJson`。
- seal evidence 与 storage evidence 分开验收；不得要求 `ARCHIVE_SEAL.metadataJson.storageRetention`。
- 失败归档 visibility。
- runtime-control API 与前端真实路径 E2E 设计。
- BDD + strict TDD + subagent-driven 任务图。

Out of scope:

- 修改生产代码、测试代码或 E2E 代码，本 planner pass 不做。
- 归档修复、补 metadata、对象存储真实 verifier、S3 Object Lock 验证。
- 调用或依赖前置 storage-retention 未提交 FileService API。
- 写芋道源码租户数据。
- 用 API-only 替代最终 E2E。

## Previous Task Check

Previous task: `doc/tasks/20260528-edhr-storage-retention-gate/task-state.json`

Observed status: `blocked`

Impact: 前置任务已明确 BLOCKED，本任务可以进入独立 planning，但后续实现和提交必须排除前置任务未提交 storage-retention 改动。

## Milestones

- [x] M0: 创建任务文档目录并完成 planning artifacts。
- [x] T1: RED contract/test。先写失败测试，固定 health item、只读 DB、metadata rules 和 no-coupling boundary。
- [x] T2: Backend collector。实现只读 eDHR 归档完整性 collector，经 business-health API 暴露。
- [x] T3: Frontend/E2E。通过 `/infra/monitors/runtime-control` 测试租户真实路径验证可见。
- [x] T4: Independent verification。独立跑回归、E2E、no-coupling、git hygiene 和 task closeout preview。

## T1 RED Evidence Summary

- Backend JUnit RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` failed at test compilation because `MesProBatchRecordExecutionArchiveBusinessHealthCollector` does not exist.
- Static contract RED: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` failed because the production collector source file does not exist.
- Frontend static RED: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` failed because `tests\e2e\runtime-control-edhr-archive-health.e2e.js` does not exist yet.
- No production code, Mapper, SQL, Controller, Vue component, real E2E implementation, staging, commit, or previous storage-retention task file was modified in T1.

## Reviewer T1 Decision

- decision: `approved_for_t2`
- date: `2026-05-28T13:44:00+08:00`
- reviewer rerun evidence:
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected missing collector class only.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> FAIL, expected missing production collector file only.
  - `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> FAIL, expected missing real E2E script only.
  - `git diff --check` on T1 backend/frontend task paths -> PASS.
- rationale:
  - T1 tests preserve the corrected event model: `ARCHIVE_SEAL` proves seal evidence only; `storageRetention` is read from an append-only metadata source event, currently `GENERATE_SUCCESS.metadataJson`.
  - Static contract explicitly forbids FileService, storage-retention DTOs, and S3 retention API coupling.
  - Real E2E remains intentionally RED until T3 creates the Playwright user-path script.

## T2 Backend GREEN Evidence Summary

- Backend collector added: `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`.
- Scope preserved: no frontend change, no real E2E creation, no mapper mutation helper needed, no FileService/S3/storage-retention file touched by T2.
- Behavior implemented:
  - Exposes `edhr-archive-integrity` / `eDHR 归档完整性` as a Spring `RuntimeOpsBusinessHealthCollector`.
  - Uses `@Transactional(readOnly = true)` and DB-only selects from archive and append-only event tables.
  - Requires `ARCHIVE_SEAL` for SEALED archive seal evidence, without requiring seal event metadata.
  - Reads `metadataJson.storageRetention` from append-only source events, including current known `GENERATE_SUCCESS` source signal.
  - Returns `BLOCKED` for missing seal event, missing source event, fileId/sha256 mismatch, or missing/invalid retention fields.
  - Returns `WARN` when SEALED archives are complete but FAILED archive rows exist.
  - Returns `PASS` for empty data or complete sealed data with explicit `sealed=<n>, failed=<n>` evidence.
- Verification:
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests run, 0 failures, 0 errors.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.
  - `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java .\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollector.java` -> PASS.

T2 worker did not stage or commit. T3 remains required for frontend real user path E2E before release.

## Reviewer T2 Decision

- decision: `approved_for_t3`
- date: `2026-05-28T14:04:30+08:00`
- reviewer rerun evidence:
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests run, 0 failures, 0 errors.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.
  - `git diff --check` on T2 backend/task paths -> PASS.
  - `task-state.json` JSON validation -> PASS.
- rationale:
  - DB-only collector stays decoupled from FileService/S3/storage-retention APIs.
  - `ARCHIVE_SEAL` remains seal evidence only; storage evidence is read from append-only event metadata.
  - Reviewer blocker was repaired: retention evidence fields are visible and malformed source metadata returns `BLOCKED` instead of escaping as a collector exception.
  - Existing frontend panel already renders business health `name/status/evidence/reason`; T3 should create real E2E evidence without UI-only test affordances.

## T2 Reviewer Blocker Repair

- blocker: PASS/WARN and mismatch signals did not consistently expose `objectVersionId`, `retainUntil`, and `verifiedAt`; invalid source event metadata could throw from the collector and be converted by runtime-control into generic `business-health-collector-failed`.
- repair:
  - Added JUnit BDD coverage for PASS retention signal, WARN retention signal, mismatch retention signal, non-JSON metadata, and `storageRetention.fileId` type errors.
  - Updated collector so retention evidence signal is carried into PASS/WARN output and mismatch BLOCKED reasons.
  - Updated collector so malformed metadata and type errors return `BLOCKED` with archive/source event/invalid metadata signal instead of throwing.
- verification:
  - RED: target Maven command failed with 4 missing-signal failures and 2 invalid-metadata errors before the collector fix.
  - GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests.
  - GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

T2 is repaired after reviewer feedback. T3 still requires real frontend/E2E work before the overall task can be released.

## T3 Frontend/E2E Evidence Summary

- Frontend E2E added: `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\tests\e2e\runtime-control-edhr-archive-health.e2e.js`.
- Static contract tightened: `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\tests\e2e\runtime-control-edhr-archive-health-static.spec.js`.
- Script design:
  - Requires explicit `RUNTIME_CONTROL_E2E_BASE_URL`, `RUNTIME_CONTROL_E2E_TEST_TENANT`, `RUNTIME_CONTROL_E2E_TEST_USERNAME`, and `RUNTIME_CONTROL_E2E_TEST_PASSWORD`; no default account or URL is embedded.
  - Opens `/login?redirect=/infra/monitors/runtime-control` and uses the real runtime-control page.
  - Captures `/admin-api/infra/runtime-control/business-health`, unwraps common `code/data` response shape, and asserts `edhr-archive-integrity` / `eDHR 归档完整性`.
  - Collects `writeRequests` and fails if any `/admin-api/infra/runtime-control` request uses `request.method() !== 'GET'`.
- Verification:
  - `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.
  - `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> BLOCKED, business-health response did not contain `edhr-archive-integrity`; observed item codes: `login`, `erp`, `mes`, `file-object`, `api-error`, `slow-request`, `job-failure`.
- Impact:
  - T3 cannot be released until the backend behind `http://localhost:8081` runs current code with the T2 collector loaded.
  - No remote test server or API-only verification was used as a substitute.

## Reviewer T3 Decision

- decision: `approved_for_t4`
- date: `2026-05-28T14:35:30+08:00`
- reviewer repair of E2E prerequisite:
  - Installed current worktree `yudao-module-mes` into the local Maven repository with `mvn -pl yudao-module-mes -am -DskipTests install`.
  - Restarted the local backend target used by the current frontend: `127.0.0.1:48098`, with explicit local DB/Redis args matching the existing dev backend.
  - Confirmed `http://127.0.0.1:48098/actuator/health` returned `UP`.
- reviewer rerun evidence:
  - `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.
  - `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS, `edhr-archive-integrity` / `eDHR 归档完整性` visible, item status `BLOCKED`, `writes=0`.
- rationale:
  - `BLOCKED` is the collector's business health status for current data, not an E2E infrastructure blocker.
  - The E2E used the real `/infra/monitors/runtime-control` user path and observed `/admin-api/infra/runtime-control/business-health` through the page.
  - T4 independent verification is still required before completion.

## T4 Blocking Repair Evidence

- worker: `review-fix-loop repair worker`
- status: `blocking_repair_green_pending_reviewer`
- scope:
  - AC-08 mapper/DB query failure and mapper missing paths.
  - AC-03 missing `ARCHIVE_SEAL` reason completeness.
  - AC-11 test-report evidence refresh.
- repair:
  - Added RED regression coverage for `archiveMapper` query failure, `archiveEventMapper` query failure, missing `archiveMapper`, missing `archiveEventMapper`, and missing `ARCHIVE_SEAL` summary semantics.
  - Updated collector dependency/query failure handling so it returns code `edhr-archive-integrity`, status `BLOCKED`, and mapper/query context in `reason` instead of allowing runtime-control to produce generic `business-health-collector-failed`.
  - Updated missing `ARCHIVE_SEAL` reason to include `archiveId` from the existing outer signal plus `missingCount=1` and `missingSummary=ARCHIVE_SEAL`; storageRetention source-event checks remain separate.
  - Refreshed `test-report.md` from planning-only placeholders to actual evidence.
- verification:
  - RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 14 tests ran with 2 failures and 3 errors for the expected T4 blockers.
  - GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests.
  - GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.
  - GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m json.tool .\doc\tasks\20260528-edhr-archive-business-health\task-state.json` -> PASS.
  - GREEN: `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java .\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollector.java` -> PASS.
- remaining status:
  - Reviewer re-check passed; awaiting independent re-verification.
  - No staging or commit was performed.

## Reviewer T4 Repair Re-check

- decision: `passed_to_independent_reverification`
- date: `2026-05-28T15:16:30+08:00`
- reviewer rerun evidence:
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests run, 0 failures, 0 errors.
  - `mvn -pl yudao-module-infra "-Dtest=RuntimeBusinessHealthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests run, 0 failures, 0 errors.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.
  - `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m json.tool .\doc\tasks\20260528-edhr-archive-business-health\task-state.json` -> PASS.
  - `git diff --check` on backend task/test/prod paths and frontend E2E scripts -> PASS.
  - `mvn -pl yudao-module-mes -am -DskipTests install` -> PASS, repaired MES module installed into the local Maven repository.
  - `http://127.0.0.1:48098/actuator/health` -> PASS after restarting the current-code backend with explicit local DB/Redis args.
  - `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS, `edhr-archive-integrity` / `eDHR 归档完整性` visible, item status `BLOCKED`, `writes=0`.
- rationale:
  - AC-08 blocker is repaired: mapper missing and mapper/query failures are converted inside the collector to `edhr-archive-integrity` `BLOCKED` with context, instead of escaping to runtime-control generic `business-health-collector-failed`.
  - AC-03 diagnostic gap is repaired for the tested missing seal path: reason includes `archiveId`, `missingCount=1`, and `missingSummary=ARCHIVE_SEAL`.
  - AC-11 is repaired: `test-report.md` no longer reports planning-only status and now records BDD/RED/GREEN/E2E evidence.
  - Independent re-verification is still required before completion.

## Independent Re-verification Decision

- decision: `PASS`
- date: `2026-05-28T15:25:01+08:00`
- report: `doc/tasks/20260528-edhr-archive-business-health/verification-report.md`
- evidence:
  - AC-01..AC-12 requirement-to-artifact matrix passed.
  - `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests.
  - `mvn -pl yudao-module-infra "-Dtest=RuntimeBusinessHealthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
  - `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.
  - `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.
  - `http://127.0.0.1:48098/actuator/health` -> PASS, `UP`.
  - Real E2E -> PASS, `edhr-archive-integrity` / `eDHR 归档完整性` visible, status `BLOCKED`, `writes=0`.
- release decision:
  - Functionality may be committed with task-only staging.
  - No staging/commit was performed by verifier.

## Closeout Preview

- command: `$env:PYTHONUTF8='1'; python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-archive-business-health --mode preview`
- result: `blocked`
- keep:
  - `doc/tasks/20260528-edhr-archive-business-health/task.md`
  - `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- after adding `Cleanup Keep`, the latest preview keep list includes all formal task docs, backend collector, backend collector tests, and static contract.
- latest preview delete set: `<none>`
- blocked reasons:
  - Current linked worktree has unrelated pending storage-retention changes from the previous blocked task.
  - Main worktree `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` is dirty and cannot receive ff-only merge.
  - Current branch cannot be fast-forward merged into `int_main` according to cleanup preview.
  - The cleanup script still conservatively reports uncommitted kept task files as pending changes; no apply was run.
- decision:
  - Cleanup apply was not run.
  - Worktree fast-forward merge and deletion were not run.
  - Formal task docs are kept because the user explicitly requested subagent-driven implementation documents and reviewer evidence written to files.

## Expected Verification

- BDD scenarios in `test-plan.md` are implemented and logged before code changes.
- RED tests fail for missing expected behavior, not for unrelated environment breakage.
- GREEN backend tests prove PASS/WARN/BLOCKED cases。
- T1/T2 后端 collector 测试统一命名为 `MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest`，不得混用其他 collector 测试类名。
- WARN 实现需注意 `RuntimeOpsBusinessHealthCheckResult` 当前没有 `warn(...)` 工厂，worker 应直接构造 WARN result 或显式新增工厂。
- Static contract proves no FileService retention API coupling。
- Playwright E2E logs into test tenant at `http://localhost:8081`, opens `/infra/monitors/runtime-control`, observes `/admin-api/infra/runtime-control/business-health`, and sees `eDHR 归档完整性` rendered。
- If frontend/backend/test tenant prerequisites are unavailable, final E2E is BLOCKED with exact missing prerequisite and impact。
- `git diff --check` passes before task completion。
- task-closeout-cleanup preview runs before completion。

## Commit and Staging Rules

- Do not stage or commit previous storage-retention dirty files.
- Do not stage or commit unrelated user edits.
- Future commits must include only this task's direct changes.
- Task-specific staging must explicitly select only:
  - `doc/tasks/20260528-edhr-archive-business-health/`
  - `script/tests/test_edhr_archive_business_health_contract.py`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java`
  - frontend `tests/e2e/runtime-control-edhr-archive-health*.js`

## Cleanup Keep

- `doc/tasks/20260528-edhr-archive-business-health/dev-plan.md`
- `doc/tasks/20260528-edhr-archive-business-health/prd.md`
- `doc/tasks/20260528-edhr-archive-business-health/request-analysis.md`
- `doc/tasks/20260528-edhr-archive-business-health/task-state.json`
- `doc/tasks/20260528-edhr-archive-business-health/test-plan.md`
- `doc/tasks/20260528-edhr-archive-business-health/test-report.md`
- `doc/tasks/20260528-edhr-archive-business-health/verification-report.md`
- `script/tests/test_edhr_archive_business_health_contract.py`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`
- `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java`

## Blockers

- No functional blocker remains.
- Cleanup apply, ff-only merge, and worktree removal remain blocked by unrelated storage-retention dirty work and dirty/non-ff main worktree state.

## Reviewer Planning Decision

- decision: `approved_for_t1`
- date: `2026-05-28T13:27:40+08:00`
- rationale:
  - 文档已修正 `ARCHIVE_SEAL` 与 `GENERATE_SUCCESS.metadataJson.storageRetention` 的事件职责，不再要求 `ARCHIVE_SEAL.metadataJson.storageRetention`。
  - BDD/TDD/E2E 计划覆盖 DB-only collector、no-coupling 边界、运行控制台真实用户路径和缺环境 BLOCKED 规则。
  - 后续实现必须先跑 T1 RED，且不得耦合前置 storage-retention 未提交 FileService/S3 API。
