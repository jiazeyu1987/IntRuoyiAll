# 执行日志：电子批记录报表视觉保真优化

## BDD

- BDD: 清除后重新生成报表 -> Given 测试租户中存在电子批记录报表入口和真实批记录模板 / When 用户通过真实前端点击 `清除电子批记录报表` 后再点击 `A 直接 doc` / Then 系统必须生成最新 Jimu 报表，并且不能沿用清除前旧报表或静默跳过失败。
- BDD: 源文档优先的布局对比 -> Given 源 Word 文档 `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 和重新生成的 Jimu 报表 / When 比较页头、页脚、分页节奏、表头层级、明细块结构、列宽比例、行高块高、跨行跨列、空白格、汇总区和清场区 / Then 差异必须按源 Word 为准记录，不能按截图或既有 Jimu 输出替换对比口径。
- BDD: 通用规则修复 -> Given 某个视觉差异可以由页型识别、行类型识别、版式求解、JSON 构建或渲染样式规则解释 / When 子 agent 实施修复 / Then 修复必须先有失败测试，且不得包含报表标题、工序名或表编号硬编码。
- BDD: 主 agent review -> Given 子 agent 返回一项修改 / When 主 agent review 其测试、作用域和规则通用性 / Then 只有通用、风险可控、测试完整且不回退他人改动的结果才能保留。
- BDD: A 路少列概览页保留满版宽度 -> Given 固定源 Word `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` 经 A 路 DocParser 解析出少列、概览信息页形态且源行宽接近页面预算 / When 构建 Jimu JSON / Then `dataRectWidth` 与 `area.width` 必须保留共享满版页面宽度，不得因列数少被压缩成窄版 portrait。
- BDD: 单包装重复设备矩阵保留源文档多行结构 -> Given 固定源 Word 中的单包装工序页包含重复设备矩阵和紧凑汇总/清场尾部 / When 经布局校准后构建 Jimu JSON / Then 文档页头只能保留一份，重复设备矩阵单元格必须保持多行内容，不得用过时的单行压缩或强制分页断言替代源文档结构。
- BDD: A 路低/中列工序页保留满版宽度 -> Given 固定源 Word 经 A 路识别和布局校准后得到源行宽接近页面预算的低/中列工序生产记录页 / When 构建 Jimu JSON / Then 应按共享页面预算保留横向满版宽度，不得因列数低于宽表阈值被压缩成 portrait。
- BDD: 任务专用端口真实验证 -> Given 用户要求前端和后端均换用非固定端口 / When 执行真实清除和 A 路生成 / Then 本轮验证必须使用前端 `18081` 和后端 `18083`，不得继续依赖 `8081` 或 `48081`。

## RED / GREEN

- GREEN: planner artifacts -> PASS, `request-analysis.md` and `prd.md` reviewed; acceptance criteria AC-01..AC-12 are concrete and testable.
- GREEN: plan artifacts -> PASS, `dev-plan.md`, `test-plan.md`, `task-state.json`, and `test-report.md` created by the main agent after planning review.
- GREEN: real frontend clear + A direct `.doc` -> PASS, sibling frontend Playwright script logged in as `测试租户 / aoteman`, opened `/report/jimu-report`, clicked `清空电子批记录报表`, then clicked `A 直接 .doc`; clear returned HTTP 200 with `deletedReportCount=15`, `deletedMetadataCount=0`; regenerate returned HTTP 200 with `importedCount=15`, `createdCount=0`, `updatedCount=15`.
- INFO: Round 0 source hash -> `D:\ProjectPackage\Int\IntRuoyi\resource\批记录模板.doc` SHA-256 is `830A89A2E116ACA4AB9ECD63A9345F5A288998DD1DDE4A434A612B7BA57C103E`.
- INFO: Round 0 visual source evidence -> Word COM conversion failed because the local COM provider is WPS and returned `文档打开失败`; existing source PDF baseline from `doc/tasks/20260522-electronic-batch-record-report-visual-fidelity-optimization/artifacts/source-batch-record-template.pdf` remains available with 19 rendered source pages.
- INFO: Round 0 Jimu evidence -> latest Route A viewer screenshots captured under sibling frontend task `doc/tasks/20260524-ebr-report-visual-fidelity/artifacts/jimu-route-a/`.
- INFO: Round 0 structural difference -> latest T01 `EBR_TN122_A_T01` generated as `layout=portrait`, `dataRectWidth=432`, `colCount=6`, while source page 1 is a landscape full-width table; assigned to worker T2 as a shared low-column overview width rule.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldKeepFullPageWidthForDocParsedLowColumnOverviewPages test` -> FAIL, A 路 `.doc` 少列概览形态页当前 `dataRectWidth` 为 432，期望 1120 满版宽度。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldKeepFullPageWidthForDocParsedLowColumnOverviewPages test` -> PASS, A 路 `.doc` 少列概览形态页 `dataRectWidth`、`area.width` 保留 1120，打印布局切换为 landscape。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldKeepFullPageWidthForDocParsedLowColumnOverviewPages+build_shouldUse1120WidthForFixedOverviewPage+build_shouldUseLandscapeA4ForWideCalibratedTables,MesProBatchRecordReportShapeRulesTest' test` -> PASS, 相关 JSON 构建宽度回归 3 条与 ShapeRules 10 条通过。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldInsertContinuationHeadersBeforeLaterOverviewSections+calibrate_shouldClampWideTableIntoSinglePageWidthBudget' test` -> PASS, 概览续页与宽表校准相关回归 2 条通过。
- INFO: Worker T2 implementation -> 在 `MesProBatchRecordReportShapeRules` 增加少列概览满版宽度形态识别：列数不超过共享窄页阈值、行结构呈概览/信息页、多行网格或满宽行覆盖、源行宽接近页面预算；`MesProBatchRecordReportJsonBuilder` 对该形态下超过预算的原始列宽按 1120 共享预算收敛，避免每列被 72px 上限压窄；未按 `产品信息`、表序号、报表编码或工序名硬编码。
- INFO: Broader JsonBuilder class check -> `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest test` -> FAIL, 43 条中 2 条既有 T13/单包装工序断言失败：`build_routeAT13_shouldKeepPagingRowAfterSyntheticHeaderRemoval`、`build_routeAT13_shouldKeepOneLineEquipmentMatrixRowsCompactInLandscapeWidePages`；失败页型为单包装工序分页/设备矩阵，不走本次少列概览宽度分支，留待主 agent 按 T13 方向处理。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 43 条中 2 条 T13 断言与源文档结构冲突：一条要求紧凑尾部仍保留 `pagingRow`，另一条要求重复设备矩阵存在单行压缩行；当前源文档和真实生成结果均表现为单份页头、多行设备矩阵、紧凑尾部不额外强制分页。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_routeAT13_shouldKeepSingleSyntheticHeaderAfterRemoval+build_routeAT13_shouldKeepRepeatedEquipmentMatrixRowsMultiLineInLandscapeWidePages -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, T13 断言改为源文档优先的单份页头与多行设备矩阵结构校验。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 43 条 JSON builder 回归全部通过。
- GREEN: `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, 当前 worktree 后端已重新打包出 `yudao-server\target\yudao-server.jar`，用于真实清除与 A 路重新生成验证。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldKeepFullPageWidthForDocParsedLowOrMediumColumnProcessPages -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 低/中列工序页命中源行宽满版形态后，sourceTableIndex=5 当前 `dataRectWidth=720`，期望共享页面预算 `1044`。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldKeepFullPageWidthForDocParsedLowOrMediumColumnProcessPages -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 低/中列工序页按源行宽接近满版且存在工序生产记录标题的通用形态保留共享页面预算。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 44 条 JSON builder 回归全部通过。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportShapeRulesTest,MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldInsertContinuationHeadersBeforeLaterOverviewSections+calibrate_shouldClampWideTableIntoSinglePageWidthBudget -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, ShapeRules 10 条与布局校准邻近回归 2 条通过。
- GREEN: `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, 已重新打包包含低/中列工序页满版规则的 worktree 后端 jar。
- GREEN: backend runtime on `http://127.0.0.1:18083/actuator/health` -> PASS, 按用户要求未使用 `48081` 作为本轮验证后端端口。
- GREEN: frontend runtime on `http://127.0.0.1:18081` -> PASS, 按用户要求未使用 `8081` 作为本轮验证前端端口，且源码探针确认加载的是当前 worktree 前端。
- GREEN: Playwright clear + A direct `.doc` via `18081 -> 18083` -> PASS, `deletedReportCount=15`, `importedCount=15`, `createdCount=0`, `updatedCount=15`。
- GREEN: Route A viewer capture via backend `18083` -> PASS, 重新采集 15 张 Jimu viewer 截图。
- BDD: A 路打包物料矩阵必须展开为结构化网格 -> Given 固定源 Word 中部分工序页把两组 `物料编码/物料名称/批号` 与若干 `/ + 物料名` 明细打包在一个大 cell 中 / When 进入工序页布局校准 / Then 校准必须把该大 cell 展开成表头行和明细行，不能继续以单个 merge 区渲染。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `组装Ⅰ工序生产记录` 仍保留打包物料矩阵 cell，未展开成结构化行。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 打包物料矩阵 cell 已按共享规则展开。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages+calibrate_shouldStretchSharedProcessColumnsToTargetWidthEvenWhenOnlyDetailRowsStayNarrow+calibrate_shouldReserveMoreWidthForDenseTailColumns -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 新规则与两条相邻宽度回归共同通过。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, LayoutCalibrator 31 条全绿。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, JsonBuilder 44 条全绿。
- GREEN: `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, 已重新打包包含打包物料矩阵展开规则的后端 jar。
- GREEN: Playwright clear + A direct `.doc` via `18081 -> 18083` after packed-matrix expansion -> PASS, `deletedReportCount=15`, `importedCount=15`, `createdCount=0`, `updatedCount=15`。
- GREEN: Route A viewer capture via backend `18083` after packed-matrix expansion -> PASS, 重新采集 15 张 viewer 截图，T06/T15 的物料矩阵已拆成结构化 header + detail rows。
- BDD: 源 DOC 显式边框样式必须贯通到生成边框 -> Given `.doc` parser 能读取 cell 四边边框码 / When 解析固定源文档并构建 Jimu JSON / Then 显式边框样式必须进入 `ParsedCell` 并在 `JsonBuilder` 中优先于通用边框推断。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordDocParserTest#parseFixedSourceDoc_shouldCaptureExplicitCellBorderStyles,MesProBatchRecordReportJsonBuilderTest#build_shouldHonorExplicitSourceBorderStylesWhenPresent -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 固定源文档至少存在显式 cell 边框样式，且 `JsonBuilder` 会优先使用显式边框。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordDocParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, DocParser 6 条全绿。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, LayoutCalibrator 31 条全绿。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, JsonBuilder 45 条全绿。
- GREEN: `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, 已重新打包包含显式边框样式贯通能力的后端 jar。
- GREEN: Playwright clear + A direct `.doc` via `18081 -> 18083` after source-border propagation -> PASS, `deletedReportCount=15`, `importedCount=15`, `createdCount=0`, `updatedCount=15`。
- GREEN: Route A viewer capture via backend `18083` after source-border propagation -> PASS, 重新采集 15 张 viewer 截图；当前改动主要为基础边框能力，无回归。
- BDD: process 页 `生效日期` footer 必须保持紧凑高度 -> Given 固定样本中的 process 页 footer 文本均为单行 `生效日期：...` / When 经过布局校准并构建 Jimu JSON / Then footer 行高度应保持紧凑，不得被默认行高或估高逻辑重新撑胖。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldKeepEffectiveDateRowsCompactForFixedRouteAProcessPages -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `精洗工序生产记录` footer 行高度为 25，高于期望的紧凑高度 20。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldKeepEffectiveDateRowsCompactForFixedRouteAProcessPages+calibrate_shouldExpandPackedMaterialMatrixCellsIntoStructuredRowsForRouteAProcessPages -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, footer 紧凑化规则与打包物料矩阵展开规则共同通过。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, LayoutCalibrator 32 条全绿。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, JsonBuilder 45 条全绿。
- GREEN: `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, 已重新打包包含 footer 紧凑化规则的后端 jar。
- GREEN: Playwright clear + A direct `.doc` via `18081 -> 18083` after footer compaction -> PASS, `deletedReportCount=15`, `importedCount=15`, `createdCount=0`, `updatedCount=15`。
- GREEN: runtime DB verification after footer compaction -> PASS, `EBR_TN122_A_T03/T06/T15` 的最后一行 `生效日期：2026年02月02日` 高度均已收敛到 20。
- INFO: task-closeout-cleanup preview @ backend repo -> BLOCKED for apply, preview would keep `task.md` / `execution-log.md` and delete intermediate docs/JSON artifacts, but linked worktree fast-forward merge and pending-change gates were not satisfied; no cleanup deletion was applied.

## 对比轮次

- Round 0：已完成真实清除与 A 路重新生成；已捕获源 Word hash、源 PDF baseline、15 张 Route A Jimu viewer 截图；首要差异是少列概览/信息页宽度被压缩。
- Round 1：保留少列概览页通用规则后，T01 从 `portrait/dataRectWidth=432` 收敛到 `landscape/dataRectWidth=1120`；剩余 T06/T09/T10/T11/T14/T15 低/中列工序页仍为窄 portrait。
- Round 2：新增低/中列工序页满版通用规则后，全 A 路 15 张报表均为 `landscape`；T06/T10/T11/T14/T15 收敛到 `1044`，T09 收敛到 `1120`。
- Round 3：按用户要求改用端口 `18081 -> 18083` 重新执行真实清除、A 路生成、DB 结构取证与 15 张 viewer 截图；结构证据写入 `round3-route-a-jimu-structure-port18083.json`。
- Round 4：新增“打包物料矩阵展开”共享规则后，T06/T15 等页的上半块不再是单个物料大 merge 区，而是拆成 `物料编码/物料名称/批号` 两组表头和明细行；真实 `18081 -> 18083` 重新生成与 viewer 截图已复验。
- Round 5：把源 `.doc` cell 四边边框样式接入 `ParsedCell` 与 `JsonBuilder`，并在 `18081 -> 18083` 真实重新生成后确认无回归；这为后续按源边框继续逼近斜线格和块边界提供了模型基础。
- Round 6：在 `LayoutCalibrator` 中为 `FOOTER` 行单独保留紧凑高度，重新生成后固定样本 process 页的 `生效日期` 行已从 23/24/28 收敛到 20。

## 子 agent 拆分与 review

- Explorer B：确认真实入口在 `报表管理 -> 报表设计器 -> 六路识别`，按钮为 `清空电子批记录报表` 与 `A 直接 .doc`；同时指出测试租户没有 `/mes/pro/batch-record-template` 菜单。
- Explorer A：确认后端链路为 controller `recognizeFixedRoute/deleteAllGeneratedReports` -> service -> Route A recognizer -> DOC parser -> Jimu gateway/json builder/layout/style rules；指出 `delete-all` 当前只删 Jimu 报表、不删本地元数据。
- Explorer C：确认源 Word、DB/Jimu JSON 取证位置和视觉对比工具；指出本机缺 LibreOffice/Poppler，Word COM 实际由 WPS provider 提供且可能失败。
- Planner：产出并提交 `request-analysis.md` 与 `prd.md`；主 agent review 通过。
- Worker T2：已分配“少列概览页宽度通用规则”修复，要求先补 RED 再改共享规则。
- Worker T13-test：已分配“修正 T13 陈旧测试断言”窄范围任务，但子 agent 服务返回 503；主 agent 接管并只修改测试语义与执行日志，未追加生产代码。
- Main review：保留 Worker T2 的通用规则修改；保留 T13 测试语义修正，因为它删除的是与源文档结构相冲突的旧断言，没有放宽生产规则或引入标题/表号硬编码。
- Main implementation：在主 agent 侧补充低/中列工序页满版通用规则，因为 Round 1 真实对比仍显示同类源行宽满版页被压窄；规则依据列密度、源行宽和工序页型，不按工序名、表号或报表编码硬编码。
- Main implementation：在 `LayoutCalibrator` 增加“打包物料矩阵展开”规则，只识别带侧边块标题、且单个大 cell 内含两组 `物料编码/物料名称/批号` 与 `/ + 物料名` 列表的共享形态，把它展开成表头行和明细行，不按工序标题或表号硬编码。
- Main implementation：把 `TableCell.getBrcTop/Bottom/Left/Right()` 解析出的显式边框样式写入 `ParsedCell`，`LayoutCalibrator.cloneCell` 保留这些源边框信号，`JsonBuilder` 在存在显式边框时优先使用源样式而非纯通用推断。
- Main implementation：在 `LayoutCalibrator.resolveRowHeights(...)` 为 `FOOTER` 行增加紧凑分支，不再从默认 28 行高起步，避免 `生效日期` 被重新估高。

## 最终结论

- 保留修改：少列概览页满版宽度规则、低/中列工序页满版宽度规则、T13 源文档优先测试语义修正、打包物料矩阵展开规则、源 `.doc` 显式边框样式贯通能力、process 页 footer 紧凑化规则、前端响应解包和错误暴露修复。
- 测试结果：后端目标测试、JSON builder 全类、Shape/Layout 邻近回归、后端打包、前端定向 node 测试、eslint、真实 Playwright 生成均通过；前端全量 `pnpm run ts:check` 仍被既有 `src/api/showroom-admin/version-center.ts` 类型错误阻塞，未涉及本任务文件。
- 真实生成验证：`18081 -> 18083` 清除 15 张并 A 路重新生成 15 张；DB/Jimu 结构显示全 A 路为 landscape，且 T06/T15 的打包物料矩阵已拆成结构化 header + detail rows，块结构进一步逼近源 Word。
- 剩余差异：斜线格语义仍未真正恢复，因为当前模型还没有显式对角线/内部子分区字段；部分块高/行高、页脚位置与底部留白节奏、局部空白格和清场区细节仍需后续按源文档继续精调。
