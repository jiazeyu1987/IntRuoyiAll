# Execution Log: 恢复 5 条 active 工艺路线

BDD: 工艺路线列表应恢复为 5 条 active 路线 -> Given 本地 MES route 表仅剩 demo route, When 恢复 IntGY 当前导出的 5 条 active 工艺路线, Then `/admin-api/mes/pro/route/simple-list` 应返回这 5 条路线且不包含 disabled 路线。

RED: `SELECT COUNT(*) FROM mes_pro_route WHERE deleted = 0 AND tenant_id = 1` -> FAIL, 当前本地库只剩 1 条 `AUTO-ROUTE-01` route。

GREEN: `python doc/tasks/20260517-restore-five-active-process-routes/scripts/restore_routes.py` -> PASS, restored 5 routes, 57 route_process rows, and 5 route_product rows.

GREEN: `SELECT COUNT(*) FROM mes_pro_route WHERE deleted = 0 AND tenant_id = 1` -> PASS, count is `5`.

GREEN: `GET /admin-api/mes/pro/route/simple-list` -> PASS, returned `PRD-TAB-PKG-002`, `PRD-TAB-STD-001`, `PROD-CATH-6F-STD`, `ROUTE-YXN.044.02.1020`, and `ROUTE-YXN.069.001.1001`.
