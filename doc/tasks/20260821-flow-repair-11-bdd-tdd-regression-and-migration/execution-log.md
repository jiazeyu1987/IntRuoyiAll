# 执行日志：流程修复 11

## 任务边界

本专项只做全链路 BDD/TDD、分层回归、历史迁移/回滚与 Go/No-Go 汇总。流程11可独立修改本任务 Markdown、无副作用迁移分类器及其合同测试；不修改生产业务状态、数据库、迁移执行、配置、服务进程、运行态数据或写入型 E2E。

## 里程碑记录

- M1 已完成：建立独立任务目录，确认目标流程、只读审计和禁止范围。
- M2 已完成：读取正式规则、产品/后端/前端/E2E 约束，审计当前代码、测试和流程修复 1-10 文档。
- M3 已完成：固定完成 -> 回填 -> 建批 -> 四材料 -> 放行顺序，定义状态 owner、接口、幂等、正式来源和追溯合同。
- M4 已完成：形成需求追踪矩阵、BDD、RED/GREEN/REGRESSION 分层计划、旧测试调整和历史迁移/回滚边界。
- M5 已完成：完成只读文档一致性检查和 task-closeout-cleanup preview/apply；五份正式文档均保留，无删除项。
- M6 已完成：按流程修复 06 最新合同修订五份文档，明确 Tx-A 失败返回 BACKFILL_ATOMIC_ROLLBACK、不提交失败 receipt，成功才提交 BACKFILL_SUCCEEDED，流程 6 独占 BATCH_* 状态。
- M7 已完成：按流程修复 04/05/06/07/09/10 最新合同同步四份材料、逐工序损耗状态、流程 7 Tx-C 放行前映射、流程 6 四个 BATCH_* 状态、独立入口凭证和合同已冻结/代码未落地的 Go/No-Go 口径。
- M8 已完成：只读一致性扫描通过，历史旧三项统一归 BLOCKED_LEGACY；cleanup preview/apply 通过且无删除项，五份正式文档全部保留；测试、E2E、迁移和生产代码均未运行或修改。
- M9 已完成：补齐 task.md 自身 Cleanup Keep，统一流程 8 材料状态为当前有效 COMPLETED（有批准字段时 APPROVED，版本/hash/source snapshot 一致），并将历史迁移分类统一为五类。
- M10 已完成：最终只读一致性扫描、cleanup preview/apply 均通过且无删除项，五份正式文档全部保留；测试、E2E、迁移和生产代码均未运行或修改。
- M11 已完成：最终五份文档一致性扫描通过；本轮 cleanup preview/apply 通过且无删除项，五份正式文档全部保留；当时流程1-10测试、E2E、生产迁移和生产代码仍未运行或修改，后续 M12 已新增流程11 runner/dry-run fixture 证据。

## 正式来源和跨线程合同证据

已核对流程修复 01、02、03、04、05、06、07、08、09、10 的任务目录/合同。流程 04 负责双 100% 完成和同一完成节点三类回填并输出 receipt；流程 05 负责逐工序 REQUIRED/NO_LOSS/BLOCKED 决策及订单 receipt SUCCESS/NOT_REQUIRED、hasActualLoss、lossQuantity、lossReportStatus；流程 06 仅消费成功回填 receipt 或独立前置凭证建立/复用批次并独占 BATCH_PROVISIONING、BATCH_PROVISIONING_RETRYABLE、BATCH_PROVISIONING_BLOCKED、BATCH_READY；流程 07 负责 BATCH_READY 前 Origin/TraceLink Tx-C 映射及放行后追溯；流程 08 负责四个独立材料节点及硬门禁；流程 09 负责多入口凭证、场景分流、幂等和追溯前置；流程 10 负责所有入口汇聚的唯一最终 RELEASED CAS 状态。

当前四个必填材料节点固定为：来料检报告、灭菌报告、成品检报告、成品检记录。四节点必须为当前有效 COMPLETED，持久化版本/hash/source snapshot 一致，有批准字段时还须 APPROVED；旧三项资料一律归 BLOCKED_LEGACY，不得成为当前流程的兼容成功条件。

## BDD/TDD markers

BDD: 活跃订单完成后统一回填再建批 -> Given 正式领料绑定、双签名/组长复核、生产和检验均 100% / When 生产组长点击完成 / Then 流程 4 同节点完成三类适用回填并返回 receipt，流程 6 才能创建或复用批次。

