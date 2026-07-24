# Execution Log: IntPP-style schedule calendar v1 frontend

BDD: Open the production schedule calendar -> Given an operator is on the production scheduling page, When the operator opens the schedule calendar, Then the frontend navigates to the new production schedule calendar page.

BDD: Save calendar rules -> Given the schedule calendar page is open, When the operator edits holiday/weekend/date-shift rules and saves, Then the frontend persists the rules and refreshes the calendar.

BDD: Advance simulation date -> Given the schedule calendar page is open, When the operator advances or resets the simulation date, Then the frontend refreshes the month and selected-day detail using the new state.

BDD: Trigger auto scheduling from the calendar -> Given a selected day and valid current schedule context exist, When the operator previews or applies auto schedule from the new calendar page, Then the frontend shows preview detail and applies current schedule changes.

BDD: Correct schedule rules contract -> Given the schedule calendar page is open, When the frontend loads or saves scheduling rules, Then it uses `skipStatutoryHolidays`, `weekendRestMode`, and `dateShiftModeByDate`, and does not send the previous guessed rule fields.

BDD: Render real month/day-detail contract -> Given the backend returns month and day-detail payloads in the real contract shape, When the operator views the month grid or a selected day, Then the frontend renders counts, shifts, workshops, lines, tasks, and material shortage summary from those payloads without assuming old top-level `tasks` or `shortages` arrays.

## Evidence

- M1/M2: Completed. Previous frontend task was checked complete and this frontend task document plus BDD scenarios were created before production code changes.
- RED: `node doc/tasks/20260513-pro-schedule-calendar-v1/verify-frontend-contract.cjs` -> FAIL, expected before implementation because `src/api/mes/pro/scheduleCalendar/index.ts` and the new schedule calendar page/route do not exist yet.
- GREEN: `node doc/tasks/20260513-pro-schedule-calendar-v1/verify-frontend-contract.cjs` -> PASS
- GREEN: `pnpm exec eslint src/api/mes/pro/scheduleCalendar/index.ts src/views/mes/pro/task/calendar/index.vue src/views/mes/pro/task/index.vue src/router/modules/remaining.ts` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260513-pro-schedule-calendar-v1/frontend-feature-evidence.md` -> PASS
- GREEN: `Invoke-WebRequest -Uri 'http://127.0.0.1:3000/' -UseBasicParsing` -> PASS, returned `200` after local Vite dev server launch.
- M4/M5/M6/M7: Completed. The new schedule calendar route, page, API wrapper, entry button, focused verification, and evidence document are all in place.
- RED: `node doc/tasks/20260513-pro-schedule-calendar-v1/verify-frontend-contract.cjs` -> FAIL, expected for the contract-correction round because the previous API wrapper/page still used guessed fields like `weekendAsWorkday` instead of the real backend fields such as `skipStatutoryHolidays`.
- GREEN: `node doc/tasks/20260513-pro-schedule-calendar-v1/verify-frontend-contract.cjs` -> PASS after aligning the API wrapper and schedule calendar page to the real backend contract.
- GREEN: `pnpm exec eslint src/api/mes/pro/scheduleCalendar/index.ts src/views/mes/pro/task/calendar/index.vue src/views/mes/pro/task/index.vue src/router/modules/remaining.ts` -> PASS after the contract-correction round.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar run-code --filename D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3\doc\tasks\20260513-pro-schedule-calendar-v1\scripts\exercise-schedule-calendar.mjs` -> PASS, using a real admin login and the real `MES 系统 -> 生产管理 -> 生产排产 -> 排程日历` path to save rules, preview auto schedule, apply current schedule, and confirm day-detail tasks render.
