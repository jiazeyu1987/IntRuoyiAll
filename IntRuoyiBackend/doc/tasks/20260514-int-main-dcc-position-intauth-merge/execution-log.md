# Execution Log: Merge DCC IntAuth position sync into int_main

BDD: int_main approval-position list syncs from IntAuth -> Given DCC on `int_main` already has the approval-position backend baseline, When the IntAuth position-sync delta is applied, Then `/dcc/approval-positions` synchronizes from `IntAuth /internal/quality-system/positions` and hides local seed-only rows.

BDD: same-name local positions keep DCC assignment bindings -> Given a local DCC position already has assignments, When an IntAuth position with the same name is synchronized on `int_main`, Then the local row is reused so route and assignment references stay attached.

BDD: missing IntAuth sync token still fails fast -> Given `yudao.dcc.int-auth.internal-service-token` is blank, When `int_main` tries to read IntAuth positions, Then the backend returns an explicit config-missing error instead of falling back to local-only position data.

RED: earlier direct integration attempt on `int_main` failed before source-branch completion because DCC position sync depended on source files that had not yet been committed on `feature/dcc-v1-backend`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc clean compile -DskipTests` -> PASS after porting the position-sync delta.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest,DccIntAuthPositionClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 9 tests green on `int_main`.
