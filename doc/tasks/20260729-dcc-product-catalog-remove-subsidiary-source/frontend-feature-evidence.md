# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: DCC 产品目录前端不再展示或默认选择 `子公司产品` 数据来源。
- Goal: 新增/重置产品目录默认数据来源为 `瑛泰产品`。
- Non-goal: 不重做产品目录页面布局、不改变列表列配置、不隐藏真实接口错误。

## Requirements And Acceptance IDs

- REQ-DCC-CATALOG-SOURCE-001: 数据来源选项只允许 `瑛泰产品`。
- REQ-DCC-CATALOG-SOURCE-002: 新增和重置表单默认 `dataSource = 瑛泰产品`。
- REQ-DCC-CATALOG-SOURCE-003: 现有产品目录列表、维护、有效期比对能力保持可用。

## UI Entry Points, Routes, Components, And Owned Files

- Entry: `基础数据 / DCC产品目录`。
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue`。
- API wrapper: `IntRuoyiFronted/src/api/dcc/controlledFile/productCatalog.ts` unchanged.
- Test script: `IntRuoyiFronted/scripts/dcc-product-catalog-source-options.test.mjs`。
- Package script: `e2e:dcc:product-catalog-source-options:static`。

## API Contracts And Data States

- `DccProductCatalogPageReqVO.dataSource` remains available for filtering.
- `DccProductCatalogSaveReqVO.dataSource` remains required.
- Frontend now only supplies `瑛泰产品` from controlled options/default state.
- Backend service validation also only accepts `瑛泰产品` for create/update/delete source validation.

## BDD Scenarios

- BDD: 新增产品目录默认瑛泰来源 -> Given 用户在 DCC 产品目录页新增记录 When 打开维护表单或选择数据来源 Then 默认值和选项中只应出现 `瑛泰产品`，不能再选择 `子公司产品`。

## RED

- RED: `node scripts\dcc-product-catalog-source-options.test.mjs` -> FAIL, expected reason: `ProductCatalogTabPanel.vue` still contained `子公司产品` option/default.

## GREEN

- GREEN: `pnpm e2e:dcc:product-catalog-source-options:static` -> PASS, 1 passed.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Existing UnifiedListTemplate usage is unchanged.
- Existing loading state, empty current-page warning for expiry compare, permission directives, and API error propagation are unchanged.
- No fallback, mock data, or silent catch was introduced.

## E2E Or Component Verification Path

- `node tests\e2e\dcc-basic-data-product-catalog-static.spec.js` -> PASS.
- `node tests\e2e\dcc-product-catalog-unified-list-template-static.spec.js` -> PASS.
- `node scripts\dcc-product-catalog-registration-expiry-contract.test.mjs` -> PASS.

## Blockers And Follow-Up Skills

- No frontend blocker remains for this scoped deletion.
- Real browser E2E was not run because the change is a static source-option/data contract update and no local runtime/login path was requested or started.

