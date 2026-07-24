# Task: Assess IntPP Auto Scheduling Migration

## Goal

Compare IntPP automatic scheduling capabilities against the current IntRuoyi MES scheduling implementation and identify which required systems already exist, which are partially present, and which must be developed before migration.

## Milestones

- [x] M1: Previous task state checked and confirmed completed.
- [x] M2: Task documentation created before assessment.
- [x] M3: Locate IntPP automatic scheduling backend, frontend, data, and tests.
- [x] M4: Map IntPP capabilities to IntRuoyi existing modules.
- [x] M5: Identify missing systems, migration blockers, and recommended split.
- [x] M6: Record final evidence and status.

## Expected Verification

- Search IntPP for automatic scheduling, production scheduling, capacity, material, BOM, calendar, and conflict logic.
- Search IntRuoyi current MES scheduling, route, calendar, workstation, BOM, material stock, and work order systems.
- Produce a migration gap matrix: existing, partial, missing.
- Record whether the change should be accepted, split, blocked, or deferred before implementation.

## Current Status

Completed. Assessment only; no production code was changed.

## Final Verification

- `python C:/Users/BJB110/.codex/skills/change-request-triage/scripts/validate_change_request.py --evidence docs/changes/20260512-intpp-auto-schedule-migration.md` -> PASS.
- Branch landing target: `ruoyi-vue-pro` branch `docs/intpp-auto-schedule-migration`.
- Storage path restored to `doc/tasks/20260512-intpp-auto-schedule-migration-assessment/` and `docs/changes/20260512-intpp-auto-schedule-migration.md`.

## Assessment Summary

### Existing In IntRuoyi

- Production work order model and APIs exist, including `quantityScheduled`.
- Production task scheduling module exists with menu, backend service, permissions, list, edit, and Gantt pages.
- Process route, route product, route process, route BOM, product BOM, workstation, worker, machine, workshop, calendar, and material stock modules exist as MES supporting master data.
- Current production task creation validates work order, workstation, route, process, and item existence before inserting a task.

### Partially Present In IntRuoyi

- Capacity-related master data exists through workstations, workers, machines, workshops, calendars, and routes, but no unified finite-capacity planning model was found in the current scheduling path.
- Material and BOM data exists through product BOM, work order BOM, route product BOM, and material stock modules, but no material availability or shortage check was found in task scheduling.
- Gantt rendering exists, but task dependency links are disabled and returned as an empty list.
- `quantityScheduled` exists on work orders, but no automatic scheduling path was found that recalculates or writes it from generated tasks.

### Missing For IntPP Automatic Scheduling Migration Under Current-System-First Scope

- Automatic scheduling command/API and service layer in IntRuoyi.
- Single-version current schedule write strategy that uses IntRuoyi production tasks as the only effective schedule result. IntPP-style schedule versions, draft tables, snapshots, publish/load workflows, and version history are out of scope unless explicitly re-approved.
- Capacity source modes equivalent to IntPP (`DEFAULT`, `PLANNED`, `ACTUAL`) and fail-fast rules for missing required actual/planned capacity.
- Daily line/process capacity plan, actual capacity, audit trail, shift split, rest-day/weekend calendar rules, and worker/process group constraints.
- Material refresh, BOM explosion, supply cache, material shortage summary, risky-plan status, and order-level shortage annotations.
- Route topology/dependency resolution from IntRuoyi route/process data into scheduler task links.
- Conflict detection for overlapping workstation/process/line capacity and missing route/capacity/material prerequisites.
- Lock/freeze/manual-window rules for fact-based replanning.
- Frontend controls for generating/applying the current schedule, capacity source mode, result review, material risk display, and replan workflows.
- Backend unit tests and frontend E2E tests covering automatic scheduling behavior.

## Recommended Decision

