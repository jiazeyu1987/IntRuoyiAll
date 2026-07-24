# eDHR 归档业务健康采集执行日志

## 2026-05-28 Planning Pass

Planner/decomposer completed documentation-only planning for `20260528-edhr-archive-business-health`.

Files planned and written in this pass:

- `doc/tasks/20260528-edhr-archive-business-health/request-analysis.md`
- `doc/tasks/20260528-edhr-archive-business-health/prd.md`
- `doc/tasks/20260528-edhr-archive-business-health/dev-plan.md`
- `doc/tasks/20260528-edhr-archive-business-health/test-plan.md`
- `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- `doc/tasks/20260528-edhr-archive-business-health/test-report.md`
- `doc/tasks/20260528-edhr-archive-business-health/task.md`
- `doc/tasks/20260528-edhr-archive-business-health/task-state.json`

Planning findings:

- Previous task `20260528-edhr-storage-retention-gate` is already `blocked`; this planning pass did not modify it.
- Current worktree contains storage-retention uncommitted changes; new task plan requires future staging and commits to exclude those files.
- Existing runtime-control business health API and frontend panel can carry the new item if a collector returns it.
- Sustainable module boundary needs reviewer attention: avoid infra depending on MES; prefer a MES-side collector implementing the runtime-control SPI if dependency direction permits.
- Real E2E depends on `http://localhost:8081` and a current-code backend service; if either is unavailable, final E2E must be `BLOCKED`.

Tests executed in this pass: none.

No RED, GREEN, REGRESSION, Playwright, Maven, or API verification was run during planning. Those commands are planned in `test-plan.md` and must be executed only after reviewer approval and implementation begins.

## 2026-05-28 Planning Revision

Reviewer blocker repair completed as documentation-only work. No production code, test code, E2E code, Maven command, Playwright command, API call, DB mutation, staging, commit, or task completion was performed.

Revised planning scope:

- `request-analysis.md`: recorded reviewer revision scope and clarified that `ARCHIVE_SEAL` proves seal evidence only, while storage evidence must come from the latest or corresponding append-only event metadata containing `storageRetention`; current known source is `GENERATE_SUCCESS.metadataJson`.
- `prd.md`: strengthened FR/AC wording so `ARCHIVE_SEAL` and storageRetention source events are separate responsibilities, and explicitly removed any requirement for `ARCHIVE_SEAL.metadataJson.storageRetention`.
- `dev-plan.md`: updated T1/T2 implementation notes so backend workers do not assume an existing `warn(...)` factory and use only `MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest` as the collector test class name.
- `test-plan.md`: added Event Responsibility Acceptance and BDD coverage for the case where `ARCHIVE_SEAL.metadataJson` is null but `GENERATE_SUCCESS.metadataJson.storageRetention` is valid.
- `task.md` and `task-state.json`: recorded the same reviewer repair constraints without marking the task completed.

BDD: seal event and storage evidence remain separate -> Given a SEALED archive has `ARCHIVE_SEAL` with null metadataJson and `GENERATE_SUCCESS.metadataJson.storageRetention` is valid / When business health collection runs / Then the archive is not blocked because of null `ARCHIVE_SEAL.metadataJson`, and storage evidence is evaluated from the append-only source event.

Tests executed in this revision: none, per reviewer instruction.

## 2026-05-28 Reviewer Planning Approval

- reviewer decision: `approved_for_t1`
- approved scope: T1 RED contract/test only; production code changes require RED evidence first.
- approval basis:
  - PRD/test-plan/dev-plan now separate `ARCHIVE_SEAL` seal evidence from the storageRetention source event.
  - Current known storageRetention source is `GENERATE_SUCCESS.metadataJson`, and documents no longer require `ARCHIVE_SEAL.metadataJson.storageRetention`.
  - BDD/TDD/E2E plan covers DB-only collector behavior, no-coupling with storage-retention FileService/S3 APIs, runtime-control visibility, and real-user E2E BLOCKED handling.

