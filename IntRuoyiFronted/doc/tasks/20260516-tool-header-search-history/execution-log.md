# Execution Log: Header Menu Search Recent History

BDD: recent menu searches should appear in the dropdown -> Given the user has already completed menu searches from the top-header input, When the user reopens the search input with an empty keyword, Then the dropdown should show up to 10 recent search records.

BDD: the latest recent search should appear first -> Given multiple menu searches have been completed, When the recent-search dropdown is shown, Then the most recent record should be listed before older records.

BDD: selecting a recent search should provide quick access -> Given the recent-search dropdown is shown, When the user selects a recent record, Then the frontend should navigate to the stored route without requiring the user to type the keyword again.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session router-search-history-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-tool-header-search-history\scripts\verify-router-search-history.mjs` -> FAIL, the dropdown timed out waiting for `最近搜索` after two prior route searches, which confirmed the current UI did not expose recent search history.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session router-search-history-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-tool-header-search-history\scripts\verify-router-search-history.mjs` -> PASS, the dropdown showed the latest-first history order `["router.home/index/index", "common.profile/user/profile/user/profile"]` and selecting the older record navigated back to `/user/profile`.

GREEN: `pnpm.cmd exec eslint src/components/RouterSearch/index.vue` -> PASS.
