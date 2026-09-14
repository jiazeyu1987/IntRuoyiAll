# Test Plan

## Test Strategy

- 测试分为 ownership gate、strict RED、shared parser unit、MES adapter equivalence、BPM adapter、跨模块静态合同、focused regression 和 independent verification。
- 真实 `.doc` 强制使用 `IntRuoyiBackend/yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc`。fixture 缺失或不可读即 FAIL，不允许 `Assumptions`、本机绝对路径或跳过。
- 合成 `.docx` 由测试内确定性创建，不能按模板名、文件名、工序名或产品名触发解析分支。
- Maven 从 `IntRuoyiBackend` 运行，使用 reactor `-am`；PowerShell 中所有 `-D...` 参数整体加双引号。不得依赖本地已安装的陈旧 shared artifact。
- 同一时间只运行一个会写 shared/BPM/MES `target` 的 Maven 命令；运行前检查同模块 Java/Maven 进程，不能强杀无关任务。
- 外部 HTTP URL/权限和前端调用不变，因此不启动服务、不写数据库；controller reflection/MockMvc、service unit/DB test 和 Node 静态合同已覆盖本次批准范围。

## Task-Level Validation

### test_case_id: TC-OWN-01

- mapped_task_ids: [T1]
- mapped_acceptance_ids: [AC-03, AC-12, AC-16]
- environment or setup: Java 17；从 `IntRuoyiBackend` 使用 Maven reactor；ownership JSON 已新增但 main source 未修改。
- steps:
  1. 反射 `MesProBatchRecordParsedCell`、`MesProBatchRecordParsedTable`、`MesProBatchRecordDocumentFrame` 的字段。
  2. 与 `shared-word-parser-ownership.json` 逐项双向比对，检查无缺项、重复项或 `UNKNOWN/MIXED`。
  3. 检查 parser helper 分类含 raw POI extraction 与 MES title/split/grid/business 两组。
  4. 运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordWordParserOwnershipContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- expected_result: 清单精确覆盖全部字段/helper；`MesProBatchRecordSharedPageTitleRules`、`splitTemplates`、标题、拆分索引、fillable/cell rule/route/Jimu 等均明确留在 MES；测试 PASS。
- evidence: Surefire XML、命令输出摘要、ownership JSON 和 execution log T1 section。

### test_case_id: TC-RED-01

- mapped_task_ids: [T2, T3]
- mapped_acceptance_ids: [AC-01, AC-15, AC-16]
- environment or setup: T1 PASS；共享模块尚未创建；MES test 可编译并可定位 backend root POM。
- steps:
  1. 运行 `mvn -pl yudao-module-mes -am "-Dtest=SharedWordParserModuleBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  2. 确认测试到达 Surefire。
  3. 记录根 POM 未声明 shared module/模块 POM 不存在的预期断言失败。
- expected_result: RED 为 FAIL，且唯一原因是批准的 shared module 合同尚未实现；不是 Maven 环境、fixture 或依赖下载失败。
- evidence: execution log 中精确 `RED:` marker、Surefire failure 和失败断言。

### test_case_id: TC-SHARED-01

- mapped_task_ids: [T2, T3]
- mapped_acceptance_ids: [AC-02, AC-03, AC-04]
- environment or setup: `yudao-module-word-parser` 已进入 reactor；测试内生成固定 `.docx`，内容包含表格外段落、页眉、页脚和顶层表格。
- steps:
  1. 创建含横向合并、纵向合并、显式列宽、行高、边框、斜线、粗体、字号、水平/垂直对齐的最小 `.docx`。
  2. 以 `WordParseProfile.STRUCTURAL_CANONICAL` 调用 `SharedWordDocumentParser`。
  3. 断言 paragraphs/frame/top-level tables 顺序和 cell raw structure。
  4. 断言 shared result 没有 title decision、split table、`sourceSplitIndex` 或 MES 字段。
  5. 运行 `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordDocumentParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- expected_result: 所有 raw structure 字段按输入稳定还原，canonical 字段不可由 caller 关闭，共享 DTO 业务中立。
- evidence: Surefire XML、测试断言、GREEN marker。

### test_case_id: TC-SHARED-02

