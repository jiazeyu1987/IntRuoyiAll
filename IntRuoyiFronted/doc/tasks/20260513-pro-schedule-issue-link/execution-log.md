# Execution Log: Make schedule issue work orders clickable

BDD: Open work order from schedule issue dialog -> Given the schedule issue dialog shows a work-order code, When the operator clicks that code, Then the frontend navigates to the production work-order page and opens the corresponding work-order detail.

## Evidence

- L1/L2: Completed. Previous frontend task was checked complete and this task document was created before production code changes.
- RED: previous issue dialog behavior -> FAIL, the `工单` column was plain text and did not navigate.
- GREEN: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue src/views/mes/pro/workorder/index.vue` -> PASS
- GREEN: `rg -n "openIssueWorkOrder|handleIssueCellClick|issueCellClassName|openId|route.query.code" src/views/mes/pro/task/calendar/index.vue src/views/mes/pro/workorder/index.vue` -> PASS
