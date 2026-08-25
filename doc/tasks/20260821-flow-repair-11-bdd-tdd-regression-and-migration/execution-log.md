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
- RED: `python -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q`（首次环境检查） -> FAIL，环境缺少 pytest（`No module named pytest`）；该次失败仅记录为依赖前置，不作为业务 RED。
- GREEN: `python IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS，12 个迁移分类、dry-run、回滚场景通过。
- GREEN: `python -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q` -> PASS，`12 passed in 0.16s`。
- GREEN: `python -m py_compile IntRuoyiBackend/script/flow_repair_11_migration.py IntRuoyiBackend/script/run_flow_repair_11_contracts.py IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py` -> PASS。
- REGRESSION: 规范化 fixture dry-run -> PASS，总数 8、唯一批次 ID 8，分类计数为 1/1/4/1/1，所有 entry `write_allowed=false`，报告 `side_effects=[]`。
- Implementation: 新增 `IntRuoyiBackend/script/flow_repair_11_migration.py`、`IntRuoyiBackend/script/run_flow_repair_11_contracts.py` 和 `IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py`；纯函数无数据库、SQL、网络和文件写入副作用。
- COMMIT: `6a6d2afac`（流程11迁移实现/合同和 runtime v5 同步）、`9e188f9d6`（本地 runtime 旧限制文档修正）、`d012479b2`（worktree 长期经验修正）均已由正常 hook 创建；未使用 `--no-verify`，未修改其它任务登记。
- Runtime verification: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/preflight/branch-runtime-port-guard.ps1` -> PASS；当前 worktree 登记为 `int_main slot=9`（8090/48090），共享 `slot=31` 按 runtime v6 合法范围处理，未启动服务。
- Backend regression attempt (M13 historical result): bundled Maven `mvn -pl yudao-module-mes -am -DskipTests compile` -> FAIL；MES 编译在 `MesFrontlinePqcContextServiceImpl.java:736` 因 `MesQaInspectionRegulationPublishedVersionRespVO` 缺少 `EquipmentOption` 符号阻断，未运行流程1-10 Java 合同测试。该阻断已由 M14 修复并复验。

## M13 提交、融合和复验

- Python 合同 runner、pytest、py_compile 和 runtime guard 已取得实际 PASS；流程11 worktree 当前 clean。
- 主工作树 `E:\IntRuoyi` 仍有 114 个 tracked dirty 文件及大量 untracked 任务产物；其中与本分支提交重叠的 `AGENTS.md`、`docs/local-runtime.md`、`docs/worktree-memory.md` 不得覆盖。`int_main` 与流程11分支均未互为 ancestor，主线融合必须等待主工作树整理并进行语义合并。
- 当前尚未启动服务、访问数据库、执行生产历史迁移、人工批准/回滚演练或真实 Playwright E2E；这些仍是跨流程 blocker。

## 收尾状态

`completed`（流程11专项范围）：流程11 task-owned 代码、runner/pytest/py_compile、全模块 Maven compile、定向 JUnit、Node/TS 静态检查、runtime guard、diff-check、主线融合和 staged 删除恢复均已取得实际证据。流程1-10生产回归、真实 E2E、生产历史迁移/回滚和人工批准仍为跨流程 No-Go blocker。未使用 `--no-verify`，未覆盖主工作树其它 dirty/untracked 文件或其它任务登记。

## M14 编译回归修复证据

BDD: 发布版 QA 设备选项 DTO -> Given `MesFrontlinePqcContextService` 和现有测试需要 `equipmentRequired`、`equipmentOptions` 及嵌套 `EquipmentOption` / When 编译 MES 模块 / Then DTO 暴露完整类型契约且生产源码编译成功。

RED: `mvn -pl yudao-module-mes -am -DskipTests compile`（修复前） -> FAIL，`MesFrontlinePqcContextServiceImpl.java:736` 找不到 `MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption`。

GREEN: `mvn -pl yudao-module-mes -am -DskipTests compile`（2026-08-22） -> PASS，Reactor 24/24 模块 `BUILD SUCCESS`；仅保留既有 javac warning。

REGRESSION: `mvn -pl yudao-module-mes -Dtest=MesFrontlinePqcContextServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`；testCompile 和定向服务回归均通过。

