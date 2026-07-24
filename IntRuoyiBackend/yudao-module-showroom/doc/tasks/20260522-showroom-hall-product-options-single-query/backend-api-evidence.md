# Scope

- Add a dedicated admin hall product options API for the hall mapping dialog.
- Keep the existing hall mapping write endpoint unchanged.
- Return lightweight candidate data: `productId`, `productCode`, `nameCn`, `revisionNo`, `incomplete`, and `hallIds`.

## Contract

- New endpoint: `GET /showroom/hall/product-options`
- Response item shape: `HallProductOptionRespVO(Long productId, String productCode, String nameCn, Integer revisionNo, boolean incomplete, List<Long> hallIds)`
- Data source rules:
  - product id/code and incomplete flag come from `showroom_product`
  - current revision name/revision number come from the current revision when present, otherwise the latest revision
  - hall ids come from `showroom_hall_product`

## Validation

- The endpoint must be readable in one request by the frontend hall mapping dialog.
- The API must not change `/showroom/hall/update-product-mapping`.
- The implementation must stay fail-fast if a referenced current revision is missing.

BDD: hall product candidate API should return all candidate products with hall ids in one request -> Given the admin hall mapping dialog needs real product ids, names, revision numbers, and existing hall relations When the frontend loads candidate products Then the backend should provide one dedicated lightweight response instead of requiring repeated `/showroom/product/page` pagination

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，修复前 `ShowroomAdminController` 不存在 `getHallProductOptions()`，新增集成测试无法编译。

GREEN: `mvn -pl yudao-module-showroom clean "-Dtest=ShowroomHttpApiIntegrationTest#hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
GREEN: live HTTP verification `GET http://127.0.0.1:48081/admin-api/showroom/hall/product-options` with test tenant auth -> PASS, `code = 0`, `count = 180`, elapsed about `42.68ms`.

## Verification

- Integration contract passed: `ShowroomHttpApiIntegrationTest#hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall`
- The endpoint now returns multi-hall product occupancy via `hallIds`, including `[]` when a product is not currently mapped to any hall.
- The implementation uses one candidate API request instead of forcing the frontend to traverse `/showroom/product/page`.
- Live local runtime returned keys `hallIds`, `incomplete`, `nameCn`, `productCode`, `productId`, `revisionNo`

## Blockers

- None after restarting the local runtime with `restart-ruoyi.bat`.
