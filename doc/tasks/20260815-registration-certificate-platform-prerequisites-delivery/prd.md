# Product Requirements Document

## Goal

交付三个可独立验证、可被后续国内注册证领域正式消费的平台前置能力：注册证受控内容投影与严格漂移阻断、全出口业务文件访问门禁、站内信业务键幂等和明确失败。三个能力必须扩展当前平台真实实现，不能复制第二套平台，也不能提前实现仍受业务裁决阻断的注册证领域行为。

## Scope

### SP-01 System controlled-content extension

- 在现有 `ControlledContentType`、`ControlledContentTransitionAction` 和 `ControlledContentTransitionProfile` 中增加注册证类型及“待生效候选登记”动作/profile。
- 复用现有 canonical 状态、version ref、transition audit、唯一候选/当前约束和本地事务。
- 为注册证类型提供精确投影期望值校验；active/candidate 的 native version ID、租户、content type/key 或审计事实漂移时，所有受保护变更在写入前失败。
- 漂移只能报告和阻断，不得自动选择领域侧或平台侧覆盖另一侧。

### SP-02 Infra/DCC business-file access guard

- 把现有直接链接 Guard 扩展为 Infra 所有、DCC 实现 provider 的通用业务文件声明/授权合同。
- 固定操作集合：`DIRECT_LINK`、`PREVIEW`、`ONLYOFFICE_PREVIEW`、`CONVERT`、`PRINT`、`DOWNLOAD`。
- 覆盖公共直链、通用预览元数据/二进制、OnlyOffice 回源、转换发起/回源、打印记录/HTML 和下载字节响应。
- 业务文件判定基于正式引用 provider，不基于路径前缀、文件名或“不是 DCC 受控文件”的反向推断。
- 当前 DCC controlled-file provider 接入统一合同；为后续注册证 provider 留正式扩展点，但本任务不创建注册证业务表或 provider。

### SP-03 System notify idempotency

- 新增显式 Admin 站内信幂等发送 API/DTO/service 合同。
- `system_notify_message.business_key` 可空；唯一范围为 `(tenant_id, business_key)`。
- 同租户同业务键、同接收人、同模板和同规范化参数的重放返回同一 message ID。
- 同键载荷冲突、模板禁用/不存在、参数缺失、业务键空白、消息服务返回空 ID 均明确失败。
- 保留既有非幂等 API，但它同样不得对禁用模板或空 message ID 返回成功/`null`。

## Non-Goals

- 不实现注册证主档、版本表、变更/延续/支持文件领域服务或注册证业务 adapter。
- 不决定 D-001..D-010，不实现立即生效、继承冲突、旧证暴露字段、授权期限/次数、日期边界、提醒默认时间或九类变更字段合同。
- 不实现注册证访问申请、BPM、grant、当前证/旧证页面、下载命名、搜索、提醒 Job、收件人解析或站内信模板种子。
- 不新增注册证菜单、页面、权限、角色、测试账号、公司/企业 Provider。
- 不导入或改写 Excel 的 119 条国内注册证数据，不补批准日、注册人、附件或生产关系。
- 不把自动校验模板纳入本期。
- 不用 mock 成功、默认允许、默认租户/用户、随机业务键、catch 后继续或查询消息内容来替代正式能力。

## User or System Scenarios

### Scenario S-01: registration projection is consistent

Given DCC 后续服务在同一事务内提供锁定的平台前置快照和从正式领域表读取的领域后置快照，When 它通过注册证 controlled-content profile 执行合法平台转换，Then 平台重新校验前置快照、动作唯一 delta 和后置投影，写 version ref 与 transition audit，且不会创建第二套注册证生命周期。

### Scenario S-02: projection drift is present

Given 领域 active/candidate native version ID 与平台引用不一致，或引用审计缺失，When 后续服务请求任何受保护注册证平台转换，Then 平台在第一条写操作前失败并报告漂移项，现有领域数据和平台数据都不被自动修复。

### Scenario S-03: anonymous direct link targets a business file

Given 文件已被 DCC 业务引用 provider 声明为受控业务文件，When 匿名调用者通过 Infra 公共直链读取，Then `DIRECT_LINK` 门禁拒绝并且文件字节不被读取。