M14 修改范围：恢复发布版/保存版 QA DTO 的设备字段和 `EquipmentOption` 嵌套 DTO，并修正 Word 导入测试的 `process` fixture；未修改业务状态、数据库、服务进程或主工作树。

M14 COMMIT: `006a954d65c770a4454f41ed60a0ea312b3ad55a`，由正常 branch-runtime hook 创建，未使用 `--no-verify`。

## M15 主线受保护融合复核

- Task-owned 审计：流程11迁移脚本、合同测试、五份任务文档以及 M14 QA DTO/测试 fixture 属于本任务；`AGENTS.md`、运行时文档和守卫历史改动属于共享基础设施，不作为流程11业务交付追加。
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/preflight/branch-runtime-port-guard.ps1`（`E:\IntRuoyi`） -> PASS，`int_main/int_main` 使用 8081/48081。
- `git merge --ff-only codex/20260822-flow-repair-11-design-development`（`E:\IntRuoyi`）仍不能直接更新本地主工作树：本地存在用户未提交/未跟踪的 `AGENTS.md`、运行时文档、任务文档和 Word fixture，快进会覆盖它们；未执行 reset/checkout/stash/clean，也未覆盖这些文件。
- 在干净流程11集成 worktree 中已合入主线前后端 task-owned 提交 `5f0138e4c`，生成受保护融合提交 `378ce5719`；补齐被全局 ignore 排除的 BPM 编译源 `dabb52df0`、ERP 编译源 `dfb3fcea8` 和 10 个运行时合同测试 `9ba449c44`。
- `git push origin HEAD:int_main` -> PASS；远端已包含集成提交 `c22d4df23`，`git merge-base --is-ancestor codex/20260822-flow-repair-11-design-development origin/int_main` -> PASS。
- 主线前后端提交 `5f0138e4c` 仅包含已审计的 68 个后端/前端代码与测试文件，正常 hook 和 push 均通过；共享文档及其它任务登记未纳入。
- 当前仍未启动服务、访问数据库、执行生产历史迁移、人工批准/回滚演练或真实 Playwright E2E；这些仍是跨流程 blocker。

## M16 编译修复与集成后验证

BDD: 被全局 ignore 排除的编译源和运行时合同测试 -> Given BPM/ERP 生产代码引用这些类型 / When 在干净集成 worktree 编译和执行合同测试 / Then 源文件与测试被 task-owned 追踪，且不通过旁路或默认成功掩盖缺失。

RED: 完整 bundled Maven compile（修复前） -> FAIL，先后暴露 `DefaultWordFormTemplateRecognizer.java:55` 缺失 `WordTableVisualSchemaBuilder`，以及 `ErpKingdeeSyncRuntimeServiceImpl.java` 缺失 `ErpKingdeeSyncRuntimeTransactionService`；根因是项目 `**/runtime/` ignore 错误排除需要的源码。

GREEN: `mvn -pl yudao-module-erp -am -DskipTests compile` -> PASS；完整 `mvn -pl yudao-module-bpm,yudao-module-erp,yudao-module-infra,yudao-module-mes -am -DskipTests compile` -> PASS，24/24 modules `BUILD SUCCESS`。

GREEN: `python -X utf8 IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS，12 场景；`python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py` -> PASS，12 passed；`python -X utf8 -m py_compile ...` -> PASS。

