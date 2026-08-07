# Request Analysis

## User Goal

用户要求完成设计、开发和验证，使“表单模板”导入不再使用 BPM 自有的简化 Word 文本提取实现，而是与 MES“批记录表单”共用同一套 Word 结构解析算法。

本请求中的“一致”定义为：两个业务入口对同一 Word 字节流使用同一个共享 parser、同一个 canonical profile，并消费同一份业务无关的 Word 原始结构结果；该结果只包含段落、页眉页脚、顶层表格、原始行列/合并关系和通用样式等信息。之后 BPM 与 MES 仍分别执行自己的业务映射，因此最终的 `FormRecognizedField` 与 `MesProBatchRecordParsedTable` / Jimu 报表不要求相同；MES 经 adapter 完成标题判定和拆表后的最终模型必须与迁移前快照等价。

用户已经同意 `docs/system/shared-word-template-parser-design.md` 中的总体方案：把 MES 现有的纯 Word 解析能力下沉到独立共享模块，BPM 和 MES 通过各自 adapter 使用它，禁止 BPM 直接依赖 MES。

## Current System

### Repository evidence inspected on 2026-08-07

- 仓库根目录是 `E:/IntRuoyi`，当前分支是 `int_main`，跟踪 `origin/int_main`。
- 检查时分支领先远端 11 个提交，且存在其他并发任务的已修改和未跟踪文件；这些改动不属于本任务，不能修改、回滚或混入本任务提交。
- `IntRuoyiBackend/pom.xml` 当前包含 BPM 和 MES 模块，但没有共享 Word parser 模块。
- `yudao-module-mes/pom.xml` 当前依赖 `yudao-module-bpm`。因此新增 BPM -> MES 依赖会形成循环依赖，不能采用。
- BPM 与 MES 当前都直接声明 Apache POI 5.4.1 的 `poi-ooxml` 和 `poi-scratchpad`。
- `yudao-module-mes/target_corrupt_m4_20260802_1327` 含操作系统报告为损坏且无法读取的旧构建目录。它不是本任务产物，不得擅自删除；若它阻止构建或 Git 验证，必须作为环境 blocker 报告。

### BPM form-template path

- 外部入口是 `POST /form-center/templates/import-doc`，权限是 `form:template:create`。
- `FormCenterRuntimeServiceImpl#importDoc` 读取文件、校验扩展名、调用 `FormTemplateRecognizer`，识别失败或字段为空时抛出 `TEMPLATE_RECOGNITION_FAILED`，成功后才写入模板版本及 `recognizedSchemaJson`。
- `DefaultWordFormTemplateRecognizer` 当前直接使用 POI：
  - `.docx` 提取文档段落和顶层表格单元格文本；
  - `.doc` 通过 `WordExtractor#getParagraphText()` 提取段落文本；
  - 对文本执行去空白、长度上限 80、去重和最多 300 字段过滤；
  - BPM 自己生成字段 code、猜测 `input/date/checkbox/textarea` 和必填状态；
  - 当前没有独立的 `DefaultWordFormTemplateRecognizer` 测试类。
- BPM 当前只持久化字段识别结果，不持久化完整 Word 布局。因此“共享解析”不会自然令 BPM 最终表单与批记录报表版式相同。

### MES batch-record path

- 外部入口包括 `POST /mes/pro/batch-record-report/recognize-uploaded` 和 `POST /mes/pro/batch-record-report/upload-extra-slot`。
- `MesProBatchRecordDocParser` 当前直接使用 POI，覆盖 `.doc` / `.docx` 的表格、行列、合并、逻辑列、视觉列宽、行高、边框、斜线、字体、对齐、页眉页脚、页数上下文和表格拆分。
- 该 parser 当前直接产出 MES 类型 `MesProBatchRecordParsedTable`、`MesProBatchRecordParsedCell` 和 `MesProBatchRecordDocumentFrame`，并直接映射 MES 错误码；纯 Word 结构与 MES 类型边界尚未拆开。
- MES 服务依据现有业务入口先校验文件，再选择 `parse` 或 `parseDocx`，并继续执行路线识别、表格数量校验、布局/Jimu 生成、版本治理和产品绑定。
- 当前主路线导入的扩展名限制与附加槽位、BPM 入口并不完全相同；共享 parser 支持 `.doc` 和 `.docx` 不代表可以扩大任何现有 HTTP 入口的文件类型合同。
- 已有 `MesProBatchRecordDocParserTest` 覆盖合成 `.docx` 表格、页眉页脚、行高、下划线空白和斜线边框等结构，也覆盖真实 `.doc` 样本。
- 仓库内存在强制可用的真实 fixture：`yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc`；`BatchRecordReportTestFixtures` 在 fixture 缺失时抛异常。另有部分测试使用机器本地路径和 JUnit assumption，这些不能作为本任务的强制通过证据。

