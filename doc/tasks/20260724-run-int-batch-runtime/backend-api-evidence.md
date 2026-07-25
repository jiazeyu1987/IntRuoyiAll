# Backend API Evidence

## Scope

- Restored compile-time contracts required to start `yudao-server` for the `int_batch` local runtime.
- Affected backend scope: BPM Form Center runtime contracts and ERP Kingdee sync runtime contracts.

## Contract And Failure Behavior

- BPM Form Center runtime now exposes the controller and Flowable listener interfaces required by existing routes.
- ERP Kingdee sync runtime now exposes command, context, task, result, and service contracts used by purchase, sale, stock, and MES production-order sync jobs.
- Runtime failures are surfaced by exceptions; no mock success or silent fallback is returned.
- ERP sync runtime records `RUNNING`, `SUCCESS`, and `FAILED` states through the existing run/watermark mappers.

## BDD

- BDD: Backend package can reach server module -> Given the branch runtime needs `yudao-server-exec.jar`, When Maven packages `yudao-server`, Then missing runtime contracts must compile instead of skipping modules.
- BDD: Kingdee sync jobs expose failures -> Given a sync runtime failure, When the job executes, Then the exception propagates to the scheduler path.

## RED

- RED: `mvn.cmd -pl yudao-module-bpm -DskipTests compile` -> FAIL, missing `FormCenterRuntimeService` and `FormCenterBpmEventBridge`.
- RED: `mvn.cmd -pl yudao-server -am -DskipTests package` -> FAIL, missing ERP `service.sync.runtime` contracts and later MES initial-window fields.

## GREEN

- GREEN: `mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterRuntimeContractTest,FormCenterRepositoryBoundaryTest" test` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-erp "-Dtest=KingdeePurchaseOrderSyncJobTest,KingdeeSaleOrderSyncJobTest,KingdeeStockSyncJobTest" test` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-mes -DskipTests compile` -> PASS.
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS.

## Runtime Resolution

- 本机 `127.0.0.1:3306` 的 MySQL 拒绝配置凭据；根据用户要求，运行时沿用 `E:\IntRuoyi` 已验证的 Docker MySQL 参数。
- 后端已在 `48041` 监听，健康检查返回 `{"status":"UP"}`。
