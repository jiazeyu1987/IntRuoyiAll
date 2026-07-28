# Bug Regression Evidence

## Bug Summary

eDHR 批记录模板中，“记录人/日期”等签名日期标签右侧的宽空白填写单元格会被后端 Jimu JSON builder 生成为文本类 `fillForm.componentFlag`。最初表现为 `input-textarea`，上一轮半修复后变为 `input-text`，但仍不是电子签名组件，导致 Jimu 编辑页右侧不会显示电子签名控件。

## Expected Behavior

签名日期语义的宽空白填写单元格不应因为宽度、合并列形态或普通文本强制路径被归类为文本类组件；它必须生成 `componentFlag=signature`，并继续携带 `edhrSignature` 签名日期标记。

## Reproduction Command

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`MesProBatchRecordReportJsonBuilder` 已能通过同一行标签识别“记录人/日期”右侧空白格，并写入 `edhrSignature` 元数据；但 Jimu 编辑器右侧“当前组件”读取的是 `fillForm.componentFlag`。上一轮修复只把这类格子从 textarea 强制成 `input-text`，没有把签名语义同步到 `componentFlag=signature`，所以 UI 仍按普通文本控件展示。

## Regression Test Added

`MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell`

## RED:

Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: FAIL, expected `signature` but was `input-text`.

## GREEN:

Command: `mvn -q -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: PASS.

## Risk And Regression Scope

The fix is limited to fillable blank cells whose nearest non-blank cell on the same source row matches the existing signature-date label detector, such as “记录人/日期”, “操作人/日期”, “复核人/日期”, “确认人/日期”, and “批准人/日期”. Existing narrative wide blank cells remain textarea, and signature-date checkbox fragments keep their existing non-signature handling.

## Verification

Targeted builder regression and signature-date cell-rule regression both passed after the fix.

## Blockers And Follow-up

No code-level blocker remains for the bug fix. Repository closeout commit/push is blocked by unrelated pre-existing / concurrent dirty worktree changes unless the project-required baseline flow is explicitly authorized.
