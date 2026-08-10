# Verification Report

## Result

- 生产实现、前端空/0 数量处理和主要回归测试已位于 `origin/int_main`，集成提交为 `61ba20294` 及其后续 checkpoint。
- 本轮仅新增合法测试夹具：历史工序快照不完整场景补齐强制非空的 ERP 固定数量快照。
- 后端隔离回归、前端静态回归和只读真实页面 E2E 通过。
- 写入型“减少已有分配数量”E2E 因缺少安全测试租户和任务自有数据而阻塞。

## Passed

- `git merge-base --is-ancestor 61ba20294 origin/int_main` -> PASS。
- 隔离 javac 编译三个目标测试 -> PASS。
- JUnit Platform Console -> PASS，37 tests / 0 failures / 0 errors。
- `shouldCalculateProductionProgressFromFormalRouteWhenActiveOrderSnapshotIsIncomplete` -> PASS，10 道工序中 1 道满额为 10%。
- `shouldRecalculateProductionProgressFromCurrentAllocationAfterQuantityReduction` -> PASS，减少到未满后进度回退。
- `node tests/e2e/team-leader-allocation-zero-quantity-static.spec.cjs` -> PASS。
- 真实只读 Playwright -> PASS，8 条活跃订单生产进度列与接口一致；截图位于任务临时输出目录，收尾时可清理。

## Standard Maven Blocker

标准 Maven 目标命令在测试源码编译阶段被非本任务测试阻塞：

`MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest` 调用当前 `MesQaInspectionRegulationMapper` 不存在的 `selectPublishedListByStableProcess(...)`。

该失败发生在 Surefire 前，与本次三个目标测试无关；本任务没有修改或绕过该并行放行流程代码。

## E2E Blocker

默认本机身份为 `芋道源码/admin`，只能用于本轮只读核对。项目规则要求写入型 E2E 使用确认的测试租户、账号和可追踪可清理的任务数据；当前缺少这些前置，因此没有执行分配减少写入，也没有使用 API-only、SQL 或 mock 替代。

## Integration

- 当前工作区：`E:\IntRuoyi`
- 当前分支：`int_main`
- `origin/int_main` 已包含生产修复。
- 本轮待提交范围仅为合法测试夹具和本任务核心记录，不包含其它并行工作区改动。
