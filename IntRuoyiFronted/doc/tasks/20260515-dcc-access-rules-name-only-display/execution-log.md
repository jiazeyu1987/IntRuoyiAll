BDD: 访问规则页目录树与标题仅显示目录名 -> Given 管理员通过真实登录进入 `DCC访问规则` 页面 and 存在 `name` 与 `code` 不同的目录 / When 用户查看左侧目录树与当前目录标题 / Then 目录树节点与当前标题只显示目录名 / And 不显示目录编码。

BDD: 访问规则页用户授权标签不应显示 undefined -> Given 管理员通过真实登录进入 `DCC访问规则` 页面 and `/admin-api/system/user/simple-list` 仅返回 `id`、`nickname`、`deptId`、`deptName` / When 页面渲染授权对象下拉与当前规则行 / Then 用户标签必须显示可读名称而不能拼出 `undefined`。

## TDD / Verification Evidence

- M1: Completed. Previous frontend task `20260515-dcc-access-rules-undefined-user-label` is completed and serves as the immediately preceding, but misinterpreted, display change.
- RED: `node doc/tasks/20260515-dcc-access-rules-name-only-display/scripts/verify-access-rules-directory-name-only-source.mjs` -> FAIL, `tree_still_displays_data_code,title_missing_selected_directory_name,title_still_displays_selected_directory_code,template_still_mentions_directory_code`.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-name-only-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-name-only-display\scripts\assert-access-rules-directory-name-only.mjs` -> FAIL, selected tree node still rendered `INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8 3.DMR`.
- GREEN: `node doc/tasks/20260515-dcc-access-rules-name-only-display/scripts/verify-access-rules-directory-name-only-source.mjs` -> PASS.
- GREEN: `pnpm exec eslint src/views/dcc/controlled-file/access-rules/index.vue` -> PASS.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-name-only-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-name-only-display\scripts\assert-access-rules-directory-name-only.mjs` -> PASS, the real page now shows only the directory name while still not rendering `undefined` user labels.
