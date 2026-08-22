# 开发计划：全链路顺序、入口合同与历史迁移

## 1. 专项职责和目标态

流程修复 11 只负责 BDD/TDD、分层回归、历史迁移/回滚和 Go/No-Go 汇总，不拥有生产事实、完成回填、批次、材料或最终放行状态。全链路固定顺序如下：

1. 流程 1：生产组长加入活跃订单时绑定工单对应的正式领料单及稳定分录。
2. 流程 2/3：一线生产、一线 PQC 提交签名事实；各自组长只复核来源事实。
3. 生产和检验进度均为 100% 后，生产组长点击完成。
4. 流程 4 作为完成状态所有者，在同一 Tx-A 完成节点统一回填批记录、过程检验单和有实际损耗时的损耗单。
5. 流程 5 对每个工序形成 REQUIRED、NO_LOSS 或 BLOCKED；订单完成 receipt 记录 SUCCESS 或 NOT_REQUIRED，并保存 hasActualLoss、lossQuantity、lossReportStatus 和零损耗正式快照。
6. Tx-A 成功后流程 6 进入 BATCH_PROVISIONING；流程 9 仅校验排产/PQC/手工/独立入口凭证并适配流程 6，活跃订单直接消费流程 4 成功 receipt。
7. 流程 6 Tx-B 创建/复用批次后，流程 7 执行 Tx-C，先写入并校验 Origin/TraceLink 及工单、领料、三类回填到批次的映射；映射完成后流程 6 才能进入 BATCH_READY。
8. 仅 BATCH_READY 批次允许流程 8 上传并检查四份独立必填材料：来料检报告、灭菌报告、成品检报告、成品检记录；齐套后返回 MATERIALS_READY。
9. 流程 10 承载唯一最终 RELEASED 状态；流程 7 同时提供放行后追溯，流程 9 不写 receipt、批次、映射、材料或最终放行状态。

本线程只修改本任务目录 Markdown；不修改生产代码、数据库、迁移脚本、配置、服务、测试代码或运行态数据。

## 2. 当前代码事实与根因

- MesTeamLeaderActiveOrderReleaseGenerationService 当前 active-order/release/apply 路径仍生成放行申请/PQC_RELEASE 审批，未证明完成回执、三类回填、批次 provision、四材料 gate 和流程 10 最终化已按统一顺序串联。
- 旧批次/资料单测和真实流程脚本仍把批次 ID 当作写资料前置；该顺序必须改为消费流程 4 完成回执和流程 6 createOrReuse 结果。
- 当前放行实体/服务的部分入口仍可直接 submit/approve/release，流程 10 合同要求所有入口收敛到唯一 finalization 命令和 CAS 胜者。
- 现有四材料枚举/断言已在合同测试、阶段初始化测试、服务测试和真实流程中存在；它们必须保留，不得把成品检报告与成品检记录合并。
- 历史记录可能只有旧三项资料；这类记录只能进入历史迁移分类，不是当前流程的兼容成功条件。

根因是完成、批次、材料和放行状态所有者分散，入口各自推进状态，且旧测试将批次提前创建固化为隐含合同；流程 4/5/7/8/9/10 的最新合同已提供统一修复边界，不再把这些线程视为缺失文档。

## 3. 各流程职责和接口契约

| 流程 | 唯一职责 | 流程 11 的消费/验证要求 |
|---|---|---|
| 1 | 活跃订单绑定工单对应正式领料单、分录和版本 | 活跃订单入口必须消费 pickListBindingId/sourceSnapshotHash；独立入口不得伪造绑定 |
| 2 | 一线生产提交、生产组长复核事实 | 完成只消费已签名/复核快照，不提前物化资料 |
| 3 | 一线 PQC 提交、PQC 组长确认汇总事实 | 过程检验只消费 confirmed aggregate 和设备快照 |
| 4 | 双 100% 完成、三类回填和完成回执 | 输出 completionBackfillReceipt、三类结果、来源 hash/version |
| 5 | 逐工序损耗计划和 REQUIRED/NO_LOSS/BLOCKED 决策 | 订单 receipt 为 SUCCESS/NOT_REQUIRED，保存 hasActualLoss、lossQuantity、lossReportStatus 和零损耗快照；BLOCKED 不得驱动建批 |
| 6 | 回填成功后创建/复用批次执行并拥有 BatchProvisioningRecord | 只写 BATCH_PROVISIONING、BATCH_PROVISIONING_RETRYABLE、BATCH_PROVISIONING_BLOCKED、BATCH_READY；不可变 completionBackfillReceipt 或 IndependentBatchPrerequisiteReceipt 只作前置事实，BatchProvisioningState 与 batchExecutionId 可变且由流程 6 独占 |
| 7 | Tx-C 放行前 Origin/TraceLink 映射及放行后完整追溯 | 映射缺失返回 TRACE_MAPPING_BLOCKED，阻断材料和放行；不八页面、备注、审计事件或可变名称推断关系 |
| 8 | 四份材料上传、版本/hash/manifest 和硬门禁 | 四节点独立、缺一阻塞，成品检报告与成品检记录不可互代 |
| 9 | 多入口场景分流、凭证、幂等、追溯前置 | 活跃订单和独立入口不得混用凭证；合法独立入口不因无 activeOrderId 被一律拒绝 |
| 10 | 唯一最终放行状态、CAS、撤回/驳回和终态追溯 | 所有放行入口消费流程 8 gate；只有流程 10 写 RELEASED |
| 11 | BDD/TDD、回归、迁移/回滚、Go/No-Go | 不拥有上述业务状态，不创建绕过入口 |

