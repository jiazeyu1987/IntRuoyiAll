# 统一 GxP 审计追踪后端与 API 设计

## Purpose and Scope

定义统一审计内核、领域接入边界、只读查询 API、周期审查 API、错误模型、事务和幂等规则。统一接口是后端内部审计写入契约，不代理或统一现有业务 Controller。

## Evidence Reviewed

- `OperateLogDO`：通用操作日志以自由文本 `action` 和 JSON `extra` 为主。
- `YudaoOperateLogConfiguration`：现有 `@LogRecord` 能力已启用但依赖业务选择性标注。
- `MesProBatchRecordExecutionFieldAuditServiceImpl`：eDHR 已有字段前后值、签名和 hash 链。
- `SignatureGovernanceRecordMapper.xml`：DCC、eDHR、Showroom、BPM 已有统一签名投影。
- `SignatureGovernanceReviewServiceImpl`：已有审查规则计算，但当前未形成持久化审查生命周期。
- `SignatureGovernanceRetentionServiceImpl`：缺少真实留存验证源时会失败关闭。
- `infra_release_migration`：Migration 已保存文件 hash、环境、状态和操作编号。
- `docs/changes/20260907-gxp-audit-trail-review-remediation.md`：独立复审要求补齐完整法规归档、自动周期审查、可执行覆盖发现、特权防篡改、状态信封和系统变更清单。

## Modules

- `yudao-framework-gxp-audit`：纯契约、规范化、hash 和通用错误定义；不得依赖具体业务模块。
- `yudao-module-system/gxpaudit`：统一账本、策略、查询、审查、清单和归档回执服务。
- 领域适配器：保留在 DCC、MES、BPM、Showroom、System、Infra、MDM 等所属模块中，负责产生真实 before/after 和证据引用。
- `yudao-server`：装配配置、权限和运行监控，不承担领域差异推断。

依赖方向为“领域模块 -> 公共审计契约”；审计查询模块通过稳定证据引用读取允许展示的摘要，不反向调用领域写服务。

## API Contracts

### Internal Write Contract

```java
public interface GxpAuditTrailService {
    GxpAuditAppendResult append(GxpAuditEventCommand command);
}
```

`GxpAuditEventCommand` 必填字段：`tenantId`、`operationId`、`domainCode`、`subjectType`、`subjectId`、`subjectVersion`、`actionType`、`resultType`、`reason`、`beforeState`、`afterState`、`diff`、`requestId`、`traceId`、`idempotencyKey`、`source`、`policyVersion`、`retentionClass`。签名型策略额外要求 `signatureRef`；业务确有独立业务发生时间时可提交 `businessOccurredAt`，但它不替代正式审计时间。

`beforeState/afterState` 使用统一状态信封：

```json
{
  "state": "ABSENT|PRESENT|VOIDED|REDACTED",
  "schemaVersion": "mes.route.v1",
  "data": {}
}
```

- CREATE：`ABSENT -> PRESENT`。
- UPDATE/CORRECT：`PRESENT -> PRESENT`。
- DELETE：`PRESENT -> ABSENT`，必须在删除前冻结完整受控快照。
- VOID/OBSOLETE：`PRESENT -> VOIDED`，保留作废后状态和原记录身份。
- `REDACTED` 必须包含脱敏策略及原值 hash；必需状态不允许 `null`、`UNAVAILABLE` 或省略 `data`。

调用者不能传入 `eventId`、`previousHash`、`eventHash`、正式操作人身份或正式服务器时间；这些由审计内核从安全上下文和受控时间源生成。Job、发布脚本和集成账号使用明确的 `SYSTEM_ACTOR` 身份，不允许空操作人。

### Read-Only Event APIs

- `GET /admin-api/system/gxp-audit-event/page`
  - 权限：`system:gxp-audit:query`
  - 条件：租户、领域、对象类型/编号、动作、结果、操作人、签名、时间范围、完整性状态。
- `GET /admin-api/system/gxp-audit-event/get?id={id}`
  - 返回标准事件、字段差异、证据引用、签名摘要、时间证据和 hash 状态。
- `POST /admin-api/system/gxp-audit-event/verify`
  - 权限：`system:gxp-audit:verify`
  - 请求只允许明确事件范围；返回验证水位、首个异常位置和清单状态。
- `POST /admin-api/system/gxp-audit-event/export`
  - 权限：`system:gxp-audit:export`
  - 生成固定范围证据包；缺少必需证据时返回失败而不是空包。

不存在事件更新、删除、补写或“标记为有效”接口。

### Periodic Review APIs

