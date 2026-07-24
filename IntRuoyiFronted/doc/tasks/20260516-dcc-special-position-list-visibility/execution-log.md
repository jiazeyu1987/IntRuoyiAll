BDD: DCC 岗位分配页显示固定本地岗位 -> Given local MySQL already contains active fixed岗位 `900333 / 900334` / When the real frontend loads `DCC岗位分配` / Then the visible list includes `部门负责人` and `部门授权代表`.

RED: pre-fix live page evidence -> FAIL, the user-visible岗位分配 page did not show `部门负责人 / 部门授权代表` even though the local MySQL rows existed.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-special-position-list-visibility\scripts\verify-dcc-position-list-special-roles.mjs` -> PASS, the real `DCC岗位分配` page showed both names and returned `rowCount=33`.