REGRESSION: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_branch_runtime_profile.py -q --basetemp D:\IntRuoyiWorktree\flow11-pytest-temp` -> PASS，16 passed；覆盖 slot=31 扩展槽可用和 slot=51 越界 fail-fast，确认 runtime v6 合法范围为 1..50，且不再受 `must be between 1 and 30` 限制。

REGRESSION: `node --check` 两个受影响 E2E 静态脚本、Flow11 `branch-runtime-port-guard.ps1` 和 `git diff --check` -> PASS；未将静态检查冒充真实 Playwright，也未运行写入型 E2E。

## M17 当前本地 int_main 受保护融合与主线验证

- 集成 worktree 以当时本地主线为基线，正常 hook 生成融合提交 `abd37c4561f6b01f6c59cd8273c1df44bc75c752`；随后以最新并行主线为基线 cherry-pick 本段收尾记录，最终本地 `int_main` 为 `46948a7dde70f495c08e2b24a4acbf982f855d11`；未使用 `--no-verify`。
- 通过受保护 compare-and-swap 更新本地分支引用；`E:\IntRuoyi` 物理工作树的既有 dirty/untracked 文件未被 checkout、覆盖、reset、stash 或 clean。`git merge-base --is-ancestor 8fe9228b2 refs/heads/int_main` -> PASS，收尾文档提交祖先核验 -> PASS。
- `python -X utf8 IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS，12 场景；`python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q --basetemp D:\IntRuoyiWorktree\flow11-main-pytest-temp` -> PASS，12 passed；`python -X utf8 -m py_compile ...` -> PASS。
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/preflight/branch-runtime-port-guard.ps1 -Profile int_main -WorktreePath D:\IntRuoyiWorktree\20260822-flow-repair-11-local-int-main-integration-v2` -> PASS，slot=25，8159/48159；slot 31 按 v5 合同合法，未修改其它登记。
- bundled Maven `mvn -pl yudao-module-bpm,yudao-module-erp,yudao-module-infra,yudao-module-mes -am -DskipTests compile` -> PASS，24/24 modules `BUILD SUCCESS`。ERP `ErpKingdeeSyncRuntimeServiceImplTest` -> PASS，6/6。BPM 定向 46 tests 中 45 通过，`DefaultWordFormTemplateRecognizerTest` 因 `resource/按压式球囊扩充压力泵IDI-001/过程检验记录.docx` 在干净集成树不存在而失败；该 fixture 仅存在于主工作树未跟踪文件，未复制、未提交或伪造，保持 blocker。
- `node --check` 两个受影响 E2E 静态脚本及合并树 `git diff --check` -> PASS；`E:\IntRuoyi` 主工作树 `git diff --check --no-ext-diff HEAD -- AGENTS.md docs scripts IntRuoyiBackend IntRuoyiFronted` -> exit 0（仅 CRLF 警告）。未启动服务、访问数据库或运行 Playwright。

## M18 物理主工作树最终验证

- 在 `E:\IntRuoyi\IntRuoyiBackend` 首先执行 bundled Maven `mvn -pl yudao-module-bpm,yudao-module-erp,yudao-module-infra,yudao-module-mes -am -DskipTests compile` -> PASS，24/24 modules `BUILD SUCCESS`。
- 复核确认上述三个 Python 文件及 BPM/ERP task-owned 源和测试源的 staged 删除均由本线程此前受保护 ref 更新造成；按路径从当前 `int_main` 精确恢复，未触碰其它 dirty/untracked 文件。恢复后 runner -> PASS 12，pytest -> PASS 12 passed，py_compile -> PASS。
- 物理主工作树前端 `pnpm run ts:check` -> PASS；两个 E2E 静态脚本 `node --check` -> PASS；主线 `branch-runtime-port-guard.ps1` -> PASS（8081/48081）；`git diff --check` -> exit 0（仅 CRLF 警告）。
- 恢复后的物理主工作树 ERP 定向 JUnit -> PASS 6/6；BPM 定向 JUnit -> PASS 46/46。未执行服务、数据库、真实 Playwright 或写入型迁移。

## M19 主线程最终收尾

- `int_main` 已确认包含流程11提交 `8fe9228b2`（祖先核验 PASS）；本次未重复融合。
- 主线程 Maven compile -> PASS，24/24 modules `BUILD SUCCESS`；流程11 Python runner -> PASS 12，pytest -> PASS 12 passed，py_compile -> PASS。
- 主线程 ERP 定向 JUnit -> PASS 6/6，BPM 定向 JUnit -> PASS 46/46；前端 `pnpm run ts:check`、两个受影响 E2E `node --check`、v5 runtime guard（8081/48081）和 `git diff --check` -> PASS。
- staged 删除复核为 0；此前确认的流程11自有误删已按当前 `int_main` 精确恢复。其它 dirty/untracked 文件（含并行 ERP 测试改动）保留未改。
- 流程11专项标记完成；未启动服务、访问数据库、执行真实 Playwright、生产历史迁移、人工批准或回滚演练，全链路继续 No-Go。

## M20 流程8全 MES 回归失败分类

BDD: 全 MES 回归失败必须可分派且不越权 -> Given 流程8提供 479 份 Surefire XML、聚合基线为 3575 tests / 59 failures / 93 errors / 19 skipped / 152 failure-or-error rows / When 流程11只读扫描并建立责任矩阵 / Then 每条 failure/error 都有 class、method、primary owner、root-cause 判断、是否阻断流程8、最小复现和后续动作；环境/fixture/依赖问题不得伪装成业务 RED，流程8业务代码不被修改。

GREEN: `PowerShell` 只读扫描 `D:\IntRuoyiWorktree\20260822-flow-repair-08-design-development\IntRuoyiBackend\yudao-module-mes\target\surefire-reports\TEST-*.xml` -> PASS，479 suites 聚合为 3575/59/93/19；逐条清单 `flow8-mes-regression-classification.md` 通过 152 条 bullet 与 59+93 对账。

REGRESSION: 流程8定向 215 tests 已有 PASS 证据且 `F8-GATE=0`；5 条 `F7-TRACE`、84 条 `A456`、63 条 `PAR` 均保留为跨线程 owner blocker/条件 blocker。前端 `batchrecordcelllink` `routeProcessId` 静态错误标记 `PAR+ENV`；runtime v6 slot=31 合法，不计入业务失败。

RED: `mvn -pl yudao-module-mes test -Dsurefire.failIfNoSpecifiedTests=false` -> FAIL，PowerShell 参数拆分为无效 lifecycle phase，未运行测试；修正引号后同一全量命令 -> FAIL，JVM native memory allocation failure，未进入 surefire。两条均为工具/环境 blocker，不改变 XML 工件事实，不得写成流程8业务 RED。

Owner 通知已写入分类报告第 5 节：流程8 owner（0 条直接 gate failure）、流程7 owner（5 条来源/追溯前置）、流程4/5/6/9/10及并行 owner（A456/PAR）和测试基础设施 owner（fixture、依赖、19 skipped）。流程11只维护总回归合同和阻断证据。

## M21 受控 Maven 重跑与内存阻断复核

BDD: 全量 MES 回归必须先排除工具环境阻断再判定业务结果 -> Given 旧命令曾因 PowerShell 参数拆分失败，修正后又在 Surefire 前触发 JVM native memory allocation failure / When 使用 bundled Maven 并限制本次进程 JVM 内存后重跑 `-pl yudao-module-mes test` / Then 命令进入 Surefire，内存崩溃不再复现，但真实测试失败仍按 owner 矩阵分类，不得写成 PASS。

RED: 未引用参数的 PowerShell Maven 命令 -> FAIL，参数被拆成无效 lifecycle phase，未运行测试；修正后无内存限制的同一全量命令 -> FAIL，`hs_err_pid49664.log` 记录 `Native memory allocation (malloc) failed to allocate 2408400 bytes`，当时 JVM ergonomic MaxHeapSize 约 8GB、系统物理可用约 1.7GB，未进入 Surefire。

GREEN（工具门禁）: `$env:MAVEN_OPTS='-Xms256m -Xmx2048m -XX:MaxMetaspaceSize=512m -XX:ReservedCodeCacheSize=128m -XX:CICompilerCount=2 -Xss512k'` 配合 bundled Maven `C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -o -pl yudao-module-mes test '-Dsurefire.failIfNoSpecifiedTests=false'` -> 进入 Surefire，未再次出现 native memory allocation failure。

REGRESSION（当前主线结果）: 本次受控重跑生成的当前 `target/surefire-reports` 汇总为 240 suites、1589 tests、7 failures、195 errors、0 skipped；失败集中在 `MesProcessPoolTeamLeaderControllerTest`（1F）、`MesProScheduleOrderControllerTest`（5E）、`MesC015RouteDccQaReconciliationSchemaTest`（1F）、`MesProcessPoolSchemaTest`（1F）、`MesProEdhrTraceTerminalPartitionContractTest`（2F）、`MesProRouteScheduleConfigServiceTest`（14E）、`MesIndependentBatchPrerequisiteReceiptServiceTest`（1F）、`MesProEdhrBatchExecutionLegacyProcessTest`（1E）、`MesProEdhrBatchExecutionServiceTest`（167E）、`MesProEdhrBatchExecutionTaskGateTest`（7E）、`MesProEdhrWorkTaskLegacyProcessTest`（1E）和 `MesFrontlineRuntimeConfigProcessScopeTest`（1F）。这些是当前主线业务/fixture/相邻流程回归结果，不能归入流程8自身 gate；不修改对应 owner 代码。

证据边界：PowerShell wrapper 的退出码不能单独代表 Surefire 通过；以 Surefire XML/TXT 的 failure/error 计数为准。本次只验证内存工具阻断已解除并取得真实失败清单，流程8四材料 gate 仍无直接失败证据；全链路 No-Go 保持不变。

## M22 当前 int_main 全 MES 314 条归属审计

BDD: 当前主线回归必须以最新 Surefire 工件为准 -> Given `int_main` 当前 HEAD 与其它并行 dirty/untracked 改动共存 / When 使用受控 `MAVEN_OPTS` 执行全 MES 测试并逐条读取 `target/surefire-reports/TEST-*.xml` / Then 记录本轮真实总数、首个根因、责任流程、级联阻断和后续动作，不沿用过期的 202 条快照。

RED: `mvn.cmd -o -pl yudao-module-mes test '-Dsurefire.failIfNoSpecifiedTests=false'`（受控 JVM） -> FAIL，退出码 1；Surefire 汇总 `3643 tests / 56 failures / 258 errors / 18 skipped`，共 314 条 failure/error。

GREEN（流程11工具门禁）: `python -X utf8 IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS，12 场景；`python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q --basetemp D:\IntRuoyiWorktree\flow11-audit-pytest-temp2` -> PASS，12 passed；三文件 `py_compile` -> PASS；ERP 定向 `ErpKingdeeSyncRuntimeServiceImplTest` -> PASS，6/6；BPM 定向 `DefaultWordFormTemplateRecognizerTest` -> PASS，1/1；前端 `pnpm run ts:check` -> PASS；runtime v6 guard -> PASS，slot=31 合法。

