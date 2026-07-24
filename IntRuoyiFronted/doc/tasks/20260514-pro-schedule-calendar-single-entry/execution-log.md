# Execution Log: Keep a single schedule calendar entry

BDD: Use one schedule-calendar entry only -> Given the system now exposes `排班管理 -> 生产排程日历`, When an operator looks for the schedule-calendar entry, Then no second shortcut or hidden production entry should remain.

## Evidence

- S1/S2: Completed. Previous frontend task was checked complete and this task document was created before frontend entry cleanup.
- RED: previous entry state -> FAIL, the production scheduling page still showed a `排程日历` shortcut button and `remaining.ts` still contained `MesProTaskCalendar`.
- GREEN: `pnpm exec eslint src/views/mes/pro/task/index.vue src/views/mes/pro/task/calendar/index.vue src/router/modules/remaining.ts` -> PASS
- GREEN: `rg -n "MesProTaskCalendar|openScheduleCalendar|排程日历" src/views/mes/pro/task/index.vue src/router/modules/remaining.ts` -> PASS, no old visible or hidden alternate entry remains.
