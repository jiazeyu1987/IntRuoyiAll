BDD: DCC 路线相关预览显示固定岗位名称 -> Given 固定本地岗位 `900333` 与 `900334` 参与 DCC 审批矩阵派生 / When 用户查看路线页、矩阵预览或上传路线预览 / Then 页面显示 `部门负责人` 与 `部门授权代表`，而不是 `岗位#900333 / 岗位#900334`。

RED: pre-change code and runtime evidence -> FAIL, DCC routes preview and matrix preview used fallback formatting `岗位#${id}` for unresolved local fixed positions, and the live local rows `900333 / 900334` still carried the old names `编制部门负责人 / 授权代表`.

GREEN: live local MySQL update -> PASS, `dcc_approval_position.id=900333` was updated to `部门负责人` and `id=900334` to `部门授权代表`.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-special-position-display-names\scripts\verify-dcc-special-position-display-names.mjs` -> PASS, the routes page, category matrix dialog, and upload preview all showed `部门负责人 / 部门授权代表` and no longer showed `岗位#900333 / 岗位#900334`.
