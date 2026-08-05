# AC-M21 过程检验记录汇集修复执行日志

## User Intent

- 用户要求对 AC-M21「系统汇集过程检验记录」不符合项进行修复。
- 修复重点：从代码层补齐最终确认修订、结构化过程检验记录、任务/轮次/版本追溯、重复排除和跨租户隔离。

## Gate Reads

- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/powershell-encoding.md`。
- 已读取 `docs/backend-development.md`。
- 已读取 `docs/database-rules.md`。
- 已读取 `docs/experience-index.md`，命中 `MES PQC 项目级检验快照门禁`。
- 已读取技能：
  - `backend-api-delivery`
  - `database-schema-delivery`
  - `quality-assurance-test-suite`
  - `project-experience-consolidation`

## BDD / TDD

- BDD: Approved PQC review creates structured process inspection aggregation -> Given a submitted PQC inspection event with structured item results and a team leader approval, When the approval is completed, Then the system persists process inspection aggregate detail rows traceable to tenant, event, review, task, round, regulation version, item, piece and revision.
- BDD: Non-final or unapproved PQC submissions are excluded -> Given pending, rejected, self-review-blocked, old revision, duplicate, or cross-tenant PQC data, When aggregation runs, Then only the final approved revision is aggregated and all other data is excluded without default success.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because AC-M21 aggregate detail DO/mapper/schema were missing; command also exposed unrelated existing module test compile blockers.
- GREEN: selected production `javac` for AC-M21 changed production classes -> PASS.
- GREEN: selected AC-M21 test `javac` -> PASS.
- GREEN: JUnit Console `MesPqcProcessInspectionAggregationServiceTest` -> PASS, 7/7 tests successful.
- GREEN: JUnit Console `MesProcessPoolSchemaTest` + `MesQaPqcSchemaTest` -> PASS, 5/5 tests successful.
- GREEN: backend API evidence validator -> PASS.
- GREEN: database schema evidence validator -> PASS.
- GREEN: experience index keyword scan for AC-M21/PQC process inspection aggregation -> PASS.

## Milestone Updates

- Created task documentation and captured applicable PQC evidence gate.
- Added `mes_pqc_process_inspection_aggregate_detail` DO/Mapper/schema and H2 fixture.
- Updated aggregation service to validate tenant/event/task/detail identity, CAS mark PQC record aggregated, CAS confirm PQC task, and insert structured aggregate rows.
- Updated AC-M21 unit and schema tests.
- Consolidated reusable PQC process-inspection aggregation gate into `docs/backend-development.md` and routed keywords in `docs/experience-index.md`.

## Verification Evidence

- `selected production javac PASS`.
- `selected AC-M21 test javac PASS`.
- `MesPqcProcessInspectionAggregationServiceTest`: 7 tests successful.
- `MesProcessPoolSchemaTest` + `MesQaPqcSchemaTest`: 5 tests successful.
- `validate_backend_api.py --evidence ...backend-api-evidence.md`: PASS.
- `validate_database_schema.py --evidence ...database-schema-evidence.md`: PASS.
- `rg -n "AC-M21|mes_pqc_process_inspection_aggregate_detail|aggregateApprovedPqcSubmission" docs/experience-index.md docs/backend-development.md`: PASS.
- Stale blocker recheck: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` from `IntRuoyiBackend` -> TIMEOUT after 304s; `jcmd 48672 Thread.print` showed Maven main thread in `java.io.WinNTFileSystem.canonicalize0` while resolving dependencies through Maven/Aether local repository tracking, before new Surefire PASS evidence was produced.
- Stale blocker process cleanup: stopped only the AC-M21 recheck-owned Maven processes `48672` and `56372`; unrelated Java/Maven processes were left untouched.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m21-process-inspection-aggregation-fix --mode preview` -> READY preview; keep `task.md`, `execution-log.md`, `verification-report.md`; delete candidates are task-local intermediate evidence files and `javac-classes`; no blocked paths or warnings.
- Removed task-owned temporary classpath and JVM crash log files from git-visible status. Remaining `javac-classes` directory under ignored backend task temp path is not reported by git and is not part of deliverable.

## Blockers

- Workspace baseline before this task was not clean: `git status --short --branch` reported branch `int_main` ahead of `origin/int_main` by 3 commits and unrelated untracked task directories. Current task will avoid unrelated paths; commit/push completion may need a separate baseline/coordination step.
- Current workspace later reported branch `int_main` ahead of `origin/int_main` by 13 commits and many modified/untracked files not owned by this task.
- Full Maven module test verification is blocked by unrelated compile errors in existing test files.
- Full MES production compile is blocked by Windows native memory/page-file exhaustion during full javac; low-memory retry timed out and the task-owned Maven PID was stopped.
- 2026-08-05 12:38 stale blocker Maven recheck remains blocked by local Maven dependency-resolution filesystem stall (`WinNTFileSystem.canonicalize0`) and produced no new target Surefire PASS.
- Cleanup apply is intentionally not run because task status remains `blocked`; apply requires `ready_for_closeout` or `completed`.
