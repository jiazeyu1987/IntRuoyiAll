# Execution Log

## User Intent

- 用户反馈一线 PQC 仍报错：`当前工序缺少已发布 QA 检验规程，activeOrderId=39，routeProcessId=980632，processId=922986`。

## BDD

- BDD: Active order process with formal published QA regulation should build PQC context -> Given activeOrderId `39` has a pending PQC task for routeProcessId `980632` and processId `922986`; When frontline PQC context is requested for the current process; Then the backend must resolve the matching published QA inspection regulation from the formal route/process/product source and return executable context, or fail fast with the exact missing formal prerequisite if no published regulation exists.

## Command And Evidence Log

- Loaded `bug-regression-fix-loop` and bug evidence contract.
- Loaded backend and database rules before backend/database diagnosis.
- Loaded task closeout rules before creating task documents.
- Read `docs/experience-index.md` and applied `docs/backend-development.md#mes-pqc-项目级检验快照门禁` to the task gate.
- Read local runtime, worktree, and branch port rules before refreshing `int_main` on `48081`.

## Diagnosis

- Read-only database evidence for active order `39`:
  - `mes_pro_process_pool_active_order`: `work_order_id=980026`, `route_id=980091`, `route_version_id=622`, product `924008`.
  - `mes_pro_process_pool_active_order_process_snapshot`: only `route_process_id=980631`, `process_id=922985`.
  - `mes_pqc_inspection_task`: pending tasks `211`-`214` are bound to `980631/922985`, regulation version `36`.
  - Current route `980091` contains `route_process_id=980632`, `process_id=922986`, but it is not in active order `39`'s frozen snapshot and has no published QA regulation.
- Root cause: `listProcessesByActiveOrder` enumerated every current `mes_pro_route_process` row for the route and then resolved QA regulations for those rows. That incorrectly included `980632/922986` in active order `39`, producing a missing-regulation error for a process the order never froze.
- Formal fix: read `MesProcessPoolActiveOrderProcessSnapshotMapper.selectListByActiveOrderId(activeOrderId)` as the authoritative active-order process set; resolve only matching current route-process metadata by `(routeProcessId, processId)`; fail fast on missing or inconsistent snapshot identity.

## RED

- BDD: Active order process set must follow frozen process identity -> Given active order `39` freezes only `980631/922985` while current route `980091` also has `980632/922986`; When the PQC active-order process context is requested; Then only the frozen process is resolved and the non-frozen process is never queried for QA regulation.
- RED: `mvn surefire:test "-Dtest=MesFrontlinePqcContextServiceTest#shouldLoadOnlyProcessesFrozenInActiveOrderSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false"` -> FAIL, before the fix the service called `processService.getProcessMap` for both current route processes (`5001` and `5002`) and attempted to resolve the non-frozen process.

## GREEN

- GREEN: `mvn surefire:test "-Dtest=MesFrontlinePqcContextServiceTest#shouldLoadOnlyProcessesFrozenInActiveOrderSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS, 1 test.
- GREEN: `mvn surefire:test "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS, 30 tests.
- GREEN: `javac @doc/tasks/20260807-pqc-missing-qa-regulation-active-order-39/javac-mes-frontline-pqc-service.args` -> PASS, generated `MesFrontlinePqcContextServiceImpl.class` and five companion classes.
- GREEN: outer runtime Jar verification -> PASS, `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` occurs once and is stored with `compress_type=0`; all six service implementation class files are present.
- GREEN: `GET http://127.0.0.1:48081/actuator/health` -> PASS, new PID `68664`, status `UP`; command line loads `backend-latest-20260807-2338-pqc-active-order-snapshot.jar`.

## Blockers

- A later broad reactor compile was started while another MES Maven test process was already running in the same workspace. The current task compile was diagnosed in `javac` class-file output and stopped after remaining in `FileDescriptor.close` for several minutes; no Java compilation error was emitted. The targeted test class and direct service compilation both passed, and the runtime Jar was built from the resulting verified service class.
- Authenticated UI/API verification of active order `39` was not run because no task-owned logged-in tenant/account session was available. The unauthenticated route probe would only prove security filtering, not the business path.

## Closeout

- Bug evidence validator: `validate_bug_regression.py --evidence .../bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- `task-closeout-cleanup --mode preview` -> PASS, no blocked paths or warnings; kept the three core task documents.
- `task-closeout-cleanup --mode apply` -> PASS; removed task-owned classpath files, javac args, extracted runtime Jars/classes, and temporary bug evidence after its conclusions were archived here and in `verification-report.md`.
- Final runtime recheck: PID `68664` remains the `48081` listener and `/actuator/health` is `UP`.
- Project experience consolidated into existing `docs/backend-development.md#mes-pqc-项目级检验快照门禁` and `docs/experience-index.md`; no new long-term experience document was created.
- Final task status: `completed`.
