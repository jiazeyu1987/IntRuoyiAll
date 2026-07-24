# Execution Log: MES route sweep

BDD: MES submenu routes load through the real admin UI -> Given an authenticated admin user is on the running frontend, When every child route under the MES system menu is opened through the frontend router, Then each route must render without unhandled frontend errors, failed MES API initialization, missing-route responses, or backend system exceptions.

BDD: MES route failures stay visible -> Given a MES child route has a missing backend route, missing schema, or frontend runtime error, When the route is opened in Playwright, Then the sweep must record the exact failing route and error instead of hiding it with mock data, fallback, or skipped navigation.

## Evidence

- M1: Created task package before route discovery and marked the unfinished BPM schema task as blocked / paused for this newer MES request.
- M2: Discovered 61 MES leaf routes from real authenticated menu data (`/admin-api/system/auth/get-permission-info`), including 60 visible routes and 1 hidden route with a real component (`/mes/wm/barcode/config`).
- RED: first Playwright sweep across the 61 MES routes -> FAIL, 54 routes returned initialization errors.
- RED: failing initialization requests were concentrated on missing `mes_*` tables, for example:
  - `/admin-api/mes/md/unit-measure/page`
  - `/admin-api/mes/md-client/page`
  - `/admin-api/mes/wm/warehouse/page`
  - `/admin-api/mes/pro/process/page`
  - `/admin-api/mes/qc/template/page`
  - `/admin-api/mes/cal/holiday/list`
- RED: backend log `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260512-200448.out.log` recorded `SQLSyntaxErrorException` for missing tables such as `mes_md_unit_measure`, `mes_md_client`, `mes_md_vendor`, `mes_md_workshop`, `mes_wm_warehouse`, `mes_wm_product_issue`, `mes_qc_template`, `mes_tm_tool`, `mes_cal_holiday`, and many other `mes_*` tables.
- RED: the three routes `生产领料` / `产品入库` / `销售出库` initially showed `[商城系统 yudao-module-mall - 已禁用]`, but log inspection proved the handler was triggered by missing MES tables named `mes_wm_product_*`, not by a real mall dependency on those route loads.
- GREEN: added reproducible schema generation and validation scripts under `doc/tasks/20260512-mes-route-sweep/scripts/`.
- GREEN: `node doc\tasks\20260512-mes-route-sweep\scripts\generate-mes-base-schema.cjs` -> PASS, generated `sql\mysql\20260512_mes_base_schema.sql` with 133 tables.
- GREEN: `node doc\tasks\20260512-mes-route-sweep\scripts\validate-mes-schema.cjs` -> PASS.
- GREEN: `cmd /c "docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro < sql\mysql\20260512_mes_base_schema.sql"` -> PASS.
- GREEN: `SELECT COUNT(*) FROM information_schema.tables ... table_name LIKE 'mes\_%'` -> PASS, 133 MES tables present in local MySQL after import.
- GREEN: second Playwright sweep across the same 61 MES routes -> PASS, `FailureCount = 0`.
