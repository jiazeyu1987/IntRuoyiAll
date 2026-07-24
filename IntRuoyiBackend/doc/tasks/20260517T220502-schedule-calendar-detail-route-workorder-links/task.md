# Task: Schedule calendar day-detail route link fields

## Goal

Extend the MES schedule-calendar day-detail backend response so each task row exposes `routeId` and `routeName`, allowing the frontend to navigate directly from the day-detail panel into the related route page.

## Scope

- Update only the day-detail response contract, service aggregation, and targeted tests.
- Keep existing work-order fields, shortage summaries, and fail-fast behavior intact.
- Do not add fallback payloads or compatibility shims.

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-workorder-status-column-and-kingdee-confirmed/task.md`
- Status before this task: completed for code delivery.
- Impact: no unfinished backend task blocked this contract change.

## Milestones

- [x] M1: Create the backend task package before production changes.
- [x] M2: Record BDD scenarios and RED evidence for the missing route fields.
- [x] M3: Implement the minimal day-detail contract update.
- [x] M4: Run targeted backend verification and record GREEN evidence.
- [x] M5: Update task status, evidence, and closeout notes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260517T220502-schedule-calendar-detail-route-workorder-links --mode preview`

## Current Status

Completed. The day-detail API now returns `routeId` and `routeName` for each task row without regressing work-order fields or shortage summaries.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260517T220502-schedule-calendar-detail-route-workorder-links --mode preview` -> PASS

## Blocker And Impact

- None for code delivery.
- Live data note: current runtime data still marks route `900020` as logically deleted, so route-name display for that record depends on data repair rather than a remaining code-path gap.
