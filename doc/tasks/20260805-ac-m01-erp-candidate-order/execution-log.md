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
- M5: blocked。RRM 真实流程脚本已补入 AC-M01 动作：生产组长在 `joinActiveOrder` 前进入 `/mes/pro/schedule-order`，打开真实“同步工单”页签，按 `RRM_PRODUCTION_ORDER_CODE` 查询 `admission-diff`，并要求缺 ERP 正式身份样本 `BLOCKED_ERP_SYNC_RECORD_MISSING` 不可选；真实执行仍缺 RRM 账号、URL、电子签名和任务订单/路线/调拨数据环境变量。
- M6: blocked。目标 AC-M01 静态合同、语法检查和 RRM preflight 已通过；真实 RRM `real:check` 仍因缺少 35 个环境/数据前置项阻塞；提交推送仍受当前大量非本任务脏改动和任务边界影响。

## RED/GREEN Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，3 个预期业务失败：缺 ERP 正式身份仍 READY，缺正式身份批量提交未抛错，未确认工单批量提交未抛错。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，70 tests, failures=0, errors=0, skipped=0。
- RED: `node tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> FAIL，缺少 `BLOCKED_ERP_SYNC_RECORD_MISSING` 本地原因码文案。
- GREEN: `node tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- REGRESSION: `node tests\e2e\smart-scheduling-smoke-real-flow-static.spec.js` -> FAIL，历史/相邻 blocker：`autoSchedulePublishResult` 标记缺失，非本次 AC-M01 ERP 准入改动。
- RED: `node tests\e2e\mes-pro-schedule-order-erp-sync-rrm-action-static.spec.js` -> FAIL，RRM 真实流程脚本缺少 `verifyScheduleOrderErpCandidateAdmission`，生产组长阶段尚未把 AC-M01 纳入 action evidence。
- GREEN: `node tests\e2e\mes-pro-schedule-order-erp-sync-rrm-action-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\role-requirement-matrix-real-flow.e2e.js`、`node --check tests\e2e\role-requirement-matrix-preflight-static.spec.cjs`、`node --check tests\e2e\mes-pro-schedule-order-erp-sync-rrm-action-static.spec.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- BLOCKED: `pnpm e2e:role-requirement-matrix:real:check` -> FAIL/BLOCKED，当前 shell 缺少 35 个 RRM 真实运行前置项，包括 `RRM_FRONTEND_URL`、`RRM_BACKEND_URL`、租户、六类角色账号/密码、电子签名 JSON、任务生产订单 ID/编号、路线/版本/工序、调拨 ID 和 QA 规程版本 ID。
- GREEN: `git diff --check -- IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted\tests\e2e\mes-pro-schedule-order-erp-sync-rrm-action-static.spec.js` -> PASS，仅提示 Git 未来可能将 LF 替换为 CRLF，无 whitespace error。

## Blockers

- Real E2E blocked/pending：RRM action evidence 已接入脚本，但当前 shell 缺少真实运行所需 RRM 环境变量和任务自有 ERP 已同步正式订单/阻断样本数据，不能把 API-only 或静态测试标记为 AC-M01 真实 E2E PASS。
- Adjacent preflight resolved：`pnpm e2e:role-requirement-matrix:preflight:static` 当前 PASS，不再阻塞 AC-M01 专项静态准出。
- Git closeout blocked：`git status --short --branch` 当前显示 `int_main...origin/int_main` 下存在大量非本任务脏改动和无关未跟踪文件；未获得用户授权前不能基线提交、暂存或推送这些无关改动。
