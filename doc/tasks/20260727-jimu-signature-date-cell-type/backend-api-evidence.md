# Backend Evidence

## Scope

Backend report JSON generation in `MesProBatchRecordReportJsonBuilder` for eDHR batch-record Jimu fillForm cells.

## Contract

For same-row signature-date labels and their right-side blank fill cells, builder output must keep a fillable Jimu control, must not emit `componentFlag=input-textarea`, and must preserve `edhrSignature` metadata.

## Auth Permissions Validation

Not applicable; this change does not alter APIs, persistence, auth, or permissions.

## Config Services Fixtures Migrations

No config, external service, database fixture, or migration is required. The regression uses an in-memory parsed table fixture.

## BDD Scenarios

BDD: 签名日期宽空白单元格不生成为多行文本 -> Given 批记录模板包含“记录人/日期”标签及其右侧宽空白填写单元格 / When 后端生成 Jimu 报表 JSON fillForm / Then 该填写控件不得生成 `input-textarea`，应按单行签名日期结构化控件生成。

## RED:

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `input-text` but was `input-textarea`.

## GREEN:

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

## Contract Verification

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldNotUseTextareaForWideSignatureDateBlankCell+build_shouldUseTextareaFillFormForTallOrMergedBlankCells" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments+buildSuggestions_rewritesExistingCheckboxFillFormUnderSignatureDateHeaders+buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments+buildSuggestions_doesNotPromoteBlankSignatureDateCellsFromLeftCheckboxResult+buildSuggestions_doesNotPromoteBlankSignatureDateCellsPastIntermediateCheckboxRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

## Observability

No runtime logging change was needed; behavior is covered by deterministic unit tests.

## Blockers

No implementation blocker remains. Commit/push closeout is blocked by unrelated workspace dirty state unless the baseline flow is authorized.
