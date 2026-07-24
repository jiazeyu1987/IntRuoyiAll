# Task: 工单详情 ERP 同步 BOM 后端

## Goal

为 MES 生产工单提供工单级 `ERP同步BOM` 接口：按工单产品编号查询 Kingdee 已审核且唯一版本的 BOM，覆盖当前工单 BOM，并驱动物料需求随工单 BOM 一起刷新。

## Scope

- 在 `yudao-module-erp` 新增 Kingdee BOM 查询客户端、DTO 与 `bom.query-limit` 配置校验。
- 在 `yudao-module-mes` 新增工单级 ERP BOM 同步服务、控制器接口、响应 VO 与 fail-fast 校验。
- 仅复用现有工单 BOM 表；不新增数据库表。
- 真实运行时若缺少本地 MES 物料映射，必须直接失败并暴露缺失编码。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-electronic-batch-record-image-timeout-logging/task.md`
- Status before this task: completed.
- Impact: no unfinished latest backend task blocked this delivery.

## Milestones

- [x] M1: Create backend task directory, task doc, execution log, and evidence file.
- [x] M2: Record BDD scenarios and RED evidence for the BOM client and sync service.
- [x] M3: Implement Kingdee BOM query client, DTOs, and config validation.
- [x] M4: Implement MES work-order BOM sync service, endpoint, and fail-fast validations.
- [x] M5: Run targeted backend verification and update evidence.
- [ ] M6: Commit only backend files produced by this task.

## Expected Verification

- `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeWorkOrderBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed and live-verified. The backend client, sync endpoint, fail-fast validations, and targeted tests are in place. Besides the mapped candidate `903544 / KDMO-309319-4244774834`, the originally blocked work order `903245` was also re-verified successfully after补齐缺失主数据映射。

## Blocker And Impact

- Blocker: no code-path blocker remains for this task; only future live products that still缺少本地 ERP/MES 主数据时，才会继续命中 fail-fast。
- Impact: the feature itself is now verified end to end on both a pre-mapped candidate and the previously blocked sampled work order.

## Final Verification Result

- RED: backend test-first stage initially failed because `ErpKingdeeBomClientImpl`, `ErpKingdeeBomLine`, `MesKingdeeWorkOrderBomSyncServiceImpl`, and `ErpKingdeeProperties#getBom()` did not exist.
- GREEN: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeWorkOrderBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- GREEN: live frontend request hit `/admin-api/mes/pro/work-order/903245/sync-erp-bom` and surfaced the missing-item blocker instead of silently downgrading.
- GREEN: live direct backend sync on `/admin-api/mes/pro/work-order/903544/sync-erp-bom` returned `{"workOrderId":903544,"erpBomVersion":"A003.017.15.001.2001_V1.0","syncedBomCount":3}` and persisted 3 BOM rows.
- GREEN: after补齐 `A002.09.001.000021` and `A002.11.001.000012` 的本地 ERP/MES 主数据，live direct backend sync on `/admin-api/mes/pro/work-order/903245/sync-erp-bom` returned `{"workOrderId":903245,"erpBomVersion":"YXN.037.011.1002_V1.1","syncedBomCount":27}` and persisted 27 BOM rows.