- mapped_task_ids: [T2, T3]
- mapped_acceptance_ids: [AC-02, AC-13]
- environment or setup: 同 TC-SHARED-01；同一 bytes/type/profile 可重复调用。
- steps:
  1. 对同一输入连续解析两次并比较完整 ordered result。
  2. 检查 diagnostics 只含 parser version、source hash、extension、file-name hash、计数和 warning/failure code。
  3. 搜索 diagnostics/toString/exception message，确认不含原始文件名、完整原文、源字节或 base64。
  4. 运行 `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordParserDiagnosticsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- expected_result: 两次结果完全相等；diagnostics 稳定且脱敏。
- evidence: Surefire XML、对象比较断言和敏感字段负向断言。

### test_case_id: TC-SHARED-03

- mapped_task_ids: [T2, T3]
- mapped_acceptance_ids: [AC-09, AC-12, AC-13]
- environment or setup: shared error contract 已定义；不连接数据库或外部服务。
- steps:
  1. 分别传入空 bytes、不支持扩展名、损坏 `.doc/.docx`、无内容 Word 和测试构造的非法表格结构。
  2. 断言稳定 shared failure code 区分 empty/unsupported/corrupt/invalid-structure/no-content。
  3. 断言没有空 `WordParseResult` success、fallback parser 调用或异常吞噬。
  4. 运行 `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordParserErrorContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- expected_result: 每种非法输入 fail fast 为指定错误；错误 diagnostics 不泄露内容。
- evidence: Surefire XML、异常 code/message 断言、GREEN marker。

### test_case_id: TC-SHARED-DOC-01

