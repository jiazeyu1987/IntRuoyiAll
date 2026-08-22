# 测试计划：BDD、严格 TDD、回归与迁移

## 1. 范围和证据规则

本文件是后续实现计划；本线程不运行服务、数据库、Maven、Node 或写入型 Playwright E2E。流程1-10 的 RED/GREEN/REGRESSION 命令均明确标记“计划，未运行”；M12 纯函数合同测试另有实际 runner 证据，不能把其它命令中的 PASS 文本当作实际通过。

写入型验证必须使用真实租户、真实角色/签名、正式工单和领料单、已确认 PQC 汇总、可追溯四份附件和任务自有数据。禁止 mock、API-only、直接 SQL、固定 ID、默认成功和按文件数量判断齐套。

## 2. BDD 场景

- BDD: 活跃订单完成统一回填再建批 -> Given 流程 1 正式领料绑定、流程 2/3 签名和复核事实齐全且生产/检验均 100% / When 生产组长点击完成 / Then 流程 4 Tx-A 同一节点回填批记录、过程检验和适用损耗并提交成功 receipt，流程 6 进入 BATCH_PROVISIONING，流程 7 Tx-C 映射成功后才进入 BATCH_READY。
- BDD: Tx-A 失败原子回滚 -> Given 三类回填任一校验或本地写入失败 / When 完成命令执行 / Then 返回 BACKFILL_ATOMIC_ROLLBACK，批记录、过程检验和损耗均不提交，不提交 completionBackfillReceipt，不产生 BACKFILL_FAILED receipt；仅记录失败尝试，用户可重新点击完成。
- BDD: 完成前禁止物化 -> Given 任一进度不足或正式来源缺失 / When 点击完成 / Then 返回 blocker，不写三类回填、不建批次、不生成放行申请。
- BDD: 无损耗不建单 -> Given 流程 5 对每个工序保存正式零损耗快照，逐工序 decision=NO_LOSS，order/process 级 hasActualLoss=false、lossQuantity=0 / When 完成 / Then completion receipt 的 lossReportStatus=NOT_REQUIRED、lossRecordId 为空且不生成损耗单，流程 6 仍可在其它适用回填成功后创建批次；缺失 lossRecordId 不能单独推断无损耗。
- BDD: 有损耗必须建单 -> Given 任一工序 decision=REQUIRED 且 lossQuantity、原因、来源映射完整 / When 完成 / Then 仅该工序写正式损耗单，order/process 级 hasActualLoss=true、lossReportStatus=SUCCESS，receipt 携带 lossRecordId；任一必填事实缺失则整体 BLOCKED 并回滚。
- BDD: 损耗事实缺失阻断建批 -> Given 任一工序为 BLOCKED 或损耗状态无法形成正式 NO_LOSS/REQUIRED 快照 / When 完成或调用流程 6 / Then 返回稳定 blocker，不提交成功 receipt，不创建或复用批次。
- BDD: 活跃订单创建入口 -> Given 流程 4 BACKFILL_SUCCEEDED receipt、流程 1 领料绑定、双 100% 和三类回填成功 / When 活跃订单入口调用流程 6 / Then 直接消费成功 receipt，进入 BATCH_PROVISIONING，随后必须经流程 7 Tx-C 映射后才可 BATCH_READY。
- BDD: 合法独立创建入口 -> Given 后端签发的 IndependentBatchPrerequisiteReceipt、正式 batchExecutionSourceRelation、来源对象和独立权限，且不属于活跃订单链 / When 流程 9 校验证据后调用流程 6 / Then 创建独立批次，不要求 activeOrderId，不伪造活跃订单关系，并等待流程 7 Tx-C 映射。
- BDD: 凭证场景不可混用 -> Given 活跃订单 entryType 携带独立凭证或反之 / When provision / Then 返回 ENTRY_SCENARIO_MISMATCH 或 SOURCE_CONTEXT_CONFLICT。
- BDD: 多入口幂等 -> Given 不同合法入口使用完全一致的 source relation、hash、版本和复用规则 / When 重试 / Then 返回同一 batch ID；载荷不同返回冲突，不重复创建。
- BDD: 放行前来源映射 -> Given 流程 6 已创建或复用批次并处于 BATCH_PROVISIONING / When 流程 7 Tx-C 校验 Origin/TraceLink、工单、领料和三类回填映射 / Then 映射成功才推进 BATCH_READY；失败返回 TRACE_MAPPING_BLOCKED，流程 8/10 均阻断。
- BDD: 四材料齐套 -> Given 批次已 BATCH_READY 且流程 7 Tx-C 映射完成 / When 来料检报告、灭菌报告、成品检报告、成品检记录四节点均为当前有效 COMPLETED（有批准字段时 APPROVED），且每个节点持久化 version/file_hash/source_snapshot_hash 并与 manifest 一致 / Then 流程 8 返回 MATERIALS_READY。
- BDD: 四材料缺一阻塞 -> Given 四节点缺任一或任一过期、hash 不一致、类型未知 / When 任一放行入口申请 / Then 返回材料 blocker，不进入 RELEASED。
- BDD: 成品检两项不可互代 -> Given 成品检报告完成但成品检记录缺失，或反之 / When 检查流程 8 gate / Then 仍阻塞，不能合并或推断。
- BDD: 多放行入口共用硬门禁 -> Given 批次详情、PQC/生产申请、管理者代表批准和独立批次放行入口 / When 进入放行 / Then 所有入口都消费流程 8 同一 manifest gate，只有流程 10 写最终 RELEASED。
- BDD: 合法独立放行不因无订单拒绝 -> Given 独立批次 source relation、流程 7 Tx-C 映射完成、BATCH_READY、四材料完整、独立权限和签名有效但没有 activeOrderId / When 管理者代表批准 / Then 流程 10 可写 RELEASED，追溯从独立 source relation 展开。
- BDD: 并发最终化唯一胜者 -> Given 两个入口使用同一 release transaction 和 expectedVersion / When 并发批准 / Then 只有一个 CAS 成功，另一请求返回重复或版本冲突并读取同一 decision。
- BDD: 放行后完整追溯 -> Given 流程 10 RELEASED / When 从订单、工单、领料、批次、申请或放行决定查询 / Then 返回生产/PQC/复核、损耗、三类回填、四材料 hash、来源关系、操作者、时间和最终决定。
- BDD: 历史关系不明阻断 -> Given 历史批次缺 completion receipt、IndependentBatchPrerequisiteReceipt、batchExecutionSourceRelation、流程 7 映射、四材料 manifest 或节点 version/file_hash/source_snapshot_hash / When 重试创建或放行 / Then 返回 BLOCKED_LEGACY 或迁移 blocker，不自动认领、复用、删除或补默认资料。

