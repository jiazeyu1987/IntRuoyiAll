# Execution Log: Schedule Calendar Day-Detail Summary Cards Open Detail

BDD: non-zero day-detail task summary opens task detail -> Given the schedule
calendar day detail shows a non-zero `任务` count, When the operator clicks the
`任务` summary card, Then the frontend opens a detail dialog with the day task rows.

BDD: non-zero day-detail order or shift summary opens filtered detail -> Given
the schedule calendar day detail shows a non-zero `工单`, `白班`, or `夜班` count,
When the operator clicks that summary card, Then the frontend opens a detail
dialog filtered to the matching work-order or shift scope.

BDD: non-zero shortage or locked summary opens matching detail -> Given the
schedule calendar day detail shows a non-zero `短缺` or `锁定` count, When the
operator clicks that summary card, Then the frontend opens the corresponding
shortage or locked-task detail without hidden fallback behavior.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-summary-card-detail run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-schedule-calendar-summary-card-detail\scripts\verify-summary-card-detail.mjs` -> FAIL, the real page loaded but clicking the non-zero `任务` summary card did not open any detail dialog within 5 seconds.

GREEN: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue` -> PASS, the changed schedule-calendar view passed focused lint verification.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-summary-card-detail run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-schedule-calendar-summary-card-detail\scripts\verify-summary-card-detail.mjs` -> PASS, the live page opened detail dialogs for non-zero `任务`, `工单`, and `白班` cards, and kept zero-value `夜班`, `短缺`, and `锁定` cards disabled.
