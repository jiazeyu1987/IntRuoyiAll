# Task: Schedule calendar shortage risk and daily material summary

## Goal

Update the MES auto-schedule backend and schedule-calendar aggregation so material shortage is preserved as a warning risk instead of a blocking issue, while selected-day calendar detail returns per-material scheduled usage, remaining available quantity, shortage quantity, and affected work-order count.

## Scope

- Block the previous same-repository backend task before starting this work.
- Preserve the already-paused route / work-order drill-down changes in the same files.
- Record BDD scenarios and strict TDD evidence for shortage-warning preview/apply behavior and daily material aggregation.
- Keep other missing prerequisites fail-fast.
- Preview closeout artifacts before commit.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-md-item-erp-bom-sync-button/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused ERP BOM sync backend task remained isolated and did not block this schedule-calendar slice.

## Milestones

- [x] M1: Block the previous same-repository backend task and create this task package first.
- [x] M2: Record BDD scenarios and add RED verification for shortage-warning preview/apply behavior and daily material summary aggregation.
- [x] M3: Implement the minimal backend severity, aggregation, contract, and test-support changes.
- [x] M4: Run targeted backend verification and update evidence.
- [x] M5: Preview closeout artifacts and prepare a task-scoped backend commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-schedule-calendar-shortage-risk-daily-material-summary --mode preview`

## Current Status

Completed. Backend implementation, targeted Maven verification, evidence validation, and closeout preview are complete.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-schedule-calendar-shortage-risk-daily-material-summary --mode preview` -> PASS

## Blocker And Impact

- None currently.
