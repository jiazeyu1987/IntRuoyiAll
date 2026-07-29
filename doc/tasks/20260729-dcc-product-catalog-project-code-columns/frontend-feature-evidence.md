# Frontend Feature Evidence

## Feature Goal

- Show DCC project name and project code columns in the DCC product catalog list.

## Entry Point

- `IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue`
- `IntRuoyiFronted/src/api/dcc/controlledFile/productCatalog.ts`

## Acceptance

- The product catalog API type includes optional `projectName` and `projectCode`.
- The product catalog table includes user-configurable “项目名称” and “项目代码” columns.
- The maintenance dialog preserves project fields on edit.

## BDD

- BDD: 产品目录显示项目对应关系 -> Given API rows include `projectName` and `projectCode`, When the product catalog table renders, Then the table exposes “项目名称” and “项目代码” columns bound to those fields.

## RED / GREEN

- RED: `node tests\e2e\dcc-basic-data-product-catalog-static.spec.js` -> FAIL, missing API `projectName/projectCode`.
- RED: `node tests\e2e\dcc-product-catalog-unified-list-template-static.spec.js` -> FAIL, missing product catalog column registration.
- GREEN: `node tests\e2e\dcc-basic-data-product-catalog-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\dcc-product-catalog-unified-list-template-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Product catalog TS API types expose `projectName/projectCode`.
- Product catalog table displays “项目名称” and “项目代码” columns under the standard user-column config.
- Product catalog maintenance dialog includes project fields so editing a catalog row does not wipe persisted project values.

## Blockers

- none