BDD: reviewer approves T1 after event model correction -> Given planning documents match the current archive event model / When the main reviewer checks PRD, dev-plan and test-plan / Then T1 may start with RED tests, while production implementation remains blocked until RED evidence exists.

Tests executed in this approval pass: none. This is a reviewer state transition only.

## 2026-05-28 T1 RED Contract/Test

T1 RED worker added only failing tests and static contracts. No production code, Mapper, SQL, Controller, Vue component, real E2E implementation, staging, commit, or previous `20260528-edhr-storage-retention-gate` file was modified.

Files written in this pass:

- `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java`
- `script/tests/test_edhr_archive_business_health_contract.py`
- `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\tests\e2e\runtime-control-edhr-archive-health-static.spec.js`
- `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- `doc/tasks/20260528-edhr-archive-business-health/task.md`

BDD: collector exposes eDHR archive integrity health contract -> Given runtime-control loads a MES-side business health collector / When `collect()` runs / Then the result code is `edhr-archive-integrity`, the name is `eDHR 归档完整性`, and the collector implements `RuntimeOpsBusinessHealthCollector`.

BDD: SEALED archive missing seal event blocks -> Given a SEALED archive has valid `GENERATE_SUCCESS.metadataJson.storageRetention` but no `ARCHIVE_SEAL` event / When business health collection runs / Then `edhr-archive-integrity` returns `BLOCKED` and identifies the missing `ARCHIVE_SEAL` evidence with archiveId signal.

BDD: seal event and storage evidence remain separate -> Given a SEALED archive has `ARCHIVE_SEAL.metadataJson=null` and a complete `GENERATE_SUCCESS.metadataJson.storageRetention` source event / When business health collection runs / Then the item is not `BLOCKED` because of null seal metadata and records sealed/failed evidence.

BDD: missing storageRetention source event blocks -> Given a SEALED archive has an `ARCHIVE_SEAL` event but no append-only event containing `metadataJson.storageRetention` / When business health collection runs / Then `edhr-archive-integrity` returns `BLOCKED` with storageRetention source-event signal.

BDD: storageRetention identity mismatch blocks -> Given a SEALED archive has seal evidence and source metadata / When `storageRetention.fileId` or `storageRetention.sha256` differs from the archive main record / Then `edhr-archive-integrity` returns `BLOCKED` and identifies the mismatched field.

BDD: complete SEALED archives with failed archive warn -> Given all SEALED archive integrity checks pass and at least one `archive_status=FAILED` archive exists / When business health collection runs / Then `edhr-archive-integrity` returns `WARN` and exposes `sealed=1`, `failed=1`, and failed archive signal.

BDD: empty archive data passes explicitly -> Given no SEALED or FAILED eDHR archive rows exist / When business health collection runs / Then `edhr-archive-integrity` returns `PASS` and evidence contains `sealed=0, failed=0`.

BDD: frontend E2E must use real runtime-control path -> Given the future E2E script is the release evidence for the UI path / When the static contract scans it / Then it must include login redirect to `/infra/monitors/runtime-control`, `/admin-api/infra/runtime-control/business-health`, `edhr-archive-integrity`, `eDHR 归档完整性`, test tenant marker, and read-only non-GET guard.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: test compilation cannot find `MesProBatchRecordExecutionArchiveBusinessHealthCollector`; Maven reported `找不到符号` for the collector class in `MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java`.

RED: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> FAIL, expected reason: production collector source is missing at `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`; 5 static contract tests failed on the same missing collector precondition.

RED: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> FAIL, expected reason: real frontend E2E script is missing at `tests\e2e\runtime-control-edhr-archive-health.e2e.js`.

`git diff --check` was not run in this T1 pass because it is optional for RED evidence and the worker scope is limited to failing tests/contracts.

## 2026-05-28 Reviewer T1 Verification

Reviewer re-ran the T1 RED commands after the worker completed. All three commands failed for the expected missing-implementation reasons and did not reveal unrelated contract drift.

BDD: reviewer accepts T1 RED only when failures match the missing implementation -> Given the backend test, static contract, and frontend static contract all fail only because the collector or real E2E file is absent / When the main reviewer reruns the commands / Then the evidence is acceptable for advancing to T2 backend implementation.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesProBatchRecordExecutionArchiveBusinessHealthCollector` is still absent and test compilation reports `找不到符号`.