REGRESSION: 最新314条矩阵写入 `flow8-mes-regression-current-20260823.md`，覆盖 32 个测试类且 314/314 可追溯。归属汇总为 `F4/F6=235（26F/209E）`、`F7/F10=3（3F/0E）`、`PAR=76（27F/49E）`、`F8-GATE=0`。批次 bean/回填/任务门禁影响流程6，追溯终态影响流程7/10，前线/排产/路线/H2/Mockito 为相邻或基础设施 owner；流程11不代改业务所有权代码。

本轮没有启动服务、访问生产数据库、执行生产迁移、人工批准/回滚或真实 Playwright；全链路继续 No-Go。待流程2/3/4/5/6/7/8/10新提交融合后再重跑全 MES。

## M23 Flow7/10 提交后定向复验

BDD: Flow7/10 提交后的来源映射与终态追溯合同必须在当前主线重新验证 -> Given 旧 314 条矩阵生成于 `a6574c3631dfa3c5f8381596fcef5c91acd98db0`，其后已融合 Flow7 `7770f36fb` 和 Flow10 `af4c6d4d1` / When 在当前 `int_main` HEAD `af4c6d4d1f0febd987a0f652ccbd085f266ea490` 只运行受影响的两类定向测试 / Then 更新矩阵中的 Flow7/10 行，不把旧全量计数冒充当前结果。

