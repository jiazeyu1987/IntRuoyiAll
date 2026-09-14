# 开发计划：四份材料上传和放行门禁

## 目标态与修改边界

目标态冻结为两阶段流程：“活跃订单点击完成触发流程 4 拥有的单一回填事务（批记录、过程检验单、实际损耗单或 NO_LOSS 事实）且全部成功 -> 流程 6（经流程 9 合法入口）创建或复用批次 -> 流程 7 pre-release Origin/TraceLink 将生产工单、领料单、批记录、过程检验、适用损耗/NO_LOSS 事实映射到 batchExecution -> 本任务上传并校验四份永久必填材料、统一 gate 返回 MATERIALS_READY -> 流程 10 唯一最终放行 -> 流程 7 post-release 追溯”。流程 5 只提供损耗判定规则，不是独立回填节点；本任务不跨层接管前后状态。

## 交付性质

本文件只定义后续实现方案，不在本任务执行生产代码或数据库变更。实现必须先写测试，再按 BDD + Strict TDD 落地。

## 模块边界

1. `BatchExecutionReportNodeService`：管理四个冻结节点、负责人工作任务、节点状态和节点级完成。
2. `AttachmentEvidenceService`：预上传校验、文件元数据核对、SHA-256、版本链、替换和审计。
3. `FourMaterialReleaseGate`：读取四节点当前有效版本，验证批次上下文、状态和来源快照，返回可解释 blocker 或冻结 manifest。
4. `ReleaseEntryAdapter`：仅适配批次详情预检、提交放行、管理者代表批准及其它合法放行入口，全部调用统一 gate；不得接管或阻塞批次执行创建/复用。

流程修复 8 的唯一业务输入是流程修复 6 已创建或复用成功的 `batchExecutionId`。流程 4 的完成事务必须原子地产出批记录、过程检验单和实际损耗单或 `NO_LOSS` 事实；流程 5 只提供损耗判定规则。任一回填失败，完成事务整体失败且不得输出可建批 evidence；这些上游事实与流程修复 6/9 的建批合同属于上游状态，不由四材料门禁重复执行。

## 接口/数据设计

### 统一门禁命令

`checkForRelease(batchExecutionId, requestId, actorUserId)`：

- 校验批次执行存在且属于当前租户，并确认流程修复 7 的 pre-release Origin/TraceLink 已成功冻结；读取该批次内四个永久必填材料节点。不把活跃订单完成、三类回填或建批前置重新建模为本门禁条件，但缺少已完成的来源映射必须阻塞放行预检。
- 锁定四个节点和当前有效附件版本，禁止读取 `formBindings`、默认 `MAIN`、文件名或最新未完成记录作为替代。
- 返回 `gateStatus`、四节点状态、缺件清单、每项 `versionNo/sha256/attachmentHash/operatorId/operatedAt`、`manifestHash` 和 `blockerCodes`。

### 节点上传命令

`prepare(nodeType, batchTaskId, requestId, file metadata)` 必须返回上传 token 和正式文件证据；`complete(nodeType, batchTaskId, requestId, attachment manifest, sterilizationBatchNo)` 必须重新读取文件对象核验 URL、大小、类型、SHA-256、保留策略和 token。

### 替换命令

`replace(nodeType, batchTaskId, previousVersionNo, newAttachment, reason, requestId)` 只能在节点未被放行冻结或已有变更流程允许时执行；新版本成功完成前，旧版本不得被标记为当前有效；新版本完成后，批次级门禁必须重新变为 `MATERIALS_RECHECK_REQUIRED`。

### 流程修复 7 映射输入输出

- `preReleaseOriginLink(batchExecutionId, sourceSnapshotHash, requestId)`：读取流程 4 完成事务的三类回填 evidence（流程 5 提供损耗判定）、流程 6 的批次创建结果和流程 9 的正式入口上下文，写入并冻结生产工单、领料单、批记录、过程检验、适用损耗或 `NO_LOSS` 事实到 batchExecution 的 Origin/TraceLink。成功输出 `originLinkId`、`traceLinkHash`、`sourceSnapshotHash`；任一来源缺失、替代字段、hash 变化或重复参数冲突时返回 blocker，不生成部分映射。
- `postReleaseTrace(releaseId, manifestHash, requestId)`：由流程 7 在流程 10 唯一最终放行成功后消费最终放行记录和四材料 manifest，补齐放行后的全链路追溯；失败不得回写或伪造材料 gate 成功。

### 完成节点事务合同

