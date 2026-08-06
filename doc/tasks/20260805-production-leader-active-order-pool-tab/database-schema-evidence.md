# Database Schema Evidence - Press Balloon Route Copy

## Data

- Goal: Copy tenant 1 MES route `RT000028 / 球囊扩张压力泵` into a new route `RT000028-IDI / 按压式球囊扩充压力泵`, then associate every tenant 1 product named `按压式球囊扩充压力泵` to the new route.
- Affected entities: `mes_pro_route`, `mes_pro_route_version`, `mes_pro_route_process`, `mes_pro_route_flow_config`, `mes_pro_route_flow_process_config`, `mes_pro_route_flow_process_batch_record`, `mes_pro_route_process_flow_*`, `mes_pro_route_schedule_config`, `mes_pro_route_product`, `mes_pro_route_product_bom`, `mes_md_item`.
- Resulting route: `routeId=980091`, `code=RT000028-IDI`, `name=按压式球囊扩充压力泵`, active version `routeVersionId=622 / V1 / ACTIVE`.
- Final product bindings: `907063 / YXN.002.006.1003 / INT-ID-233`, `913662 / YXN.002.006.1001 / INT-ID-243`, `924008 / IDI`.

## Migration

- No schema migration was created or required.
- Database engine: local Docker MySQL container `int-ruoyi-mysql`, database `ruoyi-vue-pro`, tenant `1`.
- Route copy used formal API `POST /admin-api/mes/pro/route/copy` with source route `922119`, target code `RT000028-IDI`, and target name `按压式球囊扩充压力泵`.
- Product convergence used one guarded MySQL transaction scoped to newly copied route `980091`; it soft-deleted copied source product bindings on that new route, inserted missing target-product bindings, soft-deleted mismatched target-route product BOM rows, and refreshed active version snapshot `configSnapshots.products/productBoms` for route version `622`.

## Safety

- Precheck confirmed no existing active route used `RT000028-IDI` or `按压式球囊扩充压力泵` before copy.
- Precheck confirmed exactly 3 target products existed and had 0 active route bindings before copy.
- The convergence transaction asserted target route identity, target product count, target active route version, valid snapshot JSON, final 3 target bindings, and 0 old product bindings before commit.
- Scope was limited to local tenant `1`; no remote server, production database, mock data, QA-regulation backfill, production order creation, or schedule-order creation was performed.

## Rollback

- Recovery option: soft-delete route `980091` and its route-owned children if the copied route must be removed, or restore soft-deleted copied product rows on route `980091` if the product association decision is reversed.
- The original source route `922119` and its product bindings were not modified.
- The target-product route bindings are traceable by IDs `923072`, `923073`, and `923074`.

## BDD

- BDD: Press balloon route copy and product association -> Given tenant 1 has source route `RT000028 / 球囊扩张压力泵` and exactly 3 products named `按压式球囊扩充压力泵`, When the formal route-copy API is called and the copied route product bindings are converged, Then the new route retains copied route configuration and has exactly those 3 target product bindings.
- BDD: No schedule-order fabrication -> Given the target products currently have no production or schedule orders, When route association is completed, Then no production work order or schedule order is created as a side effect.

## RED

- RED: Initial precheck SQL using hex Chinese constants without explicit collation -> FAIL with MySQL `ERROR 1270 Illegal mix of collations`; expected reason was the project collation gate requiring explicit target-column collation.
- RED: First Node route-copy attempt -> FAIL with `ECONNREFUSED 127.0.0.1:48081`; expected reason was local backend `48081` not listening, so no API write occurred.

## GREEN

- GREEN: `node doc/tasks/20260805-production-leader-active-order-pool-tab/tmp-copy-press-balloon-route.cjs` -> PASS, returned `targetRouteId=980091`.
- GREEN: `tmp-bind-press-balloon-products.sql` -> PASS, returned `target_route_id=980091`, `target_version_id=622`, `target_item_count=3`, `final_target_product_bindings=3`, `final_old_product_bindings=0`.
- GREEN: `tmp-press-balloon-verify.sql` -> PASS, target route `980091 / RT000028-IDI / 按压式球囊扩充压力泵` has active version `622 / V1`, 14 copied processes, 2 copied route flow configs, 14 active schedule configs, 3 active target products, 0 old products, 0 product BOMs, and snapshot item IDs `[907063, 913662, 924008]`.

## Verification

- Source-to-target route process count: `14 -> 14`.
- Source-to-target route flow config count: `2 -> 2`.
- Source active schedule config count to target active schedule config count: `14 -> 14`.
- Product lookup by target products now resolves all 3 products to route `980091 / RT000028-IDI`.
- Read-only order check: target products currently have `0` production work orders and `0` schedule orders, so this association does not by itself create an active-order candidate.

## Blockers

- No blocker remains for the requested route copy and product association.
- Separate blocker remains for active-order E2E: the 3 target products currently have no production work orders or schedule orders, and existing active-order candidates are still gated by QA regulation and plan-date prerequisites.