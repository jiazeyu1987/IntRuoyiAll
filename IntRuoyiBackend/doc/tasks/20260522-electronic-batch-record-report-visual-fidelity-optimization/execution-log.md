# Execution Log: 电子批记录报表视觉保真优化

## Pass 0

- Status: initialized
- Notes: task directory and initial task document created before production code changes.

## Pass 1

- BDD: 清空后 Route A 重新生成 -> Given 当前测试租户可访问 `报表管理 -> 报表设计器 -> 六路识别`, When 用户点击 `清空电子批记录报表` 后再点击 `A 直接 .doc`, Then 系统应真实生成 15 张 Route A 报表，并保持清空与生成都来自当前租户的共享规则。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `recognizeFixedRoute_withTenantContext_usesTenantScopedReportCodes` 与 `importImage_withTenantContext_usesTenantScopedReportCode` 先后暴露 `EBR_A_T01` / `FIXED_DOC` 级别的跨租户唯一键冲突。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 15/15 通过，租户维度 `reportCode` 与 `sampleKey` 隔离生效。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, 新 jar 已重打并用于 48081 live 重启。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `recognize-fixed?routeKey=A` 返回 `importedCount=15`，`createdCount=15`，最新 reportCode 为 `EBR_TN122_A_T01..T15`。
- Notes: 真实预览截图仍存在空白/叠压捕获问题，当前已从 `/jmreport/view` 直接访问切换为修正 token 解壳后的报表截图采集；首轮视觉对比可确认的主差异为第 1 张报表顺序错误以及大量“请填写”控件叠压遮挡表格。

## Pass 2

- BDD: Route A 首页保持源 Word 前导块顺序 -> Given 源 Word 第 1 页从 `产品信息` 开始并在同页继续承载配件批号、装配及包装、过程放行等前导块, When Route A / Doc parser 解析共享页头与短标题块, Then 不应把页尾 `装配及包装信息` 裁成第 1 张独立报表，而应保留前导汇总块并让第 1 张报表继续以 `产品信息` 为首。
- BDD: 紧凑填报网格页不应再被统一大尺寸控件撑坏 -> Given 多列窄格、短高度、密集空白填写区的报表页, When JSON builder 为这些空白格生成 fillForm 控件, Then 应按共享规则压缩控件尺寸、隐藏紧凑占位文案并给行高保底，避免控件叠压把表格撑坏。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 首张标题错误落成 `装配及包装信息`。
- GREEN: 同一 parser / Route A 定向命令 -> PASS, 8/8 通过，首张恢复为 `产品信息`。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 36/36 通过，紧凑填报网格规则与占位文案收缩规则生效。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, accepted parser / compact-fill 规则已进入最新 48081 live 包。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 返回 `deletedReportCount=15`、`deletedMetadataCount=15`，随后 `recognize-fixed?routeKey=A` 再次返回 15 张最新报表。
- GREEN: 真实生成首张对比 -> PASS, 最新 `firstReport.reportName` 已从 `电子批记录[A]-表1-装配及包装信息` 回到 `电子批记录[A]-表1-产品信息`。
- Notes: 紧凑 fillForm 规则已经降低控件尺寸并隐藏占位文案，但 live 截图仍显示密集空白格上的输入框数量过多、对表格留白和块高影响仍偏大；同时部分报表右侧边界仍呈锯齿形，未保持稳定矩形轮廓，下一轮需把这两个问题一起纳入共享版式规则继续收敛。

## Pass 3

- BDD: 共享宽度预算下的报表轮廓应保持矩形 -> Given 同一张工序报表内存在上部检查块、主操作块、清场块等多个区段, When JsonBuilder 为缺失列补空并生成 fillForm, Then 右侧补齐列不应再被渲染成一串 trailing 输入框，整体右边界应优先保持统一矩形轮廓。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotCreateFillFormControlsForTrailingPaddingColumns -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 右侧 trailing padding 列不再生成 fillForm 控件。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, trailing padding 规则已进入最新 live 包。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 再次返回 `15/15`，Route A 再次返回 `15` 张最新报表。
- Notes: live 截图显示右缘上多余的小输入框已有收敛，但整体右边界仍未完全矩形化；探查结论显示下一层更深的根因很可能在 `LayoutCalibrator` 的行形状/colSpan 归一化与 `JsonBuilder.placeCells()` 缺少越界 fail-fast。