- `GET /admin-api/system/gxp-audit-review-schedule/current`：返回已批准计划、规则版本、上次/下次应执行时间和未完成周期。
- `GET /admin-api/system/gxp-audit-review-run/page`：返回每次自动触发、补扫、失败、重试和逾期记录。
- `POST /admin-api/system/gxp-audit-review/create`：按批准规则冻结事件范围和策略版本。
- `GET /admin-api/system/gxp-audit-review/page`、`get`：查询批次、发现项和整改。
- `POST /admin-api/system/gxp-audit-review/finding/remediate`：提交整改证据，不改写原事件。
- `POST /admin-api/system/gxp-audit-review/sign`：质量电子签名绑定冻结快照 hash。
- `POST /admin-api/system/gxp-audit-review/close`：只有签名有效且必需整改全部关闭时成功。

自动调度按 `tenantId + ruleVersion + periodCode` 唯一创建批次。系统恢复后必须补扫所有到期但没有运行记录的周期；重复触发返回相同批次，不能跳过漏期或重复抽样。每次运行保存计划时间、触发时间、覆盖水位、总体、抽样算法/种子、样本、排除依据、结果、错误和告警事件。关键事件按策略全量审查，只有批准为风险抽样的类别才允许抽样。

### Archive and Recovery APIs

- `GET /admin-api/system/gxp-audit-archive-package/page`：查询归档包范围、事件数、包 hash、主副本和保存状态。
- `POST /admin-api/system/gxp-audit-archive-package/verify`：服务端读取指定对象版本，验证完整内容、清单、retainUntil、legal hold 和副本。
- `POST /admin-api/system/gxp-audit-recovery-rehearsal/create`：创建独立恢复演练任务；目标环境必须是已批准的隔离恢复环境。
- `GET /admin-api/system/gxp-audit-recovery-rehearsal/get`：返回恢复输入、事件/证据/签名数量、hash、查询和导出结果。

法规归档包是自包含对象，固定包含 `events.ndjson`、`evidence-links.ndjson`、签名证据、策略版本、可信时间证据、必需领域证据、`manifest.json` 和 `SHA256SUMS`。只保存 manifest、hash、数据库备份引用或对象路径不得返回 `ARCHIVED`。

### Coverage and System Change APIs

- `GET /admin-api/system/gxp-audit-coverage/current`：返回登记表版本、实际写入口清单、已登记项、有效不适用项、缺口和报告 hash。
- `GET /admin-api/system/gxp-system-change/get?releaseTag=`：返回 commit、产物/镜像、OpenAPI、Migration、配置、批准和回滚的可重算清单。

覆盖登记的权威源为 `config/gxp-audit-policy.yaml`，schema 必须包含 `operationId/sourceType/sourceLocator/domain/subjectType/actionType/reasonPolicy/signaturePolicy/statePolicy/retentionClass/testIds/owner/applicability`。`applicability=NOT_APPLICABLE` 时还必须包含批准人、批准引用、理由和有效期；它不能用来豁免已确定的 GxP 操作。

最小登记项示例：

```yaml
schemaVersion: gxp-audit-policy.v1
policyVersion: 2026-09-approved-01
operations:
  - operationId: mes.route.update
    sourceType: SERVICE_METHOD
    sourceLocator: cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteServiceImpl#updateRoute
    domain: MES
    subjectType: MES_ROUTE
    actionType: UPDATE
    reasonPolicy: REQUIRED_CATEGORY_AND_TEXT
    signaturePolicy: NOT_REQUIRED
    statePolicy: PRESENT_TO_PRESENT
    retentionClass: GXP_MASTER_DATA
    testIds: [BDD-AT-01, BDD-AT-05, BDD-AT-15]
    owner: mes-owner
    applicability: GXP
```

schema 校验必须拒绝未知字段、重复 `operationId`、不存在的源码定位、空 owner、未知策略枚举、空测试集合和失效不适用批准。源码定位在重命名后不能静默匹配旧入口；实际入口缺失和登记项悬空均为覆盖缺口。

编译期和 CI 生成实际写入口清单，至少发现 Controller、注册领域写服务、Job、消息消费者、导入、外部同步、Migration 和运维脚本。架构测试禁止 Controller/Job/消费者绕过登记服务直接调用变更 Mapper。策略和实际清单必须双向一致并生成 hash 固定覆盖报告。

发现机制采用多层交叉检查：带 `@GxpWriteOperation(operationId)` 的领域写方法由注解处理器登记；ArchUnit 禁止 Controller、Job、消费者和导入器直接依赖变更 Mapper；SQL 由 `release-migration` 元数据和脚本目录扫描登记；PowerShell/运维入口由受控命令清单登记。扫描器无法解析的动态 SQL 或反射写入必须作为显式 blocker，不允许自动标记不适用。

### Governance Status APIs