## 3.1 M12 迁移分类器合同测试

- BDD: 五类历史分类 -> Given 规范化历史记录，When `classify_legacy_batch` 运行，Then 只返回冻结五类之一，旧三材料固定为 `BLOCKED_LEGACY`，已放行且来源不完整固定为 `ALREADY_RELEASED_REVIEW_REQUIRED`。
- BDD: 可回滚计划 -> Given `PROVABLE_UNBOUND` 记录，When `build_rollback_plan` 未获人工批准，Then `write_allowed=false`；获得 `APPROVED` 后仅生成 `NEW_ORIGIN_TRACE_LINKS_ONLY` 计划。
- BDD: 只读 dry-run 矩阵 -> Given 规范化历史记录矩阵，When `build_dry_run_report` 执行，Then 输出五类计数、逐节点证据、唯一 batchExecutionId、`write_allowed=false` 和空副作用列表，不连接生产数据库。
- BDD: dry-run 唯一性 -> Given 同一 migrationBatchId 中重复 batchExecutionId，When 生成报告，Then 返回 `DUPLICATE_BATCH_EXECUTION_ID` 并停止，不写入任何关系。
- RED: `python -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q` -> FAIL（已运行，环境缺少 pytest：`No module named pytest`；随后改用标准 Python runner，不把环境缺失冒充业务失败）。
- GREEN: `python IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS（已运行，12 个场景通过）。

## 3. RED 计划（均未运行）

- RED: mvn -pl yudao-module-mes -Dtest=MesProductionCompletionBackfillContractTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseReportStageInitializerTest,MesProductionReleaseReportServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest test -> FAIL（计划，未运行），预期暴露无 receipt 先建批次、零损耗被要求空损耗证据或 writer 直接依赖 batch ID。
- RED: mvn -pl yudao-module-mes -Dtest=CompletionBackfillAtomicRollbackContractTest test -> FAIL（计划，未运行），预期暴露错误提交 BACKFILL_FAILED receipt、部分回填或允许流程 6 消费失败事实。
- RED: mvn -pl yudao-module-mes -Dtest=LossDecisionStateContractTest,LossReportRequirementContractTest test -> FAIL（计划，未运行），预期暴露把流程 5 错误简化为二态、缺失 lossRecordId 推断 NO_LOSS、BLOCKED 仍可建批或无损耗生成空单。
- RED: mvn -pl yudao-module-mes -Dtest=BatchTraceabilityOriginTest,ActiveOrderCompletionBatchTraceabilityTest,MaterialIssueTraceabilityValidationTest,LossTraceabilityTest,BatchDossierFourDocumentsTest,BatchTraceabilityQueryPermissionTest,LegacyBatchTraceabilityMigrationTest test -> FAIL（计划，未运行），预期暴露来源图、四材料、历史关系和多入口合同缺口。
- RED: mvn -pl yudao-module-mes -Dtest=BatchProvisioningStateOwnerContractTest,BatchOriginMappingGateContractTest test -> FAIL（计划，未运行），预期暴露缺少 BATCH_PROVISIONING/BATCH_PROVISIONING_RETRYABLE/BATCH_PROVISIONING_BLOCKED/BATCH_READY、Tx-C 映射前置或把可变 batchExecutionId 当作成功 receipt。
- RED: mvn -pl yudao-module-mes -Dtest=MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseServiceImplTest test -> FAIL（计划，未运行），预期暴露直接 RELEASED、批次关闭前放行或流程 8/10 未共用硬门禁。
- RED: node --test tests/e2e/edhr-batch-release-state-ui-static.spec.js -> FAIL（计划，未运行），预期暴露前端动作可绕过完成、批次和四材料 gate。

RED 只接受业务断言失败；环境、依赖、账号或正式数据缺失记为 BLOCKER，不冒充 RED。

## 4. GREEN 计划（均未运行）

- GREEN: mvn -pl yudao-module-mes -Dtest=MesProductionCompletionBackfillContractTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseReportStageInitializerTest,MesProductionReleaseReportServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest test -> PASS（计划，未运行）。
- GREEN: mvn -pl yudao-module-mes -Dtest=CompletionBackfillAtomicRollbackContractTest test -> PASS（计划，未运行；仅成功 Tx-A 产生 BACKFILL_SUCCEEDED receipt，失败只保留失败尝试审计）。
- GREEN: mvn -pl yudao-module-mes -Dtest=LossDecisionStateContractTest,LossReportRequirementContractTest,BatchTraceabilityOriginTest,ActiveOrderCompletionBatchTraceabilityTest,MaterialIssueTraceabilityValidationTest,LossTraceabilityTest,BatchDossierFourDocumentsTest,BatchTraceabilityQueryPermissionTest,LegacyBatchTraceabilityMigrationTest test -> PASS（计划，未运行；覆盖 REQUIRED/NO_LOSS/BLOCKED、订单 receipt SUCCESS/NOT_REQUIRED、四材料和历史 BLOCKED_LEGACY）。
- GREEN: mvn -pl yudao-module-mes -Dtest=BatchProvisioningStateOwnerContractTest,BatchOriginMappingGateContractTest test -> PASS（计划，未运行；Tx-B 先 BATCH_PROVISIONING，Tx-C 成功后才 BATCH_READY，映射失败保持 retryable/blocked）。
- GREEN: mvn -pl yudao-module-mes -Dtest=MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseServiceImplTest test -> PASS（计划，未运行）。
- GREEN: node --test tests/e2e/edhr-batch-release-state-ui-static.spec.js -> PASS（计划，未运行）。

最小 GREEN 顺序固定为：流程 1/2/3 正式事实 -> 流程 4 Tx-A/成功 receipt/统一回填 -> 流程 5 逐工序损耗状态 -> 活跃订单直接消费 receipt（其它入口先流程 9 凭证校验）-> 流程 6 Tx-B BATCH_PROVISIONING -> 流程 7 Tx-C 来源映射 -> 流程 6 BATCH_READY -> 流程 8 四材料 MATERIALS_READY -> 流程 10 finalization -> 流程 7 放行后追溯。

## 5. REGRESSION 计划（均未运行）

| 层级 | 覆盖范围 | 必须证据 |
|---|---|---|
| 后端单元/集成 | 双 100%、正式领料、三类回填、逐工序 REQUIRED/NO_LOSS/BLOCKED、订单 receipt SUCCESS/NOT_REQUIRED、无损耗、正损耗、幂等、版本、Tx-A 原子回滚 | 失败原因、无 receipt 断言、BACKFILL_ATOMIC_ROLLBACK、hasActualLoss/lossQuantity/lossReportStatus、receipt/batch 唯一性 |
| 批次状态与来源映射 | Tx-B、BatchProvisioningState、completionBackfillReceipt/IndependentBatchPrerequisiteReceipt、流程 7 Tx-C | 四个 BATCH_* 状态所有者明确；映射失败 TRACE_MAPPING_BLOCKED，不得 MATERIALS_READY/RELEASED |
| 创建入口合同 | 活跃订单、排产联动、PQC 联动、手工重试、独立批次 | entryType 分流、正确凭证、独立入口无 activeOrderId 可合法成功 |
| 材料合同 | 四节点缺一、两成品检项互代、重复、替换、陈旧版本、hash/manifest | 四节点枚举和稳定 blocker |
| 放行合同 | 批次详情、PQC/生产申请、管理者代表批准、独立批次放行、并发 CAS | 只有流程 10 写 RELEASED，所有入口同一流程 8 gate |
| 前端静态 | 完成确认、入口动作、四材料独立展示、失败/版本提示、终态待办清理 | Node 静态合同输出 |
| Playwright 真实 E2E | 活跃订单多角色全链路；独立批次合法创建/四材料/放行；失败路径 | 页面录像/截图、任务数据标识、最终只读 API 核验 |
| 历史迁移 | 五类分类（RECEIPT_BOUND_COMPLETE、PROVABLE_UNBOUND、INCOMPLETE_OR_AMBIGUOUS、BLOCKED_LEGACY、ALREADY_RELEASED_REVIEW_REQUIRED）、旧三项历史资料归 BLOCKED_LEGACY、缺映射/已放行来源不完整分类、dry-run、人工批准、唯一性、回滚 | migrationBatchId 报告、逐节点证据、分类计数、重复 ID 阻断、审核记录、hash 核对、ALREADY_RELEASED_REVIEW_REQUIRED |

REGRESSION: node tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js -> PASS（计划，未运行；需真实前置和新顺序）。

REGRESSION: Tx-A 失败不提交 receipt -> PASS（计划，未运行；失败尝试审计不可被流程 6 消费，成功重试才产生唯一 BACKFILL_SUCCEEDED receipt）。

## 6. 四材料保留和历史迁移规则

当前流程永远要求四个独立节点：INCOMING_INSPECTION_REPORT 来料检报告、STERILIZATION_REPORT 灭菌报告、FINISHED_PRODUCT_INSPECTION_REPORT 成品检报告、FINISHED_PRODUCT_INSPECTION_RECORD 成品检记录。四节点测试必须保留；每节点必须持久化 version、file_hash、source_snapshot_hash 并与 manifest 映射一致，有批准字段时还须 APPROVED。

历史仅有旧三项资料的记录一律进入 BLOCKED_LEGACY；即使发现候选第四节点，也必须经授权迁移证明其正式来源、hash、版本、操作者和时间，重新计算 manifest 后才能脱离该分类，不得通过兼容分支默认成功。其它关系不明记录进入 INCOMPLETE_OR_AMBIGUOUS。历史失败尝试记录不得升级为 receipt；若缺少成功 BACKFILL_SUCCEEDED receipt，建批必须阻断并等待人工迁移或重新完成。

## 7. 验收和 blocker

| 门禁 | 证据所有者 | 通过条件 |
|---|---|---|
| 流程 1/4/5/6 | MES 后端负责人 | Tx-A 失败无 receipt 且返回 BACKFILL_ATOMIC_ROLLBACK；成功 receipt、条件损耗、回填后建批和幂等通过 |
| 流程 7/8 | 追溯/资料负责人 | 来源图不可变、四节点硬门禁和 manifest 通过 |
| 流程 9 | 领域架构负责人 | 活跃/独立凭证分流、多创建入口、多放行入口合同通过 |
| 流程 10 | 放行负责人 | 唯一 finalization、CAS、驳回/撤回和终态追溯通过 |
| 真实 E2E | QA | 真实角色、真实页面、多入口和失败路径通过 |
| 历史迁移 | 数据 owner + 业务授权人 | dry-run、人工批准、核对和回滚通过 |

当前 blocker 只保留：合同已在流程 1-10 文档及本任务冻结，但生产代码尚未落地流程 4/5/6/7/8/9/10 的状态 owner、Tx-A/Tx-B/Tx-C、逐工序损耗、四材料和唯一放行；RED/GREEN/REGRESSION、真实租户/角色/正式来源/四份附件、真实 Playwright E2E 和历史迁移/回滚证据均未运行或未完成。流程 4、5、7、10 文档已存在并纳入本总方案，不再列为缺失文档 blocker。

M12 实际证据：无第三方依赖的 `python IntRuoyiBackend/script/run_flow_repair_11_contracts.py` 已执行 12 个场景并通过；pytest 入口因环境缺少 pytest 未运行。另已执行规范化历史 fixture 的只读 dry-run，输出总数 8、唯一批次 ID 8，分类计数为 RECEIPT_BOUND_COMPLETE=1、PROVABLE_UNBOUND=1、INCOMPLETE_OR_AMBIGUOUS=4、BLOCKED_LEGACY=1、ALREADY_RELEASED_REVIEW_REQUIRED=1，`write_allowed=false` 且 `side_effects=[]`。这些证据不代表生产代码、数据库迁移或全链路回归通过。
