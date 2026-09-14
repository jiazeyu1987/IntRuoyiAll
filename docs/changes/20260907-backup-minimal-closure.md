# 备份恢复最小闭环范围收缩

## Request Summary And Source

用户要求在交付时间紧张的前提下重新检查完整方案，删除首版不必要的设计，能简单实现的先简单实现；审查老师提出明确异议后再扩展。最终方案需保存为开发文档。

## Current Baseline Reviewed

- `doc/tasks/20260907-backup-full-incremental-recovery-plan/development-plan.md`
- `doc/tasks/20260907-backup-full-incremental-recovery-plan/test-plan.md`
- `doc/tasks/20260907-backup-full-incremental-recovery-plan/docs/system/`
- 当前 backup-ops 全量 dump、对象 inventory、DCC manifest、恢复演练与报告实现。
- 当前备份计划页面、DCC 签名证据导出和 eDHR evidence-package 基础。

## Classification

- 交付期限驱动的范围收缩。
- 备份/恢复运维能力与审查证据导出需求。
- 不改变“禁止 fallback、缺前置 fail fast、真实恢复才能宣称可恢复”的质量底线。

## Product Impact

首版用户可完成：

1. 配置并执行每周全量备份。
2. 配置并执行每日增量备份。
3. 查看全量基线及其连续增量链。
4. 在测试演练槽位恢复指定链并生成验证结果。
5. 点击“导出审查证据”下载当前备份链证据 ZIP。

首版不新增通用长期归档治理、legal hold/处置、归档主副本故障切换、secret readiness 页面或独立 archive reader。

## Frontend Operation Closure

首版前端只做“能被审查老师走通”的最小闭环，不做完整归档治理平台。闭环必须从运维备份页面开始，到证据 ZIP 下载结束，中间每一步都有明确状态、失败原因和后端证据来源。

1. 入口：在现有备份计划页面增加 `备份策略`、`备份链`、`恢复演练`、`审查证据` 四个区域；不新增独立大模块，避免首版导航和权限面扩大。
2. 策略配置：用户选择 `每周全量 + 每日增量`，填写维护窗口、NAS 目录、通知接收人、保存期限来源说明和质量批准引用；未填写保存期限来源或批准引用时只能保存为 `DRAFT`，不能启用。
3. 备份执行：用户可点击 `立即全量备份` 或 `立即增量备份`；按钮只提交执行请求，真实结论由后端任务回写，前端不得自行判定成功。
4. 链详情：页面按全量基线展示连续增量段，显示 `完整 / 断链 / 未校验 / 已演练` 状态；断链、hash 错误、gzip 损坏或缺对象载荷时用红色阻断标识，不提供“可恢复”结论。
5. 恢复演练：用户只能选择已校验的备份链和已批准的测试演练槽位，点击 `创建恢复演练`；production 目标不可选。演练完成前状态为 `待演练` 或 `演练中`，不能导出 PASS 结论。
6. 证据导出：用户点击 `导出审查证据` 后下载后端生成的 ZIP；ZIP 页面状态必须显示证据包 hash、生成时间、生成用户、脱敏结果、包含文件清单和整体结论 `PASS / FAIL / BLOCKED`。
7. 审计可见性：策略启用、手动执行、演练创建、证据导出都必须在页面展示操作人、操作时间、动作、对象、结果和失败原因；涉及启用策略和确认演练结论时要求电子签名或至少复用系统签名能力。
8. admin 处理：admin 仍可用于测试和只读验证，不作为正式职责分离样板；正式页面验收要预留 `备份操作员`、`质量审核员`、`系统管理员` 三类权限，但首版不强制先修复 admin 默认账号问题。

前端闭环验收标准：一个有权限的操作员能从策略草稿启用、执行全量、执行两段增量、查看链完整、发起测试恢复演练，到导出证据包；一个质量审核员能看到同一链路的保存期限来源、演练结果、证据清单和审计记录；任一关键前置缺失时页面显示后端阻断原因，而不是隐藏按钮或默认成功。

