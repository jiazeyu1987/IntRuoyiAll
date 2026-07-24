# Execution Log: DCC Tabs Missing Regression

BDD: DCC governance shows distribution and training entry points ->
Given an admin opens the DCC governance area / When the page loads / Then the
visible navigation must still expose `DCC下发` and `DCC培训`.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-tabs-missing-regression run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-tabs-missing-regression\scripts\inspect-dcc-menu.mjs`
-> FAIL initially, because the sidebar text only exposed the existing DCC child
menus plus a corrupted `DCC????` label, while `DCC下发` and `DCC培训` were both
absent from the visible DCC governance navigation.

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-tabs-missing-regression\scripts\repair-dcc-governance-menus.mjs`
-> PASS, updating the corrupted runtime menu rows to UTF-8 Chinese labels,
creating the missing `controlled-file/training` menu row, and reassigning the
admin role menu set.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-tabs-missing-regression run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-tabs-missing-regression\scripts\inspect-dcc-menu.mjs`
-> PASS, with sidebar text showing `DCC下发`, `DCC培训`, and `DCC我的培训`.