### Scenario S-04: a business file is accessed through another exit

Given 登录用户或服务回调请求预览、OnlyOffice、转换、打印或下载，When provider 对租户、对象、主体或该操作不授权，Then 对应出口在读取字节、调用转换器或生成打印结果前拒绝；粗粒度菜单权限不能改变结果。

### Scenario S-05: OnlyOffice callback is replayed in another context

Given 一个为指定租户、用户、业务对象、文件和操作签发的有效期 token，When token 被换租户、换文件、换操作、过期或撤销授权后重放，Then 回源重新执行门禁并拒绝，不返回文件内容。

### Scenario S-06: idempotent notification is replayed

Given 同一租户、收件人、模板、参数和稳定业务键，When 首次发送后因 ACK 前崩溃再次调用，Then 平台返回首次 message ID，数据库仍只有一条对应消息。

### Scenario S-07: notification request is invalid

Given 模板禁用/不存在、模板参数缺失、业务键空白、同键载荷冲突或下层创建返回空 message ID，When 调用发送 API，Then 调用明确失败且不能被记录为发送成功。

## Functional Requirements

### SP-01 requirements

- `FR-01`：System 必须增加 `DCC_REGISTRATION_CERTIFICATE`，不得通过复用 `DCC_CONTROLLED_FILE` 或自由字符串伪装注册证类型。
- `FR-02`：新增 `REGISTER_READY_CANDIDATE`，其唯一合法转换为 `null -> READY_TO_PUBLISH`，表示正式续证已由领域确认、等待生效日，不表示人工审批。注册证 profile 必须且只能包含 `REGISTER_ACTIVE`、`REGISTER_READY_CANDIDATE`、`PUBLISH`、`SUPERSEDE_ACTIVE`；其它动作明确拒绝。现有 `CREATE_CANDIDATE` 和 MES/DCC profile 语义不得改变，本任务不借用 `SUBMIT/APPROVE` 推断注册证人工审批。
- `FR-03`：注册证受保护写操作必须携带不可变的 `platformBefore` 与 `domainAfter` 精确快照，均至少包含 tenant、content type/key、active native version ID、open-candidate native version ID；缺失不得执行。System 不信任调用方声称已读取领域表，后续注册证 adapter 必须在同一事务内从正式领域表构造 `domainAfter`。
- `FR-04`：平台 mutation 必须锁定并重新比较当前 ref 与 `platformBefore` 的精确 ID、唯一计数、引用归属和每个既有 ref 的创建/最近转换 audit，再按动作校验唯一允许 delta：初始登记 `empty -> active`、候选登记 `active -> same active + ready candidate`、生效切换 `active + ready candidate -> candidate active`。任一不一致必须在 ref/audit 写入前失败；写入后还必须比较平台投影与 `domainAfter`，不一致使同一事务回滚。空投影在初始登记前允许 audit 为零，不得误报缺失 audit。
- `FR-05`：合法注册/候选/发布路径继续复用现有 ref/audit 和数据库唯一约束；发布时旧 active -> `SUPERSEDED` 与候选 -> `ACTIVE` 必须处于同一本地事务。

### SP-02 requirements

- `FR-06`：Infra 必须提供不依赖 DCC 的统一业务文件 Guard/SPI；DCC provider 基于正式文件引用声明对象并执行授权。路径、文件名和“非 DCC controlled”都不是允许依据。
- `FR-07`：Guard 请求必须显式携带 operation、infra file ID、tenant ID、已登录 user ID 或经验证的服务身份和 request ID。业务对象、版本和公司归属必须由 provider 从正式引用反查；token 或调用方携带的对象 claim 只能与反查结果比对，不能作为授权真源。provider 要求的公司/grant/版本证据缺失时必须拒绝，不得填默认值。
- `FR-08`：公共直链、预览元数据、预览二进制、OnlyOffice 回源、转换、打印和下载的每个服务端出口都必须调用统一 Guard；拒绝必须发生在任何文件 IO、外部转换调用、打印记录/HTML 创建或响应写出之前。
- `FR-09`：业务文件的公共直链始终拒绝。通用预览不得因 `controlled=false` 直接允许；必须让所有已注册 provider 解析正式引用。多个 provider 冲突声明、provider 异常或声明后的必要上下文缺失必须 fail closed。
- `FR-10`：OnlyOffice/转换 token 对业务文件必须绑定 tenant、主体/服务身份、业务对象、infra file ID、operation 和 expiration；回源时必须验证 token 并再次执行实时授权，过期 token 或授权撤销后不得继续读取。
- `FR-11`：现有 DCC 预览令牌、水印、访问审计、下载策略和打印类别权限继续生效，并与统一 Guard 形成“全部条件都通过”关系；不得用新 Guard 绕开原有更严格规则。
- `FR-12`：经全部已注册 provider 查询后确实没有正式业务引用的普通文件，沿用明确的既有普通文件合同。当前平台无法在运行时区分“真正普通文件”和“未来业务漏装 provider”，因此本任务只对当前 DCC provider 建立强制 Spring 上下文/出口合同；后续注册证引用写入与 provider 接入必须由 SP-04/SP-06 的集成测试和真实 E2E 同时证明，完成前不得用无 provider 路径宣称注册证授权成功。