活跃订单点击“完成”是唯一回填触发点，由流程修复 4 在一个业务事务内完成双进度 100% 校验、批记录回填、过程检验单回填和损耗分支：存在实际损耗时必须创建并回填损耗单；无实际损耗时禁止创建/写入损耗单，只写明确的 `NO_LOSS` 适用性事实。任一子步骤失败必须整体回滚或返回完成失败，且流程修复 6/9 不得创建或复用批次。

### 放行门禁输出

所有合法放行入口都传同一 `releaseGateRequest`，不能携带自定义 `skipMaterial`、`requiredCount` 或“已有历史附件”标记。门禁通过后冻结并返回四份当前材料 manifest，供流程修复 10 的最终放行命令使用；本任务不写签名、批准或最终放行状态。重复请求按 `(tenantId, businessScope, batchExecutionId, requestId/idempotencyKey)` 返回原结果，参数 hash 不一致则返回幂等冲突。

批次执行创建/复用接口不调用 `checkForRelease`。新建批次在四份材料为空时合法存在，并从材料状态 `MATERIALS_PENDING` 开始。

## 四节点状态模型

| 节点状态 | 含义 | 允许动作 | 是否满足放行 |
|---|---|---|---|
| `PENDING` | 无当前有效完成附件 | 上传 | 否 |
| `UPLOADING` | 已预登记，尚未完成 | 继续上传、取消预登记 | 否 |
| `COMPLETED` | 当前版本附件已核验并持久化 | 查看、按规则替换 | 是（仅该节点） |
| `REWORK_REQUIRED` | 被退回、替换中或旧版本失效 | 重新上传 | 否 |
| `BLOCKED` | 正式来源、权限、文件或批次上下文缺失 | 只读 blocker | 否 |

本任务拥有的批次材料状态仅为 `MATERIALS_PENDING`、`MATERIALS_READY`、`MATERIALS_RECHECK_REQUIRED`。任何节点版本变化、hash 不一致、文件对象被删除、路线/来源快照变化或节点回退都会使材料状态离开 `MATERIALS_READY`。`RELEASE_SUBMITTED`、`RELEASED` 等最终放行状态由流程修复 10 拥有，不在本任务状态机内。

## 附件版本/hash/并发规则

- 版本号按节点单调递增；同节点同 SHA-256 的同一请求只返回原成功结果，不产生重复版本。
- 同文件名不同内容不得覆盖；必须保留旧版本链和替换原因。
- 同节点并发完成采用行锁或等价条件更新：只有一个请求能提交当前版本，另一个返回 `ATTACHMENT_VERSION_CONFLICT`，不得静默成功。
- 放行预检读取四节点后生成 `manifestHash = SHA256(sorted(nodeType, versionNo, fileId, sha256, attachmentHash))`；提交放行时必须带回并重新比对。
- 上传预登记可产生 `UPLOADING` 记录，但不能参与放行；只有 `complete` 成功后才成为有效版本。
- 放行后替换必须新建变更/重开流程，历史 release transaction 保留原 manifest，不更新为新文件。

## 失败行为与错误码建议

`RELEASE_MATERIAL_GATE_REQUIRED`、`MATERIAL_NODE_MISSING`、`MATERIAL_UPLOAD_INCOMPLETE`、`MATERIAL_FILE_NOT_VERIFIED`、`MATERIAL_VERSION_STALE`、`MATERIAL_HASH_MISMATCH`、`MATERIAL_VERSION_CONFLICT`、`MATERIAL_MANIFEST_CHANGED`、`MATERIAL_SOURCE_SNAPSHOT_CHANGED`、`TRACE_MAPPING_BLOCKED`、`RELEASE_ENTRY_GATE_BYPASS`、`IDEMPOTENCY_CONFLICT`。流程 7 pre-release 映射缺失或 Tx-C 不可用时，对外传递 `TRACE_MAPPING_BLOCKED`；内部原因可附加但不得替代该稳定码。错误响应必须包含节点、当前状态、期望动作和可追溯对象 ID，不得返回默认成功。

## 跨线程接口契约

