# Execution Log: Route production schedule calendar through scheduling management

BDD: Open unified production schedule calendar entry -> Given the operator is on the production scheduling page, When the operator clicks `排程日历`, Then the frontend routes to the scheduling-management calendar entry instead of the old hidden production path.

## Evidence

- F1/F2: Completed. Previous frontend task was checked complete and this task document was created before frontend entry changes.
- RED: previous entry behavior -> FAIL, the production scheduling page button routed to the hidden route name `MesProTaskCalendar`.
- GREEN: `pnpm exec eslint src/views/mes/pro/task/index.vue` -> PASS