### SP-03 requirements

- `FR-13`：新增幂等 Admin 发送请求必须包含非空 `businessKey`、userId、templateCode 和 templateParams；业务键在进入持久化前规范化，空白或超过数据库合同长度时失败。
- `FR-14`：`system_notify_message` 增加 nullable `business_key`；正式 MySQL migration、DO、mapper 和 System H2 fixture 必须一致，并以 `(tenant_id, business_key)` 唯一约束作为并发最终仲裁。既有行保持 `NULL`，禁止猜测回填。
- `FR-15`：首次幂等发送必须在本地事务内完成模板状态/参数校验、内容格式化、消息创建和非空 ID 返回；任何失败不得留下被当作成功的消息。
- `FR-16`：相同租户、键、user type/ID、template code 和规范化 template params 的串行或并发重放返回同一 ID，且最多一行；同键任一绑定字段不同必须抛业务键冲突错误。
- `FR-17`：不同租户允许使用相同业务键并各自创建消息；查询既有键必须受 tenant 限制，禁止跨租户返回 message ID。
- `FR-18`：禁用模板、缺模板、缺参数以及下层返回空 message ID 对幂等和既有非幂等发送都必须抛明确错误；禁止保留返回 `null` 的 compatibility/fallback 分支。

## Non-Functional Requirements

- `NFR-01 Security`：所有对象级授权默认由正式 provider 作决定；匿名、跨租户、跨对象、跨操作、token 过期和上下文缺失均 fail closed。
- `NFR-02 Tenant isolation`：controlled-content key、业务文件访问上下文和 notify business key 都必须使用明确租户；不得依赖 `tenant_id=0`、线程缺省值或 `@TenantIgnore` 推断业务租户。
- `NFR-03 Transactionality`：SP-01 ref/audit/发布切换和 SP-03 首次消息创建必须是本地数据库原子事务。SP-02 授权必须先于非事务文件 IO/外部调用；打印记录创建在授权通过后的同一业务事务中执行。
- `NFR-04 Idempotency`：幂等结果由稳定业务键和数据库唯一约束保证，不以时间窗口、消息内容搜索、本地 `SENT` 标志或延时重试猜测。
- `NFR-05 Auditability`：拒绝和漂移错误包含 tenant、operation/content type、业务对象/file ID、request/event key 等非秘密标识；日志不得写文件内容、token、密码或连接凭据。审计写入失败不得反向放行文件。
- `NFR-06 Compatibility`：现有 MES_ROUTE、DCC_CONTROLLED_FILE profile 和合法普通文件路径不得被无关改写；但“禁用模板返回 null”不是需要保留的兼容语义。
- `NFR-07 Testability`：所有 AC 必须由真实存在并被测试 runner 发现的 JUnit/数据库合同验证；不得使用 `failIfNoSpecifiedTests=false`、只做 grep、mock 返回成功或以目标测试不存在作为 RED 证据。
- `NFR-08 No fallback`：已解析出业务 claim 却没有授权 provider、provider 异常/冲突、缺身份、缺租户、缺表/列/索引、缺模板或不支持操作时明确失败，不切换数据源、算法、入口或默认行为。完全无业务引用的普通文件按 `FR-12` 处理，不等同于业务授权 fallback。

