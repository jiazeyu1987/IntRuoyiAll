# Product Requirements Document

## Goal

将 BPM 表单模板和 MES 批记录表单的 Word 原始结构解析统一为一个独立、可测试、无业务依赖的共享 parser。两个业务入口对同一文档必须消费同一份 canonical 原始结构；共享结果只表达段落、页眉页脚、顶层表格、原始行列/合并关系和通用样式，同时保持各自的字段识别、标题/拆表、路线识别、报表生成、版本、审批、权限和持久化语义。

成功标准不是让 BPM 和 MES 最终输出同一种业务对象，也不是让共享 parser 理解批记录页面。成功标准是消除两套可分叉的 POI 原始结构解析实现，并用真实 `.doc` 与合成表格证明两个 adapter 消费同一份共享原始结构，同时证明 MES adapter 执行标题判定和拆表后的最终模型与迁移前快照等价。

## Scope

- 在后端 reactor 新增 `yudao-module-word-parser` 共享模块。
- 定义 `SharedWordDocumentParser`、`WordParseCommand`、`WordParseResult`、共享结构模型、`WordParseProfile.STRUCTURAL_CANONICAL`、稳定错误码和脱敏 diagnostics。
- 将 `MesProBatchRecordDocParser` 中纯 Word 结构解析能力迁入共享模块。
- 保留 `MesProBatchRecordDocParser` 为 MES adapter 或以等价 MES adapter 替代，负责共享结构到 MES parsed models 的映射。
- 将 `MesProBatchRecordSharedPageTitleRules`、批记录标题判定和 `splitTemplates` 拆表完整保留在 MES adapter；这些规则不得进入共享模块。
- 将 `DefaultWordFormTemplateRecognizer` 改为调用共享 parser，并负责共享结构到 BPM `FormTemplateRecognition` 的映射。
- 更新 parent、BPM、MES Maven 依赖，并增加自动化依赖方向门禁。
- 增加共享 parser、旧新结构等价、BPM adapter、MES adapter、错误映射和 API 合同测试。
- 使用仓库内真实 `.doc` fixture 与测试内最小合成表格执行严格 RED/GREEN/REGRESSION 验证。
- 移除 BPM 和 MES 中重复、可独立运行的 POI 结构解析路径；不保留旧 parser fallback。

## Non-Goals

- 不合并 BPM 与 MES 的 HTTP 上传接口，也不新增跨业务通用上传 Controller。
- 不修改前端页面、交互、菜单、API wrapper 或错误后的重试路由。
- 不改变现有 URL、请求/响应、权限、租户、事务、审批、版本、路线或产品绑定合同。
- 不扩大任何现有业务入口允许的文件类型；共享 parser 支持 `.doc/.docx` 与入口准入规则是两层合同。
- 不让 BPM 生成 Jimu 报表，也不让 MES 生成 `FormRecognizedField`。
- 不要求 BPM 最终模板还原 MES 报表版式；BPM 第一阶段仍持久化字段识别 schema。
- 不把 `fillable`、`componentFlag`、`edhrCellRule`、签名规则、路线识别或批记录页面规则迁入共享模型。
- 不新增业务表、数据库迁移、远程识别服务、消息队列或对象存储。
- 不处理图片识别 parser。
- 不顺带修改批记录导入权限、文件类型或其他产品行为。

## User or System Scenarios

### Scenario S-01: BPM and MES share structural parsing

Given 同一份受支持的 Word 文件可分别进入表单模板和批记录导入链路

When 两个业务 adapter 请求解析文档结构

Then 两者调用同一个 `SharedWordDocumentParser` 和 `STRUCTURAL_CANONICAL` profile，并以同一 `WordParseResult` 契约作为各自业务映射输入。

### Scenario S-02: Business mappings remain independent

Given shared parser 已返回段落、文档 frame、顶层表格及其原始行列/合并/样式结构

When BPM 与 MES 分别消费该结果

Then BPM 生成字段 label/code/type/required，MES adapter 使用 `MesProBatchRecordSharedPageTitleRules` 完成批记录标题判定并执行 `splitTemplates` 后生成 parsed models、路线和报表数据，且任何一方的业务字段或批记录拆表结果不进入共享模型。

### Scenario S-03: Existing BPM import succeeds

Given 用户有 `form:template:create` 权限并上传当前入口允许的有效 `.doc` 或 `.docx`

When 调用 `/form-center/templates/import-doc`