### Approved target boundary

- 建立下层共享模块，任务实现名称确定为 `yudao-module-word-parser`。
- 共享模块只负责业务无关的 Word 原始结构解析和无敏感内容的 diagnostics，输出段落、页眉页脚、顶层表格、原始行列/合并关系和通用样式；它不依赖 BPM、MES、数据库、Flowable、Jimu 或其他业务模块。
- BPM adapter 将共享结构映射为 `FormTemplateRecognition`；字段 code、类型、必填、去重和数量限制仍属于 BPM。
- MES adapter 将共享原始结构映射为现有 MES parsed models；`MesProBatchRecordSharedPageTitleRules`、批记录标题判定、`splitTemplates` 拆表、路线、fillable、componentFlag、单元格业务规则、Jimu 和版本逻辑均保留在 MES，禁止进入共享模块。
- 两个 adapter 必须使用 `WordParseProfile.STRUCTURAL_CANONICAL`；不得通过 caller-specific options 形成两套结构解析行为。

## Constraints

- 必须遵守严格 no-fallback：共享 parser 失败后不得调用旧 parser，不得返回空成功、默认成功、mock 数据或吞异常。
- 必须使用 BDD + 严格 TDD，先建立稳定 RED，再实现最小 GREEN，并保留精确的 RED/GREEN/REGRESSION 命令证据。
- 必须先通过 Maven 依赖方向门禁，再迁移生产解析逻辑。
- BPM 不得依赖 MES；MES 已依赖 BPM，反向依赖会形成循环。
- 共享模型不得含 `fillable`、`componentFlag`、`edhrCellRule`、`routeKey`、`batchRecordName`、`templateId` 等业务字段。
- BPM 与 MES 必须共用同一个 canonical structural parse；最终业务模型允许且预期不同。
- 现有 HTTP URL、请求/响应、权限、事务边界、模板生命周期、批记录版本、路线和产品绑定合同保持不变。
- 不得借共享 parser 扩大某个现有入口允许的文件扩展名。
- 第一阶段不修改前端，不新增数据库表或迁移，不新增跨业务通用上传 Controller。
- 必须同时使用提交到仓库的真实 `.doc` fixture 和最小合成表格验证；不得用本机绝对路径、assumption skip、截图或 API-only 结果替代强制测试。
- 禁止按模板名、表单名、工序名、产品名或文件名增加解析特例。
- Maven 验证应使用 reactor `-am` 构建共享模块和兄弟模块；PowerShell 中所有 `-D...` 参数整体加引号。
- 当前工作区有并发任务。只允许修改本任务文件，并在每个阶段检查目标路径是否出现非本任务并发改动。

## Unknowns

以下未知项必须在对应实现门禁前解析，不允许猜测：

1. `MesProBatchRecordParsedCell`、`MesProBatchRecordParsedTable` 和 `MesProBatchRecordDocumentFrame` 的逐字段归属尚未形成正式清单。迁移前必须逐字段标注“纯结构”或“MES 业务”，以决定共享 DTO 与 adapter 映射。
2. 视觉网格边界、逻辑列和宽度归一化 helper 的逐方法归属仍需在实现前形成清单；只有完全由 Word 原始几何信息决定、且不识别批记录页面语义的通用计算才可进入 canonical parser。`MesProBatchRecordSharedPageTitleRules`、批记录标题判定和 `splitTemplates` 拆表的归属已经确定为 MES adapter，不属于待选择项，禁止迁入共享模块。
3. BPM 对共享表格结构的第一阶段映射顺序需要通过 RED 固定。推荐保持当前可观察行为：文档段落在前，表格单元格按文档顺序在后，统一清洗和首次出现去重；若共享 parser 暴露的实际文档顺序不同，必须由测试和评审作出显式决定。
4. 共享异常是否直接穿过 `DefaultWordFormTemplateRecognizer`，还是先映射为 `FormTemplateRecognition.failure`，需以目标错误码合同决定。无论实现形式如何，外部必须得到设计规定的 BPM/MES 业务错误，且不能退化为通用 500。
5. 当前仓库没有共享 parser 模块的既有 package 命名规范样例。实现应使用与 artifact 一致的 `cn.iocoder.yudao.module.wordparser` 包边界，并由依赖合同测试锁定；如发现父 POM 约束冲突则停止并修订计划。

