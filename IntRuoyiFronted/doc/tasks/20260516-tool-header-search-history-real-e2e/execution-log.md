# Execution Log: Header Menu Search History Real E2E Verification

BDD: recent menu-search history works on the live frontend -> Given the local frontend at `http://127.0.0.1:8081` is running and the user logs in through the real login path, When the user performs menu searches and then reopens the search dropdown with an empty keyword, Then the live dropdown should show recent search records with the latest record first and allow quick navigation by selecting a record.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session router-search-history-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-tool-header-search-history\scripts\verify-router-search-history.mjs` -> PASS, the live dropdown showed recent-search order `["router.home/index/index", "common.profile/user/profile/user/profile"]` and selecting the older record navigated to `/user/profile`.
