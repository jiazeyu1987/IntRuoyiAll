BDD: DCC 岗位分配页隐藏来源与备注列 -> Given 管理员打开真实 `DCC岗位分配` 页面 / When 页面渲染岗位表格 / Then 表头中不再显示 `来源` 与 `备注` 两列 / And 现有岗位列表仍正常可见。

RED: pre-change source inspection -> FAIL, `src/views/dcc/controlled-file/positions/index.vue` still rendered `el-table-column label="来源"` and `el-table-column label="备注"`, which matched the user-provided screenshot.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-position-hide-source-remark\scripts\verify-dcc-position-columns.mjs` -> PASS, real login reached `DCC岗位分配`, `hasSourceHeader=false`, `hasRemarkHeader=false`, and representative岗位名称 remained visible.
