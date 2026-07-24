# Execution Log: IntPP auto schedule first loop backend

BDD: Preview current schedule -> Given schedulable MES work orders, routes, BOMs, stock, workstation bindings, production lines, and shift capacity exist, When an administrator requests auto-schedule preview, Then the backend returns planned tasks, issues, and dependency links without writing `mes_pro_task`.

BDD: Block publish on material shortage -> Given at least one required component is short for the requested work order scope, When an administrator requests schedule apply, Then the backend returns a blocking shortage issue and does not write formal tasks.

BDD: Preserve protected tasks during apply -> Given locked, manually adjusted, finished, or frozen tasks already exist in the current schedule, When an administrator applies a new auto schedule, Then the backend keeps the protected tasks unchanged and writes only the allowed new auto-scheduled tasks.

BDD: Synchronize scheduled quantities on publish -> Given a preview can be applied successfully, When the backend publishes the schedule, Then the written `mes_pro_task` set and each work order `quantityScheduled` remain consistent.

BDD: Expose route-based task dependencies -> Given the selected work orders reference multi-step routes, When preview is generated, Then the backend derives ordered process dependencies for Gantt rendering.

## Evidence

- M1/M2: Completed. Previous backend task status was checked and this backend task document plus BDD scenarios were created before production code changes.
- RED: `mvn -f yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest" test` -> FAIL, before this implementation the auto-schedule service, controller, data objects, and scheduling tables did not exist.
- GREEN: `mvn -f yudao-module-mes\pom.xml -DskipTests compile` -> PASS.
- GREEN: `mvn -f yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest" test` -> PASS, 2 tests run, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-server -am -DskipTests compile` -> PASS, root reactor now includes `yudao-module-mes` and `yudao-server` compiles with the MES dependency enabled.
- GREEN: local MySQL patch applied to container `int-ruoyi-mysql` using `sql\mysql\mes-auto-schedule-first-loop.sql`, and the first-loop tables plus `mes_md_workstation.production_line_id` now exist.
- GREEN: `D:\wt\intsched-be\yudao-server\target\yudao-server.jar` started successfully on `http://localhost:48080`, and the auto-schedule endpoints respond from the updated backend.
- GREEN: real local seeded MES data completed the full path: `POST /admin-api/mes/pro/auto-schedule/preview` -> 200 and `POST /admin-api/mes/pro/auto-schedule/apply` -> 200, then `mes_pro_task` contained one generated AUTO task for work order `900080` and `mes_pro_work_order.quantity_scheduled` became `1.00`.
- GREEN: repeatable scripts `sql\mysql\mes-auto-schedule-first-loop-demo-clean.sql` and `sql\mysql\mes-auto-schedule-first-loop-demo-data.sql` were executed successfully, then the same local success path was replayed again and produced a new AUTO task `PT-0002` with `quantity_scheduled = 1.00`.
