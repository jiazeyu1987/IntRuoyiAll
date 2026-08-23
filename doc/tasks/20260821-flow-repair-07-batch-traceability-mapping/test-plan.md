# 流程修复 7 测试与验收计划

## Latest Verification Boundary (2026-08-23)

Main-workspace Maven 3.9.16 compile, testCompile and the focused 29 tests passed (17 validator plus 12 service-contract, zero failures/errors/skips, `BUILD SUCCESS`). The verified contract includes required `originId`, persisted `RELEASE_DECISION` TraceLink lookup, canonical `(linkType, sourceObjectId)` identity with snapshot-hash mismatch blocking, explicit loss-fact mapping, and Tx-C success/failure transaction boundaries. Full regression, real database/runtime/permission verification, Flow8/Flow10 integration, and write-enabled E2E remain `NOT RUN`; this evidence cannot be used as release approval.

## Tx-C Producer and Event Test Boundary

BDD: Tx-C formal persistence boundary -> Given Flow6 has a successful provision audit containing formal Flow4/2/3/5 source evidence and Flow1 binding snapshot When `/traceability/tx-c` is invoked Then Flow7 rereads the same tenant-scoped sources, persists the immutable graph and outbox atomically, and publishes success after commit; When the second read differs Then the graph transaction rolls back and a separate failure transaction commits `TRACE_MAPPING_BLOCKED` with `SOURCE_CHANGED_AFTER_PRECHECK`.

RED: real Tx-C DB/outbox integration -> NOT RUN, the task has no writable verification database, Mapper runtime, or upstream immutable sourceEvidence fixture; static contract tests must not be counted as persistence proof.

GREEN: focused Flow7 contract suite -> PASS, 29 tests (17 validator + 12 service contract), 0 failures/errors/skips. The post-commit invocation did not skip testResources and copied MES test resources successfully. This is slice evidence only.

REGRESSION: Flow6 event consumer, real outbox delivery, cross-tenant permissions, Flow8 material gate, Flow10 release, full regression and write-enabled E2E -> NOT RUN.

## Purpose and Scope

覆盖批次来源图、四项资料齐套、放行前置、不可篡改、查询权限、历史迁移和跨线程合同。依据现有批次实体/Mapper/DTO、活跃订单申请实体、资料规划端口、正式来源规则、E2E 规则和经验索引 220、232、265、266、368。

## Feature Scenarios

BDD: 流程4完成与流程6建批分离 -> Given 流程2/3已提交并经各自组长复核，双进度均为 100%，流程1已冻结 pickListBindingId/sourceSnapshotHash，流程5已给出 NO_LOSS 或正式 lossRecord When 流程4唯一 owner 点击完成 Then Tx-A 统一回填三类适用表单并提交不可变 completionBackfillReceipt；When 流程6消费有效 receipt Then Tx-B 创建或复用 batchExecutionId 并返回 batch provision receipt；流程7不得在 Tx-A 中写批次映射。

BDD: 流程7仅在批次已 provision 后建立来源图 -> Given 流程6已返回 batchExecutionId、created/reused、batch provision receipt/status，且流程4 receipt、流程1/2/3/5正式来源 ID/版本/快照/hash 齐全 When 流程7消费后继事件或调用 captureBatchExecutionTrace Then 建立 Origin、TraceLink、Manifest；缺少 batchExecutionId 或流程6成功结果 Then 返回 BATCH_PROVISION_REQUIRED。

BDD: 无损耗不创建损耗单 -> Given 完成来源包证明没有实际损耗 When 创建批次来源图 Then 保存无损耗确认快照且不写损耗单收据、零损耗报告或虚构损耗链接。

BDD: 有损耗必须可追溯 -> Given 流程5判定 hasActualLoss=true 且存在正式 lossRecord When 流程4执行 Tx-A Then 必须有损耗单回填收据及一致 hash；若流程5为 NO_LOSS 则不得创建损耗单，否则整体阻断。

