# Task: Production Work Order Row Temporary Freeze API

## Goal

Expose a row-level production work-order temporary freeze update API so the frontend work-order list can replace the `新增` row action with real `冻结/解冻` behavior for individual work orders.

## Scope

- Confirm the latest same-repository backend task is explicitly completed or blocked before starting this task.
- Inspect the existing production work-order temporary-freeze controller, service, mapper capabilities, and task-clearing rules.
- Record BDD scenarios before production code changes.
- Add a failing backend regression test for row-level temporary freeze updates.
- Implement the minimal backend API and service behavior needed to freeze or unfreeze a single work order without introducing fallback paths.
- Run targeted backend verification and capture contract evidence.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-workorder-erp-bom-garbled-item-fix/task.md`
- Status before this task: completed.
- Impact: the latest same-repository backend task is already closed, so it does not block this work-order row-freeze API slice.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this backend task package.
- [x] M2: Record BDD scenarios and add RED verification for row-level temporary freeze updates.
- [x] M3: Implement the minimal backend API and service change.
- [x] M4: Run GREEN verification and update backend evidence.
- [x] M5: Commit only task-scoped backend files after required verification passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProWorkOrderServiceImplTest,MesProWorkOrderControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-workorder-row-freeze-toggle-action/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-workorder-row-freeze-toggle-action --mode preview`

## Current Status

Completed on 2026-05-18. Backend API delivery, verification, closeout preview, and task-scoped commit are complete.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProWorkOrderServiceImplTest,MesProWorkOrderControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-workorder-row-freeze-toggle-action/backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-workorder-row-freeze-toggle-action --mode preview` -> PASS

## Blocker And Impact

- Blocker: none.
- Impact: none.

## Cleanup Keep

- doc/tasks/20260518-workorder-row-freeze-toggle-action/backend-api-evidence.md
