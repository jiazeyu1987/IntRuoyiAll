# 电子签名长期架构设计审查

状态：APPROVED_DESIGN。主 Agent 完成两轮内容审查，五项架构要求全部通过设计审查。本文由主 Agent 维护，设计作者不拥有放行权限。

审查日期：2026-10-10。范围为架构与迁移设计；不包含代码实施、数据库写入、服务启停、构建、业务测试、E2E 或发布。本次只修改本任务新增的设计文件，不处理工作区其他改动。

## 审查对象与批准依据

- [长期架构设计](electronic-signature-architecture.md)。
- [ADR-0004](../adr/ADR-0004-electronic-signature-domain-boundaries.md)。
- 用户同意“模块化单体、独立签名领域、业务适配器”的方向，并明确要求子 Agent 先设计，主 Agent 在下面五项满足后放行。
- 已批准但尚未实施的 [20261006 修复合同](../../doc/tasks/20261006-electronic-signature-repair-design-review/repair-plan.md) 是衔接依据，不能当作当前已实现能力。

## 五项审查标准

| 标准 | 必须在设计中闭合的行为 | 当前结论 |
| --- | --- | --- |
| 1. 业务边界 | 原业务入口决定权限、前置条件、真实内容和版本；内核处理通用授权、认证编排与证据，不读取业务 Mapper | 通过（设计 §3、§4、§6） |
| 2. 单一事实源 | 新签只有一份不可变统一事实，业务投影明确关联统一 ID；撤销/替代追加，历史未关联不得推断补齐 | 通过（设计 §7、§9、§11） |
| 3. 稳定对象与公开契约 | 对象身份独立于内容；各业务使用公开查询、验证与流程契约，不读内部表、Mapper 或证据 JSON | 通过（设计 §4、§5、§10） |
| 4. 原子事务 | 业务状态、统一签名、关联和 GxP 审计同事务；认证失败独立提交且无嵌套持锁死锁；通知与治理投影可异步 | 通过（设计 §6、§9） |
| 5. 共享能力归属 | 通用签名授权、图片与治理脱离 DCC；历史图片冻结引用，图片要求按动作明确，不能补用现图 | 通过（设计 §3、§8） |

## 主 Agent 核对的实际业务节点

源码定位以当前工作区为准；下列问题是设计审查输入，不宣称已经修复。