### Frontend State Machine And Page Data Contract

前端实现必须围绕后端状态机展示，不在浏览器内推断合规结论。页面只允许显示后端返回的 `draftStatus`、`precheckStatus`、`runStatus`、`chainStatus`、`rehearsalStatus`、`evidenceStatus` 和 `auditStatus`。

| 页面阶段 | 允许动作 | 成功后的下一状态 | 阻断状态与提示来源 |
| --- | --- | --- | --- |
| 策略草稿 `DRAFT` | 保存草稿、查看前置检查 | `DRAFT` / `PRECHECK_READY` | 后端 `precheck.items[]`，例如保存期限批准缺失、binlog 未启用、演练槽位缺失 |
| 策略可启用 `PRECHECK_READY` | 启用策略、重新检查 | `ENABLED` | 后端 `enable.blockers[]`，审计失败也必须阻断 |
| 策略已启用 `ENABLED` | 立即 FULL、立即 INCREMENTAL、查看链 | `RUNNING` / `CHAIN_PENDING_VERIFY` | 后端任务状态；前端不得把提交请求当成备份成功 |
| 链已校验 `CHAIN_VERIFIED` | 创建测试恢复演练、导出 BLOCKED 证据包 | `REHEARSAL_RUNNING` | 链断裂、checksum 失败、对象 tombstone 缺失时只能展示 `BROKEN/BLOCKED` |
| 演练完成 `REHEARSAL_PASS` | 导出 PASS 证据包、查看审计摘要 | `EVIDENCE_READY` | 演练失败或未演练时导出包整体结论必须是 `FAIL/BLOCKED` |
| 证据已生成 `EVIDENCE_READY` | 下载 ZIP、查看包级 hash 和清单 | 闭环完成 | 审计写入失败、脱敏失败或包 hash 不一致时不能下载 PASS 包 |

前端最小页面字段：

- 策略区：计划类型、维护窗口、NAS 根目录、FULL/INCREMENTAL 频率、保存期限来源、质量批准引用、通知接收人、启用状态、前置检查结果。
- 备份链区：chainId、FULL 点、增量序号、binlog 起止位点、对象 delta/tombstone 统计、checksum 状态、配置版本、链状态。
- 演练区：rehearsalId、目标测试槽位、恢复点、开始/结束时间、恢复耗时、数据核对、对象核对、结论和失败原因。
- 证据区：evidenceId、整体结论、ZIP hash、生成时间、生成用户、脱敏状态、包含文件清单、下载审计状态。
- 审计区：操作人、操作时间、动作、对象、结果、失败原因、电子签名或签名能力复用标识。

因此，按本文档开发可以形成前端闭环；但如果实现时省略状态机、只做按钮和文件列表，就只能算“有页面”，不能算“可审查闭环”。

## Design Impact

- 使用单一 `backup-ops.config.json v2`、现有 NAS 目录、manifest、checksums、operation report 和 rehearsal report，不建设新的备份数据库表。
- 不实现通用 writer registry。备份窗口停止当前项目后端服务，使嵌入式 HTTP 写入、Quartz 和应用内消费者一起停止；恢复后再启动。若存在外部直接 DB/MinIO writer，则首版阻断，不能假设不存在。
- 不实现隔离切换平台。首版恢复目标只允许现有测试演练槽位，禁止 production；通过真实演练证明备份链可恢复。
- 不实现证据快照数据库。导出时打包现有不可变 manifest、checksum、operation/rehearsal report 和当前调度状态，生成包级 `evidence-manifest.json` 与 SHA-256。
- 保留现有 eDHR/Object Lock 能力，不在本首版扩展到所有记录类别。

## Data Impact

- MySQL 全量继续使用逻辑 dump。
- MySQL 增量必须使用真实连续 binlog 段；不能用按更新时间查询、每日全量改名或差异 SQL 冒充增量。
- MinIO/DCC 增量复用现有 inventory-delta 与 tombstone。
- checksum 必须新增覆盖 MySQL dump/binlog、对象 inventory、增量对象载荷、DCC manifest 和运行配置。
- 不新增业务表；配置与 manifest schema 升级需要显式版本和迁移检查。