## Pass 4

- BDD: Route A 工序页 JSON 结构不能越过声明列边界 -> Given `检测工序`、`光固Ⅱ`、`中包装` 等 Route A 工序页包含 rowSpan 侧栏和重复结构行, When `LayoutCalibrator` 归一化行形状并由 `JsonBuilder.placeCells()` 放置单元格, Then 任何单元格和 merge 结束列都不得超过已声明 `columnCount`，live 右边界必须落回矩形预算内。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_routeAFixedTables_shouldKeepCellsWithinDeclaredColumnBudget -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 固定样本 Route A 校准后在 `row 5` 直接越过声明列预算。
- GREEN: 同一条 calibrator 定向命令 -> PASS, Route A 固定样本所有表在校准后都能保持在声明列预算内。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 86/86 通过。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, 矩形边界修复已进入最新 live 包。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 再次返回 `15/15`，Route A 再次返回 `15` 张最新报表。
- GREEN: live JSON 结构校验 -> PASS, 最新 `EBR_TN122_A_T11`、`EBR_TN122_A_T12`、`EBR_TN122_A_T14` 的 `violation_count` 均为 `0`，声明列边界已不再被越界。
- Notes: 右侧“结构性锯齿”已经从 JSON 越界层修正到预算内，但视觉上仍有控件密度较高导致的局部不平整，下一轮优先继续压 fillForm 密度与块高。

## Pass 5

- BDD: 结构性补空行和汇总/清场语义行应减少控件噪音 -> Given 结构已回到矩形预算内的工序页, When JsonBuilder 为空白格生成 fillForm, Then trailing padding 列、汇总语义行、清场语义行不应继续补出无意义输入框，同时紧凑输入格应带更轻的控件外观参数。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotAutoFillSummaryPaddingCells+build_shouldNotCreateFillFormControlsForTrailingPaddingColumns -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 汇总行空白格仍带 fillForm。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldCompactDenseFillGridControlsAndHideCompactPlaceholders -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, compact fill 尚未输出轻量 `props`。
- GREEN: 上述 JsonBuilder 定向测试已全部通过，汇总/清场补空控件被抑制，compact fill 规则输出 `props.border=false` 与 `props.size=small`。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 87/87 通过。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, 本轮减密规则已进入最新 live 包。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 再次返回 `15/15`，Route A 再次返回 `15` 张最新报表。
- GREEN: live fillForm 数量复核 -> PASS, `EBR_TN122_A_T11` 当前 fillForm 数量为 `50`，`EBR_TN122_A_T14` 为 `38`，比上一轮进一步减少；`生产批量汇总` 与清场区补空输入框已被清除。
- Notes: 结构性锯齿与汇总/清场无意义白框已继续收敛，但正文明细录入区仍有较多真实输入框，视觉上仍不够像纸面空白格；下一轮重点将转向 Jimu 输入控件的更轻外观或进一步区分“真实录入列”与“仅结构留白列”。

## Pass 6

- BDD: 真实输入列在保留可填写性的前提下应弱化白框外观 -> Given 明细录入区仍然保留真实 fillForm, When 网关为电子批记录 Jimu 报表统一注入共享 `cssStr`, Then fillForm 容器、`ivu-input`、`textarea` 应去白底、去边框、去圆角，尽量接近纸面空白格的视觉表现。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `cssStr` 已持久化到 Jimu 报表实体。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 91/91 通过。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, `cssStr` 规则已进入最新 live 包。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 再次返回 `15/15`，Route A 再次返回 `15` 张最新报表。
- GREEN: live 渲染样式复核 -> PASS, 最新 `EBR_TN122_A_T11` / `EBR_TN122_A_T14` 的 `css_str` 长度均为 `562`，浏览器里 `.fillForm-box`、`.ivu-input`、`.ivu-input-wrapper` 的边框与背景已被压成透明；截图中原本大面积白框已明显淡化。
- Notes: 这轮视觉收敛较大，明细区白框已从“厚白块”变成更接近纸面空白格的轻量轮廓，但仍可继续微调分页留白和页头节奏。

