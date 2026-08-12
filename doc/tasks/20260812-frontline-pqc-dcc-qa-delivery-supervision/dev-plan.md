# Development Plan

本计划以 doc/tasks/20260811-frontline-pqc-dcc-qa-agent-design/ 的已通过设计包为权威基线。主管 Agent 只负责编排、评审、验证门禁、fast-forward 合并和清理；工作子 Agent 每次只领取一个任务，不得自行合并 int_main 或删除 worktree。

## Execution Rules

- max_worker_subagents: 3
- concurrency_slots_observed: 4 total agents, so supervisor + 3 workers is supported
- wave_order_locked: true
- worktree_root: D:\IntRuoyiWorktree\
- branch_base: latest int_main that already contains every completed prerequisite task
- merge_policy: task branch must absorb latest int_main, rerun validation, then merge to int_main by fast-forward only
- shared_file_serial_ownership: shared service, mapper, controller, VO, page, API type, schema, and task artifact files are owned by exactly one running task at a time; same-file successors start only after the previous owner is reviewed, verified, committed, and merged
- no_fill_parallelism: if a wave has fewer than 3 ready tasks, later wave tasks must not be started to fill unused capacity
- no_fallback: missing schema, contracts, test data, ports, credentials, or interface prerequisites block the task

## Wave Graph

| Wave | Tasks | Required completed before wave starts |
| --- | --- | --- |
| Wave 0 | C00 | design release gate passed |
| Wave 1 | DF01, DF02, DF03, DF05 | C00 |
| Wave 2 | DF04 | DF02, DF03 |
| Wave 3 | DF06 | DF04, DF05 |
| Wave 4 | DF07 | DF05, DF06 |
| Wave 5 | DF08 | DF07 |
| Wave 6 | DF09 | DF08 |
| Wave 7 | DF10, DF11 | DF08, DF09 |
| Wave 8 | INT12 | DF01, DF02, DF03, DF04, DF05, DF06, DF07, DF08, DF09, DF10, DF11 |
| Wave 9 | VAL13 | INT12 |

## Task Graph

### C00

- task_id: C00
- title: Contract and schema baseline
- objective: Freeze the formal DCC-QA-PQC schema and migration contract so downstream tasks can rely on route-DCC binding, active-order QA snapshots, rule-key task identity, canonical submission hash, and unique PQC event linkage.
- dependency_ids: []
- affected_paths:
  - IntRuoyiBackend/sql/mysql/20260812*_mes_*.sql
  - IntRuoyiBackend/sql/mysql/20260812*_mes_*_preflight.sql
  - IntRuoyiBackend/sql/mysql/20260812*_mes_*_backfill.sql
  - IntRuoyiBackend/sql/mysql/20260812*_mes_*_postflight.sql
  - IntRuoyiBackend/sql/mysql/20260812*_mes_*_rollback.sql
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesQaPqcSchemaTest.java
  - task-owned preflight, backfill, postflight, rollback, approval-list, blocker-list, row-count, and hash report artifacts
- write_scope:
  - Add a successor migration only; do not rewrite already-executed migrations.
  - Add migration static/schema tests and migration verification artifacts.
  - Do not change frontend files, runtime services, QA business logic, DCC backend, or downstream task implementation files.