## Backend Data Flow

首版后端数据流以“配置文件 + manifest/report + 证据包”为权威，不新增业务表，但必须有稳定 schema 和可重复计算结果。

1. 策略保存：`backup-ops.config.json v2` 保存计划类型、维护窗口、FULL/INCREMENTAL 频率、NAS 根目录、测试演练槽位、通知接收人、保存期限来源、质量批准引用、配置版本和启用状态。
2. 启用校验：启用策略前后端必须校验 binlog 已开启、NAS 可写、演练槽位存在、无外部 MySQL/MinIO writer 证据已确认、保存期限来源和批准引用已填写；任一缺失返回明确错误码。
3. 全量备份：后端进入维护窗口后停止本项目后端写入，生成 MySQL dump、对象 inventory、DCC manifest、配置快照、checksum 和 `operation-report.json`；全部产物 hash 校验通过后才写入 FULL manifest。
4. 增量备份：后端读取上一个备份点后的连续 binlog 区间、对象 inventory-delta 和 tombstone，生成增量载荷、checksum 和 operation report；binlog 起止位点不连续时直接阻断。
5. 链校验：链详情接口从 FULL manifest 出发按增量序列重算 checksum、binlog 连续性、对象新增/修改/删除和配置版本；只要一个环节失败，整条链状态为 `BROKEN` 或 `BLOCKED`。
6. 恢复演练：恢复任务只能写入测试演练槽位；恢复前再次校验链完整，恢复后生成 `rehearsal-report.json`，记录恢复目标、恢复点、耗时、数据核对、对象核对、失败原因和执行人。
7. 证据包：证据 ZIP 由后端生成，至少包含 config snapshot、FULL/INCREMENTAL manifest、checksums、operation reports、rehearsal report、下载审计摘要、脱敏报告和 `evidence-manifest.json`；前端不得拼包。
8. 审计写入：策略启用、执行、校验、演练、导出均通过统一审计能力记录；审计失败时对应业务动作失败，不能生成“成功但无审计”的证据。
9. 保存期限表达：首版不声明“备份载荷已保留十年”。正确表达是：灾备备份链按运维 RTO/RPO 滚动保留；长期记录保存由 eDHR/DCC 记录归档和审计追踪归档承担。证据包必须暴露保存期限来源和批准引用，若来源未批准则整体结论为 `BLOCKED`。

后端闭环验收标准：同一条备份链的页面状态、下载证据 ZIP、operation report、rehearsal report 和 manifest hash 能互相印证；删除或篡改任一 dump/binlog/object/report 后，链校验和证据导出都不能返回 PASS。

### Backend Artifact Model And Data Lineage

首版不新增业务表，并不代表后端可以只用零散文件。后端必须把每个产物当成可追溯对象处理，所有页面状态均从下列表达式派生：

| 数据对象 | 最小字段 | 产生时机 | 下游用途 |
| --- | --- | --- | --- |
| `backup-ops.config.json v2` | planType、fullSchedule、incrementalSchedule、maintenanceWindow、nasRoot、rehearsalSlot、retentionSource、qualityApprovalRef、enabled、version | 保存草稿/启用策略 | 前置检查、计划注册、证据包配置快照 |
| `full-manifest.json` | chainId、fullPointId、createdAt、mysqlDumpPath/hash、objectInventoryPath/hash、dccManifestPath/hash、configSnapshotHash | FULL 成功后 | 链起点、恢复基线、证据包 |
| `incremental-manifest.json` | chainId、incrementalPointId、previousPointId、binlogStart、binlogEnd、binlogHash、objectDeltaHash、tombstoneHash | INCREMENTAL 成功后 | 连续性校验、增量恢复、断链判断 |
| `operation-report.json` | runId、runType、operator、startedAt、completedAt、precheckResult、artifactHashes、result、failureReason | 每次备份任务结束 | 页面任务状态、审计摘要、证据包 |
| `rehearsal-report.json` | rehearsalId、chainId、restorePointId、targetSlot、startedAt、completedAt、dbCheck、objectCheck、result、failureReason | 测试恢复演练结束 | 是否可恢复、3.3 证据、证据包结论 |
| `evidence-manifest.json` | evidenceId、chainId、includedFiles、packageHash、redactionResult、auditResult、overallVerdict | 导出审查证据时 | ZIP 内容索引、下载校验、审查结论 |
| unified audit record | userId、action、objectType、objectId、before/after、reason、signatureRef、result | 每个关键动作事务内 | 2.7/权限追溯、页面审计区、证据包摘要 |

