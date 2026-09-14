# Development Plan

## Purpose And Approved Boundary

本计划只交付 `SP-01`、`SP-02`、`SP-03` 三个平台前置能力。注册证主档、版本、页面、菜单、BPM、grant、提醒 Job、历史导入和注册证文件 provider 均不在写入范围；`D-001..D-010` 不在本任务中裁决。

规划时只读核对到的仓库事实如下：

- 仓库是以 `E:\IntRuoyi` 为根的单一 Git 工作区，后端 Maven reactor 位于 `IntRuoyiBackend`，真实模块名为 `yudao-module-system`、`yudao-module-infra`、`yudao-module-dcc`。
- 规划复核实测工具链为 Apache Maven `3.9.9`、Maven runtime Java `21.0.10`；根 `IntRuoyiBackend/pom.xml` 的 `java.version`、`maven.compiler.source`、`maven.compiler.target` 均为 `17`。验收依据是 Java 21 运行 Maven 时仍严格编译到 target 17 并通过目标测试，不能把本机描述成 JDK 17；若构建实际硬性要求改用 JDK 17，必须 fail fast 并重审环境，不得静默切换。
- 分解时 `int_main` 的 committed HEAD 为 `90fb1af111e577431522a43f0d505ddfb7d8250d`，但主工作区仍被并行任务持续修改；创建 worktree 时必须重新冻结当时的 committed `int_main` HEAD，并让四个 worktree 使用同一基线。
- 当前 System controlled-content 的真实入口是 `ControlledContentLifecycleCoreService`，类型/profile/state machine/ref mapper/audit mapper 和 8 个现有 JUnit 均已存在；数据库表由 `sql/mysql/20260718_controlled_content_lifecycle.sql` 建立。
- 当前 Infra 直链只经过 `FileServiceImpl.validateDirectLinkAllowed` 与 `FileDirectLinkAccessGuard`；当前 DCC 正式文件归属由 `DccControlledFileQueryServiceImpl.identifyControlledFileScope` 从正式引用字段反查。
- 当前 DCC 的通用预览、OnlyOffice、转换、打印、下载真实入口分别位于 `DccOnlineFilePreviewServiceImpl`、`DccOnlyOfficePreviewTokenService`、`DccOnlyOfficeDocumentPdfConversionService`、`DccControlledFilePrintServiceImpl`、`DccControlledFileQueryServiceImpl`。
- 当前 System notify 的真实入口是 `NotifyMessageSendApi/Impl`、`NotifySendServiceImpl`、`NotifyMessageServiceImpl`、`NotifyMessageDO/Mapper`；`system_notify_message` 的 MySQL 基线和 System H2 fixture 均无 `business_key`。
- 分解时主工作区已存在与本任务无关的脏文件 `DccControlledFileController.java`、DCC `ErrorCodeConstants.java` 和 DCC `src/test/resources/sql/create_tables.sql`。`T-SP02` 默认禁止修改这三个文件；如实现被证明必须触及其中任一文件，executor 必须停止并由主 Agent 重新裁决写入范围，不能直接形成冲突提交。

## Dependency Graph

```text
FREEZE-BASE
  +--> T-SP01 --+--> T-VERIFY --> T-INTEGRATE --> T-FUSE
  +--> T-SP02 --+
  +--> T-SP03 --+
```

- `T-SP01`、`T-SP02`、`T-SP03` 在同一冻结基线后并行。
- 三个 executor 互不修改对方写入范围，也不修改中央任务文档。
- `T-VERIFY` 必须由未参与对应实现的 tester 执行。
- 任一任务未通过独立验证，都不能进入 `T-INTEGRATE`。
- `T-FUSE` 只允许把组合验证后的集成 HEAD fast-forward 到 `int_main`；融合前出现 `incoming paths ∩ current dirty paths != empty` 时停止。

## Worktree And Branch Layout

由主 Agent 在规划批准后创建并记录最终绝对路径：

