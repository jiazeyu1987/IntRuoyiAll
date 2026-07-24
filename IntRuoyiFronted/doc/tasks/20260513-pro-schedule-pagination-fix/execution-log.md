# Execution Log: Fix schedule scope pagination limit

BDD: Load schedule scope with backend page-size limit -> Given the filtered work-order total can exceed 200, When the production scheduling pages collect all scoped work-order ids, Then the frontend requests multiple pages within the backend limit and still builds the full scope.

## Evidence

- P1/P2: Completed. Previous frontend task was checked complete and this bug-fix task document was created before production code changes.
- RED: browser reproduction on `/mes/pro/task/calendar` -> FAIL, backend returned `请求参数不正确:每页条数最大值为 200` because the frontend attempted to fetch all filtered work orders with `pageSize = total`.
- GREEN: `rg -n "pageSize:\\s*firstPage.total|pageSize:\\s*total.value" src/views/mes/pro/task` -> PASS, no remaining over-limit scope loading call remains in the touched production scheduling pages.
- GREEN: `pnpm exec eslint src/views/mes/pro/task/scopeWorkOrders.ts src/views/mes/pro/task/index.vue src/views/mes/pro/task/calendar/index.vue` -> PASS
