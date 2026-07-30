# Frontend Feature Evidence

## Feature Goal

- 将 `/mes/pro/mes-process` 的用户可见菜单和页面标题恢复为 `MES工序`。

## Non-goals

- 不修改 MES 工序业务字段、资源池数据、权限范围或维护能力。
- 不新增兼容分支、mock 数据或写入型 E2E。

## Acceptance

- R1: 侧边栏、顶部页签或面包屑显示 `MES工序`。
- R2: 搜索 `mes工序` 能进入 `/mes/pro/mes-process`。
- R3: 页面不再显示 `标准模板列表`。
- R4: 资源列表正常加载且 E2E 不产生 MES 写请求。

## Entry And Owned Files

- Route: `/mes/pro/mes-process`
- Page: `IntRuoyiFronted/src/views/mes/pro/mes-process/index.vue`
- Search: `IntRuoyiFronted/src/components/RouterSearch/index.vue`
- Contract: `IntRuoyiFronted/tests/e2e/mes-pro-mes-process-readonly-static.spec.js`
- Menu migration: `IntRuoyiBackend/sql/mysql/20260730_mes_process_readonly_catalog_menu.sql`

## API Contract

- `GET /admin-api/mes/pro/route-resource/page`
- Expected: HTTP 200, business code 0, read-only list data.

## BDD Scenarios

BDD:

- Given admin loads the MES production menu, When the target entry is shown, Then its visible title is `MES工序`.
- Given the menu search is available, When `mes工序` is entered, Then the route `/mes/pro/mes-process` is found.
- Given the page opens, When resources load, Then the API succeeds and no MES write request is sent.

## RED

RED:

- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js`
- Result: FAIL because the page still contained `【生产】标准模板列表`.

## GREEN

GREEN:

- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js`
- Result: PASS.

## UI States And Permission Checks

- Loading state remains the existing table loading state.
- Empty and error behavior are unchanged.
- The page remains read-only; no create, edit, delete, import, enable or disable action was added.
- Real E2E used `芋道源码/admin` and sent zero MES write requests.

## Verification

- Official login preflight: PASS.
- Visible sidebar title: `MES工序`.
- Search result: `MES工序/mes/pro/mes-process`.
- Final route: `/mes/pro/mes-process`.
- Resource response: HTTP 200, code 0, total 580.
- Page errors: 0.

## Blockers

- `pnpm ts:check` did not complete within the captured timeout. The changed Vue content is a static title string and was covered by focused static contracts plus real browser E2E.
