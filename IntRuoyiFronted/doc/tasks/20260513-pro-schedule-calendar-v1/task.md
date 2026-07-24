# Task: IntPP-style schedule calendar v1 frontend

## Goal

Implement a production schedule calendar v1 in the IntRuoyi MES frontend, embedded under production scheduling and styled after the IntPP schedule calendar interaction model.

## Scope

- Add `/mes/pro/task/calendar` frontend entry and page.
- Add month toolbar, calendar grid, settings panel, detail panel, and shortage modal.
- Connect the page to new `/mes/pro/schedule-calendar/*` APIs and existing auto-schedule preview/apply APIs.
- Do not add saved-version or version-compare UI.
- Keep legacy `mes/cal/calendar` page unchanged.
- Correct the frontend/API wrapper to the real backend contract for rules, month data, and day detail data.

## Previous Task Check

- `doc/tasks/20260513-intpp-auto-schedule-first-loop/task.md` is completed.

## Milestones

- [x] M1: Previous frontend task checked complete before new work.
- [x] M2: Frontend task documentation created before production code changes.
- [x] M3: RED frontend verification written for the new calendar entry, rules panel, detail panel, and replay flow.
- [x] M4: New schedule calendar page, route, and API wrappers implemented.
- [x] M5: Existing production scheduling entry updated to open the new calendar page.
- [x] M6: Focused frontend verification passes.
- [x] M7: Evidence updated and frontend task marked completed.
- [x] M8: Frontend changes committed on `feature/auto-schedule-first-loop`.

## Correction Round: Real Backend Contract Alignment

- [x] C1: Task docs and task-local RED verification updated to the real backend contract before production code changes.
- [x] C2: `src/api/mes/pro/scheduleCalendar/index.ts` aligned to the real backend contract.
- [x] C3: `src/views/mes/pro/task/calendar/index.vue` aligned to the real backend contract without reintroducing version controls.
- [x] C4: Focused verification passes for the corrected contract and evidence is updated.
- [x] C5: Contract-correction changes committed as a new scoped Git commit.

## Expected Verification

- The production scheduling page exposes a schedule calendar entry.
- The new page renders month calendar, rule controls, day detail, and shortage modal.
- Rules and simulation actions call the new backend APIs.
- Auto-schedule preview/apply can be triggered from the new calendar page.
- No version save/load/compare controls appear.
- The rules panel uses `skipStatutoryHolidays`, `weekendRestMode`, and `dateShiftModeByDate`.
- The month and day-detail views render from the real backend month/day payloads rather than the previous assumed fields.

## Current Status

Completed on `feature/auto-schedule-first-loop`. Frontend route, entry,
real-contract API wrapper, calendar page rendering, task-local verification,
and real browser flow validation are complete.