- mapped_task_ids: [T2, T3, T9]
- mapped_acceptance_ids: [AC-02, AC-05, AC-16]
- environment or setup: shared module test 通过向上定位 `IntRuoyiBackend/pom.xml` 找到 reactor root，再读取已跟踪 `yudao-module-mes/src/test/resources/fixtures/pressure-pump-record.doc`；不使用本机绝对路径或 `Assumptions`。
- steps:
  1. 读取真实 DOC bytes，以 `STRUCTURAL_CANONICAL` 直接调用 shared parser。
  2. 断言 paragraphs/document frame/top-level tables 非空且顺序稳定，至少核对 raw row/cell/span/geometry/style 字段。
  3. 连续解析两次并比较完整 raw result。
  4. 断言 shared result 不含批记录 title decision、split table 或 `sourceSplitIndex`。
  5. 运行 `mvn -pl yudao-module-word-parser -am "-Dtest=SharedWordDocumentParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 并检查真实 DOC case `skipped=0`。
- expected_result: shared parser 自身稳定解析真实 `.doc` 原始结构；fixture 缺失时测试硬失败。
- evidence: fixture path/hash 摘要、Surefire XML、raw result assertions、GREEN marker。

### test_case_id: TC-BOUNDARY-01

- mapped_task_ids: [T2, T3, T8]
- mapped_acceptance_ids: [AC-01, AC-03, AC-11, AC-12]
- environment or setup: backend reactor POMs 和 shared source 可读。
- steps:
  1. 解析 root/shared/BPM/MES POM，断言 `BPM -> word-parser`、`MES -> word-parser`，禁止 `word-parser -> BPM/MES` 与 `BPM -> MES`。
  2. 运行 `mvn -pl "yudao-module-word-parser,yudao-module-bpm,yudao-module-mes" -am dependency:tree`。
  3. 扫描 shared source，禁止 BPM/MES/database/Flowable/Jimu package/type/annotation。
  4. 扫描 shared DTO，禁止 PRD 中业务字段。
  5. 扫描 BPM recognizer 与 MES Word adapter，确认 raw POI document traversal 仅位于 shared parser；MES 其他 Excel/路线 POI 用法不作为误报。
- expected_result: 依赖无循环，共享模块无业务/事务/数据库依赖，BPM/MES 不再保留第二套 Word raw parser。
- evidence: dependency tree、boundary test Surefire XML、静态扫描摘要。

### test_case_id: TC-MES-RED-01

- mapped_task_ids: [T4, T5]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-15, AC-16]
- environment or setup: shared parser T3 GREEN；MES production parser 尚未迁移；real fixture 可读；legacy snapshot 已由迁移前 parser 固化。
- steps:
  1. 运行 legacy baseline 用例，确认 snapshot 与当前 parser 一致。
  2. 运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordSharedParserEquivalenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  3. 确认 shared parser spy/constructor/canonical adapter 断言失败。
- expected_result: RED 为 MES 尚未消费 shared canonical result 的预期 FAIL；real fixture 用例没有 skipped/aborted。
- evidence: baseline PASS、adapter RED failure、execution log 精确 RED marker。

### test_case_id: TC-MES-REAL-01

- mapped_task_ids: [T4, T5, T9]
- mapped_acceptance_ids: [AC-02, AC-04, AC-05, AC-16]
- environment or setup: 使用 classpath fixture `fixtures/pressure-pump-record.doc`；fixture helper 缺失时抛 `IllegalStateException`；测试类不使用 `Assumptions`。
- steps:
  1. 读取 real fixture bytes，并直接调用 shared parser 的 `STRUCTURAL_CANONICAL`。
  2. 证明 BPM/MES adapter 构造的 parse command 对相同 bytes 使用同一 parser contract/profile。
  3. 断言 shared raw result 只有 paragraphs/frame/top-level tables/raw geometry/style，没有批记录 title/split/index。
  4. 经 MES adapter 执行 title/split/grid 映射，与迁移前 baseline 比较表格数量/顺序、source indexes、split index、title、rows/cells、span、logical columns、width/height、borders、diagonal、font/alignment、frame。
  5. 检查 Surefire XML 的该用例 `skipped=0`、`errors=0`、`failures=0`。
- expected_result: shared raw result 业务中立；MES final snapshot 与迁移前完全等价；fixture 不可用时硬失败。
- evidence: snapshot diff 为 0、Surefire XML、fixture path/hash 摘要、GREEN marker。

### test_case_id: TC-MES-SYNTH-01

- mapped_task_ids: [T4, T5]
- mapped_acceptance_ids: [AC-04, AC-06]
- environment or setup: 测试内确定性生成 `.docx`；不使用模板/文件/产品/工序专名作为条件。
- steps:
  1. 生成含表格外段落、页眉页脚、横向/纵向合并、宽高、边框、斜线、字体/对齐且含两个可由既有 MES title rule 识别段落的顶层表格。
  2. 比较 shared raw result 与预期 raw structure。
  3. 分别用迁移前 baseline 和新 MES adapter 生成 final snapshot。
  4. 比较 title decision、split result、sourceSplitIndex 和全部结构字段。
- expected_result: raw 层不拆表，MES adapter 层拆表；新旧 MES final snapshot 完全相等。
- evidence: JUnit assertions、snapshot diff、GREEN marker。

### test_case_id: TC-MES-ERR-01

- mapped_task_ids: [T5, T8]
- mapped_acceptance_ids: [AC-09, AC-11, AC-12]
- environment or setup: MES adapter/service tests 使用 mock mapper/gateway；不连接真实数据库。
- steps:
  1. 对 empty、unsupported extension、corrupt Word、invalid table structure、no content 分别调用对应 service/adapter 入口。
  2. 断言映射到 `PRO_BATCH_RECORD_REPORT_FILE_EMPTY`、`PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID`、`PRO_BATCH_RECORD_REPORT_PARSE_FAILED` 或 `PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID`。
  3. 验证未调用 Jimu save、version insert、report insert、product/route binding。
  4. 检查 shared failure 后不存在 legacy parser 第二次调用。
- expected_result: 所有错误精确 fail fast，无通用 500、空成功、fallback 或副作用。
- evidence: JUnit error-code assertions、Mockito `never()`、Surefire XML。

### test_case_id: TC-MES-REG-01

- mapped_task_ids: [T5, T8, T9]
- mapped_acceptance_ids: [AC-08, AC-10, AC-15]
- environment or setup: Java 17 reactor；real fixture 和 synthetic tests 已 GREEN。
- steps:
  1. 运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest,MesProBatchRecordRouteFRecognizerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  2. 运行 `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  3. 检查 main/extra-slot、route、Jimu JSON、layout、version/product binding 断言。
- expected_result: 所有 focused MES regression PASS，0 failure/0 error；现有 controller/file-type 合同不变。
- evidence: 两组 Maven 输出摘要、Surefire XML test count、execution/test report。

### test_case_id: TC-BPM-RED-01

