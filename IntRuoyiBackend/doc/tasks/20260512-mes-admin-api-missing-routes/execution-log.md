# Execution Log: MES admin-api missing routes

BDD: MES item list endpoint exists -> Given the admin UI requests `admin-api/mes/md/item/page`, When `yudao-server` is built, Then the root Maven reactor includes `yudao-module-mes` and `yudao-server` depends on `yudao-module-mes` so `MesMdItemController` can be compiled into the server.

BDD: MES home statistics endpoints exist -> Given the admin UI requests production trend, work order status, and summary endpoints under `admin-api/mes/home-statistics`, When `yudao-server` is built, Then the root Maven reactor includes `yudao-module-mes` and `yudao-server` depends on `yudao-module-mes` so `MesHomeStatisticsController` can be compiled into the server.

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260512-enable-crm-module/task.md` is already marked blocked/interrupted, so this task can proceed without completing unrelated CRM work.
- M2: Completed. This task document and execution log were created before production code changes.
- M3: Completed. `mvn "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` from `yudao-module-mes` failed on missing MES module enablement in both `pom.xml` and `yudao-server/pom.xml`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, reactor cannot select `yudao-module-mes` because the root reactor does not include it.
- RED: `mvn "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, assertions fail because `yudao-module-mes` is absent from the root reactor and `yudao-server/pom.xml`.
- M4: Completed. Added `MesModuleEnablementTest` in `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesModuleEnablementTest.java`.
- M5: Completed. Root `pom.xml` now includes `yudao-module-mes` and `yudao-server/pom.xml` depends on `yudao-module-mes`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS.
- Runtime verification: `Invoke-WebRequest` against `/admin-api/mes/md/item/page`, `/admin-api/mes/home-statistics/summary`, `/admin-api/mes/home-statistics/work-order-status`, and `/admin-api/mes/home-statistics/production-trend?days=7` returned HTTP 200.
- M6: Completed. Verification evidence recorded; commit must stage only this task's docs, MES test, and MES POM enablement hunks.
- Reproduction: Runtime log `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260512-193600.out.log` shows `NoResourceFoundException` for `/admin-api/mes/md/item/page`, `/admin-api/mes/home-statistics/summary`, `/admin-api/mes/home-statistics/work-order-status`, and `/admin-api/mes/home-statistics/production-trend`.
- Root cause: `MesMdItemController` and `MesHomeStatisticsController` exist under `yudao-module-mes`, but `pom.xml` and `yudao-server/pom.xml` previously commented out `yudao-module-mes`.
