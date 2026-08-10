# Verification Report

## Bug

一线 PQC 工序选择旧行为只展示有待检 PQC 任务的单个工序，不能展示活跃订单所属冻结工艺路线的整条路线。

## Expected

一线 PQC `active-order/processes` 必须返回该活跃订单冻结路线版本中的全部工序；只有存在正式 `PENDING` PQC 任务的工序附带 `pqcTaskId`、规程快照和检验项，其它冻结工序只作为不可提交的工序候选展示。

## Reproduction

新增回归测试 `MesFrontlinePqcContextServiceTest#shouldDisplayFullFrozenRouteAndAttachPqcTaskOnlyToPendingProcess`：构造冻结路线包含两道工序，但只有第一道工序存在 `PENDING` PQC 任务；旧实现只返回第一道工序。

## Root Cause

原链路把“工序展示集合”和“PQC 待检任务上下文”耦合到有待检任务的工序上，导致无 `PENDING` 任务的冻结路线工序被跳过。正式修复后，展示集合由活跃订单 `routeVersionId` 对应的发布快照 `routeSnapshotJson.configSnapshots.flowGraph.nodes` 决定，PQC 任务只作为附加上下文。

## RED/GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayFullFrozenRouteAndAttachPqcTaskOnlyToPendingProcess" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现返回 `[4001]`，期望 `[4001, 4002]`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `node tests\e2e\mes-frontline-pqc-process-picker-production-layout-static.spec.cjs` -> PASS。

## Verification

- 后端回归覆盖冻结路线全量展示、无待检工序不查询规程、不带检验项，以及任务上下文仅附着到待检工序。
- 前端静态合同覆盖 PQC 工序选择弹层仍使用正式候选数组，并在初始选择时优先落到首个可填写任务工序。
- `git diff --check -- <本任务涉及文件>` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-frontline-pqc-full-route-process-picker\verification-report.md` -> PASS，`Bug regression evidence is valid.`

## Blockers

无当前阻塞；工作区存在大量非本任务改动，本任务未执行暂存、提交或推送。