- mapped_task_ids: [T6, T7]
- mapped_acceptance_ids: [AC-04, AC-07, AC-09, AC-15]
- environment or setup: shared module与 MES migration GREEN；BPM production recognizer 尚未接入 shared；shared fake 可捕获 command。
- steps:
  1. 运行 `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  2. 确认当前 recognizer 无 shared parser constructor/call，canonical profile 或错误映射断言失败。
- expected_result: RED 原因是 BPM adapter 尚未实现，而不是环境失败；execution log 有精确 RED marker。
- evidence: compile/Surefire expected failure、execution log。

### test_case_id: TC-BPM-MAP-01

- mapped_task_ids: [T6, T7, T9]
- mapped_acceptance_ids: [AC-04, AC-07]
- environment or setup: fake `SharedWordDocumentParser` 返回固定 ordered paragraphs + tables；不使用 POI。
- steps:
  1. 调用 recognizer，捕获 `WordParseCommand` 并断言 profile 为 `STRUCTURAL_CANONICAL`。
  2. 断言段落在前、单元格按顶层表格文档顺序在后。
  3. 断言 clean blank、长度大于 80 过滤、首次出现去重和 300 上限。
  4. 覆盖 ASCII/non-ASCII code、input/date/checkbox/textarea 和 required 猜测。
  5. 对无候选字段断言明确 failure。
- expected_result: BPM 保持迁移前字段规则，只替换 raw structure 来源；不执行 MES title/split 规则。
- evidence: `DefaultWordFormTemplateRecognizerTest` Surefire XML、command captor 断言、GREEN marker。

### test_case_id: TC-BPM-ERR-01

- mapped_task_ids: [T6, T7, T8]
- mapped_acceptance_ids: [AC-09, AC-11, AC-12]
- environment or setup: runtime service 使用 mock mapper/approval orchestrator；shared parser fake 分别抛稳定 shared errors。
- steps:
  1. 覆盖 empty、unsupported、corrupt、invalid structure、no content。
  2. 断言映射到 `TEMPLATE_SOURCE_INVALID`、`TEMPLATE_SOURCE_TYPE_UNSUPPORTED` 或 `TEMPLATE_RECOGNITION_FAILED`，与 PRD FR-10 一致。
  3. 验证 template mapper insert/update、approval submit 均未调用。
  4. 验证错误没有被 catch 后转为空 `FormTemplateRecognition.success` 或普通 message fallback。
- expected_result: 精确业务错误，无通用 500/空成功/持久化副作用。
- evidence: error-code assertions、Mockito `never()`、Surefire XML。

### test_case_id: TC-BPM-REG-01

- mapped_task_ids: [T7, T8, T9]
- mapped_acceptance_ids: [AC-07, AC-10, AC-11, AC-15]
- environment or setup: BPM adapter GREEN；controller/main URL 未修改。
- steps:
  1. 运行 `mvn -pl yudao-module-bpm -am "-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormCenterRuntimeContractTest,FormTemplateLifecycleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  2. 检查 `/form-center/templates/import-doc`、`form:template:create`、request/response 和 lifecycle assertions。
- expected_result: BPM focused regression 全 PASS；模板只在 recognition success 后持久化。
- evidence: Maven 输出摘要、Surefire XML、controller reflection assertions。

## Integration and Static Contract Validation

### test_case_id: TC-API-01

- mapped_task_ids: [T8, T9]
- mapped_acceptance_ids: [AC-10, AC-14]
- environment or setup: `IntRuoyiFronted` Node runtime；不启动浏览器或服务；生产前端文件只读。
- steps:
  1. 运行 `node tests/e2e/shared-word-parser-api-contract-static.spec.js`。
  2. 断言 `TemplateApi.importTemplateDoc()` 仍调用 `/form-center/templates/import-doc`。
  3. 断言 `recognizeUploadedRoute()` 与 `uploadExtraFormSlot()` 仍调用既有 MES URL。
  4. 断言任一失败路径没有自动改打另一个业务 API。
  5. 配合后端 controller tests 检查 URL、权限和文件准入。
- expected_result: 三个 URL/调用入口/权限/文件类型合同不变，前端无跨业务 retry/fallback。
- evidence: Node PASS 输出、后端 controller Surefire XML、静态源码定位。

### test_case_id: TC-NOFALLBACK-01

- mapped_task_ids: [T3, T5, T7, T8, T9]
- mapped_acceptance_ids: [AC-03, AC-11, AC-12, AC-13]
- environment or setup: 所有生产迁移完成；source tree 可读，排除 `target*`。
- steps:
  1. 扫描 BPM recognizer、MES Word adapter，确认无 POI document open/traversal。
  2. 扫描 catch/exception 路径，确认 shared failure 后不调用 legacy parser、不返回空/默认/mock success、不吞异常。
  3. 扫描 shared models/source，确认无 MES/BPM/Jimu/DB/transaction imports 或业务字段。
  4. 扫描日志/diagnostics，确认无原文件名/全文/bytes/base64。
- expected_result: Word raw parsing 唯一实现位于 shared；adapter 只做业务映射；no-fallback 与脱敏合同成立。
- evidence: static boundary test、`rg` 摘要、independent test report。

