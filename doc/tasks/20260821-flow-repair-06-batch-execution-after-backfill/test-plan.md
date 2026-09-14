# Test Plan

## Validation Scope

验证统一回填 receipt、后继建批、合法多入口、四材料硬门禁、最终放行和追溯；本次只设计测试，不运行生产测试、服务或写入型 E2E。

## BDD Scenarios

BDD: T1 活跃订单完成原子回填 -> Given 活跃订单双进度均为100%、流程1正式领料绑定和提交/复核事件有效，When 用户点击完成，Then 同一事务写批记录、过程检验单及按需损耗单，并提交唯一只含成功事实的 immutable `BACKFILL_SUCCEEDED` receipt；不得包含 `BATCH_*` 或 `batchExecutionId`。

BDD: T2 回填失败全回滚且不落 receipt -> Given 三类回填任一校验或写入失败，When 完成命令执行，Then 批记录、过程检验单和损耗单（若有）均不提交，不提交 `completionBackfillReceipt`、不产生 `BACKFILL_FAILED`，仅在 Tx-A 回滚后追加失败尝试/错误码并返回 `BACKFILL_ATOMIC_ROLLBACK`，用户可重新点击完成。

BDD: T3 零损耗 -> Given 实际损耗为零，When 完成回填，Then receipt 记录订单级 `hasActualLoss=false`、`lossQuantity=0`、`lossDecision=NO_LOSS/NOT_REQUIRED` 和零损耗确认快照，不生成损耗单，不要求损耗 evidence ID；缺少 lossRecordId 不能反推零损耗。

BDD: T4 建批和映射失败后重试 -> Given Tx-A 已提交 immutable receipt，When 流程6 Tx-B 创建/复用批次或流程7 Tx-C 映射失败，Then 只更新独立 `BatchProvisioningRecord` 的 `BATCH_PROVISIONING_RETRYABLE`/`BATCH_PROVISIONING_BLOCKED` 及错误码；映射失败对外统一返回 `TRACE_MAPPING_BLOCKED`，receipt 保持不变；流程6消费 Tx-C 成功事件后才推进 `BATCH_READY`，完成按钮不重复回填，重试使用同 receipt 幂等。

BDD: T5 活跃订单入口合同 -> Given 入口声明 active-order 且携带 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId`，When 缺 receipt、绑定版本或快照不匹配，Then 返回 `BACKFILL_RECEIPT_REQUIRED`/`SOURCE_SNAPSHOT_MISMATCH`，不得建批。

BDD: T6 合法独立入口 -> Given 手工或排产入口拥有自己的正式独立前置 receipt、工单/批号/路线版本和来源快照，When 调用统一建批服务，Then 不要求 active-order 关系但建立来源追溯并允许创建/复用批次。

BDD: T7 四份材料硬门禁 -> Given 批次执行已创建且流程7 Tx-C 映射完成，When 来料检报告、灭菌报告、成品检报告、成品检记录任一缺失、未批准或快照不匹配，Then 流程8返回 `MATERIALS_PENDING` 或 `MATERIALS_RECHECK_REQUIRED`，任一入口放行均被拒绝；只有四份当前有效材料齐套时返回 `MATERIALS_READY`。

BDD: T7 材料错误码合同 -> Given 四材料门禁校验失败或入口尝试绕过门禁，When 流程8返回错误，Then 对外只使用冻结码 `RELEASE_MATERIAL_GATE_REQUIRED`、`MATERIAL_NODE_MISSING`、`MATERIAL_UPLOAD_INCOMPLETE`、`MATERIAL_FILE_NOT_VERIFIED`、`MATERIAL_VERSION_STALE`、`MATERIAL_HASH_MISMATCH`、`MATERIAL_VERSION_CONFLICT`、`MATERIAL_MANIFEST_CHANGED`、`MATERIAL_SOURCE_SNAPSHOT_CHANGED`、`RELEASE_ENTRY_GATE_BYPASS`、`IDEMPOTENCY_CONFLICT`，不得输出 `MATERIAL_GATE_NOT_HARD`。

BDD: T8 放行后追溯 -> Given 流程6 已有 `BATCH_READY`、四份材料齐套且流程10放行，When 审计用户查询，Then 流程7可反查批次、活跃订单（如适用）、工单、正式领料字段、一线生产、一线PQC、复核、三类回填及材料来源，并追加 RELEASE_DECISION 追溯。

BDD: T9 历史批次阻断 -> Given 旧批次无有效完成 receipt、流程1绑定或流程7映射，When 任一入口尝试放行，Then 返回 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`，不得静默补链。

## Strict TDD Sequence

### RED (future implementation, NOT RUN)

先为 T1-T9 编写服务、持久化和契约测试，预期当前代码因 PQC approve 先建批、无 receipt 和入口不统一而失败。下面只是后续实现计划，不是本次执行证据：

`mvn -pl <backend-module> -Dtest=CompletionBackfillReceiptTest test` -> 预期 FAIL：receipt 和 Tx-A 原子阶段尚不存在。

`mvn -pl <backend-module> -Dtest=UnifiedBatchProvisionerContractTest test` -> 预期 FAIL：入口适配器、同 receipt 重试和独立入口合同尚不存在。

`mvn -pl <backend-module> -Dtest=FourMaterialReleaseGateTest test` -> 预期 FAIL：流程8硬门禁尚未覆盖所有入口。

`mvn -pl <backend-module> -Dtest=TraceabilityMappingTest test` -> 预期 FAIL：流程7完整映射/放行后追溯尚未闭环。

### GREEN (future implementation, NOT RUN)