- acceptance_ids: [AC-01, AC-03, AC-04, AC-05, AC-07, AC-09, AC-10, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  - Run task-owned preflight, backfill, postflight, and rollback dry-run fixtures and record blocker count, affected rows, and hash evidence.
- done_definition: Schema tests pass, migration artifacts prove no guessed current-version backfill, generated constraints match data-migration-contract.md, no forbidden binding/context/item-type table is added, and the task is reviewed, committed, merged fast-forward, and documented.

### DF01

- task_id: DF01
- title: Active order selection identity
- objective: Return every effective active-order row and expose activeOrderId as the only page selection identity, without filtering by PQC task state or deduplicating by work order plus route.
- dependency_ids: [C00]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlineActiveOrderCandidate*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlineActiveOrderRespVO*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlineDeviceAccountController*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlinePqcContextServiceImpl* active-order list segment only
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlineActiveOrderControllerTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlinePqcContextServiceTest*
- write_scope:
  - Only active-order list candidate, response, controller conversion, list segment, and tests.
  - Do not edit DF02 resolver, DF06 creation/reactivation logic, DF10 process projection, or frontend components.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: Three pressure-pump active orders are selectable when effective, duplicate workOrder/route pairs stay as separate activeOrderId rows, no PQC task state filters the list, and DF01 changes are merged before DF10 takes the shared service file.

### DF02

- task_id: DF02
- title: Active-order route snapshot resolver
- objective: Resolve the selected activeOrderId to the order's fixed route snapshot and QA snapshot fields inside the current tenant, without accepting client route overrides.
- dependency_ids: [C00]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/ActiveOrderSnapshotResolver*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlineActiveOrderSnapshotResolverTest*
- write_scope:
  - Add an independent resolver and tests only.
  - Do not edit DF01 list method, DF06 creation/reactivation logic, or shared mappers beyond read-only use.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-09, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderSnapshotResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: requireEffective(activeOrderId) returns the route and QA snapshots for valid active orders, fails on missing/removed/cross-tenant/missing-route cases, performs no writes, and is merged before DF04/DF10 consume it.

### DF03

- task_id: DF03
- title: Route to DCC project binding
- objective: Provide the formal route-DCC relationship API and route edit UI configuration with monotonic versioning, optimistic concurrency, and permission-aware bind/rebind/unbind behavior.
- dependency_ids: [C00]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesRouteDccProjectBinding*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesRouteDccProjectBindingServiceTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesRouteDccProjectBindingControllerTest*
  - IntRuoyiFronted/src/views/mes/pro/route/RouteEditPage.vue
  - IntRuoyiFronted/src/views/mes/pro/route/RouteFormContent.vue
  - IntRuoyiFronted/src/api/mes/pro/route/index.ts
  - IntRuoyiFronted/tests/e2e/mes-route-dcc-project-binding-static.spec.cjs
- write_scope:
  - Route-DCC binding DO/mapper/service/controller/VO/tests and existing route edit frontend/API.
  - Do not edit DCC backend, QA service, or one-line frontline PQC aggregation.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-07, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  - node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs
  - Real Playwright route-edit path for initial concurrent bind, rebind conflict, change binding, and update-only unbind; API only for final read-only confirmation.
- done_definition: GET/PUT/DELETE route-DCC binding APIs and route UI work with expectedVersion, permissions, tombstone versioning, and no DCC-to-MES backend dependency.

### DF04

- task_id: DF04
- title: Unique enabled DCC project resolver
- objective: Resolve one enabled, same-tenant DCC project from the formal route-DCC relationship and fail fast on missing, duplicate, disabled, deleted, or cross-tenant ambiguity.
- dependency_ids: [DF02, DF03]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/DccProjectResolver*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlineDccProjectResolverTest*
- write_scope:
  - Add resolver class and tests only.
  - Do not change DCC module schema/VO, create ports, or use product/material matching.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-07, AC-09, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineDccProjectResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: Resolver returns dccProjectCodeId/projectCode/projectName only for one enabled relationship and keeps MES-to-DCC dependency direction.

### DF05

- task_id: DF05
- title: DCC directly reads QA regulation relation
- objective: Reuse mes_qa_inspection_regulation.dcc_project_code_id as the only DCC-QA relationship and align QA management and DCC project list UI with a direct bulk QA status lookup.
- dependency_ids: [C00]
- affected_paths:
  - IntRuoyiFronted/src/views/mes/qc/template/QaRegulationPage.vue
  - IntRuoyiFronted/src/api/mes/qc/template/index.ts
  - IntRuoyiFronted/src/views/dcc/project/ProjectCodeTabPanel.vue
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesQaInspectionRegulationServiceImpl* save/publish segment only
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesQaInspectionRegulationServiceTest*
  - IntRuoyiFronted/tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs
  - IntRuoyiFronted/tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs
- write_scope:
  - QA management types/calls, DCC project list QA status column, and QA save/publish segment.
  - Do not create a DCC-side binding table or add QA fields to DCC backend VO.
  - Release QA service publish/read sections to DF07 after merge.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-07, AC-11, AC-12, AC-13]
- validation_steps:
  - node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs
  - node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: QA dropdown/status uses DCC project code relation directly, DCC list makes one bulk MES status call from frontend, and no product/route/MES-process inference remains.

### DF06

