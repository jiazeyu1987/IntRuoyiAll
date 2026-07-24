# Execution Log: MES 工艺流程启用移除 BOM 消耗限制

BDD: enable route without configured route-product BOM consumption -> Given a route already has at least one process and one key process plus a bound product without any route-product BOM rows, When an operator enables the route, Then the route status is updated to enabled and no `产品 {} 未配置工序的 BOM 消耗` error is thrown.

BDD: keep existing process prerequisites when enabling route -> Given a route is missing all processes or missing any key process, When an operator enables the route, Then the existing process prerequisite errors remain unchanged.

Root cause: `MesProRouteServiceImpl.validateRouteEnable` treated an empty route-product BOM consumption list as a hard enable prerequisite and threw `PRO_ROUTE_ENABLE_PRODUCT_NO_BOM` before updating route status.

Regression test: `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImplTest.java`

RED: `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplTest test` -> FAIL, `updateRouteStatus_shouldEnableRouteEvenWithoutRouteProductBomConsumption` throws `产品 冠状动脉棘突球囊扩张导管 未配置工序的 BOM 消耗`.

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplTest test` -> PASS

Risk: routes can now be enabled without route-product BOM consumption rows, so downstream flows that truly require BOM consumption must continue to fail fast at their own execution boundary instead of at route enable time.

Regression scope: route enable validation, especially the preserved `无工序` and `无关键工序` prerequisite checks.
