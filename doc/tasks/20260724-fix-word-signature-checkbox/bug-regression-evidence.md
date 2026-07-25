# Bug Regression Evidence

## Bug

Word 批记录表单导入后，粗洗工序等表单中 `操作人/日期`、`复核人/日期` 签名区域会显示为 checkbox 结果选项，导致签名位置语义错误。

## Expected

只有 `结果` 列中的真实勾选项应生成 checkbox；落入 `操作人/日期`、`复核人/日期` 尾部区域的 checkbox-like 碎片应并入左侧结果选项或保持普通文本填写，不得生成额外 checkbox 控件。

## Reproduction

新增 `MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail`，构造 `结果 -> 操作人/日期 -> 复核人/日期` 表头与正文 checkbox 碎片列偏移的 Word 解析表格。

## Root Cause

原逻辑只通过“上方非空表头是否精确覆盖当前列”判断签名日期区域。Word 表格解析/列校准后，checkbox 碎片可能落在签名日期尾部的相邻或间隙列，不再被 `操作人/日期`、`复核人/日期` 表头精确覆盖，进而被 `isCheckboxChoiceText` 和自动规则建议提升为 checkbox。

## RED:

`mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`signature/date checkbox fragments must be merged as result options ==> expected: not <null>`。

## GREEN:

`mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotPromoteMisalignedCheckboxFragmentsInsideSignatureDateTail" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

## Verification

新增 Builder 用例已通过。自动规则识别的新增与既有签名日期用例均通过（2 tests）；JSON 构建器的新增与既有签名日期用例均通过（2 tests）。

## Blockers

无。


## 真实页面 E2E 追加复现

- Reproduction: `EDHR_WORD_IMPORT_PRODUCT_NAME=数显球囊扩张压力泵`、`EDHR_WORD_IMPORT_SAMPLE_DOC=E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\src\test\resources\fixtures\pressure-pump-record.doc` 执行 `node tests\e2e\edhr-word-template-import-real-flow.e2e.js`。
- Observed: 导入成功后 API 核验失败，`粗洗工序生产记录` 第 6 行第 16 列和第 18 列位于 `操作人/日期`、`复核人/日期` 表头下方，仍生成 `fillForm.componentFlag=checkbox` 与 `edhrCellRule.componentFlag=checkbox`。
- Refined Root Cause: 既有修复覆盖了“checkbox 文本碎片落入签名日期尾区”的场景，但真实 Word 中签名日期列是空白可填写格。自动建议先从左侧结果列取到 `□符合要求/□不符合要求` 作为 label，再由 boolean cue 将空白签名格提升为 checkbox。