| 业务节点 | 当前代码依据 | 设计必须处理的事实 |
| --- | --- | --- |
| 统一内核依赖 | `yudao-module-signature/pom.xml`；`ElectronicSignatureServiceImpl.sign/signAuthorized` | Maven 不依赖 MES/DCC/BPM，但直接使用 System 的 `service.gxpaudit`；运行期调用适配器不能变成编译期反向依赖 |
| DCC 入口到事务 | `detail/index.vue#submitDccApprovalAction` → `workflow.ts#approveControlledFileTask` → `DccControlledFileController.approveTask` → `DccControlledFileWorkflowServiceImpl.approveTask/validateTaskAction` → `DccSignatureVerificationServiceImpl.verifyPasswordAndCreateSignature` | 当前任务归属、流程状态、角色和文档证据检查必须留在 DCC；适配器仅解码 Base64，不能据此判定链路已解耦 |
| DCC 投影与响应 | `DccControlledFileSignatureDO`；`DccSignatureVerificationServiceImpl.persistSignatureEvidence` | 当前投影没有 unifiedSignatureId，接收的统一结果没有写入；对外 signatureId 是投影 ID，不能当作统一 ID |
| BPM 审批 | `approval-center/index.vue#submitReview` → `ApprovalCenterController.reviewTask` → `ApprovalCenterServiceImpl.reviewTask` → `ApprovalSignatureRecordServiceImpl.recordReviewSignature` → `ApprovalTaskProvider.review` | 审批可见性、角色、任务与结果归业务 provider；当前外层事务须纳入认证编排改造 |
| MES 生产提交 | `FrontlineFixedTemplatePanel.vue#handleProductionFormalSubmit` → `MesProFeedbackController.frontlineSubmit`（`mes:pro-feedback:create`）→ `MesProFrontlineFeedbackSubmitServiceImpl.submit` → `recordProductionSubmitSignature` → `createFrontlineFeedback` → `MesProcessPoolSubmitEventServiceImpl.createSubmitEvent` → 业务 GxP 与 handoff | 当前先签署再产生事件 ID；createSubmitEvent 又先验签，调整顺序必须消除循环。签名能力保留合法无订单路径，当前活跃订单入口自身的前置条件仍保留；设备操作者与被选实际员工分开 |
| MES PQC 放行 | `MesPqcProductionReleaseServiceImpl.approve` → 签署 → application CAS → task 完结 → 上市放行任务初始化 → GxP | 当前先拿 ledger 和业务锁再认证；改造须覆盖外层服务，不能只在内核增加 REQUIRES_NEW |
| MES 消费证据 | `MesProEdhrDeviationHandlingServiceImpl.effectiveSignatures`；`MesProEdhrNonconformanceReviewServiceImpl.requireDispositionAuditSignature`；`MesProProcessPoolTimelineReadMapper.xml`；`MesProductionSignatureEvidenceService` | 存在直接 Mapper、表查询、JSON 身份解析和创建审计查询；新契约必须保留领域匹配与审计要求，不能只返回泛化 VALID |
| 认证失败持久化 | `AdminUserServiceImpl.reauthenticateForSignature`；`ElectronicSignatureServiceImpl.signInternal`；`GxpAuditServiceImpl.acquireLedgerLock/append` | 内核当前先持 ledger 锁，凭据行在外层事务中更新；独立认证必须在业务锁前完成，失败事件持久化后才抛错 |
| 共享授权与图片 | `DccElectronicSignatureAuthorizationDO`；`DccElectronicSignatureImageDO`；`DccApprovalSignatureImageSnapshotProvider` | 当前均由 DCC 拥有，实体是 BaseDO，无显式租户；搬迁需确定原始身份归属、锁定状态与历史图片引用 |
| 治理页面与导出 | `SignatureGovernanceRecordsPane.vue` → `SignatureGovernanceController` → `SignatureGovernanceRecordServiceImpl` → `SignatureGovernanceRecordMapper.xml` | 列表联合多个领域表，缺统一事实源；导出存在现图查询。保留权限、来源 ID、历史 HMAC 和已批准带图 PDF 合同 |

## 复审记录

### 第一轮：不放行

初稿已经覆盖五项方向，但以下接口矛盾会让实施者重新猜测边界，已退回设计作者修订。

| 编号 | 发现与依据 | 必须闭合的设计 |
| --- | --- | --- |
| R1 | §4 的 ExpectedBinding 要求 adapter 重算业务 contentHash，§10 又将 contentHash 定义为含时间、身份和图片的完整 envelope 摘要 | 分离业务内容摘要与完整签署摘要，明确公开 DTO、严格业务比较及历史派生规则 |
| R2 | §4 要求 DCC adapter 使用 session 图片构造工件，但 §6 时序在 adapter 返回后才捕获图片 | 明确授权取数、公共冻结、领域工件完成、内核签署的无循环顺序；不能采两次时间/图 |
| R3 | §5.2 请求摘要包含 occurrenceId，生产的真实 submission/occurrence 却在业务事务内才生成 | 区分请求意图与服务器解析后的命令；原请求重放不依赖后来生成的 ID 或当前资料 |
| R4 | 凭据 provider 属 System API，但 SignerKey/认证 DTO 的包归属没有写明 | System SPI/DTO 不得引用 signature DTO，明确单向编译边界 |
| R5 | §10 evidenceHash 的“正式记录关联/创建审计期望”范围未固定，生成的签名/审计 ID 可能造成自引用 | 明确 hash 字段与 INSERT → 创建审计 → 关联顺序、V3 创建来源登记，不更新已签事实 |
| R6 | §8 在没有逐尝试关联证据时按 max 合并两个失败计数，不能证明两来源为同一尝试；失败窗口起点也不确定 | 禁止猜合并或清零；按确证事件迁移或明确保持原安全状态，证据不足的受影响身份阻断新签 |