## 4. 状态所有者和统一内部端口

| 状态 | 所有者 | 进入条件 |
|---|---|---|
| ACTIVE -> COMPLETION_READY -> COMPLETED | 流程 4 | 双进度 100%、正式工单/领料绑定、签名/PQC/路线来源完整 |
| BACKFILL_SUCCEEDED（仅成功 receipt）/ BACKFILL_ATOMIC_ROLLBACK（失败返回，不是 receipt 状态） | 流程 4 | Tx-A 成功才提交批记录、过程检验、适用损耗和不可变 receipt；任一校验或写入失败全部回滚，不提交 receipt，失败尝试单独审计 |
| BATCH_PROVISIONING -> BATCH_PROVISIONING_RETRYABLE / BATCH_PROVISIONING_BLOCKED -> BATCH_READY | 流程 6 | Tx-A 成功 receipt 或流程 9 校验通过的 IndependentBatchPrerequisiteReceipt；BATCH_READY 仅在流程 7 Tx-C 映射成功后写入 |
| Origin/TraceLink MAPPED / TRACE_MAPPING_BLOCKED | 流程 7 | 批次创建后先完成工单、领料、三类回填及独立来源映射；失败阻断流程 8/10 |
| 四个独立材料节点均为当前有效 COMPLETED，汇总 MATERIALS_READY | 流程 8 | 来料检报告、灭菌报告、成品检报告、成品检记录均已持久化，且每节点 version/file_hash/source_snapshot_hash 与 manifest 一致；有批准字段时还必须为 APPROVED；仅 UPLOADED 不满足放行 |
| PENDING_APPROVAL -> RELEASED/REJECTED/WITHDRAWN | 流程 10 | 流程 8 gate、授权、签名和 CAS 版本均通过 |

后续实现冻结五个内部端口：ActiveOrderCompletionPort.complete、BatchExecutionProvisionPort.provision、BatchOriginMappingPort.mapBeforeMaterials、MaterialManifestPort.upload/checkReady、ReleaseFinalizationPort.finalizeRelease。HTTP、排程、PQC/生产申请、批次详情和人工重试只能适配端口，不能自行改状态；流程 7 的 Tx-C 映射完成是流程 6 写 BATCH_READY 的必要条件。

所有命令带 entryType、正确凭证、idempotencyKey、expectedVersion 和规范化 payload hash。相同 key+相同 payload 返回原结果；相同 key+不同 payload 返回 IDEMPOTENCY_PAYLOAD_CONFLICT；版本冲突返回 VERSION_CONFLICT；未知提交结果先按 key 查询。Long ID 在 JSON 中使用字符串。

Tx-A 失败语义固定为：返回 BACKFILL_ATOMIC_ROLLBACK，不提交 completionBackfillReceipt，不产生 BACKFILL_FAILED receipt，也不允许流程 6 消费失败事实。可保留 CompletionBackfillFailureAttempt 失败尝试记录，但它不是 receipt、不是回填成功事实，不能驱动建批；流程 4 负责记录 activeOrderId、completionVersion、sourceSnapshotHash、idempotencyKey、payloadHash、稳定 errorCode、actor、occurredAt 和审计关联。用户可重新点击完成；只有 Tx-A 成功才提交 BACKFILL_SUCCEEDED receipt，之后流程 6 独占 BATCH_PROVISIONING、BATCH_PROVISIONING_RETRYABLE、BATCH_PROVISIONING_BLOCKED、BATCH_READY 等 BATCH_* 状态。