BDD: Tx-A 失败原子回滚 -> Given 三类回填任一校验或本地写入失败 / When 完成命令执行 / Then 返回 BACKFILL_ATOMIC_ROLLBACK，不提交 receipt、不产生 BACKFILL_FAILED receipt；仅记录失败尝试且不可被流程 6 消费，用户可重新点击完成。

BDD: 无损耗不建空单 -> Given 流程 5 逐工序 decision=NO_LOSS、有正式零损耗快照且 hasActualLoss=false/lossQuantity=0 / When 完成事务执行 / Then completion receipt 的 lossReportStatus=NOT_REQUIRED，不生成损耗单 ID；缺失 lossRecordId 不得单独推断无损耗。

BDD: 正损耗与阻断 -> Given 任一工序 decision=REQUIRED 且正式损耗事实完整，或任一工序事实缺失而为 BLOCKED / When 完成 / Then 正损耗提交正式损耗单并返回 lossReportStatus=SUCCESS，BLOCKED 返回稳定 blocker、Tx-A 回滚且流程 6 不得建批。

BDD: 四材料严格齐套 -> Given 批次已由合法入口创建 / When 四个材料节点缺一、过期、hash/version 不一致或类型未知 / Then 流程 8 返回稳定 blocker，所有放行入口均不得进入 RELEASED。

BDD: 成品检报告和成品检记录不可互代 -> Given 仅其中一个成品检节点完成 / When 检查材料 manifest / Then 仍阻断，不合并、不推断、不默认成功。

BDD: 多创建入口幂等和独立入口 -> Given 活跃订单、排产、PQC、受控重试或独立正式凭证入口 / When 活跃订单消费流程 4 成功 receipt，或其它入口先由流程 9 校验 IndependentBatchPrerequisiteReceipt 和正式 source relation 后调用流程 6 / Then 相同 source relation/hash/version 复用同一批次，合法无 activeOrderId 的独立入口可创建，场景凭证混用或历史关系不明则阻断，随后必须经流程 7 Tx-C 映射。

BDD: 放行前映射门禁 -> Given 流程 6 Tx-B 已写 BATCH_PROVISIONING / When 流程 7 Tx-C 写入并校验 Origin/TraceLink、工单、领料和三类回填映射 / Then 成功后才 BATCH_READY，失败返回 TRACE_MAPPING_BLOCKED，流程 8 材料上传和流程 10 放行均阻断。

BDD: 多放行入口唯一最终化 -> Given 批次详情、PQC/生产申请、管理者代表批准或独立批次放行入口 / When 申请或批准 / Then 全部消费流程 8 同一四材料 gate，只有流程 10 能写 RELEASED，CAS 只产生一个最终胜者。

BDD: 放行后完整追溯 -> Given 流程 10 已写 RELEASED / When 从订单、工单、领料、批次、申请或决定查询 / Then 返回生产/PQC/复核、回填、损耗、四材料 manifest、来源关系、操作者、时间、版本和最终决定。

BDD: 历史关系不明阻断 -> Given 历史记录缺 completion receipt、IndependentBatchPrerequisiteReceipt、batchExecutionSourceRelation、流程 7 映射、第四材料节点或 hash/version / When 重试建批或放行 / Then 返回 BLOCKED_LEGACY 或对应迁移 blocker，不自动认领、复用、删除或补默认资料。

RED: mvn -pl yudao-module-mes -Dtest=MesProductionCompletionBackfillContractTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseReportStageInitializerTest,MesProductionReleaseReportServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest test -> FAIL（计划，未运行），预期暴露旧的先建批后写资料顺序和统一回填 receipt 缺口。

RED: mvn -pl yudao-module-mes -Dtest=CompletionBackfillAtomicRollbackContractTest test -> FAIL（计划，未运行），预期暴露 BACKFILL_FAILED receipt、部分提交或失败事实被流程 6 消费。

RED: mvn -pl yudao-module-mes -Dtest=LossDecisionStateContractTest,LossReportRequirementContractTest test -> FAIL（计划，未运行），预期暴露把损耗状态错误简化为二态、BLOCKED 仍建批、缺 lossRecordId 推断 NO_LOSS 或无损耗生成空单。

