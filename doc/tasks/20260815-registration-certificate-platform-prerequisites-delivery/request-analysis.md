# Request Analysis

## User Goal

在隔离 worktree 中，以 BDD、严格 TDD 和 Subagent-Driven 方式先交付国内注册证后续实现所需的三个平台前置能力，并由主 Agent 审查、独立验证后再融合到 `int_main`：

- `SP-01`：扩展 System 受控内容生命周期核心，使其识别注册证类型、提供注册证最小静态 profile，并对领域事实与平台投影漂移执行强制阻断。
- `SP-02`：把现有 Infra/DCC 文件保护扩展为按操作、对象、租户和调用身份判定的统一业务文件访问门禁，覆盖公共直链、通用预览、OnlyOffice、转换、打印和下载。
- `SP-03`：为 System 站内信增加租户内稳定业务键幂等；禁用模板、缺参数和空消息 ID 必须明确失败。

本任务只交付可被后续注册证领域消费的平台能力，不把三个前置能力等同于完整注册证需求已经完成。

## Current System

### Evidence baseline

- 规划基线为 Git `HEAD=bba5ba689a75008a0fb8d1ce3eb9f38ee68e47a4`。主工作区存在大量其他任务的未提交修改；本规划不以这些脏文件作为目标能力已经存在的证据。
- 原始 Markdown 明确了注册证提醒、维护、查看和下载诉求；后续用户确认了“当前证基本信息和文件在线查看免申请，旧证基本信息和文件需申请，所有下载需申请，无项目代码禁止下载，每日固定时间检查且时间可配置，仅站内信提醒，自动校验模板暂不做”。
- OfficeCLI L1 实际读取工作簿：4 个工作表、3477 个单元格、无公式和错误；“国内注册证”工作表 120 行（1 行表头、119 行数据）、9 列。该数据不在本平台前置任务中导入或修改。
- 原设计独立终审为 `FAIL`。D-001..D-005、D-007..D-010 和业务主数据、审批、附件、真实账号等前置仍未关闭，因此完整注册证领域、页面、审批、提醒和迁移仍禁止实施。

### SP-01 controlled-content

| Item | Committed HEAD fact | Target change |
| --- | --- | --- |
| 类型 | `ControlledContentType` 只有 `MES_ROUTE`、`DCC_CONTROLLED_FILE` | 扩展复用，增加 `DCC_REGISTRATION_CERTIFICATE` |
| profile | `ControlledContentTransitionProfile` 只为上述两类定义静态动作集合，缺失 profile 会抛异常 | 增加仅覆盖首证、待生效候选登记和生效切换所需的不变动作集合，不引入注册证人工审批 |
| 状态机 | 现有 canonical 状态和转换已支持 active、candidate、ready、publish、supersede | 直接复用；不新增第二套注册证平台状态机 |
| 漂移检查 | `ControlledContentLifecycleHealthCheckService` 只比较 active/open-candidate 数量与审计数量，返回 issues；不会阻止后续写操作，也不核对精确 native version ID | 增加强制一致性合同；注册证类型的受保护写操作缺少期望投影或发现任何漂移时，在写 ref/audit 前失败，且不得自动修复 |
| 事务 | ref 更新和 transition audit 已在本地 Spring 事务中写入 | 保持 ref、audit 和漂移前置校验处于同一本地事务边界 |
| 测试入口 | System 已有 `ControlledContentTransitionProfileContractTest`、`ControlledContentHealthCheckReadOnlyTest`、core/state-machine/persistence 测试 | 扩展真实 JUnit 测试；不能把目标测试文件不存在本身当成 RED 成功 |

当前 `CREATE_CANDIDATE` 固定创建 `DRAFT`，而 `PUBLISH` 只接受 `READY_TO_PUBLISH`；若 profile 只有原四个动作，候选无法发布。依据原需求“正式续证可提前上传，到生效日自动切换”且没有续证人工审批，本任务新增平台动作 `REGISTER_READY_CANDIDATE`，只表示把已由领域确认的正式续证登记为待生效候选。注册证最小 profile 精确包含 `REGISTER_ACTIVE`、`REGISTER_READY_CANDIDATE`、`PUBLISH`、`SUPERSEDE_ACTIVE`，不借用 `SUBMIT/APPROVE` 猜测人工审批。作废、撤回、重做和冲突继承仍由后续领域设计决定。