Split the migration and keep IntRuoyi as the stable integration model. IntPP has a real automatic scheduling subsystem, while IntRuoyi currently has manual production task scheduling plus supporting master data. The migration must port useful IntPP scheduling logic into an adapter that reads/writes IntRuoyi models. It must not force IntRuoyi to adopt IntPP's schedule version/draft/snapshot model because IntRuoyi also needs to adapt to other systems. The practical split is:

1. Data/model alignment: map IntPP scheduler input/output concepts onto IntRuoyi work order, task, route, BOM, stock, resource, and calendar models.
2. Backend scheduling foundation: single-version current-task write strategy, capacity/calendar/material query services, route dependency resolver, and fail-fast validations.
3. Scheduling engine: finite capacity, shift/rest-day rules, material shortage/risky-plan behavior, and lock/freeze replan behavior.
4. Frontend migration: schedule generation/apply, Gantt dependency display, capacity mode selection, shortages, conflicts, and replan controls.
5. Verification: backend unit tests, API integration tests, and real-path Playwright E2E tests.

## Maximum Reuse Development Scope

When maximizing reuse of IntRuoyi, do not copy IntPP production-order, BOM, stock, route, final task, schedule version, draft, or snapshot tables one-for-one. Use IntRuoyi as the source of truth and add only the missing scheduling layer that cannot be represented by current modules.

### Reuse As Source Of Truth

- Work orders: reuse IntRuoyi production work orders.
- Final scheduled tasks: reuse IntRuoyi production tasks as the published/executed schedule records.
- Route/process data: reuse IntRuoyi route, route process, route product, and route BOM modules.
- BOM/material data: reuse IntRuoyi product BOM, work order BOM, and material stock modules.
- Resource data: reuse IntRuoyi workshop, workstation, worker, machine, and tool modules.
- Calendar data: reuse IntRuoyi calendar module where possible.
- Current schedule result: reuse IntRuoyi production tasks as the only persisted effective schedule.
- Permissions/menu shell: reuse the existing production scheduling menu and permissions, adding only new permissions for auto-generate/apply and replan.

### Still Required Development

- Scheduler facade and adapters that translate IntRuoyi work orders, routes, BOM, stock, workstations, workers, machines, and calendars into the IntPP scheduler input model.
- Single-version current schedule writer that applies generated results to IntRuoyi production tasks in one transaction. It must define which existing tasks can be replaced, updated, locked, or preserved.
- Capacity planning extensions for date + shift + process/resource capacity, planned capacity, optional actual capacity, and audit trail.
- Material availability service for BOM explosion, stock aggregation, shortage calculation, shortage summaries, and risky-plan marking.
- Route topology/dependency service that builds operation precedence links from IntRuoyi route/process data.
- Automatic scheduling engine port that supports finite capacity, capacity modes, calendar/rest-day rules, route dependencies, material risk, and fail-fast prerequisite validation.
- Replan rules for locked/manual/finished tasks, frozen work orders, and partial replanning.
- Conflict detection and validation for duplicate/overlapping capacity usage, missing route, missing capacity, missing BOM/material, invalid calendar, and quantity over-scheduling.
- API layer for generate-and-apply current schedule, optional in-memory preview response, replan, and view shortages/conflicts. No persisted version-detail, publish/load version, or snapshot APIs.
- Frontend integration on the current scheduling page for auto-generate/apply, capacity mode, optional non-persisted preview, replan, shortage/conflict display, and Gantt dependency links.
- Backfill/update logic for work order `quantityScheduled` after generate/apply, update, delete, and replan.
- Backend unit/API tests and frontend Playwright E2E tests using real data.

### Explicitly Out Of Scope Under Latest User Direction

- IntPP `schedule_versions` equivalent tables.
- Persisted schedule draft tables.
- Schedule snapshots and load-saved-schedule workflows.
- Multiple schedule versions or version comparison.
- Requiring IntRuoyi's core work order, task, route, BOM, stock, resource, or calendar model to conform to IntPP naming or persistence design.
