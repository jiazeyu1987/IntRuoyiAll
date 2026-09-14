# 流程修复 10 开发方案：最终状态与追溯出口

## 目标态与范围

本任务实现放行完成后的最终状态闭环和追溯出口。流程 6 负责三类回填成功后的批次执行创建/复用；流程 10 只消费流程 6 已返回的 batchExecutionId。所有放行入口（活跃订单放行、排产放行、PQC 独立放行、人工放行）必须收敛到同一个 finalizeRelease 命令。四份正式材料由流程 8 统一硬门禁后，单一并发胜者只写入唯一放行决策；批次、订单、工单的后续状态通过各自 owner 的受控接口或事件收敛。领料单保持 ERP 正式状态，只保存正式来源引用与快照。活跃订单完成不等于已经放行。

## 正式规则

- docs/backend-development.md 的“活跃订单申请放行资料必须只使用正式来源”要求双进度 100% 且点击完成后，才在同一业务节点回填批记录、过程检验单及实际损耗单；三类回填成功后才创建或复用批次执行。
- 批次执行创建后必须上传来料检报告、灭菌报告、成品检报告、成品检记录四个独立材料；流程 8 统一 gate 通过后才能进入流程 10 最终放行。历史仅有三份材料的记录属于迁移阻断；旧可选开关属于实现 blocker，不得成为当前可配置放行条件。
- 一线生产/PQC 签名和两组长复核只形成来源事实；终态待办必须清理，不得在 CLOSED、ARCHIVED、REJECTED、VOIDED 对象上残留可办入口。

## 当前代码事实

- MesProEdhrReleaseController.java:57 保留入口权限边界；终态写入统一委托 finalizeRelease，submitForApproval 仅准备审批，不直接写 RELEASED。
- MesProEdhrReleaseServiceImpl.java:260 的预检事务保存快照/hash、事件和审计；329、376、423、469 分别实现提交、批准、驳回、撤回事务；1573 另有 submitForApproval 提交入口。
- MesProEdhrReleaseTransactionMapper.java:62 的生产放行 CAS 只在 expectedVersion 匹配且原状态为 PENDING_APPROVAL 时，将放行事务更新为 RELEASED。
- MesProEdhrReleaseServiceImpl.java:1013 的批次副作用仍由批次 owner 负责；流程10通过受控 upstream state port 请求订单/工单收敛，不直接改写 ERP 领料单。
- MesProEdhrReleaseTransactionDO.java:33 及 release decision 记录补充来源关系、快照/hash、材料 manifest 和最终决策审计字段；active-order 与独立来源按 origin 条件化，不强制伪造不适用 ID。
- 原有直接 RELEASED/批次未关闭测试已由权威来源、四材料 gate、审批 owner 和唯一决策合同覆盖；定向回归证据见 execution-log.md。

## 根因

状态所有权分散在放行事务、审批待办和批次服务；入口各自做幂等与 CAS，但没有统一终态命令、上游状态传播和完整来源快照。直接放行测试固化了入口差异。历史“三份材料”表述及材料可选开关还可能把四材料硬门禁降级为配置项。

## 目标状态与所有者

状态码必须在实现前由领域所有者冻结，以下仅是设计语义，不把不存在的枚举冒充当前代码：

| 对象 | 目标语义 | 唯一状态所有者 |
|---|---|---|
| 放行申请/放行事务 | RELEASED、REJECTED、WITHDRAWN（允许状态按合同冻结） | 流程 10 唯一写最终 release decision/release transaction |
| 批次执行 provision | 创建/复用及 provision 状态 | 流程 6；流程 10 只消费 batchExecutionId |
| 四份材料 gate | 四份材料齐套和有效版本判定 | 流程 8 |
| 活跃订单完成/回填 receipt | 完成事件、双 100%、三类回填 receipt | 流程 4 |
| 条件损耗事实 | 实际损耗及损耗来源 | 流程 5 |
| 多入口前置 | MANUAL/SCHEDULED/PQC_INDEPENDENT 等入口前置 | 流程 9 |
| 生产/PQC 映射与 trace graph | 正式来源映射和追溯图 | 流程 7 |
| 活跃订单后续状态 | 受控关闭/释放命令或事件 | 活跃订单状态 owner |
| 生产工单后续状态 | 受控完成/关闭命令或事件 | 生产工单状态 owner |
| 领料单 | ERP 正式状态不被本系统擅自改写 | ERP/领料域 |