BDD: 领料单关系不可猜测 -> Given 存在多个审核领料单、缺稳定分录或物料/路线/DCC/工序不一致 When 请求完成 Then 返回 MATERIAL_ISSUE_SOURCE_INVALID，不写回填、批次或资料要求。

BDD: 回填成功但建批失败可重试 -> Given 流程4 Tx-A 已提交 completionBackfillReceipt 但流程6 Tx-B 创建/复用批次失败 When 重试流程6 Then 保留原 receipt，不重复回填；流程7在没有 batchExecutionId 前不得建立主体为 batchExecutionId 的映射。

BDD: 重复完成与映射幂等 -> Given 同一 completionTransactionId 已有流程4 receipt、流程6 batchExecutionId 和流程7来源图 When 重试相同来源 bundle Then 返回原 batchExecutionId 与 manifest；When 任一来源 hash 不同 Then 返回 TRACE_SOURCE_CONFLICT。

BDD: 映射缺失阻断追溯和放行 -> Given batchExecutionId 已创建但流程7 TraceLink/Manifest 未成功 When 查询完整追溯或流程10请求放行 Then 返回 TRACE_MAPPING_BLOCKED，不得显示完整链路或写 RELEASED。

BDD: 四份资料未齐不得放行 -> Given 批次来源完整但缺来料检、灭菌、成品检报告或成品检记录之一 When 管理者代表放行 Then 返回 dossier blocker 且不写放行决定。

BDD: 放行后完整追溯 -> Given 四份资料齐全且管理者代表有正式候选授权 When 放行成功 Then 批次和订单查询均显示订单、工单、领料分录、生产/PQC、损耗、回填、文件和放行 hash 链。

BDD: 放行申请后置关联 -> Given 活跃订单批次已经由完成交易和回填收据创建 When 后续确实产生 releaseApplicationId 并完成正式放行 Then 追加 RELEASE_DECISION 关系；When 建批时没有 releaseApplicationId Then 建批仍可成功。

BDD: 人工批次不得冒充订单放行 -> Given 人工打开批次只有工单、批号、路线 When 请求标注活跃订单完成来源 Then 拒绝并要求流程4/6正式凭证；When 显式 MANUAL 且满足流程9独立前置 Then 建立独立主来源，订单/领料/完成关系显式为 NOT_APPLICABLE，不显示虚假订单链。

BDD: 独立和排产入口保持自身追溯 -> Given 独立 PQC 或排产入口经流程9校验自己的 entryType、sourceCredential、路线快照和幂等键 When 流程6返回 batchExecutionId 后流程7建立映射 Then 使用对应 originType 建立来源图，不要求 activeOrderId 或 releaseApplicationId，也不把入口来源补算为活跃订单；不适用关系写 NOT_APPLICABLE。

## Failure and Boundary Scenarios

BDD: 旧批次安全阻断 -> Given 旧批次只有工单、批号、路线 When 重试或查看追溯 Then 返回 LEGACY_TRACEABILITY_MIGRATION_REQUIRED，不自动关联、复用或删除。

BDD: 无权用户无法越权查看或放行 -> Given 当前用户不属于来源对象范围或管理者代表候选 When 查询完整明细或放行 Then 返回权限 blocker，不泄露签名和附件。

## Strict TDD Sequence