RED: mvn -pl yudao-module-mes -Dtest=BatchTraceabilityOriginTest,ActiveOrderCompletionBatchTraceabilityTest,MaterialIssueTraceabilityValidationTest,LossTraceabilityTest,BatchDossierFourDocumentsTest,BatchTraceabilityQueryPermissionTest,LegacyBatchTraceabilityMigrationTest test -> FAIL（计划，未运行），预期暴露来源图、四节点、历史分类和多入口合同缺口。

RED: mvn -pl yudao-module-mes -Dtest=BatchProvisioningStateOwnerContractTest,BatchOriginMappingGateContractTest test -> FAIL（计划，未运行），预期暴露 BATCH_* 状态 owner 不完整、completionBackfillReceipt/IndependentBatchPrerequisiteReceipt 与可变 BatchProvisioningState 边界缺失、Tx-C 映射前误进 BATCH_READY 或材料/放行。

RED: mvn -pl yudao-module-mes -Dtest=MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseServiceImplTest test -> FAIL（计划，未运行），预期暴露直接 RELEASED 或流程 8/10 未共用硬门禁。

RED: node --test tests/e2e/edhr-batch-release-state-ui-static.spec.js -> FAIL（计划，未运行），预期暴露前端动作可绕过完成、建批和四材料 gate；真实 Playwright E2E 仍需真实前置数据。

GREEN: 上述后端合同测试组 -> PASS（计划，未运行；仅在实现线程完成 RED 修复且真实依赖可用后执行）。

GREEN: mvn -pl yudao-module-mes -Dtest=CompletionBackfillAtomicRollbackContractTest test -> PASS（计划，未运行；失败仅记录 CompletionBackfillFailureAttempt，成功才提交 BACKFILL_SUCCEEDED receipt）。

GREEN: mvn -pl yudao-module-mes -Dtest=LossDecisionStateContractTest,LossReportRequirementContractTest,BatchProvisioningStateOwnerContractTest,BatchOriginMappingGateContractTest test -> PASS（计划，未运行；覆盖 REQUIRED/NO_LOSS/BLOCKED、SUCCESS/NOT_REQUIRED、四个 BATCH_* 状态和 Tx-C 映射门禁）。

GREEN: node --test tests/e2e/edhr-batch-release-state-ui-static.spec.js -> PASS（计划，未运行；静态合同通过不替代真实 Playwright）。

REGRESSION: 保留四材料枚举和四节点缺一阻断 -> PASS（计划，未运行；不得将历史三项记录当作当前成功，旧三项仅 BLOCKED_LEGACY）。

REGRESSION: Tx-A 失败不提交 receipt -> PASS（计划，未运行；失败尝试不是 receipt/成功事实，流程 6 不得消费，成功重试生成唯一 BACKFILL_SUCCEEDED）。

REGRESSION: node tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js -> PASS（计划，未运行；需真实角色、正式工单/领料、签名、PQC 和四份附件，按新顺序执行）。

REGRESSION: node tests/e2e/edhr-special-nodes-real-flow.e2e.js -> PASS（计划，未运行；需验证无损耗、不适用和历史阻断节点）。

流程1-10相关 RED/GREEN/REGRESSION 命令均为计划，不构成实际 PASS；M12 runner 和 fixture dry-run 的实际结果见下文。环境、依赖、账号或正式数据缺失必须记录为 BLOCKER，不冒充 RED。

## 历史迁移与回滚证据

迁移先只读 dry-run，使用五类分类：RECEIPT_BOUND_COMPLETE、PROVABLE_UNBOUND、INCOMPLETE_OR_AMBIGUOUS、BLOCKED_LEGACY、ALREADY_RELEASED_REVIEW_REQUIRED。只有关系可证明且人工批准的记录才能写 Origin/TraceLink；旧三项资料一律归入 BLOCKED_LEGACY，缺第四节点、关系不明或仅有失败尝试记录而无成功 BACKFILL_SUCCEEDED receipt 的其它记录归入 INCOMPLETE_OR_AMBIGUOUS；已放行但来源/映射不完整归入 ALREADY_RELEASED_REVIEW_REQUIRED。迁移批号+业务 ID 幂等，写后核对计数/唯一性/hash/追溯链。回滚只撤销本批新增关系，不删除原始生产事实；已放行记录须走正式撤销/纠错，不得覆盖。