Then 系统用共享结构识别候选 label，保持现有去重、字段推断、版本和审批语义，并只在识别成功后持久化模板版本。

### Scenario S-04: Existing MES imports remain stable

Given 用户通过批记录主表单或附加槽位的既有入口上传该入口允许的有效 Word 文件

When MES adapter 消费共享结构

Then 现有表格数量、路线识别、布局、Jimu JSON、版本治理和产品绑定结果不因 parser 下沉而改变。

### Scenario S-05: Invalid input fails explicitly

Given 上传为空、类型不支持、文件损坏、没有内容或表格结构非法

When shared parser 或业务 adapter 处理该输入

Then 系统返回对应的 BPM/MES 业务错误，不创建空模板或空报表，不切换到旧 parser，也不返回默认成功。

### Scenario S-06: Real and synthetic fixtures prevent structural regression

Given 仓库内真实 `pressure-pump-record.doc` 和最小合成 Word 表格

When 比较迁移前 MES parser 快照与共享 parser 加 MES adapter 的快照

Then 所有纳入合同的结构字段等价；任何差异必须有明确批准的合同变更，否则阻塞迁移。

## Functional Requirements

- `FR-01`：新增 `yudao-module-word-parser`，由根 parent reactor 管理；BPM 和 MES 可依赖它，它不得依赖 BPM、MES、数据库 starter、Flowable、Jimu 或任一业务模块。
- `FR-02`：共享模块必须提供单一 `SharedWordDocumentParser#parse(WordParseCommand)` 入口，命令包含非空源字节、源类型识别信息和固定 canonical profile。
- `FR-03`：`STRUCTURAL_CANONICAL` 必须固定提取表格外段落、页眉页脚、按文档顺序排列的顶层表格、原始行列、单元格文本、rowSpan、colSpan、由 Word 几何直接表达的列边界/宽度、行高、边框、斜线、字体粗细/字号、水平/垂直对齐和非敏感 diagnostics。共享结果不得包含批记录标题判定、共享页标题规则输出、拆分表格或 `sourceSplitIndex`；调用方不得关闭上述原始结构字段后仍声明解析一致。
- `FR-04`：共享结果模型只表达 Word 结构，不得引用 BPM 或 MES package/type，也不得包含业务标志、业务 id、路线、审批、版本、Jimu 或持久化信息。
- `FR-05`：迁移前必须建立 MES model 和相关 helper 的逐字段/逐规则归属清单；只有业务无关、直接来源于 Word 原始段落、顶层表格、行列/合并、几何或样式的信息可进入共享 DTO。`MesProBatchRecordSharedPageTitleRules`、批记录标题判定、`splitTemplates`、拆分索引及其他页面语义必须留在 MES adapter；无法明确其他字段归属时停止对应迁移。
- `FR-06`：MES adapter 必须消费共享原始结构，继续调用/承载 `MesProBatchRecordSharedPageTitleRules`、批记录标题判定和 `splitTemplates`，再映射为现有 `MesProBatchRecordParsedTable`、`MesProBatchRecordParsedCell` 和 `MesProBatchRecordDocumentFrame` 可观察合同。adapter 后最终模型（包括表格数量/顺序、标题和拆分索引）必须与迁移前快照等价，路线识别和后续报表逻辑继续消费 MES 类型。
- `FR-07`：BPM adapter 必须从共享结果中的表格外段落和表格单元格生成候选 label，并保留当前的空白清洗、长度不超过 80、首次出现去重、最多 300 字段、字段 code、类型猜测和必填猜测规则。
- `FR-08`：BPM 与 MES adapter 必须注入同一个共享 parser 类型并显式使用 `STRUCTURAL_CANONICAL`；不能各自复制 POI 遍历逻辑或配置成结构不等价的 options。
- `FR-09`：共享 parser 必须用稳定内部错误区分空内容、类型不支持、损坏源、非法表格结构和无可解析内容；不得捕获后返回空结果冒充成功。
- `FR-10`：业务 adapter 必须按批准设计映射错误：

| Shared condition | BPM mapping | MES mapping |
| --- | --- | --- |
| empty source | `TEMPLATE_SOURCE_INVALID` | `PRO_BATCH_RECORD_REPORT_FILE_EMPTY` |
| unsupported source type | `TEMPLATE_SOURCE_TYPE_UNSUPPORTED` | `PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID` |
| corrupt/unreadable Word | `TEMPLATE_SOURCE_INVALID` | `PRO_BATCH_RECORD_REPORT_PARSE_FAILED` |
| invalid table structure | `TEMPLATE_RECOGNITION_FAILED` | `PRO_BATCH_RECORD_REPORT_PARSE_FAILED` |
| no parseable content | `TEMPLATE_RECOGNITION_FAILED` | `PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID` |

