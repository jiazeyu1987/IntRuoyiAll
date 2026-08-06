# Database Schema Evidence - Press Balloon Route Copy

## Data

- Goal: Copy tenant 1 MES route `RT000028 / 球囊扩张压力泵` into a new route `RT000028-IDI / 按压式球囊扩充压力泵`, associate every tenant 1 product named `按压式球囊扩充压力泵` to the new route, and bind the target route to its formal DCC project code and MDM product.
- Affected entities: `mes_pro_route`, `mes_pro_route_version`, `mes_pro_route_process`, `mes_pro_route_flow_config`, `mes_pro_route_flow_process_config`, `mes_pro_route_flow_process_batch_record`, `mes_pro_route_process_flow_*`, `mes_pro_route_schedule_config`, `mes_pro_route_product`, `mes_pro_route_product_bom`, `mes_md_item`, `dcc_project_code`, `mdm_product`.
- Resulting route: `routeId=980091`, `code=RT000028-IDI`, `name=按压式球囊扩充压力泵`, active version `routeVersionId=622 / V1 / ACTIVE`.
- Final MES product bindings: `907063 / YXN.002.006.1003 / INT-ID-233`, `913662 / YXN.002.006.1001 / INT-ID-243`, `924008 / IDI`.
- Final DCC/MDM binding: `dcc_project_code.id=129 / project_code=IDI / product_master_id=14`, resolving to enabled `mdm_product.id=14 / product_code=INT-15 / name_cn=按压式球囊扩充压力泵`; target route product binding is `mes_pro_route_product.id=923079 / route_id=980091 / item_id=14`.

## Migration

- No schema migration was created or required.
- Database engine: local Docker MySQL container `int-ruoyi-mysql`, database `ruoyi-vue-pro`, tenant `1`.
- Route copy used formal API `POST /admin-api/mes/pro/route/copy` with source route `922119`, target code `RT000028-IDI`, and target name `按压式球囊扩充压力泵`.
- Product convergence used one guarded MySQL transaction scoped to newly copied route `980091`; it soft-deleted copied source product bindings on that new route, inserted missing target-product bindings, soft-deleted mismatched target-route product BOM rows, and refreshed active version snapshot `configSnapshots.products/productBoms` for route version `622`.
- Project/MDM convergence used one guarded MySQL transaction scoped to target route `980091`, DCC project code `IDI`, and MDM product `14`; it soft-deleted the stale active `item_id=14` route-product binding on old route `922119`, inserted the target active binding `923079`, and appended `itemId=14` to active version `622` snapshot products.

## Safety

- Precheck confirmed no existing active route used `RT000028-IDI` or `按压式球囊扩充压力泵` before copy.
- Precheck confirmed exactly 3 target products existed and had 0 active route bindings before copy.
- The convergence transaction asserted target route identity, target product count, target active route version, valid snapshot JSON, final 3 target bindings, and 0 old product bindings before commit.
- Project/MDM precheck confirmed target `dcc_project_code.product_master_id=14` resolves to one enabled `mdm_product` row and that no active non-target route-product binding remains for `item_id=14` after convergence.
- Scope was limited to local tenant `1`; no remote server, production database, mock data, QA-regulation backfill, production order creation, or schedule-order creation was performed.

## Rollback

- Recovery option: soft-delete route `980091` and its route-owned children if the copied route must be removed, or restore soft-deleted copied product rows on route `980091` if the product association decision is reversed.
- The original route copy did not modify source route `922119`; the later project/MDM binding intentionally soft-deleted stale active source binding `923065 / item_id=14` so MDM product `14` is active only on target route `980091`.
- The target-product route bindings are traceable by IDs `923072`, `923073`, `923074`, and target MDM binding `923079`.

## BDD

- BDD: Press balloon route copy and product association -> Given tenant 1 has source route `RT000028 / 球囊扩张压力泵` and exactly 3 products named `按压式球囊扩充压力泵`, When the formal route-copy API is called and the copied route product bindings are converged, Then the new route retains copied route configuration and has exactly those 3 target product bindings.
- BDD: No schedule-order fabrication -> Given the target products currently have no production or schedule orders, When route association is completed, Then no production work order or schedule order is created as a side effect.
- BDD: Press balloon project code and MDM route binding -> Given DCC project code `IDI` is enabled and bound to MDM product `14`, When the press balloon route binding is converged, Then MDM product `14` is active only on route `980091` and route version `622` snapshot contains that product.

## RED

- RED: Initial precheck SQL using hex Chinese constants without explicit collation -> FAIL with MySQL `ERROR 1270 Illegal mix of collations`; expected reason was the project collation gate requiring explicit target-column collation.
- RED: First Node route-copy attempt -> FAIL with `ECONNREFUSED 127.0.0.1:48081`; expected reason was local backend `48081` not listening, so no API write occurred.
- RED: First `tmp-bind-press-balloon-project-mdm.sql` run -> FAIL with MySQL `ERROR 1267 Illegal mix of collations`; transaction was not committed and the procedure was corrected to declare UTF-8 variables with `utf8mb4_unicode_ci`.

## GREEN

- GREEN: `node doc/tasks/20260805-production-leader-active-order-pool-tab/tmp-copy-press-balloon-route.cjs` -> PASS, returned `targetRouteId=980091`.
- GREEN: `tmp-bind-press-balloon-products.sql` -> PASS, returned `target_route_id=980091`, `target_version_id=622`, `target_item_count=3`, `final_target_product_bindings=3`, `final_old_product_bindings=0`.
- GREEN: `tmp-press-balloon-verify.sql` -> PASS, target route `980091 / RT000028-IDI / 按压式球囊扩充压力泵` has active version `622 / V1`, 14 copied processes, 2 copied route flow configs, 14 active schedule configs, 3 active target products, 0 old products, 0 product BOMs, and snapshot item IDs `[907063, 913662, 924008]`.
- GREEN: `tmp-bind-press-balloon-project-mdm.sql` -> PASS, returned `target_route_id=980091`, `target_route_version_id=622`, `project_code=IDI`, `mdm_product_id=14`, `target_route_product_id=923079`, `final_target_mdm_bindings=1`, `final_non_target_mdm_bindings=0`, `final_snapshot_contains_mdm=1`.
- GREEN: `tmp-press-balloon-project-mdm-verify.sql` -> PASS, DCC project code `IDI` resolves to enabled MDM `INT-15/id=14`; target route `980091` has one active `item_id=14` binding and no active non-target `item_id=14` binding.

## Verification

- Source-to-target route process count: `14 -> 14`.
- Source-to-target route flow config count: `2 -> 2`.
- Source active schedule config count to target active schedule config count: `14 -> 14`.
- Product lookup by target products now resolves all 3 products to route `980091 / RT000028-IDI`.
- DCC project-code lookup by target project name resolves `IDI` to enabled MDM product `INT-15/id=14`; route-product lookup shows target route `980091` now has 4 active products: the 3 MES item products plus MDM product `14`.
- Read-only order check: target products currently have `0` production work orders and `0` schedule orders, so this association does not by itself create an active-order candidate.

## Blockers

- No blocker remains for the requested route copy, product association, and DCC/MDM binding.
- Separate blocker remains for active-order E2E: the 3 target products currently have no production work orders or schedule orders, and existing active-order candidates are still gated by QA regulation and plan-date prerequisites.
