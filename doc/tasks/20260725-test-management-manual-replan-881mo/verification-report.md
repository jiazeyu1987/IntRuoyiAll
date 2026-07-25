# Verification Report

## Objective

在本机 `int_main` 使用 `芋道源码/admin`，把排产工单手动重排样例加入 `系统管理 > 测试管理`，并通过真实前端 Playwright 验证来源生产工单号 `881MO093613`、`881MO093615` 的手动重排结果。

## Status

PASS

## Test Management Result

- 测试项名称：`排产工单手动重排 881MO093613/881MO093615`。
- 测试项 ID：`1`。
- 状态：`ENABLE`。
- 默认执行方式：`SEQUENTIAL`。
- 并行安全：`false`。
- 检查点数：`4`，分别覆盖重排成功、仅目标工单产品编号变橙色、最近一次成功排产时间更新、生产排产甘特图范围。

## E2E Result

- 登录前置：`芋道源码/admin` 通过官方 `scripts/preflight/login-preflight.mjs`，目标页签 `系统管理 > 测试管理` 可见。
- RED：`assert-existing` 模式首次失败，原因是测试管理中尚不存在目标测试项。
- GREEN：`full` 模式通过真实前端新增/更新测试项，并执行手动重排真实路径。
- 静态合同：`node IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js` 通过。

## Manual Replan Evidence

- 目标排产工单：`SCH-881MO093613-20260707-0001` / id `131`，`SCH-881MO093615-20260707-0001` / id `127`。
- 应用请求：`scheduleOrderIds=[131,127]`，`startTime=2026-07-26 00:00:00`。
- a. 重排成功：PASS，`applyResult.applied=true`，生成任务 `136` 个，保留任务 `7` 个。
- b. 仅目标两个工单产品编号变橙色：PASS，目标产品编号分别为 `YXN.069.001.1013` 和 `YXN.069.001.1002`，均为 `rgb(212, 107, 8)`，其他可见行保持未排产样式。
- c. 最近一次成功排产时间更新：PASS，接口 `operationType=REPLAN_APPLY`，页面显示 `2026-07-25 15:33:56`。
- d. 生产排产页签甘特图有且仅有两个目标工单：PASS，Gantt API/UI codes 均仅为 `881MO093613`、`881MO093615`。

## Artifacts

- `doc/tasks/20260725-test-management-manual-replan-881mo/artifacts/test-management-manual-replan-summary.json`。
- `doc/tasks/20260725-test-management-manual-replan-881mo/artifacts/manual-replan/repair-verification-report.json`。
- `doc/tasks/20260725-test-management-manual-replan-881mo/test-management-manual-replan-full.e2e.cjs`。

## Closeout State

Required verification is complete. Task status is `ready_for_closeout`; remaining work is task-owned cleanup, final document status update, task commit, and push.