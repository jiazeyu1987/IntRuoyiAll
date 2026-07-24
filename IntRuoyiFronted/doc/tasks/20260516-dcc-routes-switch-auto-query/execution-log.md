# Execution Log: DCC Approval Routes Switch Auto Query

BDD: switching file category auto-refreshes approval routes -> Given the user is on `DCC审批路线` and has already loaded a valid category / When the user switches to another category / Then the page automatically re-runs the routes query and derived preview refresh without requiring a click on `查询路线`.
BDD: clearing file category returns the page to empty state -> Given the user is on `DCC审批路线` / When the current category selection is cleared / Then the page clears the routes list, preview rows, and preview error state instead of keeping stale data.

- M1: Completed. Previous frontend task `20260516-dcc-access-rules-real-content-e2e` remains blocked by missing live access-rule data, so this approval-routes behavior fix can proceed independently.
- RED: `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-routes-switch-auto-query\scripts\verify-dcc-routes-switch-auto-query.mjs` -> FAIL, after manually querying baseline category `产品技术要求`, switching to `生产用设备清单` produced `routeRequests=[]` until the user clicked `查询路线`.
- GREEN: `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-routes-switch-auto-query\scripts\verify-dcc-routes-switch-auto-query.mjs` -> PASS, switching from `产品技术要求` to `生产用设备清单` automatically triggered `1` routes request and `1` preview request without manual query.
- GREEN: `npx eslint D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\routes\index.vue` -> PASS.
