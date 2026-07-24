# Task: 恢复 5 条 active 工艺路线

## Goal
将本地 MES 工艺路线数据恢复为 IntGY 本地路由中心当前导出的 5 条 active 工艺路线：
- `PRD-TAB-STD-001`
- `PROD-CATH-6F-STD`
- `PRD-TAB-PKG-002`
- `ROUTE-YXN.044.02.1020`
- `ROUTE-YXN.069.001.1001`

不恢复 `PRD-LIQ-STD-003` disabled 路线。

## Milestones
- [x] M1: 记录当前本地 MES 工艺路线仅剩 demo 数据的现状。
- [x] M2: 从 IntGY 本地路由中心和现有导出文档提取 5 条 active 路线的完整步骤。
- [x] M3: 回填工艺路线、工序和产品绑定数据到本地 MySQL。
- [x] M4: 真实接口验证工艺路线列表恢复为 5 条 active 路线。

## Verification
- `SELECT COUNT(*) FROM mes_pro_route WHERE deleted = 0 AND tenant_id = 1`
- `GET /admin-api/mes/pro/route/simple-list`

## Status
- Completed.

## Completed Work
- 从 IntGY 本地 `process_route_center.sqlite3` 恢复 5 条 active 工艺路线。
- 回填 5 条 route 主数据、57 条 route_process、5 条 route_product 绑定。
- 软删除本地 demo route `AUTO-ROUTE-01`，避免其继续污染工艺路线列表。

## Final Verification
- `SELECT COUNT(*) FROM mes_pro_route WHERE deleted = 0 AND tenant_id = 1` -> `5`
- `GET /admin-api/mes/pro/route/simple-list` -> 返回 5 条路线
