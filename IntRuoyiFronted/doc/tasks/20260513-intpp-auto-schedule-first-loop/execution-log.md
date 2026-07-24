# Execution Log: IntPP auto schedule first loop frontend

BDD: Preview auto schedule from the task page -> Given an administrator opens the MES production scheduling page with the required permission, When the administrator submits auto-schedule parameters, Then the frontend requests schedule preview and renders planned tasks, capacity summary, and dependency-aware Gantt data.

BDD: Surface blocking issues before publish -> Given the backend returns shortage or prerequisite issues for the selected work orders, When preview completes, Then the frontend shows the blocking issues clearly and does not pretend the schedule can be published.

BDD: Confirm publish current schedule -> Given a preview result is publishable, When the administrator confirms publish, Then the frontend calls the apply API and refreshes the current task list and Gantt data.

BDD: Keep dependency editing disabled -> Given the Gantt chart renders dependency links, When the user interacts with the chart, Then dependency lines are visible but manual link dragging remains disabled.

## Evidence

- M1/M2: Completed. Previous frontend task status was checked and this frontend task document plus BDD scenarios were created before production code changes.
- RED: `node doc\tasks\20260513-intpp-auto-schedule-first-loop\verify-frontend-contract.cjs` -> FAIL, before this implementation the auto-schedule API wrapper, page entry, preview/apply flow, and Gantt links path were missing.
- GREEN: `node doc\tasks\20260513-intpp-auto-schedule-first-loop\verify-frontend-contract.cjs` -> PASS.
- GREEN: `pnpm exec eslint src/api/mes/pro/task/autoSchedule/index.ts src/views/mes/pro/task/components/GanttChart.vue src/views/mes/pro/task/edit/index.vue src/views/mes/pro/task/index.vue` -> PASS.
- GREEN: Playwright real-user-path check passed on `http://localhost:3100`: login succeeded, `/mes/pro/task` loaded against the backend on `http://localhost:48080`, the auto-schedule button was visible, and the auto-schedule drawer opened.
- GREEN: after seeding one real local schedulable MES work order, Playwright completed the success path: the work order was visible in the scheduling table, auto-schedule preview rendered one generated task with zero blocking issues, and publish completed against `/admin-api/mes/pro/auto-schedule/apply`.
- GREEN: after replaying the committed backend demo-data scripts, Playwright reran the same browser path and the preview/apply success flow still held.
- BLOCKED: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL because the repository has extensive unrelated TypeScript errors outside this task scope, including `src/api/mall/statistics/trade.ts`, BPMN designer components, and multiple system views.