RED: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> FAIL, expected reason: production collector source file is still absent and the static contract fails fast on that missing prerequisite.

RED: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> FAIL, expected reason: the real E2E script `tests\e2e\runtime-control-edhr-archive-health.e2e.js` is still missing.

GREEN: `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java` -> PASS.

GREEN: `git diff --check -- .\tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.

Reviewer decision: T1 approved, T2 may start.

## 2026-05-28 T2 Backend Collector GREEN

T2 backend worker implemented the MES-side eDHR archive business health collector. No frontend file, real E2E script, mapper helper, FileService, S3 client, storage-retention DTO, staging, commit, or previous `20260528-edhr-storage-retention-gate` file was modified by this pass.

Files written in this pass:

- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`
- `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- `doc/tasks/20260528-edhr-archive-business-health/task.md`
- `doc/tasks/20260528-edhr-archive-business-health/task-state.json`

BDD: runtime-control exposes eDHR archive integrity -> Given runtime-control loads Spring business health collectors / When the MES collector runs / Then it returns code `edhr-archive-integrity`, name `eDHR 归档完整性`, and a sampled timestamp.

BDD: SEALED archive integrity is DB-only -> Given SEALED archive rows and append-only event rows exist / When collection runs / Then it reads only archive and archive event tables, performs no insert/update/delete, does not generate or download archives, and does not read file content.

BDD: seal evidence and storage evidence stay separate -> Given a SEALED archive has `ARCHIVE_SEAL.metadataJson=null` and a valid append-only event containing `metadataJson.storageRetention` / When collection runs / Then null seal metadata does not block the archive, and the storage evidence is evaluated from the source event.

BDD: missing or inconsistent SEALED evidence blocks release -> Given any SEALED archive lacks `ARCHIVE_SEAL`, lacks a storageRetention source event, has mismatched fileId/sha256, or lacks `objectVersionId`, `retainUntil`, or `verifiedAt` / When collection runs / Then `edhr-archive-integrity` returns `BLOCKED` with archive id and field signal.

BDD: failed archive visibility warns after SEALED integrity passes -> Given all SEALED archives are complete but one or more `archive_status=FAILED` rows exist / When collection runs / Then `edhr-archive-integrity` returns `WARN` with `sealed=<n>`, `failed=<n>`, and failed archive id signal.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason before implementation: test compilation could not find `MesProBatchRecordExecutionArchiveBusinessHealthCollector`.

RED: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> FAIL, expected reason before implementation: production collector source file was missing.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests run, 0 failures, 0 errors.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

GREEN: `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java .\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollector.java` -> PASS.

T2 outcome: backend collector is GREEN and ready for T3 frontend real user path E2E. The overall task is not releasable until T3 and independent verification pass.

## 2026-05-28 T2 Reviewer Blocker Repair

T2 reviewer found a blocker before T3 approval: retention evidence fields `objectVersionId`, `retainUntil`, and `verifiedAt` were not consistently visible in PASS/WARN and mismatch signals, and malformed source event metadata could escape as collector exceptions instead of returning `BLOCKED`.

Files changed in this repair:

- `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`
- `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- `doc/tasks/20260528-edhr-archive-business-health/task.md`
- `doc/tasks/20260528-edhr-archive-business-health/task-state.json`

BDD: complete SEALED archive exposes retention signal -> Given a SEALED archive has a valid `GENERATE_SUCCESS.metadataJson.storageRetention` / When the collector returns PASS / Then `objectVersionId`, `retainUntil`, and `verifiedAt` are visible in the health signal.

BDD: failed archives retain SEALED retention evidence -> Given SEALED archive integrity passes and a FAILED archive exists / When the collector returns WARN / Then the warning signal still includes SEALED retention evidence plus failed archive ids.

