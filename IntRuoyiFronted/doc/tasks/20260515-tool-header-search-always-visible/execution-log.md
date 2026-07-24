# Execution Log: Keep Header Search Visible And Hide Tenant/Screenfull

BDD: top header should hide tenant and screenfull controls -> Given the real frontend shell at `http://localhost:8081` loads after login, When the user views the top-right tool area on `/index`, Then the tenant select and screenfull controls should not be visible.

BDD: top header should keep the menu search input visible -> Given the real frontend shell at `http://localhost:8081` loads after login, When the user views the top-right tool area on `/index`, Then the menu search input should already be visible and ready for typing without clicking a search icon first.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session tool-header-search-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-tool-header-search-always-visible\scripts\verify-tool-header-visibility.mjs` -> FAIL, the real header still showed `tenantVisible=true`, `screenfullVisible=true`, and `searchVisible=false`.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session tool-header-search-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-tool-header-search-always-visible\scripts\verify-tool-header-visibility.mjs` -> PASS, the real header showed `tenantVisible=false`, `screenfullVisible=false`, and `searchVisible=true`.

GREEN: `pnpm.cmd exec eslint src/components/RouterSearch/index.vue src/layout/components/ToolHeader.vue` -> PASS.

Verification note: `pnpm.cmd ts:check` still fails on unrelated pre-existing repository-wide TypeScript errors outside this task scope; no new failure was attributed to the two changed header files during this fix.