- task_id: DF06
- title: Active order QA version lock
- objective: Lock dccProjectCodeId, qaRegulationId, and qaRegulationVersionId during new active-order creation and generate rule-key PQC tasks in the same transaction, while removed-row reactivation preserves the old snapshot and task history.
- dependency_ids: [DF04, DF05]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesProcessPoolActiveOrderDO*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesTeamLeaderActiveOrderService*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesProcessPoolActiveOrderMapper*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesPqcInspectionTask*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesTeamLeaderActiveOrderServiceTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesProcessPoolActiveOrderMapperTest*
- write_scope:
  - Active-order DO/mapper, creation/reactivation service, rule-driven task generation, and tests.
  - Do not edit QA process read assembly, frontend pages, or route-DCC management.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-08, AC-09, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: New active orders atomically lock current enabled DCC and current PUBLISHED QA version, create FIRST/PATROL_AM/PATROL_PM/FINAL tasks correctly, and removed reactivation never relocks today's current version.

### DF07

- task_id: DF07
- title: Locked QA version processes
- objective: Add a frontline runtime contract that reads an order-locked QA version and returns QA-owned processes from PUBLISHED or RETIRED versions without route-process existence checks or DCC current-enabled checks.
- dependency_ids: [DF05, DF06]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesQaInspectionRegulationServiceImpl* locked-version/process-read segment only
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesQaInspectionRegulation*Mapper*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesQaInspectionRegulationServiceTest*
- write_scope:
  - QA locked-version read and process assembly segment after DF05 releases QA service.
  - Do not edit route process logic, DCC current resolver, item/equipment assembly, or frontend.
  - Release QA service item assembly segment to DF08 after merge.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-07, AC-09, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: getLockedVersionForOrder(dccProjectCodeId, qaRegulationId, qaRegulationVersionId) validates same-tenant ownership and PUBLISHED/RETIRED status, returns QA processes sorted by sort ASC,id ASC, and never checks MES route processes.

### DF08

- task_id: DF08
- title: QA process inspection item aggregation
- objective: Aggregate QA business inspection items by qaProcessId + itemCode with full published item fields, rule-key validation, equipment options, resultType contract, and type-row consistency.
- dependency_ids: [DF07]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesQaInspectionRegulationServiceImpl* item/equipment assembly segment only
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesQaInspectionRegulationServiceTest*
- write_scope:
  - QA item, equipment, rule validation, and published-version item response assembly after DF07 releases QA service.
  - Do not create item-type tables, reduced DTOs, or frontend projections.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-08, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: Pressure-pump version returns 18 business items from 51 type rows with full source fields, BOOLEAN/NUMERIC/TEXT only, PATROL_AM/PATROL_PM rule integrity, and no second reduced item model.

### DF09

- task_id: DF09
- title: PQC task and production event overlay
- objective: Overlay PQC task status/options and candidate production submit events by activeOrderId + regulationVersionId + qaProcessId + inspectionRuleKey without hiding QA processes or merging PATROL_AM/PATROL_PM.
- dependency_ids: [DF08]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesPqcInspectionTaskMapper*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlinePqcTaskOverlay*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlineProductionSubmitCandidate*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlinePqcTaskOverlayTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlineProductionSubmitCandidateTest*
- write_scope:
  - PQC task mapper/overlay and production event/process snapshot helper only.
  - Do not filter outer QA processes/items and do not edit final controller/page submit flow.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-08, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: Overlay returns NOT_CREATED when tasks are absent, preserves distinct task options for all four rule keys, validates production event ownership through active-order process snapshots, and produces stable business sorting.

### DF10

- task_id: DF10
- title: Backend process page projection
- objective: Assemble the dedicated frontline PQC process response from active-order snapshot, locked QA version, QA processes/items, task overlay, and production candidates.
- dependency_ids: [DF02, DF07, DF08, DF09]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlinePqcContextService*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlinePqcContextServiceImpl*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlinePqcProcessRespVO*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlinePqcContextServiceTest*
- write_scope:
  - Dedicated PQC response assembler and service projection after DF01 releases the shared service file.
  - Do not edit controller, frontend component, production-route response model, schema, or upstream mappers.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-07, AC-09, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- done_definition: listProcessesByActiveOrder(activeOrderId) uses batched reads, returns all locked QA processes/items with task state overlay, and does not call management current QA or route-process QA validation.

