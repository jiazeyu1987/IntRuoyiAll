# PQC 活跃订单切换来源验证报告

## Result

PASS: PQC 检验员切换订单、工序、员工来源已实现并通过定向验证。与生产组长任务不冲突：生产模式继续使用设备账号工序和员工绑定链路，PQC 模式使用独立活跃订单链路。

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

## Scope Boundary

- 本任务不修改生产组长活跃订单/FIFO 分配实现；只消费“活跃订单是 PQC 选择订单唯一来源”的口径。
- 本任务不新增数据库 schema。
- 真实写入型 E2E 未执行；未启动本地服务、未登录、未创建测试数据。

## Closeout Blocker

当前分支 `int_main...origin/int_main [ahead 1]` 且工作区存在并行无关改动；为避免混入其它任务，本任务未提交/推送，任务状态保持 `ready_for_closeout`。
