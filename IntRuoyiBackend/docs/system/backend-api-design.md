# Backend API Design: Current-System-First Auto Scheduling

## Purpose and Scope

This document defines backend modules and API contracts for IntRuoyi automatic scheduling. The backend must adapt IntPP scheduling logic to IntRuoyi data. It must write one current effective schedule into IntRuoyi production tasks and must not add persisted schedule versions, drafts, snapshots, or version comparison APIs.

## Evidence Reviewed

- `docs/product/prd.md`
- `docs/product/acceptance-criteria.md`
- `docs/changes/20260512-intpp-auto-schedule-migration.md`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/task/MesProTaskServiceImpl.java`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/task/MesProTaskDO.java`
- `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/workorder/MesProWorkOrderDO.java`
- Route, work order BOM, workstation, calendar, and material stock services under `yudao-module-mes`.

## Modules

Add scheduling-specific backend modules under the MES module. Recommended package boundary:

- `pro.schedule.controller`
  - `MesProAutoScheduleController`

- `pro.schedule.service`
  - `MesProAutoScheduleService`
  - `MesProScheduleAdapterService`
  - `MesProCapacityPlanService`
  - `MesProMaterialAvailabilityService`
  - `MesProRouteDependencyService`
  - `MesProScheduleConflictService`
  - `MesProCurrentScheduleWriter`
  - `MesProScheduledQuantitySyncService`

- `pro.schedule.dal`
  - capacity plan/actual/audit mapper and DO classes
  - task dependency mapper and DO classes
  - task scheduling metadata mapper and DO classes
  - current schedule issue mapper and DO classes

Module responsibilities:

- `MesProScheduleAdapterService`
  - Reads IntRuoyi work orders, tasks, route/process, BOM, stock, workstation, calendar, and capacity data.
  - Builds the scheduler input model.
  - Does not require IntRuoyi entities to match IntPP table names.

- `MesProAutoScheduleService`
  - Orchestrates validation, scheduling, conflict detection, current-task write, issue persistence, and quantity sync.

- `MesProCurrentScheduleWriter`
  - Applies generated task changes to `mes_pro_task` in one transaction.
  - Preserves finished and locked tasks.
  - Replaces only tasks allowed by replan policy.

- `MesProMaterialAvailabilityService`
  - Explodes BOM, aggregates available stock, calculates shortages, and returns blocking/risk issues.

- `MesProCapacityPlanService`
  - Resolves planned or actual capacity for date, shift, process, and resource.
  - Fails fast when selected mode requires missing capacity data.

- `MesProRouteDependencyService`
  - Builds operation/task precedence from current route/process data.
  - Detects cycles and missing process references.

- `MesProScheduleConflictService`
  - Checks capacity overuse, duplicate resource occupancy, missing prerequisites, quantity over-scheduling, and protected task conflicts.

## API Contracts

Base path: `/mes/pro/auto-schedule`

### POST `/preview`

Purpose: return a non-persisted schedule preview. This endpoint is optional for first version if generate/apply requires immediate application after confirmation.

Permission: `mes:pro-auto-schedule:preview`

Request fields:

- `workOrderIds: Long[]`
- `startTime: LocalDateTime`
- `capacityMode: DEFAULT | PLANNED | ACTUAL`
- `materialPolicy: BLOCK_ON_SHORTAGE | ALLOW_RISK`
- `replanPolicy: PRESERVE_LOCKED | PRESERVE_MANUAL | PRESERVE_STARTED | REPLACE_NOT_STARTED`
- `resourceScope: optional`
- `requestId: optional client idempotency token`

Response fields:

- `previewOnly: true`
- `summary`
- `tasks`
- `links`
- `issues`
- `protectedTasks`

No database task writes are allowed.

### POST `/apply`

Purpose: generate and apply the current effective schedule.

Permission: `mes:pro-auto-schedule:apply`

Request fields: same as `/preview`, plus:

