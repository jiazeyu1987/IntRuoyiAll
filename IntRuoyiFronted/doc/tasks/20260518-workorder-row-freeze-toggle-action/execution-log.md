# Execution Log: Production Work Order Row Freeze Toggle Action

BDD: confirmed non-frozen row shows freeze action -> Given the production work-order list renders a row that is eligible for the previous `新增` action and `temporaryFrozen` is false / When row actions are displayed / Then the row shows `冻结` instead of `新增`.

BDD: confirmed frozen row shows unfreeze action -> Given the production work-order list renders a row that is eligible for the previous `新增` action and `temporaryFrozen` is true / When row actions are displayed / Then the row shows `解冻` instead of `新增`.

BDD: row action triggers real temporary freeze API -> Given a user clicks the visible `冻结` or `解冻` row action / When the frontend handles the click / Then it calls the dedicated row temporary-freeze update API and refreshes the list instead of opening the child work-order form.

RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle-source.mjs` -> FAIL, the production work-order list still wired `handleAddChild(scope.row)` and had no row temporary-freeze API method.

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle-source.mjs` -> PASS, the row action now binds `handleToggleTemporaryFrozen(scope.row)`, renders `temporaryFrozen ? '解冻' : '冻结'`, and calls the dedicated row update API.

GREEN: `pnpm exec eslint src\views\mes\pro\workorder\index.vue src\api\mes\pro\workorder\index.ts` -> PASS, the touched frontend files pass scoped static validation.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-row-freeze-toggle run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle.mjs` -> PASS, the real page at `http://127.0.0.1:8081/mes/pro/work-order` showed confirmed non-frozen rows with `冻结` and frozen rows with `解冻`, with no visible `新增` action in the sampled actionable rows.