## Pass 7

- BDD: 叙述型低明细工序页不应再被双重单页压缩吃掉分页留白 -> Given Route A 固定样本中的 `检测工序生产记录` 以页头、前检叙述块、主检叙述块、汇总块和清场块为主且明细行较少, When `LayoutCalibrator` 与 `JsonBuilder` 共同应用单页高度预算, Then 共享规则应对这类页型放宽到 relaxed single-page budget，而不是继续压到 `632px` 一类的过低占页高度。
- BDD: 固定矩阵页不应被同一放宽规则连带撑高 -> Given `粗洗工序生产记录` 这类明细矩阵较多的固定页仍需要保持紧凑块高, When 主 agent 引入 relaxed single-page budget, Then 放宽规则必须限定在叙述型低明细工序页，不能把粗洗等矩阵页一并抬高。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotOverCompressFixedRouteADetectionPageIntoLargeBottomWhitespace -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `检测工序生产记录` live-like 页在最终 JSON 中仅保留 `totalHeight=632`，占页高度不足。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 首版放宽规则把 `粗洗工序生产记录` 一并抬到 `totalHeight=670`、`detailRowHeight=32`，暴露出放宽范围过宽。
- GREEN: 同一条 JsonBuilder 定向命令 -> PASS, `检测工序生产记录` 已回到 `>= 660px` 的 relaxed budget 区间。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 放宽规则已收窄到叙述型低明细工序页，粗洗矩阵页恢复原有紧凑预算。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 92/92 通过。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, relaxed single-page budget 规则已进入最新 live 包。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 返回 `deletedReportCount=15`、`deletedMetadataCount=15`，`recognize-fixed?routeKey=A` 再次返回 `15` 张最新报表。
- GREEN: live 截图复核 -> PASS, 最新 `T11` 非白像素高度由上一轮约 `649px` 提升到 `685px`，`T14` 当前约 `847px`；`检测工序生产记录` 的下半段留白已收敛，仍残留但不再是上一轮的 `632px` 级别过压。
- Notes: `artifacts/source-pdf-pages/source-page-01.png` 到 `source-page-19.png` 当前 hash 完全一致，不能继续作为可靠页级基线；本轮对源稿分页节奏的判断改以 `artifacts/source-batch-record-template.pdf` 实页为准，后续仍需补更稳定的源 PDF 页渲染脚本。

## Pass 8

