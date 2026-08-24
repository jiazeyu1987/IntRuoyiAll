# Verification Report

## Verification Scope

本次只核验流程修复6五份开发文档是否符合用户修订意见和当前流程线程职责；不把代码审计当测试，不运行生产代码、数据库、服务或写入型 E2E。

## Artifacts

- `task.md`
- `development-plan.md`
- `test-plan.md`
- `execution-log.md`
- `verification-report.md`

## Structural Results

| 检查项 | 结果 | 证据 |
|---|---|---|
| 目标态、当前代码事实、根因和修改边界 | PASS | `task.md` |
| Tx-A 原子回填、Tx-B/Tx-C 顺序、失败/重试/UI语义 | PASS | `task.md`、`development-plan.md` |
| immutable receipt 与 provisioning 状态分离 | PASS | receipt 只含成功回填/损耗/来源 hash；流程6独占 `BatchProvisioningRecord` 的 `batchExecutionId`、`BATCH_*`、错误码和重试元数据 |
| receipt 失败状态模型无歧义 | PASS | Tx-A 失败回滚后只追加审计失败尝试并返回 `BACKFILL_ATOMIC_ROLLBACK`；不提交 receipt、不产生 `BACKFILL_FAILED`；成功才提交 `BACKFILL_SUCCEEDED` |
| 活跃订单与合法独立入口合同 | PASS | `task.md`、`development-plan.md`、`test-plan.md` |
| 正式领料字段 | PASS | 五份文档均使用 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId` |
| 零损耗口径 | PASS | `hasActualLoss=false`/`NO_LOSS`，无损耗单、无 loss evidence ID |
| 四份材料名称和流程8门禁 | PASS | 来料检报告、灭菌报告、成品检报告、成品检记录；状态统一为 `MATERIALS_PENDING/MATERIALS_READY/MATERIALS_RECHECK_REQUIRED` |
| 流程8对外错误码 | PASS | 仅使用冻结材料错误码集合；未定义的 `MATERIAL_GATE_NOT_HARD` 不作为流程8输出 |
| 流程1/4/5/7/8/9/10/11字段级合同 | PASS | `development-plan.md#Cross-thread Contracts` |
| blocker、迁移和回滚边界 | PASS | `task.md`、`development-plan.md`、`test-plan.md` |
| BDD、RED/GREEN/REGRESSION标记 | PASS | `test-plan.md`、`execution-log.md`；实现证据均 NOT RUN |

## Opinion Closure

1. 跨线程职责已改为：1只管正式领料绑定；7管批次完整映射和放行后追溯；8管四材料上传/硬门禁；9管多入口前置合同；10管最终放行状态/追溯事件；11管 BDD/TDD/迁移总门禁；4管完成与三类回填；5管条件损耗。邻接5、8、10已显式列出。
2. 已关闭本次复核发现的状态矛盾：Tx-A 三类回填成功才在同一事务提交 immutable `BACKFILL_SUCCEEDED` receipt；receipt 不含 `BATCH_*` 或 `batchExecutionId`。流程6独占可变 `BatchProvisioningRecord`；Tx-B 创建/复用批次后，流程7以独立 Tx-C 写 Origin/TraceLink/Manifest，流程6消费成功事件后才推进 `BATCH_READY`。失败仅更新 provisioning 记录，严格复用 receipt 重试。
3. 多入口已分类：活跃订单入口必须 receipt+流程1绑定；合法独立手工/排产入口使用自己的正式等价 receipt、来源关系和幂等键；全部汇聚统一建批服务和流程8/10门禁，不因无 active-order 关系一律拒绝。
4. 领料字段已替换为流程1冻结的五字段合同，不再以 `materialPickListId` 作为唯一语义。
5. 零损耗已固定为事实标记而非第三张单：有实际损耗才有 `lossRecordId`，无损耗只 `NO_LOSS`。
6. 四份材料已明确为来料检报告、灭菌报告、成品检报告、成品检记录，批次创建后上传，四份齐套后任何合法入口才可请求放行。
7. 五份文档均列出输入、输出、owner、幂等键、状态和失败码，且同时对接1、4、5、7、8、9、10、11；领料五字段和流程5 hasActualLoss/损耗快照已完整输出。
8. execution-log 和 test-plan 已把严格 TDD RED/GREEN/REGRESSION 标为后续实现计划/NOT RUN；`rg` 审计只标 AUDIT，文档结构 PASS 不等于代码 GREEN。

## Code Conformity Conclusion

当前代码不符合目标顺序：PQC approve 仍先 `openOrCreate` 再写 dossier；手工/排产入口也未统一消费完成回填 receipt。现有四节点报告初始化、申请级复用和旧批次迁移 blocker 可作为实现输入，但不能替代本任务冻结的 receipt、统一服务、流程8硬门禁和流程10最终状态。

