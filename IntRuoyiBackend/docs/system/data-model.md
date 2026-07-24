# Data Model: Current-System-First Auto Scheduling

## Purpose and Scope

This document defines the data model for adding automatic scheduling while reusing IntRuoyi MES entities as the source of truth. The model must not introduce IntPP-style schedule versions, persisted drafts, snapshots, or version comparison tables.

## Evidence Reviewed

- `docs/product/prd.md`
- `docs/product/acceptance-criteria.md`
- `docs/changes/20260512-intpp-auto-schedule-migration.md`
- `MesProWorkOrderDO.quantityScheduled`
- `MesProTaskDO`
- `MesProRouteDO`, `MesProRouteProcessDO`, `MesProRouteProductBomDO`
- `MesProWorkOrderBomDO`
- `MesWmMaterialStockDO`
- `MesMdWorkstationDO`, workstation worker/machine/tool entities
- `MesCalCalendarController` and calendar response/list request VOs

## Entities

### Reused Entities

- `mes_pro_workorder`
  - Source of production demand.
  - Existing `quantityScheduled` remains the scheduled quantity summary.

- `mes_pro_task`
  - Current effective schedule result.
  - Auto scheduling writes final applied tasks here.

- Route/process entities
  - Source for operation sequence and dependency resolution.

- BOM entities
  - Product BOM, route product BOM, and work order BOM are the material requirement source.

- Material stock entities
  - Source for available quantity calculation after final stock policy is approved.

- Workstation/resource entities
  - Source for schedulable resource candidates.

- Calendar entities
  - Source for working-day and shift availability where current calendar data is sufficient.

### New Entities

The following entities are scheduling-specific extensions. They support one current effective schedule only.

#### `mes_pro_capacity_plan`

Purpose: planned capacity by date, shift, process, and resource.

Candidate fields:

- `id`
- `calendar_date`
- `shift_code`
- `process_id`
- `workstation_id` or `line_id`
- `planned_capacity_qty`
- `worker_count`
- `machine_count`
- `enabled`
- `remark`
- standard audit fields

#### `mes_pro_capacity_actual`

Purpose: actual usable capacity for actual-capacity scheduling mode.

Candidate fields:

- `id`
- `calendar_date`
- `shift_code`
- `process_id`
- `workstation_id` or `line_id`
- `actual_capacity_qty`
- `source_type`
- `source_id`
- standard audit fields

#### `mes_pro_capacity_audit`

Purpose: audit capacity changes.

Candidate fields:

- `id`
- `capacity_type: PLAN | ACTUAL`
- `capacity_id`
- `changed_field`
- `old_value`
- `new_value`
- `change_reason`
- `changed_by`
- `changed_at`

#### `mes_pro_task_schedule_ext`

Purpose: scheduling metadata for existing production tasks without changing core task ownership too much.

Candidate fields:

- `id`
- `task_id`
- `schedule_source: MANUAL | AUTO`
- `locked`
- `locked_reason`
- `generated_request_id`
- `risk_status: NONE | RISKY | BLOCKED`
- standard audit fields

#### `mes_pro_task_dependency`

Purpose: current Gantt and scheduling dependency links.

Candidate fields:

- `id`
- `source_task_id`
- `target_task_id`
- `source_process_id`
- `target_process_id`
- `dependency_type`
- `enabled`
- standard audit fields

#### `mes_pro_schedule_issue`

Purpose: current schedule issues, conflicts, and shortage records.

Candidate fields:

- `id`
- `issue_type: CAPACITY | MATERIAL | ROUTE | CALENDAR | QUANTITY | PROTECTED_TASK`
- `severity: BLOCKING | RISK | INFO`
- `work_order_id`
- `task_id`
- `process_id`
- `workstation_id`
- `material_id`
- `calendar_date`
- `shift_code`
- `required_qty`
- `available_qty`
- `shortage_qty`
- `message`
- `resolved`
- standard audit fields

This table represents current issues only. It is not a schedule snapshot or version history.

## Relationships

- Work order `1 -> many` production tasks.
- Work order/task `1 -> optional 1` scheduling metadata.
- Task `many -> many` task dependency through `mes_pro_task_dependency`.
- Capacity rows relate to process and resource by date/shift.
- Schedule issues relate to work order, task, process, resource, or material depending on issue type.
- `quantityScheduled` on work order is derived from current non-canceled production tasks.

## State Models

### Task Scheduling Source

- `MANUAL`
  - Created or maintained manually.
  - Can be preserved during replan depending on policy.

- `AUTO`
  - Created or updated by auto scheduling.
  - Can be replaced by later auto scheduling unless locked, started, or finished.

### Task Lock

- `locked = false`
  - Task can be changed if status and policy allow.

- `locked = true`
  - Task cannot be moved, deleted, or overwritten by auto scheduling unless a separate unlock action occurs.

### Schedule Issue Severity

- `BLOCKING`
  - Apply/replan fails.

- `RISK`
  - Apply is allowed only if risk policy and user confirmation allow it.

- `INFO`
  - Display only.

### Capacity Mode

- `DEFAULT`
  - Uses route/resource default capacity if approved.

- `PLANNED`
  - Requires planned capacity rows.

- `ACTUAL`
  - Requires actual capacity rows and fails fast when missing.

## Migration Notes

- Do not create `schedule_versions`, `schedule_tasks`, `schedule_snapshots`, or equivalent persisted version tables.
- Add capacity tables only after capacity granularity is approved.
- Prefer extension tables for scheduling metadata to reduce churn in existing core task tables.
- Add indexes for:
  - capacity date/shift/process/resource
  - task dependency source/target task
  - task schedule metadata task id
  - schedule issue work order/task/severity
- Existing data migration should initialize current manual tasks with `schedule_source = MANUAL` only if the extension table is approved.
- Existing `quantityScheduled` should be recalculated once during migration if historical data is inconsistent.

## Data Integrity Rules

- `quantityScheduled` must equal the sum of current effective scheduled task quantities for each work order, according to the approved inclusion rule.
- Finished tasks must not be deleted or moved by auto scheduling.
- Locked tasks must not be overwritten by auto scheduling.
- Task dependencies must not form cycles.
- Applied tasks must not exceed work order quantity.
- Capacity mode `ACTUAL` must not run without actual capacity rows.
- Blocking issues must prevent apply/replan.
- No persisted preview data is allowed in first version.

## Open Questions

- Should task scheduling metadata be added as columns on `mes_pro_task` or as `mes_pro_task_schedule_ext`?
- What exact task statuses count as started or finished?
- What inventory quantity fields count as available?
- What resource granularity should capacity use?
- Should current schedule issues persist until the next apply, or be recomputed on each query?

## Design Blockers

- Capacity dimension is not approved.
- Inventory availability policy is not approved.
- Task lock/manual-preserve persistence is not approved.
- Issue persistence policy is not approved.