## Risks

- 直接搬移 1400 余行 MES parser 容易把批记录业务字段带入共享模块，造成伪共享和后续耦合。
- 只让 BPM 调用 MES facade 会产生 Maven 循环依赖，并让 BPM 隐式继承 MES 错误码与业务语义。
- 文本规范化、遍历顺序、拆表、逻辑列或宽度换算的轻微变化会导致 MES Jimu JSON 和 BPM 字段列表漂移。
- 旧 parser 与新 parser 同时保留为可运行路径，会形成隐藏 fallback 和长期分叉。
- 当前 BPM recognizer 捕获所有异常并只返回 message；迁移时若继续宽泛吞异常，可能把损坏文件误报为普通“无字段”。
- 依赖本机路径或 assumption 的测试可能在缺 fixture 时显示跳过而非失败，形成假通过。
- 共享 diagnostics 若记录原始文件名、完整文本或上传字节，会泄露生产文件信息。
- 共享分支并发改动可能污染 staged 文件或使测试结果不再对应当前 diff。
- 已损坏的旧构建目录可能导致 `rg`、Git 状态或 Maven 文件遍历报错；不得通过删除非任务产物来掩盖。
- Java/Maven 的并发构建可能同时写相同 `target`，造成陈旧产物、锁等待或错误测试结论。

## Validation Surface

- 依赖与模块边界：父 POM modules、共享模块 POM、BPM POM、MES POM及依赖树静态合同。
- 共享 parser 单元测试：`.doc`、`.docx`、段落、页眉页脚、顶层表格、原始行列/合并、通用几何与样式、空文件、错误扩展名、损坏文件、无内容和非法结构；共享结果不得出现批记录标题或拆表结果。
- canonical profile 合同：BPM/MES adapter 均注入同一 parser 并固定使用 `STRUCTURAL_CANONICAL`。
- 一致性与迁移等价：对 `pressure-pump-record.doc` 和最小合成表格先证明 BPM/MES 消费同一份 canonical 原始结构，再比较迁移前 MES parser 与“共享原始结构 + MES adapter”的最终快照；最终快照比较包含标题判定和拆表结果。
- MES 业务回归：`MesProBatchRecordDocParserTest`、Route A/B/D/E/F、报表 JSON、布局校准、Jimu 网关、DB service、controller URL/权限合同、主表单与附加槽位。
- BPM 业务回归：新增 recognizer 测试、`FormCenterRuntimeContractTest`、`FormTemplateLifecycleServiceTest`，覆盖字段映射和失败不落库。
- 前端静态合同：三个现有 API wrapper URL 和调用入口不变，失败后不自动切换到另一业务接口。
- 安全与错误：共享错误到 BPM/MES 错误码的精确映射，diagnostics 不含原文、原始文件名或字节。
- 工程验证：reactor 定向测试、必要的模块回归、`git diff --check`、任务 evidence validator、独立测试报告和推送状态。

## Blocking Prerequisites

规划阶段未发现必须向用户索取的新业务输入；已批准的系统设计、Java 17/Maven 基线和仓库内真实 `.doc` fixture 均存在。

执行阶段遇到以下任一条件必须立即停止对应门禁并记录影响，不得继续猜测或降级：

- `pressure-pump-record.doc` 无法读取、被替换，或真实 `.doc` 等价测试只能通过 assumption 跳过。
- 无法完成 MES parsed model 的纯结构/业务字段归属清单。
- Maven 依赖树出现 shared -> BPM/MES/数据库/Flowable/Jimu、BPM -> MES 或其他循环依赖。
- 当前并发任务修改了本任务计划写入的 POM、parser、adapter 或目标测试文件，导致文件所有权冲突。
- Java 17、Maven、POI 5.4.1 或 reactor 依赖不可用，目标测试未到达 Surefire。
- 损坏目录或并发 Maven 使目标 source/target 不可读写，且不能在不修改他人产物的条件下完成隔离验证。
- 旧新结构快照发生无法解释的漂移，或真实 DOC / 合成 fixture 不能稳定重现。
- 现有 URL、权限、文件类型、事务或持久化合同缺少可自动验证的基线，且继续迁移会使行为是否改变无法判定。