RED: 旧矩阵证据 -> 记录为历史 `MesProEdhrTraceTerminalPartitionContractTest` 2F、`MesProBatchRecordRouteIdentityContractTest` 1F；该结果不再代表当前提交后的行为。

GREEN: `$env:MAVEN_OPTS='-Xms256m -Xmx1536m -XX:MaxMetaspaceSize=384m -XX:CICompilerCount=2'; mvn.cmd -o -pl yudao-module-mes '-Dtest=MesProEdhrTraceTerminalPartitionContractTest,MesProBatchRecordRouteIdentityContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，退出码 0；两类各 2 tests，合计 4 tests / 0 failures / 0 errors / 0 skipped。

REGRESSION: `flow8-mes-regression-current-20260823.md` 已将 Flow7/10 标注为历史 3F 与当前定向 0F/0E；其余历史 311 条 F/E 未因本轮重复全 MES，`F8-GATE=0` 保持。未终止 PID 4176/12944 等其它任务 runtime/Flow10 进程，未修改流程7/10业务代码。

本轮流程11门禁复验：`python -X utf8 IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS，12 场景；`python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q --basetemp D:\IntRuoyiWorktree\flow11-targeted-pytest-temp` -> PASS，12 passed；三文件 `py_compile` -> 退出码 0；`branch-runtime-port-guard.ps1 -Profile int_main -WorktreePath E:\IntRuoyi` -> 退出码 0（8081/48081）。