- `FR-11`：BPM 识别失败时不得插入模板版本或写入空 `recognizedSchemaJson`；MES 解析失败时不得生成空报表、版本或绑定副作用。
- `FR-12`：以下外部合同必须保持：`/form-center/templates/import-doc` 及 `form:template:create`；`/mes/pro/batch-record-report/recognize-uploaded`；`/mes/pro/batch-record-report/upload-extra-slot`；各入口现有请求/响应、权限注解和文件类型校验保持原状。
- `FR-13`：现有业务事务继续由 `FormCenterRuntimeServiceImpl` 和 `MesProBatchRecordReportServiceImpl` 管理；共享 parser 必须无状态且不创建事务或数据库副作用。
- `FR-14`：共享 diagnostics 仅允许 parser version、source hash、扩展名、文件名 hash、段落/表格计数、warning code 和 failure code；不得包含上传字节、原始文件名或完整原文。
- `FR-15`：迁移完成后不得保留“共享 parser 失败则调用旧 BPM/MES parser”的路径，也不得保留第二套可继续分叉的 POI 文档结构解析实现。

## Non-Functional Requirements

- `NFR-01 Maintainability`：共享 parser 的 public API 和 DTO 必须稳定、业务中立；BPM/MES 差异只存在于 adapter 和业务服务层。
- `NFR-02 Determinism`：同一字节、源类型和 canonical profile 必须产生顺序稳定、可快照比较的结构结果。
- `NFR-03 Fail-fast`：缺文件、损坏、非法结构、依赖缺失或 fixture 缺失必须让测试或请求明确失败，禁止 assumption skip、fallback、mock success 和异常吞噬。
- `NFR-04 Security`：日志和 diagnostics 不得泄露源文件内容、原始文件名或敏感业务文本。
- `NFR-05 Compatibility boundary`：只保持已批准的外部和业务合同；不新增兼容 shim，也不保留重复 parser 作为“保险”。
- `NFR-06 Build isolation`：验证必须在 Java 17 Maven reactor 中使用 `-am` 构建依赖模块；不得依赖陈旧本地 Maven artifact。
- `NFR-07 Test reproducibility`：强制测试 fixture 必须提交到仓库或由测试内确定性生成，不得依赖桌面路径、其他项目目录或环境专属文件。
- `NFR-08 Concurrency safety`：实现、暂存和提交只包含本任务文件；发现并发任务触碰同一目标文件时停止协调。

## Dependencies and Constraints

- 语言与构建：Java 17、Maven multi-module reactor。
- Word 库：Apache POI 5.4.1，包含 `poi-ooxml` 和 `poi-scratchpad`。
- 目标依赖方向：`BPM -> word-parser`、`MES -> word-parser`，现有 `MES -> BPM` 保留；禁止 `word-parser -> BPM/MES` 和 `BPM -> MES`。
- 共享模块允许依赖项目公共基础模块的最小集合，但不得为日志、异常或 Spring 注入引入数据库/业务 starter。
- 真实 fixture：`IntRuoyiBackend/yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc`，必须作为强制 `.doc` 回归输入。
- 合成 fixture：测试内确定性创建最小表格，覆盖合并、宽度/行高、边框、页眉页脚和文本；不得按文件名触发专用规则。
- 既有验证基线包括 `MesProBatchRecordDocParserTest`、Route A/B/D/E/F tests、`MesProBatchRecordReportServiceImplDbTest`、报表 JSON/布局/Jimu tests、`FormCenterRuntimeContractTest` 和 `FormTemplateLifecycleServiceTest`。
- 当前工作区存在其他任务脏改动和不可读的旧 `target_corrupt_m4_20260802_1327` 目录；本任务不能清理或提交这些内容。
- 开发严格遵循 Gate 0 依赖边界、Gate 1 parser 核心、Gate 2 canonical profile、Gate 3 旧新等价、Gate 4 MES、Gate 5 BPM、Gate 6 前端合同、Gate 7 集成回归的顺序。

## Acceptance Criteria

