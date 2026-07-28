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


## 真实页面 E2E 二次复现与最终修复

- Reproduction: 最新 Jar 首次复跑后，`清洁工序生产记录` 第 9 行第 8 列位于签名日期尾部区域，但从左侧 `□30atm压力表` 继承为 `fillForm.componentFlag=checkbox`。
- Reproduction: 修复中间表头遮挡后再次复跑，`粗洗工序生产记录` 第 8 行第 17 列已识别为 `valueType=STRING`、`label=操作人/日期`，但旧 `fillForm.componentFlag=checkbox` 和 boolean value 仍残留。
- Root Cause: `resolveUpperSignatureDateLabel` 在遇到最近的非签名中间表头时直接返回空字符串，导致更上层签名日期表头失效；同时 STRING 规则默认保留 existingComponentFlag，使签名日期区域旧 checkbox fillForm 未被强制改写。
- Fix: 签名日期表头识别改为跳过非签名中间表头并继续向上查找；签名日期 STRING 规则固定使用 `input-text`；非 BOOLEAN 同步 fillForm 时清理 boolean `value/defaultValue`。
- RED: `MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteBlankSignatureDateCellsPastIntermediateCheckboxRows` -> FAIL，`expected: <STRING> but was: <BOOLEAN>`。
- RED: `MesProBatchRecordCellRuleSupportTest#buildSuggestions_rewritesExistingCheckboxFillFormUnderSignatureDateHeaders` -> FAIL，`expected: <input-text> but was: <checkbox>`，随后暴露 `expected: <> but was: <false>`。
- GREEN: 自动规则识别 5 tests PASS；JSON 构建器 2 tests PASS；真实页面 E2E PASS，`signatureDateCellsChecked=177`。
- Risk: 已导入并保存的旧错误模板不会自动迁移，需重新导入或重新生成受影响模板。
