# Task Execution Log: MES 工艺流程编辑/删除按钮可用

BDD: active route edit button is clickable -> Given the MES 工艺流程列表 contains an active route, When the operator focuses the `编辑` action, Then the button is enabled and opens the route form.

BDD: active route delete button is clickable -> Given the MES 工艺流程列表 contains an active route, When the operator focuses the `删除` action, Then the button is enabled and can open the delete confirmation flow.

RED: `node tests/e2e/mes-pro-route-actions.spec.js` -> FAIL, found the forbidden `:disabled="scope.row.status !== CommonStatusEnum.DISABLE"` guard in `src/views/mes/pro/route/index.vue`.

Completed:
- Identified the real frontend root as `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`.
- Confirmed the route list page itself hard-coded both route action buttons to disabled unless the row status was `CommonStatusEnum.DISABLE`.
- Removed the obsolete tooltip wrappers and the row-status disabled guards, while keeping `v-hasPermi="['mes:pro-route:update']"` and `v-hasPermi="['mes:pro-route:delete']"` unchanged.

GREEN: `node tests/e2e/mes-pro-route-actions.spec.js` -> PASS
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS
GREEN: live dev-server source probe -> PASS, `http://localhost:8081/src/views/mes/pro/route/index.vue` reports `guard_present False` and `tooltip_present False`