### DF11

- task_id: DF11
- title: Frontend item and task projection
- objective: Align frontend API types, request identity, item projection, resultType union, inspection rule key union, task ordering, and state display for the dedicated PQC process response.
- dependency_ids: [DF08, DF09]
- affected_paths:
  - IntRuoyiFronted/src/api/mes/pro/feedback/**
  - IntRuoyiFronted/src/api/mes/qc/template/index.ts
  - IntRuoyiFronted/tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs
- write_scope:
  - Frontline PQC API types, request helpers, static contract tests, and typecheck fixes.
  - Do not edit page components; INT12 owns final page behavior.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-08, AC-11, AC-12, AC-13]
- validation_steps:
  - node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs
  - pnpm ts:check
- done_definition: Frontend uses getPqcProcesses(activeOrderId), retains full item/task/rule/equipment fields, preserves AM/PM task identity, and typecheck passes.

### INT12

- task_id: INT12
- title: Final frontline PQC integration
- objective: Integrate controller, page, personnel switch, equipment selection, submission, actual employee signature, resultType validation, canonical hash idempotency, correction/release consumers, and production event attribution.
- dependency_ids: [DF01, DF02, DF03, DF04, DF05, DF06, DF07, DF08, DF09, DF10, DF11]
- affected_paths:
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesFrontlineDeviceAccountController*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/Pqc*Switch*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesProFrontlineFeedbackSubmitService*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesProcessPoolPqcInspectionCorrectionService*
  - IntRuoyiBackend/yudao-module-mes/src/main/java/**/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlinePqcEmployeeSwitchServiceTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesProFrontlineFeedbackSubmitServiceTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesFrontlinePqcSubmissionConcurrencyTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesProcessPoolPqcInspectionCorrectionServiceTest*
  - IntRuoyiBackend/yudao-module-mes/src/test/java/**/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest*
  - IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue
  - IntRuoyiFronted/tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs
  - IntRuoyiFronted/tests/e2e/frontline-pqc-formal-submit-static.spec.js
- write_scope:
  - Final integration endpoints, page component, personnel/submit VOs and services, resultType consumers, and final integration tests.
  - Do not change upstream schema, route-DCC mapper, QA locked-version assembly, or DF11 API contracts except through approved integration fixes.
- acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13]
- validation_steps:
  - mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcEmployeeSwitchServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcSubmissionConcurrencyTest,MesProcessPoolPqcInspectionCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  - node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs
  - node tests/e2e/frontline-pqc-formal-submit-static.spec.js
  - pnpm ts:check
  - Real Playwright path for active-order selection, QA process display, employee switch, equipment selection, formal submit, and separate PATROL_AM/PATROL_PM behavior.
- done_definition: End-to-end frontline PQC uses activeOrderId and QA process/task identity everywhere, actual employee signature is authoritative, canonical hash/idempotency works, and INT12 is merged before VAL13 starts.

### VAL13

- task_id: VAL13
- title: Independent acceptance
- objective: Independently verify the fully merged chain against the design package, test plan, real UI paths, JUnit/static tests, SQL/migration evidence, and final no-push/no-deploy constraints.
- dependency_ids: [INT12]
- affected_paths:
  - doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/test-report.md
  - read-only access to production code, tests, task documents, local runtime, and final merged int_main
- write_scope:
  - VAL13 may write only independent acceptance evidence to test-report.md and its own task-owned validation notes if supervisor grants that exact doc path.
  - VAL13 must not modify production code, schema, frontend, backend, tests, migrations, shared business data, or implementation task records.
- acceptance_ids: [AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14]
- validation_steps:
  - Run the VAL13 backend command from the design task.
  - Run each task-added frontend static test directly with node and run pnpm ts:check.
  - Execute C00 SQL preflight/postflight/rollback dry-run evidence checks.
  - Use Playwright on real local frontend/backend paths for route-DCC config, DCC QA link/status, three active orders, 8 QA processes, 18 business items, personnel switch, equipment, formal submit, and AM/PM separation.
  - Use APIs only as final read-only confirmation.
- done_definition: Independent tester that did not implement INT12 records pass/fail evidence for every acceptance criterion, reports exact defects back to the responsible task if any fail, and makes no production-code writes.