BDD: mismatch blocks with retention signal -> Given `storageRetention.fileId` or `storageRetention.sha256` differs from the archive row / When the collector returns BLOCKED / Then the reason includes the mismatch field and the source event retention signal.

BDD: invalid source metadata blocks without collector failure -> Given a source event has non-JSON metadata or `storageRetention.fileId` has the wrong type / When the collector runs / Then it returns `BLOCKED` with archive id, source event, and invalid metadata signal instead of throwing an exception to runtime-control.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 4 failures for missing retention signals in PASS/WARN/mismatch output and 2 errors for JSON/fileId conversion exceptions escaping the collector.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests run, 0 failures, 0 errors.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

T2 blocker repair outcome: backend collector now satisfies the reviewer blocker and remains ready for T3 frontend real user path E2E review. No staging or commit was performed.

## 2026-05-28 Reviewer T2 Verification

Reviewer re-ran the backend GREEN gates after the T2 blocker repair and approved T3.

BDD: reviewer approves T3 only after backend evidence and no-coupling gates pass -> Given the collector returns DB-only business health results and malformed retention metadata is reported as `BLOCKED` / When the reviewer reruns Maven, static contract, diff check, and JSON validation / Then frontend real user E2E may start without changing the runtime-control UI solely for testability.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests run, 0 failures, 0 errors.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

GREEN: `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java .\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollector.java` -> PASS.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m json.tool .\doc\tasks\20260528-edhr-archive-business-health\task-state.json` -> PASS.

Reviewer decision: T2 approved, T3 frontend real user E2E may start.

## 2026-05-28 T3 Frontend Real User E2E

T3 frontend/E2E worker added the real Playwright user-path script and tightened the static contract to require all explicit E2E environment variables. No runtime-control Vue component, backend production code, storage-retention file, staging, commit, or UI-only test affordance was modified.

Files written in this pass:

- `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\tests\e2e\runtime-control-edhr-archive-health.e2e.js`
- `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\tests\e2e\runtime-control-edhr-archive-health-static.spec.js`
- `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- `doc/tasks/20260528-edhr-archive-business-health/task.md`
- `doc/tasks/20260528-edhr-archive-business-health/task-state.json`

BDD: runtime-control page exposes eDHR archive integrity through real user path -> Given the test tenant logs in through `/login?redirect=/infra/monitors/runtime-control` / When the runtime-control page calls `/admin-api/infra/runtime-control/business-health` / Then the response contains item code `edhr-archive-integrity`, name `eDHR 归档完整性`, status `PASS`/`WARN`/`BLOCKED`, and the page renders `eDHR 归档完整性`.

BDD: business health E2E remains read-only -> Given the E2E opens only the runtime-control business health path / When the page loads and business-health is observed / Then no non-GET request is sent to `/admin-api/infra/runtime-control`.

BDD: E2E requires explicit target and test tenant credentials -> Given the E2E script has no default frontend URL, tenant, username, or password / When any required env var is absent / Then the script fails fast instead of guessing credentials or using a fallback target.

GREEN: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS, real E2E script is wired and contains `/infra/monitors/runtime-control`, `/admin-api/infra/runtime-control/business-health`, `edhr-archive-integrity`, `eDHR 归档完整性`, `login?redirect=/infra/monitors/runtime-control`, explicit test tenant env markers, `writeRequests`, and `request.method() !== 'GET'`.

BLOCKED: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> FAIL, exact reason: frontend login and business-health response succeeded, but the response did not contain `edhr-archive-integrity`; observed item codes were `login`, `erp`, `mes`, `file-object`, `api-error`, `slow-request`, and `job-failure`.

Impact: T3 cannot provide release E2E evidence until the backend serving `http://localhost:8081` loads the current-code T2 MES collector change. Per reviewer instruction, this is recorded as `BLOCKED` and was not replaced by remote test server verification or API-only verification.

## 2026-05-28 Reviewer T3 Verification

Reviewer verified the T3 BLOCKED report and repaired the local E2E prerequisite by restarting the backend used by the current frontend with current-code MES module artifacts.