## M24 流程6提交门禁解锁审计

BDD: 流程6 task-owned 提交不得被旧 runtime 规则阻断 -> Given 全局 runtime v6 合同要求 slot `1..50`，而流程6 worktree 的未提交 profile 对 slot `41..50` 缺少第三扩展段 / When 只读检查 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`、流程6 worktree 规则文件并运行 branch-runtime guard / Then 由 runtime owner 补齐第三扩展解析和 registry contract 校验，流程6只提交业务路径。

RED: 流程6 worktree guard（修复前） -> FAIL，slot=41 登记为 `8256/48256`，旧 profile 错算为 `8206/48206`；不是流程4/6业务失败，也不是 slot=31 越界。

GREEN: 补齐 `scripts/runtime/branch-runtime-profile.ps1` 的第三扩展端口和 registry `contractVersion` 校验，修正 `docs/local-runtime.md` / `docs/worktree-restrictions.md` 的 `slot >= 51` 规则后，流程6 worktree `branch-runtime-port-guard.ps1 -Profile int_main -WorktreePath D:\IntRuoyiWorktree\20260822-flow-repair-06-design-development` -> PASS（8213/48213）；主线 E:\IntRuoyi guard -> PASS（8081/48081）；`git diff --check` -> PASS。

REGRESSION: 全局 runtime canonical commit 已存在于 `ea39dacc2`；流程6 owner 必须只 `git add` task-owned 代码，不能提交 runtime/docs 全局规则。registry 已有流程6 active slot=38 登记；slot=31 合法，不作为 blocker。未运行全量 MES、未修改流程4业务代码。

## M25 流程6/8旧 worktree 基线迁移

BDD: 旧 worktree 不得以 v4/v5 runtime 规则继续提交 -> Given 当前主线 `int_main=6717b60c1` 已统一 v6 / slot `1..50`，流程6 current-main-verify 仍为 v4/v5、流程8 design-development 仍为 v4 且无 registry / When 只读运行各自 guard 并为需要继续开发的线程创建干净 integration worktree / Then 旧目录保留只读，不覆盖其 dirty/staged 业务文件，新的 worktree 从 `6717b60c1` 建立并用 reserve 脚本登记。

RED: `D:\IntRuoyiWorktree\20260822-flow-repair-06-current-main-verify` guard -> FAIL，旧脚本/文档要求 `1..40` / v4-v5；`D:\IntRuoyiWorktree\20260822-flow-repair-08-design-development` guard -> FAIL，旧脚本要求 `1..30` / v4。两者都是 runtime 基线阻断，不是流程4、流程6或流程8业务 RED。

GREEN: `git worktree add -b codex/20260823-flow06-int-main-integration D:\IntRuoyiWorktree\20260823-flow06-int-main-integration 6717b60c1` 后，`reserve-worktree-slot.ps1` 登记 slot=45 / `8260/48260`；`git worktree add -b codex/20260823-flow08-int-main-integration D:\IntRuoyiWorktree\20260823-flow08-int-main-integration 6717b60c1` 后登记 slot=46 / `8261/48261`。两个 clean worktree 的 guard 均 PASS。

REGRESSION: 旧流程6/8目录不得继续提交全局 runtime 文件；对应 owner 应将业务差异迁移到新的 integration worktree，并只提交 task-owned 路径。slot=31 合法，未停止服务、未运行全 MES、未修改流程6/8业务代码。

M26: 两个干净 integration worktree 已通过 `git merge --ff-only 0fcb3f365` 快进到当前主线；各自 `branch-runtime-port-guard.ps1` 在其真实工作目录执行均退出码 0，F6 为 `8260/48260`、F8 为 `8261/48261`，worktree clean。主线 dirty/untracked 文件未触碰。

## M27 F4/F6/F8 统一 v6 基线与错误分类

BDD: F4/F6/F8 提交和验证必须使用同一 v6 基线 -> Given 当前唯一基线为 `8f4d843ad`、runtime 合同为 slot `1..50` / When 核对旧 worktree、registry 和新 integration worktree / Then 旧目录只读保留，统一从 `8f4d843ad` 创建并以 reserve 脚本登记，业务线程不修改全局 runtime 文件。

RED（旧版本）: F6 `D:\IntRuoyiWorktree\20260822-flow-repair-06-current-main-verify` guard -> FAIL，旧脚本/文档缺少当前 v6 所需 `1..40` 文本；F8 `D:\IntRuoyiWorktree\20260822-flow-repair-08-design-development` guard -> FAIL，旧脚本/文档缺少当前 v6 所需 `1..30` 文本。两者归类为旧版本，不是业务 RED。

RED（无登记）: F8 旧目录在 `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 无对应 active entry；归类为无登记，不能通过随机端口或手工改 registry 解决。