数据流规则：

1. `config -> precheck -> run -> manifest/report -> chainVerify -> rehearsal -> evidence -> audit` 必须单向推进，后一步不能绕过前一步生成 PASS。
2. 页面列表可缓存查询结果，但缓存只能保存后端返回的状态；重新打开页面必须能从 manifest/report 重新计算同一结论。
3. `overallVerdict=PASS` 的必要条件是：策略启用前置通过、FULL 与至少两段 INCREMENTAL 连续、checksum 全部通过、测试槽位恢复演练通过、证据包脱敏通过、下载审计写入成功、保存期限来源和批准引用有效。
4. `overallVerdict=BLOCKED` 是合规保护状态，不是系统失败；当保存期限矩阵、质量批准、外部 writer 确认或长期归档证据缺失时必须显示 BLOCKED。
5. `overallVerdict=FAIL` 用于已执行但验证失败，例如 hash 不一致、binlog 断链、恢复演练失败、审计写入失败。

这样实现后，后端数据流符合检查清单要求的“可查阅、可恢复、可追溯”逻辑；如果实现成前端拼接 ZIP、前端计算 PASS、或缺少 manifest/hash/rehearsal/audit 的任一链路，则不符合本文档。

## API Impact

- 扩展现有 `/infra/backup-plan`，增加 FULL/INCREMENTAL 计划、立即执行类型、链详情和证据导出。
- 保留现有运行控制台演练能力；首版不开放 production restore-data。
- 新增一个后端生成的证据 ZIP 下载接口；前端不拼包、不生成结论。
- 不新增归档副本、secret readiness、legal hold 或处置 API。

### Minimal API Contract

- `GET /admin-api/infra/backup-plan/current`：返回当前策略、启用状态、保存期限来源、批准引用和前置检查状态。
- `POST /admin-api/infra/backup-plan/save-draft`：保存草稿，不启用计划；缺少批准引用时允许草稿但返回 `activatable=false`。
- `POST /admin-api/infra/backup-plan/enable`：启用计划；必须后端校验全部前置并写审计。
- `POST /admin-api/infra/backup-run/create`：创建 FULL 或 INCREMENTAL 备份任务；返回任务 ID，不返回成功结论。
- `GET /admin-api/infra/backup-chain/page`：分页查询全量基线和增量链状态。
- `GET /admin-api/infra/backup-chain/{chainId}`：返回链条 manifest、checksum、binlog 连续性、对象 delta 和演练状态。
- `POST /admin-api/infra/backup-rehearsal/create`：创建测试槽位恢复演练；目标环境只能是批准的 rehearsal slot。
- `GET /admin-api/infra/backup-rehearsal/{id}`：返回演练报告和 PASS / FAIL / BLOCKED。
- `POST /admin-api/infra/backup-evidence/export`：后端生成并返回证据 ZIP；未完成演练或保存期限来源未批准时仍可导出，但整体结论必须是 `BLOCKED`，不能伪装 PASS。

错误模型必须稳定：`BACKUP_RETENTION_APPROVAL_MISSING`、`BACKUP_BINLOG_NOT_ENABLED`、`BACKUP_EXTERNAL_WRITER_UNCONFIRMED`、`BACKUP_CHAIN_BROKEN`、`BACKUP_REHEARSAL_SLOT_MISSING`、`BACKUP_EVIDENCE_AUDIT_FAILED` 至少需要覆盖到前端阻断提示和测试断言。