严格投影合同分成三个不可混淆的值：锁定时的平台前置快照、同一事务内从正式领域表读取的领域后置快照、动作允许的唯一 delta。调用顺序固定为“锁定并比对领域/平台前置事实 -> 写领域事实 -> 读取领域后置事实 -> 调用平台 mutation -> 平台重新比对前置快照并校验 delta -> 写 ref/audit -> 比对平台后置快照 -> 同一事务提交”。System 只校验调用合同和平台事实，不声称能读取尚不存在的注册证表；后续注册证 adapter 必须以真实领域查询实现快照提供者，完成前产品级漂移闭环仍 blocked。

### SP-02 business-file access

| Item | Committed HEAD fact | Target change |
| --- | --- | --- |
| 公共直链 | `FileController.getFileContent` 是 `@PermitAll + @TenantIgnore`，读取前调用 `FileService.validateDirectLinkAllowed` | 保留唯一入口检查，但升级为统一、按操作的业务文件门禁；业务受控文件不得因匿名或缺上下文被放行 |
| 现有 Guard | Infra 定义 `FileDirectLinkAccessGuard`；DCC 的 `DccFileDirectLinkAccessGuard` 仅按 DCC 受控文件引用识别并拒绝直链 | 扩展复用为业务文件声明/授权 SPI，Infra 不反向依赖 DCC；DCC provider 必须从正式引用反查对象，不信任调用方自报对象 |
| 通用预览 | `DccOnlineFilePreviewServiceImpl.requirePreviewableInfraFile` 只拒绝 DCC `controlled=true`；任何“非 DCC 受控”的裸 `fileId` 可进入通用预览 | 每次元数据、二进制和 OnlyOffice 回源都按业务引用和操作重新授权，不能用“非 DCC 受控”推导为允许 |
| OnlyOffice | 通用回源端点是 `@PermitAll + @TenantIgnore`；当前通用 token 主要绑定 resource/fileId，DCC 受控预览另有更完整证据 | 对业务文件的 token 必须绑定租户、操作者或明确服务身份、业务对象、操作和有效期；回源时重新运行门禁 |
| 转换 | `DccOnlyOfficeDocumentPdfConversionService` 生成 upload-preview 回源 URL，未接收统一对象级访问判定上下文 | 转换发起和源文件回源均执行 `CONVERT` 操作授权；拒绝后不得调用外部转换器 |
| 打印 | `DccControlledFilePrintServiceImpl` 自行校验 ACTIVE、当前版本和类别打印权限 | 保留领域规则，并在产生打印记录/HTML 前接入统一 `PRINT` 门禁 |
| 下载 | `DccControlledFileQueryServiceImpl` 有独立下载策略、审计和 request ID | 保留领域下载策略，并在输出字节前接入统一 `DOWNLOAD` 门禁 |
| 权限边界 | 控制器有粗粒度权限，DCC 各出口有不同对象校验 | 粗粒度权限不能替代对象级门禁；所有业务文件出口都必须同时满足 provider 的租户/对象/操作判定 |

本任务只能交付平台门禁和当前 DCC provider 的接入。注册证业务文件引用表、当前证/旧证规则、公司范围和 grant provider 属于后续 SP-04/SP-06；在该 provider 真正接入前，不得宣称原注册证文件 P0 已经整体关闭。

### SP-03 notify