BDD: real runtime-control page exposes eDHR archive integrity after current-code backend restart -> Given the current frontend at `http://localhost:8081` points to local backend `127.0.0.1:48098` / When the backend is restarted after installing the T2 MES module and the test tenant logs in through `/login?redirect=/infra/monitors/runtime-control` / Then `/admin-api/infra/runtime-control/business-health` contains `edhr-archive-integrity`, the page renders `eDHR 归档完整性`, and no non-GET runtime-control requests are made.

GREEN: `mvn -pl yudao-module-mes -am -DskipTests install` -> PASS, current worktree MES module installed into the local Maven repository for the local current-code backend restart.

GREEN: `http://127.0.0.1:48098/actuator/health` -> PASS, returned `UP` after restarting the local backend with explicit local DB/Redis args.

GREEN: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.

GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS, `edhr-archive-integrity` / `eDHR 归档完整性` visible, item status `BLOCKED`, `writes=0`.

Reviewer decision: T3 approved. `BLOCKED` is the collector's business health status for the current data set, not an E2E infrastructure blocker. T4 independent verification may start.

## 2026-05-28 T4 Blocking Repair Worker

Scope was limited to T4 independent verification blocking issues AC-03, AC-08, and AC-11. No storage-retention dirty files, previous task artifacts, frontend files, staging, or commits were touched.

Files changed in this repair:

- `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionArchiveBusinessHealthCollector.java`
- `doc/tasks/20260528-edhr-archive-business-health/execution-log.md`
- `doc/tasks/20260528-edhr-archive-business-health/test-report.md`
- `doc/tasks/20260528-edhr-archive-business-health/task.md`
- `doc/tasks/20260528-edhr-archive-business-health/task-state.json`

BDD: mapper query failure stays in archive integrity collector -> Given the archive mapper or archive event mapper throws during a DB query / When runtime-control invokes the eDHR archive business health collector / Then the collector returns code `edhr-archive-integrity`, status `BLOCKED`, and a reason containing mapper/query context instead of letting runtime-control emit generic `business-health-collector-failed`.

BDD: missing mapper dependency blocks explicitly -> Given `archiveMapper` or `archiveEventMapper` is missing / When the collector runs / Then it returns code `edhr-archive-integrity`, status `BLOCKED`, and a reason naming the missing mapper dependency.

BDD: missing ARCHIVE_SEAL reports summary and archive id -> Given a SEALED archive has valid storage evidence but no `ARCHIVE_SEAL` event / When the collector runs / Then it returns `BLOCKED` with `archiveId`, `missingCount=1`, and `missingSummary=ARCHIVE_SEAL`, while storageRetention evidence remains a separate source-event responsibility.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 14 tests ran with 2 failures and 3 errors; mapper query exceptions escaped (`archive query failed`, `archive event query failed`), missing `archiveMapper` caused NPE, missing `archiveEventMapper` returned PASS, and missing `ARCHIVE_SEAL` reason lacked `missingCount=1`.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests run, 0 failures, 0 errors.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m json.tool .\doc\tasks\20260528-edhr-archive-business-health\task-state.json` -> PASS.

GREEN: `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java .\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollector.java` -> PASS.

T4 repair outcome: AC-03 and AC-08 blocking issues are repaired. AC-11 test-report was refreshed with actual BDD/RED/GREEN/E2E/regression evidence and no longer reports planning-only as the current status. Awaiting reviewer re-check; no staging or commit was performed.

## 2026-05-28 Reviewer T4 Repair Re-check

Reviewer rechecked the T4 blocking repair, reinstalled the repaired MES module into the local Maven repository, restarted the current-code backend on `127.0.0.1:48098`, and reran the real frontend E2E.

BDD: reviewer accepts T4 repair only after collector-owned BLOCKED and real UI evidence both pass -> Given T4 previously failed AC-03, AC-08, and AC-11 / When mapper dependency/query failure tests, runtime aggregation tests, no-coupling static tests, task evidence, and the real runtime-control page all pass against the repaired backend / Then independent re-verification may start, but the task remains incomplete until that gate and closeout preview pass.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests run, 0 failures, 0 errors.

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBusinessHealthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests run, 0 failures, 0 errors.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

GREEN: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m json.tool .\doc\tasks\20260528-edhr-archive-business-health\task-state.json` -> PASS.