- `AC-01`：自动化依赖合同验证根 POM 包含 `yudao-module-word-parser`，BPM/MES 依赖该模块，共享模块不依赖 BPM、MES、数据库 starter、Flowable、Jimu 或业务模块，BPM 不依赖 MES；Maven reactor 不出现循环依赖。
- `AC-02`：共享 parser 单元测试对有效 `.doc` 和 `.docx` 输出稳定的 paragraphs、document frame、按文档顺序排列的顶层 tables 和 diagnostics，并覆盖原始行列、合并、Word 几何列边界/宽度、行高、边框、斜线、字体和对齐字段；断言共享结果不包含批记录标题、`MesProBatchRecordSharedPageTitleRules` 输出、拆分表格或拆分索引。
- `AC-03`：静态合同或编译合同证明共享模块源码不引用 BPM/MES package，且共享 DTO 不含 `fillable`、`componentFlag`、`edhrCellRule`、`routeKey`、`batchRecordName`、`templateId`、Jimu、审批或版本字段。
- `AC-04`：BPM adapter 与 MES adapter 合同测试均验证调用 `SharedWordDocumentParser` 且 profile 精确为 `STRUCTURAL_CANONICAL`；不存在 caller-specific 关闭结构字段的配置。
- `AC-05`：对强制真实 fixture `pressure-pump-record.doc` 执行两层断言：第一层证明 BPM/MES adapter 对同一文档消费同一份 `STRUCTURAL_CANONICAL` 原始结构契约；第二层比较旧 MES parser 基线与“共享原始结构 + MES adapter”最终快照，断言表格数量/顺序、来源表索引、拆分索引、标题、行列、文本、rowSpan/colSpan、逻辑列、宽高、边框、斜线、字体/对齐和页眉页脚相等。共享 parser 自身不生成标题或拆表结果，整个测试不得被 assumption 跳过。
- `AC-06`：对不含模板名/产品名条件的最小合成表格执行“共享原始结构一致 + MES adapter 最终快照等价”的两层断言；共享原始结构覆盖至少一个横向合并、一个纵向合并、显式宽度/行高、显式边框、页眉、页脚和表格外段落，MES adapter 快照另外证明标题判定和 `splitTemplates` 拆表结果与迁移前相等。
- `AC-07`：BPM recognizer 测试证明共享结构中的段落和单元格都可形成字段，保持长度过滤、首次出现去重、300 上限、code、`input/date/checkbox/textarea` 和必填猜测规则；无可识别字段时明确失败。
- `AC-08`：MES 定向测试证明主批记录和附加槽位迁移后的 parsed models、Route A/B/D/E/F 识别、表格数校验、Jimu JSON、布局校准、版本和产品绑定合同无回归。
- `AC-09`：错误合同测试逐项覆盖空源、扩展名不支持、损坏 Word、非法表格结构和无内容，并断言得到 `FR-10` 指定的 BPM/MES 业务错误；测试同时断言没有模板版本、报表或绑定副作用，且不是通用 500/空成功。
- `AC-10`：Controller/静态合同测试证明三个既有 URL、请求/响应和权限注解保持不变；现有业务入口的 `.doc/.docx` 准入范围没有被共享 parser 扩大或缩小。
- `AC-11`：事务合同测试证明共享 parser 无数据库依赖和事务，BPM/MES 业务事务仍由原 service 控制，失败时由调用方回滚。
- `AC-12`：静态源码扫描和行为测试证明 BPM/MES 不再含第二套可运行的 POI 文档结构解析路径；共享 parser 失败后不会调用旧 parser、返回 mock/default/empty success 或吞异常。
- `AC-13`：diagnostics 测试证明结果只含批准的摘要字段，不含原始文件名、完整文本、源字节或 base64；错误日志合同使用 hash/错误码而非敏感内容。
- `AC-14`：前端静态合同证明 `TemplateApi.importTemplateDoc()`、`BatchRecordReportApi.recognizeUploadedRoute()` 和 `uploadExtraFormSlot()` 的 URL 与调用入口未改变，且失败后不会自动改打另一业务接口。
- `AC-15`：所有 Gate 0-6 定向命令在 Java 17 reactor `-am` 环境下通过，Surefire 报告为 0 failure/0 error，`git diff --check` 通过，并在任务日志记录对应 BDD、RED、GREEN 和 REGRESSION 证据。
- `AC-16`：若真实 fixture、依赖、文件系统、并发文件所有权或旧新快照任一前提失败，任务状态和测试报告明确记录 blocker 与影响，且不以跳过、fallback、手工截图或旧 parser 兜底标记完成。