- `GET /admin-api/system/gxp-audit-governance/status`：返回覆盖、时间、未封存水位、清单、完整归档包、主副本、备份恢复、特权审计和周期审查状态；任一证据缺失显示 `BLOCKED/FAIL`，不得默认 `PASS`。
- `GET /admin-api/system/gxp-audit-policy/current`：返回当前策略版本和批准引用，只读。

## Error Model

- `GXP_AUDIT_POLICY_NOT_FOUND`：动作未登记或策略版本不可用。
- `GXP_AUDIT_REASON_REQUIRED`：受控动作缺少原因。
- `GXP_AUDIT_BEFORE_AFTER_REQUIRED`：缺少可证明的前后快照。
- `GXP_AUDIT_SIGNATURE_REQUIRED`：需要签名但未提供有效签名关联。
- `GXP_AUDIT_ACTOR_MISMATCH`：安全上下文与请求声明不一致。
- `GXP_AUDIT_APPEND_FAILED`：事件未写入，业务事务必须回滚。
- `GXP_AUDIT_IDEMPOTENCY_CONFLICT`：同一幂等键的规范载荷不同。
- `GXP_AUDIT_CHAIN_CONFLICT`：并发链头或对象版本冲突。
- `GXP_AUDIT_INTEGRITY_FAILED`：事件链、证据或清单校验失败。
- `GXP_AUDIT_RETENTION_BLOCKED`：保存策略、WORM 或回执不完整。
- `GXP_AUDIT_TRUSTED_TIME_BLOCKED`：可信时间证据缺失、过期或偏差超限。
- `GXP_AUDIT_REVIEW_NOT_CLOSABLE`：缺少签名或存在开放整改。
- `GXP_AUDIT_STATE_ENVELOPE_INVALID`：动作与前后状态信封不匹配或必需数据缺失。
- `GXP_AUDIT_ARCHIVE_PACKAGE_INCOMPLETE`：归档包缺少事件、证据、签名、时间或清单内容。
- `GXP_AUDIT_REVIEW_PERIOD_MISSING`：已到期周期没有唯一运行记录或批次。
- `GXP_AUDIT_COVERAGE_GAP`：实际写入口与策略登记表不一致。
- `GXP_AUDIT_PRIVILEGED_AUDIT_GAP`：特权审计、外送或未封存水位证据异常。
- `GXP_SYSTEM_CHANGE_MANIFEST_INCOMPLETE`：系统变更缺少前后基线、diff、批准或结果。

错误必须包含稳定业务对象身份和失败阶段，不记录密码、令牌或未脱敏载荷。

## Transactions and Idempotency

- 成功业务状态与成功审计事件使用同一个 Spring 事务管理器、同一物理数据库事务；禁止 `REQUIRES_NEW`、异步队列或 after-commit 补记成功事件。
- 在锁定业务对象并读取 before 后执行更新，再读取/构造 after，最后 append；事务提交前校验事件 hash 和业务版本。
- `tenant_id + source_module + idempotency_key` 唯一。相同键和相同规范载荷返回原事件；相同键不同载荷报冲突。
- 单对象链使用 `tenant + subjectType + subjectId` 锁定链头；每日租户清单覆盖所有事件，防止整条对象链被删除后无法发现。
- `recordedAtUtc` 由数据库或同一受控服务器时间源生成并加入 event hash；`occurredAtUtc` 表示正式业务动作时间，两者不得由客户端覆盖。事务身份或等价提交关联必须写入事件，支持证明事件与业务写入属于同一事务。
- 事件和全部必需证据链接在同一事务内形成冻结集合后计算 event hash；提交后不得增加、替换或删除必需证据链接，更正只能新增更正事件。
- 业务失败事件可以在独立事务保存，但必须明确 `resultType=FAILED`，且不能与成功事件或业务成功混淆。
- 跨数据库或外部平台动作必须先在开发计划中定义一致性协议；没有可证明协议时该动作保持阻塞，不自动退化为访问日志。

## Open Questions

- 哪些失败尝试属于法规审计，哪些只进入安全/技术日志。
- 超大字段、附件和二进制内容采用完整快照、结构化摘要还是 hash 证据。
- 跨租户质量审查是否允许以及对应的独立授权模型。
- 具体数据库特权审计产品和外送载体由安全/运维选型，但必须满足不可由业务应用或被审 DBA 同时改写。

## Design Blockers

- 缺少经批准的 GxP 操作登记表和保存期限矩阵。
- 缺少正式可信时间、WORM 和恢复环境证据。
- 任何领域无法提供稳定对象身份、真实 before/after 或唯一签名关联时不得接入并宣称满足。
- 数据库特权审计无法覆盖 DML、DDL、授权和关闭审计，或外送保护边界不独立时不得放行。