| Item | Committed HEAD fact | Target change |
| --- | --- | --- |
| API/DTO | `NotifyMessageSendApi` 只有非幂等 Admin/Member 发送；`NotifySendSingleToUserReqDTO` 无业务键 | 新增显式 Admin 幂等发送 DTO/方法；既有非幂等方法不伪装成幂等 |
| 发送行为 | `NotifySendServiceImpl` 遇到禁用模板记录日志并返回 `null`；现有测试明确断言 `null` | 改为明确异常；任何发送路径不能以 `null` 表示成功或可忽略结果 |
| 持久化 | `NotifyMessageDO`、`NotifyMessageMapper` 和 `system_notify_message` 无 `business_key` | 增加可空列和租户内唯一约束；幂等 API 必须提供非空键 |
| 当前调用方 | BPM、DCC、MES、IoT、Showroom 等调用既有 API；部分忽略返回值，部分自己检查 `null` | 禁用模板改为异常是有意的 fail-fast 行为，必须回归现有调用契约；不得保留返回 null 的兼容分支 |
| 数据库 | committed MySQL schema 和 System H2 fixture 均无业务键；现有行没有可追溯稳定键 | 迁移只增加 nullable 列/唯一索引，不猜测回填既有消息 |
| 测试入口 | `NotifySendServiceImplTest`、`NotifyMessageServiceImplTest` 真实存在，后者使用 H2 表 fixture | 新增数据库幂等/并发测试并同步 fixture；目标 class 名必须由分解阶段按真实路径固定 |

### Roles, identities, and data quantities

- `SP-01` 的调用方是后续 DCC 注册证领域服务；目标消息/文件业务人员角色不参与 platform type/profile 判定。
- `SP-02` 的主体包括：已登录后台用户、携带已验证上下文的 OnlyOffice/转换服务回调、没有身份的公共直链调用者。匿名调用者访问被业务 provider 声明的文件时只能拒绝。
- `SP-03` 的调用方是后续 DCC reminder/notification 服务，收件人是一个明确 Admin 用户。业务键按当前租户隔离；本任务不解析“注册部、子公司负责人、集团管代、总经办”等人员。
- 本任务写入的注册证业务记录数为 0，导入 Excel 行数为 0，创建注册证菜单/角色/账号数为 0。测试只创建任务自有的生命周期引用、文件声明和通知消息数据。

## Constraints

- 只允许实现 SP-01/SP-02/SP-03；不得新增注册证主档、版本、审批、grant、提醒任务、配置页、菜单、权限种子或 119 行历史迁移。
- 不得依据 D-001..D-010 的推荐值写代码；缺失方向裁决必须继续保持 blocked。
- 扩展已有 controlled-content、Infra file access、DCC preview/protection 和 System notify；没有充分证据不得平行新建第二套生命周期、文件平台或消息表。
- Infra 只能定义通用 SPI，不能依赖 DCC；DCC provider 可以依赖 Infra 合同。
- 所有业务文件授权必须先于文件字节读取、外部转换调用、打印 HTML/记录生成和下载响应。
- 租户、操作者/服务身份、操作、请求 ID 等必要上下文缺失时必须失败；业务对象由 provider 从正式引用解析，token/调用方 claim 只能用于匹配，不能作为授权真源；不得填默认租户、默认用户或默认 grant。
- 既有非幂等站内信允许 `business_key=NULL`；幂等 API 不允许空键。不得把随机 UUID 当稳定业务键。
- 不使用 fallback、mock 成功、默认成功、吞异常或“查不到就允许”的业务文件策略。
- 三个 executor 使用互不冲突的 worktree 和写入范围；共享任务状态、规划文档和最终集成由主 Agent 单独管理。

## Unknowns

- 当前运行数据库尚未在本规划线程中获得连接目标和只读 schema 证据。committed MySQL DDL、正式迁移目录和 H2 fixture 已核对；最终迁移验证前仍必须对实际目标库执行列/索引 preflight。
- SP-02 本任务没有注册证业务文件引用 provider，因为注册证领域表尚不存在。它只能证明“provider 声明的业务文件在全部出口被统一门禁处理”；完整注册证查看/下载行为必须等 SP-04/SP-06 接入后再验收。
- 现有各业务模块对“禁用站内信模板返回 null”的依赖程度需要由回归测试确认；不允许因调用方失败而恢复 null 行为。
- `int_main` 主工作区的未提交变更可能与后续集成提交重叠。融合前必须逐文件核对；冲突时停止并由主 Agent 裁决，不能覆盖用户改动。