## 当前 blocker

1. 四份材料、流程 1-10 的接口、状态 owner、入口顺序、Tx-A/Tx-B/Tx-C、Origin/TraceLink、流程 8 gate 和流程 10 finalization 合同已冻结；生产代码尚未落地这些合同，且 Tx-A 失败不得产生 BACKFILL_FAILED receipt、流程 5 损耗状态和流程 6 BATCH_* 语义尚未实现。
2. 当前实现和旧测试仍存在先建批、资料后写及多入口直接放行路径，必须完成 RED/GREEN/REGRESSION。
3. 真实租户、角色、签名、正式工单/领料、PQC 汇总和四份附件尚未准备或使用，真实 Playwright E2E 尚未执行。
4. 生产历史批次/申请尚未执行授权后的真实只读 dry-run、人工复核和回滚演练；规范化 fixture dry-run 已执行并有计数证据。

流程修复 04、05、07、10 的任务文档和合同已存在并纳入本日志，不列为缺失文档 blocker。

## M12 独立 worktree 实现证据

- Worktree：`D:\IntRuoyiWorktree\20260822-flow-repair-11-design-development`；分支：`codex/20260822-flow-repair-11-design-development`。主工作树未修改。
- 提交前按 worktree 规则登记 `int_main` profile slot=9（8090/48090）；本任务未启动前后端服务，登记仅用于提交 hook 门禁。
- BDD: 五类历史分类 -> Given 规范化历史记录，When 运行 `classify_legacy_batch`，Then 只返回五类冻结枚举；旧三材料为 `BLOCKED_LEGACY`，已放行来源不完整为 `ALREADY_RELEASED_REVIEW_REQUIRED`。
- BDD: 四节点持久化证据 -> Given 四节点均为 COMPLETED，When 任一节点或 manifest 缺 version、file_hash 或 source_snapshot_hash，Then 分类为 `INCOMPLETE_OR_AMBIGUOUS`，不得认定材料齐套。
- BDD: 独立来源凭证 -> Given IndependentBatchPrerequisiteReceipt，When 缺正式 batchExecutionSourceRelation，Then 分类为 `INCOMPLETE_OR_AMBIGUOUS`，不得进入 `PROVABLE_UNBOUND`。
- BDD: 可回滚计划 -> Given `PROVABLE_UNBOUND`，When 未获人工批准，Then 不允许写入；获 `APPROVED` 后只生成 `NEW_ORIGIN_TRACE_LINKS_ONLY` 计划。
- RED: `python IntRuoyiBackend/script/run_flow_repair_11_contracts.py`（实现前） -> FAIL，`ImportError: cannot import name 'build_dry_run_report'`，证明 dry-run 合同先行失败。
- RED: `python -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q` -> FAIL，环境缺少 pytest（`No module named pytest`）；该失败记录为测试依赖 blocker，不作为业务 RED。
- GREEN: `python IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS，12 个迁移分类、dry-run、回滚场景通过。
- REGRESSION: 规范化 fixture dry-run -> PASS，总数 8、唯一批次 ID 8，分类计数为 1/1/4/1/1，所有 entry `write_allowed=false`，报告 `side_effects=[]`。
- Implementation: 新增 `IntRuoyiBackend/script/flow_repair_11_migration.py`、`IntRuoyiBackend/script/run_flow_repair_11_contracts.py` 和 `IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py`；纯函数无数据库、SQL、网络和文件写入副作用。
- COMMIT: `git commit -m "feat(flow11): add migration dry-run contracts"` -> BLOCKER；提交 hook 使用当前基线 v4 守卫检查共享登记表时，发现其它任务 worktree `D:\IntRuoyiWorktree\20260820-pqc-inspection-equipment-selection` 已登记 slot=31，超出 v4 的 1..30 范围。未使用 `--no-verify`，未修改其它任务登记；task-owned 变更保持 staged，等待项目级 runtime guard 同步后再提交。
- Backend regression attempt: bundled Maven 编译在既有 `MesFrontlinePqcContextServiceImpl.java:736` 因缺少 `EquipmentOption` 符号失败；未运行到流程11 Java 测试，记录为基线 blocker。

## 收尾状态