## Test Impact

首版必测：

- FULL、FULL+I1、FULL+I1+I2 三个真实恢复结果。
- binlog 断链、载荷 hash 错、gzip 损坏时恢复前阻断。
- 停服备份期间写入口不可用，恢复服务后正常。
- 对象新增、修改、删除可按增量链恢复。
- 未演练点不能显示可恢复。
- 证据 ZIP 内容、脱敏、文件 hash、整体状态和下载审计。
- Playwright 只在后续当轮明确授权时走真实页面。

延期项只保留设计记录，不纳入首版完成门禁。

## Checklist Mapping And Reanalysis

按本文档开发后，结论不是“完全解决所有备份和长期归档问题”，而是“可以形成备份恢复最小闭环，并能解释 3.1、3.3、3.6；对 3.2 和 2.7 只能在保存期限矩阵与长期归档证据批准后声明符合”。

| 检查清单条款 | 修改后判断 | 文档覆盖点 | 仍需证据 |
| --- | --- | --- | --- |
| 2.7 审计追踪未备份或保存期不足 | 部分符合 | 证据包包含下载审计摘要；审计动作必须写入统一审计；长期保存由审计追踪归档承担，不由灾备链冒充 | 审计追踪归档包、WORM/对象锁回执、保存期限矩阵 |
| 3.1 备份频率不足或无定期备份机制 | 可符合 | 每周 FULL + 每日 INCREMENTAL，策略启用前置校验，手动执行和计划执行均有报告 | 真实计划任务运行记录、失败通知记录 |
| 3.2 备份保存期限不足或未与记录保存期一致 | 条件符合 / 未批准前 BLOCKED | 页面和证据包必须展示保存期限来源、质量批准引用；灾备滚动保留与法规长期归档分工明确 | 经质量批准的记录保存期限矩阵、归档对象 retain-until 或等价证据 |
| 3.3 备份不便于查阅或恢复测试缺失 | 可符合 | 链详情、恢复演练、证据 ZIP、manifest/hash/rehearsal report 闭环 | 独立测试环境 FULL+I1+I2 真实恢复演练报告 |
| 3.4 备份介质管理不规范 | 部分符合 | NAS 路径、manifest、checksum 和 evidence-manifest 提供唯一标识 | NAS 介质责任、访问控制、环境和借阅/变更记录 |
| 3.5 备份责任人不明确 | 部分符合 | 策略记录通知接收人、执行人、导出人和审计记录 | 正式 SOP 中质量、IT、运维责任人批准 |
| 3.6 增量备份与全量备份搭配不合理 | 可符合 | FULL + 连续 binlog INCREMENTAL，断链阻断，恢复演练覆盖 FULL+I1+I2 | 真实 binlog 位点连续性和对象 delta 恢复证据 |

### Frontend Closure Verdict

可以实现前端操作闭环，但前提是严格按本文的最小页面状态机实现：草稿策略不能直接启用，备份任务不能由前端判定成功，链详情必须能看到断链/未演练/已演练，证据导出必须显示后端整体结论。这样审查老师可以从页面完成“配置策略 -> 执行备份 -> 查看链 -> 发起恢复演练 -> 导出证据 -> 查看审计”的连续路径。

不满足闭环的情况：如果只做一个下载按钮、只展示最近备份文件列表、没有恢复演练状态、没有保存期限来源、没有审计记录，前端看起来能操作，但无法解释 3.2 和 3.3。

修改后再次分析：按当前文档开发，前端闭环是可实现的，因为每个页面动作都有明确前置、后置状态和后端错误来源；审查老师能在 UI 上看到为什么可以继续、为什么被阻断、证据包来自哪条备份链。闭环的最小风险是权限与电子签名复用：如果首版只用 admin 单账号演示，测试可走通，但正式职责分离证据仍不能宣称完全符合 1.1、1.3、1.6。

### Requirement Fit Verdict