GREEN（当前基线）: `git worktree add -b codex/20260823-flow04-int-main-integration D:\IntRuoyiWorktree\20260823-flow04-int-main-integration 8f4d843ad` + reserve -> F4 slot=48 / `8263/48263`; F6 slot=45 / `8260/48260`; F8 slot=46 / `8261/48261`。三者 HEAD 均为 `8f4d843ad`、worktree clean、guard PASS。

REGRESSION（真实 ACL）: 对 F6/F8 旧目录执行 `Get-Acl` 均成功，owner 为 `A\BJB110`；不存在 helper deny-read、UnauthorizedAccess 或 ACL 拒绝证据，因此本轮没有真实权限错误。未删除旧目录、未改写 slot31、未整体提交主工作树。

## M28 基线 8af0aa8f2 流程 1-10 验证矩阵（观察员复核）

本轮只读复核基线为 `int_main` HEAD `8af0aa8f2`。通过 `git merge-base --is-ancestor` 核对的历史提交可作为该基线的可追溯定向证据；未在本轮重新运行全 MES、服务、数据库、迁移或写入型 Playwright。

| 流程 | 目标门禁 | 基线可引用证据 | 本轮状态 | 责任与阻断 |
|---|---|---|---|---|
| 2 | 一线生产提交签名事实，组长复核来源事实，不触发完成回填 | `cf58816f7` 在基线祖先链；任务报告记录 108 项定向/相邻测试通过 | 定向证据 PASS；服务/DB/写入 E2E 未运行 | 流程2 owner；不能替代流程4完成 receipt |
| 3 | 一线 PQC 提交/组长复核与汇集事实 | `477c97d410` 在基线祖先链；任务报告记录 27/27 定向测试通过 | 定向证据 PASS；完整下游闭环未运行 | 流程3 owner；正式过程检验回填仍由流程4消费 |
| 4 | 双 100% 完成节点 Tx-A 原子回填三类资料，仅成功提交 `BACKFILL_SUCCEEDED` | 当前报告明确为文档设计、实现测试 NOT_RUN，代码不符合目标顺序 | NOT RUN / No-Go | 流程4 owner；无 receipt 不得驱动流程6 |
| 5 | 逐工序 `REQUIRED/NO_LOSS/BLOCKED`，有损耗建单、无损耗不建单 | `24fdf7767a` 及验证基线提交在祖先链；任务报告记录 27/27 定向 JUnit 通过 | task-owned 定向证据 PASS；与流程4 Tx-A 集成未运行 | 流程5 owner；缺失正式损耗/零损耗快照仍阻断 |
| 6 | 消费成功回填 receipt 后建/复用批次，独占 `BATCH_*`，映射完成前不进材料 | 当前报告明确生产实现和测试 NOT_RUN；仅合同冻结 | NOT RUN / No-Go | 流程6 owner；不得先建批或消费失败尝试记录 |
| 7 | 建批后 Tx-C 写 Origin/TraceLink/Manifest，映射完成后才允许材料/放行，放行后追溯 | `7770f36fb` 在基线祖先链；任务报告记录 29/29，且 Flow7/10 受影响定向 4/4 通过 | 定向证据 PASS；真实 DB 映射/跨流程 E2E 未运行 | 流程7 owner；缺映射必须 `TRACE_MAPPING_BLOCKED` |
| 8 | 四独立节点均 `COMPLETED`（有批准字段时 `APPROVED`），version/hash/source snapshot/manifest 一致后 `MATERIALS_READY` | 当前报告仅文档与只读代码审计；四材料生产 gate 测试 NOT_RUN，无直接 gate failure 证据 | NOT RUN / No-Go | 流程8 owner；旧三材料、两成品检互代均阻断 |
| 9 | 多入口正式凭证、来源关系、幂等和建批前置；不拥有批次/放行状态 | `2a0d6d948` 在基线祖先链；任务报告记录 SQL 合同 1/1、receipt 4/4、入口回归 42/42 | 定向/SQL 合同证据 PASS；真实 DB migration 与 E2E 未运行 | 流程9 owner；无正式 source relation 的历史入口 `BLOCKED_LEGACY` |
| 10 | 所有合法放行入口共用材料硬门禁，CAS 唯一写 `RELEASED`，随后产生完整追溯 | `7f3547c17` 在基线祖先链；任务报告记录 focused 47/47、扩展终态 49/49，package PASS | 定向证据 PASS；权威适配器、迁移、真实放行 E2E 未运行 | 流程10 owner；流程8 gate/流程4/6 receipt 适配缺失即阻断 |

