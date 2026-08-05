# AC-M01 ERP 确认生产订单候选查询执行日志

## User Intent

- 用户要求在既有岗位需求矩阵分析基础上继续推进 `AC-M01 | 确认生产订单`，明确当前系统已经做到哪一步，并继续把不符合项推进到可验证实现。

## Rules And Skills Loaded

- Project rules: `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`。
- Skills: `backend-api-delivery`、`frontend-feature-delivery`、`quality-assurance-test-suite`。
- Skill contracts: backend API evidence、frontend feature evidence、QA evidence。

## BDD

- BDD: ERP confirmed formal order candidate -> Given ERP 已同步并确认的生产订单存在正式 ERP ID/编号和本租户权限；When 计划排产员或生产班组长按正式 ID/编号查询候选订单；Then 该订单进入候选结果，结果保留同一正式 ID/编号供后续确认生产订单使用。
- BDD: ERP unconfirmed order excluded -> Given ERP 生产订单尚未达到确认状态；When 用户查询生产订单候选；Then 该订单不得出现在候选结果中。
- BDD: Missing formal ERP identity excluded -> Given 本地工单缺少正式 ERP ID/编号或缺少同步记录；When 用户查询生产订单候选；Then 该订单不得出现在候选结果中。
- BDD: Unauthorized or cross-tenant order excluded -> Given 订单属于其他租户或当前用户无查询权限；When 用户查询生产订单候选；Then API/UI 不返回该订单并保留现有权限错误语义。

## Milestone Updates

- M1: completed。AC-M01 可见入口定位为 `排产工单 -> 选中工单加入排产工单池`；后端入口为 `GET /mes/pro/schedule-order/admission-diff` 和 `POST /mes/pro/schedule-order/create-from-work-orders`。当前系统已做到 admission-diff 默认按本地生产工单 `CONFIRMED` 查询，但 `createFromWorkOrders` 仍可直接提交未确认工单，且缺 ERP/Kingdee 同步正式身份只在 preflight 中作为 `WARN_ERP_SYNC_RECORD_MISSING`，未作为候选准入 blocker。
- M2: completed。新增后端 RED：`getAdmissionDiff_shouldBlockConfirmedOrderWithoutFormalErpSyncIdentity`、`createFromWorkOrders_shouldFailFastWhenSelectedWorkOrderMissingErpFormalIdentity`、`createFromWorkOrders_shouldFailFastWhenSelectedWorkOrderNotConfirmed`。
- M3: completed。实现后端正式准入：工单必须 `CONFIRMED`，且 `mes_kingdee_production_order_sync_record` 必须匹配 `workOrderId` 并具备非空 `sourceFid/sourceBillNo`。
- M4: completed。新增前端静态合同并补齐 `BLOCKED_ERP_SYNC_RECORD_MISSING` 本地原因码文案。
- M5: blocked。真实 E2E 尚未执行；需要确认本机运行态、登录账号/权限、以及任务自有 ERP 已同步正式订单样本。
- M6: in_progress。证据文件已生成，准备运行 evidence validators；提交推送受既有脏工作区和 ahead 状态阻塞。

## RED/GREEN Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，3 个预期业务失败：缺 ERP 正式身份仍 READY，缺正式身份批量提交未抛错，未确认工单批量提交未抛错。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，70 tests, failures=0, errors=0, skipped=0。
- RED: `node tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> FAIL，缺少 `BLOCKED_ERP_SYNC_RECORD_MISSING` 本地原因码文案。
- GREEN: `node tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- REGRESSION: `node tests\e2e\smart-scheduling-smoke-real-flow-static.spec.js` -> FAIL，历史/相邻 blocker：`autoSchedulePublishResult` 标记缺失，非本次 AC-M01 ERP 准入改动。

## Blockers

- Real E2E blocked/pending：尚未确认本次任务自有 ERP 已同步正式订单样本、登录账号/权限和真实页面运行态，不能把 API-only 或静态测试标记为 AC-M01 真实 E2E PASS。
- Git closeout blocked：`git status --short --branch` 显示 `int_main...origin/int_main [ahead 1]` 且存在大量非本任务脏改/未跟踪文件；未获得用户授权前不能做全量脏工作区基线提交或推送。