## 5. 合法批次创建入口

流程 9 是只读/校验型适配器，不写 receipt、批次、映射、材料或最终放行状态。入口差异是业务场景差异，不是绕过门禁的权限。活跃订单入口直接消费流程 4 成功 receipt；排产、PQC、手工、独立入口必须先经流程 9 校验对应凭证和 source relation，再调用流程 6；流程 6 成功后仍必须经流程 7 Tx-C 映射才能进入 BATCH_READY：

| 创建入口 | 必须凭证/前置 | 状态所有者和追溯 |
|---|---|---|
| 活跃订单完成 | 流程 4 completionBackfillReceipt、流程 1 领料绑定、双 100%、三类回填结果 | 流程 4 拥有完成，流程 6 拥有批次；receipt -> 订单/工单/领料/回填 -> batch |
| 排产完成联动 | 活跃订单消费 completion receipt；独立场景消费独立正式凭证 | 排产只拥有排产状态；schedule -> credential -> batch |
| PQC 批准联动 | 活跃订单仍需完成回执；独立场景需 IndependentBatchPrerequisiteReceipt 和正式 batchExecutionSourceRelation | PQC 只拥有质量事实，不替代完成/回填 |
| 页面手工/受控重试 | 选择已存在的正确凭证、原因、专用权限和 expectedVersion | 页面不拥有业务状态，审计记录 entryType/reason |
| 合法独立批次创建 | 后端签发的 IndependentBatchPrerequisiteReceipt、正式 batchExecutionSourceRelation、路线版本、来源对象和独立权限；可无 activeOrderId | 流程 9 只校验并转交，流程 6 拥有批次，流程 7 拥有映射；不得伪造活跃订单链 |

不同合法入口仅在 source relation、sourceContextHash、routeVersion、业务范围和复用规则完全一致时复用；否则创建独立批次。工单号、批次号、路线号只能查询候选，不能证明来源。历史无正式凭证/关系的批次返回 BLOCKED_LEGACY。

## 6. 合法放行入口

所有放行入口必须消费流程 8 同一四材料硬门禁和流程 10 唯一最终状态；缺少 activeOrderId 不得一律拒绝合法独立批次，但也不得绕过材料门禁：

| 放行入口 | 合法动作 | 最终状态 |
|---|---|---|
| 批次详情提交 | 提交申请/预检，携带 batch source relation、四材料 manifest、expectedVersion | 只能进入待审批，不得直接 RELEASED |
| PQC/生产申请或复核 | 形成申请事实和来源审计；不代替流程 10 | 只能在 MATERIALS_READY 后生成待放行申请 |
| 管理者代表批准 | 携带 releaseTransactionId、manifest hash、签名和 expectedVersion | 流程 10 唯一 CAS 写 RELEASED，并封存 manifest/link |
| 独立批次放行 | 使用独立 source relation、独立权限和四材料 manifest | 不要求 activeOrderId，但必须经过同一流程 8 gate 和流程 10 finalization |
| 驳回/撤回/受控重试 | 按流程 10 允许窗口和幂等合同执行 | 不回滚已完成生产事实；终态动作由流程 10 拥有 |

## 7. 正式来源、数据和追溯

- 批记录只取冻结工序逐工序正式 BATCH 绑定和 RECORD_CATEGORY_BATCH_RECORD。
- 过程检验只取已确认 PQC 汇总；设备取提交/汇总快照，不取最新配置。
- 损耗按流程 5 决策：正损耗才写损耗单；零损耗保存零损耗事实快照，不生成空单。
- 批次 Origin/TraceLink 保存 active/independent origin、凭证 ID、source object、领料分录、三类回填、四材料 manifest、hash、操作者、时间和版本。
- 流程 10 release decision 保存入口、actor、权限、completion/credential、batch、四材料 manifest、CAS 版本和审计 hash。
- 流程 7 必须在材料上传前写入并校验 Origin/TraceLink，覆盖工单、正式领料、三类回填到 batchExecutionId 的映射；缺失返回 TRACE_MAPPING_BLOCKED，流程 8/10 均不得继续。放行后流程 7 再展开完整追溯；releaseApplicationId 仅在流程 10 实际放行后作为 RELEASE_DECISION 关系追加。追溯可从 activeOrderId、workOrderId、pickListId、batchExecutionId、releaseApplicationId 或 releaseDecisionId 反查；独立入口从 source relation/batch/release 反查。任何来源缺失都返回稳定 blocker，不用前端拼接。