### test_case_id: TC-REACTOR-01

- mapped_task_ids: [T8, T9]
- mapped_acceptance_ids: [AC-01, AC-02, AC-04, AC-05, AC-06, AC-07, AC-09, AC-15]
- environment or setup: Java 17、Maven、POI 5.4.1 可用；无其他进程写同模块 target。
- steps:
  1. 运行 `mvn -pl "yudao-module-word-parser,yudao-module-bpm,yudao-module-mes" -am "-Dtest=SharedWordDocumentParserTest,SharedWordParserDiagnosticsTest,SharedWordParserErrorContractTest,DefaultWordFormTemplateRecognizerTest,FormCenterTemplateImportRuntimeTest,FormCenterRuntimeContractTest,MesProBatchRecordSharedParserEquivalenceTest,MesProBatchRecordDocParserTest,MesProBatchRecordReportControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  2. 检查 reactor summary 与各模块 Surefire XML。
  3. 确认 required test 均实际执行，尤其 real DOC test 的 skipped/aborted 为 0。
- expected_result: reactor SUCCESS，0 failure/0 error；没有因 `-Dtest` 跨模块找不到测试导致误失败或误跳过。
- evidence: reactor summary、Surefire XML 列表/计数、execution/test report。

### test_case_id: TC-DIFF-01

- mapped_task_ids: [T8, T9]
- mapped_acceptance_ids: [AC-15, AC-16]
- environment or setup: 只检查本任务允许路径；不 stage/commit/push。
- steps:
  1. 运行 `git diff --check -- IntRuoyiBackend/pom.xml IntRuoyiBackend/yudao-module-word-parser IntRuoyiBackend/yudao-module-bpm IntRuoyiBackend/yudao-module-mes IntRuoyiFronted/tests/e2e/shared-word-parser-api-contract-static.spec.js doc/tasks/20260807-shared-word-parser-implementation`。
  2. 运行 `git status --short --` 加同一组目标路径，确认无意外文件。
  3. 对照 dev-plan write scope 审核每个 changed path。
- expected_result: diff check PASS；本任务没有修改前端生产代码、数据库、无关 MES/BPM 文件或并发任务文件。
- evidence: 命令输出摘要、changed-path inventory、independent report。

## Independent Verification Phase

### test_case_id: TC-IND-01

- mapped_task_ids: [T9]
- mapped_acceptance_ids: [AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16]
- environment or setup: T1-T8 已报告完成；tester 未参与实现，且只能写 `test-report.md`。
- steps:
  1. 独立重跑 TC-BOUNDARY-01、TC-SHARED-01/02/03、TC-SHARED-DOC-01、TC-MES-REAL-01、TC-MES-SYNTH-01、TC-MES-ERR-01、TC-MES-REG-01、TC-BPM-MAP-01、TC-BPM-ERR-01、TC-BPM-REG-01、TC-API-01、TC-NOFALLBACK-01、TC-REACTOR-01 和 TC-DIFF-01。
  2. 对每个 AC 记录 tested cases、actual/expected、PASS/FAIL、命令、测试数、failure/error/skipped 和 blocker。
  3. 明确检查真实 fixture 测试没有 assumption、fallback、截图替代或手工成功声明。
- expected_result: 全部 AC PASS、0 blocker、0 required skipped/aborted 才建议放行；任一失败则报告 FAIL，不修改代码。
- evidence: `doc/tasks/20260807-shared-word-parser-implementation/test-report.md`。

## Blocker and Fail-Fast Rules

- `pressure-pump-record.doc` 缺失、无法读取或只能 assumption skip：TC-MES-REAL-01 FAIL，停止 MES migration 放行。
- T1 归属清单出现未决字段/helper：停止 T2 及后续任务。
- dependency tree 出现 shared -> business、BPM -> MES 或循环：TC-BOUNDARY-01 FAIL，停止 adapter migration。
- 并发任务修改 root POM、MES parser/POM 或 BPM recognizer/POM：对应 task 阻塞，不能覆盖。
- Java 17/Maven/POI/reactor 未到达 Surefire：不能记作 RED 或 GREEN，记录环境 blocker。
- `target_corrupt_m4_20260802_1327` 阻止验证：记录非任务文件系统 blocker，不清理该目录。
- real/synthetic snapshot 任何无法解释漂移：TC-MES-REAL-01/TC-MES-SYNTH-01 FAIL，不保留旧 parser 兜底。
- required test 有 skipped/aborted、API contract 缺基线或 diagnostics 泄露：任务不能标记完成。
