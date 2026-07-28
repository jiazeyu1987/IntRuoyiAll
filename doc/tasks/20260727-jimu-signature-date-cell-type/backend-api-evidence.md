# Backend Evidence

## Scope

Backend report JSON generation in `MesProBatchRecordReportJsonBuilder` for eDHR batch-record Jimu fillForm cells.

## Contract

For same-row signature-date labels and their right-side blank fill cells, builder output must keep a fillable Jimu control, emit `componentFlag=signature`, and preserve `edhrSignature` metadata.

## Auth Permissions Validation

Not applicable; this change does not alter APIs, persistence, auth, or permissions.

## Config Services Fixtures Migrations

No config, external service, database fixture, or migration is required. The regression uses an in-memory parsed table fixture.

## BDD Scenarios

BDD: 签名日期宽空白单元格生成为电子签名控件 -> Given 批记录模板包含“记录人/日期”标签及其右侧宽空白填写单元格 / When 后端生成 Jimu 报表 JSON fillForm / Then 该填写控件必须生成 `componentFlag=signature`，并保留 `edhrSignature.enabled=true`、`actionType=SUBMIT` 和原始标签。

## RED:

`mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `signature` but was `input-text`.

## GREEN:

`mvn -q -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

## Contract Verification

`mvn -q -pl yudao-module-mes "-Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldUseSignatureComponentForWideSignatureDateBlankCell+build_shouldUseTextareaFillFormForTallOrMergedBlankCells" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

`mvn -q -pl yudao-module-mes "-Dtest=MesProBatchRecordCellRuleSupportTest#buildSuggestions_doesNotPromoteSignatureDateColumnCheckboxFragments+buildSuggestions_rewritesExistingCheckboxFillFormUnderSignatureDateHeaders+buildSuggestions_doesNotPromoteMisalignedSignatureDateTailCheckboxFragments+buildSuggestions_doesNotPromoteBlankSignatureDateCellsFromLeftCheckboxResult+buildSuggestions_doesNotPromoteBlankSignatureDateCellsPastIntermediateCheckboxRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

## Observability

No runtime logging change was needed; behavior is covered by deterministic unit tests.

## Blockers

No implementation blocker remains. Commit/push closeout is blocked by unrelated workspace dirty state unless the baseline flow is authorized.
