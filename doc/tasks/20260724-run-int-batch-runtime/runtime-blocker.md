# Runtime Blocker Evidence

## Current Runtime State

- Frontend `int_batch` is running on `http://127.0.0.1:8041/`.
- Frontend verification returned HTTP `200`.
- Frontend listener: port `8041`, PID `30620`, `node.exe`.
- Backend `int_batch` is running on `http://127.0.0.1:48041/actuator/health`.
- Backend verification returned `{"status":"UP"}`.
- Backend listener: port `48041`, PID `25760`, Java.

## Backend Failure

- Backend jar build succeeded and produced `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Initial backend startup failed while creating the `master` datasource.
- Failure category: MySQL authentication rejected the configured local `root` credentials on `127.0.0.1:3306`.
- Port `3306` is owned by local `mysqld.exe` PID `8312`.
- Docker has `int-ruoyi-mysql` mapped as `127.0.0.1:23306 -> 3306`, while the static backend config points to `127.0.0.1:3306`.

## Verification Completed

- `mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterRuntimeContractTest,FormCenterRepositoryBoundaryTest" test` -> PASS, 14 tests.
- `mvn.cmd -pl yudao-module-erp "-Dtest=KingdeePurchaseOrderSyncJobTest,KingdeeSaleOrderSyncJobTest,KingdeeStockSyncJobTest" test` -> PASS, 9 tests.
- `mvn.cmd -pl yudao-module-mes -DskipTests compile` -> PASS.
- `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS.

## Resolution

用户要求采用 `E:\IntRuoyi` 相同的 MySQL 连接方式，因此后端使用 `E:\IntRuoyi` 已验证的显式 Docker MySQL 参数：`127.0.0.1:23306/ruoyi-vue-pro`，并使用 Docker Redis `127.0.0.1:26379`。共享配置文件未修改。