## Dependencies and Constraints

### Reuse classification

| Capability | Classification | Evidence and rule |
| --- | --- | --- |
| controlled-content ref/audit/state machine | 直接复用 | 当前 System core、mapper、唯一约束和 JUnit 已存在 |
| registration content type/profile/drift guard | 扩展复用 | 在现有 controlledcontent 包内扩展；禁止平行状态机 |
| Infra direct-link Guard aggregation | 扩展复用 | `FileDirectLinkAccessGuard`、`FileService.validateDirectLinkAllowed` 已存在 |
| DCC preview/token/watermark/audit/download/print policy | 扩展复用 | 保留现有规则并统一接入 operation-aware Guard |
| System notify template/message | 扩展复用 | 现有 API/service/DO/mapper/H2 测试存在；增加显式幂等合同 |
| registration business file provider | 必须后续新建 | 注册证表/引用当前不存在，归 SP-04/SP-06，不在本任务 |
| registration domain/page/reminder/approval/migration | 必须后续新建 | 仍受 D-001..D-010 和外部前置阻断 |

### State and transaction boundaries

| Aggregate | Observable platform path | Boundary |
| --- | --- | --- |
| 注册证 lifecycle projection | no ref -> ACTIVE；或 active + no candidate -> READY_TO_PUBLISH -> old SUPERSEDED + candidate ACTIVE | 锁定并核对 platformBefore；领域写入、domainAfter 读取、ref/audit 写入和后置比对同一事务，发布两端同事务 |
| 文件访问 decision | UNRESOLVED -> ALLOWED 或 DENIED；token VALID -> EXPIRED/CONTEXT_MISMATCH/DENIED | 授权先于 IO/外部调用；任何异常不是 ALLOWED |
| 幂等站内信 | ABSENT -> CREATED(messageId)；same replay -> SAME_ID；payload conflict -> ERROR | 模板校验、唯一写入和 ID 返回在同一本地事务；唯一索引处理并发 |

### External prerequisites

- 三个子项目实现不依赖 D-001..D-010。
- 正式 migration 运行态验收依赖目标数据库只读 schema/index preflight；未提供时可以完成代码/H2/静态迁移验证，但不能宣称运行库已迁移。
- SP-02 的产品级注册证 P0 关闭依赖后续 SP-04/SP-06 提供注册证引用 provider、公司/grant 规则和真实 E2E；本任务完成只表示平台前置就绪。
- 集成只能合并任务自有提交；不得覆盖 `int_main` 现有未提交用户修改。

## Acceptance Criteria

### SP-01

- `AC-01` (maps `FR-01`, `FR-02`): Given committed HEAD 的两类 profile 和新注册证类型，When 枚举并读取全部 profile，Then 注册证类型与 `REGISTER_READY_CANDIDATE` 真实存在，其动作集合精确等于 `REGISTER_ACTIVE/REGISTER_READY_CANDIDATE/PUBLISH/SUPERSEDE_ACTIVE`，新动作只允许 `null -> READY_TO_PUBLISH`，MES/DCC 原动作集合不变，任何未列动作被拒绝。
- `AC-02` (maps `FR-03`, `FR-04`): Given 注册证平台 active/candidate 数量与领域相同但 native version ID 错位，When 发起受保护转换，Then 在 version-ref mapper 和 audit mapper 发生任何 insert/update 前抛出漂移异常，错误列出不一致 ID，两个存储均无写入。
- `AC-03` (maps `FR-03`, `FR-04`): Given 注册证 mutation 未提供 platformBefore/domainAfter、tenant/type/key 不一致、动作 delta 不合法、存在多个 active/open candidate 或既有引用缺 transition audit，When 调用平台 mutation，Then 每一种情况都在首个写操作前明确失败且不自动修复、不补默认期望值；初始 empty 投影不会因 audit 为零被误拒绝。
- `AC-04` (maps `FR-05`): Given 精确投影一致且候选处于可发布状态，When 执行发布，Then 旧 active 变为 `SUPERSEDED`、候选变为 `ACTIVE`、两条 audit 完整写入；注入任一写失败时整个事务回滚，不留下两个 active 或半条审计。

### SP-02