| Purpose | Proposed branch | Proposed path |
| --- | --- | --- |
| integration | `codex/20260815-reg-cert-platform-integration` | `D:\IntRuoyiWorktree\reg-cert-platform-integration` |
| SP-01 | `codex/20260815-reg-cert-platform-sp01` | `D:\IntRuoyiWorktree\reg-cert-platform-sp01` |
| SP-02 | `codex/20260815-reg-cert-platform-sp02` | `D:\IntRuoyiWorktree\reg-cert-platform-sp02` |
| SP-03 | `codex/20260815-reg-cert-platform-sp03` | `D:\IntRuoyiWorktree\reg-cert-platform-sp03` |

创建前必须解析每个路径并证明它是 `D:\IntRuoyiWorktree\` 的子路径。计划内不启动前后端服务，但项目经验门禁仍要求在提交和运行 branch runtime guard 前预留 slot。规划复核时注册表只剩 `14`、`18` 两个空闲 slot，因此按以下顺序执行；创建 worktree 时若空闲数量或编号变化，以重新原子检查结果为准，少于两个则立即阻塞：

1. 先按 integration、SP-01 顺序分别调用各自 worktree 的 `scripts\runtime\reserve-worktree-slot.ps1 -Name <directory-name> -Path <absolute-path> -Branch <branch> -Profile int_main -AsJson`；脚本在互斥锁内选择最低空闲槽，按当前快照预期为 integration=`14`、SP-01=`18`。必须以两个 JSON 返回值和 `show-branch-runtime.ps1` 为证据，不手工指定或改写 slot。SP-02、SP-03 可并行开发和运行不启动服务的测试，但在获得 slot 前不得提交、推送或运行 branch runtime guard。
2. SP-01 自测后先运行 branch runtime guard、形成任务自有提交，再由主 Agent审查和独立 tester 验证该提交；验证通过并进入 integration 后，证明提交已成为 integration HEAD 的祖先，移除 SP-01 worktree，确认目录/Git 登记不存在且端口无监听，再持与 reserve 脚本相同的登记表 mutex 只把 SP-01 记录标为 inactive。随后为 SP-02 调用 reserve 脚本原子取得该槽，不能在目录删除前释放。
3. SP-02 融合并清理后把同一空闲 slot 分配给 SP-03。integration 的 slot 保留到最终融合和 closeout 完成。
4. 任一 worktree 均不得使用 `48081`、随机端口、他人的 slot 或直接手改注册表绕过原子脚本。

## Task T-SP01

- `task_id`: `T-SP01`
- `title`: System registration controlled-content projection contract
- `objective`: 在现有受控内容核心中增加注册证类型、`REGISTER_READY_CANDIDATE`、精确静态 profile，以及仅对注册证受保护 mutation 生效的 `platformBefore + action delta + domainAfter` 强制投影合同；漂移必须在 ref/audit 首次写入前失败，发布保持单事务原子性。
- `dependency_ids`: `[]`（仅依赖主 Agent 已冻结统一 committed 基线）
- `acceptance_ids`: `AC-01`, `AC-02`, `AC-03`, `AC-04`
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/enums/controlledcontent/ControlledContentType.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/enums/controlledcontent/ControlledContentTransitionAction.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/controlledcontent/**`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/controlledcontent/ControlledContentVersionRefMapper.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/controlledcontent/ControlledContentTransitionAuditMapper.java`
  - `IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/controlledcontent/**`
  - 可新增 `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/controlledcontent/**` 作为 SP-01 独占的事务测试 DDL；不得修改 SP-03 所有的 System 全局 H2 fixture。
- `write_scope`: 仅上述 controlled-content enum/service/mapper/test 和 SP-01 专属测试资源。不得修改 DCC adapter、注册证领域、System notify、全局 `create_tables.sql`、pom、前端或迁移。当前 ref 表以字符串存类型/动作，本切片不需要 schema 迁移；executor 不得为枚举新增无依据 SQL。
- `implementation_contract`:
  - 新动作唯一状态规则为 `null -> READY_TO_PUBLISH`；现有 `CREATE_CANDIDATE` 仍为 `null -> DRAFT`，MES/DCC profile 不变。
  - 注册证 profile 精确等于 `REGISTER_ACTIVE`, `REGISTER_READY_CANDIDATE`, `PUBLISH`, `SUPERSEDE_ACTIVE`。
  - 精确 snapshot 至少包含 tenant、type、content key、active native version ID、open candidate native version ID；平台侧还必须检查 active/open candidate 唯一计数、每个已有 ref 的归属和 transition audit 存在性。
  - 初始空投影允许 ref/audit 均为零；非空既有 ref 缺 audit 必须拒绝。
  - 受保护 mutation 必须先锁定并比较 `platformBefore`，再校验动作唯一 delta，写入后比较 `domainAfter`；任一不一致使事务失败，不自动修复。
  - 计划不伪造注册证领域查询。后续 adapter 必须从真实领域表构造 `domainAfter`，完成前不得声称产品级漂移闭环关闭。
- `validation_steps`:
  - 先新增/修改可发现测试并执行 `CMD-SP01-FOCUSED`，获得业务断言失败且测试数大于零的 RED。
  - 完成最小实现后以同一 `CMD-SP01-FOCUSED` 获得 GREEN。
  - 执行 `CMD-SP01-REGRESSION`，覆盖现有 profile/state-machine/core/persistence/concurrency/idempotency/health-check 测试。
  - 用 Mockito 调用顺序证明漂移时两个 mapper 均未写；用真实事务测试注入第二段写失败并证明 ref/audit 均回滚。只检查 `@Transactional` 注解不能替代回滚证据。
- `done_definition`: `AC-01..AC-04` 全部有真实 RED、同命令 GREEN、相邻回归、非零测试数和事务回滚证据；无 DCC 注册证 adapter、无第二套状态机、无 fallback；分支只含任务自有提交。

## Task T-SP02

- `task_id`: `T-SP02`
- `title`: Infra/DCC operation-aware business-file access gate
- `objective`: 将现有直链 Guard 扩展为 Infra 所有、provider 解析正式引用的统一业务文件门禁，并把当前 DCC 的 `DIRECT_LINK/PREVIEW/ONLYOFFICE_PREVIEW/CONVERT/PRINT/DOWNLOAD` 全部服务端出口接入；授权必须先于文件 IO、转换调用、打印记录/HTML 和下载输出。
- `dependency_ids`: `[]`（仅依赖主 Agent 已冻结统一 committed 基线）
- `acceptance_ids`: `AC-05`, `AC-06`, `AC-07`, `AC-08`, `AC-09`
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/access/**`
  - `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/FileService.java`
  - `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/FileServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/enums/ErrorCodeConstants.java`（只有不能复用现有 Infra 拒绝码时才允许）
  - `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/file/**`
  - `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/controller/admin/file/FileControllerTest.java`
  - `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccFileDirectLinkAccessGuard.java` 及可新增的 `service/file/access/**`
  - `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/protection/DccControlledFileTemporaryFileDO.java`（仅补齐表中既有 `tenant_id` 的正式租户映射，使 DCC 临时上传引用不能被跨租户当作普通文件）
  - `DccControlledFileQueryService.java`, `DccControlledFileQueryServiceImpl.java`, `DccControlledFileScope.java`, `DccControlledFileArtifactReference.java`
  - `DccOnlyOfficePreviewTokenService.java`, `DccOnlyOfficeDocumentPdfConversionService.java`, `DccControlledFileUploadService.java`, `DccControlledFileUploadServiceImpl.java`
  - `DccControlledFilePrintService.java`, `DccControlledFilePrintServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/filepreview/**`
  - 与以上生产类一一对应的 DCC 测试文件，重点为 `DccFileDirectLinkAccessGuardTest`, `DccOnlineFilePreviewServiceTest`, `DccOnlineFilePreviewControllerTest`, `DccOnlyOfficeControlledPreviewTest`, `DccOnlyOfficeDocumentPdfConversionServiceTest`, `DccControlledFilePrintServiceImplTest`, `DccControlledFileQueryServiceTest`, `DccControlledFilePreviewDownloadApiTest`。
- `write_scope`: 仅上述 Infra access/core file service 和 DCC 文件出口/service/test。默认明确排除当前脏的 `DccControlledFileController.java`、DCC `ErrorCodeConstants.java`、DCC `src/test/resources/sql/create_tables.sql`，也排除所有注册证包、前端、SQL、pom。若无法在服务层完成门禁而确需改排除文件，立即阻塞并请求主 Agent 重划范围。
- `review_scope_correction`: 主 Agent代码审查发现 `dcc_controlled_file_temporary_file` 已有正式 `tenant_id`，但 DO 未映射该字段，导致 DCC 上传预览的正式引用无法携带租户并可被误判为普通文件；批准只把该 DO 的基类改为现有 `TenantBaseDO`，不改表、SQL、mapper、controller 或领域行为。
- `implementation_contract`:
  - Infra 定义操作枚举、请求上下文、provider resolution/decision 和唯一聚合服务；Infra 不依赖 DCC。
  - provider 按 infra file ID 从正式引用反查业务对象、版本、租户和归属。调用方/token claim 只能与反查结果比对，不能制造业务对象。
  - 同一文件被多个 provider 声明、provider 抛错、已声明却缺 tenant/主体或验证服务身份/request ID、claim 不匹配或操作不支持时明确拒绝。
  - 全部 provider 都明确未声明的普通文件才沿用既有普通文件合同；这不是业务文件 fallback，也不能用于证明注册证文件已受保护。
  - 公共直链的业务文件始终拒绝。通用预览不得以 `controlled=false` 反推允许。
  - token 必须绑定 tenant、主体或明确服务身份、正式业务对象、infra file ID、operation、有效期；OnlyOffice/转换回源再次进行实时门禁。
  - 现有 DCC viewer token、水印、访问审计、类别打印权限、下载策略全部继续执行，统一 Guard 是附加且必要的条件。
  - DCC provider 必须有 Spring 上下文存在合同；移除 provider 的 context test 必须失败，不能把 provider list 空视作当前 DCC 正常启动。
- `validation_steps`:
  - 按 test plan 的 SP02-A/B/C 三个最小切片分别执行真实 RED、同命令 GREEN；每个命令必须发现至少一个目标测试。
  - 严格按 `SP02-A test -> A RED -> A 最小生产实现 -> A GREEN -> SP02-B test -> B RED -> B 最小生产实现 -> B GREEN -> SP02-C test -> C RED -> C 最小生产实现 -> C GREEN` 推进；禁止先完成 A/B/C 全部生产代码再补分片 RED。
  - 用 mock 顺序验证 deny 时 `FileClient/getFileContent`、OnlyOffice conversion client、print mapper/HTML builder、download byte reader 均未发生。
  - 执行 Infra 与 DCC 相邻回归，并验证普通无引用文件的既有路径不被错误标记为注册证授权。
  - 独立 tester 必须逐个点名六种 operation 与七类出口；不能以单一 controller 测试替代全出口证明。
- `done_definition`: `AC-05..AC-09` 全部 GREEN；所有出口在副作用前使用同一聚合门禁；provider 正式反查、冲突/异常/缺上下文 fail closed；普通文件合同只在无引用时成立；不宣称注册证文件 P0 整体关闭。

## Task T-SP03

- `task_id`: `T-SP03`
- `title`: System notify tenant business-key idempotency and explicit failure
- `objective`: 为 Admin 站内信增加显式幂等 API，持久化规范化业务键并以 `(tenant_id,business_key)` 唯一约束仲裁串行/并发重放，同时把模板禁用、模板缺失、参数缺失和空 message ID 改为明确失败。
- `dependency_ids`: `[]`（仅依赖主 Agent 已冻结统一 committed 基线）
- `acceptance_ids`: `AC-10`, `AC-11`, `AC-12`, `AC-13`, `AC-14`, `AC-15`
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/api/notify/**`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/notify/**`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/dataobject/notify/NotifyMessageDO.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/notify/NotifyMessageMapper.java`
  - `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/enums/ErrorCodeConstants.java`
  - `IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/notify/**`
  - 可新增 System notify API 合同测试。
  - `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql`
  - `IntRuoyiBackend/sql/mysql/20260815_system_notify_message_business_key.sql`
  - `IntRuoyiBackend/sql/mysql/ruoyi-vue-pro.sql`
  - `IntRuoyiBackend/script/tests/test_system_notify_message_business_key_sql.py`
- `write_scope`: 仅上述 System notify/API/error、System H2 fixture、一个正式 migration、full-schema 同步和一个 migration contract test。执行期证据审查另批准只同步 Showroom 测试夹具中的既有 `system_role.category_id` 与本任务新增的 `system_notify_message.business_key`/唯一约束，用于恢复真实调用方测试；不得修改 Showroom 生产代码、DCC delivery、提醒、模板 seed、其它模块调用方、controlled-content 或文件门禁。
- `execution_review_correction`: Trade 唯一 Member 站内信调用位于固定提前返回之后，当前没有可达运行时调用方。删除该提前返回会新增订单发货通知业务行为，违反本任务“只交付三个平台前置能力”的边界，因此以静态不可达证明和 leaf compile 合同取代被禁用的无关 Trade 测试，不修改 Trade 生产代码。原 broad caller 命令在未进入 System/BPM/DCC/MES/Showroom 前即被四个与本任务无关、且可在无 SP-03 的当前主线基线上复现的 Infra runtime-control 用例阻断；它保留为基线诊断，实际 caller gate 改为逐个可达调用点的非禁用定向测试，不以跳过或 `failIfNoSpecifiedTests=false` 放行。
- `implementation_contract`:
  - 新 DTO/方法只面向 Admin 幂等发送，`businessKey` trim 后非空且最大长度固定为数据库合同 `255`；不接受随机 UUID 代替调用方稳定业务键。
  - `business_key` 可空，旧非幂等路径继续写 `NULL`；迁移不回填历史行。
  - migration 首行固定为仓库正式元数据，`dependsOn=20260715_showroom_notify_template_garbled_repair`；它是实际触及 `system_notify_message` 的最新 DAG 叶子，并已依赖同日 MES eDHR repair。元数据还必须显式填写 `allowedEnvironments=test,backup,prod`、`type=schema`、`riskLevel=medium`；DDL 先通过 `information_schema` 做表/列/索引前置检查，发现同名但合同不一致时 `SIGNAL` 失败，不能跳过或假装成功。
  - 首次发送在本地事务内完成模板存在/启用/参数校验、规范化参数、消息插入与非空 ID 校验。
  - 串行或并发同租户、同键、同 user type/ID、模板和规范化参数返回同一 ID；任一载荷字段不同抛业务键冲突。
  - 不同租户同键互不读取。查询必须受 tenant context/tenant 条件约束。
  - 数据库唯一约束是最终仲裁；duplicate-key 只允许在重新读取并严格比对既有行后返回同一 ID，不能吞异常后返回空或新 ID。
  - 模板禁用、缺模板、缺参数和底层空 ID 对新旧发送路径都失败；现有 `return null` 分支必须删除，不增加 compatibility shim。
- `validation_steps`:
  - 先改写禁用模板旧测试预期并新增业务键 DB/并发测试，执行 `CMD-SP03-FOCUSED` 取得真实 RED 和非零测试数。
  - 最小实现后执行同一 `CMD-SP03-FOCUSED` 取得 GREEN。
  - 执行 `CMD-SP03-MIGRATION` 验证 migration 元数据、nullable column、唯一索引、H2 fixture 和 full schema 一致。
  - 执行 `CMD-SP03-CALLER-REGRESSION` 的逐模块命令，覆盖 System notify 和现有 BPM/DCC/Infra/MES/Showroom 可达调用点；再执行被 root reactor 排除但真实引用该 API 的 IoT 定向回归，并执行 Trade 静态不可达及 leaf compile 合同。保留原 broad 命令作为基线诊断；失败时不得恢复 null 行为、跳过目标测试或修改 Trade 业务行为。
- `done_definition`: `AC-10..AC-15` 全部 GREEN；并发最多一行且同 ID；冲突、跨租户和所有无效发送明确失败；正式 migration/H2/full schema 一致；无随机键、查文案去重、空 ID 或 fallback。

## Task T-VERIFY

- `task_id`: `T-VERIFY`
- `title`: Independent branch verification
- `objective`: 由未参与对应分支编写的 tester 在每个 executor worktree 中重新执行计划测试，直接检查代码、Git diff、Surefire/pytest 输出和迁移合同；tester 不修产品代码。
- `dependency_ids`: `T-SP01`, `T-SP02`, `T-SP03`
- `affected_paths`: 三个 executor 分支的只读代码与生成的 `target/surefire-reports`、pytest/Maven stdout。
- `write_scope`: tester 不写产品代码、测试预期、规划文档或中央报告；只把结构化结果返回主 Agent，由主 Agent写中央 `test-report.md`。
- `acceptance_ids`: `AC-01..AC-15`
- `validation_steps`: tester 开始前确认当前 executor worktree 已按槽位轮转规则获得 active registry entry、branch runtime guard 已通过且目标提交 clean；逐分支重跑对应 toolchain preflight、focused、regression 和 migration 命令，核对测试数大于零、退出码、目标方法、未关闭“指定测试未匹配即失败”的 Surefire 保护、无未提交产品 diff；按 AC 给 PASS/FAIL。
- `done_definition`: 三个分支分别得到独立 tester 的真实 PASS；任一 FAIL 只退回原 executor，最多按监督规则 3 次修复/复验。

## Task T-INTEGRATE

- `task_id`: `T-INTEGRATE`
- `title`: Verified commit integration and combined regression
- `objective`: 只把独立验证通过的任务自有提交按 `SP-01 -> SP-02 -> SP-03` 顺序融合到集成 worktree，检查跨分支运行时合同并执行组合回归、迁移与安全门禁。
- `dependency_ids`: `T-VERIFY`
- `affected_paths`: 三个任务提交的并集；集成 worktree 自有 `target` 验证产物。
- `write_scope`: 主 Agent独占集成分支；除为解决三个已验证提交之间的真实语义冲突所需的最小集成测试/修复外，不新增范围。任何额外产品修复都必须先形成 RED，并接受独立复验。
- `acceptance_ids`: `AC-01..AC-15`
- `validation_steps`:
  - 每次融合前后比较 `git diff --name-status <frozen-base>..HEAD` 与批准写入范围。
  - 每次融合后运行该分支 focused tests；全部融合后执行 `CMD-INTEGRATION-REGRESSION`, `CMD-INTEGRATION-MIGRATION`、runtime schema read-only gate、Spring provider presence、`git diff --check`、冲突标记扫描和 branch runtime guard。目标运行库或只读凭据缺失时必须按 `B-PLATFORM-DB-001` 阻塞，不能把未验证降级为可融合。
  - 确认 Infra 不依赖 DCC、DCC runtime 注入到 Infra 聚合器、授权发生在任何 IO/写库/外部调用之前。
- `done_definition`: 组合回归、静态/H2 migration 合同、运行态 schema preflight、安全门禁和 diff 审计全部 PASS；集成 worktree clean，HEAD 只含已批准任务提交。

## Task T-FUSE

- `task_id`: `T-FUSE`
- `title`: Fast-forward integration into int_main and closeout
- `objective`: 在不覆盖主工作区任何未提交改动的前提下，把已验证集成 HEAD fast-forward 融合到 `int_main`，随后由主 Agent执行任务收尾。
- `dependency_ids`: `T-INTEGRATE`
- `affected_paths`: Git ref `int_main`、本任务中央任务文档、四个任务 worktree/branches。
- `write_scope`: 主 Agent独占；不得暂存、提交、stash、清理或覆盖非本任务脏文件。
- `acceptance_ids`: `AC-01..AC-15`
- `validation_steps`:
  - 融合前重新记录 `int_main` HEAD、全部 dirty/staged/untracked 路径与 `incoming paths`，计算精确交集。
  - 交集非空立即停止，不自动 stash、不整文件 ours/theirs、不覆盖。交集为空且 `int_main` 是集成 HEAD 祖先时才执行 `git merge --ff-only`。
  - 融合后证明集成 HEAD 是 `int_main` 祖先，脏文件清单和内容仍保留且未进入任务提交。
  - 主 Agent将机器状态先设为 `ready_for_closeout`，按 task-closeout preview/apply 仅清理任务自有临时物，完成后设为 `completed`；保留 `task.md`, `execution-log.md`, `verification-report.md` 及任务明确 keep 文件。
- `done_definition`: `int_main` 只 fast-forward 到已验证提交；用户/其它任务脏改动未被覆盖、暂存或提交；任务记录完成且 task-owned worktree 清理证据完整。

## Shared Conflict Matrix

| Shared surface | Owner | Rule |
| --- | --- | --- |
| System `controlledcontent/**` | `T-SP01` | SP-03 不得触碰 |
| System `notify/**`, System H2 fixture, System notify migration | `T-SP03` | SP-01 事务测试使用专属资源，不改全局 fixture |
| Infra `service/file/access/**` and `FileService*` | `T-SP02` | 其它 executor 只读 |
| DCC file preview/query/token/conversion/print/download | `T-SP02` | 不改注册证域；不改当前脏 controller/error/fixture |
| root pom / module poms | none | 当前计划不需要依赖变更；发现需要时阻塞重审 |
| central task artifacts | main Agent | executor/tester 均不写 |
| integration branch/ref | main Agent | 子 Agent不得 merge/rebase/followup 其它 Agent |

## Planning Blockers And Stop Conditions

- `B-BASE-MOVED`: worktree 创建前若 committed `int_main` 又前进，主 Agent重新冻结同一基线并复核上述真实路径；不得让四个 worktree 使用不同基线。
- `B-SP02-DIRTY-OVERLAP`: SP-02 若必须修改当前主工作区脏的 DCC controller/error/fixture，停止并由主 Agent判断是否等待 owner、调整实现边界或最终阻塞；本计划不授权 fallback。
- `B-PLATFORM-DB-001`: 融合前必须获得目标运行库及只读凭据，并完成 `system_notify_message` 与 controlled-content 表/列/索引 preflight。缺少连接证据、基表缺失、半迁移或既有列/索引与合同冲突都会阻断最终 fast-forward；新 notify 列/索引完全缺失可记录为正常待部署事实，但前置基表和既有 controlled-content 合同仍必须实际验证。
- `B-REG-FILE-PROVIDER-001`: 本任务不创建注册证文件 provider。即使 SP-02 通过，也只能标记平台门禁就绪，注册证文件权限 P0 保持 blocked。
- `B-TEST-DISCOVERY`: 任何 Maven/pytest 命令测试数为零、目标 class 未发现、命令无退出码、`exec {}` 或没有 tool output，均不是 RED/GREEN/PASS。
- `B-INTEGRATION-001`: 融合前 incoming 与 `int_main` 当前 dirty 文件有交集时停止；不得覆盖。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；分别扩展现有 lifecycle core、Infra file gate 和 System notify 表/API。
- 是否存在临时补丁或绕过：否。
