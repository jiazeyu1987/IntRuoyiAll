# 执行日志

BDD: Route E 损耗报告单顶部填写格必须保留 -> Given 源 Word 顶部存在交替的标签列与空白填写列 / When Route E 先渲染 PNG 再回识别 / Then 回识别后的表格结构仍必须保留这些空白填写格，供后续 JsonBuilder 生成 fillForm。

RED: `mvn.cmd -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteERecognizerTest#recognize_whenImageParserMergesLossReportEntryCells_restoresSourceFillableBlanks" "-DfailIfNoTests=false" "-DfailIfNoSpecifiedTests=false" test` -> FAIL, expected `<8>` but was `<4>`, 证明 Route E 当前把顶部 4 个空白填写格吞并成 4 个合并标签格。
GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteERecognizerTest#recognize_whenImageParserMergesLossReportEntryCells_restoresSourceFillableBlanks" "-DfailIfNoTests=false" "-DfailIfNoSpecifiedTests=false" test` -> PASS, 结构化字段行的空白填写格已恢复。
GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteERecognizerTest" "-DfailIfNoTests=false" "-DfailIfNoSpecifiedTests=false" test` -> PASS, Route E 全类 7 tests 通过。
GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportJsonBuilderTest#build_shouldNotCreateFillFormControlsForTrailingPaddingColumns" "-DfailIfNoTests=false" "-DfailIfNoSpecifiedTests=false" test` -> PASS, 现有 JsonBuilder 元数据空白格 fillForm 行为未回退。
REVIEW: candidate comparison -> a2 FAIL；a1/a3/a4/a5 PASS。放行 a3，因为它只对 `FIELD/TABLE_HEADER` 结构化行恢复空白格，逻辑面最窄，较 a1 更少误恢复风险，较 a4/a5 少样式/列宽附带复杂度。