驳回不回滚已完成生产事实；撤回只撤申请且受冻结窗口约束；重复请求返回同一正式结果；并发只有一个正式终态胜者。终态对象必须清除可办理待办。

## 统一接口、幂等和并发

所有放行入口调用统一 finalizeRelease 命令，不得各自直接写终态。命令始终携带 tenantId、batchExecutionId、entryType、origin、sourceRelation、sourceSnapshotHash、materialGateReceipt、idempotencyKey、expectedVersions、actor 和 permission；activeOrderId、workOrderId、pickListId、completionBackfillReceipt 仅在对应来源要求时携带，releaseApplicationId 如存在只是后续放行关联。

前置条件按 origin/entryType 条件化校验：

1. 所有来源都必须消费流程 6 已存在的 batchExecutionId，具备流程 8 四材料 gate 通过回执、正式 source relation、来源快照/hash、权限和自身业务前置。
2. active-order 来源必须额外校验流程 1 pickListBindingId/sourceSnapshotHash、流程 4 completionBackfillReceipt（状态 BACKFILL_SUCCEEDED）、双进度 100%、完成事件、生产/PQC 签名及组长复核和三类回填；不得把这些字段伪造为独立来源必填。
3. MANUAL、SCHEDULED、PQC_INDEPENDENT 来源必须提供流程 6 冻结的 canonical IndependentBatchPrerequisiteReceipt，按其字段、签发者、有效期、撤销状态和 source relation 校验；不要求 activeOrderId、pickListId 或 completionBackfillReceipt。
4. 四材料 gate 必须是来料检报告、灭菌报告、成品检报告、成品检记录四份独立材料的当前有效版本、hash、上传/审核状态和 manifest；缺失、过期或类型不明即拒绝。
5. 申请未处于终态，所有 expectedVersions CAS 成功；相同幂等键同 payload 返回同一结果，不同 payload 返回冲突。

最终化必须产生唯一 release decision、放行交易终态、来源链快照和审计事件。批次/订单/工单后续状态必须通过各自 owner 的受控命令或事件收敛；跨服务不能同库原子时，必须采用明确重试上限、补偿状态、人工阻断和消费幂等的 outbox 合同，失败不得返回假成功。流程 10 不直接改写 ERP 领料单，也不未经各领域端口直接关闭活跃订单或生产工单。

## 数据、权限、快照和审计

不可变 release decision、来源链 manifest、四材料 manifest 以及各上游对象的 before/after 状态和 version 是逻辑合同；数据库字段、约束和索引由后续数据库任务设计，本任务不改表。审计必须保存 tenant、入口、actor、权限、幂等键、decision/event ID、completionEventId、sourceFactIds/version/hash、batch/work-order/active-order/pick-list IDs、材料 manifest hash、时间和失败原因。

创建/提交、复核、批准、驳回、撤回分别授权。最终化服务同时验证角色、对象归属和签名，不得以提交人等于审批人绕过审批。快照在最终化 CAS 前生成并校验，CAS 成功后封存；来源事实或材料版本变化时，旧快照失效并要求重新预检。

## 追溯出口

正式读取接口必须支持从 activeOrderId、workOrderId、pickListId、batchExecutionId、releaseApplicationId 或 releaseDecisionId 任一入口反查：生产/PQC/组长复核事实、损耗存在性、三类回填回执、四材料版本/hash、权限与签名结果、状态版本和放行决定。不得依赖可变名称、前端拼接或事后事件猜测成功。

