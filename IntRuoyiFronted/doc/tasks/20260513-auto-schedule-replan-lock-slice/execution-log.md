# Execution Log: Auto schedule replan and task-lock slice frontend

BDD: Replan submits explicit scope -> Given the planner chooses a replan scope, When replan is confirmed, Then the frontend submits only that scope and does not silently fall back to full apply.

BDD: Protected impact is visible -> Given the backend reports preserved protected tasks for a replan scope, When the planner reviews the replan dialog, Then the UI shows that preserved impact before final confirmation.

BDD: Lock and unlock are explicit actions -> Given a task row or day-detail task is shown, When the planner locks or unlocks the task, Then the frontend calls the explicit lock/unlock API and refreshes the visible locked state.

## Evidence

- M1/M2: Completed. Frontend slice docs created before production code changes.
- GREEN: `pnpm exec eslint src/api/mes/pro/task/index.ts src/views/mes/pro/task/calendar/index.vue` -> PASS after adding explicit lock/unlock API wrappers and calendar task-card actions.
- GREEN: real browser verification against frontend `http://127.0.0.1:3102/mes/pro/task/calendar` and backend `http://127.0.0.1:48092/admin-api` -> PASS for task lock/unlock. Locking a day-detail task increased the visible locked count and changed the card action from `锁定` to `解锁`; unlocking reverted the state and hit the explicit backend unlock API.