同轮补充：身份 domain 必须是枚举而非十进制数字；分页水位不能在只有可变 upsert 行时被宣称为冻结集合的快照 token。ADR 与早期外部时间/HMAC/WORM 目标的关系需区分长期目标和 M5 当前核验范围，不虚构已实现能力或新增本轮未决环境前置。

这些均是设计内容阻塞，未通过不能以 ADR 章节完整或作者自评代替放行。

### 第二轮：六项关闭，五项通过

| 编号 | 修订后的具体合同 | 复审结论 |
| --- | --- | --- |
| R1 | §4 固定五键 BusinessContent、独立 businessContentHash；contentHash 继续保护完整签署 envelope。V1/V2 先完整验真再从固定历史 codec 派生业务比较值，标注 DERIVED_VERIFIED_LEGACY，旧摘要不改 | 关闭；业务可通过公开合同严格匹配，完整验真没有被业务比较替代 |
| R2 | §4、§6 先授权与冻结业务内容，内核一次捕获图并使用 session 时间，再调用 finalizeArtifacts；核对不变项后封装，领域工件不含后来生成的统一 ID/hash | 关闭；DCC 工件和公共证据使用同一身份、时间与图片，没有生成循环 |
| R3 | §5.2 固定 RequestIntentHash 与 ResolvedSigningCommandHash 两范围；所有后生成 ID/occurrence 排除重放输入，已有 task/node 定位明确保护。生产先插真实本域 submission，再签署、关联并完成，失败全回滚 | 关闭；重放和新提交可区分，无订单合法路径不依赖伪造订单或批执行 |
| R4 | §4 明确凭据 SPI/DTO 属 system.api.signaturecredential，签名 DTO 属 signature.api，审计合同属 system.api.gxpaudit；依赖单向 | 关闭；主线程补正 beginSigning：provider 只管凭据，signature 自有服务锁公共授权并判资格 |
| R5 | §10 固定完整 envelope 和 EvidenceHashInput，先算摘要、一次 INSERT、再以 signatureId/hash 创建唯一审计；auditId 不回写。V3 精确来源登记与 V1/V2 分离，业务审计来源不混用 | 关闭；原子事务内生成顺序可实施，无后生成 ID 自引用及未知创建来源 |
| R6 | §8 禁止按 max/求和猜合并；完整逐尝试证据才去重重放。证据不足保留各源可证明窗口/锁并阻断受影响身份，全部自然到期或确证审核后收口，旧 writer 停止 | 关闭；失败状态不会被静默清零、缩锁或改用旧凭据路径 |

同轮补充已关闭：domain 为两身份域枚举；治理采用明确的最终一致 cursor，水位不充当 MVCC，导出独立确定集合并复核当前权限；ADR 区分长期外部控制目标和 M5 当前能力。主线程还补正租户 ID：继承项目既有合法租户合同，不额外规定正数，不从缺失历史证据填默认租户。

复审覆盖设计与 ADR 全文及上述实际业务节点。现有实现缺陷没有在此被宣称修复；修复合同 M5 的状态、历史 V1/V2 格式、创建来源及带图 PDF 合同均保持。后续实际 schema、Spring/Flowable 同事务、MySQL 锁与触发器、各 consumer、迁移对账及真实页面验收是 §11、§12 的实施条件。

## 本次实际验证

- ADR 官方技能结构脚本：退出码 0，必需章节齐全。此结果仅证明结构。
- 三份文档严格 UTF-8 读取、无替换字符、无行尾空白、本地 Markdown 链接目标存在：通过。
- 源码静态核对：入口、权限、上游事务、签署服务、持久化和下游证据消费的依据见本记录和设计 §2；没有运行业务测试或 E2E。

## 最终放行

主 Agent 于 2026-10-10 放行本设计，五项均通过，无未关闭的设计内容阻塞。架构文件和 ADR 可作为后续实施合同；本次交付完成于文档设计与审查，未启动代码实施、数据库迁移或发布。实施需按用户后续授权及 §11、§12 验收，不因本次设计批准宣称系统已经解耦或具备运行证明。
