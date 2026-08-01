# PQC 活跃订单切换来源验证报告

## Result

PASS for targeted implementation and verification: PQC 检验员选择来源、正式提交落库、PQC 组长列表读取同一份提交明细的代码链路已补通。PQC 提交现在先校验模板 payload，再调用专用正式提交接口写入工序池 PQC 事件；rawPayload 保留 pqcDraft 与 pqcPieceValues，供组长页按 originalPayloadJson 展示。

真实写入型 Playwright E2E 未运行；本轮验证覆盖前端静态契约、TypeScript、后端 PQC service JUnit 和 PQC 组长/修订/日志相邻回归。

与生产组长任务不冲突：生产模式继续使用设备账号工序和员工绑定链路，PQC 模式使用独立活跃订单链路与专用 PQC 提交端点。

## Implemented

- Backend PQC endpoints: active orders, active-order route processes, PQC personnel, PQC employee switch.
- Frontend PQC panel: order picker, route-process picker, PQC personnel picker, PQC-specific employee switch, formal `PQC_RESULT` payload mapping.
- Existing group-leader review/correction/log contracts retained and regression-tested through review-copy, event-revision, and submission-review tests.

## Verification Commands

- PASS: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js`.
- PASS: `pnpm ts:check`.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 5 tests.
- PASS: `node tests\e2e\mes-process-pool-team-leader-static.spec.js`.
- PASS: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js`.
- PASS: `node tests\e2e\process-pool-event-revision-api-static.spec.js`.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 10 tests.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 26 tests.

## 2026-08-01 Refresh

- PASS: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js`.
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260801-pqc-active-order-switching\backend-api-evidence.md`.
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260801-pqc-active-order-switching\frontend-feature-evidence.md`.
- PASS: `pnpm ts:check`.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 5 tests.
- PASS: `node tests\e2e\mes-process-pool-team-leader-static.spec.js`.
- PASS: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js`.
- PASS: `node tests\e2e\process-pool-event-revision-api-static.spec.js`.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolPqcEventTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 38 tests.

## 2026-08-01 Full Chain Audit RED (Before Optimization)

- FAIL: `node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> expected RED. Failure reason: `FrontlineFixedTemplatePanel.vue` `handleValidate()` 只调用 `FrontlineTemplateApi.validatePayload(...)` 后提示 `已提交`，未调用 `/mes/pro/feedback/frontline/submit` 或正式 PQC 提交接口。
- PASS: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js`.
- PASS: `node tests\e2e\mes-process-pool-team-leader-static.spec.js`.
- PASS: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js`.
- PASS: `node tests\e2e\process-pool-event-revision-api-static.spec.js`.
- PASS: `pnpm ts:check`.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesProcessPoolPqcEventTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 37 tests.

## Chain Assessment Before Optimization

- PASS: PQC 检验员切换订单来源 = 当前活跃订单。
- PASS: PQC 检验员切换工序来源 = 所选活跃订单产品对应工艺路线工序。
- PASS: PQC 检验员切换员工来源 = PQC 员工 + PQC 组长。
- PASS: PQC 组长列表、详情、判定正确/不正确、修正不正确内容、提交日志和修改日志入口存在，并通过相邻回归。
- RESOLVED: 优化前 PQC 检验员点击提交没有正式落库；已在 Submit Chain GREEN 中通过专用 PQC 提交端点修复。
- RESOLVED: 未复用缺上下文的生产提交 API；改为 PQC 专用提交端点，并从当前活跃工序池最新事件继承正式设备、报工和记录本来源。

## Scope Boundary

- 本任务不修改生产组长活跃订单/FIFO 分配实现；只消费“活跃订单是 PQC 选择订单唯一来源”的口径。
- 本任务不新增数据库 schema。
- 真实写入型 E2E 未执行；未启动本地服务、未登录、未创建测试数据。

## Historical Closeout Note

优化前因全链路审计发现 PQC 提交落库断点，任务曾标记为 `blocked`；本轮已修复并改为 `ready_for_closeout`。当前仍未提交/推送，原因是工作区存在无关并行改动。

## Product Blocker Resolution

PQC 提交落库断点已解决：提交按钮已从 validate-only 改为模板校验后正式提交，`mes-frontline-pqc-submit-to-leader-chain-static.spec.js` 已 GREEN。剩余缺口是未执行真实写入型 Playwright E2E。

## 2026-08-01 Submit Chain GREEN

- PASS: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> 6 tests.
- PASS: node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js.
- PASS: node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js.
- PASS: pnpm ts:check.
- PASS: node tests\e2e\mes-process-pool-team-leader-static.spec.js.
- PASS: node tests\e2e\process-pool-review-copy-and-revision-static.spec.js.
- PASS: node tests\e2e\process-pool-event-revision-api-static.spec.js.
- PASS: mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcEventTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> 32 tests.

## Updated Chain Assessment

- PASS: PQC 检验员切换订单来源 = 当前活跃订单。
- PASS: PQC 检验员切换工序来源 = 所选活跃订单产品对应工艺路线工序。
- PASS: PQC 检验员切换员工来源 = PQC 员工 + PQC 组长。
- PASS: PQC 检验员提交 = 模板校验后调用 POST /mes/pro/feedback/frontline/device-account/pqc/submit 正式落库。
- PASS: PQC 提交日志 = 工序池 PQC_INSPECTION event + mes_pro_process_pool_pqc_record。
- PASS: PQC 组长列表/详情继续按 originalPayloadJson 解析 pqcDraft/pqcPieceValues，可判定、修正并记录修订日志。
- NOT RUN: 真实写入型 Playwright E2E；本轮未启动本地服务、未登录、未创建写入型测试数据。

## Closeout Boundary

当前实现和定向验证已完成；由于工作区有无关并行改动，本轮未执行提交/推送。
