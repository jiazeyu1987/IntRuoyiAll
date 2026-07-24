# Execution Log: Assess IntPP Auto Scheduling Migration

BDD: Migration gap assessment -> Given IntPP contains an automatic scheduling capability and IntRuoyi contains a basic MES scheduling page, When both systems are compared, Then the assessment should identify reusable existing systems, partial systems, missing systems, blockers, and a recommended migration split without changing production code.

GREEN: previous task state -> PASS, latest IntRuoyi task `20260512-restart-ruoyi-bat` is marked completed.

GREEN: task documentation -> PASS, this assessment task was created before codebase comparison.

GREEN: IntRuoyi manual scheduling evidence -> PASS, `yudao-ui-admin-vue3/src/views/mes/pro/task/components/GanttChart.vue` has `gantt.config.auto_scheduling = false`, `gantt.config.drag_links = false`, and returns `links: []`.

GREEN: IntRuoyi task creation evidence -> PASS, `yudao-ui-admin-vue3/src/views/mes/pro/task/ProTaskList.vue` requires manual workstation, quantity, start time, and duration input; duration is converted to end time by `duration * 8h`.

GREEN: IntRuoyi backend evidence -> PASS, `MesProTaskServiceImpl` validates work order, workstation, route, process, and item before inserting a PREPARE task, but no automatic capacity/material scheduling logic was found in that service path.

GREEN: IntRuoyi supporting modules evidence -> PASS, route, route process, route BOM, product BOM, work order BOM, workstation, worker, machine, workshop, calendar, and material stock modules exist in `ruoyi-vue-pro/yudao-module-mes`.

GREEN: IntPP schema evidence -> PASS, `D:/ProjectPackage/Int/IntPP/backend/sqlite/001_init.sql` includes production orders, material issue items, BOM children, material supply cache, capacity bindings, schedule versions, schedule tasks, current schedule tasks, schedule snapshots, schedule calendar rules, daily line capacity plan, daily line capacity actual, and capacity audit tables.

GREEN: IntPP service evidence -> PASS, `D:/ProjectPackage/Int/IntPP/backend/app/services/app_service.py` includes schedule version/task APIs, current schedule/snapshot APIs, material shortage analysis, `generate_schedule`, `generate_schedule_by_fact`, capacity source normalization, and actual capacity fail-fast validation.

GREEN: IntPP test evidence -> PASS, IntPP backend tests cover planned/actual/default capacity modes, actual-capacity-required failure, shift capacity, worker-process group scheduling, material shortage risky plans, and fact-based replanning.

GREEN: migration decision -> PASS, migration should be split before implementation because current IntRuoyi has manual scheduling plus master data, but lacks IntPP automatic scheduling persistence, capacity/material services, engine rules, frontend workflows, and tests.

GREEN: change request validator -> PASS, `python C:/Users/BJB110/.codex/skills/change-request-triage/scripts/validate_change_request.py --evidence docs/changes/20260512-intpp-auto-schedule-migration.md` returned `Change request evidence is valid.`

GREEN: branch landing target -> PASS, copied the assessment documents into `ruoyi-vue-pro` on branch `docs/intpp-auto-schedule-migration` because the original root-level documents are outside a Git repository.

BDD: Single-version current-system-first migration scope -> Given the user rejected IntPP-style draft/version management and required IntPP to adapt to IntRuoyi, When the migration scope is updated, Then the plan should use IntRuoyi production tasks as the only effective schedule result and exclude persisted versions, drafts, snapshots, and version review.

GREEN: scope revision documented -> PASS, assessment now records single-version current schedule write strategy and explicitly excludes IntPP schedule versions, persisted drafts, snapshots, and version comparison.

GREEN: paichan relocation -> PASS, previous relocation stored scheduling documents under `paichan/20260512-intpp-auto-schedule-migration/`.

BDD: Restore document location -> Given the user requested restoring document locations, When the files are moved back, Then the task document should live under `doc/tasks/20260512-intpp-auto-schedule-migration-assessment/` and the change request should live under `docs/changes/20260512-intpp-auto-schedule-migration.md`.

GREEN: document location restored -> PASS, scheduling documents were moved back to the original task and change-request locations.
