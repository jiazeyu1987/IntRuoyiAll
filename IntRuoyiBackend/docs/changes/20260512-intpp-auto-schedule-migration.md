# Change Request: Migrate IntPP Automatic Scheduling Into IntRuoyi

## Request Summary And Source

Source: user request in current thread.

Request: assess what IntRuoyi already has and what must be developed to migrate IntPP automatic scheduling into the current system.

Latest scope revision: do not migrate IntPP draft/version/snapshot management. Keep only one effective schedule version, use IntRuoyi production tasks as the current schedule result, and make IntPP scheduling logic adapt to IntRuoyi rather than reshaping IntRuoyi to match IntPP.

## Current Baseline Reviewed

- IntRuoyi current production scheduling assessment task: `doc/tasks/20260512-assess-production-scheduling`.
- IntRuoyi current MES scheduling implementation.
- IntPP repository at `D:\ProjectPackage\Int\IntPP`.

## Classification

Requirement change / cross-project migration assessment.

## Impact

### Product Impact

High. This changes production scheduling from manual task creation to automatic/assisted finite-capacity scheduling with material risk visibility and replan behavior. Multi-version schedule review is out of scope.

### Design Impact

Medium to high. IntRuoyi currently has a manual production task list/Gantt page. Migration needs new controls for generate/apply schedule, capacity source mode, material shortage visibility, dependency links, conflict display, and replan/lock handling. It must not add IntPP-style persisted version review or load-snapshot workflows.

### Data Impact

Medium to high. IntPP has schedule versions, schedule tasks, current schedule tasks, schedule snapshots, calendar rules, daily planned/actual capacity, capacity audit, material issue items, BOM children, material supply cache, and capacity bindings. Under the revised scope, IntRuoyi should not adopt IntPP's version/draft/snapshot persistence. Data work should focus on scheduler adapters, capacity plan/actual/audit data if not already representable, material shortage calculations, route dependency metadata if needed, and transactional writes to current production tasks.

### API Impact

High. IntRuoyi needs new backend APIs/services for generate-and-apply current schedule, optional non-persisted preview, fact-based replanning, capacity mode selection, material shortage analysis, conflict analysis, and route/capacity prerequisite validation. Version listing/detail, publish/load-version, and saved snapshot APIs are out of scope.

### Test Impact

High. Required coverage includes backend unit tests for capacity/material/route constraints, API tests for schedule generation/apply/replan, and Playwright E2E tests with real data for the frontend scheduling workflow.

### Release Impact

High. This should not be shipped as a single unscoped transplant. It touches schema, backend behavior, frontend user paths, and operational production planning data.

### Operations Impact

Medium to high. Missing or stale capacity/material/route data must fail fast and be visible to users. Operational readiness requires import/sync ownership for capacity, actual output, BOM/material availability, and route/process topology data.

## Decision

Split.

Rationale: IntPP has a broad automatic scheduling subsystem. IntRuoyi currently has manual production scheduling plus supporting MES master data. A direct one-step migration would create unclear schema ownership, unclear source-of-truth rules, and insufficient verification surface. Split the migration into data/model alignment, backend scheduling foundation, scheduling engine, frontend workflow, and verification phases.

Revised constraint accepted: single effective schedule only. Do not migrate IntPP schedule versions, persisted drafts, snapshots, version comparison, or load-saved-schedule workflows unless separately approved later.

## Required Approvals

Implementation approval is required before production code changes. Schema additions and scheduling behavior rules require explicit approval before implementation. Any proposal to add persisted schedule versions, drafts, or snapshots must be treated as a new scope change.

## Downstream Skill Reruns

For implementation, rerun BDD/TDD planning and then route separate tasks through database-schema-delivery, backend-api-delivery, frontend-feature-delivery, and quality-assurance-test-suite.

## Blockers And Next Action

No code implementation should start until the first migration slice is selected.

Recommended next action: choose the first slice as "single-version data/model alignment" and produce a concrete mapping from IntPP scheduler inputs/outputs to IntRuoyi work orders, production tasks, route/process, BOM/material stock, resources, and calendar modules.

## Maximum Reuse Direction

Use IntRuoyi work orders, production tasks, routes, BOM/material stock, workstations, workers, machines, workshops, calendars, menus, and permissions as the baseline. Do not duplicate these with IntPP table names. In particular, do not add persisted schedule draft/version/snapshot tables under the current scope.

The required new work is the scheduling layer around those existing systems: IntRuoyi-to-scheduler adapters, single-version current-task writer, capacity plan/actual/audit data where required, material shortage service, route dependency resolver, scheduling engine port, replan/lock rules, conflict validation, auto-schedule APIs, frontend controls, `quantityScheduled` synchronization, and real-data tests.