- `confirmRisk: boolean`
- `confirmProtectedImpact: boolean`

Response fields:

- `applied: true`
- `summary`
- `createdTaskIds`
- `updatedTaskIds`
- `deletedTaskIds`
- `preservedTaskIds`
- `quantitySyncResults`
- `issues`

Database writes:

- allowed production task inserts/updates/deletes
- task scheduling metadata
- task dependencies
- current schedule issues
- work order `quantityScheduled`
- capacity audit rows when capacity tables are changed

### POST `/replan`

Purpose: replan existing current production tasks in the selected scope.

Permission: `mes:pro-auto-schedule:replan`

Request fields:

- `workOrderIds: Long[]`
- `fromTime: LocalDateTime`
- `capacityMode`
- `materialPolicy`
- `replanPolicy`
- `confirmRisk`
- `confirmProtectedImpact`

Response fields: same shape as `/apply`.

### GET `/issues`

Purpose: list current schedule conflicts and material shortages for the current effective schedule.

Permission: `mes:pro-auto-schedule:query`

Query fields:

- `workOrderId`
- `taskId`
- `issueType`
- `severity`

Response fields:

- `items`
- `summary`

### GET `/dependencies`

Purpose: list current task dependency links for Gantt.

Permission: `mes:pro-task:query`

Query fields:

- `workOrderIds`
- `taskIds`

Response fields:

- `links: [{ id, sourceTaskId, targetTaskId, type, sourceProcessId, targetProcessId }]`

## Error Model

Use existing `ServiceException` and error code patterns. Add explicit MES error codes for:

- `PRO_AUTO_SCHEDULE_SCOPE_EMPTY`
- `PRO_AUTO_SCHEDULE_ROUTE_REQUIRED`
- `PRO_AUTO_SCHEDULE_ROUTE_PROCESS_REQUIRED`
- `PRO_AUTO_SCHEDULE_ROUTE_CYCLE`
- `PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED`
- `PRO_AUTO_SCHEDULE_CALENDAR_REQUIRED`
- `PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED`
- `PRO_AUTO_SCHEDULE_ACTUAL_CAPACITY_REQUIRED`
- `PRO_AUTO_SCHEDULE_MATERIAL_SHORTAGE_BLOCKED`
- `PRO_AUTO_SCHEDULE_QUANTITY_EXCEEDS_WORK_ORDER`
- `PRO_AUTO_SCHEDULE_PROTECTED_TASK_CONFLICT`
- `PRO_AUTO_SCHEDULE_FINISHED_TASK_IMMUTABLE`
- `PRO_AUTO_SCHEDULE_CONFLICT_EXISTS`

Failure behavior:

- Missing required data must fail fast.
- No endpoint may return success after silently dropping constraints.
- Apply/replan must not partially write tasks when validation fails.

## Transactions and Idempotency

Transactions:

- `/apply` and `/replan` must run validation and write operations inside a transactional service boundary.
- The writer must calculate the full change set before modifying tasks.
- On any validation or write failure, transaction rollback must leave current production tasks unchanged.
- Finished tasks must be rechecked in the transaction before write.

Idempotency:

- First version may use `requestId` as an optional duplicate-protection key if an operation log table is approved.
- If no idempotency persistence is approved, repeated calls are treated as new scheduling attempts and must be deterministic for identical current data.
- The service must not pretend idempotency exists without a persisted key or clear duplicate policy.

## Open Questions

- Is preview mandatory before apply?
- Should `requestId` be persisted for idempotency and audit?
- Which existing task statuses map to started, locked, manual, and finished?
- Should current schedule issues be persisted after apply or recomputed on demand?
- What is the final capacity granularity: workstation, line, process, or process-resource combination?

## Design Blockers

- Capacity granularity is not approved.
- Material shortage policy is not approved.
- Task lock/manual-preserve schema is not approved.
- Idempotency/audit persistence is not approved.