- 流程修复 4：拥有活跃订单“完成”唯一节点及原子回填事务，负责双 100%、批记录、过程检验单和实际损耗单/`NO_LOSS` 事实的整体成功或失败。
- 流程修复 5：只提供条件损耗判定与事实规则；不拆出独立回填节点，实际损耗分支必须在流程 4 完成事务内创建/回填损耗单，无损耗分支只允许 `NO_LOSS` 事实。
- 流程修复 6：只负责在流程修复 4 的单一完成事务（含流程修复 5 损耗判定分支）全部回填成功后创建/复用批次执行；成功后输出 `batchExecutionId`、`sourceSnapshotHash` 和上游 evidence IDs，不上传或推断四份材料。
- 流程修复 8（本任务）：接收已创建/复用且已完成流程 7 pre-release Origin/TraceLink 的批次执行，建立四节点上传、版本/hash、负责人和材料门禁；不重新创建活跃订单来源，也不代替 post-release 追溯。
- 流程修复 7：分两阶段负责映射与追溯：pre-release 在材料上传前冻结 Origin/TraceLink 并输出 `traceLinkHash`；post-release 在流程 10 成功后消费最终放行和材料 manifest 完成追溯。两阶段各自有 owner、输入、输出和失败记录。
- 流程修复 9：负责多种合法建批入口的前置、状态所有者、幂等和追溯合同；建批入口不得被四材料 gate 阻塞。其合法放行入口适配必须调用本任务 gate。
- 流程修复 10：负责放行角色、管理者代表候选、电子签名和最终放行状态；只有 gate 返回 `MATERIALS_READY` 才可进入最终流程，签名不能替代材料门禁。
- 流程修复 11：负责迁移、回滚、BDD/TDD/回归编排；旧数据没有四节点正式版本时必须标记 blocker 或受控迁移，不得用旧附件或最新记录补齐。

## 迁移/回滚边界

- 迁移前只读盘点每个批次执行四节点数量、状态、当前附件版本、SHA-256、负责人和历史放行结果。
- 仅能把可证明属于四节点且具备正式文件元数据的历史附件迁移为版本 1；无法证明来源、节点或 hash 的记录保持 `BLOCKED`，不得猜测。
- 迁移必须可按批次执行回滚到迁移前状态并保留审计；禁止直接修改已放行 manifest。
- 服务端固定要求四个材料节点；旧配置只能转换为只读兼容数据或明确的迁移 blocker，不能参与 gate 或放行决策。若运行时仍读取旧 required flag，则代码符合性为 FAIL。

## 实施顺序

1. 先写证明活跃订单完成事务原子回填：任一批记录、过程检验单或损耗分支失败均整体失败；无损耗不建损耗单，实际损耗必须建损耗单。
2. 写证明流程 6/9 可在材料为空时合法创建/复用批次的 RED 合同测试。
3. 写流程 7 pre-release Origin/TraceLink 完整映射、冻结 hash 和缺失来源阻塞的 RED 合同测试。
4. 写四节点状态、hash、幂等和统一放行 gate 的 RED 合同测试。
5. 实现节点上传完成与版本链，再实现批次级 gate，并替换全部放行入口调用。
6. 隔离旧配置的运行时影响并完成只读兼容/迁移设计，前端只展示固定必填状态、缺件、旧版本和 blocker。
7. 在流程 10 最终放行成功后实现流程 7 post-release 追溯，运行后端回归、前端静态合同和真实页面 E2E，流程修复 11 最后评估迁移总门禁。

## BDD、RED/GREEN/REGRESSION 索引

- BDD 场景、RED 预期失败、GREEN 通过条件和 REGRESSION 范围统一维护在 `test-plan.md` 与 `execution-log.md`。
- GREEN 必须同时证明完成事务三类回填原子成功（无损耗不生成损耗单，仅保存 `NO_LOSS` 事实；实际损耗生成并回填损耗单）、材料为空时建批成功、流程 7 pre-release Origin/TraceLink 已冻结、四节点齐套后 gate 返回 `MATERIALS_READY`、流程 10 才能继续，不能以 API-only 或隐藏开关作为证据。
- REGRESSION 必须由流程修复 11 编排，覆盖完成事务失败不建批、无损耗/实际损耗分支、流程 4（含流程 5 损耗规则）/6/7/9/10 的两阶段映射边界和迁移回滚。

### 主流程冻结核验
材料有效必须为 COMPLETED、文件持久化、元数据/SHA-256 校验、当前版本和 source hash 一致；已有批准字段时必须 APPROVED，无字段不得默认批准。变化后进入 MATERIALS_RECHECK_REQUIRED；流程 7 pre-release 映射缺失对外返回 TRACE_MAPPING_BLOCKED；服务端固定要求四节点，任何配置或前端开关不得改变必填性。

## 本任务状态

completed（仅表示文档设计与审计交付完成）。生产实现、配置迁移和测试不在本任务执行，均为 NOT RUN。