## 8. 历史迁移与回滚

只读分类：RECEIPT_BOUND_COMPLETE、PROVABLE_UNBOUND、INCOMPLETE_OR_AMBIGUOUS、BLOCKED_LEGACY、ALREADY_RELEASED_REVIEW_REQUIRED。历史仅有旧三项资料的记录一律归入 BLOCKED_LEGACY；缺第四节点、缺成功 BACKFILL_SUCCEEDED receipt、缺流程 7 映射或无法证明独立成品检记录的其它记录归入 INCOMPLETE_OR_AMBIGUOUS，不能作为当前四材料流程成功条件；已放行但映射/来源不完整归入 ALREADY_RELEASED_REVIEW_REQUIRED。禁止按名称、时间、最新配置猜测关系。

dry-run 输出 migrationBatchId、batchExecutionId、completion/independent credential、classification、sourceHash、fourNodeStatus、逐节点 material evidence、reasonCode、reviewer、reviewStatus；以迁移批号+业务 ID 保证唯一，重复业务 ID 立即失败。`build_dry_run_report` 只消费规范化输入，明确 `side_effects=[]` 和 `write_allowed=false`。仅对人工批准且关系可证明的记录写显式 Origin/TraceLink，写入后核对计数、唯一性、hash 和追溯链。

回填成功而批次创建失败时保留完成回执/独立凭证和失败原因，按同一 key 重试；已放行记录只能正式撤销/纠错，不能代码覆盖；迁移回滚只撤销本批新增关系，不删除原始事实。

## 8.1 流程 11 可独立交付实现

流程 11 的迁移实现为 `IntRuoyiBackend/script/flow_repair_11_migration.py`，只接受已规范化的历史记录并返回五类冻结枚举：`RECEIPT_BOUND_COMPLETE`、`PROVABLE_UNBOUND`、`INCOMPLETE_OR_AMBIGUOUS`、`BLOCKED_LEGACY`、`ALREADY_RELEASED_REVIEW_REQUIRED`。它不连接数据库、不执行 SQL、不猜测工单/领料/批次关系；失败尝试没有成功 receipt 时始终阻断。

`build_rollback_plan` 只生成计划对象：仅 `PROVABLE_UNBOUND` 且人工复核为 `APPROVED` 时允许后续实现线程写入新的 Origin/TraceLink；回滚范围固定为 `NEW_ORIGIN_TRACE_LINKS_ONLY`，不得删除原始生产或放行事实。旧三材料记录必须先归 `BLOCKED_LEGACY`，已放行但来源或映射不完整必须先归 `ALREADY_RELEASED_REVIEW_REQUIRED`。

`build_dry_run_report` 和 `run_flow_repair_11_contracts.py` 位于流程11脚本目录，合同测试实际覆盖 12 个 Given/When/Then 场景，包含四材料持久化证据、旧三材料阻断、缺第四节点、hash/version 冲突、独立凭证及正式来源关系、失败尝试无成功 receipt、已放行复核、批准/未批准回滚计划、分类计数和重复 ID 阻断。

## 9. Go/No-Go blocker

- 四份材料、流程 1-10 的状态 owner、接口、入口顺序、Tx-A/Tx-B/Tx-C、Origin/TraceLink、流程 8 gate 和流程 10 finalization 已在本任务及流程修复 1-10 文档冻结；当前 blocker 仅是生产代码、测试、真实 E2E 和迁移证据尚未完成。
- 必须实现并验证流程 5 逐工序 REQUIRED/NO_LOSS/BLOCKED、订单 receipt SUCCESS/NOT_REQUIRED 及 hasActualLoss/lossQuantity/lossReportStatus/零损耗快照，流程 6 的 BATCH_PROVISIONING/BATCH_PROVISIONING_RETRYABLE/BATCH_PROVISIONING_BLOCKED/BATCH_READY 状态、流程 7 TRACE_MAPPING_BLOCKED 与 BATCH_READY 前 Tx-C 映射、流程 9 IndependentBatchPrerequisiteReceipt 校验和流程 8/10 硬门禁。
- 需准备真实租户、角色、签名、正式工单/领料单、PQC 汇总和四份附件，才可运行写入 E2E。
- 当前代码仍有提前申请/先建批次旧顺序，必须由实现线程通过 RED/GREEN/REGRESSION 关闭。

在上述证据齐全前，流程 11 结论为 No-Go；流程 11 文档本身不宣称代码已合规。
