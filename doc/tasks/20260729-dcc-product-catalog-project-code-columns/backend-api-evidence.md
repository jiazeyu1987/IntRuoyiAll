# Backend API Evidence

## Scope

- Endpoint: `/dcc/product-catalog/page`
- Data contract: `DccProductCatalogDO`, `DccProductCatalogRespVO`, save/update VO, TypeScript API type.

## Contract

- Product catalog rows expose `projectName` and `projectCode`.
- No fallback values are generated in service code; values come from persisted catalog columns.

## Validation

- Create/update validation remains unchanged: `dataSource` and `product` are required, project fields are optional persisted metadata.
- Page API response validation is covered by service and controller tests.

## BDD

- BDD: 产品目录显示项目对应关系 -> Given persisted project fields exist on a product catalog row, When page API returns rows, Then response includes `projectName` and `projectCode`.

## RED / GREEN

- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing project field methods/builders.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogControllerTest,DccProductCatalogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.

## Verification

- `DccProductCatalogDO`, `DccProductCatalogRespVO`, and `DccProductCatalogSaveReqVO` include `projectName/projectCode`.
- Service conversion returns persisted project fields without fallback generation.
- Create/update payloads preserve manually supplied project fields.

## Blockers

- none