结论：**代码不符合目标态；文档合同已修订为可实现的后续实现基线。**

## Not Run

- 生产代码修改：NOT RUN（用户明确禁止）。
- 数据库迁移/回滚演练：NOT RUN。
- 后端测试、构建、服务启动：NOT RUN。
- Playwright 写入型 E2E：NOT RUN。
- strict TDD RED/GREEN/REGRESSION：NOT RUN，等待流程修复11总门禁。

## Unresolved Blockers

1. 旧批次缺有效完成 receipt、流程1绑定或流程7映射时必须迁移或阻断。
2. 流程4/5/7/8/9/10/11 的实现、迁移和总门禁证据尚未完成。
3. 当前 `requireDossierWrite` 的无条件损耗 evidence 校验需后续实现按 `hasActualLoss` 修正。

## Final Status

in_progress（流程6局部实现、主线选择性融合和定向验证已通过；正式跨流程闭环、迁移和运行验证仍 blocked）。

## Coding Verification Update (2026-08-24)

- 37 个流程6定向测试通过；MES 24 模块 reactor compile 通过；`git diff --check` 通过。
- `e539e8a2c` 为流程6 task-owned 实现提交，`fa2593258` 为验证记录，`ecf8053f4` 已快进融合到 `int_main`。
- 主线定向 suite 37/37 PASS，24 模块 MES compile PASS，`git diff --check` PASS，HEAD containment PASS。
- 仍未完成：流程4 Tx-A receipt producer、流程7 Tx-C 真实事件闭环、流程9正式签名凭证消费的端到端运行证据、迁移 dry-run/apply/rollback、材料/最终放行运行验证。

## Independent Receipt Verification Fix (2026-08-24)

| Gate | Result | Evidence |
|---|---|---|
| Caller payload is not trusted | PASS | Flow 6 reloads by `sourceCredentialId` through Flow 9 before local contract validation; regression asserts the verified object identity reaches Tx-B |
| RED | PASS | Pre-fix targeted test compile failed because the verification constructor/seam was absent |
| Isolated GREEN | PASS | 39 targeted tests, 0 failures/errors; MES 24-module reactor compile exit 0 |
| Mainline GREEN | PASS | `int_main` same 39-test suite and 24-module MES compile both passed |
| Commit/containment | PASS | `90455bdba`, two task-owned Java files; `git merge --ff-only` to `int_main` |
| Diff/runtime guard | PASS | `git diff --check` and branch-runtime-port-guard passed |

This is a Flow 6 unit/contract integration slice, not full production-chain completion. Migration, service runtime, write E2E, and cross-thread Flow 4/7/8/10 runtime gates remain NOT RUN.

## 主流程冻结合同核验（2026-08-22）

| 合同 | 结果 | 说明 |
|---|---|---|
| Tx-A 仅本地事务、外部预校验快照 | PASS | `SOURCE_SNAPSHOT_MISMATCH` fail fast，不远程重查 |
| receipt/provisioning 状态 owner | PASS | 流程4只写 immutable receipt；流程6只写独立 `BatchProvisioningRecord` 的 `BATCH_*`；流程7只写 Tx-C 映射 |
| Tx-C 映射执行顺序 | PASS | Tx-B 先有 batchExecutionId，流程7随后建 Origin/TraceLink/Manifest，成功事件后流程6才 BATCH_READY，之后才能进入材料阶段 |
| Tx-C 映射失败稳定错误码 | PASS | 流程7映射缺失/失败对外统一返回 `TRACE_MAPPING_BLOCKED`；内部原因只能作为细分，不替代稳定码 |
| Tx-A 失败 receipt 语义 | PASS | 已移除 `BACKFILL_PENDING`/`BACKFILL_FAILED` 持久化状态；失败返回 `BACKFILL_ATOMIC_ROLLBACK` 并只保留失败尝试审计，成功才产生 `BACKFILL_SUCCEEDED` |
| 独立凭证与 PQC 分流 | PASS | 后端签发、服务端有效期、entryType 分流 |
| 建批重试白名单 | PASS | 五类临时错误可重试，其余 blocked |
| 四材料有效定义 | PASS | COMPLETED、持久化、元数据/SHA-256、当前版本/hash；门禁状态为 MATERIALS_*，批准字段按存在性校验 |
| 历史迁移与独立追溯 | PASS | dry-run 分类；不适用关系使用 `NOT_APPLICABLE` 原因码 |

上述合同 PASS 为设计边界核验；代码定向测试和主线编译已有 GREEN 证据，但不替代流程4/7/9真实 producer、数据库迁移、材料和放行运行证据。若发现任一旧关键词（receipt 持有 BATCH_*、流程6写流程7映射、READY 材料状态、PQC_RELEASE 主键、旧流程7映射错误码）残留，应重新阻断而非宣称合同一致。