- `AC-05` (maps `FR-06`, `FR-08`, `FR-09`): Given 一个由当前 DCC provider 正式声明的业务文件，When 通过 `@PermitAll/@TenantIgnore` Infra 直链访问，Then `DIRECT_LINK` Guard 在文件读取前拒绝，读取方法未被调用，并记录不含秘密的拒绝证据。
- `AC-06` (maps `FR-06`..`FR-09`, `FR-11`): Given 一个被业务 provider 声明的文件和只有粗粒度 controller permission 的用户，When 以裸 `fileId` 请求通用预览元数据或二进制，Then 两个入口都执行 `PREVIEW` 对象授权；跨租户、错误对象/公司范围或无有效 grant 的样本均拒绝，不能因 `controlled=false` 放行。
- `AC-07` (maps `FR-07`, `FR-10`): Given 为业务文件预览签发的 OnlyOffice token，When 使用正确上下文回源，Then token 校验和实时 `ONLYOFFICE_PREVIEW` 授权均通过后才读取；换租户、换用户、换文件、换操作、过期或授权撤销任一情况都拒绝且不读取。
- `AC-08` (maps `FR-08`, `FR-10`, `FR-11`): Given provider 分别拒绝 `CONVERT`、`PRINT`、`DOWNLOAD`，When 调用对应服务端出口，Then 外部转换 client、打印记录/HTML创建和文件字节读取分别从未发生；provider 允许时仍必须继续通过既有 DCC 转换校验、打印类别权限和下载策略。
- `AC-09` (maps `FR-09`, `FR-12`): Given provider 对同一 file ID 冲突声明、provider 抛错、已解析 claim 却缺授权 provider，或声明后缺 tenant/主体/request ID，When 任一文件出口求值，Then 统一 Guard 明确拒绝；Given 当前 DCC provider 在 Spring 上下文中被移除，When 启动合同测试运行，Then 明确失败；Given 全部 provider 确认无正式引用的普通文件，When 走既有普通文件合同，Then 不被伪标为注册证授权，且注册证业务测试不能用该路径证明受控文件通过。

### SP-03

- `AC-10` (maps `FR-13`..`FR-15`): Given 启用模板、完整参数、明确 tenant 和新业务键，When 首次调用幂等 Admin 发送，Then 返回非空 message ID，消息行保存规范化 business key，且 `(tenant_id,business_key)` 唯一索引和 H2 fixture 合同均存在。
- `AC-11` (maps `FR-16`): Given AC-10 已成功但调用方在 ACK 前崩溃，When 以完全相同请求串行重放，Then 返回同一 message ID，数据库对应租户/业务键仍只有一行。
- `AC-12` (maps `FR-16`): Given 多线程/多事务同时使用同一租户、同一业务键和相同载荷发送，When 全部调用完成，Then 成功调用得到同一 message ID、数据库最多一行；不得把 duplicate-key 异常吞掉后返回空或新 ID。
- `AC-13` (maps `FR-16`): Given 同租户同业务键已绑定一个 user type/ID、template code 和规范化参数，When 任一绑定字段不同的请求重放，Then 抛业务键冲突错误，既有消息不改变且不新增消息。
- `AC-14` (maps `FR-17`): Given 两个明确租户使用相同业务键和相同载荷，When 分别发送，Then 每个租户各有一个不同 message ID；任一租户查询/重放都不能返回另一租户消息。
- `AC-15` (maps `FR-13`, `FR-15`, `FR-18`): Given business key 空白、模板不存在/禁用、模板参数缺失或下层创建返回空 ID，When 调用幂等或对应既有非幂等发送路径，Then 每种情况都抛明确错误、返回值不为 `null`、没有成功消息；现有“禁用模板断言 null”的测试先 RED 后改为异常 GREEN。

### Traceability

| Requirement group | Acceptance IDs |
| --- | --- |
| `FR-01`..`FR-05` | `AC-01`..`AC-04` |
| `FR-06`..`FR-12` | `AC-05`..`AC-09` |
| `FR-13`..`FR-18` | `AC-10`..`AC-15` |
| `NFR-01`..`NFR-08` | 横向由 `AC-02`..`AC-15` 的拒绝、事务、租户、并发和真实测试证据验证 |