- BDD: 文档头部块应为打印和预览保留更接近源 Word 的顶部留白 -> Given 组装Ⅱ、检测等 Route A 工序页都以前置文档头部块开头, When JsonBuilder 输出最终 Jimu JSON, Then 顶部不应再让文档头直接贴到 canvas 顶边，而应通过共享规则保留顶端留白并填充 `fixedPrintHeadRows`。
- BDD: 重复文档头块应在续页位置前形成可见的新页间隔 -> Given 单包装等长工序页在中段已插入重复文档头, When JsonBuilder 渲染这些重复文档头块, Then 续页头前应出现可见分页留白，而不是继续和上一段表体紧贴。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 第一次引入 JSON 页装饰后暴露 `build_shouldAllowLiveLikeDenseProcessPagesToExceedTheGeneric650pxJsonCap` 总高阈值未计入顶部留白，以及 `build_shouldPreserveCompactedOneLineDetailHeightsOnStructuredTailPages` 仍按原始行号读取 JSON 行的测试口径问题。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldReserveTopWhitespaceAndFixedPrintHeaderForDocLikeHeaderBlocks+build_shouldInsertVisiblePageGapBeforeRepeatedDocLikeHeaderBlocks -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 顶部留白与续页头前分页留白的共享规则已进入 JSON builder。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 94/94 通过。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> FAIL, 无关 DCC 模块 `DccControlledFileController` 当前存在 `RequestParam` / `TenantIgnore` / `PermitAll` 缺失符号，阻塞整包 `yudao-server` 重新构建。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -DskipTests package` -> PASS, 当前任务直接相关的 `yudao-module-mes` jar 成功重打。
- GREEN: 作用域受控的 live 包替换 -> PASS, 将新的 `yudao-module-mes-2026.04-SNAPSHOT.jar` 按原 `STORED` 嵌套条目格式替换进现有 `yudao-server.jar`，生成 `output/runtime/backend-mespatched-stored-20260522-152103.jar` 并成功启动到 `48081`；首版替换因 nested jar 压缩格式不对导致 `/admin-api/mes/pro/batch-record-report/*` 路由丢失，已显式定位并修正。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 返回 `deletedReportCount=15`、`deletedMetadataCount=15`，`recognize-fixed?routeKey=A` 再次返回 `15` 张。
- GREEN: live 截图复核 -> PASS, `T10` 当前非白像素高度约 `692px`，文档标题已完整露出；`T13` 续页头前可见留白较上一轮再扩大一档，但仍未达到源 Word 连续页间距。

## Pass 9

- BDD: 源 Word 页级基线必须来自真实 PDF 渲染，而不是重复的浏览器截图 -> Given `source-page-01.png` 到 `source-page-19.png` 曾全部 hash 相同, When 任务脚本重新采集源 PDF 页图, Then 应直接从 `source-batch-record-template.pdf` 真页渲染 19 张互不相同的 PNG，作为后续分页节奏对比基线。
- BDD: 打印头配置应回到积木报表约定的范围语义 -> Given `jmsheet.js` 对 `fixedPrintHeadRows` / `fixedPrintTailRows` 的消费是 `{sri,sci,eri,eci}` 范围对象, When JsonBuilder 输出文档头页型的打印头配置, Then 不应再输出整数行号数组，而应输出标准范围对象。
- RED: 旧 `capture-source-doc-pdf-pages.mjs` -> FAIL, `source-page-01.png`、`source-page-02.png`、`source-page-10.png`、`source-page-19.png` 的 SHA-256 完全相同，证明浏览器 `file://...#page=` 截图口径失效。
- GREEN: 新 `capture-source-doc-pdf-pages.mjs` -> PASS, 已通过 `pdfjs-dist + pdf.worker + canvas native binding` 真页渲染出 `19` 张源页 PNG；`source-page-01/02/10/16/17/19.png` 的 SHA-256 全部不同。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 79/79 通过，覆盖本轮直接改动面。
- RED: 更广义 batchrecord 回归 `...MesProBatchRecordReportServiceImplDbTest...` -> FAIL, 当前仓内该测试类额外暴露一条报错文案断言波动；它不在本轮直接改动面，但需要单独跟踪。
- GREEN: 真实前端 `A 直接 .doc` 再生成与抓图 -> PASS, `recognize-fixed?routeKey=A` 返回 `importedCount=15`、`updatedCount=15`，最新 Route A 15 张报表均可打开并抓图。
- RED: 真实前端 `清空电子批记录报表` -> FAIL, live 数据库当前缺少 `mes_pro_route_process.batch_record_report_id` 列，`delete-all` 返回 SQLSyntaxError：`Unknown column 'batch_record_report_id' in 'where clause'`。这属于前置条件缺失，当前严格工作流不能把它当成功删除。
- Notes: 本轮已把“源页基线脚本”从不可靠状态修复到可用状态；但真实 `清空 -> 重新生成` 全链路又被 live schema 缺列阻塞，后续若继续迭代视觉差异，必须先决定是补 live schema 还是调整绑定校验查询口径。

## Pass 10

- BDD: 严格真实工作流中的 `清空电子批记录报表` 必须基于已声明 schema 前置条件成功执行 -> Given 代码、DO、测试资源和现有迁移都一致要求 `mes_pro_route_process.batch_record_report_id` 存在, When 当前验证库缺少这列导致 `delete-all` 500, Then 应显式补齐已有迁移前置条件，而不是用 fallback、mock 成功或跳过删除来掩盖。
- RED: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> FAIL, `delete-all` 返回 SQLSyntaxError：`Unknown column 'batch_record_report_id' in 'where clause'`，导致只能 `updatedCount=15`，无法满足“先清空再重生成”的严格要求。
- GREEN: 应用已有迁移 `sql/mysql/20260522_mes_route_process_batch_record_binding.sql` 到当前验证库 -> PASS, `mes_pro_route_process.batch_record_report_id` 已补齐。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 再次返回 `deletedReportCount=15`、`deletedMetadataCount=15`，随后 `recognize-fixed?routeKey=A` 返回 `importedCount=15`、`createdCount=15`、`updatedCount=0`，严格真实工作流恢复。
- Notes: 这轮没有新增兼容分支或降级逻辑；只是把仓库里已存在的 route-process 批记录绑定迁移补到当前 live 验证库，使真实验证前置条件恢复成立。

## Pass 11

- BDD: 续页头首行必须向分页器显式声明“从这里开始新页” -> Given `jmsheet.js` 当前不会消费 `zonedEditionList` 做分页，而分页器会读取真实行高与 `row.pagingRow` 一类的 break-before 物理信号, When T13 这类长工序页在中段出现重复文档头, Then JsonBuilder 应对续页头首行输出 `pagingRow=true`，并保留续页前真实空白行，而不是只靠 `zonedEditionList` 或继续增大弱 spacer。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 第一版 `pagingRow` 识别按文档头同构结构判断，未命中 T13 这种“文本相同但列位漂移”的续页头；对应测试暴露 `paging row not found`。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldMarkPagingRowForShiftedRepeatedDocHeaderBlocks -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 续页头识别已收敛到共享文档头文本语义，不再依赖完全相同的列位结构。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 83/83 通过，覆盖本轮直接改动面。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, `delete-all` 返回 `15/15`，`recognize-fixed?routeKey=A` 返回 `importedCount=15`、`createdCount=15`、`updatedCount=0`。
- GREEN: live JSON 复核 -> PASS, 最新 `EBR_TN122_A_T13` 的 `jimu_report.json_str` 已出现 `ROW 17 pagingRow=true`，其前一行 `ROW 16 height=56` 为续页前真实分页留白，`fixedPrintHeadRows=[{sri:1,sci:0,eri:2,eci:18}]` 仍保持有效。
- GREEN: live 截图复核 -> PASS, 最新 `T13` 截图里第二个文档头前的断页留白明显强于上一轮，不再和上一段热合明细紧贴；但与源 Word 第 16/17 页的真实分页间隔相比仍有差距。

## Pass 12

- BDD: 续页空白必须属于新页顶部而不是上一页底部 -> Given `jmsheet.js` 会在命中 `pagingRow=true` 的当前行前强制翻页, When JsonBuilder 为重复文档头插入续页 spacer, Then `pagingRow` 应挂到 spacer 行本身，让这段 `56px` 留白跟着新页一起开始，而不是继续挂在续页头首行。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldInsertVisiblePageGapBeforeRepeatedDocLikeHeaderBlocks+build_shouldMarkPagingRowForShiftedRepeatedDocHeaderBlocks -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 当前实现仍把 `pagingRow` 打在续页头行本身：定向测试预期 blank spacer row 却读到了 `球囊扩张压力泵生产记录`，shifted header 场景也暴露 `pagingRow` 未落到 spacer 行。
- GREEN: 同一条 JsonBuilder 定向命令 -> PASS, `pagingRow` 已从续页头行移动到续页 spacer 行，blank row + next header row 的分页语义回到共享预期。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordDocParserTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 83/83 通过。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -DskipTests package` -> PASS, 当前任务相关 `yudao-module-mes-2026.04-SNAPSHOT.jar` 已重打。
- GREEN: 作用域受控的 live 包替换 -> PASS, 将新的 `yudao-module-mes-2026.04-SNAPSHOT.jar` 以 `STORED` 方式替换进当前 `output/runtime/backend-20260522-222706.jar`，生成 `output/runtime/backend-20260522-231110.jar` 并成功重启到 `48081`。
- GREEN: 真实前端 `清空电子批记录报表 -> A 直接 .doc` -> PASS, Playwright 实际返回 `deletedReportCount=15`、`deletedMetadataCount=15`，随后 `recognize-fixed?routeKey=A` 返回 `importedCount=15`、`createdCount=15`、`updatedCount=0`。
- GREEN: live `jmreport/show` 运行时取证 -> PASS, 最新 `EBR_TN122_A_T13` 的 `jsonStr` 中已确认 `ROW 16 pagingRow=true`、`ROW 16 text=' '`、`ROW 16 height=56`，`fixedPrintHeadRows=[{sri:1,sci:0,eri:2,eci:18}]` 保持有效；这说明续页留白现在属于新页起点而不是旧页尾部。
- Notes: 这一轮更偏分页语义修正，连续长页截图里的视觉变化有限，但运行时 break-before 所属行已经回正；若还要继续增强 `T13` 的“断页感”，下一步应优先考虑 continuation top spacer 节奏或直接抓打印页，而不是再把 `pagingRow` 挂回页头行。

## Pass 13

- BDD: 电子批记录报表的真实分页对比必须基于分页 PDF，而不是连续长页截图 -> Given `T10/T13` 的连续长页截图会把分页语义和打印盒模型揉平, When 主 agent 复核 Jimu 的真实导出入口, Then 应优先拿到稳定的分页 PDF，再把生成 PDF 渲染成逐页 PNG 与源文真页对比。
- RED: 复用现有 `capture-source-doc-pdf-pages.mjs` 去渲染生成 PDF -> FAIL, 现有脚本一方面仍带源稿路径假设，另一方面在 fresh shell 里会撞到 `DOMMatrix` / canvas binding 差异，不能稳定产出“生成 PDF 页图”清单。
- GREEN: 真实 `T13` 报表页 `导出 -> PDF图像` -> PASS, 当前 viewer 可稳定下载 `电子批记录[A]-表13-单包装工序生产记录.pdf`；同一页 `导出 -> PDF` 仍会命中 `/jmreport/exportPdfStream` 并因 `Font.getSize()` 空指针在 live 上失败，这一条已记录为独立前置风险。
- GREEN: 新增任务脚本 `scripts/render-pdf-pages.py` -> PASS, 使用 `PyMuPDF` 已稳定把生成的 `T13/T10` 打印态 PDF 渲染成逐页 PNG。
- GREEN: 打印态页级基线建立 -> PASS, `T13` 当前真实打印态为 `2` 页，`T10` 当前真实打印态为 `1` 页；渲染产物已落到：
  - `artifacts/generated-print-probe/t13-pdf-pages/`
  - `artifacts/generated-print-probe/t10-pdf-pages/`
- GREEN: 打印态对比复核 -> PASS, 最新 `T13` 的分页 PDF 证明确实存在新的结构性差异：第 1 页尾部出现了一个源 Word 第 16 页中并不存在的重复文档头，而第 2 页顶部并没有续页头；这说明当前问题已经从“页间距偏弱”升级成“合成 continuation header 本身与源稿打印态不一致”。
- Notes: 这轮 accepted 改动主要是验证脚本层，不是生产代码层。打印态证据已经表明，下一轮如果继续改生产规则，优先应回到 `LayoutCalibrator.insertContinuationHeadersForLongRepeatedOperationSegments()` 这层，重新审视 synthetic document header 是否该出现在 Route A 这类长设备矩阵页里。

## Pass 14

- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordDocParserTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordRouteARecognizerTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 100/100 通过，当前 accepted 共享分页/版式收敛改动已完成定向回归。
- GREEN: `git commit -m "任务: 优化电子批记录报表分页版式"` -> PASS，accepted shared pagination/layout slice 已作为独立后端提交 `720c8e4ced` 落盘。
- BLOCKED: 最终视觉保真闭环 -> FAIL，打印态 `T13` 仍存在源 Word 中不存在的 synthetic continuation header，且 `/jmreport/exportPdfStream` NPE 与无关 DCC 编译阻塞仍限制最终闭环验证。