符合“最小实现”的要求，但必须在文案上保持边界：首版解决的是备份频率、全量/增量搭配、恢复可验证和审查证据导出；长期保存期限一致性需要质量批准的记录保存矩阵和长期归档证据支撑。换句话说，3.2 的正确做法不是把所有备份文件都简单保留十年，而是把灾备备份和法规记录归档分开：灾备链保证可恢复，归档链保证记录在保存期内可查阅、不可篡改、可验证。

修改后再次分析：文档符合检查清单 3.1、3.3、3.6 的开发要求；对 3.2 和 2.7 采用“条件符合/BLOCKED”是正确做法，因为系统开发不能替代质量部门批准保存期限矩阵，也不能用灾备滚动备份冒充长期法规归档。只要实现时保持证据包 `overallVerdict` 的 PASS/BLOCKED 边界，就不会过度承诺。

### Backend Data Flow Verdict

后端数据流符合文档要求：配置、执行、manifest、checksum、演练报告、证据包和审计记录均由后端生成并互相校验，前端只消费状态和下载结果。关键合规点是 fail fast：binlog、外部 writer、NAS、演练槽位、审计写入、保存期限批准任一缺失，都只能返回 BLOCKED/FAIL，不能降级为默认 PASS。

修改后再次分析：后端数据流符合文档要求，原因是权威数据对象已经明确到 config、FULL manifest、INCREMENTAL manifest、operation report、rehearsal report、evidence manifest 和统一审计记录；这些对象能从同一 chainId 串起来，也能在文件被删除、篡改或断链时反向证明不能 PASS。首版仍需保留实现门禁：不新增业务表可以接受，但必须固定 JSON schema、hash 规则、错误码和审计事务边界。

### Minimum Implementation Boundary

首版最小实现到此为止：

- 必须做：策略草稿/启用、FULL/INCREMENTAL 执行、链校验、测试槽位恢复演练、证据 ZIP、审计记录、保存期限来源展示。
- 暂不做：production restore、通用 legal hold、到期处置、归档主副本故障切换、独立 archive reader、secret readiness 控制台。
- 不能省：真实 binlog 增量、hash 校验、恢复演练、证据包整体结论、缺前置 BLOCKED。

## Release Impact

- 首版必须先在独立测试环境完成全量加两段增量恢复演练。
- 未获得真实演练证据前不能发布为“可恢复”。
- 正式服任务注册、备用服和生产恢复仍需单独授权。
- 证据包只能陈述当前事实；存在缺项时整体结论为 BLOCKED/FAIL，不能输出默认 PASS。

## Operations Impact

- 首版备份采用明确维护窗口，允许短暂停止后端以换取一致性和更少实现复杂度。
- 必须确认没有后端之外的直接 MySQL/MinIO writer；无法确认则阻断备份。
- 运维需提供 binlog 配置、NAS 容量、任务主体、通知接收人和测试演练槽位。
- 证据包导出不包含备份载荷、业务附件或任何 secret。

## Decision

ACCEPT AND SPLIT。

- 接受最小闭环作为首版。
- 延期项保留在完整方案中作为后续增强，不与首版混合实施。
- 不简化数据完整性、恢复验证、权限、脱敏和 fail-fast 门禁。

## Required Approvals

- 用户已批准范围收缩。
- 测试环境真实备份/恢复、计划任务和 E2E 仍需后续当轮授权。
- 运维负责人需确认维护窗口、无外部 writer、binlog 与 NAS 前置。
- 质量负责人需确认首版审查证据包目录是否满足本次检查。

## Downstream Skill Reruns

- 更新最小开发文档与首版测试门禁。
- 重跑灾备文档、BDD/TDD、系统设计和合规结构验证。
- 收尾前重跑 task-closeout-cleanup preview/apply。

## Blockers And Next Action

- Blocker：MySQL binlog、无外部 writer、测试演练槽位和质量审查目录尚未获得环境证据。
- Next action：以最小开发文档作为首版实施依据，按 TDD 顺序实现 FULL、INCREMENTAL、恢复演练和证据导出。