GREEN: `git diff --check -- .\doc\tasks\20260528-edhr-archive-business-health .\script\tests\test_edhr_archive_business_health_contract.py .\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest.java .\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProBatchRecordExecutionArchiveBusinessHealthCollector.java` -> PASS.

GREEN: `git diff --check -- .\tests\e2e\runtime-control-edhr-archive-health-static.spec.js .\tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS.

GREEN: `mvn -pl yudao-module-mes -am -DskipTests install` -> PASS, repaired MES module installed into the local Maven repository for runtime E2E.

GREEN: `http://127.0.0.1:48098/actuator/health` -> PASS after restarting the current-code backend with explicit local DB/Redis args.

GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS, `edhr-archive-integrity` / `eDHR 归档完整性` visible, item status `BLOCKED`, `writes=0`.

Reviewer decision: T4 repair passed main reviewer re-check. Independent re-verification may start.

## 2026-05-28 Independent Re-verification PASS

Independent re-verifier superseded the prior FAIL report with a PASS report at `doc/tasks/20260528-edhr-archive-business-health/verification-report.md`.

BDD: independent re-verification approves release only when prior blockers are repaired -> Given AC-03, AC-08, and AC-11 had previously failed / When the independent reviewer reinspects the collector, tests, task evidence, git hygiene, backend health, and real E2E / Then AC-01..AC-12 must all have artifact and command evidence before task completion.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests run, 0 failures, 0 errors.

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBusinessHealthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests run, 0 failures, 0 errors.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m pytest script\tests\test_edhr_archive_business_health_contract.py -q` -> PASS, 5 passed.

GREEN: `node tests\e2e\runtime-control-edhr-archive-health-static.spec.js` -> PASS.

GREEN: `http://127.0.0.1:48098/actuator/health` -> PASS, `UP`.

GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_TEST_TENANT='测试租户'; $env:RUNTIME_CONTROL_E2E_TEST_USERNAME='aoteman'; $env:RUNTIME_CONTROL_E2E_TEST_PASSWORD='admin123'; node tests\e2e\runtime-control-edhr-archive-health.e2e.js` -> PASS, `edhr-archive-integrity` / `eDHR 归档完整性` visible, item status `BLOCKED`, `writes=0`.

GREEN: `$env:PYTHONUTF8='1'; python -X utf8 -m json.tool .\doc\tasks\20260528-edhr-archive-business-health\task-state.json` -> PASS.

GREEN: scoped `git diff --check` for backend task/test/prod paths and frontend E2E paths -> PASS.

Independent decision: PASS. No stage or commit was performed by the verifier.

## 2026-05-28 Task Closeout Preview

BDD: closeout preview must not delete requested formal docs or merge dirty worktrees -> Given the task is functionally complete and formal docs are part of the requested deliverable / When task-closeout-cleanup preview classifies cleanup and worktree state / Then cleanup apply must not run if it would delete required docs or merge/delete a worktree with unrelated dirty changes.

BLOCKED: `$env:PYTHONUTF8='1'; python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-archive-business-health --mode preview` -> blocked, exact reasons: current linked worktree has unrelated storage-retention pending changes, main worktree `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` is dirty, and current branch cannot be ff-only merged into `int_main`.

GREEN: repeated task-closeout preview after adding `Cleanup Keep` -> PASS for deletion safety, keep list includes all formal task docs plus backend collector/test/static contract, and delete set is `<none>`.

BLOCKED: repeated task-closeout preview after adding `Cleanup Keep` -> still blocked for worktree closeout, because unrelated storage-retention dirty files, dirty main worktree, non-ff merge guard, and conservative pending-change reporting for uncommitted kept task files remain.

Impact: cleanup apply, ff-only merge, and worktree removal were not run. Functional task completion and task-only commit remain allowed with explicit staging that excludes unrelated storage-retention files.

Final task status: completed.
