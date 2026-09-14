# Dev Plan

## Planning Decisions

- 以已批准 PRD 为实现边界：共享模块只产出业务无关的 canonical 原始 Word 结构，不产出批记录标题、拆分表格或 `sourceSplitIndex`。系统设计中早期“共享结果含拆分表格”的建议由 PRD `FR-03`、`FR-05`、`FR-06` 覆盖。
- 模块名固定为 `yudao-module-word-parser`，Java 包固定为 `cn.iocoder.yudao.module.wordparser`。
- `MesProBatchRecordSharedPageTitleRules`、`splitTemplates`、标题提取、拆分索引、视觉/逻辑输出网格归一化、packed label 判定、fillable/cell rule/Jimu/路线规则全部留在 MES adapter。
- 共享模块可以包含直接由 Word 原始文件决定的段落、页眉页脚、顶层表格、原始行列/合并、几何、样式、文本规范化和脱敏 diagnostics；不得依赖 BPM、MES、数据库、Flowable 或 Jimu。
- 本次不修改数据库，不修改前端生产代码，不启动服务，也不运行写数据 E2E。外部接口稳定性通过后端 controller 合同和前端 API 静态合同验证。
- 所有 executor 在写入前必须重新检查其 `affected_paths` 的 Git 状态；发现并发任务改动同一文件即阻塞，不能覆盖或合并猜测。

## Delivery Graph

```text
T1 ownership inventory
  -> T2 shared/boundary RED tests
    -> T3 shared parser implementation
      -> T4 MES equivalence RED tests
        -> T5 MES adapter migration
          -> T6 BPM adapter RED tests
            -> T7 BPM adapter migration
              -> T8 cross-module contracts and regression
                -> T9 independent verification
```

任务按上述顺序执行。即使代码写入范围不重叠，也不得并行运行 Maven 命令，因为 `-am` 会让多个任务同时写共享 reactor 的 `target`。

## Task Graph

### task_id: T1

