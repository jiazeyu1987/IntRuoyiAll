# Backend API Evidence

## Scope

- Endpoint scope: `GET /mes/pro/schedule-order/admission-diff` and `POST /mes/pro/schedule-order/create-from-work-orders`.
- Service scope: `MesProScheduleOrderServiceImpl` admission candidate row building and batch admission fail-fast validation.
- Requirement: AC-M01 requires only ERP-confirmed production orders with formal ERP ID/number to enter candidate selection and batch admission.

## Contract

- API contract: admission diff rows continue to return `admissionStatus`, `reasonCode`, `severity`, `message`, and `selectable`.
- Data contract: a candidate work order must have local `MesProWorkOrderStatusEnum.CONFIRMED`, must not be temporarily frozen, and must have `MesKingdeeProductionOrderSyncRecordDO` matching the work order id with nonblank `sourceFid` and `sourceBillNo`.
- Validation: missing, mismatched, or blank formal ERP sync identity returns `BLOCKED_ERP_SYNC_RECORD_MISSING` in admission diff and raises `PRO_SCHEDULE_ORDER_WORK_ORDER_ERP_SYNC_REQUIRED` during batch admission.
- Error behavior: non-confirmed work orders now fail with `PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED`; missing formal ERP identity fails before route creation.

## BDD

- BDD: ERP confirmed formal order candidate -> Given ERP 已同步并确认的生产订单存在正式 ERP ID/编号和本租户权限；When 计划排产员或生产班组长按候选订单查询；Then 该订单进入可入池结果。
- BDD: ERP unconfirmed order excluded -> Given ERP 生产订单尚未达到确认状态；When 用户查询或批量提交生产订单候选；Then 该订单不得进入候选或入池。
- BDD: Missing formal ERP identity excluded -> Given 本地工单缺少正式 ERP ID/编号或缺少同步记录；When 用户查询或批量提交生产订单候选；Then 该订单被阻断。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected failures showed `getAdmissionDiff_shouldBlockConfirmedOrderWithoutFormalErpSyncIdentity` returned `READY_TO_ADMIT`, and `createFromWorkOrders_shouldFailFastWhenSelectedWorkOrderMissingErpFormalIdentity` / `createFromWorkOrders_shouldFailFastWhenSelectedWorkOrderNotConfirmed` threw nothing.
- RED note: first Maven attempt without `"-Dsurefire.failIfNoSpecifiedTests=false"` failed in upstream reactor modules with "No tests matching pattern"; per Maven reactor gate, this was parameter scope handling, not product behavior.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 70 tests, 0 failures, 0 errors, 0 skipped.

## Verification

- Verified admission diff blocks confirmed orders without formal ERP sync identity as `BLOCKED_ERP_SYNC_RECORD_MISSING`.
- Verified batch admission fails fast before inserts when selected work order is not `CONFIRMED`.
- Verified batch admission fails fast before inserts when selected work order lacks formal ERP sync record or formal ID/number.
- Verified existing schedule-order service tests still pass with explicit formal sync identity fixtures.

## Blockers

- Real E2E for AC-M01 was not marked PASS in this evidence because it requires a live frontend path, confirmed login context, and task-owned ERP-synced sample order.
- Commit/push closeout is blocked by pre-existing dirty worktree and branch `int_main...origin/int_main [ahead 1]` with many unrelated modified/untracked files.