实现最小 Tx-A/Tx-B 状态机、统一建批服务、入口合同和流程8/10/7适配后，逐项运行上述测试，只有全部 PASS 才可记录 GREEN。设计审阅或 `rg` 发现缺陷绝不算 GREEN。

### REGRESSION (future implementation, NOT RUN)

回归活跃订单加入和正式领料绑定、生产/PQC提交及组长复核、批记录/过程检验/条件损耗、手工与排产合法独立入口、PQC申请、四份材料上传、最终放行、放行后追溯、并发幂等、旧批次迁移阻断。

## Real Browser E2E Matrix (future, NOT RUN)

| 场景 | 真实用户路径 | API 最终核验 |
|---|---|---|
| E1 | 活跃订单加入 -> 绑定领料 -> 双100% -> 完成 -> Tx-C映射 | immutable receipt、BatchProvisioningRecord、三类回填、BATCH_READY |
| E2 | Tx-B/Tx-C 模拟失败 -> 页面重试 | 同 receipt、无重复回填、独立 provisioning 记录、最终批次唯一 |
| E3 | 手工合法独立批次创建 | 独立 receipt、来源关系和批次唯一 |
| E4 | BATCH_READY 后逐项上传四份材料 -> MATERIALS_READY -> 放行 | 四节点当前版本、流程10 RELEASED、流程7 trace |

缺少真实入口、测试数据或服务时必须记录 blocker，不使用 mock 成功。

## Failure and Blocker Rules

- 测试 RED/GREEN 未运行是本次预期状态，不是失败伪装。
- 缺正式来源、绑定、receipt、材料或迁移证据即阻断对应测试。
- 任何入口绕过流程8四材料门禁、把零损耗当损耗单、或将 receipt 误标 `BATCH_READY` 均为 P0 blocker。
- 流程8对外错误码必须来自冻结集合；`MATERIAL_GATE_NOT_HARD` 只能作为流程6内部实现阻断描述，不得作为流程8输出。

## Current Status

in_progress（BDD/TDD/回归计划已冻结；流程6定向实现测试和主线复验已通过，跨流程和迁移测试仍待真实环境）。

## Coding Verification Update (2026-08-24)

GREEN: Flow 6 targeted Maven suite -> 37 tests passed.

GREEN: MES reactor compile (`-pl yudao-module-mes -am -DskipTests`) -> exit code 0.

NOT RUN: migration dry-run/apply/rollback, service/E2E, and cross-thread Flow 4/7/9 integration.

## Implemented TDD Slice (2026-08-24)

BDD: Flow 9 receipt reload -> Given an independent entry may contain a forged full receipt object, When Flow 6 provisions it, Then only the Flow 9 service result loaded by receipt id, entry type, source snapshot and security tenant reaches validation and Tx-B.

RED: `MesProductionReleaseBatchExecutionPortTest` initially failed at test compile because the production port had no Flow 9 service constructor/verification seam.

GREEN: Flow 6 targeted Maven suite -> PASS, 39 tests, 0 failures, 0 errors; MES 24-module reactor compile -> PASS.

REGRESSION: Main `int_main` targeted suite -> PASS, 39 tests, 0 failures, 0 errors; main MES compile -> PASS; `git diff --check` and branch-runtime guard -> PASS.

NOT RUN: database migration, service runtime, write E2E, and full Flow 4/7/8/10 cross-thread runtime gates.

## 主流程冻结合同测试增补（后续实现，NOT RUN）

BDD: Tx-A 外部调用阻断 -> Given 外部快照在事务前已生成，When 本地版本/hash 不一致，Then fail fast 返回 `SOURCE_SNAPSHOT_MISMATCH`，不远程重查、不写任何回填。

BDD: 状态 owner 隔离 -> Given 流程4已提交 immutable receipt，When 流程6建批失败或重试，Then 只改变 `BatchProvisioningRecord` 的 `BATCH_*` 状态，receipt payload 三类结果不变；流程4和流程7都不能写流程6的 provisioning 状态。

BDD: 映射阻断稳定错误码 -> Given Tx-B 已创建 batchExecutionId，When 流程7 Tx-C 缺少或无法验证来源映射，Then 对外返回稳定码 `TRACE_MAPPING_BLOCKED`，流程6保持 provisioning pending/blocked，不得写 `BATCH_READY` 或开放材料上传。

BDD: Tx-A 失败重发 -> Given 上一次完成请求返回 `BACKFILL_ATOMIC_ROLLBACK` 且不存在 receipt，When 用户以同一业务版本重新点击完成，Then 页面保持 `COMPLETION_NOT_SUBMITTED` 且 Tx-A 重新校验并最多产生一个新的成功 `BACKFILL_SUCCEEDED` receipt，不得把失败尝试升级为 receipt。

BDD: 重试白名单 -> Given 建批返回白名单临时错误或永久错误，When 编排器处理，Then 前者进入 retryable，后者进入 blocked；`BATCH_PROVISIONING_OUTCOME_UNKNOWN` 先查询最终结果。

BDD: 独立凭证有效期 -> Given 凭证未签发、已过期、已撤销或 hash 不一致，When 独立入口建批，Then 返回 `INDEPENDENT_RECEIPT_INVALID/EXPIRED/REVOKED`，不得创建批次。

BDD: 材料当前版本 -> Given 四节点任一替换或来源 hash 变化，When 预检放行，Then 旧 manifest 失效并返回 `MATERIALS_RECHECK_REQUIRED`，不能复用旧申请。

BDD: 独立追溯不适用关系 -> Given 独立批次没有 activeOrderId，When 查询追溯，Then 返回 `relationStatus=NOT_APPLICABLE` 和原因码，不返回空字符串或伪造订单关系。
