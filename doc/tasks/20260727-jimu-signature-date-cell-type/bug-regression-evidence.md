# Bug Regression Evidence

## Bug Summary

eDHR 批记录模板中，“记录人/日期”等签名日期标签右侧的宽空白填写单元格会被后端 Jimu JSON builder 生成为 `fillForm.componentFlag=input-textarea`，导致 Jimu 编辑页右侧显示“当前组件：多行文本”。

## Expected Behavior

签名日期语义的宽空白填写单元格不应因为宽度或合并列形态被归类为叙述型多行文本；它应保持单行结构化输入控件 `input-text`，并继续携带 `edhrSignature` 签名日期标记。

## Reproduction Command

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`MesProBatchRecordReportJsonBuilder#resolveFillInputType` 在没有同一行标签语义上下文的情况下，先用 `isWideBlankNarrativeArea` 将宽合并空白填写格归类为 `Textarea`。当该宽空白格位于“记录人/日期”这类签名日期标签右侧时，语义上是签名日期填写位，但仍被宽度规则抢先判定为多行文本。

## Regression Test Added

`MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell`

## RED:

Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: FAIL, expected `input-text` but was `input-textarea`.

## GREEN:

Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: PASS, 1 test run, 0 failures.

## Risk And Regression Scope

The fix is limited to fillable blank cells whose nearest non-blank cell on the same source row matches the existing signature-date label detector, such as “记录人/日期”, “操作人/日期”, “复核人/日期”, “确认人/日期”, and “批准人/日期”. Existing narrative wide blank cells remain textarea, verified by `build_shouldUseTextareaFillFormForTallOrMergedBlankCells`.

## Verification

Targeted builder regression and signature-date cell-rule regression both passed after the fix.

## Blockers And Follow-up

No code-level blocker remains for the bug fix. Repository closeout commit/push is blocked by unrelated pre-existing / concurrent dirty worktree changes unless the project-required baseline flow is explicitly authorized.