- title: 建立迁移前字段与 helper 归属清单门禁
- objective: 在任何生产代码迁移前，对 `MesProBatchRecordParsedCell`、`MesProBatchRecordParsedTable`、`MesProBatchRecordDocumentFrame` 的每个字段和 `MesProBatchRecordDocParser` 的相关 helper 逐项标注 `SHARED_RAW_STRUCTURE` 或 `MES_ADAPTER_SEMANTICS`，并以自动化测试锁定完整性。
- dependency_ids: []
- affected_paths:
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordWordParserOwnershipContractTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/resources/contracts/shared-word-parser-ownership.json`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 只允许新增上述 test/resource 文件，并在 execution log 追加 T1 证据；不得修改任何 main source 或 POM。
  - 清单必须覆盖三个 MES parsed model 的全部字段及 parser 中参与 POI 抽取、标题、拆表、网格归一化的全部 helper。
  - `MesProBatchRecordSharedPageTitleRules`、`extractTemplateTitle`、`splitTemplates`、`sourceSplitIndex`、`fillable`、`visualBlank`、`reviewedCellRule`、`cellRuleSource`、`placeholder`、`inputType`、`routeBSource`、Jimu/路线/版本字段必须明确归为 MES；禁止用 `UNKNOWN`、`MIXED` 或默认归类。
- acceptance_ids: [AC-03, AC-12, AC-16]
- validation_steps:
  - 校验 fixture `IntRuoyiBackend/yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc` 可读；缺失立即阻塞。
  - 运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 测试必须反射三个 MES model 并与清单字段精确比对，同时检查禁止进入 shared 的 MES 语义条目完整存在。
  - 在 execution log 记录清单审查结论和 GREEN 命令；T1 不创建生产代码 RED。
- done_definition: 归属清单无遗漏、无未决项并通过自动化完整性测试；若任一字段/helper 无法归属，T1 阻塞且 T2 不得开始。

### task_id: T2

- title: 先写共享 parser、依赖边界与 diagnostics 的 RED 测试
- objective: 在共享模块和 adapter 生产实现前，创建可重复的测试合同，并取得一个由“共享模块尚不存在”导致的稳定 RED。
- dependency_ids: [T1]
- affected_paths:
  - `IntRuoyiBackend/yudao-module-word-parser/src/test/java/cn/iocoder/yudao/module/wordparser/**`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/SharedWordParserModuleBoundaryTest.java`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 只允许新增 shared 模块 test source 和 MES 静态边界 test，并追加 T2 日志；不得新增 shared POM、main source 或修改现有 POM。
  - shared tests 必须用测试内确定性生成的最小 `.docx`，覆盖表格外段落、页眉页脚、横向/纵向合并、显式宽度/行高、边框、斜线、字体/字号和对齐。
  - shared tests 必须通过向上定位 backend reactor root 的方式读取已跟踪 `yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc`，直接验证 shared `.doc` raw structure；禁止本机绝对路径和 assumption。
  - 测试必须覆盖空源、不支持类型、损坏源、无可解析内容、非法结构、canonical profile、determinism 和 diagnostics 脱敏。
- acceptance_ids: [AC-01, AC-02, AC-03, AC-13, AC-15, AC-16]
- validation_steps:
  - 运行 `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 预期 RED：断言根 `pom.xml` 尚未声明 `yudao-module-word-parser`，或共享模块 POM 尚不存在；失败必须到达 Surefire，不接受环境/依赖下载失败冒充 RED。
  - 在 execution log 精确记录 `RED: <command> -> FAIL, <expected reason>`。
  - 对新增测试执行源码检查，确认没有 JUnit `Assumptions`、本机绝对 fixture 路径、文件名/模板名特例或 mock success。
- done_definition: RED 原因唯一且稳定，shared parser 的结构、错误、diagnostics 和边界合同已先于生产实现落盘。

### task_id: T3

- title: 实现业务中立的共享 Word canonical parser
- objective: 新增 `yudao-module-word-parser`，实现单一 `SharedWordDocumentParser#parse(WordParseCommand)` 及 `.doc/.docx` 原始结构解析，使 T2 的共享与依赖边界测试转 GREEN。
- dependency_ids: [T2]
- affected_paths:
  - `IntRuoyiBackend/pom.xml`
  - `IntRuoyiBackend/yudao-module-word-parser/pom.xml`
  - `IntRuoyiBackend/yudao-module-word-parser/src/main/java/cn/iocoder/yudao/module/wordparser/**`
  - `IntRuoyiBackend/yudao-module-word-parser/src/test/java/cn/iocoder/yudao/module/wordparser/**`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/SharedWordParserModuleBoundaryTest.java`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 允许修改 root backend POM、创建 shared module POM/main source，并仅按编译需要修正 T2 测试；不得修改 BPM/MES main source 或其 POM。
  - public contract 必须包含 `SharedWordDocumentParser`、`WordParseCommand`、`WordParseProfile.STRUCTURAL_CANONICAL`、稳定错误类型、`WordParseResult`、业务中立结构 DTO 和脱敏 diagnostics。
  - 实现只能迁入 T1 标记为 `SHARED_RAW_STRUCTURE` 的逻辑；不得复制或引用任何 MES title/split/grid-business helper。
  - diagnostics 只能包含 parser version、source hash、扩展名、文件名 hash、计数、warning/failure code，不得包含原始文件名、完整文本、字节或 base64。
- acceptance_ids: [AC-01, AC-02, AC-03, AC-11, AC-13]
- validation_steps:
  - 运行 `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 运行 `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest,MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 运行 `mvn -pl yudao-module-word-parser -am dependency:tree`，检查 shared 不依赖 BPM/MES/database/Flowable/Jimu/业务模块。
  - 检查同一 bytes/source type/profile 连续解析两次得到相同有序结构。
  - 在 execution log 记录 T2 对应 RED 转为 GREEN 的命令和摘要。
- done_definition: shared module 在 reactor 中可独立构建；`.doc/.docx` canonical 结构、错误和 diagnostics 测试通过；共享源码无 BPM/MES 语义或依赖。

### task_id: T4

- title: 先写 MES adapter 真实 DOC 与合成表格等价 RED
- objective: 在修改 `MesProBatchRecordDocParser` 前固化迁移前 MES 输出，并创建“共享原始结构 + MES adapter”尚未接线导致的 RED。
- dependency_ids: [T3]
- affected_paths:
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordSharedParserEquivalenceTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/resources/contracts/pressure-pump-parser-baseline.json`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordDocParserTest.java`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 只允许新增等价测试/基线资源，及为移除 pressure-pump 强制用例中的 assumption 而修改现有 parser test；不得修改 MES main source 或 POM。
  - 真实 fixture 必须使用已跟踪的 `pressure-pump-record.doc`；新强制用例不得 import/call `Assumptions`。
  - 基线必须由迁移前 parser 的 deterministic canonical snapshot 形成，并覆盖表格顺序、来源索引、拆分索引、标题、行列、文本、span、逻辑列、宽高、边框、斜线、字体/对齐和 frame。
  - 合成 `.docx` 必须在测试内生成，不得按文件名、模板名、产品名或工序名触发规则。
- acceptance_ids: [AC-02, AC-04, AC-05, AC-06, AC-08, AC-16]
- validation_steps:
  - 先运行迁移前 baseline 断言，确认旧 parser 对 real/synthetic 输入稳定且 fixture 缺失会 FAIL。
  - 再运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordSharedParserEquivalenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 预期 RED：MES parser 仍无 shared parser 构造/调用合同，或共享 raw result 尚未经过 MES title/split adapter；失败必须是预期断言或编译合同，不得是 fixture skip。
  - 在 execution log 记录 real DOC 与 synthetic 两个 BDD 场景和 RED。
- done_definition: 迁移前快照已固化且可重复；真实 fixture 强制执行；MES shared adapter 的失败合同已先于生产迁移存在。

### task_id: T5

- title: 将 MES Word parser 收敛为 shared parser adapter
- objective: 让所有 MES Word 入口通过 `SharedWordDocumentParser` 的 `STRUCTURAL_CANONICAL` 结果，再在 MES 内执行 title/split/grid/business 映射，同时保持最终 parsed model、路线和报表行为。
- dependency_ids: [T4]
- affected_paths:
  - `IntRuoyiBackend/yudao-module-mes/pom.xml`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordDocParser.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteARecognizer.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteBRecognizer.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteERecognizer.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteFRecognizer.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/**`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 只允许修改 MES POM、Word adapter 及因 constructor injection/错误映射直接受影响的 batchrecordreport service/route recognizer/test；不得修改其他 MES 域、BPM、shared main source、前端或数据库。
  - `MesProBatchRecordDocParser` 保留为 MES adapter，可以保留现有 `parse`/`parseDocx` 业务入口签名，但内部不得再打开 POI 文档或包含第二套 raw parser。
  - `MesProBatchRecordSharedPageTitleRules`、`splitTemplates`、网格归一化、`sourceSplitIndex`、fillable 和其他 MES 语义继续留在 MES；禁止移动到 shared。
  - 清除 production 中 `new MesProBatchRecordDocParser()` 的隐式 parser 创建，使用明确 constructor injection；不得新增“shared 失败后旧 parser”分支。
  - MES 模块仍有 Excel/其他 POI 使用，不能为了本任务误删模块级 POI 依赖；边界测试应检查 Word raw parsing 代码唯一，而不是禁止 MES 全模块使用 POI。
- acceptance_ids: [AC-04, AC-05, AC-06, AC-08, AC-09, AC-11, AC-12]
- validation_steps:
  - 运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，确认 T4 RED 转 GREEN。
  - 运行 Route A/B/D/E/F：`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest,MesProBatchRecordRouteFRecognizerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 运行报表回归：`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 静态检查 `MesProBatchRecordDocParser` 无 `org.apache.poi` import，且无 catch 后调用 legacy parser/返回空成功。
  - 核对 real fixture 等价测试无 skipped/aborted，Surefire 为 0 failure/0 error。
- done_definition: MES adapter 对 real/synthetic 输入与迁移前最终快照等价；主表单/附加槽位、路线和报表定向回归通过；MES raw Word 解析只有 shared 一条路径。

### task_id: T6

- title: 先写 BPM shared adapter 与失败不落库 RED
- objective: 在修改 BPM recognizer 前，用 fake shared parser 固定 canonical 调用、字段映射顺序/规则和 shared error 到 BPM error 的合同。
- dependency_ids: [T5]
- affected_paths:
  - `IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/DefaultWordFormTemplateRecognizerTest.java`
  - `IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterTemplateImportRuntimeTest.java`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 只允许新增/修改上述 BPM test 和追加 T6 日志；不得修改 BPM main source 或 POM。
  - fake parser 必须捕获 `WordParseCommand` 并断言 profile 精确为 `STRUCTURAL_CANONICAL`，不得返回 mock success 来绕过待测 adapter 分支。
  - 测试固定顺序为表格外段落在前、顶层表格单元格按文档顺序在后，统一 clean blank、80 长度、首次出现去重和最多 300 个字段。
  - 覆盖 input/date/checkbox/textarea、required、无字段、empty/unsupported/corrupt/invalid-structure/no-content 以及失败时 mapper `insert/update` 从未调用。
- acceptance_ids: [AC-04, AC-07, AC-09, AC-11, AC-16]
- validation_steps:
  - 运行 `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 预期 RED：当前 recognizer 不接受/不调用 shared parser，canonical profile/错误映射断言失败；失败必须到达 test compile 或 Surefire。
  - 在 execution log 记录 BPM BDD 和 RED 精确原因。
- done_definition: BPM adapter 的调用、字段规则、错误码和无副作用合同均已在生产修改前形成稳定 RED。

### task_id: T7

- title: 将 BPM Word recognizer 切换到 shared parser
- objective: 删除 BPM 自有 POI 遍历，实现 shared result 到 `FormTemplateRecognition` 的单一 adapter，并保持模板生命周期和外部错误合同。
- dependency_ids: [T6]
- affected_paths:
  - `IntRuoyiBackend/yudao-module-bpm/pom.xml`
  - `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/DefaultWordFormTemplateRecognizer.java`
  - `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/model/FormTemplateRecognition.java`
  - `IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/DefaultWordFormTemplateRecognizerTest.java`
  - `IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterTemplateImportRuntimeTest.java`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 允许修改 BPM POM、recognizer、仅为精确错误传播/无副作用所需的 runtime/recognition model，以及 T6 tests；不得修改 controller URL/权限、MES、shared main source、前端或数据库。
  - recognizer 必须 constructor-inject `SharedWordDocumentParser` 并固定使用 `STRUCTURAL_CANONICAL`。
  - 删除 BPM 内 `HWPFDocument`/`WordExtractor`/`XWPFDocument` 遍历；shared 失败不得调用旧方法、返回空成功或吞异常 message。
  - 保持既有 BPM 字段 code/type/required/去重/上限规则，精确实现 PRD `FR-10` 的 BPM 错误映射。
- acceptance_ids: [AC-04, AC-07, AC-09, AC-10, AC-11, AC-12]
- validation_steps:
  - 运行 `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，确认 T6 RED 转 GREEN。
  - 运行 `mvn -pl yudao-module-bpm -am "-Dtest=FormCenterRuntimeContractTest,FormTemplateLifecycleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 静态检查 `DefaultWordFormTemplateRecognizer` 不再 import POI，且无 broad catch 将所有 shared 错误降级为普通 failure。
  - 断言解析失败前没有 template version insert/update 或 `recognizedSchemaJson` 写入。
- done_definition: BPM 使用 shared canonical parser；字段识别行为和生命周期合同通过；所有 shared 错误映射明确且失败无持久化副作用。

### task_id: T8

- title: 补齐跨模块静态合同并执行集成回归
- objective: 用自动化静态合同锁定依赖方向、API URL/权限/文件准入、无 fallback、无重复 Word raw parser 和前端不跨接口重试，并执行 Gate 0-6 的定向回归。
- dependency_ids: [T7]
- affected_paths:
  - `IntRuoyiFronted/tests/e2e/shared-word-parser-api-contract-static.spec.js`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/SharedWordParserModuleBoundaryTest.java`
  - `IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/controller/FormCenterRuntimeContractTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/MesProBatchRecordReportControllerTest.java`
  - `doc/tasks/20260807-shared-word-parser-implementation/execution-log.md`
- write_scope:
  - 只允许新增前端静态 test、增强现有后端合同 test 和追加 T8 日志；不得修改前端生产代码、controller 生产代码、数据库或解析生产实现。
  - 若回归发现生产缺陷，本任务标记 FAIL 并交由 supervisor 新建有 RED 的修复任务；T8 不在测试任务内顺手修 production。
- acceptance_ids: [AC-01, AC-03, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16]
- validation_steps:
  - 运行 `mvn -pl "yudao-module-word-parser,yudao-module-bpm,yudao-module-mes" -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormCenterRuntimeContractTest,MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 分别运行 T5 的 Route A/B/D/E/F 和报表/Jimu/DB service 回归命令，避免一个过长 `-Dtest` 隐藏失败来源。
  - 在 `IntRuoyiFronted` 运行 `node tests/e2e/shared-word-parser-api-contract-static.spec.js`。
  - 运行 `git diff --check -- IntRuoyiBackend/pom.xml IntRuoyiBackend/yudao-module-word-parser IntRuoyiBackend/yudao-module-bpm IntRuoyiBackend/yudao-module-mes IntRuoyiFronted/tests/e2e/shared-word-parser-api-contract-static.spec.js doc/tasks/20260807-shared-word-parser-implementation`。
  - 检查所有相关 Surefire XML：0 failures、0 errors、real fixture case 0 skipped/aborted。
- done_definition: Gate 0-6 的依赖、shared、MES、BPM、API/权限、前端静态和回归命令全部通过；任何失败均有精确 blocker，未以 skip/fallback 标记成功。

### task_id: T9

- title: 独立验证与放行判定
- objective: 由未参与 T1-T8 实现的 tester 独立按 test plan 重跑关键验证，检查 acceptance coverage 和 no-fallback 边界，并只写测试报告。
- dependency_ids: [T8]
- affected_paths:
  - `doc/tasks/20260807-shared-word-parser-implementation/test-report.md`
- write_scope:
  - tester 只能新增/更新 `test-report.md`；不得修改 product source、POM、测试代码、execution log、task-state 或其他任务文档。
  - tester 发现失败只记录实际/预期/证据/blocker，不得自行修复。
- acceptance_ids: [AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16]
- validation_steps:
  - 按 `test-plan.md` 的 independent phase 重跑 dependency boundary、shared parser、real DOC equivalence、synthetic equivalence、MES/BPM focused regression、frontend static contract 和 `git diff --check`。
  - 对照每个 AC 记录 PASS/FAIL、命令、测试数、失败数、跳过数和 blocker。
  - 验证 `pressure-pump-record.doc` 用例没有 assumption/skip，且实现代码不存在 legacy fallback。
- done_definition: `test-report.md` 给出可审计的全 AC 独立结论；只有全部必须项 PASS 且 0 blocker 才可建议进入 closeout。

## Conflict Analysis

- `IntRuoyiBackend/pom.xml` 是 reactor 高风险共享文件，只允许 T3 修改；T2/T8 只能读取或在测试中断言。写入前若已被并发任务修改，T3 必须阻塞协调。
- `IntRuoyiBackend/yudao-module-mes/pom.xml` 与 `MesProBatchRecordDocParser.java` 是 MES 高风险共享文件，只允许 T5 修改。T1/T4/T8 只能改其明确列出的测试文件。T4 必须先完成快照，T5 才能迁移 parser。
- `IntRuoyiBackend/yudao-module-bpm/pom.xml` 与 `DefaultWordFormTemplateRecognizer.java` 只允许 T7 修改。T6 必须先取得 RED，T7 才能实现。
- T1/T2/T4/T6/T8 会追加同一 `execution-log.md`，因此所有任务按依赖串行，禁止并发写日志。
- T2/T3、T4/T5、T6/T7 会先后接触相同测试文件；后续任务只能做让既定 RED 转 GREEN 所需的最小修正，不得弱化、删除或改写断言含义。
- 所有 Maven `-am` 命令会写 shared/BPM/MES 的 `target`，即使代码 write scope 不重叠也禁止并行运行。发现其他 Java/Maven 进程正在写相同模块 target 时，记录环境冲突并等待/协调，不得强杀无关进程。
- `IntRuoyiBackend/yudao-module-mes/target_corrupt_m4_20260802_1327` 为非任务损坏产物，本计划不读取、清理、重命名或删除它；若 Maven/Git 因它无法完成目标验证，按 AC-16 阻塞。

## Execution Gates

1. Gate 0: T1 归属清单全部明确，fixture 可读。
2. Gate 1 RED: T2 边界测试因 shared module 缺失而在 Surefire 失败。
3. Gate 1 GREEN: T3 shared canonical parser、依赖边界、错误和 diagnostics 通过。
4. Gate 2 RED: T4 real/synthetic equivalence 因 MES 尚未接入 shared 而失败。
5. Gate 2 GREEN: T5 MES adapter、真实 DOC、合成表格和 MES 回归通过。
6. Gate 3 RED: T6 BPM canonical adapter/错误映射测试因 recognizer 尚未接入 shared 而失败。
7. Gate 3 GREEN: T7 BPM adapter、字段规则、生命周期和错误回滚通过。
8. Gate 4 REGRESSION: T8 跨模块静态合同和 Gate 0-6 定向回归通过。
9. Gate 5 INDEPENDENT: T9 独立测试报告全 AC PASS；否则返回 supervisor 进入有界修复/复测循环。
