# P1 独立测试审查报告（2026-08-22 历史基线）

> 本报告记录 2026-08-22 在旧主线快照上的独立缺口审查，不代表 2026-08-25 Flow4 实现状态。当前实现和测试证据见 `execution-log.md`、`verification-report.md` 及本报告末尾的复核补记。

## 审查范围

- 审查日期：2026-08-22。
- 范围：流程修复 4 的 P1 目标（来源 hash 重放、双 100% 完成、Tx-A 失败无回执）。
- 方式：当前 `int_main` dirty worktree 的只读源码/测试清单审查，以及不产生业务数据的静态命令。
- 约束：未修改生产代码、数据库、`task-state` 或其它任务文档；未启动服务、未执行写入型 E2E。当前 worktree 的既有并发修改保留。

## 命令证据

| 命令 | 结果 | 证据/解释 |
| --- | --- | --- |
| `rg -n -F -e 'completionBackfillReceipt' -e 'BACKFILL_ATOMIC_ROLLBACK' -e 'BACKFILL_SUCCEEDED' -e 'ACTIVE_ORDER_PROGRESS_NOT_COMPLETE' IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java` | **PASS（缺口扫描）**，exit 1、无匹配 | 当前 MES 主/测试源码没有 P1 目标回执、Tx-A 失败状态或完成命令合同符号。此结果证明目标实现/测试尚不存在，不是目标行为通过。 |
| `rg -n -F -e '@PostMapping("/active-order/release/apply")' -e '@PostMapping("/active-order/complete")' -e 'PQC_RELEASE_PENDING' -e 'completeAndBackfill' -e 'BACKFILL_STATUS_SUCCESS' .../controller/.../team .../service/.../team` | **PASS（现状确认）** | 现有正式入口为 `MesProcessPoolTeamLeaderController.java:396` 的 `/active-order/release/apply`；生成服务写入 `PQC_RELEASE_PENDING`（`MesTeamLeaderActiveOrderReleaseGenerationService.java:415`），并按工序 `BACKFILL_STATUS_SUCCESS` 检查。未发现 `/active-order/complete`。 |
| `rg -n -F -e 'hashIsStableAcrossAuthoritativeListOrder' -e 'productionOrInspectionEvidenceChangeChangesHash' -e 'productionBelowOneHundredPercentBlocksBeforeAnyWrite' -e 'sameRequestWithChangedAuthoritativeSnapshotReturnsPayloadConflict' .../src/test/java` | **PASS（既有测试清单）** | 既有 hasher 测试覆盖正式列表排序稳定和生产/PQC 证据变化敏感性（`MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest.java:24,40`）；旧放行申请测试覆盖生产进度不足零写入（`MesProductionReleaseApplySp1Test.java:183`）及同键来源 hash 变化冲突（`:247`）。这些是申请级/hasher 证据，不是完成级不可变回执重放。 |
| `rg -n -F 'verify(backfillService, never())' .../MesTeamLeaderOrderProcessCompletionServiceTest.java` | **PASS（边界确认）** | 现有断言（例如 `:95`、`:119`、`:147`）只证明工序完成在若干无损耗路径不调用旧的逐工序回填；未证明订单完成时三类 writer 的同事务原子性。 |
| `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java` | **PASS** | 未报告空白或补丁格式错误。 |
| `mvn -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest,MesProductionReleaseApplySp1Test -Dsurefire.failIfNoSpecifiedTests=false test` | **BLOCKED / NOT_RUN** | Windows 环境找不到 `mvn`；后端目录也没有 `mvnw`。因此没有可归因于当前源码的编译、单元测试或回滚数据库证据。 |

## P1 验收矩阵

| 目标 | 独立结论 | 依据 |
| --- | --- | --- |
| 来源 hash 重放 | **PARTIAL；P1 不通过** | hash 工具已有排序稳定/来源变化测试，但没有 `completionBackfillReceipt` 的规范请求 hash、同 hash 返回同一不可变回执、不同 hash 冲突及版本锁测试。申请级 `PQC_RELEASE_PENDING` 幂等不能替代完成级回执。 |
| 权威双 100% | **FAIL / 未实现** | 只有旧申请流程的生产低于 100% 阻断测试；没有 PQC 低于 100% 对称测试、订单级完成命令、完成前禁止三类资料/建批的实现级断言。当前入口仍是放行申请。 |
| Tx-A 任一回填失败无回执 | **FAIL / 未实现** | 目标状态和回执符号均不存在；没有三类 writer 失败后的整体回滚测试，也没有 `BACKFILL_ATOMIC_ROLLBACK`/无 `completionBackfillReceipt` 断言。现有工序回填和申请事务边界不能证明订单级 Tx-A 原子性。 |

## Blocker 与风险

1. Maven/Maven Wrapper 缺失，P1 测试无法执行；需先提供可审计的 Java/Maven 工具链和精确命令。
2. P1 所需完成命令、权威双 100% 读取、三类统一 writer、不可变回执、Tx-A/Tx-B 边界及对应测试尚未进入当前源码；不能把旧申请级测试或静态扫描当作 GREEN。
3. 未准备任务自有正式订单、工单/领料、生产/PQC 签名复核、损耗分支和隔离失败注入数据；按正式来源门禁，不能用 mock、SQL 或历史批次补充此缺口。
4. 当前 worktree 存在大量并发 dirty 修改。本报告只对读取到的当前快照负责；实施阶段应在可复现提交/隔离 worktree 上重新运行 RED/GREEN/REGRESSION。

## 独立结论

**P1 = NO-GO（实现与测试证据不足，且测试执行被工具链阻塞）。** 现有测试可作为 hash 基础能力和旧申请流程回归基线，但不能证明流程修复 4 的双 100% 完成、Tx-A 原子三类回填、失败无回执或完成级幂等已经满足目标态。

## 2026-08-25 implementation recheck

- Flow4 已新增订单完成 Tx-A、正式三类回填结果 ID、不可变 `BACKFILL_SUCCEEDED` receipt 和 Flow6 tenant-scoped read port；不含 `batchExecutionId`/`BATCH_*`。
- `MesTeamLeaderActiveOrderCompletionServiceTest`、`MesTeamLeaderActiveOrderCompletionBackfillPortImplTest`、`MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest`、`MesProcessPoolTeamLeaderSchemaTest` 定向套件为 `37/37 PASS`；MES compile 为 `BUILD SUCCESS`。
- 数据库 migration apply/rollback、真实租户事务回滚和 Playwright E2E 仍 `NOT_RUN`；完整 reactor 在无关 `yudao-server` MDEP-98 阻断。
