# Dev Plan

## Task Graph

### T1
- task_id: T1
- title: Binding source production implementations
- objective: Provide real Spring beans for device-account route bindings and employee fixed-template bindings.
- dependency_ids: []
- affected_paths: `IntRuoyiBackend/yudao-module-mes`
- write_scope: MES frontline services, mapper/tests, migration if schema-backed config is missing.
- acceptance_ids: AC-01
- validation_steps: target JUnit plus schema/static checks.
- done_definition: production beans are injected, tests fail fast on missing/invalid bindings, no mock/fallback beans.

### T2
- task_id: T2
- title: Frontline real production/PQC submit
- objective: Replace validate-only UI success with real submit payload and PQC submit path.
- dependency_ids: [T1]
- affected_paths: `IntRuoyiFronted/src/views/mes/pro/feedback`, `IntRuoyiFronted/src/api/mes/pro/feedback`, MES submit backend if payload gaps exist.
- write_scope: fixed-template panel, API types, focused static contracts, submit service tests.
- acceptance_ids: AC-02, AC-03
- validation_steps: frontend static RED/GREEN, backend target tests.
- done_definition: production and PQC submit call formal APIs and surface failures.

### T3
- task_id: T3
- title: FIFO orchestration for production work orders
- objective: Wire existing FIFO allocation service into formal API/job using production work orders sorted by planned start time.
- dependency_ids: [T2]
- affected_paths: MES process-pool services/controllers/mappers/tests.
- write_scope: FIFO orchestration service/controller/VO/tests.
- acceptance_ids: AC-04
- validation_steps: target JUnit for sort order, missing planned start fail-fast, allocation lines.
- done_definition: API/job allocates available fragments to production work orders in FIFO order.

### T4
- task_id: T4
- title: Automatic review-copy rules
- objective: Generate review-copy mappings from formal limit configuration and clamp values while preserving original records.
- dependency_ids: [T2]
- affected_paths: MES process-pool review-copy services/controllers/frontend.
- write_scope: review-copy auto service/API/page/static/backend tests.
- acceptance_ids: AC-05
- validation_steps: target JUnit and frontend static contract.
- done_definition: reviewer can generate rule-driven clamped copy without hand-entering JSON mappings.

### T5
- task_id: T5
- title: Team-leader workbench
- objective: Add real read-only workbench surface for process-pool monitoring.
- dependency_ids: [T2, T3, T4]
- affected_paths: MES process-pool frontend/backend route/API/menu tests.
- write_scope: workbench API/read model/frontend route/page/static contracts.
- acceptance_ids: AC-06
- validation_steps: backend read-model test and frontend static contract.
- done_definition: workbench route displays real events/statuses and no mock/browser storage.

### T6
- task_id: T6
- title: Full-chain E2E
- objective: Verify the real page path on worktree runtime ports.
- dependency_ids: [T1, T2, T3, T4, T5]
- affected_paths: `IntRuoyiFronted/tests/e2e`, `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`, task evidence.
- write_scope: Playwright spec/evidence plus focused product regressions exposed before the first business write.
- acceptance_ids: AC-07
- validation_steps: start/verify `8082/48082`, run real Playwright or record exact prerequisite blocker.
- done_definition: E2E passes or is formally blocked with missing prerequisite and impact.

## Conflict Notes
- T2 touches `FrontlineFixedTemplatePanel.vue`, which had concurrent edits in the main workspace; work must stay in this clean worktree.
- T3/T4/T5 all touch process-pool APIs and should be sequenced after T2 to avoid read/write contract drift.
