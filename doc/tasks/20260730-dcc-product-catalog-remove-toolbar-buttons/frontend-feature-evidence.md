# Frontend Feature Evidence

## Feature Goal

Remove the two highlighted DCC product catalog action buttons: `重置` and `注册证有效期`.

## Non-Goals

- Do not remove `新增产品目录`.
- Do not change backend APIs, permissions, table columns, row edit/delete actions, quick filter definitions, or data loading.
- Do not introduce fallback, mock data, or hidden error handling.

## UI Entry Point

- Page title: `基础数据 / DCC产品目录`.
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue`.

## Owned Files

- `IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue`
- `IntRuoyiFronted/tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js`
- `IntRuoyiFronted/tests/e2e/dcc-basic-data-product-catalog-static.spec.js`
- `doc/tasks/20260730-dcc-product-catalog-remove-toolbar-buttons/*`

## API Contracts

No API contract changes. The removed `注册证有效期` button means the compare action is no longer user-triggered from this toolbar.

## BDD Scenario

Given a user opens the DCC product catalog list, When the toolbar is rendered, Then `新增产品目录` remains available and the highlighted `重置` / `注册证有效期` toolbar buttons are not rendered.

## RED / GREEN Evidence

- RED: `node tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js` failed because the existing actions slot still rendered `productCatalogQuickFilter.resetQuickFilter` and `handleCompareRegistrationExpiry`.
- GREEN: `node tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js` passed.
- REGRESSION: `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` passed.
- TYPECHECK: `pnpm ts:check` passed.

## UX Checks

- Toolbar remains usable with `新增产品目录`.
- Standard quick filter query remains wired through `UnifiedListTemplate`.
- Display-field and table column configuration remain unchanged.

## Blockers

- Shared-branch commit boundary was affected by concurrent baseline commit `4158334f`; no DCC functional blocker remains.