| 顺序 | RED 命令及预期失败 | 最小实现 | GREEN 命令 | 回归 |
| --- | --- | --- | --- | --- |
| 1 | mvn -pl yudao-module-mes -Dtest=BatchTraceabilityOriginTest test；现有模型没有 origin/link | 来源实体、迁移、唯一键 | 同命令 PASS | 既有批次创建/打开 |
| 2 | mvn -pl yudao-module-mes -Dtest=ActiveOrderCompletionBackfillBoundaryTest test；流程4完成与流程6建批/流程7映射边界缺失或错误要求 releaseApplicationId | 流程4 Tx-A receipt、流程6 Tx-B provision、流程7后继映射命令与幂等 | 同命令 PASS | 双 100%、流程 1/2/3/4/5/6 正式来源 |
| 3 | mvn -pl yudao-module-mes -Dtest=MaterialIssueTraceabilityValidationTest test；多单/坏分录仍可通过 | 正式领料解析与 fail-fast | 同命令 PASS | 批记录领料来源 |
| 4 | mvn -pl yudao-module-mes -Dtest=LossTraceabilityTest test；零损耗仍生成收据 | 仅实际损耗写损耗收据 | 同命令 PASS | 资料回填 |
| 5 | mvn -pl yudao-module-mes -Dtest=BatchDossierFourDocumentsTest test；三个或合并文件可放行 | 四项 requirement 和文件 hash | 同命令 PASS | 上传/对象权限 |
| 6 | mvn -pl yudao-module-mes -Dtest=BatchTraceabilityQueryPermissionTest test；DTO/API 缺来源图 | 查询、对象权限、脱敏 | 同命令 PASS | 批次详情/工作台 |
| 7 | mvn -pl yudao-module-mes -Dtest=LegacyBatchTraceabilityMigrationTest test；旧批次按工单复用 | 历史 blocker、受审计迁移 | 同命令 PASS | 既有重试/复用 |
| 8 | mvn -pl yudao-module-mes -Dtest=BatchOriginEntryContractTest test；独立/手工/排产入口被迫要求 releaseApplicationId 或 activeOrderId，或未写 NOT_APPLICABLE | 流程9入口凭证/幂等合同与流程7独立 origin relation | 同命令 PASS | 多入口列表/详情筛选 |

实施时以仓库实际 Maven 模块和测试结构校正命令，但不可用 mock 成功替代正式仓储和事务测试。

## Regression and E2E Plan

- 后端：双 100%、负责人范围、生产/PQC/复核缺失、领料唯一性/分录、三类收据、损耗有/无、重试、来源冲突、四项资料逐一缺失、管理者代表权限、immutable manifest、历史 blocker。
- 职责回归：流程 2 负责生产提交/组长复核/驳回重提/分配事实；流程 3 负责 PQC 提交/组长确认/汇集事实；流程 4 是双 100% 点击完成及 Tx-A 三类适用回填的唯一 owner；流程 5 负责实际损耗判定、损耗单条件和 NO_LOSS；流程 6 负责 receipt 成功后的 batch provision；流程 7 负责已有 batchExecutionId 的来源映射；流程 8 负责四份材料及硬 gate；流程 9 负责多入口正式凭证/幂等前置；流程 10 唯一写最终 RELEASED；流程 11 负责 BDD/TDD、回归、迁移和总门禁。任一线程不得把 releaseApplicationId 变成建批必填。
- Schema 合同：唯一键、关系引用、link 类型、hash 非空、四项枚举、状态转换、Long ID JSON 字符串、稳定 blocker code。
- 前端静态合同：筛选、来源分组、四项独立显示、缺失 blocker、非授权不显示放行、禁止 formBindings/默认 MAIN 冒充批记录来源。
- Playwright：任务自有正式双 100% 活跃订单，经页面完成、上传四项材料、管理者代表放行，在订单和批次两端浏览追溯。API 仅作最终只读核验。

## Required Test Data and Blockers

需要任务自有测试租户/账号：生产组长、一线生产、一线 PQC、PQC 组长、管理者代表；一张确认工单、一张唯一审核领料单及稳定分录、已发布路线逐工序 BATCH 绑定、确认 PQC 汇集、流程4 completionBackfillReceipt、流程6 batch provision receipt、可验证三类表单、四份文件。分别准备有损耗和无损耗样本，并额外准备独立/手工/排产入口各自的 sourceCredential 与 NOT_APPLICABLE 关系。写入后用真实页面撤销或清理任务自有数据。

缺少正式数据、权限、菜单、文件能力或清理路径，写入型 E2E 必须 BLOCKED；禁止 SQL、API-only、mock 或历史批次替代。

## Evidence Log Template

BDD: 场景 -> Given ... When ... Then ...

RED: 命令 -> FAIL，预期原因

GREEN: 命令 -> PASS

REGRESSION: 命令 -> PASS，覆盖范围

BLOCKER: 编码 -> 缺失前置与影响