## Risks

| Risk | Impact | Required control |
| --- | --- | --- |
| 把 coarse controller permission 当对象授权 | 裸 `fileId` 或公共回源可绕过旧证/grant 规则 | 每个出口显式调用同一 operation-aware Guard，provider 负责对象级决定 |
| 未来业务 provider 漏装无法由当前聚合器运行时识别 | 新业务文件可能被误当普通文件 | 当前 DCC provider 以 Spring 上下文合同强制存在；未来注册证引用创建与 provider 接入必须由 SP-04/SP-06 的集成测试和真实 E2E 同时证明，完成前不得宣称注册证文件受保护 |
| OnlyOffice token 只绑定 fileId | token 可跨用户、租户或操作重放 | token 绑定租户、主体、业务对象、操作和期限，回源二次授权 |
| 漂移检查只看数量 | active/candidate 数量相同但 ID 错位仍被误判一致 | 比较精确 native version ID、content key、租户、类型和审计事实 |
| 通知先查后插并发竞态 | 同一业务键产生两条消息 | 数据库唯一约束为最终仲裁，事务内返回唯一既有/新建 ID |
| business key 被不同载荷复用 | 重放返回错误接收人或内容 | 比对用户类型/ID、模板 code 和规范化参数摘要；冲突明确失败 |
| 禁用模板异常影响旧调用方 | 既有业务从静默不发变为可见失败 | 这是预期安全修正；跑 System 及实际调用模块相邻回归，不增加兼容分支 |
| 迁移与测试 fixture 不一致 | 生产能跑但 H2 测试失真，或反之 | 同步正式 migration、schema contract 和 System H2 fixture |

## Validation Surface

- System controlled-content：现有 profile、state-machine、core、persistence、concurrency、idempotency、health-check JUnit。
- Infra：`FileControllerTest`、`FileServiceImplTest`、`FileDirectLinkAccessGuard` 相关测试。
- DCC：`DccFileDirectLinkAccessGuardTest`、`DccOnlineFilePreviewControllerTest`、`DccOnlineFilePreviewServiceTest`、`DccOnlyOfficeControlledPreviewTest`、`DccOnlyOfficeDocumentPdfConversionServiceTest`、`DccControlledFilePrintServiceImplTest`、preview/download API 和 protection 测试。
- System notify：`NotifySendServiceImplTest`、`NotifyMessageServiceImplTest` 及新增 business-key 数据库/并发合同测试。
- Schema：正式 MySQL migration、`yudao-module-system/src/test/resources/sql/create_tables.sql`、唯一索引/nullable 兼容合同、release migration policy gate。
- 组合验证：System、Infra、DCC 三模块目标测试与相邻回归；不以跳过未匹配测试、静态 grep 或目标文件不存在代替测试执行。
- 本任务不需要注册证页面 E2E。后续注册证 provider 接入后，必须另做真实登录、真实菜单、真实文件和真实权限路径 E2E，才能关闭产品级文件访问 P0。

## Blocking Prerequisites

### Planning and isolated platform implementation

当前没有依赖 D-001..D-010 的阻断项。三个平台能力可以在明确边界内规划和实现。

### Integration and final claims

- `B-PLATFORM-DB-001`：融合前必须获得目标运行库 `system_notify_message` 与 controlled-content 表的只读 schema/索引证据；缺失时迁移运行态验收失败。
- `B-PLATFORM-FILE-001`：若本任务的 SP-02 没有把当前 DCC 的公共直链、通用预览、OnlyOffice、转换、打印和下载全部接入统一 Guard，SP-02 必须 FAIL，不能缩小为只保护一个 controller。
- `B-REG-FILE-PROVIDER-001`：注册证业务文件引用 provider 不属于本任务；在 SP-04/SP-06 交付并通过真实 E2E 前，原注册证“当前证/旧证/下载申请”产品验收保持 blocked。
- `B-INTEGRATION-001`：若任务提交与 `int_main` 未提交用户修改在同一文件发生冲突，停止融合并报告精确文件；禁止强制覆盖。
