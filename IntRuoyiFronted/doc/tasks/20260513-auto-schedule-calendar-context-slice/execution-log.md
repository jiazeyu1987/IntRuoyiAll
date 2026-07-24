# Execution Log: Auto schedule calendar-context slice frontend

BDD: Publish uses previewed calendar context -> Given preview completed successfully, When the user clicks publish, Then the frontend submits the exact previewed calendar-context token with the apply request.

BDD: Calendar change invalidates preview -> Given a preview exists, When schedule calendar rules or simulation state change, Then the frontend blocks publish until the preview is regenerated.

BDD: Preview shows normal versus simulated calendar -> Given preview exists on the schedule calendar page, When the current calendar context is simulated or normal, Then the page shows which calendar context the preview is bound to.

BDD: Task page remains usable for real auto-schedule -> Given the task page loads waiting work orders, When the user filters to one work order and runs auto-schedule, Then the page must issue preview/apply requests instead of failing during mounted setup.

## Evidence

- M1/M2: Completed. Frontend slice docs created before production code changes.
- M3: Task-local verifier added at `doc/tasks/20260513-auto-schedule-calendar-context-slice/verify-frontend-contract.cjs`.
- RED: `node doc/tasks/20260513-auto-schedule-calendar-context-slice/verify-frontend-contract.cjs` -> FAIL, missing `calendarContextToken` contract in `src/api/mes/pro/task/autoSchedule/index.ts`.
- GREEN: `node doc/tasks/20260513-auto-schedule-calendar-context-slice/verify-frontend-contract.cjs` -> PASS.
- GREEN: `pnpm exec eslint src/api/mes/pro/task/autoSchedule/index.ts src/views/mes/pro/task/index.vue src/views/mes/pro/task/calendar/index.vue` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260513-auto-schedule-calendar-context-slice/frontend-feature-evidence.md` -> PASS.
- RED: real browser verification on `http://127.0.0.1:3102/mes/pro/task` hit `ReferenceError: handleTree is not defined` during the mounted load path, so the waiting work-order table rendered no rows and the auto-schedule drawer could not issue preview requests.
- GREEN: after restoring the missing `handleTree` import in `src/views/mes/pro/task/index.vue`, real browser verification on `http://127.0.0.1:3102/mes/pro/task` passed. Filtering to `AUTO-WO-001` produced a real `preview` request for work order `900080`, the page enabled publish, and the subsequent `apply` request replayed the exact `calendarContextToken` returned by preview.