## 跨流程接口契约

- 流程 1：输出 active-order 来源需要的 pickListBindingId/sourceSnapshotHash。
- 流程 4：输出 active-order 来源需要的 completionBackfillReceipt，且状态必须为 BACKFILL_SUCCEEDED，同时证明双 100% 和三类回填。
- 流程 5：输出实际损耗事实；零损耗不生成空损耗单。
- 流程 6：负责三类回填成功后的批次执行创建/复用，并冻结 canonical IndependentBatchPrerequisiteReceipt；流程 10 只消费 batchExecutionId。
- 流程 7：提供正式来源映射和 trace graph；流程 10 输出 RELEASE_DECISION link、放行 hash 和封存 manifest。
- 流程 8：提供四份材料 gate，通过后流程 10 才能最终放行。
- 流程 9：定义多入口前置和 entryType/origin 规则，不得把独立入口强行伪造为 active-order 来源。
- 流程 11：要求实现阶段提交实际 RED/GREEN/REGRESSION/迁移证据；其任务文档交付已完成，不作为当前 blocker。

## 迁移与回滚边界

历史批次缺少有效完成事件、三类回填关联、四材料 manifest、上游引用或版本/hash 时全部进入 migration blocker，不得自动删除、复用或标记已放行。先回填并校验快照，再切换唯一状态所有者；迁移必须可审计、可重试、幂等。

回滚只允许在新 release decision 和上游终态尚未写入前切换入口。一旦写入正式决定，禁止用代码回滚或直接编辑覆盖事实，只能执行有权限、带原因和审计的正式撤销/反向业务命令。本任务不执行 SQL、迁移、回滚、服务启停或数据修复。

## 修改边界

- 后续允许修改：统一最终化应用服务、状态所有者服务、正式读模型、权限/审计、必要 mapper 和对应测试；数据库改动须单独经过数据库规则与迁移评审。
- 后续禁止修改：领料单正式 ERP 事实、完成前来源事实、无损耗订单的空损耗单、其它入口的直写终态旁路。
- 本专项实际修改：流程10生产代码、对应测试、迁移脚本及本任务文档；代码已由 `7f3547c17` 融合到 `int_main`。

## 未解决 blocker

流程4/6/8权威持久化凭证适配器、审批中心权威上下文接入、生产迁移/历史回填、跨服务 outbox 投递和全链路真实 E2E 尚未完成；旧三材料历史记录迁移及旧可选开关清理仍是上线阻断。流程 7/8/9/11 文档已交付，不作为文档 blocker。

### 主流程冻结核验
流程10唯一拥有最终 RELEASED 和 release manifest/签名审计。独立批次无 activeOrderId 时必须返回 originType/凭证/来源快照/适用事实/四材料版本/hash/放行审计，并以 NOT_APPLICABLE+原因码表示不适用关系；应有关系缺失为 MISSING/BLOCKED。

## 启动阻断修复证据（2026-08-23）

运行时缺失 Bean 的根因是实现类依赖 @Service + @ConditionalOnMissingBean 的扫描时机，主应用启动时没有形成可注入的端口 Bean。MesReleaseAuthoritativeContextConfiguration 现在通过显式 @Configuration/@Bean 注册唯一 blocker 实现；真实流程4/6/8适配器接入后可由条件注册替换，当前缺失适配器仍抛结构化 blocker。

MesReleaseAuthoritativeContextConfigurationTest 的两个断言分别确认端口 Bean 恰好一个和缺失适配器时返回结构化 blocker；实际定向 suite 47/47 PASS，yudao-server package BUILD SUCCESS，48081 实际监听且 /actuator/health 为 UP。运行时 JAR 中配置类和 blocker 类与当前构建产物 SHA-256 一致。