矩阵结论：已有定向 PASS 仅覆盖各线程 task-owned 切片；流程4/6/8的实现级门禁仍未取得证据，流程7/10的当前定向结果不能替代全量回归。流程11观察员不修改业务 owner 代码、不运行全 MES，不把 runtime/worktree PASS 升级为全链路 PASS；全链路继续 No-Go。

## M29 建批权威凭证链验收准备（待流程6/9提交后执行）

- 新增 task-owned 清单：batch-authoritative-receipt-acceptance.md，覆盖 A1-A8 Given/When/Then、RED/GREEN/REGRESSION、验收数据前置和 F6/F9/F7/F4/F8-10/PAR-ENV 失败归属。
- 已核对流程6当前合同要求 active-order 仅消费 BACKFILL_SUCCEEDED receipt，independent 仅消费流程9服务端签发 receiptId；流程9验真必须重新读取租户范围内权威记录，拒绝 nested receipt、字段/hash 篡改、过期、撤销和跨租户请求。
- RED/GREEN 命令已准备，但流程6/9提交后的干净 integration worktree、真实测试租户、正式来源关系和可写测试数据尚未就绪；本轮不运行建批回归，不修改业务代码。
- 当前状态：验收计划 READY，实际建批 GREEN/REGRESSION 为 NOT RUN；receipt service/SQL 单测通过不能替代真实建批持久化和流程7消费证据。

## M30 当前主线统一验收（2026-08-25）

- Baseline：int_main HEAD 27386bbc483d7b9b2dde02e905b926825363c875；未修改流程4/6/7/8/10业务代码，未整体提交并行 dirty 文件。
- RED：bundled Maven -o -pl yudao-module-mes -am -Dmaven.test.skip=true compile 进入24模块、编译2929个MES源码后失败；首要错误为当前 system target 缺 RoleMapper/AdminUserMapper/RoleConfigPackageService，及 MesProEdhrWorkTask* getter/VO 不匹配。未进入Surefire，归属 PAR/ENV/并行基线 owner。
- GREEN：Flow11 runner 12 场景 PASS；pytest 12 passed；runtime guard 8081/48081 PASS。
- RED（工具前置）：py_compile 因 [Errno 13] Permission denied 写入 script/__pycache__ 失败，不能写成 PASS。
- REGRESSION：现有 MesProEdhrFourMaterialGateReleaseContractTest XML（2026-08-25 11:11）为1 failure；everyReleaseEntryUsesSharedServerGate 期望 true、实际 false。因本轮compile未到Surefire，此artifact仅作待重跑证据。
- 矩阵：流程4 receipt 37/37为历史slice；流程6建批39/39为历史slice；流程7 trace 29/29为历史slice；流程8 shared gate BLOCKED；流程10 47/47/49/49为历史slice。当前完成-建批-Tx-C-四材料-finalize-追溯链未实证。
