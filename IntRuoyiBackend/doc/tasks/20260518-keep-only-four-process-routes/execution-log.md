# Execution Log: 工艺路线仅保留图示 4 条

BDD: 工艺路线列表仅保留图示 4 条 -> Given 本地 MES 工艺路线表中存在图示 4 条之外的路线 / When 执行本次真实库清理 / Then 系统仅保留 `ROUTE-XLSX-00001`、`ROUTE-XLSX-00002`、`ROUTE-YXN.044.02.1020`、`ROUTE-YXN.069.001.1001` 四条未删除路线，其他路线全部为停用并删除状态，且其 route_process / route_product / route_product_bom 关联数据被清理。
RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProFeedbackControllerHistoryDisplayTest,MesProTaskControllerHistoryDisplayTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `MesProRouteProcessService` 缺少历史读取的 `getRouteProcessListByRouteIdsIgnoreDeleted` 方法。
GREEN: `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260518-keep-only-four-process-routes\scripts\prune_routes_to_four.py --dry-run` -> PASS, 预览确认 28 条非目标路线待清理，且历史引用来自 `mes_pro_feedback` / `mes_pro_task` / `mes_pro_route_process` / `mes_pro_route_product`。
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProFeedbackControllerHistoryDisplayTest,MesProTaskControllerHistoryDisplayTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 历史读取回归测试通过。
GREEN: `python D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260518-keep-only-four-process-routes\scripts\prune_routes_to_four.py` -> PASS, 非目标路线已停用并逻辑删除，保留路线仅剩 4 条。

Final verification:
- `SELECT code, status, deleted FROM mes_pro_route WHERE deleted = 0 ORDER BY code` -> PASS, only 4 keep codes remain.
- `SELECT code, status, deleted FROM mes_pro_route WHERE code = 'AUTO-ROUTE-01' OR code LIKE 'TPFBRT-%' ORDER BY code` -> PASS, deleted rows show `status=1` and `deleted=1`.
- `SELECT route_id, COUNT(*) FROM mes_pro_route_process WHERE deleted = 0 GROUP BY route_id` -> PASS, only `900021`, `900022`, `900025`, `900026` remain active.
- `SELECT route_id, COUNT(*) FROM mes_pro_route_product WHERE deleted = 0 GROUP BY route_id` -> PASS, only `900025` and `900026` remain active.
- `SELECT route_id, COUNT(*) FROM mes_pro_route_product_bom WHERE deleted = 0 GROUP BY route_id` -> PASS, only `900025` and `900026` remain active.
